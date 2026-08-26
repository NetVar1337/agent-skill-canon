# Domain Routing Guide

Use this map to choose one primary skill before loading specialists. Start broad only when the target crosses domains; otherwise route directly to the narrowest entrypoint.

## Reverse engineering

**Primary:** `reverse-skill-router` for unknown/mixed targets; otherwise `ida-reverse`, `radare2`, `ghidra-reverse`, `dotnet-reverse`, `apk-reverse`, or `js-reverse`.

1. Triage format, architecture, protections, imports, and strings.
2. Select one static-analysis backend and preserve addresses/symbol evidence.
3. Add dynamic analysis only when it answers a specific uncertainty.
4. Persist names, types, comments, traces, and recovered protocols.
5. Hand exploit primitives to `pwn-chain` or findings to `docs-generator`.

Deep routes: `advanced-packer-unpacking`, `virtualization-deobfuscation`, `trace-guided-deobfuscation`, `protocol-reverse`, `go-rust-reverse`, `browser-extension-reverse`.

## Game hacking and game exploits

**Primary:** `game-hacking`; use `game-hacking-exploits` for authority, memory, packet, movement, and race bug classes.

- Engine recovery: `game-internals`, `game-engine-resources`, `offset-dumper`, `pattern-scanner`.
- Internal/external tooling: `lang-cpp-game-hacking`, `imgui-overlay`, `manual-map-injector-engineering`.
- Aim systems: `aimbot-triggerbot`, `aimbot-humanization`.
- Anti-cheat: `anti-cheat-bypass`, `ac-bypass-source-index`, `eac-usermode-telemetry-re`, `eac-kernel-driver-re`, `eac-ban-stack`.
- Lower layers: `kernel-dev`, `hypervisor-memory-introspection`, `stealth-hypervisor`, `dma-attack-techniques`, `tpm-attestation-research`.

Keep game state recovery, feature logic, memory transport, rendering, and anti-cheat research as separate modules so offsets or transports can change independently.

## Windows internals and kernel research

**Primary:** `windows-internals`; use `windows-driver-0day` for unknown `.sys` attack surfaces and `windows-0day-hunting` for privileged state machines.

- Drivers: `kernel-dev`, `driver-comm`, `kernel-callbacks`, `rust-driver-reconstruction`.
- Debugging/symbols: `windbg-ttd`, `windows-symbols-debugging`, `windows-postmortem`.
- Security boundaries: `windows-boundaries`, `windows-object-manager-confusion`, `windows-privileged-file-workflows`, `windows-profile-hive-research`.
- Boot/virtualization: `secure-boot-uefi-research`, `vbs-hvci-research`, `hypervisor-dev`.
- Known vulnerable drivers: `byovd`.

Always pin the exact OS build, symbols, architecture, mitigation state, and request schema before claiming a stable kernel offset or primitive.

## Exploit development and vulnerability research

**Primary:** `exploit-dev` for a known primitive; `vuln-research` for discovery; `pwn-chain` for reliable end-to-end exploitation.

- Memory classes: `stack-buffer-overflow`, `core-heap-exploitation`, `use-after-free`, `integer-overflow`, `format-string-bug`.
- Construction: `rop-chains`, `shellcode-dev`, `exploitation`.
- Discovery: `offensive-fuzzing`, `sanitizers`, `patch-diff-variant-hunting`.
- N-day work: `patch-diff-exploit`, `binary-diff`.
- Specialized targets: `browser-security-research`, `application-sandbox-escape-research`, `virtualization-escape-research`, `enterprise-server-rce-research`.

A completed exploit route records target version, mitigations, primitive, reliability conditions, expected crash/side effect, and a reproducible verification command.

## Penetration testing and red team

**Primary:** `red-team-assessment` or `engagement-flow` for a full engagement; `pentest-tools` for tool execution; direct-route single vulnerability classes.

1. Scope and asset map: `recon-full`, `recon-osint`, `target-profiling`.
2. Surface testing: `web-pentest`, `api-security`, `network-attack`, `cloud-security`.
3. Identity: `windows-ad`, `identity-federation`, `jwt-attack-methodology`.
4. Post-exploitation: `post-exploit-linux`, `post-exploit-windows`, `lateral-movement`.
5. Multi-stage paths: `attack-chain`, `finding-chain-correlator`.
6. Evidence/reporting: `finding-discipline`, `report-generate`, `offensive-reporting`.

Use the specialist skill for the actual parser or trust boundary: SQLi, SSRF, request smuggling, AD CS, IAM, Kubernetes, and similar classes should not be reduced to generic scanner output.

## Web and API security

**Primary:** `web-pentest` or `api-security`. Use `hack` and `src-hunter` when broad payload/playbook coverage is the goal.

Direct routes include `sql-injection-methodology`, `xss-methodology`, `ssrf-methodology`, `ssti-methodology`, `idor-methodology`, `jwt-attack-methodology`, `oauth-sso-attack`, `request-smuggling`, `race-condition`, and `file-upload-methodology`.

Build an endpoint, role, object, state-transition, and parser map before fuzzing. Confirm findings with control requests and reproducible evidence.

## Cloud, containers, and supply chain

**Primary:** `cloud-k8s` for mixed environments; platform-specific routes for AWS, Azure, GCP, Alibaba, Tencent, or Huawei.

- IAM and credentials: `cloud-iam-audit`, `cloud-aksk-exploit`, `cloud-metadata`.
- Containers/Kubernetes: `container-k8s-escape`, `k8s-container-escape`, `docker-pentesting`.
- CI/CD and artifacts: `cicd-supply-chain`, `supply-chain-security`, `terraform-tactics`.
- Products: `argocd-tactics`, `harbor-tactics`, `portainer-tactics`.

Trace identity from acquisition through accepted API action. Distinguish manifest state, build artifact, deployment state, and live runtime state.

## Malware, DFIR, and threat hunting

**Primary:** `malware-analysis-lifecycle` for samples; `digital-forensics` for evidence; `threat-hunting` for detection hypotheses.

Route file types to APK, .NET, Go/Rust, Office, PDF, packed PE, ELF, firmware, or bootkit specialists. Preserve hashes and acquisition provenance. Convert confirmed behavior into `yara-rule-authoring`, Sigma, network signatures, and IOC packages.

## Mobile, firmware, hardware, OT, and radio

- Mobile: `mobile-reverse`, `apk-reverse`, `mobile-pentest`, `ios-pentesting`.
- Firmware: `firmware-pentest`, `firmware-analysis`, `performing-firmware-extraction-with-binwalk`.
- Hardware: `hardware-security`, `auditing-uefi-firmware-with-chipsec`.
- OT/ICS: `ot-ics`, `industrial-control-vulnerability-research`.
- Wireless/RF: `wifi-wireless`, `radio-sdr`, protocol-specific Bluetooth/Zigbee/Z-Wave/LoRaWAN skills.

Keep acquisition, extraction, emulation, dynamic validation, and physical-device validation as explicit stages.

## AI and agent security

**Primary:** `llm-security` or `ai-agent-redteam`; use `llm-jailbreak-taxonomy` to choose an attack class.

Specialists cover prompt injection, RAG poisoning, memory contamination, MCP tool poisoning, identity and data security, classifier bypass, automated jailbreak optimization, and harness regression testing. Model the full prompt → planner → tool schema → credential → side-effect chain.

## Systems engineering

Use `systems-language-engineering` to route language and build work. Direct routes include `lang-cpp23`, `lang-rust`, `lang-go`, `lang-zig`, `lang-assembly`, compiler/build skills under `low-level-dev`, and kernel/hypervisor skills for privileged code.

## Completion standard

Regardless of domain, finish with:

1. Exact artifact paths and immutable target/version identifiers.
2. Evidence that supports each conclusion.
3. Commands or scenarios actually run after the final change.
4. Known assumptions and the next test that would retire each one.
5. A report or durable annotation when the work spans more than one session.
