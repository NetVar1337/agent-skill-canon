---
name: external-esp
description: "Use when building or validating an external game telemetry overlay (ESP): read a separate process, recover build-pinned entity/camera data, transform world coordinates through a verified view-projection matrix, and render the result in an external HWND. Routes offset recovery and overlay implementation to their specialist skills."
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: game
  author: Admin
  related_skills: [game-hacking, offset-dumper, pattern-scanner, imgui-overlay]
---

# External ESP

## Overview

An external ESP has two isolated pipelines:

1. **Telemetry:** obtain a coherent, read-only snapshot of world, camera, and entity state from a separate process.
2. **Projection and rendering:** convert each world-space point into client-space pixels, then render those pixels through a separately owned overlay window.

Keep the pipelines independent. The renderer consumes plain `Snapshot` data and never dereferences target-process addresses; telemetry can therefore be tested without drawing, and projection can be tested with synthetic coordinates before any target data is used.

## When to Use

- Building a read-only, external player/entity diagnostic overlay.
- Recovering entity positions, health, team, camera, or a view-projection matrix for a pinned game build.
- Debugging WorldToScreen, matrix conventions, viewport alignment, or external overlay drift.
- Replacing brittle bare offsets with a validated address recipe.

Route engine schemas and patch-update recovery to `offset-dumper`; route HWND, Direct2D/D3D/ImGui, DPI, input, and multi-monitor behavior to `imgui-overlay`. Use `game-hacking` for broader engine, server-authority, or anti-cheat research.

## Contract

Record these inputs before treating any value as usable:

| Input | Required evidence |
|---|---|
| Target | process image name, PID, architecture, and main-window HWND |
| Build | module SHA-256, image size, timestamp/version |
| Address recipe | module-relative RVA/signature, dereference sequence, field offset, expected type |
| Coordinate dialect | handedness, matrix layout/order, world unit, and camera origin convention |
| Viewport | target *client* rectangle, monitor DPI, and display mode |

A field is valid only when it is readable **and** satisfies a semantic check, such as health in a plausible range or a position changing coherently while its observed actor moves. An address that merely returns bytes is not validated.

## Workflow

### 1. Pin target and build

1. Locate the target process with `CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0)` and match the executable name case-insensitively.
2. Open it with the minimum read/query access required by the data path: `PROCESS_VM_READ | PROCESS_QUERY_LIMITED_INFORMATION`.
3. Enumerate the target modules and record the main-module base, size, and hash. Never rely on an absolute address across restarts or builds.
4. Identify the game HWND and obtain `GetClientRect`; use `ClientToScreen` to map client coordinates to overlay coordinates.

**Done when:** the evidence record names one PID, one module hash, an HWND, and a client viewport. If any differs after a restart/update, invalidate the recovered telemetry recipe.

### 2. Build a checked reader

Use a result-bearing reader. A failed read must skip the current entity/frame rather than reuse stale or uninitialised data.

```cpp
#include <optional>
#include <type_traits>

bool IsCanonicalUserAddress(uintptr_t address) {
#if defined(_WIN64)
    return address >= 0x10000 && address <= 0x00007FFFFFFFFFFF;
#else
    return address >= 0x10000 && address <= 0x7FFEFFFF;
#endif
}

template <typename T>
std::optional<T> Read(HANDLE process, uintptr_t address) {
    static_assert(std::is_trivially_copyable_v<T>);
    if (!IsCanonicalUserAddress(address)) return std::nullopt;

    T value{};
    SIZE_T transferred = 0;
    if (!ReadProcessMemory(process, reinterpret_cast<LPCVOID>(address),
                           &value, sizeof(value), &transferred) ||
        transferred != sizeof(value)) {
        return std::nullopt;
    }
    return value;
}
```

For pointer traversal, read each pointer into a local `uintptr_t`, reject null/non-canonical values, then add the next offset with overflow checking. Use `VirtualQueryEx` when diagnosing unexplained failures or validating that a pointer leads to a committed readable region; it is not a replacement for `ReadProcessMemory` success checks.

**Done when:** a deliberately invalid address returns failure without crashing, and every consumer handles missing data explicitly.

### 3. Recover data as recipes, not bare offsets

Recover the minimum read model first:

```text
module + entity-list recipe  -> entity pointer(s)
entity + transform field     -> world position
entity + state fields        -> health/team/alive state
module/camera recipe         -> view-projection matrix
```

For each field, record the source (schema, disassembly observation, dynamic scan, or signature), type, alignment, expected range, and a live transition that proves it. Prefer a unique executable-section signature plus a relative-address resolution over a raw module offset. If an engine exposes reflection/schema data, use that source before pointer scans.

Follow `offset-dumper` for build manifests, signatures, drift testing, and engine-specific extraction. Follow `pattern-scanner` for signature design and uniqueness assertions.

**Done when:** each recipe resolves exactly once for the pinned build and passes at least one independent runtime semantic assertion.

### 4. Capture coherent snapshots

Do not render values while traversing target memory. Read into local value types, validate them, then publish one immutable `Snapshot` to the renderer.

```cpp
struct Vec3 { float x, y, z; };
struct EntitySample { Vec3 world; int health; int team; bool alive; };
struct Snapshot {
    float viewProjection[16];
    std::vector<EntitySample> entities;
    RECT clientRect;
};
```

Read the matrix once per frame, then collect entity samples. If the target exposes a frame counter/tick, read it before and after collection and discard the snapshot if it changed. Otherwise, bound collection work, reject partial records, and label the frame as best-effort.

**Done when:** the draw thread touches only local `Snapshot` data, and a target restart/exit produces an empty or skipped frame rather than stale boxes.

### 5. Establish the matrix dialect before using WorldToScreen

A 4×4 float array has no universal memory order. Establish all of these with a controlled observation:

- Does the target use row vectors or column vectors?
- Is the array row-major or column-major in memory?
- Is it a combined view-projection matrix or must view and projection be multiplied?
- Which axis is up/forward, and which clip-space Z range is used?

Start with an identity matrix test, then observe a known point while moving the camera right, up, and forward. A correct implementation moves in the expected screen direction, goes off-screen at the expected edge, and rejects points behind the camera.

For the common row-vector formulation used below, the screen conversion is:

```cpp
struct Vec4 { float x, y, z, w; };

bool WorldToScreen(const Vec3& world, Vec3& screen,
                   const float m[16], const RECT& viewport) {
    const Vec4 clip{
        world.x * m[0]  + world.y * m[1]  + world.z * m[2]  + m[3],
        world.x * m[4]  + world.y * m[5]  + world.z * m[6]  + m[7],
        world.x * m[8]  + world.y * m[9]  + world.z * m[10] + m[11],
        world.x * m[12] + world.y * m[13] + world.z * m[14] + m[15],
    };
    if (clip.w <= 0.001f) return false;

    const float ndcX = clip.x / clip.w;
    const float ndcY = clip.y / clip.w;
    const float ndcZ = clip.z / clip.w;
    if (ndcX < -1.f || ndcX > 1.f || ndcY < -1.f || ndcY > 1.f) return false;

    const float width = static_cast<float>(viewport.right - viewport.left);
    const float height = static_cast<float>(viewport.bottom - viewport.top);
    screen.x = viewport.left + (ndcX + 1.f) * 0.5f * width;
    screen.y = viewport.top  + (1.f - ndcY) * 0.5f * height;
    screen.z = ndcZ;
    return std::isfinite(screen.x) && std::isfinite(screen.y);
}
```

If the target matrix has the opposite convention, transpose the matrix once at the ingestion boundary or use the matching multiplication order. Do not compensate with arbitrary sign flips until the dialect test identifies the mismatch.

**Done when:** synthetic identity-matrix cases and three camera-motion observations agree with the selected convention; the screen mapping uses `(ndc + 1) / 2`, not the common erroneous addition of `ndc` to half the viewport size.

### 6. Render through a client-aligned external overlay

Use a separate topmost layered HWND matched to the target client rectangle. Add `WS_EX_NOACTIVATE` and return `HTTRANSPARENT` for click-through regions so drawing does not take focus. Update its location whenever the target moves, resizes, changes DPI, or changes display mode.

Use `BeginPaint`/`EndPaint` or a retained Direct2D/D3D/ImGui surface. Do not call `GetDC(NULL)` in a frame loop: it draws on the desktop rather than the target viewport and requires `ReleaseDC`; creating GDI objects each frame also leaks unless previous selections are restored before deletion.

For a first diagnostic renderer, draw only a crosshair at a projected sample and a bounded rectangle around each accepted entity. Route production renderer choice and DPI/device-loss handling to `imgui-overlay`.

**Done when:** the overlay remains client-aligned through window moves, resize, alt-tab, and DPI changes; closing either process releases HWND, DC, GDI/D3D resources, and process handles.

## Projection Test Cases

Run these before accepting game-derived matrix data:

| Case | Matrix/world point | Expected result |
|---|---|---|
| Centre | identity; `(0, 0, 0)` | viewport centre |
| Right edge | identity; `(1, 0, 0)` | right edge |
| Top edge | identity; `(0, 1, 0)` | top edge |
| Out of frustum | identity; `(1.1, 0, 0)` | rejected |
| Behind camera | any `clip.w <= epsilon` | rejected |
| Resize | same NDC point after viewport resize | same proportional location |

## Common Pitfalls

1. **Using desktop dimensions.** ESP coordinates must start in the game client rectangle; translate to screen coordinates only when positioning the external HWND.
2. **Assuming every matrix is alike.** Array order, vector order, handedness, and clip Z differ. Prove the dialect with movement and synthetic tests.
3. **Accepting readable garbage.** Require actor identity, plausible ranges, and a live transition for every field.
4. **Mixing remote reads with drawing.** A failed read must never become an old on-screen value. Snapshot first, render second.
5. **Leaking GDI resources.** Restore the old selected pen/brush before deleting an object and release acquired DCs every frame.
6. **Using fixed strides without evidence.** Entity lists may be chunked, sparse, handle-based, or schema-backed. Validate the container layout per build.
7. **Treating overlay architecture as invisibility.** A separate process has different artifacts, not an absence of observable artifacts; evaluate the selected environment with `game-hacking` and `imgui-overlay`.

## Verification Checklist

- [ ] Target PID, module hash, architecture, HWND, and client rectangle are recorded.
- [ ] Every address recipe is build-pinned, uniquely resolved, and semantically validated.
- [ ] Failed reads, invalid pointers, target exit, and restart yield skipped frames without crashes or stale data.
- [ ] Renderer receives only local snapshot values.
- [ ] WorldToScreen passes all synthetic projection cases and controlled camera-motion checks.
- [ ] Overlay tracks client moves, resize, alt-tab, DPI changes, and multi-monitor transitions.
- [ ] Frame timing, read failures, rejected entities, and resource teardown are logged or otherwise observable.
