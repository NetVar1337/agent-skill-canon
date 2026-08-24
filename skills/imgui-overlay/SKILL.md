---
name: imgui-overlay
description: "ImGui overlay engineering: internal present-hook (D3D11/12, Vulkan) vs external layered window, input capture without stealing focus (raw input, driver input), DPI/multi-monitor, fullscreen/HDR pitfalls, render-backend wiring, and detectability tradeoffs of each mode. Use when building menu/ESP overlays for tools or research."
version: 2.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: game
---

# ImGui overlay

Two architectures, different threat models:

| Mode | Strength | Weakness |
|---|---|---|
| Internal (hook Present in-process) | pixel-perfect sync, no window artifacts | code inside the target; injection surface; AC prime target |
| External (own transparent HWND) | no code in game process | window is enumerable; bitblt/capture-compare detection; input routing is harder |
| External + HV/kernel read | same, with read isolation | the full stealth stack (see `valthrun-style-stack`, `stealth-hypervisor`) |

Pick the mode *from the detection requirements*, not habit.

## Internal: present hook

- **D3D11** (the default): grab swapchain vtable — either from an existing
  device (find via game's dxgi usage) or create a temp dummy device/swapchain
  and steal vtable indices: `Present` = index 8, `ResizeBuffers` = 13
  (IDXGISwapChain). Hook both — ResizeBuffers without releasing/recreating
  ImGui render data = crash on alt-enter/settings change.
- **D3D12**: hook `IDXGISwapChain::Present` on the game's queue; ImGui DX12
  backend needs the render target per buffer — `ID3D12DescriptorHeap` RTV
  management is the boilerplate that breaks; copy a known-good backend wiring
  and pin the backend version to your Dear ImGui version.
- **Vulkan**: hook `vkQueuePresentKHR` via trampoline on the driver's function
  (get proc address per instance); render pass creation inside the hook on the
  game's image index. Linux/Proton games: same hook, watch layer-registry
  differences.
- Init order rule: build fonts, style, and platform backend *inside* the first
  Present call you intercept (device/context exist by then), not at DLL load —
  load-time D3D state is not guaranteed.
- Frame hygiene: never block Present beyond vsync budget; if your work (ESP
  math) is heavy, compute into a buffer elsewhere, render cheap.

```cpp
// D3D11 skeleton
HRESULT hkPresent(IDXGISwapChain* sc, UINT sync, UINT flags) {
    if (!init) { ID3D11Device* dev; ID3D11DeviceContext* ctx;
        sc->GetDevice(__uuidof(ID3D11Device), (void**)&dev);
        dev->GetImmediateContext(&ctx);
        ImGui_ImplDX11_Init(dev, ctx); ImGui_ImplWin32_Init(game_hwnd); init = true; }
    ImGui_ImplDX11_NewFrame(); ImGui_ImplWin32_NewFrame(); ImGui::NewFrame();
    Render();                       // menu + draw lists
    ImGui::Render(); ImGui_ImplDX11_RenderDrawData(ImGui::GetDrawData());
    return oPresent(sc, sync, flags);
}
```

## External: layered window

- `WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_TOPMOST | WS_EX_NOACTIVATE`,
  `SetLayeredWindowAttributes` (color-key) or per-pixel alpha via
  `UpdateLayeredWindow`. Color-key gets edge fringing on AA text; per-pixel
  costs a memcopy per frame — benchmark both.
- Render with D3D11 offscreen + flip into the layered surface, or Direct2D.
  Do **not** GDI-TextOut per frame.
- Click-through: toggle `WS_EX_TRANSPARENT` off only while menu open, and only
  over the menu's rect (hit-test via `WM_NCHITTEST` return `HTTRANSPARENT`)
  so the game keeps receiving input elsewhere.
- Sync: present on the game's monitor VBlank (DWM composition makes external
  overlays tear-free in windowed/borderless; measure with present mon
  counters).

## Input

- Menu hotkey: raw input (`WM_INPUT`, `RID_INPUT`) on the overlay or a message
 -only window — `RegisterRawInputDevices` without RIDEV_INPUTSINK only gets
  input when focused; a low-level `WH_KEYBOARD_LL` works globally but is a
  classic AC/EDR telemetry point. Choose consciously.
- External aim-style input synthesis is **out of scope here** — that's
  `aimbot-humanization` (and driver-level input is `kernel-dev` territory).
- Never `SetForegroundWindow` the overlay mid-game; steal focus = stutter +
  obvious.

## DPI, monitors, fullscreen

- Per-Monitor-V2 DPI awareness in the manifest; scale ImGui font atlas per
  DPI change (`WM_DPICHANGED`), not a global constant.
- Exclusive fullscreen: external overlays can't composite — require
  borderless (most modern games default), or hook the game's
  `SetFullscreenState` and refuse/bounce to borderless.
- HDR enabled: SDR overlay content gets washed out; render with correct
  color space support or tell the user HDR is unsupported for the overlay.

## Detectability notes (be honest in threat models)

- Internal: hook bytes on Present = classic AC scan (they checksum the
  vtable target's prologue). Options: hardware-breakpoint hooks
  (`anti-cheat-stack-walk-stealth`), kernel-assisted render (risky), or
  render-free overlays.
- External: `EnumWindows` sees you — window name/class/style combo is a
  fingerprint (unnamed windows still enumerable); occlusion/bitblt sampling
  (AC screenshots the screen and diffs overlay pixels) is cheap for them.
  Mitigations: hide window from enumeration requires kernel support;
  anti-screenshot via `SetWindowDisplayAffinity(WDA_EXCLUDEFROMCAPTURE)` on
  *your* window actually excludes it from most capture paths — verify per
  OS build.
- Stack, module, and handle hygiene: see `cheat-longevity-engineering`.

## Test matrix

1. Win10 + Win11, 100%/125%/150% mixed-DPI dual monitors, HDR on/off.
2. Alt-tab storm, resolution change, exclusive-FS fallback, overlay open at
  device-lost (`DXGI_ERROR_DEVICE_REMOVED` path must not crash).
3. Frame budget: overlay cost < 1.5 ms at 1440p/144 Hz on mid GPU.
4. Focus edge cases: gamepad-only UIs, game running as admin (external can't
   inject input/capture — know before you ship).

## Pair with

`game-hacking` (where overlay fits a tool), `valthrun-style-stack`
(external+kernel read stack), `aimbot-humanization` (input), `lang-cpp-game-hacking`
(code idiom), `cheat-longevity-engineering` (survival).
