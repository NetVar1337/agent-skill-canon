# Aftermath Labs blog index

Site: `https://aftermathlabs.net/blog/` (Hugo; RSS `/blog/index.xml`). Older mirrors: `back.engineering`.

## 2026 (current method)

| Date | Title | Skill |
|---|---|---|
| 2026-07-31 | Static Devirtualization of Tencent VM | `virtualization-deobfuscation` |
| 2026-05-09 | Static Devirtualization of Themida | `virtualization-deobfuscation` |
| 2026-02-04 | Deobfuscation and Analysis of Ring-1.io | `ring-1-bootkit` |

Themida/Tencent pipeline they actually recommend: guided symbolic eval → const promotion & memory model → fold / DSE / combine / branch fold → VMEXIT + virtual CF → dead-dependency + RSP rewrite → 1:1 lower (BLARE2, not LLVM-as-compiler). vmp2 handler ID is documented as brittle.

## Hypervisor / physmem lineage

| Date | Title | Skill |
|---|---|---|
| 2022-08-04 | AMD-V Hypervisor Development (KrakenSvm) | `hypervisor-dev` |
| 2021-04-20 | Voyager — Hyper-V hacking framework | `hypervisor-memory-introspection` |
| 2021-03-29 | Hyperspace — hidden address spaces | `hypervisor-memory-introspection` |
| 2021-03-27 | Reverse Injector — merging address spaces | `stealth-injectors` |
| 2021-03-22 | MSREXEC — WRMSR → kernel exec | `byovd` |
| 2020-12-01 | PTM — page tables from usermode | `byovd` |
| 2020-11-01 | VDM — vulnerable driver manipulation | `byovd` |
| 2020-08-25 | PSKP — process-context specific kernel patch | `kernel-dev` |
| 2020-08-23 | Virtual Memory — paging intro | `windows-internals` |
| 2020-04-19 | Physmeme — unsigned kernel mapper | `byovd` |

## AC / game / misc

| Date | Title | Skill |
|---|---|---|
| 2022-09-26 | FiveM / GTA V client RCE | `game-hacking-exploits` |
| 2022-05-06 | Theodosius — jit linker / OBJ obfuscator | `llvm-lift-deobfuscation` |
| 2022-04-13 | Mutation engine vs Aimware | `binary-obfuscation-deconstruction` |
| 2021-08-12 | EQU8 kernel component | `ags-anti-cheat` |
| 2021-08-10 | EAC inject unsigned code into protected process | `eac-kernel-driver-re` |
| 2021-06-21 / 05-17 | VMProtect 2 architecture + static analysis | `virtualization-deobfuscation` (legacy) |
| 2022-01-20 | EZVIZ camera + SNES9X port | `firmware-pentest` |

## GitHub org map

`https://github.com/aftermathlabs` — 36 public repos. High-signal: `ring-1.io`, `Voyager`, `vmp2`, `themida-devirt`, `VDM`, `msrexec`, `vmhook`, `CallMeWin32kDriver`, `FakeEnclave`, `elderscroll`, `pdbgen2`, `llvm-msvc`. `llvm-msvc` is a compiler fork, not a skill.
