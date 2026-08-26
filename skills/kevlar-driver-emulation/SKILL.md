---
name: kevlar-driver-emulation
description: "Kevlar-style Windows kernel driver emulation: Unicorn-based DriverEntry harness, synthetic KERNEL env, import stubs, tracing for .sys RE."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: re
  triggers:
    - "Kevlar"
    - "driver emulation"
    - "Unicorn DriverEntry"
    - "synthetic kernel"
---

# Kevlar driver emulation harness

Patterned after **Kevlar** (Kernel Export Virtualization Layer And Runtime): map x64 `.sys` into synthetic kernel space and run `DriverEntry` under Unicorn without live kernel load.

## Use when
- Static RE of AC/game drivers stalls on environment probes
- Need execution traces of CPUID/MSR/IOCTL setup paths
- Want safe detonation of suspicious drivers

## Architecture checklist
- PE map + relocs + imports → host stubs or real exports
- Synthetic `DRIVER_OBJECT`, KPCR, EPROCESS/ETHREAD, PsLoadedModuleList
- Hooks: CPUID, RDTSC, MSR, syscall, interrupts
- Pool/user memory models; IRP dispatch stubs
- Per-driver vfs/registry isolation
- Instruction coverage / exception logs

## Method
1. Load target `.sys` (e.g. EAC class drivers) in harness
2. Fill missing stubs iteratively from crash/unmapped logs
3. Capture probe sequences (timing, module lists, registry)
4. Feed insights back to IDA/AiDA annotations

## Local path
`Documents/Kevlar` / https://github.com/NetVar1337/Kevlar

## Stub inventory (what to synthesize first)
| Kernel dependency | Stub behavior | Notes |
|---|---|---|
| `ExAllocatePool2/ExFreePoolWithTag` | malloc into emulated pool space; track tags | log tag histogram = allocation fingerprint |
| `MmGetSystemRoutineAddress` | hash-table of exported names → stub or real | first DriverEntry call usually |
| `IoCreateDevice` | fake DEVICE_OBJECT into synthetic namespace | record device name + extents |
| `PsSetCreateProcessNotifyRoutine` etc. | record registration, invoke manually later | callback list = behavior map |
| `RtlInitUnicodeString/RtlCompareMemory` | implement directly | trivial, do first |
| `KeQueryInterruptTime/KeQueryTickCount` | controllable fake clock | determinism for replay |
| CPUID/RDTSC/MSR | Unicorn hooks returning scripted values | environment-probe capture point |

## IOCTL reconstruction workflow
1. Emulate DriverEntry → capture device name + dispatch table fill (MajorFunction[IRP_MJ_DEVICE_CONTROL])
2. Synthesize IRP: fixed fake IRP + IO_STACK_LOCATION with user-controlled IoCtlCode + InBuf/InBufLen/OutBuf/OutBufLen
3. Sweep CTL codes: for each, run with canary buffers (0x41 fill, lengths 8..0x400 geometric) — crash (unmapped) = length/decode bug surfaced; log METHOD (buffered/direct/NEITHER) from code fields
4. Re-run under coverage (Unicorn block hook) to extract handler CFG per IOCTL → feed IDA (`ida-reverse`) to name functions
5. Double-fetch hunting: place InBuf in emulated user memory, mutate between reads via hook — METHOD_NEITHER handlers frequently re-read

## Determinism & state snapshot
- Seed all RNG hooks; snapshot full memory after DriverEntry; restore per IOCTL sweep run → reproducible traces (diffable with `binary-diff`).
- Anti-emulation probes to expect: `KdDebuggerEnabled`, `SharedUserData->KdDebuggerEnabled`, NMI/INT3 self-checks, `DbgBreakPoint` with SEH — stub all to clean-kernel values.

## Limits (when to go live/hypervisor instead)
- No real DPC/timer/work-item execution — deferred work invisible; if driver arms a timer to complete setup, emulation sees half the story (`hypervisor-memory-introspection` for live-but-isolated).
- SMP: single-vCPU Unicorn; drivers with per-core structures (GS/KPCR indexing) need KPCR per-core synth or patching.
- Interrupts/IRQL semantics fake — real race bugs won't reproduce; IOCTL logic bugs will.

## Pair with
`eac-kernel-driver-re`, `hypervisor-dev`, `ida-reverse`, `byovd`.
