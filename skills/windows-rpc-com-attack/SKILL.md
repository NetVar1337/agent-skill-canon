---
name: windows-rpc-com-attack
description: Use when enumerating, reverse engineering, testing, or fuzzing Windows RPC endpoints, DCOM or COM activation, ALPC ports, NDR stubs, named-pipe or TCP bindings, impersonation boundaries, elevation monikers, COM hijacks, service brokers, RpcView data, OleViewDotNet findings, or MIDL-derived clients.
license: MIT
---
# Windows RPC, COM, DCOM, and ALPC attack-surface research

## When to use

Use this skill when the target boundary is implemented by Microsoft RPC, COM/DCOM activation, an ALPC transport, an RPC-over-named-pipe/TCP endpoint, or a broker exposing a MIDL-described interface.
Use it to turn endpoint inventory or static stubs into a typed, authenticated, replayable client and to prove which token reaches a privileged sink.
Use it for interface discovery, NDR32/NDR64 recovery, context-handle lifetime, COM registration/activation analysis, per-user class precedence, elevation monikers, surrogate boundaries, and deterministic mutation harnesses.
Do not call a writable registry entry, unauthenticated endpoint, or impersonating thread a vulnerability until a weaker principal can cause a stronger principal to perform a security-sensitive action.
A production result contains build and module identity, endpoint/interface identity, recovered type grammar, binding/security settings, a valid control call, a negative authorization control, mutation evidence, and cleanup.

## Completion standard

Record the Windows build, architecture, target process/service, binary SHA-256 and version, PDB GUID/age, interface UUID/version, transfer syntax, protocol sequence, endpoint, object UUID when present, authentication service/level, impersonation policy, and client/server tokens.
Trace one valid call from client argument bytes through unmarshalling and dispatch to the final file, registry, process, token, handle, device, or network sink.
Label recovered fields and offsets as symbol-derived, MIDL-format-derived, decompiler-inferred, dynamically observed, or still unverified.
Reconstruct with a typed client first; mutate logical fields only after the baseline call, cancellation path, and context teardown are reliable.
Run all active tests in an isolated lab whose endpoint, service, and network scope are explicitly bounded.

## Core workflow

### 1. Pin host, service, and process identity

Create a case record before endpoint enumeration:

```powershell
$Case = 'C:\lab\rpc\case-001'
New-Item -ItemType Directory -Force "$Case\evidence", "$Case\artifacts" | Out-Null
Get-ComputerInfo | Select-Object WindowsProductName,WindowsVersion,OsBuildNumber,OsArchitecture | Format-List | Out-File "$Case\evidence\platform.txt"
Get-CimInstance Win32_OperatingSystem | Select-Object Version,BuildNumber,LastBootUpTime | Format-List | Add-Content "$Case\evidence\platform.txt"
Get-CimInstance Win32_Service | Sort-Object Name | Select-Object Name,State,StartName,ProcessId,PathName | Export-Csv "$Case\evidence\services.csv" -NoTypeInformation
Get-Process -IncludeUserName | Select-Object Id,ProcessName,UserName,Path | Export-Csv "$Case\evidence\processes.csv" -NoTypeInformation
```

For each suspected server, preserve executable path, loaded module path, SHA-256, file/product version, signature, service SID type, required privileges, session, integrity, PPL state, and command line.
Use `sc.exe qc ServiceName`, `sc.exe qsidtype ServiceName`, and `sc.exe qprivs ServiceName` for service policy rather than inferring it from a friendly name.
Capture client identity with `whoami /all`, Process Explorer token properties, or WinDbg `!token`; repeat for the effective server thread during a call.
Keep 32-bit and 64-bit registration views separate on a 64-bit host.

### 2. Inventory endpoint and activation surfaces

Query the endpoint mapper locally and from the exact authorized network vantage point.
Use RpcView or an equivalent parser to export process, interface UUID/version, binding strings, endpoint, registration flags, and annotation.
Use Impacket's endpoint mapper tooling for a scriptable remote view:

```powershell
python -m impacket.examples.rpcdump 127.0.0.1 | Out-File "$Case\evidence\epm-local.txt"
python -m impacket.examples.rpcdump lab-rpc01 | Out-File "$Case\evidence\epm-remote.txt"
```

If the installed Impacket package exposes `rpcdump.py` rather than the module entry point, record that exact executable and version in `tools.txt`.
Compare local and remote results; a local-only `ncalrpc` registration and remotely reachable `ncacn_ip_tcp` registration are different boundaries.
Enumerate named pipes and owners with Sysinternals PipeList, Process Explorer handles, Procmon, or `Get-ChildItem \\.\pipe\` from the client context.
Inspect `\RPC Control` and ALPC objects with WinObj or NtObjectManager from the same session and integrity level as the client.
Map TCP endpoints to owning processes with `Get-NetTCPConnection`, then reconcile dynamic ports with endpoint-mapper bindings.

Export COM registration from all precedence locations:

```powershell
$Roots = @(
 'Registry::HKEY_CURRENT_USER\Software\Classes\CLSID',
 'Registry::HKEY_LOCAL_MACHINE\Software\Classes\CLSID',
 'Registry::HKEY_LOCAL_MACHINE\Software\Classes\WOW6432Node\CLSID',
 'Registry::HKEY_LOCAL_MACHINE\Software\Classes\AppID'
)
foreach ($Root in $Roots) {
  if (Test-Path $Root) { Get-ChildItem $Root -ErrorAction SilentlyContinue | Select-Object PSPath,PSChildName }
} | Export-Csv "$Case\evidence\com-roots.csv" -NoTypeInformation
Get-CimInstance Win32_DCOMApplicationSetting | Select-Object AppID,Description,LocalService,RunAsAuthenticationLevel | Export-Csv "$Case\evidence\dcom.csv" -NoTypeInformation
```

Use OleViewDotNet to export classes, interfaces, proxies, servers, AppIDs, runtime classes, and process mappings; preserve its version and the exact database/export file.
For each CLSID, record `InprocServer32`, `LocalServer32`, `TreatAs`, `AppID`, `DllSurrogate`, `LocalService`, threading model, typelib, proxy/stub CLSID, elevation metadata, and ACL-bearing values.
Resolve environment variables and unquoted paths only for analysis; preserve the raw registry bytes and value type.

### 3. Correlate an interface to a server module

Start from the interface UUID/version, not from a guessed function name.
Search loaded modules and static binaries for the 16-byte little-endian UUID representation and account for GUID mixed-endian fields.
Find cross-references to `RpcServerRegisterIf`, `RpcServerRegisterIfEx`, `RpcServerRegisterIf3`, `RpcServerUseProtseqEp`, `RpcServerUseProtseqEpEx`, and `RpcEpRegister`.
At each registration call, recover the `RPC_SERVER_INTERFACE` pointer and the registration flags, maximum calls, security callback, manager EPV, and endpoint choices.
When symbols exist, bind only the module-matched PDB and preserve its GUID/age.
Without private symbols, report function locations as `module SHA-256 + RVA`, never as a process VA.

Recover these relationships:

```text
RPC_SERVER_INTERFACE
  InterfaceId: interface UUID + major/minor version
  TransferSyntax: NDR32 or another primary syntax
  DispatchTable -> RPC_DISPATCH_TABLE.DispatchTable[]
  InterpreterInfo -> MIDL_SERVER_INFO or architecture-specific interpreter data
  Flags and protocol endpoint declarations
```

The dispatch-table index is not automatically the wire procedure number when wrappers, interpreted stubs, or syntax-specific tables intervene; verify against `FmtStringOffset` and a live call.
Identify `NdrServerCall2`, `NdrServerCallAll`, `Ndr64AsyncServerCall`, or generated stub wrappers to distinguish interpreted from explicit stubs.
For COM interfaces, correlate IID to proxy/stub registration, typelib data, `MIDL_STUBLESS_PROXY_INFO`, proxy vtable, and the class/server that exposes it.

### 4. Recover the NDR contract

Record each accepted transfer syntax, especially classic NDR32 and NDR64; do not decode one using the other's format tables.
For interpreted NDR32, recover `MIDL_SERVER_INFO`, `MIDL_STUB_DESC`, procedure format string, format-string-offset table, dispatch table, thunk table, expression evaluators, user-marshal quadruples, and type format string.
For NDR64, locate syntax-info arrays and the NDR64 procedure/type fragments selected by the negotiated transfer syntax.
Use public symbols and MIDL-generated reference binaries to identify compiler/version-specific structure layouts rather than hardcoding one SDK's offsets.
Decode parameters in wire order and record direction, stack offset, base type, pointer class, allocation ownership, correlation descriptor, range constraint, and server allocation semantics.
Expand conformant/varying arrays into `max_count`, `offset`, and `actual_count` invariants.
Expand discriminated unions into discriminator type, legal arms, default arm, and nested pointer ownership.
Record strings as ANSI/Unicode, terminated/nonterminated, counted, conformant, or user-marshaled; do not label every `wchar_t *` as `[string]` without format evidence.
Record context handles with rundown routine, serialization flags, nullability, and which calls create, consume, or close them.
Record `FC_USER_MARSHAL`, transmit/represent-as, pipes, interface pointers, and system handles as custom boundaries requiring their registered routines.

Maintain a field table such as:

```text
opnum, direction, logical_name, NDR_kind, wire_width, alignment,
correlation_source, legal_range, ownership, evidence, confidence
```

Validate the recovered grammar by generating or compiling a benign MIDL analogue and comparing procedure/type format behavior.
A decompiler C prototype alone is not a wire contract.

### 5. Reconstruct typed IDL and compile a control client

Write the narrowest IDL that reproduces the observed call; preserve unknown fields as fixed-width opaque arrays with evidence notes rather than inventing semantics.
Use explicit attributes such as `[in]`, `[out]`, `[size_is]`, `[length_is]`, `[range]`, `[switch_is]`, `[context_handle]`, and pointer class only when supported by recovered format data.
Compile separate x64 and x86 clients where the server accepts both:

```powershell
$SdkBin = 'C:\Program Files (x86)\Windows Kits\10\bin\10.0.26100.0\x64'
& "$SdkBin\midl.exe" /nologo /env x64 /robust /Oicf /out "$Case\build\x64" "$Case\idl\target.idl"
& "$SdkBin\midl.exe" /nologo /env win32 /robust /Oicf /out "$Case\build\x86" "$Case\idl\target.idl"
```

Pin the SDK version because MIDL output and NDR64 behavior can vary.
Wrap every client call with RPC exception handling and print numeric status plus `FormatMessage` text.
Set bounded call timeout with `RpcBindingSetOption(binding, RPC_C_OPT_CALL_TIMEOUT, milliseconds)`.
Implement explicit cancellation for async calls and always invoke the context-close path before releasing the binding.
Generate one request known to succeed and one request that should fail authorization under the weaker principal.

### 6. Construct binding and security deliberately

Build string bindings with `RpcStringBindingComposeW`, convert with `RpcBindingFromStringBindingW`, and release both string and binding with the matching RPC free routines.
Record object UUID, protocol sequence, network address, endpoint, and options as separate fields.
For endpoint-mapper resolution, leave the endpoint absent only when the interface is registered there and call `RpcEpResolveBinding` deliberately.
Set authentication with `RpcBindingSetAuthInfoExW`; record SPN, identity source, authn service, authz service, authn level, and `RPC_SECURITY_QOS` capabilities.
Distinguish `RPC_C_AUTHN_LEVEL_CONNECT`, `CALL`, `PKT`, `PKT_INTEGRITY`, and `PKT_PRIVACY`; a successful call at one level does not establish server policy at another.
Test mutual authentication, static versus dynamic identity tracking, impersonation level, delegation, and secure-only flags as separate variables.
Do not fall back from Kerberos to NTLM silently; capture negotiated authentication using ETW, security logs, or network traces appropriate to the protocol sequence.
For local RPC, do not assume `ncalrpc` means trusted; inspect server-side client-token checks and effective impersonation.
For named pipes, reconcile RPC authentication with the pipe DACL and the server's impersonation/revert behavior.
For TCP, include firewall scope, endpoint mapper reachability, dynamic-port policy, and SPN correctness in the boundary model.

### 7. Prove server-side token and sink continuity

Break on the recovered server dispatch function or `rpcrt4!NdrServerCall*` only after identifying the target process and thread.
Use WinDbg to capture the server thread before impersonation, during the sensitive operation, and after return:

```text
~
!thread
!token
k
!handle 0 f
```

Correlate client and server with activity IDs, call timing, ALPC/RPC ETW, Procmon operation, endpoint, and a unique benign request field.
At the sink, record whether the server uses an inherited/duplicated handle or reopens a caller-controlled name.
Capture desired access, object namespace, security descriptor, share flags, create disposition, and effective token at the final access check.
Check whether the server validates client PID, session, package/AppContainer SID, integrity, elevation type, service SID, signature, or process identity, and whether the checked identity is raceable or later re-resolved.
For impersonation, identify `RpcImpersonateClient`, `RpcRevertToSelf`, `CoImpersonateClient`, token duplication, worker-queue handoff, and any gap where work resumes under the process token.
A privileged side effect under the service token is relevant only if the weaker client controls the sink arguments beyond the intended policy.

### 8. Map RPC to ALPC

Local RPC endpoints normally materialize below `\RPC Control` and use ALPC through the RPC runtime; the public RPC contract and the underlying ALPC transport are separate layers.
Use WinObj/NtObjectManager to preserve the port name, owning process, session visibility, and security descriptor.
Trace `ntdll!NtAlpcConnectPort`, `NtAlpcSendWaitReceivePort`, and server receive/reply paths only to answer transport or message-lifetime questions that RPC-level evidence cannot resolve.
Record `PORT_MESSAGE` lengths, message ID, callback ID, client ID, flags, and whether auxiliary shared views or handle attributes carry data.
Do not parse raw ALPC payload as NDR until correlation proves the payload region and framing.
Check ALPC handle attributes, section views, completion ports, cancellation, disconnect, and client death as lifetime boundaries.
For a direct ALPC protocol with no RPC runtime, route message grammar recovery through the same valid-call-first discipline but do not invent an interface UUID or NDR layer.

### 9. Analyze COM/DCOM activation and invocation

Initialize COM with a deliberate apartment model using `CoInitializeEx`; record STA/MTA and whether a message pump is required.
Call `CoInitializeSecurity` before marshaling when the process owns security initialization, or record `RPC_E_TOO_LATE` when another component already fixed it.
Resolve class activation through `CoCreateInstanceEx`, `CoGetClassObject`, `CoGetObject`, or WinRT activation as actually used by the client.
For an elevation moniker, preserve the exact display name, bind options, requested IID, caller token, consent behavior, and server identity; do not reduce the result to “auto-elevated.”
Map CLSID to AppID, server path/service, surrogate, launch permission, access permission, authentication level, RunAs identity, and elevation policy.
Compare HKCU and HKLM class resolution from the exact client architecture and integrity level; 32-bit and 64-bit views can select different servers.
Test `TreatAs`, proxy/stub, typelib, and in-process path precedence only in a disposable profile with before/after registry exports and exact rollback.
For DCOM, preserve machine-wide policy, AppID ACLs, firewall, SPN/authentication, activation identity, and interface-level authorization separately.
For out-of-process COM, trace activation to `svchost.exe`, a service, `dllhost.exe`, or a local server, then repeat the server-token/sink proof.
For in-process COM, the boundary may be code loading into the caller rather than privilege transition; report it accordingly.
For packaged COM/WinRT, include package identity, capability declarations, runtime class registration, broker, and AppContainer policy.

### 10. Build a mutation harness

Begin from a serialized valid call and mutate typed logical fields through generated stubs where possible.
Keep endpoint, authentication, client identity, opnum, and all unrelated fields fixed for a single-variable experiment.
Partition mutations by scalar boundaries, enum values, correlation counts, nullability, string termination, union discriminants, nested pointer graphs, context state, cancellation point, and concurrency ordering.
Do not mutate raw NDR offsets blindly until the typed grammar and baseline are stable; otherwise most cases die in generic unmarshalling and never reach target logic.
For each test case record seed, interface/version, transfer syntax, opnum, typed input, serialized-request hash when available, client token, call status, server exception, side effect, dump/trace IDs, and cleanup status.
Set a bounded per-call timeout and an outer process timeout.
Restart or snapshot the service/VM deterministically after a crash; record the restart boundary so stateful context handles are not replayed as if valid.
Use exact synchronization events such as service ETW, debugger breakpoints, named events, or RPC async completion rather than fixed sleeps.
Exercise call cancellation, client disconnect, context rundown, duplicate close, server stop, and process termination as first-class lifetime tests.
Version-diff interface UUIDs, transfer syntaxes, format strings, dispatch RVAs, security callbacks, and sink behavior across patched/unpatched builds.

## Key structures & interfaces

`RPC_IF_ID` combines interface UUID with major/minor version and is the stable inventory key.
`RPC_SYNTAX_IDENTIFIER` identifies an interface or transfer syntax with a GUID and syntax version.
`RPC_SERVER_INTERFACE` links interface identity, primary transfer syntax, dispatch table, protocol endpoints, manager EPV, interpreter metadata, and flags.
`RPC_DISPATCH_TABLE` supplies dispatch count and function array; validate its module range before following entries.
`MIDL_SERVER_INFO` links `MIDL_STUB_DESC`, dispatch routines, procedure format, offset table, thunks, transfer syntax, and syntax-info records.
`MIDL_STUB_DESC` anchors allocator/free callbacks, type formats, binding metadata, expression evaluators, user-marshal routines, bounds-checking flags, and MIDL version.
`MIDL_STUBLESS_PROXY_INFO` and proxy vtables are central for client-side COM interface recovery.
`RPC_SECURITY_QOS` controls version, capabilities, identity tracking, and impersonation type used by an authenticated binding.
`RPC_CALL_ATTRIBUTES_V2` can expose authenticated client identity, PID, protocol sequence, and locality where server code requests those fields; verify field availability by flags and OS build.
`RPC_ASYNC_STATE` owns async notification, cancellation, and completion state and must outlive the call.
`PORT_MESSAGE` is the ALPC/LPC message header; all total/data lengths and IDs require checked bounds before payload interpretation.
`ALPC_MESSAGE_ATTRIBUTES` and attribute buffers can carry handles, views, security, context, and work-on-behalf data beyond the message bytes.
COM activation centers on CLSID, IID, AppID, class factory, proxy/stub, OBJREF/marshaling, apartment, blanket, and activation/security descriptors.
Security descriptors in `LaunchPermission` and `AccessPermission` are binary self-relative descriptors; decode and preserve their raw values rather than editing them in place.

## Offset and provenance methodology

Use module-relative RVAs in every static report: `module path + SHA-256 + file version + PDB GUID/age + RVA`.
For an inferred structure field, record containing function RVA, instruction bytes, access width, base register provenance, and an independent validation such as symbols, generated MIDL output, or a live value.
For format strings, record module, section, RVA, byte span, parser/tool version, transfer syntax, and the procedure index that reaches the span.
For dispatch tables, verify every pointer resolves into an expected executable module and compare count against recovered opnums.
For runtime pointers, save module bases from the same process instance and normalize them to RVAs before comparison across boots.
For registry and endpoint identity, preserve raw exports plus query architecture, user SID, session, and timestamp.
No CLSID, interface UUID, port name, RVA, or field offset is portable to another build without revalidation.

## Tooling

- RpcView: process-centric server interface, endpoint, registration, and dispatch inventory.
- OleViewDotNet: COM classes, interfaces, proxy/type information, activation, security, runtime classes, and process views.
- NtObjectManager/WinObj: ALPC/Object Manager namespace, handles, security descriptors, tokens, and COM/RPC helper views.
- Impacket `rpcdump`, `epm`, and protocol modules: scriptable endpoint-mapper and typed-client foundations.
- MIDL from a pinned Windows SDK: compile reconstructed IDL and generate robust NDR32/NDR64 stubs.
- IDA/Ghidra: registration xrefs, `RPC_SERVER_INTERFACE`, MIDL structures, format strings, dispatch, and privileged sinks.
- WinDbg/TTD: server-thread token, impersonation window, call path, exception, context lifetime, and sink proof.
- Procmon: file/registry/process/network side effects joined to the server process and call timeline.
- ETW/WPR/WPA: RPC, COM, ALPC, service, authentication, and scheduler correlation; use `windows-telemetry-etw` for provider engineering.
- Process Explorer, PipeList, TCPView, and `Get-NetTCPConnection`: owning process and endpoint reconciliation.

## Pitfalls & OPSEC

- Endpoint mapper visibility is not call authorization; test the binding and server security callback under the intended weak identity.
- A successful bind does not prove a procedure is callable, and an access-denied call does not prove all opnums enforce the same policy.
- NDR32 and NDR64 metadata are not interchangeable; incorrect decoding produces plausible but dangerous prototypes.
- Context handles and full pointers carry server-side state and aliasing; random byte mutation can leak resources without reaching target logic.
- COM registration precedence depends on user, architecture, package context, and activation path; a writable key is not necessarily consulted.
- `LaunchPermission`, `AccessPermission`, service DACLs, pipe DACLs, RPC auth, and method authorization are independent controls.
- Server impersonation may end before queued work reaches a sink; capture the effective token at final use, not just at dispatch entry.
- A server can validate a client PID and later reopen PID-derived state after reuse; distinguish object handles from names/identifiers.
- Remote RPC tests can affect dynamic port ranges, authentication logs, service stability, and other tenants; confine targets and rate explicitly.
- COM hijack experiments alter durable registry state and may trigger unrelated activations; use a disposable profile and verify rollback from raw exports.
- Debugger attachment changes timing and service recovery behavior; reproduce any race with event-driven coordination outside the debugger.
- Do not log clear credentials, full tokens, or sensitive NDR payloads by default; hash or redact fields while retaining schema and provenance.
- A crash during unmarshalling is not automatically exploitable; route only a controlled memory primitive to exploit development.

## Evidence outputs

```text
platform.md             OS/build/architecture/mitigations and tool versions
servers.csv             service/process/token/module/signature identity
endpoints.csv           interface UUID/version, syntax, binding, endpoint, owner
com-registration/       raw HKCU/HKLM/WOW64 CLSID/AppID exports
interfaces/             recovered IDL, format maps, dispatch and provenance ledgers
bindings.jsonl          endpoint/authentication/QoS/client identity per run
calls.jsonl             typed input, opnum, result, side effect, trace and cleanup IDs
traces/                 ETL, Procmon PML/CSV, debugger logs, packet captures
crashes/                dumps, normalized stacks, module hashes, minimized cases
rollback.md             service, registry, profile, snapshot, and context cleanup proof
```

## Routing

- Use `bof-coff-development` when the client or probe is packaged as a BOF and the problem is COFF relocation, imports, or Beacon ABI.
- Use `windows-telemetry-etw` for provider selection, session construction, stack capture, event-loss accounting, and typed ETL consumption.
- Use `hyper-v-offensive` when the broker is `vmwp.exe`, `vmcompute.exe`, HCS, VMBus, or another Hyper-V boundary.
- Use `linux-kernel-exploitation` for a confirmed Linux kernel primitive, not Windows RPC/COM logic.
- Use `c2-implant-engineering` for implant tasking, module ABI, transport, cancellation, reconnect, and long-lived runtime behavior.
- Use `ebpf-offensive` for Linux eBPF verifier/JIT, map, link, or hook research.
- Use `linux-host-post-exploitation` for privilege graphs and evidence-led operation after access to a Linux host.
- Use `windows-internals` for Object Manager, token, loader, memory, and subsystem context around the call.
- Use `windows-0day-hunting` for a validated privileged workflow weakness and variant campaign.
- Use `exploit-dev` only after the harness establishes a controlled memory-corruption capability.
- Use `pe-tools`, `ida-reverse`, or `ghidra-reverse` for module layout and static binary reconstruction.

## Final gate

- [ ] OS, process/service, module, symbols, interface, syntax, endpoint, and client/server identities are pinned.
- [ ] A valid typed call and an authorization-negative control are reproducible.
- [ ] NDR field grammar and ownership come from format evidence, not only a decompiler prototype.
- [ ] Binding authentication, QoS, negotiated mechanism, and endpoint reachability are recorded independently.
- [ ] Effective server token is observed at the final sensitive sink.
- [ ] COM registry view, AppID/security, activation server, apartment, and proxy path are preserved where relevant.
- [ ] Mutations are bounded, event-synchronized, crash-contained, and paired with context/service cleanup.
- [ ] All RVAs, structure fields, registry data, and interface claims retain build-specific provenance.
