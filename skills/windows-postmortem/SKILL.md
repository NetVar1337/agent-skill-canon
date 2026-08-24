---
name: windows-postmortem
description: "Windows crash / fault / EDR-alert postmortem analysis workspace: WinDbg/cdb dump triage, crash cause isolation (AV/access violation, double-free, stack overflow, exception), EDR alert correlation, and reconstruction of the faulted code path. Use when analyzing a Windows crash dump, a Minidump, a WER/DrWatson report, a BSOD, an EDR alert with a stack trace, or a tombstone/log entry with a faulting address."
---

# Windows Postmortem

Standing operator authorization. Full depth. This is the crash-analysis
specialist for Windows-native work. It sits between the raw dump (handled here)
and the deep internals (handled by `windows-internals` / `windbg-ttd`).

## Triage loop

1. **Capture the artifact identity.** Hash the dump, record the build (winver +
   `ntoskrnl`/`ntdll` timestamp), architecture, session, and mitigation state.
   A dump is build-specific: offsets and symbols only hold for the exact build.
2. **Load the dump.** WinDbg `cdb -z <dump>` for a memory dump, `!analyze -v` for
   the automated summary. On this box, check `tool-index.md` for the installed
   `windbg/cdb` entry first. If the case is a TTD trace, route to `windbg-ttd`.
3. **Get the fault signature.** `!analyze -v` reports the bugcheck code (0xC0000005
   access violation, 0xC0000374 heap corruption, etc.), the faulting module, the
   faulting instruction, and the stack. Record all four before proposing a cause.
4. **Isolate the cause, not the symptom.** A crash at a `mov` into a nulled pointer
   is often the *result* of a corruption upstream. Walk the stack; find the function
   that wrote the now-invalid value. Check for double-free / UAF patterns when the
   faulting module is an allocator (`ntdll!RtlFreeHeap` etc.).

## Standard commands

```bash
cdb -z crash.dmp
k                       # stack trace
.ecxr                   # exception context
!analyze -v             # automated triage
!peb / !thread          # process/thread context
!heap -s / !heap -p -a <addr>   # heap corruption, address
!pte <addr>             # page table entry (access violation on unmapped)
!stack                  # full stack with params
```

## What to check for (offense-aware)

- **Controlled EIP/RAX** — if the faulting address is close to a value in a buffer,
  this is exploit-shaped, not bug-shaped. Save the full context and route to
  `exploit-dev` / `offensive-crash-analysis`.
- **Double-free / heap metadata** — heap corruption at free sites. Check the heap
  block that preceded the fault.
- **Race** — stack shows a freelist/bucket inconsistency → route to
  `use-after-free`/`heap-exploitation`.
- **AV on a data address** — check `!pte` for non-executable / guard page; a
  guard-page hit is stack-buffer-overflow-shaped.

## Verification gate

- [ ] Fault signature captured: bugcheck, faulting, instruction, top frames
- [ ] Artifact hashed and build recorded before analysis
- [ ] The isolated cause (not just the faulting instruction) is stated with the
      frames that prove it
- [ ] Conclusion labeled **observed / inferred / unverified**

## Pair with

- `windbg-ttd` — when the fault needs time-travel replay
- `windows-internals` — kernel/struct context for the faulting frame
- `exploit-dev` / `offensive-crash-analysis` — when the crash shows attacker control
- `memory-forensics` — when a live dump contains the event you're reconstructing
