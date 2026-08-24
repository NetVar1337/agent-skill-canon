---
name: kernel-callbacks
description: "Windows kernel callback tradecraft: enumerate Ps*/Ob/Cm callback registrations and minifilters, attribute each to its owning module, then choose unlink / proxy-filter / EPT-hide; PatchGuard-vs-HVCI implications, races, and EDR/anti-cheat re-validation countermeasures. Use when attacking or auditing kernel notification mechanisms."
version: 2.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: stealth
---

# Kernel callbacks

Callbacks are how EDRs and anti-cheats observe process/thread/image/handle
activity. Every technique below starts from the same loop: **enumerate →
attribute → decide → verify nothing regressed.**

## Target inventory

| Callback | Registration API | Storage (ntos / fltmgr) | Notes |
|---|---|---|---|
| Process-create (Ex) | `PsSetCreateProcessNotifyRoutineEx` | `PspCreateProcessNotifyRoutine[]` + count | veto-capable via `Creation.Status` |
| Process-create (Ex2, container) | `PsSetCreateProcessNotifyRoutineEx2` | separate array | rarely used |
| Thread-create | `PsSetCreateThreadNotifyRoutine` | `PspCreateThreadNotifyRoutine[]` | runs with thread lock held — keep tiny |
| Image-load | `PsSetLoadImageNotifyRoutine` | `PspLoadImageNotifyRoutine[]` | info-only |
| Object pre/post | `ObRegisterCallbacks` | `OBJECT_TYPE` callback lists (process/thread/desktop) | pre-op can strip access rights (e.g. `PROCESS_VM_READ`) |
| Registry | `CmRegisterCallbackEx` | Cm callback list + `LARGE_INTEGER` cookie | `REG_XXX` class in Altitude space |
| Filesystem | `FltRegisterFilter` | fltmgr filter/instance/frame lists | altitude-ordered |
| Power/PCB, Debug | `PoRegisterPowerStateCallback`, `Dbgk*` | misc | niche |

## Enumeration

Entries are `EX_CALLBACK_ROUTINE_BLOCK`-style `{ refs, Function, Context }`
pairs (fast-ref wrapped on modern builds). Walk:

1. Resolve arrays by signature (symbols for `nt!PspCreateProcessNotifyRoutine`
   are public in many builds; otherwise pattern-scan the
   `PsSet...Routine` API body for the array operand).
2. For each non-null slot: read `Function`.
3. Attribute owner: iterate `ZwQuerySystemInformation(SystemModuleInformation)`
   ranges and find the module containing `Function`
   (`RtlPcToFileHeader` works on kernel pointers too); record module name,
   path, signer.
4. Minifilters: walk fltmgr's filter list from `FltGetFilterInformation`-backed
   globals by signature, or usermode `fltmc filters` for the sanctioned view —
   diff the two views; a filter visible in one and not the other is itself a
   finding.

Emit a table: `slot, function RVA, module, signer, type`. Diff against a clean
VM snapshot of the same build to isolate EDR/AC additions.

## Decision matrix (attack side)

| Goal | Technique | Constraints |
|---|---|---|
| Blind the observer | Unlink: zero the array slot / unlink list entry | data write — survives HVCI; race with in-flight callback invocation; counters must be consistent |
| Keep logging, alter facts | Proxy: replace entry with your shim that calls (or conditionally skips) the original | must preserve calling convention + IRQL; original function pointer captured first |
| Hide only from victim reads | EPT split-view on the array's page for the reading core | needs `hypervisor-dev`/`stealth-hypervisor`; arrays are shared data — per-core view switching cost |
| Registry/fs specific | Re-register order games, or strip at the source (e.g. remove `PROCESS_VM_READ` is done *by* Ob pre-ops — attack the pre-op, not the list) | — |
| You own the callback | Just unregister via the documented API | trivial, detection: EDR re-registers and diffs |

### Unlink mechanics and races

- The arrays are walked under a lock/ATOMIC ops by the dispatcher
  (`KeEnterCriticalRegion`-style). Zeroing a slot while the dispatcher holds a
  reference to the block is the race: safest order is (a) swap slot → NULL with
  an interlocked exchange, (b) keep the stolen block alive (don't free) so any
  in-flight call lands on valid code, (c) leave count/epoch fields untouched
  unless you also own re-registration logic.
- Ob callback lists live inside `OBJECT_TYPE` (per-type); unlinking means
  list surgery under the type's lock — same in-flight rule: never free what a
  core may be executing.
- Minifilter removal: unlink from fltmgr's filter list **and** teardown paths
  (frames/instances reference filters); incomplete unlinks BSOD on next I/O.

### PatchGuard vs HVCI (what actually applies)

- These arrays/lists are **kernel data**, not code: PatchGuard's classic
  coverage (SSDT/IDT/GDT/MSRs, certain structures) does not directly guard
  them — historically plain DKOM on notify arrays persists. Do not extend that
  assumption to anything you *patch in code*.
- HVCI/KCFG: still fine — you write data, and any pointer you install must
  point at valid executable kernel memory (a manually mapped driver's pages
  are fine for the CPU; policy visibility is a separate question — see
  `vbs-hvci-research`).
- Anti-cheat angle: EAC/BE/vanguard-style drivers register several of these and
  re-validate (re-enumerate, checksum their own registrations) periodically —
  measure the re-registration interval before choosing unlink vs proxy.

## Proxy filter sketch

```c
// captured: originalProcessNotify (PsSetCreateProcessNotifyRoutineEx signature)
VOID ProxyProcessNotify(PEPROCESS proc, HANDLE pid, PPS_CREATE_NOTIFY_INFO info) {
    if (ShouldHide(pid)) return;              // swallow
    originalProcessNotify(proc, pid, info);   // pass-through preserves EDR view
}
```

Pass-through proxies survive re-registration diffing (the EDR's pointer is
intact) — but your shim must be reachable from the array, so the array now
points at your code: integrity scans that hash *their own* function pointer
see it unchanged; scans that hash the array see yours. Know which one the
opponent runs before betting on either.

## Defense/blue verification

- Re-enumerate from a driver of your own; compare with `fltmc`, Process
  Explorer's sys internals view, and a clean-snapshot diff.
- Canary registration: register your own callback, watch for it being skipped
  (proxy detection: feed events only you can generate, confirm your slot's
  function pointer still equals your registration address).
- Alert on: array slot pointing into an unsigned module's range, unknown
  module attribution, count/epoch regressions.

## Test plan

1. Lab VM, target build pinned, symbols loaded (`windbg-ttd` for setup).
2. Baseline table → apply technique → re-enumerate → run victim workload
   (spawn/kill, image loads, cross-process OpenProcess) → confirm observer
   behavior changed as intended → soak overnight for BSOD/races.
3. Driver Verifier on the target's driver where possible; KDNET attached so a
   race is diagnosable, not just a bugcheck code.

## Pair with

`kernel-dev` (write primitives, mapping your own code), `windows-internals`
(offset provenance), `stealth-hypervisor` (split-view option), `edr-bypass-re`
(opponent modeling), `windbg-ttd` (verification).
