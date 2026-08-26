---
name: stealth-injectors
description: "Stealthy usermode/kernel injection: manual map, thread hijack, APC, module stomping, hollow, mapper design, artifact hygiene."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: stealth
  triggers:
    - "manual map"
    - "thread hijack"
    - "APC injection"
    - "module stomping"
    - "stealth injector"
---

# Stealthy injectors

## Goals
Execute foreign code in a target process/kernel with minimal artifacts versus EDR/AC.

## Usermode technique ladder
1. **CreateRemoteThread + LoadLibrary** — noisy baseline (know it to avoid)
2. **NtCreateThreadEx** + manual map
3. **Thread hijack** (suspend/context/RIP swap/resume)
4. **QueueUserAPC / special user APC** on alertable threads
5. **Module stomping / module overloading** (execute in legit module RX)
6. **Transacted/ghost / dual-mapping** variants
7. **Process hollowing / doppelgänging / herpaderping** (know detection cost)
8. **SetWindowsHookEx** (only if UI-thread delivery fits)
9. **Instrumentation callback / VEH** abuse for redirect

## Manual map checklist
- Map sections with correct protections; apply relocs; resolve imports (including API sets)
- TLS callbacks decision; exception directory; cookie
- Erase PE headers optional; fix or avoid module list visibility intentionally
- Prefer RW→RX transitions; avoid long RWX

## Kernel injection ladder
- APC to user thread from kernel
- Attach process + write + context
- Thread creation in target via NtCreateThreadEx from kernel
- Shared sections + user trigger

## OPSEC
- Call stacks, allocation stubs, RWX, cross-process handle rights, abnormal module ranges
- Clean handles; avoid known bad patterns in public GH gists
- Test under target AC/EDR with telemetry capture

## Technique deep mechanics

### Thread hijack (the workhorse)
```
1. OpenThread(target main or worker with known alertable state)
2. SuspendThread -> GetThreadContext
3. Write shellcode stub (loadlib-style or map-call) to executable scratch
4. RIP = stub; stack aligned (0x28 shadow + 16-align per MS x64 ABI)
5. ResumeThread -> wait for completion flag -> restore original RIP/CONTEXT -> resume
```
- Detection: CONTEXT manipulation flagged by some EDRs via thread-start telemetry; use a legitimately alertable thread and restore RSP exactly.

### Special user APC (Win10 20H1+)
- `NtQueueApcThreadEx(th, …, QUEUE_USER_APC_FLAGS_SPECIAL_USER_APC)` — runs regardless of alertable state, on next thread scheduling; kernel-mode counterpart `KeInitializeApc` with `UserApcRoutine` inside ntdll's `RtlUserApcTrampoline`-adjacent region.
- Pair with mapping stub that frees itself; APC routine address must be valid at run time.

### Module stomping detail
1. Load (or find already-loaded) large legit signed module with RX padding (common: system DLLs with >4KB slack at end of .text)
2. memcpy shellcode or tiny PE into slack; optional: use `NtCreateSection` dual-map of a data file onto an RX section of a signed module (module overloading proper)
3. Execute via hijacked thread; restore original bytes after done if transient
- Wins: allocation is a legit signed image; memory scan sees signed entropy; no RWX.
- Losses: MZ/PE headers of stomped region if sloppy; code-integrity scans of system DLLs catch byte drift.

### Manual map CFG + exception correctness
- Register `UNWIND_INFO` + functions via `RtlAddFunctionTable` — or SEH/VEH and C++ exceptions crash inside your image on first throw.
- Control Flow Guard: indirect calls into your mapped image get terminated unless (a) `SetProcessValidCallTargets` adds your ranges, (b) you patch CFG bitmap directly (find via `NtGetCurrentProcess`->`ProcessDynamicCodePolicy`… actually bitmap via `LdrSystemDllInit` internals — brittle), or (c) your code only uses direct calls internally and exported thunks via legitimate addresses. Simplest: compile with `/guard:cf-` and avoid indirect calls out.
- TLS: if image has TLS callbacks, allocate TLS index via `TlsAlloc`, fill `ThreadLocalStoragePointer`, run callbacks with DLL_THREAD_ATTACH on each existing thread you care about.

### Kernel-side injection mechanics
- `KeStackAttachProcess(targetEp, &apcState)` -> `MmCopyVirtualMemory` (pass your driver EPROCESS as from-proc) -> detach. Works for data + stub writes.
- APC from kernel: allocate user-mode stub in target (ZwAllocateVirtualMemory with previous-mode tricks or attach+MmCopy), `KeInitializeApc(apc, thread, OriginalApc, …, UserApcRoutine=stub, UserApcContext=dllpath)`, `KeInsertQueueApc`. Choose `NormalRoutine`-null kernel APC for pure-kernel payloads.
- Create remote thread from kernel: `PsCreateSystemThread` then set `Thread->CrossThreadFlags`/process — brittle across builds; prefer APC or attach+hijack of existing thread via `KeSuspendThread`-equivalents (undocumented, pin per build — `windows-internals`).

## Detection mapping (what catches what)
| Technique | Primary detector | Hardening |
|---|---|---|
| CreateRemoteThread+LoadLibrary | everything, since 2010 | never ship |
| Manual map + NtCreateThreadEx | ETW-TI thread-create telemetry; start-address heuristics | indirect syscalls, spoof start address via thread-parameter gadget (`call-stack-spoofing`) |
| RWX allocations | memory scanners, ETW map-view | RW→RX transitions only; consider stomping |
| Module stomping | integrity scans of signed modules | restore bytes; pick cold modules |
| Hollowing/doppelgänging | image-load callbacks mismatch, prefetch artifacts | transacted-file variants leave USN journal — wipe |
| Kernel APC | PatchGuard-adjacent, handle-strip telemetry | hide thread object? no — target legit alertable threads |

## Test discipline
- Detonate under target AC with procmon+ETW capture first on a sacrificial account.
- YARA-scan your own artifacts (`yara` on E:\Tools) before shipping; public-gist shellcode is signatured within weeks.
- Verify on both 22H2/24H2+ builds: special-APC semantics and CFG defaults differ.

## Pair with
`kernel-dev`, `game-hacking`, `windows-internals`, `hypervisor-dev`.
