---
name: windows-telemetry-etw
description: Use when tracing, consuming, reverse engineering, or validating Windows ETW, ETW-TI, WPP, TraceLogging, manifests, provider GUIDs, event descriptors, NtTraceControl, WPR/WPA profiles, stack-walk events, private schemas, dropped buffers, or EDR and anti-cheat telemetry coverage.
license: MIT
---
# Windows ETW, WPP, and TraceLogging engineering

## When to use

Use this skill when a Windows behavior must be measured through Event Tracing for Windows rather than inferred from API calls or alert presence.
Use it to enumerate providers, build private or file-mode sessions, consume `EVENT_RECORD` data, recover manifested/WPP/TraceLogging schemas, enable stack capture, quantify event loss, and correlate security telemetry across processes and clocks.
Use it when the prompt names ETW, ETW Threat Intelligence, WPP, TraceLogging, provider GUIDs, `EtwEventWrite`, `NtTraceControl`, WPR/WPA, `logman`, ETL, keywords, levels, stack walking, activity IDs, or dropped buffers.
Do not interpret “no event observed” until provider identity, enable state, keywords, level, filters, session health, consumer health, buffer loss, rundown, and clock alignment are proven.
A production result contains platform/provider provenance, the exact session configuration, raw ETL, decoded schema with confidence, loss counters, a positive control, a negative control, and a teardown receipt.

## Completion standard

Pin the Windows build, kernel build, target module hash/version, provider GUID and registration source, controller tool/API version, session name/GUID, logger mode, clock source, keyword masks, level, filters, buffer settings, stack policy, and consumer build.
Capture a known event whose generating action and provider call site are understood before using the trace to decide whether a target action was visible.
Preserve raw `EVENT_RECORD` identity and payload bytes even when TDH decoding succeeds.
Report every field as manifest-defined, TMF/PDB-defined, TraceLogging metadata-defined, static-call-site inferred, or unverified.
Query and record loss at session and consumer boundaries before stopping the session.

## Core workflow

### 1. Freeze platform and tool identity

Create a case directory and collect platform state:

```powershell
$Case = 'C:\lab\etw\case-001'
New-Item -ItemType Directory -Force "$Case\evidence", "$Case\artifacts", "$Case\profiles" | Out-Null
Get-ComputerInfo | Select-Object WindowsProductName,WindowsVersion,OsBuildNumber,OsArchitecture | Format-List | Out-File "$Case\evidence\platform.txt"
Get-CimInstance Win32_OperatingSystem | Select-Object Version,BuildNumber,LastBootUpTime | Format-List | Add-Content "$Case\evidence\platform.txt"
Get-CimInstance Win32_DeviceGuard | Format-List * | Out-File "$Case\evidence\deviceguard.txt"
logman /? | Select-Object -First 5 | Out-File "$Case\evidence\logman-version.txt"
wpr -version | Out-File "$Case\evidence\wpr-version.txt"
xperf -help 2>&1 | Select-Object -First 5 | Out-File "$Case\evidence\xperf-version.txt"
```

For every provider module under study, record path, SHA-256, file/product version, signature, architecture, loaded base, PDB GUID/age, and process/service identity.
Record whether the controller and consumer are elevated, service-hosted, protected, cross-session, or remote.
Keep provider display names as labels only; the GUID and module/build provenance are the stable keys.

### 2. Model the trace path

Write the expected path before collecting:

```text
instrumented call site or provider runtime
  -> provider registration and enable callback
  -> event descriptor + payload + optional metadata
  -> enabled logger/session and per-CPU buffers
  -> file and/or real-time delivery
  -> consumer callback and schema decoder
  -> normalized event and correlation timeline
```

Separate provider, controller, session/logger, consumer, and decoder failures.
A provider can be registered but disabled, enabled with a nonmatching keyword/level, filtered out, blocked by permissions, or writing to a different session.
A session can accept events but lose buffers before file flush or real-time delivery.
A consumer can fall behind, decode against the wrong schema, or discard events after receipt.

### 3. Inventory provider identity and schema sources

Enumerate public registrations using multiple views:

```powershell
logman query providers | Out-File "$Case\evidence\providers-logman.txt"
wpr -providers | Out-File "$Case\evidence\providers-wpr.txt"
Get-WinEvent -ListProvider * | Select-Object Name,Id,MessageFilePath,ResourceFilePath,ParameterFilePath | Export-Csv "$Case\evidence\providers-powershell.csv" -NoTypeInformation
```

For a known manifested publisher, export metadata and events:

```powershell
$ProviderName = 'Microsoft-Windows-Kernel-Process'
wevtutil gp "$ProviderName" /ge:true /gm:true /f:xml | Out-File "$Case\evidence\provider-metadata.xml"
logman query providers "$ProviderName" | Out-File "$Case\evidence\provider-logman.txt"
```

Record provider GUID, event IDs, versions, channels, levels, opcodes, tasks, keyword names/bits, maps, templates, localization resource, and manifest provenance.
Query registry publisher metadata where applicable, but do not assume every runtime provider is registered as an Event Log publisher.
Use `tracelog -enumguid` or equivalent legacy tooling for classic/WPP registrations when the WDK tool is installed.
Capture live provider registration only from a controlled process instance; dynamic TraceLogging providers may have no durable manifest.

### 4. Classify instrumentation family

Classify each provider before decoding:

- Manifested ETW uses a compiled manifest/resource with stable event descriptors and TDH-readable templates.
- Classic/MOF ETW often uses class GUIDs, event types/opcodes, and MOF metadata rather than modern event IDs/templates.
- WPP emits trace-message records keyed by control GUID, flag bits, message GUID/number, and TMF/PDB formatting metadata.
- TraceLogging embeds self-describing metadata associated with events and commonly registers dynamically through the TraceLogging runtime.
- Kernel logger flags and stack-walk configuration have special controller semantics and should not be treated as an arbitrary user provider.

Do not combine fields from two families merely because tools render both into the same ETL view.
Record exactly how the provider was classified and which independent source confirms it.

### 5. Capture a bounded command-line session

For a manifested provider, create a named file-mode collector with explicit level, keywords, and buffers:

```powershell
$Session = 'LabEtwCase001'
$ProviderGuid = '{22fb2cd6-0e7b-422b-a0c7-2fad1fd0e716}'
logman create trace $Session -ow -o "$Case\artifacts\provider.etl" -p $ProviderGuid 0xffffffffffffffff 0xff -bs 1024 -nb 32 256
logman start $Session
Get-Process | Select-Object -First 5 | Out-Null
logman query $Session | Out-File "$Case\evidence\session-running.txt"
logman stop $Session
logman delete $Session
```

The GUID above is an example kernel-process provider identity; verify it from the current host before using it in a real case.
Choose a narrow keyword mask after a broad discovery run; retain both configurations in evidence.
Set level `0xff` only for discovery when volume is bounded, then tighten it to the provider's documented level.
Use a unique session name and ETL path so concurrent collectors are not modified accidentally.
Before stopping, query session state and note buffers, events lost, and file status through native APIs when `logman` does not expose enough detail.

For WPR profiles, keep the WPRP under case control:

```powershell
wpr -start "$Case\profiles\lab.wprp!Lab.Verbose" -filemode
Get-Process | Select-Object -First 5 | Out-Null
wpr -status | Out-File "$Case\evidence\wpr-running.txt"
wpr -stop "$Case\artifacts\wpr.etl" "case-001 controlled capture"
```

For kernel stacks with Xperf, name flags and stack events explicitly:

```powershell
xperf -on PROC_THREAD+LOADER+PROFILE -stackwalk Profile+CSwitch+ReadyThread -BufferSize 1024 -MinBuffers 64 -MaxBuffers 256 -f "$Case\artifacts\kernel.etl"
Get-Process | Select-Object -First 5 | Out-Null
xperf -d "$Case\artifacts\kernel.etl"
```

Check for an existing kernel logger first; never stop or reconfigure a logger you do not own.

### 6. Create a native controller correctly

Allocate `EVENT_TRACE_PROPERTIES_V2` or the documented properties version plus contiguous storage for logger and log-file names.
Set `WNODE_HEADER.BufferSize` to the entire allocation, assign a unique session GUID, choose `ClientContext`, and set `WNODE_FLAG_TRACED_GUID`.
Set `BufferSize` in KiB, `MinimumBuffers`, `MaximumBuffers`, `FlushTimer`, `LogFileMode`, `MaximumFileSize`, and file naming policy deliberately.
Use `ClientContext = 1` for QPC, `2` for system time, or `3` for CPU cycle counter only after confirming the documented semantics on the target SDK/OS.
Call `StartTraceW` and treat `ERROR_ALREADY_EXISTS` as a name/GUID ownership conflict, not permission to stop the existing session.
Enable providers with `EnableTraceEx2` and record control code, level, `MatchAnyKeyword`, `MatchAllKeyword`, timeout, and every `EVENT_FILTER_DESCRIPTOR`.
Set `ENABLE_TRACE_PARAMETERS.EnableProperty` for requested stack traces, SID, terminal-session ID, container ID, or other supported extended data; record unsupported-return codes.
Use provider-specific filters such as event ID, process ID, executable name, stack walk, payload, or scope only when the provider/runtime documents support.
Query the session with `ControlTraceW(..., EVENT_TRACE_CONTROL_QUERY)` before and after the stimulus.
Disable providers explicitly, request rundown when the provider defines a rundown keyword/event set, then stop with `ControlTraceW(..., EVENT_TRACE_CONTROL_STOP)`.
Free the properties buffer only after all control calls finish.

A controller result must distinguish:

```text
start_status, enable_status, provider_enable_callback_observed,
query_status, disable_status, stop_status, events_lost,
buffers_written, log_buffers_lost, real_time_buffers_lost
```

### 7. Consume ETL or real-time records safely

Initialize `EVENT_TRACE_LOGFILEW` with a file path or logger name, `PROCESS_TRACE_MODE_EVENT_RECORD`, and `EVENT_RECORD` callback.
Add `PROCESS_TRACE_MODE_REAL_TIME` only for a real-time session.
Call `OpenTraceW`, reject `INVALID_PROCESSTRACE_HANDLE`, and run `ProcessTrace` on a dedicated consumer thread because it blocks until stop or close.
In the callback, preserve `EVENT_HEADER`, `BufferContext`, extended-data items, and raw user-data bytes before returning.
Pointers in `EVENT_RECORD` are callback-scoped; copy data needed by asynchronous decoders.
Use two-pass `TdhGetEventInformation` to size and obtain `TRACE_EVENT_INFO`.
Use `TdhGetPropertySize` and `TdhGetProperty` with `PROPERTY_DATA_DESCRIPTOR` chains for arrays and nested structures.
Respect property length/count references, maps, pointer width, event version, and decoding source.
Do not cast `UserData` to a C struct unless the exact event version, alignment, pointer-size rules, and schema source prove the layout.
Call `CloseTrace` to unblock real-time processing during teardown and join the consumer thread on an exact completion event.
Record `EVENT_TRACE_LOGFILEW.EventsLost` and `LogfileHeader` timing/frequency metadata after processing.

### 8. Normalize event identity

Use this identity tuple before field-level interpretation:

```text
provider_guid, event_id, version, channel, level, opcode, task,
keyword, process_id, thread_id, processor, timestamp, activity_id,
related_activity_id, pointer_size, decoding_source
```

Provider GUID plus event ID alone is insufficient because versions can change payload templates.
Classic/WPP events may require GUID/type/message-number identities instead of a modern event ID.
Preserve unknown extended-data types and raw bytes rather than discarding them.
Normalize process/thread identity against lifecycle events; a PID/TID can be reused later in the capture.
Join image identity using process start key or start timestamp plus PID, not PID alone.

### 9. Recover manifested and private schemas

For manifested providers, prefer the exact binary's embedded/resource manifest and current-host publisher metadata.
Verify event version and template against the emitted descriptor at the static call site when binaries are available.
For private manifested providers, locate `EventRegister*`/`EtwEventRegister`, provider GUID data, and `EventWrite*`/`EtwEventWrite*` call sites.
Recover `EVENT_DESCRIPTOR` values from constants or data references and map payload arguments in call order.
For `EventWrite`, interpret `EVENT_DATA_DESCRIPTOR` entries by source type, size, and lifetime; do not assume strings are NUL-terminated when an explicit byte count is passed.
For `EtwEventWrite`, trace registration handle provenance and descriptor pointer before associating a call site with a provider.
Use version-to-version binary diffing to detect field insertions, width changes, and descriptor reuse.
Validate inferred fields by varying one source value while holding the rest constant and comparing raw payload byte deltas.

Keep a field ledger:

```text
provider, event identity, byte offset or descriptor index, width,
type/encoding, count/length source, semantic name, evidence, confidence
```

Offsets are event-version and pointer-width specific and must be re-derived after a module or schema change.

### 10. Recover WPP metadata

Identify WPP control GUIDs, trace flags, levels, message GUIDs, and message numbers from PDB, TMF, source-generated headers, or static registration data.
When matching PDBs exist, use `tracepdb`/TraceView tooling from the pinned WDK to produce TMF files and retain PDB GUID/age.
Decode WPP with the exact TMF set; a message number rendered with another build's TMF can look valid while assigning wrong arguments.
Record format string, source file/line if present, argument types/order, pointer width, and module build.
For stripped binaries, locate `WPP_CONTROL_GUIDS`, registration/init calls, `WPP_SF_*` formatter call patterns, and argument marshalling statically.
Validate recovered WPP formats with controlled values and raw-byte comparison.
Treat dynamic strings and pointers as data-lifetime/privacy concerns; some WPP helpers copy data while others encode values immediately according to generated macros.

### 11. Recover TraceLogging metadata

Identify dynamic provider registration through TraceLogging/EventRegister wrappers and capture provider name/GUID from the live registration or binary data.
Let TDH consume TraceLogging self-describing metadata first; preserve the raw metadata and payload when decoding fails.
Record event name, tags, field names, in-types, out-types, lengths, counts, nested structs, and event version as emitted.
Do not assume source-level field order from a decompiler when compiler-generated metadata proves a different encoded order.
Correlate a static `TraceLoggingWrite` call with runtime metadata by provider, event name, call-site RVA, and controlled field values.
Version private TraceLogging schemas by module hash because event names can remain stable while fields change.

### 12. Configure stacks deliberately

For modern providers, request `EVENT_ENABLE_PROPERTY_STACK_TRACE` and confirm the provider/runtime honors it.
Use event-ID stack filters where supported so high-volume providers do not capture stacks for every event.
For classic kernel events, configure `TraceSetInformation` with `TraceStackTracingInfo` and `CLASSIC_EVENT_ID` records, or use a pinned WPR/Xperf profile that emits the equivalent configuration.
Collect image-load and process/thread lifecycle events needed to symbolize stacks.
Pin the symbol path and cache, then verify module PDB GUID/age before assigning function names.
Preserve raw instruction addresses and module base ranges so symbols can be corrected after capture.
Account for user/kernel stack separation, WOW64 transitions, stack truncation, missing unwind data, frame-pointer omission, and protected-process restrictions.
A missing stack is a distinct result from a missing event.

### 13. Measure buffers, ordering, and loss

Treat buffer configuration as an experimental variable.
Record buffer size, minimum/maximum buffers, per-processor buffering mode, flush timer, file mode, real-time mode, file size policy, consumer latency, CPU count, event rate, and capture duration.
Query session counters before stimulus, after stimulus, and immediately before stop.
Record `EventsLost`, `LogBuffersLost`, `RealTimeBuffersLost`, buffer counts, and consumer-side dropped/parse-failed counts separately.
Run a controlled burst whose generated operation count is independently known and compare generated, written, delivered, decoded, and normalized counts.
If loss occurs, repeat by changing one variable: larger buffers, more buffers, file mode instead of real time, narrower keywords, provider filter, or faster consumer.
Do not infer total order across CPUs from callback order.
Use QPC timestamp/frequency metadata, activity IDs, related activity IDs, process start keys, and explicit marker events for correlation.
When merging traces, preserve each logger's clock source and conversion basis; wall-clock timestamps alone can hide drift and reordering.
Enable rundown for long-lived objects when a capture starts mid-lifecycle, and label objects unresolved when rundown was unavailable.

### 14. Validate security visibility with controls

Build a provider/sensor map before evaluating EDR or anti-cheat coverage:

```text
source operation -> instrumentation/provider -> kernel/user boundary ->
collector/service -> local persistence/queue -> cloud or server ingestion ->
rule/model -> operator-visible result
```

Use a clean baseline to measure ambient events and a benign positive control known to produce the target event family.
Use a negative control that differs by one relevant parameter and should not produce the event or alert.
Verify provider/session health immediately before and after the target action.
For ETW Threat Intelligence, record required privilege/protection context, provider enable result, consumer identity, event filters, and target build; do not assume ordinary user consumers can reproduce protected sensor collection.
For Defender, PowerShell, CLR, AMSI-adjacent, RPC, kernel process/thread/image, minifilter, WFP, EDR, and anti-cheat sources, distinguish raw provider emission from product ingestion and alerting.
Account for delayed cloud verdicts and queueing; preserve local event time, ingest time, and alert time.
A missing alert with healthy raw telemetry is a detection-policy result, not an ETW bypass.
A missing raw event with loss or disabled provider is an invalid experiment.

### 15. Correlate activity across providers

Use `EventActivityIdControl` or provider-specific correlation where the application supports activity propagation.
Capture `EVENT_HEADER_EXT_TYPE_RELATED_ACTIVITYID` and preserve parent/child relationships.
When activity IDs are absent, join on process start key, PID/TID lifetime, object/handle identifier, endpoint, file ID, network tuple, and tightly bounded QPC windows.
Insert a private marker provider into lab harnesses so stimulus begin/end and case ID exist in the same clock domain as target events.
Never use a fixed sleep as proof of event arrival; wait for the exact marker/target record with a bounded timeout.
Record failed correlations as ambiguous rather than selecting the nearest event by timestamp.

## Key structures & interfaces

`EVENT_DESCRIPTOR` contains ID, Version, Channel, Level, Opcode, Task, and Keyword and is the first key for modern event identity.
`EVENT_DATA_DESCRIPTOR` describes one provider-supplied payload fragment by pointer, size, and reserved/type field.
`EVENT_HEADER` contains provider/event identity, timestamp, process/thread, activity ID, flags, and pointer-width/timing context.
`EVENT_RECORD` adds buffer context, extended data, user-data length/pointer, and user context for modern callbacks.
`EVENT_HEADER_EXTENDED_DATA_ITEM` carries related activity IDs, stacks, SID, terminal session, instance info, process start key, container, and other typed extensions.
`WNODE_HEADER` and `EVENT_TRACE_PROPERTIES_V2` define logger identity, clock, buffers, modes, file policy, and post-query loss counters.
`ENABLE_TRACE_PARAMETERS` and `EVENT_FILTER_DESCRIPTOR` define provider enable properties and filters.
`EVENT_TRACE_LOGFILEW` configures file/real-time consumption and reports logfile timing plus consumer-visible loss.
`TRACE_EVENT_INFO` and `EVENT_PROPERTY_INFO` describe TDH templates, maps, property flags, counts, lengths, and decoding source.
`PROPERTY_DATA_DESCRIPTOR` selects nested/array properties for TDH extraction.
`CLASSIC_EVENT_ID` pairs a classic event GUID with type for kernel stack tracing configuration.
`TRACE_GUID_REGISTRATION` and classic trace headers matter for legacy providers; do not force them into `EVENT_RECORD` semantics without the compatibility path.

## Native API sequence

Controller ownership should follow this exact lifecycle:

```text
allocate and initialize properties
StartTraceW
EnableTraceEx2 for each provider
ControlTraceW QUERY and preserve counters
trigger bounded workload
ControlTraceW QUERY and preserve counters
EnableTraceEx2 DISABLE for owned providers
ControlTraceW STOP for owned session
free properties
```

Consumer ownership should follow this lifecycle:

```text
initialize EVENT_TRACE_LOGFILEW
OpenTraceW
signal consumer-ready
ProcessTrace on owned thread
copy records inside callback
CloseTrace during bounded teardown
wait for exact consumer-exit signal
free decoder and output state
```

Do not let controller failure paths skip provider disable/session stop, and do not stop a session whose ownership token/name/GUID does not match the case record.

## Offset and provenance methodology

Represent static event call sites as `module path + SHA-256 + version + PDB GUID/age + RVA`.
Represent payload layouts as `provider GUID + event ID/version + pointer width + decoding source + byte/descriptor offset`.
For WPP, add control GUID, message GUID/number, TMF/PDB identity, format string, and source line when available.
For TraceLogging, add provider/event name, metadata hash, module hash, and runtime-capture ID.
For live addresses, save the process/module map from that capture and normalize to RVA before cross-build comparison.
For inferred private fields, retain the instruction/call-site bytes and controlled byte-delta validation.
Never copy an event offset, keyword mask, or WPP TMF from another build without checking descriptor and module identity.

## Tooling

- `logman`: provider inventory and bounded command-line sessions.
- `wevtutil`: manifested publisher/event metadata and channel configuration.
- WPR/WPA and Windows Performance Toolkit: profile-controlled capture, stacks, graphs, and ETL analysis.
- Xperf: kernel logger flags, stack-walk configuration, merge, and low-level trace control.
- PerfView/TraceEvent: managed consumer, stack, rundown, and high-volume analysis workflows.
- `tracelog`, TraceView, and `tracepdb`: classic ETW/WPP control and TMF recovery from matched PDBs.
- TDH APIs: machine-consumed event metadata, maps, property sizes, and property values.
- krabsetw or a small native controller: reproducible provider enable/filter/consumer harnesses; pin library commit and SDK.
- IDA/Ghidra: provider GUID, descriptor, payload-construction, WPP macro, and private schema recovery.
- WinDbg: registration handles, enable callbacks, `EtwEventWrite` call sites, stack/lifetime issues, and kernel provider paths.

## Pitfalls & OPSEC

- Provider names are not unique identities; always anchor the GUID and module/build.
- Keyword zero has provider-specific semantics and may bypass an expected `MatchAnyKeyword` filter; test it explicitly.
- Level and keyword filtering occur before consumer decoding; a healthy consumer cannot recover events never enabled.
- Real-time consumers can cause loss through backpressure; file-mode success does not prove real-time success.
- Circular ETL files overwrite old buffers by design; distinguish overwrite policy from dropped buffers.
- Starting mid-lifecycle without rundown can leave process, image, handle, or object references unresolved.
- TDH success does not prove semantic names are current if publisher resources came from a mismatched binary/build.
- Event payloads can contain paths, command lines, SIDs, tokens, script content, network data, and credentials; classify and minimize retention.
- Broad kernel stacks and all-keyword captures can impose measurable overhead and alter race timing; quantify overhead with a no-stack baseline.
- Protected/security providers may require identities and access not available to an ordinary controller; record access failure rather than weakening host controls.
- Stopping a shared or system session can blind unrelated diagnostics; use unique names and verify ownership.
- Attaching a debugger or enabling a provider may change timing and code paths through enable callbacks; preserve a nondebug capture.
- “No ETW event” never means “no telemetry”; callbacks, minifilters, WFP, Event Log, private IPC, memory scanners, and server-side signals can observe the same behavior.

## Evidence outputs

```text
platform.md          OS/kernel/build, mitigations, controller/consumer privilege
providers.csv        GUID, name, family, module, version, registration/schema source
session.json         logger GUID/name, clock, modes, buffers, filters, keywords, level
raw/                 immutable ETL and hashes
schema/              manifests, TMF, TDH exports, TraceLogging metadata, field ledger
records.jsonl        normalized identity, decoded fields, raw payload hash, confidence
loss.json            query snapshots, session loss, consumer loss, parse failures
controls.md          positive/negative stimuli and expected/observed records
symbols/             module map, PDB GUID/age, stack symbolization receipt
teardown.md          providers disabled, session stopped, consumer joined, files closed
```

## Routing

- Use `bof-coff-development` for COFF parsing, Beacon API shims, BOF imports, relocations, and loader cleanup.
- Use `windows-rpc-com-attack` for RPC/COM/ALPC interface recovery, binding security, impersonation, and typed call harnesses; use this skill to trace them.
- Use `hyper-v-offensive` for Hyper-V partitions, hypercalls, VMBus, VSP/VSC, worker processes, or HCS; use this skill for host/guest trace collection.
- Use `linux-kernel-exploitation` for Linux kernel primitives and mitigation chains, not Windows ETW.
- Use `c2-implant-engineering` for task protocol, module lifecycle, transport, reconnect, and implant-level observability experiments.
- Use `ebpf-offensive` for Linux eBPF verifier/JIT and hook telemetry.
- Use `linux-host-post-exploitation` for Linux host privilege graphs, evidence, and cleanup.
- Use `edr-bypass-re` only after this workflow establishes sensor health, positive control, raw telemetry, and loss state for the exact product/build.
- Use `windows-internals` for surrounding process, token, loader, memory, and Object Manager mechanics.
- Use `threat-hunting` for cross-host detection content and investigations after event semantics are stable.
- Use `windbg-ttd` when temporal debugging, kernel debugging, or TTD is the primary evidence source.

## Final gate

- [ ] OS, module, provider, controller, session, consumer, schema, and clock provenance are pinned.
- [ ] Provider family, GUID, event identity, keywords, level, filters, and enable result are recorded.
- [ ] A known positive-control event and a one-variable negative control are present.
- [ ] Raw ETL and payload bytes are retained with hashes before normalization.
- [ ] Session, file, real-time, consumer, and parser loss are measured separately.
- [ ] Stack capture and symbol identity are validated when stack claims are made.
- [ ] Missing-event conclusions account for enablement, filters, rundown, clocks, buffers, and consumer backpressure.
- [ ] Owned providers/sessions are disabled/stopped and the consumer exits on an exact bounded signal.
