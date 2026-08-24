---
name: windows-driver-0day
description: Windows kernel-driver vulnerability discovery and reverse-engineering workflow for WDM and KMDF .sys files. Use when auditing an unknown driver, recovering device and IOCTL attack surfaces, reconstructing request schemas, fuzzing IOCTL handlers, triaging Driver Verifier crashes, performing cross-version variant analysis, or assessing whether a driver flaw is a new vulnerability.
compatibility: Windows analysis host; IDA Pro or Ghidra recommended. Dynamic work requires an isolated Windows VM with kernel debugging and snapshots.
---

# Windows Driver 0-Day Research

Use this skill as the coordinator for closed-source Windows driver vulnerability research. Load supporting skills only when their phase is reached:

- `ida-reverse`: binary loading, decompilation, types, xrefs, and data-flow work.
- `vuln-research`: general fuzzing, variant analysis, and crash minimization.
- `patch-diff-exploit` and `binary-diff`: cross-version and incomplete-fix hunting.
- `exploit-dev`: primitive construction after a vulnerability is confirmed.
- `code-audit`: use instead of binary-only analysis when source is available.

Do not begin with exploitation. First prove reachability, root cause, affected versions, and driver culpability.

## Required outputs

Maintain these artifacts in a case directory without modifying the original driver:

```text
case/
├── samples/                 # Read-only copies, INF/CAT, version metadata
├── static/
│   ├── surface.md           # Devices, ACLs, dispatch paths, IOCTL table
│   ├── ioctls.csv           # Code, method, access, schemas, handler, confidence
│   └── types.h              # Reconstructed request/object types
├── harness/                 # Reproducible user-mode test or fuzzer
├── corpus/                  # Valid seeds and minimized crashing inputs
├── crashes/<id>/            # Dump, debugger log, exact input, environment
└── report.md                # Root cause and affected-version evidence
```

For every conclusion, distinguish `observed`, `inferred`, and `unverified` facts.

## Phase 0: Preserve and fingerprint

1. Copy the `.sys`, INF, CAT, installer, and related DLLs into the case directory.
2. Record SHA-256, file version, signer, signature status, architecture, timestamp, and source package.
3. Record the analysis OS build, target VM build, virtualization settings, VBS/HVCI state, and symbol path.
4. Preserve at least one clean VM snapshot before loading the driver.
5. Search local notes and public vulnerability lists by hash, filename, signer, product, and version. A renamed known-vulnerable driver is not a 0-day.

Do not execute an unknown driver on the analysis host.

## Phase 1: Map the externally reachable surface

### Installation and access control

Recover from INF, registry setup, and code:

- Service name, start type, dependencies, filter class, altitude, and load conditions.
- Named devices, DOS symbolic links, device interfaces, control devices, and per-session devices.
- SDDL from INF `Security`, `IoCreateDeviceSecure`, registry values, or framework configuration.
- Whether a standard user, AppContainer, low-integrity process, service account, or administrator can obtain a handle.
- Required sharing flags, desired access, privileges, session, and device state.

Test access with the lowest-privileged token first. Record the exact `CreateFile`/`NtCreateFile` parameters and resulting status.

### WDM dispatch recovery

Start at the real entry point after compiler and security-cookie thunks. Recover:

- `DriverObject->MajorFunction[]`, especially `CREATE`, `CLOSE`, `CLEANUP`, `READ`, `WRITE`, `DEVICE_CONTROL`, `INTERNAL_DEVICE_CONTROL`, `PNP`, and `POWER`.
- `DriverUnload`, `FastIoDispatch`, completion routines, cancel routines, work items, DPCs, timers, callbacks, and lower-device forwarding.
- Dispatch trampolines that route by device extension, minor function, or IOCTL family.

### KMDF recovery

Find `WdfDriverCreate`, device/queue creation, and callback registration. Recover:

- `EvtDeviceAdd`, `EvtIoDeviceControl`, `EvtIoInternalDeviceControl`, read/write callbacks, file-object callbacks, queue stop/resume/cancel callbacks, and cleanup callbacks.
- Queue dispatch mode, synchronization scope, execution level, request ownership, and parent-child object lifetimes.
- WDF function-table calls using symbols or types matching the exact framework version. Do not label callbacks from guessed table indexes.

### Other ingress paths

Check whether the driver is also reachable through:

- Filtered IRPs from another device stack.
- File-system minifilter communication ports and callbacks.
- Network/WFP, NDIS, USB, Bluetooth, HID, storage, ACPI, or display class requests.
- Shared memory, sections, events, registry callbacks, ALPC, or companion services.
- Firmware, PCI configuration, MMIO, port I/O, or embedded command streams.

## Phase 2: Reconstruct every IOCTL contract

Decode each control code into device type, function, transfer method, and required access. Record aliases and ranges, not only switch constants.

| Method | Primary request locations | Review focus |
|---|---|---|
| `METHOD_BUFFERED` | `Irp->AssociatedIrp.SystemBuffer` | shared input/output buffer, returned length, partial initialization |
| `METHOD_IN_DIRECT` | input in `SystemBuffer`, MDL describes second buffer | direction assumptions, MDL length, writable mappings |
| `METHOD_OUT_DIRECT` | input in `SystemBuffer`, MDL describes second buffer | output bounds, mapping failures, information leaks |
| `METHOD_NEITHER` | `Type3InputBuffer` and `Irp->UserBuffer` | probing, capture, exception handling, double fetch, requestor mode |

For KMDF, map `WdfRequestRetrieve*Buffer`, unsafe user-buffer retrieval, memory objects, MDLs, and request completion lengths back to the same model.

For every IOCTL, reconstruct:

- Minimum and maximum input/output lengths.
- Fixed header, discriminant/opcode, flags, counts, offsets, variable tails, alignment, and nested records.
- Embedded pointers, handles, physical addresses, kernel addresses, callbacks, and user-provided function pointers.
- 32-bit/WOW64 layouts and pointer-width conversions.
- State prerequisites and cross-request object IDs, cookies, or handles.
- All validation branches and the point where data is captured into trusted storage.

A successful `ProbeForRead` or `ProbeForWrite` does not make later user-memory access safe. Look for mutation between check and use.

## Phase 3: Directed static vulnerability review

Trace untrusted fields to allocation, copy, mapping, arithmetic, indexing, indirect call, object lookup, and hardware-access sinks. Work backward from dangerous imports and forward from request buffers.

Prioritize:

1. `METHOD_NEITHER`, unsafe KMDF buffers, embedded pointers, and `RequestorMode` confusion.
2. Size arithmetic, narrowing conversions, signed comparisons, count-times-stride calculations, and output-length accounting.
3. `memcpy`/`memmove`/RTL copies, structure tails, array indexes, and offset-plus-length checks.
4. Async ownership: pending IRPs, cancellation, cleanup/close, work items, DPCs, timers, and completion routines.
5. Shared state accessed from parallel queues or multiple handles without a coherent lock/refcount protocol.
6. Object or handle lookup with wrong access mode, type, lifetime, or reference balancing.
7. `MmMapIoSpace`, `MmMapLockedPagesSpecifyCache`, MDLs, physical memory, MSRs, I/O ports, PCI config, arbitrary process attachment, and kernel virtual addresses.
8. Uninitialized pool/stack data returned through output buffers or completion lengths.
9. Device ACL, IOCTL access-bit, privilege, impersonation, and ownership mistakes.
10. PnP/power teardown races and stale device-extension or framework-object references.

Use [references/review-checklist.md](references/review-checklist.md) during this phase.

## Phase 4: Build a semantic harness

Start with a deterministic exerciser before mutation fuzzing.

1. Enumerate and open each reachable interface under both standard-user and administrator tokens.
2. Replay known-good application traffic when available; record request order and schemas.
3. Implement one typed builder per IOCTL family. Keep serialization separate from transport.
4. Log seed, iteration, device path, open flags, IOCTL, input/output sizes, bytes, thread schedule, status, returned length, and elapsed time.
5. Add a watchdog and preserve the last submitted test case outside the guest when possible.

Mutation dimensions:

- Zero, one-less, exact, one-more, page-boundary, signed-boundary, and maximum lengths.
- Inconsistent count/size/offset fields; integer-wrap combinations.
- Null, low, noncanonical, kernel-range, guard-page, read-only, freed, aliased, and cross-page pointers.
- Misalignment and 32/64-bit layout confusion.
- Input/output aliasing and overlapping nested buffers.
- Valid request sequences with repeated create/use/free/close operations.
- Parallel IOCTL, cleanup, close, cancellation, and handle duplication.
- PnP/power transitions only in a disposable VM with a recoverable device setup.

Preserve enough valid structure to reach deep code. Pure random IOCTL spraying usually measures the rejection path.

## Phase 5: Dynamic verification

Use a disposable VM with kernel debugging and a targeted Driver Verifier configuration. Record the exact verifier settings. Prefer the smallest set that exposes the suspected class; broad settings can change timing and obscure causality.

Useful debugger evidence includes:

```text
lmvm <driver>
!drvobj <driver-object> 7
!devobj <device-object>
!irp <irp>
!analyze -v
.exr -1
.cxr <context>
kv
!pool <address>
!verifier
!locks
!deadlock
```

Set breakpoints on the recovered dispatch/callback and suspected sink. Confirm:

- The harness reaches the intended branch.
- Register and memory values match the reconstructed schema.
- The crashing access derives from controlled input or a controlled lifetime transition.
- The target driver, rather than Verifier, another filter, or malformed harness state, owns the root cause.

Repeat without unrelated instrumentation when timing permits. Keep the dump and complete debugger transcript.

## Phase 6: Crash triage and minimization

For each crash:

1. Hash the tuple of bugcheck/exception, faulting instruction, top target-driver frames, allocation/free stack, and semantic operation.
2. Reproduce from the clean snapshot at least three times.
3. Minimize bytes, lengths, request sequence, concurrency, and verifier flags independently.
4. Identify the first invalid state transition or unsafe operation, not merely the final fault.
5. Determine whether the condition is memory corruption, disclosure, race, authorization bypass, or intended privileged functionality.
6. Test adjacent driver and OS versions with the same minimized reproducer.

Do not call a hang a deadlock without lock/wait evidence. Do not call a Verifier stop exploitable without proving the underlying invariant violation.

## Phase 7: Variant and novelty analysis

Use `patch-diff-exploit` when fixed and vulnerable versions exist; use `binary-diff` to migrate names and recovered types.

Search for:

- Sibling IOCTLs missing the newly added validation.
- Checks applied before one dereference but not repeated after a user-memory re-read.
- Fixes limited to one architecture, product SKU, queue, or compatibility handler.
- Equivalent helpers in related vendor drivers.
- Error paths that bypass the fixed reference, lock, or cleanup protocol.

Novelty requires evidence. Compare hashes, versions, advisories, CVEs, vendor release notes, driver blocklists, and known-vulnerable-driver datasets. Label the result `candidate 0-day` until that search is complete.

## Phase 8: Exploitability handoff

After confirming root cause, document:

- Attacker prerequisites and lowest token that reaches the path.
- Controlled bytes, size, offset, timing, target object, and repeatability.
- Read/write/disclosure/lifetime primitive potential.
- Pool type, allocation size/tag, allocation/free sites, IRQL, processor context, and reclaim constraints.
- Relevant mitigations: KASLR, SMEP/SMAP, CFG/KCFG, CET, VBS/HVCI, pool hardening, and driver blocklist state.

Then load `exploit-dev`. Keep the minimal crash PoC separate from later exploit work.

## Completion gate

Do not report a finding as confirmed until all applicable items pass:

- [ ] Reachable from the stated attacker context.
- [ ] Exact dispatch and data-flow path recovered.
- [ ] Minimal reproducer works from a clean snapshot.
- [ ] Root cause occurs in the target driver.
- [ ] Driver Verifier and debugger evidence preserved.
- [ ] Affected and unaffected versions tested where available.
- [ ] Known-vulnerability and duplicate search completed.
- [ ] Impact claims match the demonstrated primitive.
- [ ] Driver hash, OS build, symbols, verifier settings, and harness input recorded.
