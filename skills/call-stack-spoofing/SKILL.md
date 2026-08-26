---
name: call-stack-spoofing
description: "Usermode call-stack spoofing primitives: synthetic frames, gadget restore, JMP vs CALL semantics, ETW-friendly stacks."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: stealth
  triggers:
    - "stack spoofing"
    - "fake call stack"
    - "RtlCaptureStackBackTrace spoof"
---

# Call-stack spoofing primitives

Companion to `anti-cheat-stack-walk-stealth` focused on **mechanism**.

## Building blocks
- **Spoof JMP**: transfer without pushing real ret; walker sees crafted frames only
- **Restore gadget**: pops saved real RSP/RBX and resumes
- **Desync FP vs RSP walks**: some walkers trust RBP chain — keep both consistent when possible
- **Veh/instrumentation** paths: don’t leave VEH frames that advertise private code

## Validation
- Compare `CaptureStackBackTrace` output before/after
- Kernel: optional driver logging of user stack on syscall
- Game+AC stress: inventory, shoot, inventory loops

## Canonical x64 spoof chain construction
```
; entry shim (before sensitive call)
; save real RSP/RBX -> globals
mov  [real_rsp], rsp
mov  [real_rbx], rbx
; build fake stack below real stack (red-zone safe: use deeper stack)
; Frame 1: return addr = gadget1 (pop rsp; ret or restore stub)
; Frame 2..N: return addrs inside legit modules (kernel32!BaseThreadInitThunk,
;              ntdll!RtlUserThreadStart, ntdll!RtlpStartThread)
lea  rsp, [fake_frames]
jmp  sensitive_api        ; JMP not CALL: no real ret pushed

; restore gadget (reached when sensitive API rets into Frame1 addr)
mov  rbx, [real_rbx]
mov  rsp, [real_rsp]
ret                       ; back to caller as if nothing happened
```
- Frame addresses must be chosen from a **consistent fake call graph**: RtlUserThreadStart → BaseThreadInitThunk → your thread-proc-looking symbol. Random legit-looking addresses fail semantic checks (walkers verify module+offset ranges are plausible start chains).

## Rules for unwind-correct stacks
- `RtlVirtualUnwind` (used by ETW stack walks and crash handlers) follows RUNTIME_FUNCTION pdata; fake return addresses inside arbitrary mid-function bytes cause walk aborts → suspicious.
- Prefer fake frames landing on function-start addresses with proper unwind info, or accept partial walks (many EDR collectors cap at 32 frames and tolerate truncation — verify per collector).
- Never spoof below current RSP: only replace the chain your sensitive call would push.

## Syscall-path spoofing (direct syscall era)
- Direct syscall stubs (`syscall; ret`) produce 1-frame stacks — flagged. Wrap: build fake chain, JMP to stub, restore-gadget as ret-addr; or use indirect syscall (syscall instruction inside ntdll — gadget-hunt a `syscall; ret` inside ntdll and call it) so at least the return address is ntdll-canonical.
- `RtlUserThreadStart` frame presence is the most commonly checked heuristic — always include it as the terminal fake frame for thread-originating calls.

## Detection arms race notes
- Some ACs hash the whole used-stack region for anomalies (uninitialized patterns); pre-fill fake region with realistic locals (zeros + pointers mix), not 0xCC/0x90 sleds.
- FP walkers (RBP-chain) are nearly extinct on x64; MS x64 uses pdata — but games' own crash reporters may use RBP chains: keep RBP chain coherent too when feasible.
- Validate by capturing real telemetry: procmon WER + `wpr -start GeneralProfile` stackwalk events before/after; plus in-game stress loops (inventory/shoot/UI) to catch edge paths.

## Pair with
`anti-cheat-stack-walk-stealth`, `lang-assembly`, `stealth-injectors`.
