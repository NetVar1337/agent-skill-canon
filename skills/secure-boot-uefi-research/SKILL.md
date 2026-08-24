---
name: secure-boot-uefi-research
description: "Boot-trust chain research: UEFI phases and variable services (PK/KEK/db/dbx), Secure Boot enforcement points, BCD policy persistence (testsigning/nointegritychecks), BitLocker key hierarchy and PCR binding, measured boot vs secure boot, bootkit precedents and their bypass classes, lab setup for OVMF/hardware research. Use when work touches boot trust, BitLocker, or pre-OS code."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: stealth
  triggers:
    - "secure boot"
    - "uefi"
    - "bitlocker"
    - "bootkit"
    - "bcdedit"
    - "measured boot"
    - "tpm pcr"
---

# Secure Boot / UEFI research

The chain, in enforcement order: firmware (SEC→PEI→DXE→BDS) verifies the
boot loader (db signatures), bootmgr/winload verify kernel + ELAM + Critical
services, kernel CI takes over. Each link has distinct attack surface;
identify *which link* a question belongs to before touching anything.

## Inventory the box first (windows-internals platform truth)

```powershell
Confirm-SecureBootUEFI                       # SB state (needs admin, UEFI boot)
bcdedit /enum {current}                      # nointegritychecks/testsigning flags
manage-bde -status                            # BitLocker state, protector IDs
Get-Tpm; Get-TpmEndorsementKeyInfo           # TPM present/owned/attestation-ready
# Measured-boot evidence: parse C:\Windows\Logs\MeasuredBoot\*.log (TCG log) directly
```

UEFI variables live in `HKLM\SYSTEM\CurrentControlSet\Control\SecureBoot`
(state mirror) and the real store in NVRAM — query with
`GetUefiVariable`-style tools or the firmware shell; never assume the
registry mirror is authoritative.

## Variable/authority model

- `PK` (Platform Key) owns `KEK`; `KEK` updates `db` (allowed signers) /
`dbx` (revocation). `SetupMode` variable open at deployment = the classic
"deploy own keys" lab path (physical presence required).
- dbx updates arrive via Windows Update — for bootkit research, track dbx
  revocations as the defender's move history (each entry = a killed bypass).
- `*Default` variables matter: on some firmware, failure to read db falls
  back to vendor defaults — historically a bypass class (default-db
  trusting vendor-only chains).

## BCD policy layer (most-used in driver work)

```
bcdedit /set testsigning on            # allows test-signed kernel modules
bcdedit /set nointegritychecks on      # CI bypass for ELAM-era checks (may be ignored on recent builds)
bcdedit /set debug on                  # KDNET — pairs with windbg-ttd
```

These live in the BCD store (`\EFI\Microsoft\Boot\BCD` + registry hive
`HKLM\BCD00000000` view). Research notes: (a) flags are themselves measured
into PCR context on some configs (BitLocker sees tampering), (b) bootmgr
honors them before winload — a bootkit's cheapest trick is BCD surgery,
which is why defenders watch the store hash, (c) some flags are ignored when
Secure Boot is ON — always verify effective policy with an actual
unsigned-driver load test, not documentation.

## BitLocker key hierarchy

- FVEK (encrypts data) ← VMK ← protector(s): TPM (PCR-bound), PIN, USB,
  recovery password, AD escrow.
- PCR binding for TPM-only: PCR0..7 (firmware/config), PCR11 (bitlocker/
  access control). `testsigning` flips measured state → recovery prompt on
  next boot. Research flow: `manage-bde -protectors -get C:`, note PCRs,
  `suspend` before lab changes, `resume` after — avoid burning recovery
  prompts (they're logged and, on managed boxes, noticed).
- "Hardware encryption" on cheap SSDs (TCG Opal self-encrypting drives with
  default keys) historically degraded to trivially-decryptable states —
  check `manage-bde -status` "Encryption Method" before assuming FVEK-based
  crypto.

## Measured vs Secure Boot (don't conflate)

- Secure Boot = signature enforcement (an *active gate*).
- Measured Boot = PCR hash log (TPM) of everything booted — *evidence, not
  enforcement*. You can boot unsigned stuff and TPM happily records your sin;
  detection comes only from attestation (remote/PCR quotes — see
  `tpm-attestation-research`) or BitLocker's PCR-bound unsealing.
- TCG log (`\Windows\Logs\MeasuredBoot\*.log`) is parseable — as an attacker,
  read it to know exactly what you changed that got measured.

## Bootkit precedent classes (what public attacks did)

| Class | Mechanism | Lesson |
|---|---|---|
| MBR/VBR bootkits (pre-UEFI era) | stage0 in boot sector | dead on UEFI+GPT default installs |
| UEFI implant (LoJax-style) | DXE/runtime driver written to ESP/firmware | persistence below OS reimaging |
| Vulnerable-bootloader bypass (BlackLotus lineage) | abused a *signed-but-vulnerable* bootmgr/winload + dbx gaps to run unsigned policy | signing ≠ correctness; revocation lag is the window |
| BCD/policy surgery | flip nointegritychecks via physical/direct disk access | cheapest pre-OS tamper; measured |
| ESP implant | replace/chain bootloader on EFI System Partition | needs the partition writable — check BitLocker/ESP protections |

When researching a new one: diff the ESP contents (`mountvol S: /S`), hash
bootmgr/winload against known-good from the installed build, parse the TCG
log for unexpected measurements before the loader.

## Lab setups

- **QEMU + OVMF** (in WSL2/secondary box — QEMU not installed locally,
  install per need): enroll your own PK/KEK/db (`KeyTool` on the OVMF
  frontend), sign test binaries with created certs — full authority over
  every link; the only way to cheaply iterate on dbx/PKI logic.
- **Hardware**: Intel Boot Guard present boxes (OEM signed IBB) resist
  firmware replacement — check with `bootguard` status tools before planning
  firmware-level persistence experiments; boxes without Boot Guard remain
  open to SPI reflashing (CH341A-class programmer for extraction — pairs
  with `hardware-security`).
- Tooling gaps on this workstation: no UEFITool/chipsec — install when the
  task needs them and record in `tool-index.md`.

## OPSEC/tripwires while researching on live boxes

- Secure Boot state changes and BitLocker recovery events land in Event Log
  (Microsoft-Windows-BitLocker, Event 246/546 families) — clean your lab
  state *before* abandoning a box, or note the noise you made.
- Never flip secure-boot-critical vars on a box whose BitLocker recovery
  keys you don't hold.

## Pair with

`vbs-hvci-research` (the trust stack above boot), `tpm-attestation-research`
(PCR/attestation deep dive), `windbg-ttd` (boot debugging:
`bcdedit /bootdebug`), `firmware-pentest` (embedded/IoT firmware flows),
`hardware-security` (SPI/JTAG extraction), `windows-internals` (platform
truth first).
