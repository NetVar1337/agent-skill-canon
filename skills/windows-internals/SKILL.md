---
name: windows-internals
description: Use when tracing Windows process creation, tokens, handles, RPC, COM, ALPC, ETW, WPP, TraceLogging, memory, Object Manager, loader, kernel I/O, mitigations, symbols, or build-specific offsets. Builds a version-aware subsystem map and validates claims against the exact Windows build before debugging, driver work, or security research.
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

## 7. Subsystem evidence recipes

Each lab gets its own directory and starts with `platform.md` plus hashes for every user-mode binary, driver, and PDB used. Checkpoints are evidence boundaries, not elapsed-time milestones.

### Recipe A: process creation, token, and handle transaction

**Identity.** Record the OS build, parent/child hashes, `kernel32.dll`, `ntdll.dll`, and `ntoskrnl.exe`; in KD retain `vertarget` and `lmvm nt` output.

```powershell
$lab = 'C:\lab\proc-create'; New-Item -ItemType Directory -Force $lab | Out-Null
Get-ComputerInfo | Select WindowsVersion,OsBuildNumber,OsArchitecture | Out-File "$lab\platform.txt"
Get-FileHash C:\lab\bin\parent.exe,C:\lab\bin\child.exe,$env:SystemRoot\System32\ntdll.dll |
  Format-Table -Auto | Out-File "$lab\modules.txt"
procmon64.exe /AcceptEula /Quiet /Minimized /BackingFile "$lab\create.pml"
C:\lab\bin\parent.exe
procmon64.exe /Terminate
procmon64.exe /OpenLog "$lab\create.pml" /SaveAs "$lab\create.csv" /SaveApplyFilter
```

Save a Procmon filter for the parent/child PIDs and include Process Create, Process Start, Process Exit, file, and registry operations. At the process-create breakpoint, resolve the child in KD:

```text
vertarget
lmvm nt
!process 0 0 child.exe
!process <child_EPROCESS> 7
!token <child_TOKEN>
!handle 0 f <child_EPROCESS>
```

Checkpoints:

1. The PML establishes parent PID, command line, image path, and operation ordering.
2. `!process` and `!token` establish primary token, integrity, session, and protection state.
3. `!handle` proves which inherited or duplicated handle names, granted-access masks, and kernel objects the child can actually use.
4. A debugger or handle-close event proves final object lifetime and cleanup.

Use a `CreateProcessW` harness with `STARTUPINFOEXW` and `PROC_THREAD_ATTRIBUTE_HANDLE_LIST`. The negative control sets `bInheritHandles=FALSE`; the positive case explicitly lists one inheritable event handle. Do not infer inheritance from equal handle values. Close process/thread/event handles, stop Procmon if the harness fails, and archive the PML before reverting the snapshot.

### Recipe B: RPC to ALPC and server impersonation

**Identity.** Hash the client, server, `rpcrt4.dll`, proxy/stub DLL, and service image. Export from RpcView the interface UUID/version, PID, endpoint, protocol sequence, transfer syntax, and procedure number. For COM, also retain the CLSID/AppID registration and activation identity.

```powershell
$lab = 'C:\lab\rpc-alpc'; New-Item -ItemType Directory -Force $lab | Out-Null
Get-FileHash C:\lab\bin\client.exe,C:\lab\bin\server.exe,$env:SystemRoot\System32\rpcrt4.dll |
  Export-Csv "$lab\modules.csv" -NoTypeInformation
reg.exe query HKCR\CLSID\{CLSID} /s > "$lab\com-registration.txt"
python rpcdump.py @localhost > "$lab\endpoint-mapper.txt"
```

For `ncalrpc`, confirm the named port below `\RPC Control` in WinObj, then correlate it in KD or a server debugger:

```text
!object \RPC Control
!process 0 0 server.exe
.process /r /p <server_EPROCESS>
lmvm rpcrt4
bp rpcrt4!NdrServerCallAll
 g
!thread
!token <thread_ImpersonationToken>
!alpc /lpp <server_pid>
```

Checkpoints are client bind, ALPC connection/port identity, entry to the recovered opnum, `RpcImpersonateClient` or equivalent transition, effective thread token at the privileged sink, `RpcRevertToSelf`, and context-handle/rundown completion. Preserve requested authentication service/level and impersonation QoS; a service process token does not prove the dispatch thread's effective identity.

The negative control repeats the same opnum from a lower-integrity or unauthenticated client and must fail at the expected authorization check without reaching the sink. Close binding and context handles, cancel outstanding calls, revert impersonation, detach the debugger, and verify the server retains no client context. Route interface/NDR or COM activation depth to `windows-rpc-com-attack`.

### Recipe C: ETW provider, session, and loss accounting

**Identity.** Record the provider GUID/name, owning module hash/version, manifest or TraceLogging schema source, keyword/level mask, controller version, clock, and OS build.

```powershell
$lab = 'C:\lab\etw'; $guid = '{PROVIDER-GUID}'
New-Item -ItemType Directory -Force $lab | Out-Null
logman query providers > "$lab\providers.txt"
wevtutil.exe gp 'Provider-Name' /ge:true > "$lab\provider-manifest.txt"
wpr.exe -providers > "$lab\wpr-providers.txt"
logman create trace WI-Lab -p $guid 0xffffffffffffffff 0xff -bs 1024 -nb 16 128 `
  -o "$lab\provider.etl" -ets
C:\lab\bin\emit-known-event.exe
logman stop WI-Lab -ets
tracerpt "$lab\provider.etl" -of XML -o "$lab\events.xml" -summary "$lab\summary.txt"
```

For a separate kernel/user correlation run, preserve the profile name with the ETL:

```powershell
wpr.exe -start GeneralProfile -filemode
C:\lab\bin\emit-known-event.exe
wpr.exe -stop "$lab\general-profile.etl" 'WI correlation run'
```

Checkpoints are provider registration, enable callback state, one schema-validated sentinel event, the target transaction and activity ID, session stop/rundown, and ETL header/summary loss counters. Open the ETL in WPA to verify clock domain, per-CPU ordering, stacks when requested, and payload decoding.

For the loss control, replay the same bounded event corpus once with deliberately small buffers (`-bs 64 -nb 2 2`) and once with the normal allocation above. Compare emitted sequence numbers, consumed events, `EventsLost`, and buffer loss; never interpret absence until the healthy run captures the sentinel. The negative control disables the target keyword while retaining an independent enabled sentinel keyword, proving that filtering—not a dead provider or consumer—caused the expected absence.

Always run `logman stop WI-Lab -ets` and `logman delete WI-Lab` during cleanup; use `wpr -cancel` if a parallel WPR profile remains active. Preserve ETL, manifest/metadata, controller command line, summary, and consumer export. Route private-schema, WPP, TraceLogging, stack-walk, or buffer-engineering depth to `windows-telemetry-etw`.

Completion: all three recipes identify the tested build, record their checkpoints and negative control, and leave no live trace session, debugger attachment, impersonation token, binding, or inherited handle.

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

## Routing

- Batch A siblings: `bof-coff-development`, `windows-rpc-com-attack`, `windows-telemetry-etw`, and `hyper-v-offensive`.
- Batch B siblings: `linux-kernel-exploitation`, `c2-implant-engineering`, `ebpf-offensive`, and `linux-host-post-exploitation`.
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
