---
name: reverse-skill-router
description: Routes ambiguous or cross-domain reverse engineering, exploit development, Windows internals, kernel, hypervisor, game and anti-cheat, EDR, malware, mobile, firmware, cloud, Active Directory, wireless, ZDI, browser, and documentation tasks to one deterministic PRIMARY skill plus justified secondary handoffs.
---
# Reverse Engineering Skills — Master Control

This directory hosts a collection of reverse-engineering skill modules. Each subdirectory is an independent module containing a `SKILL.md` describing its scope, toolchain, and workflow.

## CRITICAL: routing execution contract (execute immediately)

After reading this file, replying only "read/understood" is not allowed. Execute in order:

1. `NOW`: run the platform-native router (Windows `scripts/master-route.ps1`; Linux/macOS/Kali `scripts/master-route.sh`) to select the PRIMARY from `config/routing.json`; for hard cases also read the three-axis appendix in `routing.md`.
2. `NOW`: run the platform-native `case-init` to create `work/<case>/scope.md` for the current analysis project; **while auth is not granted, ACTing on the target is forbidden**. Local offline samples use the `offline-sample` preset + an explicit sample note; Force must not bypass the hard gates.
3. `ACT`: open the PRIMARY `SKILL.md` and execute its ACTION REQUIRED.
4. `NEXT`: tool paths come only from `tool-index.md`; missing tool → platform-native bootstrap (manifest only).
5. State conclusions as Evidence→Finding→Path. Reports/journal entries are a SHOULD unless the user wants a deliverable.

**Identity**: see `ops/IDENTITY.md` (lightweight routing pack + tool bootstrap + journal; **not** a Z3r0-style platform).

If routing cannot resolve a match, you MUST first research methodology online and propose a new skill — forcing a task into a mismatched module is forbidden.

## Instruction semantics (RFC 2119)

- `MUST`: mandatory; violating it fails the task.
- `MUST NOT`: forbidden; violating it is a security violation.
- `SHOULD`: do it in principle; skipping requires a stated reason.
- `MAY`: optional action.

## Deterministic routing precedence (Routing)

When artifact, objective, and platform point at different skills, resolve them in this order:

1. **Artifact / trust boundary:** identify the real object and boundary first (COFF, RPC, ETW, SYS, VMBus, eBPF, game client/server, cloud control plane).
2. **Objective:** distinguish understanding, implementation, discovery, turning a known bug into a primitive, evasion measurement, post-exploitation, and reporting.
3. **PRIMARY:** select exactly one skill that owns the next verifiable output; secondary skills receive a handoff only when its criterion is met.
4. **Platform/tool:** use these only to break ties within the same artifact and objective. A tool name never overrides the boundary.
5. **Unresolved:** select `reverse-skill-router` only when the first four axes cannot resolve; select `attack-chain` only for a multi-host kill chain.

| Artifact / trust boundary | Objective | PRIMARY | Secondary / handoff |
|---|---|---|---|
| Unknown Windows-native transaction | explain process/token/object/loader/memory behavior | `windows-internals` | `windows-0day-hunting` after a privileged-workflow hypothesis |
| Windows privileged broker/service | discover a trust-boundary flaw | `windows-0day-hunting` | `windows-rpc-com-attack`, then `exploit-dev` after a primitive |
| Windows `.sys`, IOCTL, or kernel crash | root-cause and exploit a driver flaw | `windows-driver-0day` | `exploit-dev`; `kernel-dev` only for implementation |
| WDM/KMDF source, IRP, PnP/power, or queue | build or fix a driver | `kernel-dev` | `windows-internals`, `driver-comm` |
| Known crash/bug with generic capability work | derive primitive and exploit chain | `exploit-dev` | allocator/platform specialist after the contract |
| COFF object, BOF ABI, relocations, Beacon imports | build/debug object or loader | `bof-coff-development` | `c2-implant-engineering`, `exploit-dev` |
| RPC/COM/DCOM/ALPC interface | recover/test authorization or IDL path | `windows-rpc-com-attack` | `windows-internals`, `windows-telemetry-etw` |
| ETW/WPP/TraceLogging provider or ETL | engineer schema/session/loss evidence | `windows-telemetry-etw` | `edr-bypass-re`, `windows-internals` |
| Hyper-V partition, hypercall, VMBus, VSP/VSC, HCS | Microsoft-specific virtualization research | `hyper-v-offensive` | `hypervisor-dev`, then `exploit-dev` after a primitive |
| Generic VMX/SVM VMM implementation | build a custom hypervisor | `hypervisor-dev` | `hyper-v-offensive` only for Hyper-V surfaces |
| Xeroxz/Bluepill type-2 (host GDT/TSS/IDT, self-ref PML4, VDM) | bring up or debug that lineage | `bluepill-type2-hv` | `hypervisor-dev` after first handled VMEXIT |
| QEMU/KVM/Proxmox guest identity | hide emulator from AC/packers | `qemu-anti-detection` | `hypervisor-detection` to score; `stealth-hypervisor` for a custom VMM |
| Guest VM/HV probe construction | detect QEMU/KVM/VMware/Hyper-V | `hypervisor-detection` | `stealth-hypervisor` / `qemu-anti-detection` to implement the lie |
| Intel SMM module / XHCI SMI / SPI implant | Plouton-class ring -2 framework | `plouton-smm` | `auditing-uefi-firmware-with-chipsec`, `secure-boot-uefi-research` |
| x86 lift to LLVM IR / MBA simplify | Mergen, Dna, GAMBA, Simplifier | `llvm-lift-deobfuscation` | `virtualization-deobfuscation` for VM bytecode; `binary-obfuscation-deconstruction` for CFF/opaque |
| Sogen / syscall-level usermode emu | run PE/ELF on real ntdll without a host OS | `sogen-usermode-emulator` | `kevlar-driver-emulation` for `.sys` |
| EPT/SLAT split-view hook detect | write-reflect / RDTSC / thread-race | `ept-hook-detection` | `hypervisor-detection`, `stealth-hypervisor` |
| Ring-1.io bootkit / bootmgfw implant | analyze Aftermath corpus + 2026 writeup | `ring-1-bootkit` | `hypervisor-memory-introspection`, `ept-hook-detection` |
| Linux kernel bug or proved kernel primitive | build a stable LPE chain | `linux-kernel-exploitation` | `exploit-dev`; `ebpf-offensive` if verifier/JIT-specific |
| eBPF verifier/JIT/program/map/link/hook | verifier research or instrumentation | `ebpf-offensive` | `linux-kernel-exploitation` after a generic primitive |
| Authorized Linux shell/host boundary | enumerate, escalate, persist, or pivot | `linux-host-post-exploitation` | kernel/eBPF/container owner at the discovered edge |
| Implant runtime, task protocol, module ABI, sleep/update | engineer or debug an agent | `c2-implant-engineering` | `bof-coff-development`, `windows-telemetry-etw` |
| Game engine/layout/client-server boundary | engine RE, instrumentation, protocol logic, exploit | `game-hacking` | `offset-dumper`, `network-protocol-re`, `anti-cheat-bypass` |
| EDR user/kernel/cloud sensor pipeline | product-pinned evasion measurement | `edr-bypass-re` | `windows-telemetry-etw`, `kernel-dev` |
| ZDI target decision | decide whether target/bug is eligible | `zero-day-target-eligibility` | `zdi-researcher-guidelines` after eligibility |
| Eligible vendor bug/disclosure package | reproduce, minimize, and submit | `zdi-researcher-guidelines` | platform exploit/reverse owner |
| Active Directory/Kerberos/AD CS | execute domain offense | `offensive-active-directory` | `windows-ad` for lightweight routing/reference |
| Cloud IAM/control plane | execute cloud offense | `offensive-cloud` | `cloud-k8s` for Kubernetes/container boundaries |
| Wi-Fi airspace discovery | map AP/client/channel/handshake surface | `offensive-wifi-recon` | `offensive-wifi`; `wifi-wireless` for scope-first routing |
| Wi-Fi exploitation lifecycle | authorized wireless attack workflow | `offensive-wifi` | `offensive-wifi-recon`, `wifi-wireless` |
| Artifact/objective/platform genuinely unresolved | produce the routing decision | `reverse-skill-router` | `case-review` if evidence quality is the blocker |

### Worked ambiguity cases

- A `.sys` crash plus “write an exploit” routes first to `windows-driver-0day` for root cause; hand off to `exploit-dev` only after a measured primitive. “Kernel” does not make `kernel-dev` primary.
- A missing EDR ETW event routes first to `windows-telemetry-etw` to prove provider/session/schema/loss; only a healthy sensor path moves to `edr-bypass-re`.
- A BOF that crashes on its second implant invocation routes to `bof-coff-development` for relocation/import/section cleanup, or to `c2-implant-engineering` when job cancellation/module ownership is the failing edge.
- Hyper-V guest input causing a root-side crash routes to `hyper-v-offensive`; `exploit-dev` begins after controllable bytes, lifetime, or read/write is proved.
- An eBPF verifier case causing a kernel UAF remains in `ebpf-offensive` until verifier/JIT behavior is pinned, then moves to `linux-kernel-exploitation` for the generic kernel chain.

Explicit execute/implement/continue intent runs through the selected workflow and evidence-backed handoffs without asking for repeated permission. A menu is valid only when materially different objectives remain unresolved.

## Current modules

| Module | Directory | Applicable scope |
|------|------|---------|
| **General RE** | `reverse-engineering/` | GDB / Frida / angr / Unicorn / Qiling / anti-analysis countermeasures / all-language-platform RE / CTF pattern library |
| **APK RE** | `apk-reverse/` | Android APK unpacking, jadx decompilation, smali editing, Frida hooks, repack-sign-install |
| **.NET / C# RE** | `dotnet-reverse/` | Managed PE RE, dnSpyEx + de4dot deobfuscation (ConfuserEx/SmartAssembly/Babel), IL patching, Sharp* red-team tool analysis, dnSpy MCP integration |
| **IDA Pro RE** | `ida-reverse/` | IDA Pro MCP HTTP server (72 tools): decompile, disassemble, dataflow tracing, cross-references |
| **Frontend JS RE** | `js-reverse/` | Browser-side signature hunting, encrypted-parameter analysis, runtime sampling, Node environment replay; prefer the existing `js-reverse_*`; bring in jshookmcp only when a stronger browser/CDP/hook surface is needed, and only after that MCP server is downloaded/registered/enabled |
| **radare2 analysis** | `radare2/` | CLI binary recon, disassembly, patching: r2 / rabin2 / rasm2 / radiff2 |
| **CTF entry** | `ctf-sandbox/` | Single PRIMARY; downstream remains in the sidecar `../CTF-Sandbox-Orchestrator/` |
| **Technical docs** | `docs-generator/` | Auto-generate RE reports, pentest reports, CTF writeups, signature RE reports after task completion |
| **Evidence graph review** | `case-review/` | Validate scope, Evidence→Finding→Path traceability, workitems, timeline, artifact hashes |
| **Browser & desktop automation** | `browser-automation/` | Browser operation (Playwright) + Windows desktop app operation (OpenReverse UIA/CUA) + network observation |
| **Cross-version symbol migration** | `binary-diff/` | Migrate symbols from an old version to a new one, infer without PDBs, bulk-migrate function names after updates |
| **N-day patch-diff→exploit** | `patch-diff-exploit/` | Locate the vulnerability from vendor patches, write PoCs, weaponize N-days (split vs binary-diff: this skill is the offensive side) |
| **RE→exploit chain** | `pwn-chain/` | From RE to a working exploit: stack/heap/kernel pwn, pwntools, libc-database, stabilizing CTF exploits into real-world remotes |
| **Firmware pentest chain** | `firmware-pentest/` | OWASP FSTM nine phases: extraction→EMBA automation→Firmadyne/QEMU emulation→AFL++ fuzzing→live-device exploitation |
| **EDR bypass RE** | `edr-bypass-re/` | Red-team scenarios: reverse the EDR's hook table/ETW/AMSI → direct syscalls / Hell's Gate / hardware breakpoints / call stack spoofing |
| **Pentest toolchain** | `pentest-tools/` | Nmap/Nuclei/SQLMap/FFUF/Hashcat/Pentest Swarm and 20+ pentest tools exposed to the AI via MCP |
| **Diagram generation** | `diagram-generator/` | Generate Mermaid/Graphviz/PlantUML diagrams from natural language (attack-path, data-flow, architecture, state machines) |
| **Attack-chain orchestration** | `attack-chain/` | Master planner for multi-stage attack paths; full pentests, HW exercises, external-to-DC campaigns start here |
| **LLM/AI security testing** | `llm-security/` | OWASP LLM + ASI Top 10: prompt injection, tool abuse, memory poisoning, agent hijacking, system-prompt extraction, **agent compliance engineering** |
| **API security testing** | `api-security/` | REST/GraphQL/WebSocket, all protocols: BOLA/IDOR, JWT/OAuth attacks, 10-phase methodology |
| **Supply-chain security** | `supply-chain-security/` | SBOM/SCA/CI-CD pipelines: dependency scanning, container security, build integrity, vulnerability reachability |
| **Mobile RE** | `mobile-reverse/` | Android + iOS: Frida/Objection dynamic instrumentation, SSL pinning/root/jailbreak detection bypass, OWASP MASTG |
| **Malware analysis** | `malware-analysis/` | Six-phase sample analysis, YARA/Sigma, anti-analysis detection, sandbox orchestration |
| **DSL VM RE** | `reverse-engineering/dsl-vm-reverse/` | JS custom-instruction-set VMs (IIFE + switch-case opcodes); risk-control/captcha engines etc. |
| **Ops contract** | `ops/` | Scope / evidence chain / roles / timeline / identity / skill supply-chain security |
| **Community skill comparison** | `references/community-security-skills.md` | External security skill index and borrowing rules (no blind installs) |
| **Skill supply chain** | `ops/skill-supply-chain.md` | External skill/MCP install gates (AST10 condensed) |
| **RE phase gates** | `reverse-engineering/references/re-agent-workflow.md` | triage→static→dynamic→synthesis |
| **Authorized recon pipeline** | `pentest-tools/references/recon-pipeline.md` | scope gate + hit≠verified |
| **Protocol RE** | `protocol-reverse/` | Custom binary protocols / Protobuf / gRPC / PCAP frame layouts |
| **Ghidra RE** | `ghidra-reverse/` | Open-source decompilation, headless, Ghidra MCP (main entry when IDA is absent) |
| **Cloud / containers / K8s** | `cloud-k8s/` | IMDS/IAM, container escape surface, Kubernetes RBAC |
| **Windows / AD** | `windows-ad/` | Kerberos, AD CS, BloodHound, relaying and domain paths |
| **Digital forensics** | `digital-forensics/` | Memory/disk timelines, PCAP tracing, IR preservation |
| **Code audit / SAST** | `code-audit/` | Semgrep/CodeQL, whitebox, dangerous-API and auth review |
| **Threat hunting** | `threat-hunting/` | Hypothesis-driven hunting, Sigma detection engineering, blue-team validation |
| **OT / ICS** | `ot-ics/` | Purdue zones, PLC/SCADA, passive-first assessment |
| **Wi-Fi / wireless** | `wifi-wireless/` | Authorized wireless assessment, handshakes/PMKID, lab rules |
| **Browser extension RE** | `browser-extension-reverse/` | Chrome/Firefox extensions, MV3 workers, permission surface |
| **macOS / Mach-O** | `macos-reverse/` | Signing, ObjC/Swift, LaunchAgents, macOS samples |
| **Thick clients** | `thick-client/` | Desktop C/S, local storage, IPC, update channels |
| **Go / Rust RE** | `go-rust-reverse/` | Stripped Go/Rust, pclntab, panic strings |
| **Hardware debug interfaces** | `hardware-security/` | UART/JTAG/SWD, read-only extraction, firmware handoff |
| **Database security** | `database-security/` | MySQL/PG/MSSQL/Mongo/Redis exposure and configuration |
| **Email security** | `email-security/` | Phishing teardown, SPF/DKIM/DMARC, BEC |
| **Identity federation** | `identity-federation/` | SAML/OIDC/OAuth SSO flows and misconfigurations |
| **RF / SDR** | `radio-sdr/` | Authorized RF research, receive-only by default |
| **Dynamic instrumentation (usermode)** | `frida-dbi/` | Frida hooks/Stalker/anti-anti-debug/il2cpp; plaintext capture and runtime offset validation |
| **WinDbg / TTD** | `windbg-ttd/` | KDNET kernel debugging, dump triage (!analyze/0x109), time-travel replay, cdb automation |
| **VBS / HVCI research** | `vbs-hvci-research/` | VTL1 boundaries, Secure Kernel/Ium attack surface, Credential Guard, technique survival table |
| **Secure Boot / UEFI** | `secure-boot-uefi-research/` | PK/KEK/db/dbx, BCD policy, BitLocker PCR binding, bootkit precedent classes |
| **Driver communication** | `driver-comm/` | IOCTL/shared-section/inverted-call design and RE; METHOD_NEITHER traps |
| **Kernel callbacks** | `kernel-callbacks/` | Ps*/Ob/Cm enumeration, ownership attribution, unlink vs proxy vs EPT hide |
| **Pattern scanning** | `pattern-scanner/` | Signature formats/mask design/Horspool+SIMD/uniqueness validation pipeline |
| **PE engineering** | `pe-tools/` | Header/directory parsing, rebuild, manual-map ordering, dump reconstruction, PDB GUID extraction |
| **Offset dumper** | `offset-dumper/` | UE/Unity/Source pipelines, build pinning, drift alerts, binding generation |
| **Network protocol RE** | `network-protocol-re/` | Capture-layer selection, framing/opcode discovery, crypto detection, replay+fuzz |
| **ImGui overlay** | `imgui-overlay/` | Internal present-hook vs external transparent window, input/DPI/detectability |
| **Stealth hypervisor** | `stealth-hypervisor/` | Detection-surface model, VMFUNC split-view, TSC discipline, nested virtualization |
| **Bluepill type-2 HV** | `bluepill-type2-hv/` | Xeroxz host GDT/TSS/IDT, self-ref PML4 map, VMX-root SEH, VDM |
| **QEMU anti-detection** | `qemu-anti-detection/` | zhaodice QEMU patches, libvirt SMBIOS/CPU XML, RDTSC-KVM-Handler |
| **Hypervisor detection** | `hypervisor-detection/` | Guest CPUID/FYL2XP1/vendor/WMI probes that score a hide job |
| **Plouton SMM** | `plouton-smm/` | Intel SMM module, XHCI SMI, SPI implant, Windows phys walk |
| **LLVM lift deobf** | `llvm-lift-deobfuscation/` | Mergen/Dna lift-to-LLVM, GAMBA/Simplifier MBA, Polaris inverse |
| **Sogen usermode emu** | `sogen-usermode-emulator/` | momo5502 syscall-level PE/ELF emu, WHP/Unicorn/KVM |
| **EPT hook detection** | `ept-hook-detection/` | Guest-side SLAT split-view probes (write/timing/thread) |
| **Ring-1 bootkit** | `ring-1-bootkit/` | Aftermath ring-1.io bootmgfw + Hyper-V SLAT inject analysis |

## Unified entry

For RE, CTF, traffic capture, frontend signature, APK repack, or binary-analysis tasks, enter in this order:

1. Platform-native router (Windows `scripts/master-route.ps1`; Linux/macOS/Kali `scripts/master-route.sh`) → PRIMARY (from `config/routing.json`)
2. Platform-native `case-init` → `scope.md`
3. Open the PRIMARY `SKILL.md`
4. For hard cases read `routing.md`; for local tool paths read `tool-index.md`

## Working approach

Combine modules as needed:

1. **Got a target** → check the file type first, pick the matching analysis tool
2. **Quick wins** → strings / rabin2 -z / ltrace for immediate leads
3. **Deep analysis** → decompiling → IDA; dynamic hooking → Frida; symbolic execution → angr
4. **Switch tracks when one stalls** → static fails → dynamic; Java layer fails → native .so; page observation isn't enough → breakpoints

## Conditional next-step menu pattern

Use a menu only for a **genuinely unresolved user decision**: several objectives remain valid, have materially different scope, and the artifact/objective precedence above cannot decide. With explicit autonomous, execute, continue, implement, or verify intent, `MUST` finish the PRIMARY phase and evidence-backed handoff; `MUST NOT` pause merely to display a menu.

When a menu is necessary, offer 3-6 numbered choices and state which objective or deliverable each changes:
- Each option numbered (1-6)
- Each option describes one concrete executable action (not an abstract direction)
- Include at least one "export report / write writeup" option
- Include at least one "keep digging / switch method" option
- When appropriate, include a "stop/pause/ask something else" exit

Example:
```
## Suggested next steps (pick a number)

1. Deep-decompile sub_140001000 and reconstruct the algorithm
2. Verify the parameter hypothesis with a Frida dynamic hook
3. Export the named functions and generate a symbol-migration YAML
4. Generate the report for this phase
5. Cross-check with a light radare2 recon pass
6. Pause — I want to re-check the evidence first
```

## The directory grows dynamically

This tree keeps growing. When you find a new subdirectory, reading its `SKILL.md` tells you what it's for.

When adding a new skill, follow the standard process in `CONTRIBUTING.md` so that:
- The routing matrix dispatches it correctly
- The bootstrap system can auto-provision dependencies
- tool-index reflects the new tool state

## Related resources

- A local **anything-analyzer** (port 23816) MCP server provides browser automation, HTTP capture, and AI analysis
- `tool-index.md` records whether local RE tools are available, their real paths, versions, and script references
- The `Readme.md` at the package root has generic install/integration instructions for Claude Code, Codex CLI, and other code-AI clients

## Bootstrap on demand

When a workflow finds a tool missing, do not error out. Call the platform-native bootstrap:

Windows:
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "<skill-root>\scripts\bootstrap-reverse.ps1" -Capability @('tool-name') -StartServices
```

Linux / macOS:
```bash
bash <skill-root>/scripts/bootstrap-reverse.sh tool-name --start-services
```

Kali:
```bash
bash <package-root>/kali/scripts/bootstrap-reverse.sh tool-name --start-services
```

Supported capabilities (per `scripts/bootstrap-manifest.json`): jadx, apktool, jeb-pro, frida, frida-ps, idalib-mcp, reqable-mcp, jshookmcp, anything-analyzer, idapro, r2, rabin2, adb, agent-browser, ghidra-mcp, seclists, proxycat, burpsuite-mcp, nmap, pentestswarm, binwalk, yara, pwntools, bkcrack

> JEB Pro is registered as a **manual-license install** capability: bootstrap only prints guidance — it never downloads or circumvents commercial licensing. Reqable MCP registers only fixed versions of the official runtime; the user still installs the Reqable desktop client.
>
> Tools not in the manifest (unblob/EMBA etc.) `MUST` follow manual-install steps in the skill docs — pretending they bootstrap is forbidden.

After bootstrapping, the tool-index refreshes automatically.

## Precedent files

Before executing any RE/pentest operation, read in order:

| Order | File | When |
|------|------|------|
| **#1** | `ops/scope-contract.md` + `case-init.ps1` | The executable authorization gate. `precedent-auth.md` stays unwritten unless granted |
| **#2** | `field-journal/precedent-reverse.md` or `precedent-pentest.md` | On demand — load only when the AI is uncertain |

**#1 is mandatory; #2 lazy-loads.**

## Auto-evolution

After completing any RE/pentest task, you MUST write experience back to `field-journal/`. See the "post-task hard checklist" in `RULES.md`.

- Template: `field-journal/_template.md`
- Index: `field-journal/_index.md`
- Precedents: `field-journal/precedent-auth.md` → `precedent-reverse.md` → `precedent-pentest.md`
- Check the index and precedents before new tasks; reuse prior experience

## Pre-completion self-check (MUST pass before claiming done)

- [ ] Did I complete the routing tri-axis match (target type + user intent + toolchain)?
- [ ] Did I read the target skill's SKILL.md after routing resolved?
- [ ] When routing missed, did I propose a new skill instead of force-matching?
- [ ] Did I use real tool paths from `tool-index`?
