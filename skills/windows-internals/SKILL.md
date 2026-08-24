---
name: windows-internals
description: Use when analyzing Windows-native process, memory, object-manager, loader, security-token, ETW, IPC, kernel, or mitigation behavior. Builds a version-aware subsystem map and validates claims against the exact Windows build before offset recovery, debugging, driver work, or security research.
license: MIT
---
# Windows internals research

Use this skill to understand a Windows-native mechanism. It is the coordinator for internals evidence; route a confirmed driver vulnerability to `windows-driver-0day`, a privileged-workflow hypothesis to `windows-0day-hunting`, a binary to `ida-reverse` or `ghidra-reverse`, and a known primitive to `exploit-dev`.

## Completion standard

Before asserting a Windows-internals conclusion, preserve the exact build and module identity, explain the relevant user/kernel or trust-boundary path, and label evidence as **observed**, **inferred**, or **unverified**. Treat layouts, symbols, offsets, and mitigations as build-specific.

## 1. Establish the platform truth

Record these before interpretation or mutation:

```powershell
Get-ComputerInfo | Select-Object WindowsProductName, WindowsVersion, OsBuildNumber, OsArchitecture
Get-CimInstance Win32_OperatingSystem | Select-Object Version, BuildNumber, LastBootUpTime
Get-CimInstance Win32_DeviceGuard | Format-List *
Get-ProcessMitigation -System
```

For each target module or driver, record its path, SHA-256, file/product version, PE timestamp, signer/signature status, architecture, loaded base, and symbol source. For a live process record PID, parent, token user/groups/integrity/privileges, session, WOW64 state, protection level, and loaded module list.

Completion: a second researcher can select the same symbols and recreate the relevant mitigation state.

## 2. Select the subsystem and boundary

| Subsystem | Core objects and questions | Primary evidence |
|---|---|---|
| Process/thread | EPROCESS, ETHREAD, KTHREAD, PEB, TEB, APC state, handles | WinDbg, Process Explorer, PEB/TEB inspection |
| Virtual memory | VADs, PTEs, sections, working sets, copy-on-write, MDLs | `!vad`, `!pte`, `!address`, VMMap, ETW |
| Object Manager | directories, symbolic links, object types, handles, security descriptors | WinObj, `!object`, `!handle`, Nt object paths |
| Security | token, SID, integrity, privileges, AppContainer, PPL, CI, VBS/HVCI | token dump, `!token`, Code Integrity/Defender logs |
| Loader | PEB.Ldr, Ldrp, KnownDlls, ApiSet, SxS, delay load, TLS | loader snaps, ETW loader provider, debugger |
| IPC | ALPC, RPC, COM, named pipes, shared sections, win32k | ETW, RpcView, Procmon, debugger |
| Telemetry | ETW providers, ETW-TI, AMSI, callbacks, minifilters | `logman`, WPR/WPA, provider manifests, static RE |
| Kernel scheduling/I/O | IRPs, device stacks, WDF queues, DPCs, callbacks | WinDbg, Driver Verifier, WPP/ETW, driver RE |

State the trust transition in one sentence: **weaker actor/control → Windows component → stronger identity or security-sensitive sink**. If no boundary matters, write the functional question instead.

Completion: one subsystem and one concrete question are selected; adjacent subsystems are listed only when a trace crosses into them.

## 3. Build the path, not a field list

Trace one valid transaction end-to-end before exploring mutations:

```text
caller token/process
  -> Win32/COM/RPC/API entry
  -> ntdll syscall or broker transition
  -> kernel/service/driver dispatch
  -> object, memory, file, registry, or security sink
  -> observable result and cleanup
```

At each transition capture:

- process/thread, effective or impersonation token, session, and IRQL when relevant;
- Win32, NT, device, and Object Manager names where a resource is resolved;
- whether a validated handle persists or the resource is reopened by name;
- requested access, sharing, security descriptor, and final access check;
- synchronization and lifetime ownership across async work, APCs, IRPs, callbacks, or worker queues.

Use Procmon for the transaction outline, ETW/WPR for timing and provider data, WinDbg for object/lifetime proof, and static RE/PDBs for hidden checks. No single source establishes all four.

Completion: the first interpretation of relevant input and the final sensitive operation are both evidenced.

## 4. Resolve symbols, types, and offsets correctly

1. Prefer Microsoft public symbols and module-matched PDBs. Record exact paths, GUID/age, and failures.
2. Apply public types only when their build/module provenance matches. Mark inferred private fields and verify with independent references or live observations.
3. For structure recovery, triangulate PDBs, compiler access patterns, object dumps, and version-matched binaries. Never promote a pattern-scan match without uniqueness and runtime/static validation.
4. Report every offset as `module + build/hash + RVA/field + validation method + confidence`; never as a bare constant.
5. For WOW64, explicitly separate 32-bit and 64-bit PEB/TEB, pointer widths, syscall paths, and thunk layers.

Completion: any layout used in an experiment has provenance and a target-build validation note.

## 5. Apply subsystem-specific invariants

- **Object identity:** the object authorized is the object used at the sink; name re-resolution and namespace changes do not substitute it.
- **Token continuity:** access checks and final side effects run under the intended primary or impersonation token.
- **Lifetime ownership:** references, handles, IRPs, MDLs, APCs, and callbacks remain valid until final use and are released exactly once.
- **Memory mapping:** user/kernel addressability, cache type, page protections, and length remain valid across capture, lock, map, and copy.
- **Loader trust:** module path, section, signature, KnownDlls/API-set resolution, and search order stay within the intended policy.
- **Telemetry interpretation:** provider enablement, event loss, consumer filters, and timestamps are known before a missing event becomes a conclusion.
- **Mitigation scope:** CFG/CET, KASLR, VBS/HVCI, PPL, CI, PatchGuard, and sandbox policy are measured on the tested build rather than assumed from marketing names.

Completion: each experiment names the invariant it tests and the observation that would disprove it.

## 6. Use controlled experiments

- Start from a snapshot or reversible state. Log commands, timing, inputs, and cleanup.
- Change one variable per run: token, session, namespace, object name, handle reuse, module build, mitigation, race timing, or request layout.
- Use debugger events, ETW events, oplocks, handle state, or completion objects instead of sleeps when synchronization matters.
- Maintain a negative control that preserves the expected invariant. If it produces the same result, the signal is not diagnostic.
- Separate a functional anomaly, crash, controllable primitive, and demonstrated impact.

Completion: the result is reproducible and the negative control is explained.

## Offense quick-map (build-pinned, verify before use)

Methodology above governs proof; this map speeds recall. Every entry is
build-specific — derive per target build via public PDBs or signatures, never
hardcode one build's value and ship it.

- **Process/thread**: `EPROCESS` { ImageFileName, ActiveProcessLinks, Token,
  VadRoot, ObjectTable, Flags2/Protection (PP) }; `PEB` { Ldr, BeingDebugged,
  ProcessParameters }; `TEB` { Tib, Win32ThreadInfo, PEB pointer at gs:[0x60] }.
- **Memory**: VAD tree (per-process ranges, `!vad`), PTE/working set, sections;
  external-read stacks walk PML4 via physical translation (see
  `hypervisor-memory-introspection`, `valthrun-style-stack`).
- **Objects**: OBJECT_HEADER Type offset, handle tables, `ObRegisterCallbacks`
  lists — enumeration and unlink craft in `kernel-callbacks`.
- **Security**: `_TOKEN` privileges/SIDs, integrity levels, PPL (protects
  lsass et al.), CI/DSE gates (`CiValidateImageHeader`), HVCI/VBS — see
  `vbs-hvci-research` for VTL1 boundaries.
- **Telemetry**: ETW providers/controllers/consumers, ETW Threat-Intelligence
  (EtwTi) syscall/alloc logging, AMSI — patch points and counter-forensics in
  `edr-bypass-re`.
- **Loader**: `LdrLoadDll` path, KnownDlls, API-set schema resolution (v2+
  dynamic), IAT mechanics (`pe-tools`).
- **Syscalls**: x64 SSDT hooking is dead (KASLR+PG); modern surface = direct/
  indirect syscall stubs, instrumentation-callback hijack, ETW-TI interplay.
- **IPC**: ALPC ports (`\RPC Control` mapping to RPC), RPC runtime, named
  pipes — thick-client and lateral angles in `thick-client`/`offensive-*`.
- **Debugging reality on this box**: classic WinDbg suite at
  `C:\Program Files (x86)\Windows Kits\10\Debuggers\x64\` — kernel/debugger
  workflows in `windbg-ttd`.

## Evidence outputs

Keep a compact case record containing:

```text
platform.md       build, mitigations, symbols, tool versions
modules.csv       path, hash, version, base/RVA, signer
trace.md          transaction timeline and token/namespace transitions
layouts.md        fields/offsets, provenance, validation, confidence
hypotheses.md     invariant, experiment, control, result, status
artifacts/        ETL, PML, dumps, debugger logs, scripts, minimized inputs
```

## Pairings

- Native binary, driver, or crash: `ida-reverse`, `ghidra-reverse`, `windows-driver-0day`, `pwndbg-dynamic-analysis` as appropriate.
- Privileged Windows workflow: `windows-0day-hunting`, `windows-privileged-file-workflows`, `windows-object-manager-confusion`, `windows-profile-hive-research`, or `windows-recovery-state-research`.
- Kernel development or virtualization: `kernel-dev`, `hypervisor-dev`, `kevlar-driver-emulation`, `stealth-hypervisor`.
- Dynamic instrumentation of usermode mechanisms: `frida-dbi`.
- Debugger-driven verification (KDNET, dumps, TTD): `windbg-ttd`.
- VBS/HVCI/VTL1 boundaries: `vbs-hvci-research`; boot-trust chain: `secure-boot-uefi-research`.
- Detection/telemetry work: `threat-hunting`, `malware-analysis`, `edr-bypass-re`.

## Final gate

- [ ] Exact OS/module build, architecture, mitigation state, and symbol provenance recorded.
- [ ] Relevant transaction traced across its actual trust boundary.
- [ ] Layouts and offsets tied to this build and independently validated.
- [ ] Observed, inferred, and unverified claims separated.
- [ ] Negative control and cleanup are preserved.
- [ ] Follow-on skill selected only after the internals question is resolved.
