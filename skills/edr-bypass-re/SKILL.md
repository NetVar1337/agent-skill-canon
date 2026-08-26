---
name: edr-bypass-re
description: |
  Use when reverse engineering and measuring EDR, Defender, AV, or XDR behavior pinned to a Windows build, vendor, sensor, and policy: user-mode hooks, kernel callbacks, minifilters, WFP, ETW/ETW-TI, AMSI, memory scanners, and cloud ingestion. Treat unhooking, direct or indirect syscalls, ETW/AMSI patches, call-stack spoofing, sleep masks, and process injection as falsifiable bypass hypotheses with sensor-health, clean-baseline, positive-control, event-loss, delayed-verdict, and rollback evidence. Maps to MITRE ATT&CK T1562 Defense Evasion.
  Trigger keywords: EDR bypass, AV bypass, AV evasion, unhook, direct syscall, indirect syscall, Hell's Gate, Halo's Gate,
  Tartarus Gate, ETW patch, AMSI patch, call stack spoofing, hardware breakpoint Blindside, MITRE T1562,
  ntdll unhook, kernel callback, CrowdStrike bypass, Defender bypass, Sentinel One bypass, Elastic Defend,
  Sysmon evasion, PPID spoof, Sleep mask, Process Hollowing, Reflective DLL, sensor health, minifilter, WFP, XDR.
---

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read `../field-journal/precedent-reverse.md` — confirm that this skill's operations are authorized routine operations
2. `NOW`: confirm whether the current task falls within this skill's scope
3. `NEXT`: read `../tool-index.md`, verify tool availability and actual paths
4. `NEXT`: when tools are missing, invoke bootstrap; do not guess paths
5. `ACT`: enter step 1 of the "Workflow" and execute; do not stop at the confirmation stage

# EDR Bypass: From Reversing the Defenders' Implementations to Red Team Bypasses

> Authorized red teaming / adversary emulation / own-product testing only; use against unauthorized targets is forbidden.

## Scope

Use this skill when a red team / adversary emulation needs to deliver an implant onto an authorized target host and evade a modern EDR.

1. **Red team / purple team / adversary emulation** — the customer wants to assess the real detection capability of their SOC and EDR
2. **In-house implant / C2 framework development** — developing payloads for testing your own products, needing to bypass your own or the target EDR
3. **EDR product evaluation** — objectively evaluating an EDR's detection coverage after compliance boundaries are confirmed
4. **CTF / attack-defense exercise Windows-side breakouts** — needing reliable execution on hardened hosts during a competition

**Not applicable scenarios**:

- Antivirus vendors doing a full RE of their own product to produce a commercial assessment for customers (seek formal vendor partnership)
- AV evasion against unauthorized targets (illegal)
- AV evasion for ordinary viruses/trojans (this skill focuses on red team OPSEC and does not teach malware authoring)

### Division of Labor with Other Skills

| Scenario | Use |
|------|--------|
| Full-chain offense and defense (from external network to domain controller) | `attack-chain/` |
| Internal network lateral movement / AD attacks | `pentest-tools/network-attack-defense.md` |
| Delivering an implant past an EDR on a specific host | **this skill** |
| Purely static AV evasion (obfuscation / packing) | `malware-analysis/` (reverse perspective) |

`attack-chain` covers the complete kill chain; this skill focuses only on the internals of **the EDR as a single adversary** and targeted bypasses.

## Core Principle

```text
The EDR's four main monitoring surfaces           Candidate experiment surfaces
─────────────────────              ─────────────────────
user-mode ntdll hooks       ◄──►   unhook (Peruns Fart / fresh ntdll)
                                  indirect syscalls / Hell's Gate
                                  hardware breakpoint Blindside

kernel callbacks             ◄──►   call stack spoof
(Ps/Cm/Ob families)                  use legitimate trigger chains (don't bypass directly; combine with upstream stealth)

ETW telemetry                ◄──►   EtwEventWrite patch
(Microsoft-Windows-Threat-          NtTraceControl to disable the provider
 Intelligence etc.)                 AmsiContext handled in sync

AMSI scanning                ◄──►   AmsiScanBuffer patch (mov eax,0x80070057; ret)
(amsi.dll)                          hardware breakpoint bypass
                                    reflectively load a copy of amsi.dll
```

Every arrow above is a hypothesis to measure on one pinned product/build, not a recipe or a claim that the named change suppresses the corresponding sensor.

Key insights:

- **An EDR is not a black box** — the key hooks / callbacks / providers can all be reversed with IDA + windbg
- **Telemetry layers must be correlated** — a local unhook or AMSI result says nothing by itself about ETW, callbacks, memory scanning, or cloud/XDR outcomes
- **There is no cross-product fixed order** — state expected telemetry and a disproof condition for each change; derive ordering from the measured dependency graph for this build/vendor instead of assuming ETW → AMSI → unhook
- **Modern EDRs have made ETW + kernel callbacks the main battleground**; purely user-mode unhooking has long been insufficient

## Workflow

### Step 1: Identify the Target Host's EDR

```powershell
# List common EDR / AV services
Get-Service | Where-Object {$_.Name -match 'CSAgent|SentinelAgent|elasticendpoint|esets|ekrn|MsMpEng|wdsvc|cyserver|sysmon|aswbidsagent'}

# List loaded minifilters
fltmc filters

# List registered kernel callbacks (needs windbg + kernel debugging / or use PChunter / DRVHV)
# !object \Callback
# !pnpcallback / Process / Thread / Image
```

See the top of `references/hook-survey.md` for the EDR fingerprint table.

### Step 2: Extract the Hook Table from the EDR DLL

1. Attach to a process injected with the EDR's user-mode component (any landed process)
2. In windbg, dump the current `ntdll.dll` `.text` section
3. Diff it against a clean `C:\Windows\System32\ntdll.dll` on disk
4. The mismatches are the hook points

Or use `pe-sieve` directly:

```powershell
pe-sieve64.exe /pid 1234 /shellc 3 /modules 3 /dir hooks_dump
```

Detailed methods are in `references/hook-survey.md`.

### Step 3: Build a Bypass-Hypothesis Matrix

The table creates experiments, not portable recommendations. Every row requires a healthy sensor, a positive control, one changed variable, and a post-rollback positive control.

| Defense point | Candidate hypothesis to prove or disprove |
|--------|---------|
| ntdll inline hook | Does indirect syscall + dynamic SSN change this sensor's call-chain evidence? |
| ETW-TI provider | Does an `EtwEventWrite` change affect the target event while provider/session health remains intact? |
| AMSI (PowerShell / .NET) | How do an `AmsiScanBuffer` patch or HWBP affect AMSI and memory scanning separately? |
| kernel callback | Is a spoofed stack/legitimate trigger still correlated by callbacks, minifilters, or WFP? |
| Sysmon ProcessCreate | Does PPID metadata change Event ID 1 while other process-lineage evidence remains? |

### Step 4: Implement One Hypothesis in the Lab Implant

Change one measured edge only, keep a byte-for-byte rollback artifact, and define the expected local and cloud observations before execution. See `references/unhook-techniques.md` and `references/telemetry-blinding.md` for candidate code skeletons; they are not build/vendor guarantees.

### Step 5: Validate in a Local Sandbox

```powershell
# Deploy the target EDR trial in an isolated environment (Defender is fine to start with)
# Enable Sysmon + olaf-config
sysmon64.exe -i sysmonconfig.xml

# Run the implant and check whether it trips these alert sources:
#   - Defender AMSI
#   - ETW-TI
#   - Sysmon Event ID 1/7/8/10
#   - EDR console
```

## Build- and vendor-pinned telemetry experiments

### 1. Create the identity manifest

Pin OS build/KB, VBS/HVCI, target process plus `ntdll`/`amsi` hashes, EDR agent/service/driver/minifilter versions and signatures, policy ID/update time, cloud tenant/connectivity, capture-tool versions, and UTC clock source.

```powershell
Get-ComputerInfo | Select WindowsVersion,OsBuildNumber,OsArchitecture
Get-CimInstance Win32_DeviceGuard | Format-List *
Get-CimInstance Win32_SystemDriver | Select Name,State,PathName,StartMode
fltmc.exe filters
netsh.exe wfp show state file=C:\lab\wfp-state.xml
logman.exe query providers > C:\lab\providers.txt
Get-MpComputerStatus | Format-List *
Get-FileHash $env:SystemRoot\System32\ntdll.dll,$env:SystemRoot\System32\amsi.dll
```

Export or capture the vendor console's policy revision, sensor ID, last-seen, and content/model version. A running local service does not prove healthy cloud ingestion.

### 2. Map every telemetry layer

| Layer | Concrete API/structure | Evidence required |
|---|---|---|
| User-mode hooks | PE `.text`/IAT/EAT, `Nt*` stubs, loader notifications, stack capture | byte diff against the same-hash disk image, hook owner, before/after stack and return semantics |
| Kernel callbacks | `PsSetCreateProcessNotifyRoutineEx`, `PsSetLoadImageNotifyRoutine`, `OB_CALLBACK_REGISTRATION`, `CmRegisterCallbackEx` | owner, altitude/order, observed object, pre/post data, effective token |
| Minifilter/WFP | `FLT_REGISTRATION`, operation callbacks/altitude; `FWPM_*` state, `FwpsCalloutRegister*` | file/network operation, layer/callout/filter ID, process/token, permit/block result |
| ETW/ETW-TI/AMSI | provider GUID, `EVENT_TRACE_PROPERTIES`, `EnableTraceEx2`, keyword/level, `AmsiScanBuffer` | enable state, schema, activity/process/thread correlation, loss counters, AMSI result |
| Memory scanner | VAD/type/protection/backing, working set, thread start/stack, scan cadence | allocate→write→protect/map→execute→sleep/wake timeline and actual scan result |
| Cloud/XDR | sensor queue, event/alert/case ID, policy revision, ingest/detection timestamps | local-to-cloud correlation ID and latency; “not visible yet” is not no detection |

Route callback/minifilter/WFP internals to `kernel-callbacks`/`kernel-dev`; route provider, WPP, TraceLogging, and buffer-loss engineering to `windows-telemetry-etw`.

### 3. Prove sensor health with controls

1. **Clean baseline:** with no sensor/implant modification, execute the same harmless uniquely marked action and preserve local ETL, agent logs, network, and cloud events.
2. **Positive control:** use a vendor-supported test alert. EICAR proves only the file-AV path, not behavior, ETW-TI, memory, or XDR. Record alert/event ID and end-to-end latency.
3. **Technique run:** change one variable; keep input, process tree, modules, network action, and capture boundaries equal to baseline.
4. **Delayed verdict:** follow event/correlation state until an explicit verdict or the predeclared bounded vendor SLA; record ingestion and detection latency separately.
5. **Rollback:** restore bytes/hooks/policy/module lifecycle, verify process/driver/filter/session state, then repeat the positive control.

“Alert not observed” is usable only when positive controls before and after succeed, loss is zero or quantified, policy is unchanged, and cloud last-seen is healthy. It remains scoped to this build/vendor/policy.

### 4. Technique survival matrix

Each row records `technique + component hash + build/vendor/policy + hypothesis + expected local event + expected cloud result + baseline + positive control + modified result + delayed verdict + rollback + residual artifacts`.

Classify local block, payload failure, local-telemetry-only, cloud-telemetry-only, real-time alert, delayed alert, cross-layer correlation, unhealthy sensor, event loss, inconclusive, and no observed delta on this build/policy separately. Never flatten these states into one bypass/pass column.

### 5. Correlate implant lifecycle

Build a timeline for `bootstrap/config -> allocate -> write -> protect/map -> thread/APC/callback -> task/module/BOF -> sleep -> wake -> reconnect -> unload/update`. At each phase retain API/NT transition, memory type/protection/backing, thread start/stack, module ownership, ETW/callback/minifilter/WFP events, and cloud correlation ID.

- Route runtime/job/module ownership to `c2-implant-engineering`.
- Route COFF relocations, Beacon API shims, and section cleanup to `bof-coff-development`.
- Route generic loader/shellcode mechanics to `offensive-shellcode`.
- Route Linux kernel, eBPF, and host work to `linux-kernel-exploitation`, `ebpf-offensive`, and `linux-host-post-exploitation`.

Evidence output: `identity.md`, `policy.json`, `telemetry-map.csv`, `controls.md`, `survival-matrix.csv`, `timeline.csv`, ETL/agent/cloud exports, module hashes, and rollback verification.

### Step 6: Delivery Gate

Do not leave the sandbox until controls and rollback pass. Select path, process tree/PPID, memory lifecycle, and transport from this vendor/build's survival matrix rather than assuming a “legitimate” directory or `explorer.exe` parent suppresses telemetry. Route the approved delivery chain to `attack-chain`.

## Typical Scenarios

### Scenario 1: Delivering a cobalt-strike-alike beacon past Defender + Sysmon

```text
Target: Windows 11 Enterprise + Defender (cloud protection on) + Sysmon (olaf config)
Requirement: classify local and cloud observations while preserving beacon function, sensor health, and rollback

Hypotheses to test one at a time (not a portable recipe):
  1. Does encrypted-at-rest shellcode alter file, memory, or cloud results?
  2. For PowerShell delivery, how does an AMSI change affect AMSI versus later memory scans?
  3. Does an EtwEventWrite change alter the intended provider event without event loss or sensor failure?
  4. Does indirect syscall + Halo's Gate change hook/stack evidence while kernel telemetry remains?
  5. Does PPID metadata change process-lineage correlation or only one displayed field?
  6. Does Ekko/Foliage change sleep-memory observations across the scanner cadence?
```

### Scenario 2: EDR Sleep Mask on an Already-Landed Low-Privilege Shell

```text
Precondition: a medium IL shell was obtained via phishing; the EDR is watching
Risk: long dwell times make beacon signatures easy to find via memory scanning

Hypothesis set:
  1. Compare the existing allocation/protection lifecycle with an explicit no-new-RWX variant
  2. Instrument an Ekko candidate around WaitForSingleObjectEx/CreateTimerQueueTimer and preserve scanner events
  3. Prove wake restoration and exception/unwind behavior before measuring the telemetry delta
  4. Compare captured stacks before/after a stack-shaping candidate; do not assume the sensor uses RtlCaptureStackBackTrace
```

## On-Demand Bootstrap

### Tool Dependencies

| Tool | Purpose | Auto-installable |
|------|------|-----------|
| pe-sieve | Detect hooks / injections in a process | ✓ |
| API Monitor v2 | Dynamically observe API calls and hooks | Semi-auto (manual download) |
| SysWhispers3 | Generate direct / indirect syscall stubs | ✓ (git clone + python) |
| Hell's Gate POC | Reference implementation for dynamic SSN resolution | ✓ (git clone) |
| windbg + IDA | Statically reverse EDR DLLs / kernel callbacks | ✗ (install yourself) |
| Sysmon + olaf config | Local validation environment | ✓ |

### Bootstrap Command

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "&lt;SKILL_ROOT&gt;\skills\scripts\bootstrap-reverse.ps1" -Capability @('pe-sieve','syswhispers3','sysmon') -StartServices
```

## Routing Context

**New sibling routes**:

- Batch A: `bof-coff-development`, `windows-rpc-com-attack`, `windows-telemetry-etw`, `hyper-v-offensive`
- Batch B: `linux-kernel-exploitation`, `c2-implant-engineering`, `ebpf-offensive`, `linux-host-post-exploitation`
- Windows callbacks/minifilters/WFP: `kernel-callbacks`, `kernel-dev`; implant/BOF lifecycle: `c2-implant-engineering`, `bof-coff-development`

**Upstream entry points**:

- `reverse-engineering/` — first understand the EDR DLL / driver implementation
- `attack-chain/` — decide at which kill-chain stage to bring in this skill

**Related siblings**:

- `pentest-tools/network-attack-defense.md` — how to coordinate this skill during intranet lateral movement
- `malware-analysis/` — the reverse perspective, seeing how detection teams write rules
- `field-journal/` — write experience back after each engagement

**Downstream deliverables**:

- When generating reports, cite MITRE ATT&CK **T1562 (Impair Defenses)**, T1562.001 (Disable or Modify Tools), T1562.006 (Indicator Blocking), T1055 (Process Injection), T1027 (Obfuscated Files or Information)

## Legal Boundary Statement

- Authorized red teaming / adversary emulation / own-product testing only
- Written authorization (SoW / test contract / SRC scope statement) must be obtained before operating
- Must not be used against unauthorized targets or beyond the authorized scope
- Report critical findings to the customer immediately; follow responsible disclosure
- All real target information in reports must be sanitized (IP / hostname / domain / credential placeholders)

## References

- Detailed hook survey: `references/hook-survey.md`
- unhook / syscall techniques: `references/unhook-techniques.md`
- ETW / AMSI / anti-forensics: `references/telemetry-blinding.md`
- MITRE ATT&CK T1562: <https://attack.mitre.org/techniques/T1562/>


## Task Completion Self-Check (MUST pass before claiming completion)

- [ ] Did I execute every step of the workflow (rather than just reading it)?
- [ ] Did I use real tool paths based on `tool-index`?
- [ ] Did I produce reproducible evidence (commands/scripts/screenshots/reports)?
- [ ] Did I complete and write back the Checklist items required by RULES?
