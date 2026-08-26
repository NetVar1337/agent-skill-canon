---
name: valthrun-style-stack
description: "Valthrun-style external/HV game stacks: kernel driver + usermode interface + UEFI loader + overlay/radar; CS2-class architecture notes."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: game
  triggers:
    - "Valthrun"
    - "CS2 overlay"
    - "kernel read driver"
    - "UEFI driver loader"
---

# Valthrun-style read stack

Architecture pattern seen in external/HV game tooling releases:

## Components
| Piece | Role |
|---|---|
| Usermode overlay/radar | consume world state |
| Driver interface DLL | IOCTL/user API |
| Kernel driver | read physical/virtual memory |
| UEFI/ISO loader | early driver bring-up path |
| Zenith-class installer | package/driver deployment |

## Design lessons
- Split **trust** (loader/driver) from **features** (overlay)
- Version stamp every artifact (`_0157087` style hashes)
- Prefer read-only features for longevity (`cheat-longevity-engineering`)
- UEFI loaders interact with measured boot / TPM stories

## Local path
`Desktop/Valthrun/Release/*`

## Reference architecture walkthrough (CS2-class)

### Usermode layer
- Overlay window: `WS_EX_LAYERED|WS_EX_TRANSPARENT|WS_EX_NOACTIVATE` + `SetLayeredWindowAttributes`; fullscreen-exclusive targets force borderless or Present-hook rendering (`imgui-overlay`).
- Read abstraction: **batched multi-read** — collect all offsets for a frame into one `read_multiple` request; 1 syscall-ish transition per frame instead of hundreds. Latency budget: 16.6ms frame, keep read+draw < 3ms.
- Offset provider: consume schema (`schema.json` for CS2) → typed views; auto-refresh on game build stamp change; hard-fail (stop reading) on stale build to avoid garbage-driven bans.

### Driver interface
- No named device object (unnamed + direct-IPC via section object or flipped IOCtl on a hidden interface); see `driver-comm` for ring-buffer + event design.
- Process attach by PID → CR3 cache; handle PID reuse (cache invalidation on process teardown — check EPROCESS exit flag or re-resolve on access violation).
- Provide: `read_process_memory` (GVA via CR3 page-walk), `read_multiple`, optional `get_module_base` (walk guest PEB/LDR via CR3 — needs two-level: read PEB, walk InLoadOrderModuleList).

### Kernel read engine
- CR3 → PML4 self-walk translation (or use `MmGetVirtualForPhysical` on host side after resolving HPA):
```
HPA translate(CR3, GVA):
  for level in PML4..PT: phys = readphys(table + (idx*8)); apply mask; next
  PTE.Large -> huge page arithmetic
```
- Physical reads via MmMapIoSpace temp-map or direct __cpuid-checked map; batch contiguous PTEs when GVA range is 2MB-aligned.
- Keep driver read-only (no writes): removes a whole detection/telemetry class and most ban-stack exposure.

### Loader story
- Service-based load leaves `HKLM\SYSTEM\CurrentControlSet\Services` traces + event-log 7045. Alternatives: UEFI-stage load (pre-OS, no service record; interacts with measured boot — pair `tpm-attestation-research`), or kdmapper-style vulnerable-driver map (adds BYOVD ban surface — `byovd`).
- Version-stamp every artifact; mismatched driver/DLL versions = crash reports you can't reproduce.

## Detection surfaces (and counters) for external stacks
| Surface | Counter |
|---|---|
| PiDDBCache / MmUnloadedDrivers entries | don't MmUnload; map, don't register, or scrub |
| Kernel module list visibility | unlink or never link (manual map) — PatchGuard cost, see `kernel-dev` |
| ObRegisterCallbacks handle-strip by AC | external reader needs no cross-process handles — inherent win |
| NtReadVirtualMemory hooks in-game | you're not in-process — immune |
| Overlay window enumeration by AC | randomize window name/class, no GPU overlay caption, protect against `EnumWindows` heuristics |
| Read-latency fingerprints (thread hijack detection doesn't apply; but service IRP timing visible) | batch reads, jitter request timing |

## CS2-specific notes
- Schema fields (`m_iHealth`, `m_pGameSceneNode`) resolve via `schema.json` per build; entity list via `CEntityIdentity` singly-linked list.
- Valthrun-family reads: controller pawn link `m_hControllerPawn`, view via `C_CSPlayerPawn` + camera services; spread/recoil via weapon `m_flRecoilIndex` — server authoritative on fire, client-side ESP only = longevity tier 1 (`cheat-longevity-engineering`).

## Pair with
`hypervisor-memory-introspection`, `driver-comm`, `game-hacking`, `tpm-attestation-research`.
