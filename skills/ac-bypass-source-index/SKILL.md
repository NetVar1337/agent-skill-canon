---
name: ac-bypass-source-index
description: "Index skill for anti-cheat bypass research sources: UC topic map, local injector/HV corpora, Kevlar/AiDA/Valthrun, how to turn threads into lab checklists."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: game
  triggers:
    - "unknowncheats"
    - "AC sources list"
    - "bypass index"
---

# AC bypass research source index

## How to use threads/releases
1. Extract **claims** vs **repro steps** vs **code**
2. Recreate in lab with canary accounts
3. Convert into checklists/skills (this pack)
4. Never paste session cookies into repos

## Topic → skill routing
| Topic | Skill |
|---|---|
| HWID/serials | `hwid-identifier-surfaces` |
| TPM attestation | `tpm-attestation-research` |
| EAC bans | `eac-ban-stack` |
| Longevity design | `cheat-longevity-engineering` |
| Hyper-RE | `hypervisor-memory-introspection` |
| EAC.sys | `eac-kernel-driver-re` |
| EOS telemetry | `eac-usermode-telemetry-re` |
| Anti-debug | `x64dbg-anti-debugger` |
| Injectors | `manual-map-injector-engineering` |
| Driver emu | `kevlar-driver-emulation` |
| IDA AI | `aida-ida-assistant` |
| External HV stacks | `valthrun-style-stack` |
| Stack walks | `anti-cheat-stack-walk-stealth` |
| Bluepill type-2 HV | `bluepill-type2-hv` |
| QEMU/KVM hide | `qemu-anti-detection` |
| Guest HV/VM probes | `hypervisor-detection` |
| SMM (Plouton) | `plouton-smm` |
| Lift-to-LLVM deobf | `llvm-lift-deobfuscation` |
| Hyper-V boot implant (hyper-reV) | `hypervisor-memory-introspection` |

## Local corpora
- `Desktop/Injectors/**`
- `Desktop/AiDA-Fork-9.4`
- `Desktop/Valthrun/Release`
- `Documents/Kevlar` (https://github.com/NetVar1337/Kevlar)

## CR3Swapper GitHub corpus

https://github.com/CR3Swapper?tab=repositories — 37 public repos, 0 stars. Only `bluepill` is original (xeroxz type-2 HV, archived). Everything else is a fork. Map the useful ones; skip manim/MS-DOS/empty stubs.

| Repo | Upstream | Skill |
|---|---|---|
| `CR3Swapper/bluepill` | xeroxz Bluepill | `bluepill-type2-hv` |
| `qemu-anti-detection` | zhaodice/qemu-anti-detection | `qemu-anti-detection` |
| `plouton` | pRain1337/plouton | `plouton-smm` |
| `hyper-reV` | noahware/hyper-reV | `hypervisor-memory-introspection` |
| `Hypervisor-Detection` | void-stack/Hypervisor-Detection | `hypervisor-detection` |
| `Mergen` / `Dna` / `GAMBA` / `Simplifier` / `Polaris-Obfuscator` | NaC-L, Colton1skees, Denuvo, mazeworks, za233 | `llvm-lift-deobfuscation` |
| `emulator` | momo5502/sogen | `kevlar-driver-emulation` (usermode syscall emu) |
| `ac` | donnaskiez/ac | `ags-anti-cheat` |
| `DSE-Patcher` | gmh5225/DSE-Patcher | `byovd` / `ags-windows-kernel` |
| `ANTI_DBVM_TRACE` | Japrajah/ANTI_DBVM_TRACE | `x64dbg-anti-debugger` |
| `Awesome-Binary-Rewriting` / `rose` / `diablo` | paper list + rewriting frameworks | `llvm-lift-deobfuscation` (rewriting lane) |

## momo5502 corpus

https://github.com/momo5502 — trees under `E:\Tools\git\momo5502`.

| Repo | Skill |
|---|---|
| `sogen` / `sogen-linux-files` / `vmtrace` | `sogen-usermode-emulator` |
| `ept-hook-detection` + blog `?p=255` | `ept-hook-detection` |
| `hypervisor` | `hypervisor-dev` |
| `levo` | `llvm-lift-deobfuscation` |
| `cod-exploits` (CVE-2018-20817 / 10718) | `game-hacking-exploits` |
| `drm-analysis` / `denuvo-slides` | `virtualization-deobfuscation` |
| `patch-finder` | `ida-reverse` |
| `gameoverlay` | `imgui-overlay` |

## Aftermath Labs corpus

Org https://github.com/aftermathlabs · blog https://aftermathlabs.net/blog/ · trees `E:\Tools\git\aftermathlabs`. Full post index: `ring-1-bootkit/references/aftermath-blog.md`.

| Repo / post | Skill |
|---|---|
| `ring-1.io` + 2026-02-04 | `ring-1-bootkit` |
| Themida / Tencent VM 2026 + `themida-devirt` / `vmp2` | `virtualization-deobfuscation` |
| `Voyager` | `hypervisor-memory-introspection` |
| `VDM` / `msrexec` | `byovd` |
| `vmhook` (VMP2 READ hook) | `eac-kernel-driver-re` |
| `CallMeWin32kDriver` / `FakeEnclave` | `kernel-dev` |
| FiveM/GTA RCE 2022-09-26 | `game-hacking-exploits` |
| EQU8 / EAC 2021-08 | `ags-anti-cheat` / `eac-kernel-driver-re` |

## Public topic seeds (UnknownCheats)
- serials / EAC / FN bans
- remote TPM attestation trust crypto
- write cheat unless bans
- Hyper-RE memory introspection
- HWID retrieval methods
- AC bypass complete sources list
- EasyAntiCheat.sys RE
- EasyAntiCheat_EOS telemetry RE
- x64dbg anti-debugger
- stealth injector / Sastasha injector threads

## Reviewed member sources

For the `l55legend` (4711467) and `Spacebd` (740576) source review, use [references/uc-member-l55legend-spacebd.md](references/uc-member-l55legend-spacebd.md). It extracts reusable practices—build pinning, schema-first layout recovery, pointer-depth validation, static/runtime cross-checks, and controlled practice—without promoting target-specific offsets or community claims to portable facts.
