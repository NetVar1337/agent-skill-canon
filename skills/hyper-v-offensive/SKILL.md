---
name: hyper-v-offensive
description: Use when auditing, reverse engineering, fuzzing, or triaging Hyper-V partitions, hypercalls, synthetic MSRs, SynIC, VMBus channels, VSP/VSC devices, vmwp.exe, vmcompute.exe, Host Compute Service, saved state, enlightened I/O, nested virtualization, root-partition boundaries, or Hyper-V escape research.
license: MIT
---
# Hyper-V offensive research

## When to use

Use this skill for Microsoft Hyper-V-specific guest-to-root, worker-process, synthetic-device, partition, saved-state, and management-plane boundaries.
Use it when the task names Hyper-V, a child or root partition, virtual processor, hypercall page, synthetic MSR, SynIC/SINT, VMBus, GPADL, ring buffer, VSP/VSC, `vmwp.exe`, `vmcompute.exe`, HCS, enlightened I/O, nested virtualization, VMRS/VMCX, or a Hyper-V host crash.
Use it to build replayable guest-driver, hypercall, VMBus packet, device-state, and HCS harnesses while preserving the next boundary after initial code execution.
Do not use generic VM-escape assumptions in place of the Hyper-V Top-Level Functional Specification, target-build symbols, protocol captures, and paired host/guest evidence.
A production result pins the complete host/guest stack, names the exact trust transition, records protocol state before mutation, captures root-side effects or dumps, and restores a verified clean checkpoint.

## Completion standard

Record host Windows build, hypervisor binaries, root-partition drivers, worker/compute services, symbols, firmware, CPU virtualization features, VBS/VTL state, VM generation/configuration, guest build/kernel/integration components, and all relevant hashes.
Identify the ingress surface and the first parser/dispatcher in the root partition, then trace to the final kernel, worker, device-emulator, service, or management sink.
For VMBus work, recover offer/open/GPADL/ring/channel state and message ownership before mutating packets.
For hypercalls, derive call layout and status meanings from the matching TLFS/build and record GPAs, VP, repetition fields, and result.
For every crash or anomalous result, preserve deterministic input, operation sequence, host/guest time correlation, module-relative stack, reset/recovery behavior, and remaining sandbox boundary.

## Core workflow

### 1. Build a nested, recoverable laboratory

Use a disposable outer host or nested Hyper-V lab for root-partition crash work.
Separate the research controller from the Hyper-V host so a host bug cannot destroy the operation log or only copy of a dump.
Keep host management reachable over an isolated management interface that guest traffic cannot route to.
Enable complete or active kernel dumps on the research host, verify pagefile/dedicated dump configuration, and test one manual crash before fuzzing.
Use checkpoints only as replay aids; retain immutable base disks and export critical VM configuration before mutation.
Do not attach production virtual switches, storage, GPU, USB, or management credentials to the research VM.

Create a case record on the Hyper-V host:

```powershell
$Case = 'C:\lab\hyperv\case-001'
New-Item -ItemType Directory -Force "$Case\evidence", "$Case\artifacts", "$Case\dumps" | Out-Null
Get-ComputerInfo | Select-Object WindowsProductName,WindowsVersion,OsBuildNumber,OsArchitecture,HyperVisorPresent | Format-List | Out-File "$Case\evidence\host-platform.txt"
Get-WindowsOptionalFeature -Online -FeatureName Microsoft-Hyper-V-All | Format-List | Out-File "$Case\evidence\hyperv-feature.txt"
Get-CimInstance Win32_DeviceGuard | Format-List * | Out-File "$Case\evidence\deviceguard.txt"
Get-VMHost | Format-List * | Out-File "$Case\evidence\vmhost.txt"
Get-VM | Select-Object Id,Name,State,Generation,Version,ProcessorCount,MemoryAssigned,Uptime | Export-Csv "$Case\evidence\vms.csv" -NoTypeInformation
Get-VMSwitch | Select-Object Id,Name,SwitchType,NetAdapterInterfaceDescription | Export-Csv "$Case\evidence\switches.csv" -NoTypeInformation
```

Record CPU vendor/model/microcode, SLAT, IOMMU, Secure Boot, Credential Guard, HVCI, VBS, nested-virtualization state, NUMA, and VM configuration version.
Record whether the host uses Intel `hvix64.exe` or AMD `hvax64.exe` and preserve its hash/version when accessible.

### 2. Pin every Hyper-V component

Inventory the root-partition files most relevant to the selected path:

```powershell
$Files = @(
 'C:\Windows\System32\vmwp.exe',
 'C:\Windows\System32\vmcompute.exe',
 'C:\Windows\System32\drivers\vid.sys',
 'C:\Windows\System32\drivers\vmbkmcl.sys',
 'C:\Windows\System32\drivers\vmswitch.sys',
 'C:\Windows\System32\hvix64.exe',
 'C:\Windows\System32\hvax64.exe'
) | Where-Object { Test-Path $_ }
$Files | ForEach-Object {
  $Item = Get-Item $_
  $Hash = Get-FileHash $_ -Algorithm SHA256
  $Sig = Get-AuthenticodeSignature $_
  [pscustomobject]@{Path=$_;SHA256=$Hash.Hash;FileVersion=$Item.VersionInfo.FileVersion;ProductVersion=$Item.VersionInfo.ProductVersion;Signer=$Sig.SignerCertificate.Subject;Signature=$Sig.Status}
} | Export-Csv "$Case\evidence\host-modules.csv" -NoTypeInformation
```

Add device-specific VSP drivers, network/storage stack modules, HCS/HNS components, integration services, and third-party filter drivers that lie on the tested path.
Capture service identity and process command lines for `vmcompute`, `vmms`, `vmwp`, HNS, and device-specific brokers.
Map each `vmwp.exe` instance to a VM ID using process command line, ETW, WMI/CIM, or debugger evidence; never rely only on start order.
For the guest, record OS/kernel, architecture, generation, firmware, Secure Boot template, integration component versions, synthetic-device drivers, initramfs, and kernel config.
For a Linux guest, retain `uname -a`, `/proc/cpuinfo`, relevant `/sys/bus/vmbus/devices` trees, module hashes, and exact kernel package/debuginfo/BTF.
For a Windows guest, retain build, `msinfo32`, device/driver inventory, integration-service state, and symbols.

### 3. State the boundary graph

Write one concrete graph before opening a debugger:

```text
guest user input or guest kernel request
  -> VSC / hypercall / synthetic register / VMBus packet
  -> hypervisor or root VID/VMBus/VSP dispatch
  -> root kernel driver and/or vmwp worker/device model
  -> vmcompute/HCS/management service when applicable
  -> host resource or second sandbox boundary
```

Name the weaker actor, bytes or state controlled, parser, ownership transition, stronger identity, and intended policy.
A `vmwp.exe` compromise is not automatically root-partition kernel compromise; record its token, mitigations, job/silo, handles, device access, and IPC boundary.
A root-kernel memory corruption is not automatically hypervisor compromise; the hypervisor/root partition boundary still exists.
An HCS authorization defect is a management-plane issue even if the target object is a VM.

### 4. Establish Hyper-V interface truth with CPUID and TLFS

From a guest kernel harness, query the hypervisor CPUID range beginning at `0x40000000`.
Record maximum leaf, vendor signature, interface signature, version, feature bits, recommendations, implementation limits, and nested-virtualization leaves that the guest can actually observe.
Check the hypervisor-present bit in architectural CPUID but do not infer individual features from it.
Pin the Hyper-V TLFS revision used to decode CPUID, MSRs/registers, hypercall input/output, status values, message formats, and partition privileges.
Treat reserved bits as required-zero unless the pinned specification states otherwise.
Compare documented availability with observed CPUID feature/privilege bits before touching a synthetic MSR or issuing a hypercall.
Do not execute privileged `rdmsr/wrmsr` or hypercalls from guest user mode; put the minimal operation in a signed/test guest driver or Linux kernel module with strict IOCTL/sysfs input validation.

Common x64 synthetic MSRs include guest OS identity, hypercall page, VP index, time reference, reference TSC, SynIC control/version, SIEFP, SIMP, EOM, and SINT registers.
Use symbolic constants from the pinned TLFS or current kernel headers; record numeric register IDs in the operation log only after version validation.
On ARM64 or newer register-based interfaces, follow the architecture-specific TLFS calling convention instead of assuming x64 MSRs.

### 5. Initialize and call the hypercall interface safely

Read the hypercall control MSR/register and record existing enable state plus guest-physical page number before changing it.
Allocate a guest-physical, page-aligned, executable page through the guest kernel's supported DMA/page APIs when the interface requires a hypercall page.
Write the guest physical page number and enable bit according to the TLFS, then verify the hypervisor populated/accepted the page before executing it.
Allocate input/output pages with known GPAs, correct alignment, zeroed reserved fields, and explicit lifetime ownership.
Encode the input-control value using the pinned definition for call code, fast-call flag, variable header size, repetition count, and repetition start index.
Issue only a documented discovery/control hypercall first and compare returned `HV_STATUS` plus repetition-completed count with the expected result.
For repetition calls, validate `reps_completed <= rep_count` and resume only according to documented semantics.
Record current VP index, partition context, input/output GPA, control value, page hashes before/after, status, and host trace marker.
Never reuse freed guest pages while the hypervisor or root component can still reference their GPAs.
On teardown, disable only state owned by the harness, flush execution as required, and release pages after all calls complete.

A hypercall operation record should contain:

```text
seed, guest build, VP index, call code, fast/slow mode, control value,
input GPA/size/hash, output GPA/size/hash, repetition start/count/completed,
HV_STATUS, guest exception, host event/dump ID, cleanup state
```

### 6. Map partitions, VPs, and translations

Distinguish root, child, and any nested partition; identify which component owns partition creation and policy.
Record partition ID, VP count, VP index, virtual NUMA, intercept configuration, synthetic feature exposure, and lifecycle state where observable.
Treat GPA, GVA, system physical address, and host/root virtual address as different namespaces.
For every translated address, preserve source namespace, partition, VP/CR3 where relevant, page size, access type, translation method, and captured mapping lifetime.
Account for large pages, sparse GPA mappings, ballooning/dynamic memory, hot add/remove, device DMA mappings, IOMMU remapping, and saved-state restore.
Do not reuse a translation across a reset, checkpoint restore, migration, dynamic-memory change, or GPADL teardown without revalidation.
Use debugger extensions or symbol-backed VID structures only on the exact host build and normalize all pointers to module/type provenance.

### 7. Initialize and observe SynIC

Map the SynIC message page and event-flags page with correctly aligned GPAs and enable bits according to the target VP's feature set.
Record `SCONTROL`, `SVERSION`, `SIMP`, `SIEFP`, every configured `SINT`, auto-EOI behavior, vector, masking, polling, and ownership.
A SynIC message page has per-SINT message slots under the TLFS contract; validate slot size/count from the pinned revision instead of assuming a copied layout.
Parse each `HV_MESSAGE` header with checked payload length and message type before following payload fields.
Clear or acknowledge message slots using the specified message/EOM protocol; an incorrect sequence can suppress later notifications and invalidate a fuzz result.
For event flags, record the connection ID/flag number mapping and use atomic ordering required by the guest driver and protocol.
Correlate synthetic interrupt delivery to VP, SINT, message/event source, ISR/DPC, and VSC worker.
Exercise masked/unmasked, polling/interrupt, empty/nonempty, and teardown states before mutations.

### 8. Inventory VMBus offers and channel state

From a Linux guest, preserve VMBus devices and identifiers:

```bash
case_dir=/var/tmp/hyperv-case-001
mkdir -p "$case_dir"
uname -a > "$case_dir/uname.txt"
find /sys/bus/vmbus/devices -maxdepth 2 -type f -print -exec sh -c 'printf "--- %s\n" "$1"; cat "$1" 2>/dev/null' sh {} \; > "$case_dir/vmbus-sysfs.txt"
lsmod | grep '^hv_' > "$case_dir/hyperv-modules.txt"
modinfo hv_vmbus hv_netvsc hv_storvsc 2>&1 > "$case_dir/module-info.txt"
```

Record interface type GUID, instance GUID, child relid, connection ID, monitor ID, subchannel index, target VP, offer flags, user-defined bytes, and owning guest driver.
Correlate the offer with the root-side VSP, worker process when present, and device instance.
Trace contact/initiate-contact, version negotiation, offers delivered, open request/result, rescind, unload, pause, reset, and close transitions.
Build a channel state machine and reject packet mutations when the harness is in the wrong state.
Record host and guest support for protocol versions; a parser path can differ materially by negotiated version.

### 9. Recover GPADL ownership

A Guest Physical Address Descriptor List grants the root-side endpoint access to guest pages for a channel operation.
Record GPADL handle, channel relid, range count, byte offset, byte count, PFN list, direction, creation messages, accepted result, users, and teardown result.
Check every range/count multiplication and make the harness enforce a configured page ceiling.
Keep pages pinned and mapped until GPADL teardown is acknowledged and all transactions referencing it complete.
Exercise create failure, partial message sequence, duplicate handle, use-before-accept, teardown with in-flight I/O, rescind, guest reset, and host-side cancellation as separate tests.
A guest freeing or repurposing pages early can create a harness-induced race that is not a root-side defect; prove ownership at the exact fault time.
Normalize GPADL-related root addresses back to channel/handle/PFN evidence rather than publishing bare pointers.

### 10. Parse VMBus rings and packets

Record inbound and outbound ring GPADLs, page counts, data size, feature bits, read index, write index, interrupt mask, pending-send size, and signaling method.
Take a coherent snapshot using the protocol's memory-ordering rules; do not read indices and data independently while another CPU advances them.
Validate index ranges, available-to-read/write arithmetic, wraparound, packet alignment, and reserved trailer semantics before parsing a descriptor.
The common packet descriptor family includes packet type, header offset in 8-byte units, total length in 8-byte units, flags, and transaction ID.
Check `offset8 <= len8`, convert units with overflow checks, and prove the descriptor plus payload lies within the ring's available bytes.
Classify in-band, GPA-direct, transfer-page, completion, cancellation, and additional device-specific packet types using build/protocol evidence.
For GPA-direct packets, validate range count and every GPA range against GPADL/channel ownership before interpreting device payload.
For transfer-page packets, validate transfer-page set ID, range count, offsets, lengths, and lifetime.
Track transaction IDs through request, completion, cancellation, reset, and duplicate completion.
Signal the peer only according to the ring/channel transition rules; over-signaling can change timing and hide lost-wakeup defects.

Maintain a packet ledger:

```text
direction, channel relid, ring generation, read/write indexes,
packet type, offset8, len8, flags, transaction ID, payload schema,
GPADL/range references, device state, host handler RVA, result
```

### 11. Recover VSP/VSC device protocols

Identify the VSC guest driver, VSP root module/process, interface GUID, negotiated protocol version, feature flags, and state machine.
Use open-source Linux Hyper-V drivers as one independent structure/protocol source, but validate Windows-host behavior and negotiated versions against the target build.
For storage, map channel setup, protocol negotiation, SCSI request/completion, sense data, SRB fields, scatter/gather ownership, reset, and hot-remove.
For network, map NVS negotiation, receive-buffer/send-buffer GPADLs, RNDIS messages, packet descriptors, checksum/LSO/RSS metadata, subchannels, switch extensions, and teardown.
For sockets, map service IDs, bind/connect/listen/accept, credit/window state, shutdown, reset, and host AF_VSOCK/HvSocket policy.
For HID/video/integration services, recover message type/version, payload bounds, host object lifetime, and worker-process versus kernel handling.
For each field, record byte offset, width, endian/unit, count/length source, legal state, producer, consumer, module/build evidence, and confidence.
Do not apply a Linux header layout to Windows root code until a valid packet and static/dynamic handler confirm it.

### 12. Trace guest input into the root partition

Use host ETW/WPR providers, Driver Verifier where appropriate, kernel debugging, and worker-process debugging to bridge the boundary.
Insert a case/seed marker into a benign companion channel or host log so guest and host operation records can be joined.
At the root dispatch point, preserve current process/thread, IRQL, module/RVA, channel/VM identity, packet length/type, GPADL references, and effective token if user mode.
Follow validated lengths and pointers to the first semantic parser and final sink.
For root-kernel paths, record locks, reference counts, IRPs, MDLs, DPC/work-item transitions, cancel/reset paths, and device removal state.
For `vmwp.exe`, record process mitigations, token, job/silo, loaded device-emulator modules, RPC/ALPC/handle boundaries, and crash-restart behavior.
For `vmcompute.exe`/HCS, record caller identity, schema version, resource GUID, authorization, service RPC/COM boundary, and downstream VM worker/kernel call.
A stack containing Hyper-V modules is not enough; tie it to the exact VM, channel, request, and seed.

### 13. Exercise management-plane HCS boundaries

Inventory HCS and VM state with supported PowerShell/CIM and `hcsdiag` tooling available on the pinned host:

```powershell
hcsdiag list | Out-File "$Case\evidence\hcsdiag-list.txt"
Get-Service vmcompute,vmms | Select-Object Name,Status,StartType | Format-List | Out-File "$Case\evidence\management-services.txt"
Get-VM | Select-Object Id,Name,State,Status,Version | Export-Csv "$Case\evidence\management-vms.csv" -NoTypeInformation
```

Use the documented Host Compute Service API from a typed harness for compute-system create/open/modify/start/shutdown/terminate/property operations.
Record HCS API/schema version, JSON document hash, caller token, target resource ID, service result, extended error JSON, and resulting VM/container state.
Validate object ownership and access control with a lower-privileged negative control; do not infer authorization from PowerShell cmdlet availability.
Keep VM configuration mutation separate from guest-originated packet fuzzing so a crash can be attributed to one ingress.
Treat HCS JSON fields, device paths, mapped directories, network endpoints, utility VMs, and saved-state references as boundary-controlled inputs requiring canonicalization and lifetime proof.
Route RPC/COM/ALPC transport internals of `vmcompute` to `windows-rpc-com-attack` after identifying the management operation.

### 14. Build deterministic mutation harnesses

Create separate harnesses for hypercalls, SynIC messages/events, VMBus control messages, ring packet descriptors, device payloads, saved state, and HCS documents.
Begin with a valid transaction and mutate one logical field while preserving protocol state, checksums, negotiated version, and unrelated bytes.
Partition cases by length/count arithmetic, descriptor offsets, PFN/range ownership, transaction IDs, unknown enum/flag values, reset/rescind races, duplicate completion, cancellation, hot remove, pause/resume, saved-state restore, and nested virtualization.
Use deterministic PRNG seeds and emit the complete operation sequence before issuing each request.
Use exact state signals such as channel-open result, completion packet, debugger breakpoint, ETW event, VM state transition, or HCS callback; never rely on a fixed sleep.
Bound request rate, outstanding transactions, GPADLs, pages, channel count, worker memory, and test duration.
After each case, prove expected completion, no orphaned GPADL/channel/context, VM responsiveness, host service health, and unchanged unrelated VMs.
After a root crash, let the external controller preserve dump and operation log before restoring the known snapshot.
Minimize by replaying the entire protocol prefix and reducing only one state transition or field at a time.

### 15. Fuzz and validate saved state

Record VM generation, configuration version, device set, checkpoint type, host build, and exact `.vmcx`/`.vmrs` identities before testing save/restore.
Create a clean checkpoint through supported management APIs:

```powershell
$VMName = 'hv-lab-guest'
$SnapshotName = 'case-001-clean'
Checkpoint-VM -Name $VMName -SnapshotName $SnapshotName
Get-VMSnapshot -VMName $VMName | Select-Object Id,Name,SnapshotType,CreationTime | Export-Csv "$Case\evidence\checkpoints.csv" -NoTypeInformation
```

Do not modify the only checkpoint or live production state file.
Prefer API-level device-state generation and an isolated copy parser before direct artifact mutation.
Map each serialized field to device/version/lifecycle evidence and validate length/count/offset/checksum before changing it.
Exercise save during in-flight requests, restore after device-version change, missing/extra device state, nested state, GPADL teardown, and hot-remove as controlled scenarios.
Restore through the supported API and record worker/service status, guest-visible device state, root-side parser path, and cleanup.
Delete only checkpoints created by the case and verify the base VM remains bootable.

### 16. Triage root-side failures

Configure host dump capture and symbol cache before the first case.
For a host bugcheck, preserve full dump, bugcheck parameters, blackbox data, module list, hypervisor flags, current process/thread, IRQL, locks, normalized stack, faulting instruction, VM/channel/seed correlation, and operation prefix.
Use WinDbg commands appropriate to the dump:

```text
!analyze -v
vertarget
lm t n
!thread
kv
!locks
!irpfind
!blackboxbsd
```

Load only build-matched symbols and record failures; normalize private/inferred functions to module SHA-256 plus RVA.
For worker/service crashes, preserve WER dump, process token/mitigations, loaded modules, exception context, heap state, handles, job/silo, and parent/service recovery event.
Classify outcome as guest-only failure, device reset, worker crash, service crash, root bugcheck, root code execution, worker code execution, management authorization flaw, or hypervisor failure.
State the remaining boundary explicitly after any code-execution result.
Reproduce outside the debugger with host ETW and external operation logging before claiming timing-independent behavior.

## Key structures & interfaces

The Hyper-V partition model contains root/child partitions, virtual processors, partition privileges, intercepts, GPA mappings, and synthetic interfaces.
`HV_HYPERCALL_INPUT`/control fields select call code, fast/slow mode, variable header, repetition count, and repetition start; use the pinned TLFS bit layout.
Hypercall output encodes `HV_STATUS` and repetition completion; reserved bits and partial completion require exact handling.
`HV_MESSAGE` contains message type, payload size, flags/pending state, sender/port context, and bounded payload under the target TLFS.
SynIC state includes `SCONTROL`, `SIMP`, `SIEFP`, `SINT` routing/mask/vector, EOM, per-VP message page, and event flags.
VMBus channel offers identify interface/instance GUIDs, relid/connection, monitor/subchannel/target VP, flags, and user-defined device data.
GPADL headers/body messages carry channel, handle, range descriptors, offsets/lengths, and PFNs across a multi-message lifecycle.
The VMBus ring header owns read/write indexes, interrupt mask, pending-send state, and feature bits shared across guest/root.
`vmpacket_descriptor`-style headers carry packet type, offset/length in 8-byte units, flags, and transaction ID.
GPA-direct and transfer-page descriptors add page/range references whose lifetime is outside the ring bytes.
HCS exposes versioned compute-system APIs and JSON schemas through `vmcompute`; preserve extended result documents and callback ordering.
VM configuration/state artifacts are versioned inputs to management and worker/device parsers, not generic files with portable offsets.

## Offset and provenance methodology

Record host static locations as `module path + SHA-256 + file version + PDB GUID/age + RVA`.
Record guest locations as `kernel/module build ID or PE hash + symbol provenance + section/RVA`.
Record protocol fields as `interface/protocol version + message/packet type + byte offset + width/unit + count source + producer/consumer + validation`.
Record synthetic register and hypercall layouts with TLFS revision, architecture, CPUID feature gate, and observed status.
Record GPA/PFN evidence with partition, channel/GPADL, page generation, ownership interval, and translation timestamp.
Record ring offsets relative to the coherent ring generation and preserved read/write indices, never as unqualified addresses.
For runtime root pointers, preserve the same-boot module map and normalize to RVA before comparing builds.
Triangulate private layouts from matched symbols, instruction access patterns, valid protocol captures, open-source guest drivers, and controlled field variation.
No root structure offset, VMBus field, VM state offset, hypercall assumption, or function signature transfers to a new host build without revalidation.

## Tooling

- Hyper-V PowerShell and CIM: VM/host identity, configuration, checkpoints, switches, integration services, and controlled state changes.
- `hcsdiag` and documented HCS APIs: compute-system inventory, typed management operations, extended errors, and lifecycle callbacks.
- Hyper-V TLFS: architecture-specific CPUID, synthetic registers/MSRs, hypercalls, SynIC, messages, and status contracts; pin revision.
- WinDbg/KDNET: root dump triage, worker debugging, VID/VMBus/VSP paths, symbols, IRQL, locks, IRPs, and normalized stacks.
- WPR/WPA/Xperf: Hyper-V, VMBus, storage, network, worker, service, scheduler, and crash-adjacent timelines.
- IDA/Ghidra/BinDiff: root driver, hypervisor, worker/device-emulator, HCS, and version-diff reconstruction.
- Linux kernel Hyper-V drivers/headers: observable VMBus/SynIC/GPADL protocol source and guest harness foundation, validated against target behavior.
- Driver Verifier: selected root/guest drivers in a disposable lab only, with verifier settings and resulting timing changes recorded.
- Wireshark/NetMon where supported: synthetic network/RNDIS views; retain raw ring/protocol evidence when dissectors are incomplete.
- External controller: case sequencing, exact-event waits, host liveness, dump collection, checkpoint restore, and artifact hashing.

## Pitfalls & OPSEC

- A Hyper-V host crash affects every child partition; never fuzz a shared or production host.
- Checkpoints do not protect artifacts stored only inside the crashing host; stream operation logs to an external controller.
- VBS/HVCI and Hyper-V can coexist with nested virtualization and change code paths; record VTL and feature state for every run.
- CPUID feature exposure, TLFS revision, host build, and guest integration version jointly define the usable interface.
- Bare host virtual addresses and undocumented structure offsets are not portable evidence; normalize and retain symbols/hash provenance.
- Early page reuse after GPADL or hypercall teardown can manufacture guest-side use-after-free behavior; prove acknowledgement and ownership.
- Ring races caused by incoherent snapshots or missing memory barriers are harness defects until reproduced with correct ordering.
- A malformed packet can desynchronize a stateful channel and contaminate later cases; reset to a proven state or recreate the channel.
- Worker-process compromise still faces token, job, silo, handle, IPC, and kernel boundaries; state them instead of claiming a complete escape.
- Root-kernel compromise still does not establish hypervisor/VTL1 compromise.
- Broad host tracing, Driver Verifier, and kernel debugging alter timing; retain a nondebug, verifier-off reproduction.
- Saved-state and HCS mutations are durable and can make a VM unbootable; use copies and verify checkpoint cleanup.
- Guest, host, and management logs can contain VM names, paths, network identifiers, and tenant data; isolate and classify evidence.
- Synthetic-device fuzzing can emit real network/storage operations through attached resources; use isolated switches and disposable virtual disks.

## Evidence outputs

```text
host-platform.md       build, CPU/microcode, VBS/VTL, firmware, dump policy
host-modules.csv       hypervisor/root/worker/HCS hashes, versions, symbols, signers
guest-platform.md      build/kernel, config, integration/VSC identities
vm-config/             exported configuration, device inventory, switch/storage scope
interfaces.md          CPUID/TLFS, hypercall, SynIC, VMBus and HCS version matrix
channels.jsonl         offers, open state, relid, GUIDs, GPADLs, rings, target VP
operations.jsonl       seed, state prefix, typed mutation, result, cleanup
traces/                 host/guest ETL, debugger logs, protocol/ring captures
crashes/                host/worker dumps, normalized stack, case/VM/channel correlation
snapshots.md           created/restored/deleted checkpoint IDs and base-boot proof
cleanup.md             channels/GPADLs closed, VM healthy, services healthy, no extra state
```

## Routing

- Use `bof-coff-development` when a Hyper-V probe is delivered as a BOF and the problem is COFF loading, relocations, or Beacon ABI.
- Use `windows-rpc-com-attack` for `vmcompute`/HCS RPC, COM, ALPC, authentication, impersonation, or typed management clients.
- Use `windows-telemetry-etw` for provider/session engineering, Hyper-V ETL, stack capture, loss measurement, and timeline correlation.
- Use `linux-kernel-exploitation` after a Linux guest or host kernel bug yields a generic memory primitive requiring allocator/mitigation work.
- Use `c2-implant-engineering` for implant protocol, job/module lifecycle, transport, reconnect, and long-lived agent behavior; do not place those concerns in a VMBus harness.
- Use `ebpf-offensive` for eBPF verifier/JIT or hook research in a Linux guest/host.
- Use `linux-host-post-exploitation` for evidence-driven operation after access to a Linux Hyper-V guest or management host.
- Use `virtualization-escape-research` for vendor-neutral device-emulation and escape methodology.
- Use `hypervisor-dev` for custom VMX/SVM VMM implementation, EPT/NPT, exits, and hypervisor architecture.
- Use `vbs-hvci-research` for VTL0/VTL1, Secure Kernel, HVCI, isolated user mode, and VBS trust boundaries.
- Use `hypervisor-memory-introspection` for coherent cross-partition translation and guest-object reconstruction.
- Use `windows-driver-0day` or `exploit-dev` after a root driver or worker crash has a proven controllable primitive.

## Final gate

- [ ] Host/guest builds, firmware, CPU, VBS/VTL, VM configuration, modules, symbols, and TLFS revision are pinned.
- [ ] Ingress, root dispatcher/parser, sink, stronger identity, and remaining boundary are named.
- [ ] Hypercall/SynIC/VMBus/HCS state is valid and recorded before mutation.
- [ ] GPADL, ring, transaction, page, and callback lifetimes remain owned through completion/teardown.
- [ ] Every field/offset has protocol/build provenance and checked width/count arithmetic.
- [ ] Mutations are single-variable, deterministic, event-synchronized, bounded, and externally logged.
- [ ] Host/worker crashes preserve dumps, module-relative stacks, VM/channel/seed correlation, and minimized replay.
- [ ] Checkpoints created by the case are restored/deleted deliberately and the clean base VM/host health is verified.
