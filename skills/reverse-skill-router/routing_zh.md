# Reverse Skill Routing Matrix

Routes tasks to the most appropriate skill module by target type, user intent, and toolchain. This matrix is enforced by default; it is not advisory.

## CRITICAL: Routing Decision Enforcement Protocol

1. `MUST` complete routing before executing; "do it first, backfill routing later" is not allowed.
2. `SHOULD` read `MASTER-ROUTING.md` first or run `scripts/master-route.ps1` to pick the PRIMARY; this table is for resolving hard cases.
3. `MUST` output your routing rationale (at least one of target type / intent / toolchain must hit).
4. `MUST` complete `case-init` / `scope.md` (`ops/scope-contract.md`) before ACTing on the target: `auth.status=granted` + `network_profile`.
5. `MUST NOT` shoehorn a task into a mismatched skill because "it looks close enough".
6. `MUST` go online to supplement methodology when routing misses, and propose a new skill.
7. `MUST NOT` just reply "give me a concrete task"; start the determinable steps based on existing input first.
8. Operations contract: `ops/` (evidence chain / roles / timeline / IDENTITY).
## By Target Type

| Target Type | Recommended Entry | Alternatives |
|---------|---------|---------|
| APK / Android app | `mobile-reverse/SKILL.md` — Frida/Objection/MobSF full-platform mobile reversing | `apk-reverse/` — static analysis, jadx decompilation; optionally licensed JEB Pro for cross-validation |
| iOS / IPA app | `mobile-reverse/SKILL.md` — iOS reversing + Frida/Objection | `mobile-reverse/references/ios-reverse-guide.md` — iOS specific |
| Binary exe/dll/so/elf | `ida-reverse/` — IDA Pro decompilation | `radare2/` — CLI analysis, or `reverse-engineering/tools.md` — GDB/Unicorn |
| JavaScript / Web frontend | `js-reverse/` — 5-phase workflow | anything-analyzer MCP's browser tools, or jshookmcp's browser/CDP/Hook capabilities |
| HTTP traffic capture / browser sampling / request replay | anything-analyzer MCP (23816) | Reqable MCP, `js-reverse/`, jshookmcp, or `competition-web-runtime/` |
| Firmware / IoT | `firmware-pentest/` — full OWASP FSTM chain: extraction→emulation→fuzz→exploitation | `reverse-engineering/platforms.md` — static RE only / `reverse-engineering/tools.md` — Ghidra headless |
| WASM / Python bytecode / .NET | `reverse-engineering/languages.md` | Look up the specific language's section |
| macOS / iOS | `reverse-engineering/platforms.md` — Mach-O/ObjC/Swift | — |
| Memory dump / PCAP | `reverse-engineering/platforms.md` | `reverse-engineering/patterns*.md` |
| Existing case / evidence handover review | `case-review/SKILL.md`: Evidence graph and fixity verification | `docs-generator/`: final report |
| Cryptography / encryption-decryption algorithms | `reverse-engineering/patterns*.md` — crypto patterns | `js-reverse/` (if it is frontend encryption) |
| Protocol reversing / custom protocol | `reverse-engineering/platforms.md` — network protocols | `js-reverse/` (if WebSocket/HTTP) |
| Go / Rust binaries | `reverse-engineering/languages-compiled.md` + `go-reverse.md` | `ida-reverse/` or `radare2/` |
| **CTF competition full stack** | `../CTF-Sandbox-Orchestrator/ctf-sandbox-orchestrator/SKILL.md` — master control entry | Route by evidence surface to 40+ sub-skills |
| **CTF ZIP / PKZIP archive challenges** | `../CTF-Sandbox-Orchestrator/competition-zip-archive/SKILL.md` — legacy ZipCrypto + `bkcrack` plaintext attack | Preferred over password brute force |
| Web runtime / API | `../CTF-Sandbox-Orchestrator/competition-web-runtime/SKILL.md` | — |
| Cloud / containers / K8s | `../CTF-Sandbox-Orchestrator/competition-agent-cloud/SKILL.md` | — |
| Windows / AD / identity | `../CTF-Sandbox-Orchestrator/competition-identity-windows/SKILL.md` | — |
| Forensics / PCAP / steganography | `../CTF-Sandbox-Orchestrator/competition-forensic-timeline/SKILL.md` | — |
| Prompt injection / Agent | `../CTF-Sandbox-Orchestrator/competition-prompt-injection/SKILL.md` | — |
| Mobile (Android/iOS) | `../CTF-Sandbox-Orchestrator/competition-android-hooking/SKILL.md` | — |
| Firmware / malicious samples | `../CTF-Sandbox-Orchestrator/competition-firmware-layout/SKILL.md` | — |
| **LLM apps / AI Agents** | `llm-security/SKILL.md` — OWASP LLM + ASI Top 10 | `../CTF-Sandbox-Orchestrator/competition-prompt-injection/SKILL.md` — CTF scenarios |
| **REST / GraphQL / WebSocket APIs** | `api-security/SKILL.md` — 10-phase methodology | `pentest-tools/SKILL.md` — basic web penetration |
| **Software supply chain / SBOM / SCA** | `supply-chain-security/SKILL.md` — six-layer governance framework | `pentest-tools/SKILL.md` — dependency scanning tools |
| **Malware / virus samples** | `malware-analysis/SKILL.md` — six-phase analysis + YARA/Sigma | `reverse-engineering/SKILL.md` — general reversing only / `ida-reverse/` deep analysis |

## By User Intent

| User Says | Reference |
|--------|---------|
| "Decompile / take a look in IDA" | `ida-reverse/SKILL.md` — IDA MCP workflow |
| "Recover source code / recover as assembly / reverse reconstruction" | `reverse-engineering/SKILL.md` — general reversing + `ida-reverse/` or capstone static disassembly |
| "Frida hook it / dynamic injection" | `reverse-engineering/tools-dynamic.md` — Frida chapter |
| "radare2 / r2 analysis" | `radare2/SKILL.md` — CLI workflow |
| "Find frontend signatures / encrypted parameters" | `js-reverse/SKILL.md` — Observe→Capture→Rebuild |
| "jshookmcp / JS hook / CDP debugging" | `js-reverse/SKILL.md` — still the same JS/Web reversing chain; before invoking, confirm the MCP server is downloaded, registered to the client, and enabled |
| "Reqable / Reqable MCP / capture and replay" | `pentest-tools/SKILL.md` — local traffic capture and API workflows within authorized scope |
| "JEB / JEB Pro" | `apk-reverse/SKILL.md` — licensed Android / ARM cross-validation; confirm it is installed locally first |
| "APK unpack / repack / edit smali" | `apk-reverse/SKILL.md` — decode→rebuild-sign-install |
| "Bypass anti-debugging / anti-detection" | `reverse-engineering/anti-analysis.md` |
| "What obfuscation / VM is this" | `reverse-engineering/patterns*.md` — look up by pattern |
| "Go/Rust/Swift reversing" | `reverse-engineering/languages-compiled.md` + `reverse-engineering/go-reverse.md` (Go specific) |
| "Kernel drivers / rootkits / LKM" | `reverse-engineering/kernel-driver-reverse.md` — kernel driver reversing |
| "C++ vtables / virtual functions / class recovery" | `reverse-engineering/kernel-driver-reverse.md` — C/C++ pattern recognition |
| "IOCTL/DeviceIoControl" | `reverse-engineering/kernel-driver-reverse.md` — Windows driver analysis |
| "Python bytecode / pyc" | `reverse-engineering/languages.md` — Python chapter |
| "Symbolic execution / angr" | `reverse-engineering/tools-dynamic.md` — angr chapter |
| "Emulated execution / Unicorn" | `reverse-engineering/tools.md` — Unicorn chapter |
| "Patch the environment / reproduce in Node" | `js-reverse/references/env-patching.md` |
| "CTF challenges / competition reversing" | `reverse-engineering/patterns-ctf*.md` |
| "CTF ZIP/PKZIP/bkcrack/archive plaintext attack" | `../CTF-Sandbox-Orchestrator/competition-zip-archive/SKILL.md` |
| "Write a report / write docs / produce a report" | `docs-generator/` — technical documentation |
| "Review a case / evidence chain / traceability" | `case-review/SKILL.md`: read-only Evidence graph review |
| "Write a writeup" | `docs-generator/` — CTF writeup templates |
| "Open a webpage / browser automation / fill forms" | `browser-automation/SKILL.md` — Playwright browser operations |
| "Crawl pages / screenshots / automated login" | `browser-automation/SKILL.md` — browser automation |
| "Playwright / headless" | `browser-automation/SKILL.md` — browser automation |
| "Operate desktop apps / Windows automation" | `browser-automation/SKILL.md` — OpenReverse desktop automation |
| "UIA/CUA/desktop GUI operations" | `browser-automation/SKILL.md` — OpenReverse (UIA/CUA mode) |
| "OpenReverse" | `browser-automation/SKILL.md` — desktop interaction + network observation |
| "Symbol migration / cross-version comparison" | `binary-diff/SKILL.md` — LLM batch symbol migration |
| "Missing PDB / derive new-version symbols from old" | `binary-diff/SKILL.md` — cross-version symbol migration |
| "bindiff / function offset migration" | `binary-diff/SKILL.md` — binary diffing |
| "N-day / patch diffing / CVE restoration / 1-day weaponization" | `patch-diff-exploit/SKILL.md` — patch→PoC→pre-patch hosts |
| "Patch Tuesday/MSRC/Microsoft Update Catalog" | `patch-diff-exploit/references/patch-tuesday-workflow.md` |
| "ghidriff/Diaphora/DeepDiff (offensive side)" | `patch-diff-exploit/references/diff-tools-comparison.md` |
| "pwn / stack overflow / ROP / ret2libc / write an exploit" | `pwn-chain/SKILL.md` — RE→exploit full pipeline |
| "Heap exploitation / tcache/fastbin/unsorted bin" | `pwn-chain/references/heap-pwn.md` |
| "kernel pwn / kernel privilege escalation / modprobe_path / commit_creds" | `pwn-chain/references/kernel-pwn.md` |
| "pwntools/GEF/pwndbg/one_gadget/libc-database" | `pwn-chain/SKILL.md` |
| "Firmware pentest / router firmware / IoT exploitation" | `firmware-pentest/SKILL.md` — from extraction to live hardware |
| "binwalk/unblob/SquashFS/UBI/JFFS2" | `firmware-pentest/references/extraction-methodology.md` |
| "EMBA/automated firmware auditing/cve-bin-tool" | `firmware-pentest/references/emba-automated-analysis.md` |
| "Firmadyne/FAT/QEMU full-system emulation/AFL++ fuzz" | `firmware-pentest/references/emulation-and-fuzz.md` |
| "EDR bypass / AV bypass / AV evasion / red team delivery" | `edr-bypass-re/SKILL.md` — reverse the defender→targeted bypass |
| "direct syscall/indirect syscall/Hell's Gate/SysWhispers" | `edr-bypass-re/references/unhook-techniques.md` |
| "ETW patch/AMSI patch/telemetry blinding" | `edr-bypass-re/references/telemetry-blinding.md` |
| "ntdll hook/pe-sieve/EDR hook tables" | `edr-bypass-re/references/hook-survey.md` |
| "Port scanning / Nmap" | `pentest-tools/SKILL.md` — reconnaissance |
| "Vulnerability scanning / Nuclei" | `pentest-tools/SKILL.md` — vulnerability detection |
| "SQL injection / SQLMap" | `pentest-tools/SKILL.md` — web penetration |
| "Directory brute force / FFUF / Gobuster" | `pentest-tools/SKILL.md` — web penetration |
| "Password cracking / Hashcat" | `pentest-tools/SKILL.md` — password cracking |
| "Penetration testing / active scanning" | `pentest-tools/SKILL.md` — pentest toolchain |
| "SRC bug hunting / Bug Bounty / crowdsourced testing" | `pentest-tools/src-hunter/SKILL.md` — 19 playbooks + H1 cases |
| "WAF bypass / bypass" | `pentest-tools/src-hunter/references/payloader/` — 263 bypass steps |
| "Diagrams / flowcharts / architecture diagrams / attack path diagrams" | `diagram-generator/SKILL.md` — diagram generation |
| "Sequence diagrams / state diagrams / ER diagrams / data flow diagrams" | `diagram-generator/SKILL.md` — Mermaid/Graphviz/PlantUML |
| "Mermaid/Graphviz/PlantUML" | `diagram-generator/SKILL.md` — diagram generation |
| "Malware / virus analysis / sample analysis" | `malware-analysis/SKILL.md` — six-phase analysis + YARA/Sigma/sandbox |
| "Go reversing / Rust reversing / stripped" | `reverse-engineering/languages-compiled.md` + `go-reverse.md` |
| "Firmware / IoT / binwalk / ARM" | `firmware-pentest/SKILL.md` — full firmware chain analysis (consistent with the by-target-type table) |
| "Cryptography / encryption-decryption / AES / RSA" | `reverse-engineering/patterns*.md` — crypto pattern recognition |
| "Protocol reversing / Protobuf / custom protocols" | `reverse-engineering/platforms.md` — network protocol analysis |
| "Memory dumps / dump analysis" | `reverse-engineering/platforms.md` |
| "Cloud security / container escape / K8s" | `../CTF-Sandbox-Orchestrator/competition-agent-cloud/SKILL.md` |
| "Prompt injection / AI security" | `llm-security/SKILL.md` — OWASP LLM + ASI Top 10 complete framework |
| "Agent won't work / read-only no action / lazy AI / skips steps" | `llm-security/references/agent-obedience-engineering.md` — agent obedience engineering |
| "Make the AI work / enforce / strengthen agent instructions" | `llm-security/references/agent-obedience-engineering.md` — 8 techniques + excuse rebuttal table |
| "Prompt engineering / skill engineering / harness engineering" | `llm-security/references/agent-obedience-engineering.md` — context window layout + five modes |
| "Codex not working / Claude Code not executing / AI coding agent idle" | `llm-security/references/agent-obedience-engineering.md` — forced-start templates |
| "Precedent / operation log / what has been done / already executed / operation authorization" | `field-journal/precedent-auth.md` → `field-journal/precedent-reverse.md` or `field-journal/precedent-pentest.md` — authorization chain: first clear the safety review → confirm operating routine |
| "Authorization / is it allowed / can I do this / safety warning / legal risk / scope" | `field-journal/precedent-auth.md` — pre-declared authorization; once confirmed, no more safety warnings |
| "Internal network penetration / lateral movement" | `pentest-tools/SKILL.md` + `references/network-attack-defense.md` |
| "Privilege escalation / privesc" | `pentest-tools/references/network-attack-defense.md` — privilege escalation chapter |
| "Mimikatz / credential extraction / PtH" | `pentest-tools/references/network-attack-defense.md` — credential acquisition |
| "Kerberos / domain penetration / AD" | `pentest-tools/references/network-attack-defense.md` — Kerberos attacks |
| "C2 / remote control / persistence" | `pentest-tools/references/network-attack-defense.md` — persistence + C2 |
| "Blue team / detection / defense / incident response" | `pentest-tools/references/network-attack-defense.md` — defense system |
| "APK security testing / mobile security" | `apk-reverse/references/apk-security-checklist.md` — OWASP MASTG |
| "SSTI / template injection" | `pentest-tools/SKILL.md` — SSTImap automated detection |
| "XSS scanning / cross-site scripting" | `pentest-tools/SKILL.md` — XSStrike advanced scanning |
| "WordPress pentest / WP enumeration" | `pentest-tools/SKILL.md` — WPProbe plugin enumeration |
| "C2 frameworks / adversary emulation / AdaptixC2" | `pentest-tools/SKILL.md` — AdaptixC2 post-exploitation and adversary emulation framework |
| "Atomic Red Team / detection testing" | `pentest-tools/SKILL.md` — Atomic-Operator |
| "WiFi attacks / wireless penetration" | `pentest-tools/SKILL.md` — Fluxion + aircrack-ng |
| "NTLM relay / authentication coercion" | `pentest-tools/SKILL.md` — Coercer |
| "WinRM / Windows remote" | `pentest-tools/SKILL.md` — evil-winrm-py |
| "NetExec/CrackMapExec/nxc" | `pentest-tools/SKILL.md` — network service enumeration |
| "AI automated pentest / MCP security" | `pentest-tools/SKILL.md` — HexStrike AI / MetasploitMCP / mcp-kali-server |
| "Swarm / swarm penetration / autonomous scanning" | `pentest-tools/SKILL.md` — Pentest Swarm AI (pentestswarm scan --swarm) |
| "Bug bounty automation / continuous monitoring" | `pentest-tools/SKILL.md` — Pentest Swarm AI playbook: bug-bounty |
| "Attack surface management / ASM" | `pentest-tools/SKILL.md` — Pentest Swarm AI playbook: external-asm |
| "Red team / offense-defense exercises / HW" | `attack-chain/SKILL.md` — full attack chain orchestration (reconnaissance→breakthrough→privesc→lateral→persistence) |
| "Initial foothold / first breach / perimeter breach" | `attack-chain/SKILL.md` — perimeter breach phase |
| "Near-field penetration / BadUSB / WiFi phishing" | `attack-chain/SKILL.md` — near-field penetration chapter |
| "AV evasion delivery / real-world EDR bypass / shellcode loaders" | `attack-chain/SKILL.md` — EDR/AV bypass in the attack chain (delivery phase) |
| "Phishing / social engineering / email phishing" | `attack-chain/SKILL.md` — phishing chapter |
| "Supply chain attacks" | `attack-chain/SKILL.md` — supply chain attack chapter |
| "Trace cleanup / anti-forensics" | `attack-chain/SKILL.md` — trace cleanup chapter |
| "Full penetration test / end-to-end" | `attack-chain/SKILL.md` — full-chain planning |
| "From external network to domain controller / internal network" | `attack-chain/SKILL.md` — cross-phase path orchestration |
| "Attack surface assessment / attack path planning" | `attack-chain/SKILL.md` — path planning decision tree |
| "Got a shell, what next / post-exploitation" | `attack-chain/SKILL.md` — plan follow-up from the current foothold |
| "Internal network penetration end-to-end" | `attack-chain/SKILL.md` — lateral movement + privesc + domain attacks |
| "msfconsole hangs / orphan processes / MSF invocation rules" | `pentest-tools/references/msf-protocol.md` — 3 correct MSF modes + 6 major mistakes |
| "Anonymization / placeholders / sharing payloads / sanitize before writing a writeup" | `field-journal/anonymization.md` — anonymization placeholder conventions |
| "Hydra / online brute force / SSH brute force" | `pentest-tools/SKILL.md` — online password brute force |
| "Nikto / web server scanning" | `pentest-tools/SKILL.md` — web vulnerability scanning |
| "Metasploit/msfconsole/exploit" | `pentest-tools/SKILL.md` — exploitation framework |
| "Wireshark / traffic analysis / PCAP" | `digital-forensics/` or `protocol-reverse/` |
| "Protocol reversing / Protobuf / custom protocols" | `protocol-reverse/SKILL.md` |
| "Ghidra / no IDA" | `ghidra-reverse/SKILL.md` |
| "K8s / container escape / cloud security" | `cloud-k8s/SKILL.md` |
| "Domain penetration / BloodHound / Certipy / Kerberoast" | `windows-ad/SKILL.md` |
| "Forensics / Volatility / memory dumps" | `digital-forensics/SKILL.md` |
| "Code audit / SAST / Semgrep" | `code-audit/SKILL.md` |
| "Threat hunting / blue team / detection engineering" | `threat-hunting/SKILL.md` |
| "Game reversing / IL2CPP / Unity" | `reverse-engineering/SKILL.md` + seed-014 |
| "WiFi / wireless penetration / aircrack" | `wifi-wireless/SKILL.md` |
| "Browser extensions / Chrome extensions / crx" | `browser-extension-reverse/SKILL.md` |
| "Industrial control / OT / ICS / SCADA / PLC" | `ot-ics/SKILL.md` |
| "macOS reversing / Mach-O" | `macos-reverse/SKILL.md` |
| "Thick clients / desktop clients" | `thick-client/SKILL.md` |
| "Go reversing / Rust reversing" | `go-rust-reverse/SKILL.md` |
| "UART/JTAG / hardware debugging" | `hardware-security/SKILL.md` |
| "Database security / Redis / Mongo" | `database-security/SKILL.md` |
| "Phishing email / SPF / DKIM / DMARC" | `email-security/SKILL.md` |
| "SAML/OIDC/SSO federation" | `identity-federation/SKILL.md` |
| "SDR / radio frequency / HackRF" | `radio-sdr/SKILL.md` |
| "BurpSuite / web proxy / interception" | `pentest-tools/SKILL.md` — web proxy |
| "Responder / LLMNR poisoning / NBT-NS" | `pentest-tools/SKILL.md` — intranet poisoning |
| "BloodHound / AD paths / attack graphs" | `pentest-tools/SKILL.md` — AD attack path visualization |
| "Certipy / AD CS / certificate attacks" | `pentest-tools/SKILL.md` — AD certificate service attacks |
| "wfuzz / parameter fuzzing / web fuzz" | `pentest-tools/SKILL.md` — web fuzzing |
| "GDB/GEF / debugging / breakpoints" | `reverse-engineering/tools.md` — dynamic debugging |
| "objdump / disassembly / ELF analysis" | `reverse-engineering/SKILL.md` — static analysis |
| "strings / string extraction" | `reverse-engineering/SKILL.md` — rapid triage |
| "ProxyCat / proxy pools / IP rotation" | `pentest-tools/SKILL.md` — proxy management |
| "LLM security / AI security testing / prompt injection testing" | `llm-security/SKILL.md` — OWASP LLM + ASI Top 10 complete framework |
| "LLM jailbreak / jailbreak / system prompt extraction" | `llm-security/references/prompt-injection-methodology.md` — five escalating injection levels |
| "Agent security / tool abuse / memory poisoning / goal hijacking" | `llm-security/references/agent-security-testing.md` — seven-phase agent testing |
| "garak/PyRIT / AI red team" | `llm-security/SKILL.md` — LLM security toolchain |
| "API security testing / API penetration" | `api-security/SKILL.md` — 10-phase API testing methodology |
| "GraphQL security / introspection attacks / batch query bypass" | `api-security/references/rest-graphql-testing.md` — GraphQL specific |
| "JWT attacks / OAuth bypass / alg:none" | `api-security/references/jwt-oauth-testing.md` — JWT + OAuth testing |
| "BOLA/IDOR/BFLA / object-level authorization bypass" | `api-security/SKILL.md` — Phase 3 authorization testing |
| "Supply chain security / SBOM / SCA / dependency scanning" | `supply-chain-security/SKILL.md` — six-layer supply chain governance |
| "CI/CD security / pipeline audits / build integrity" | `supply-chain-security/references/cicd-pipeline-security.md` — pipeline security |
| "Container security / image scanning / Trivy / Cosign" | `supply-chain-security/SKILL.md` — container security chapter |
| "gitleaks / secret scanning / credential leakage" | `supply-chain-security/SKILL.md` — CI/CD pipeline security |
| "iOS reversing / IPA / Objective-C / Swift / Mach-O" | `mobile-reverse/SKILL.md` — iOS reversing + Frida/Objection |
| "Frida/Objection / dynamic instrumentation / SSL unpinning" | `mobile-reverse/references/frida-objection-deep.md` — Frida deep usage |
| "Root detection bypass / jailbreak detection bypass / mobile anti-debugging" | `mobile-reverse/references/anti-detection-bypass.md` — multi-layer bypass |
| "Mobile security testing / MSTG / OWASP Mobile" | `mobile-reverse/SKILL.md` — OWASP MASTG methodology |
| "YARA rules / Sigma rules / behavioral detection rules" | `malware-analysis/references/yara-sigma-rules.md` — rule authoring methodology |
| "Sandbox analysis / CAPE / Joe Sandbox / malware sandboxes" | `malware-analysis/references/sandbox-orchestration.md` — sandbox orchestration |
| "Anti-analysis / anti-sandbox / anti-debugging / VM detection" | `malware-analysis/references/anti-analysis-techniques.md` — 94 techniques |
| "IOC extraction / threat intelligence / malware analysis" | `malware-analysis/SKILL.md` — six-phase analysis workflow |
| "AI decompilation / LLM reversing / neural decompilation" | `reverse-engineering/references/ai-assisted-re.md` — AI-assisted reversing |

| Tool | Related Module |
|------|---------|
| IDA Pro (idapro_*) | `ida-reverse/` — MCP HTTP server + 72 tools |
| radare2 (r2/rabin2/rasm2) | `radare2/` — CLI + recon.ps1 |
| jadx / apktool | `apk-reverse/` — decode.ps1 / manifest-summary.ps1 |
| Frida | `reverse-engineering/tools-dynamic.md` |
| GDB / rr (general debugging) | `reverse-engineering/tools.md` |
| Ghidra (headless) | `reverse-engineering/tools.md` + Ghidra MCP (free IDA alternative, auto-registered via bootstrap) |
| Python 3 stdlib | `case-review/`: read-only case Evidence graph review |
| angr / Qiling / Unicorn | `reverse-engineering/tools-dynamic.md` |
| BinDiff / Diaphora | `reverse-engineering/tools-advanced.md` |
| anything-analyzer MCP | MCP server on port 23816 (browser + HTTP capture + AI analysis) |
| jshookmcp | reinforcement MCP surface for `js-reverse/`, suited to browser/CDP/Hook/Network/SourceMap/AST scenarios; must be downloaded and enabled in an MCP client first |
| agent-browser / Playwright | `browser-automation/` — browser automation (open, click, fill forms, crawl, screenshot) |
| OpenReverse (UIA/CUA) | `browser-automation/` — Windows desktop app automation + network observation (mitmproxy) |
| LLM symbol migration / BinDiff alternative | `binary-diff/` — cross-version batch symbol migration (DeepSeek/GPT) |
| BinDiff / Diaphora / ghidriff / DeepDiff (offensive side) | `patch-diff-exploit/` — locate the vulnerability from the patch → weaponize |
| binwalk v3 / unblob / EMBA / Firmadyne / FAT | `firmware-pentest/` — firmware extraction / automated auditing / emulation |
| pwntools / GEF / pwndbg / ROPgadget / Ropper / one_gadget / libc-database | `pwn-chain/` — RE→working exploit |
| SysWhispers3 / Hell's Gate / pe-sieve / API Monitor | `edr-bypass-re/` — EDR bypass research and implementation |
| Nmap / Masscan | `pentest-tools/` — port scanning, service identification |
| Nuclei / ZAP / Nikto | `pentest-tools/` — vulnerability scanning |
| SQLMap / FFUF / Gobuster | `pentest-tools/` — web penetration (injection/brute force) |
| SSTImap | `pentest-tools/` — automated SSTI detection and exploitation (Kali 2026.1: `apt install sstimap`) |
| XSStrike | `pentest-tools/` — advanced XSS scanning (Kali 2026.1: `apt install xsstrike`) |
| WPProbe | `pentest-tools/` — WordPress plugin enumeration (Kali 2026.1: `apt install wpprobe`) |
| Hashcat / John / Hydra | `pentest-tools/` — password cracking |
| Metasploit / Impacket | `pentest-tools/` — exploitation frameworks |
| MetasploitMCP | `pentest-tools/` — Metasploit MCP interface (Kali 2026.1: `apt install metasploitmcp`) |
| mcp-kali-server | `pentest-tools/` — official Kali MCP, AI directly invokes terminal tools (`apt install mcp-kali-server`) |
| HexStrike AI | `pentest-tools/` — MCP automation of 150+ security tools (Kali 2025.4: `apt install hexstrike-ai`) |
| Pentest Swarm AI | `pentest-tools/` — swarm-intelligence autonomous penetration framework, stigmergic blackboard coordinating multiple agents (`go install` or Docker) |
| AdaptixC2 | `pentest-tools/` — post-exploitation and adversary emulation framework (Kali 2026.1: `apt install adaptixc2`) |
| Atomic-Operator | `pentest-tools/` — Atomic Red Team test execution (Kali 2026.1) |
| Coercer | `pentest-tools/` — Windows authentication coercion / NTLM relay (`apt install coercer`) |
| NetExec (nxc) | `pentest-tools/` — network service enumeration and exploitation, CrackMapExec successor (preinstalled on Kali) |
| evil-winrm-py | `pentest-tools/` — Python WinRM remote execution (Kali 2025.4) |
| Fluxion / aircrack-ng | `pentest-tools/` — WiFi security auditing and cracking (aircrack-ng preinstalled on Kali, fluxion added in 2026.1) |
| Responder | `pentest-tools/` — LLMNR/NBT-NS/MDNS poisoning (preinstalled on Kali) |
| BloodHound | `pentest-tools/` — AD attack path visualization (`apt install bloodhound`) |
| Certipy | `pentest-tools/` — AD certificate service attacks (`apt install certipy-ad`) |
| CrackMapExec / NetExec | `pentest-tools/` — network service enumeration (nxc is CME's successor, preinstalled on Kali) |
| wfuzz | `pentest-tools/` — web parameter fuzzing (preinstalled on Kali) |
| Wireshark / tshark | `pentest-tools/` — network protocol analysis and PCAP parsing (preinstalled on Kali) |
| BurpSuite | `pentest-tools/` — web proxy, interception, vulnerability scanning (Community edition preinstalled on Kali) |
| BurpSuite MCP | `pentest-tools/` — full AI control of 63 tools (proxy history/Intruder/Repeater/Scanner/Collaborator), see `references/burpsuite-mcp-guide.md` |
| ProxyCat | `pentest-tools/` — proxy pool management and IP rotation |
| objdump / strings / file | `reverse-engineering/` — basic static analysis (preinstalled on Kali) |
| Cobalt Strike / Sliver / Havoc / Mythic | `pentest-tools/` — C2 framework tools (same module as AdaptixC2) |
| Rubber Ducky / WiFi Pineapple / Proxmark3 | `attack-chain/` — near-field penetration hardware |
| pentestMCP (Docker) | `pentest-tools/` — one-command MCP for 20+ tools |
| Mermaid / Graphviz / PlantUML | `diagram-generator/` — diagram generation (flowcharts/sequence diagrams/architecture diagrams/attack paths) |
| garak / PyRIT / promptfoo | `llm-security/` — LLM security testing (100+ injection probes/multi-turn orchestration) |
| Vespasian / Entropy / api.sh | `api-security/` — API discovery and attack scenario generation |
| jwt_tool | `api-security/` — comprehensive JWT testing (alg:none/key confusion/kid injection) |
| FireTail / Escape DAST | `api-security/` — GraphQL specific + business logic security |
| OSV-Scanner / Trivy / Syft | `supply-chain-security/` — SBOM generation + SCA scanning |
| OWASP Dependency-Track | `supply-chain-security/` — enterprise continuous SCA monitoring |
| Gitleaks / truffleHog | `supply-chain-security/` — secret/credential scanning |
| Cosign / SLSA | `supply-chain-security/` — build signing and provenance |
| Frida / Objection | `mobile-reverse/` — dynamic instrumentation + Frida Gadget injection |
| JADX / apktool / MobSF | `mobile-reverse/` — Android static analysis |
| class-dump / jtool2 / Hopper | `mobile-reverse/` — iOS static analysis |
| CAPE Sandbox / ASD Azul | `malware-analysis/` — sandbox automation orchestration |
| YARA / FLOSS | `malware-analysis/` — pattern matching + string deobfuscation |
| Sigma / Sigma CLI | `malware-analysis/` — SIEM behavioral detection rules |
| pe-sieve / Detect It Easy | `malware-analysis/` — process scanning + packer detection |
| LLM4Decompile / Glaurung | `reverse-engineering/` — AI-assisted decompilation |

When you need to confirm whether a tool is available on this machine, where its path is, or which script invokes it, consult `tool-index.md` uniformly — do not guess paths ad hoc.

---

## Handling Routing Misses

If the current task cannot be matched in any of the tables above, **do not force it into an existing skill**; follow this process:

1. First confirm whether it is an edge case of an existing skill (the existing skill can be extended to cover it)
2. If it is genuinely a new type, proactively propose a new skill to the user:
   - State the proposed skill name and the scenarios it covers
   - State the required toolchain
   - State its relationship to existing skills
3. After user confirmation, follow the `CONTRIBUTING.md` process to add it
4. After adding, update this routing matrix

**The AI does not need to wait for the user to notice the gap. A routing failure is itself the signal to add a new skill.**

## Path Crossings (Cross-Module Scenarios)

Some tasks span multiple modules; here are the common path crossings:

```
APK reversing path:
  apk-reverse/scripts/decode.ps1 → Java layer analysis
  ↓ if the core is in a .so
  ida-reverse/ or radare2/ → so analysis
  ↓ if dynamic verification is needed
  apk-reverse/scripts/frida-run.ps1 → Frida hook

Frontend JS reversing path:
  js-reverse/Observe → locate the target request
  ↓ when a stronger browser/CDP/Hook/Network surface is needed
  jshookmcp → do page runtime sampling, breakpoints, interception, SourceMap/AST assistance
  ↓ after confirming the entry function
  js-reverse/Rebuild → local Node reproduction
  ↓ when the environment needs patching
  js-reverse/references/env-patching.md

Binary reversing path:
  radare2/scripts/recon.ps1 → quick recon
  ↓ deep analysis
  ida-reverse/ → IDA decompilation
  ↓ dynamic verification
  reverse-engineering/tools-dynamic.md → Frida/GDB

CTF competition path (via CTF-Sandbox-Orchestrator):
  ctf-sandbox-orchestrator/SKILL.md → build the sandbox model
  ↓ route by the dominant evidence surface
  competition-web-runtime/ or competition-reverse-pwn/ or competition-identity-windows/
  ↓ when stuck, return to master control
  ctf-sandbox-orchestrator → re-route

Cookie HMAC key reuse → backend auth bypass:
  competition-web-runtime/references/cookie-hmac-key-reuse-auth-bypass.md
  ↓ applicable scenarios
  URL contains an access token, signed cookies, and the backend admin_session share the same key

Firmware pentest path:
  firmware-pentest/references/extraction-methodology.md → extract the filesystem
  ↓ once binaries are obtained
  firmware-pentest/references/emba-automated-analysis.md → EMBA automated audit to find known CVEs
  ↓ when known CVEs are not enough / hunting 0-days
  firmware-pentest/references/emulation-and-fuzz.md → Firmadyne emulation + AFL++ fuzz
  ↓ crash found
  pwn-chain/references/stack-pwn.md or heap-pwn.md → write the exploit
  ↓ attack live hardware
  attack-chain/SKILL.md → integrate into the attack chain

N-day weaponization path:
  patch-diff-exploit/references/patch-tuesday-workflow.md → obtain pre/post-patch binaries
  ↓ align symbols
  patch-diff-exploit/references/diff-tools-comparison.md → BinDiff/ghidriff/Diaphora selection
  ↓ locate the change
  patch-diff-exploit/references/root-cause-and-poc.md → LLM-assisted root cause + write the PoC
  ↓ weaponize
  pwn-chain/SKILL.md (build a stable exploit) + pentest-tools/references/msf-protocol.md (Metasploit modularization)

Red team delivery path:
  attack-chain/SKILL.md → pick the phase
  ↓ when EDR bypass is needed
  edr-bypass-re/references/hook-survey.md → identify the target EDR's hooks
  ↓ pick the bypass technique
  edr-bypass-re/references/unhook-techniques.md → direct syscalls / Hell's Gate
  edr-bypass-re/references/telemetry-blinding.md → ETW patch / AMSI patch
  ↓ local verification
  pe-sieve / API Monitor → confirm the unhook is clean
  ↓ deliver
  return to the attack-chain post-exploitation phase
```
