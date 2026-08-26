# This Pack's Domain Coverage Map (Depth First)

> Compared with the community's "hundreds of micro-skills": we cover the main battlefields with **a few deep skills + routing + ops**.  
> Date: 2026-07-18

## Domain → This Pack's Entry

| Domain | PRIMARY / module | Notes |
|----|----------------|------|
| Mobile Android | `apk-reverse/` `mobile-reverse/` | |
| Mobile iOS | `mobile-reverse/` | |
| Deep binary work | `ida-reverse/` `radare2/` `ghidra-reverse/` | Ghidra = the open-source main path |
| General RE / anti-debug / OLLVM | `reverse-engineering/` | |
| .NET | `dotnet-reverse/` | |
| Frontend JS / signatures | `js-reverse/` | |
| Browser extensions | `browser-extension-reverse/` | |
| DSL / risk-control VMs | `reverse-engineering/dsl-vm-reverse/` | |
| Protocols / PCAP protocols | `protocol-reverse/` | |
| Firmware IoT | `firmware-pentest/` | |
| Malware samples | `malware-analysis/` | |
| Digital forensics / IR | `digital-forensics/` | |
| Threat hunting / blue team | `threat-hunting/` | |
| Pentest tools | `pentest-tools/` (+ src-hunter) | |
| Windows / AD | `windows-ad/` | |
| Cloud / containers / K8s | `cloud-k8s/` | |
| Code audit / SAST | `code-audit/` | |
| Wi-Fi / wireless | `wifi-wireless/` | |
| OT / ICS | `ot-ics/` | passive first; writing registers forbidden by default |
| macOS | `macos-reverse/` | iOS still goes through mobile-reverse |
| Thick clients | `thick-client/` | |
| Go / Rust binaries | `go-rust-reverse/` | |
| Hardware debug ports | `hardware-security/` | hands off to firmware-pentest |
| Databases | `database-security/` | |
| Email / phishing | `email-security/` | |
| Federated identity SSO | `identity-federation/` | complements api-security JWT |
| RF / SDR | `radio-sdr/` | receive-only by default; non-Wi-Fi |
| Multi-stage attacks | `attack-chain/` | |
| Pwn | `pwn-chain/` | |
| N-day patching | `patch-diff-exploit/` | |
| EDR research | `edr-bypass-re/` | |
| API | `api-security/` | |
| Supply chain SBOM | `supply-chain-security/` | |
| LLM/Agent | `llm-security/` | + `ops/skill-supply-chain.md` |
| Browser automation | `browser-automation/` | |
| Reports/diagrams | `docs-generator/` `diagram-generator/` | |
| Symbol migration | `binary-diff/` | |
| Operations contracts | `ops/` | **distinctive** |
| CTF orchestration | `CTF-Sandbox-Orchestrator/` | |
| Cryptographic pattern recognition | `reverse-engineering` pattern documents | shared with reverse engineering tasks; no separate extension pack maintained |

## Domains Explicitly Not Merged Wholesale (policy when routing misses)

| Domain | Policy |
|----|------|
| Pure game cheat development | not a product direction; Unity samples can still go through `reverse-engineering` + seed-014 |
| Deep automotive/aviation certification grade | can link out; this pack has only RF/OT entry level |
| Pure GRC/compliance long-form | does not replace professional GRC tools; report templates may reference |
| 800+ ATT&CK micro-skills | use this table + optional ATT&CK tags (Finding field) |

## With MITRE ATT&CK (optional)

The Finding template allows `optional_attack: Txxxx` (see `ops/evidence-finding-path.md`); a full ATT&CK engine is **not** required.
