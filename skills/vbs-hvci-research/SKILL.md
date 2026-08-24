---
name: vbs-hvci-research
description: "Virtualization-Based Security research surface: VTL0/VTL1 split, Secure Kernel and Ium syscalls, HVCI kernel code-integrity enforcement, Credential Guard/LSAIso, what each stealth technique survives (DKOM vs code patches), VTL0→VTL1 attack surfaces (hypercalls, VMBus/VMWP, secure-kernel parsers), lab toggling and detection. Use when a target runs VBS/HVCI/Credential Guard or a technique must survive it."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: stealth
  triggers:
    - "VBS"
    - "HVCI"
    - "credential guard"
    - "VTL1"
    - "secure kernel"
    - "memory integrity"
---

# VBS / HVCI research

VBS runs a hypervisor-enforced privilege split: normal kernel (VTL0) and a
Secure Kernel (VTL1, `securekernel.exe`) that VTL0 cannot read, write, or
execute — even with ring-0 primitives. Every technique plan starts with one
question: **does this box run VBS/HVCI?** (`Get-CimInstance
Win32_DeviceGuard`, `msinfo32` "Virtualization-based security"; see
`windows-internals` §1 platform truth).

## Component map

| Component | What it actually enforces |
|---|---|
| VTL1 / Secure Kernel | owns higher privilege; VTL0 kernel memory is VTL1-readable, never the reverse |
| HVCI (Memory Integrity) | kernel code integrity decisions enforced with hypervisor help: unsigned images in kernel, RWX kernel pages, patching RX code — denied at EPT level, not at policy level |
| Credential Guard | lsass secrets in a VTL1 trustlet (LSAIso); VTL0 lsass holds no usable secrets |
| Kernel-data protection (KDP) | designated kernel *data* (e.g. flag tables) made RO from VTL0 |
| Secure Launch / DRTM | VTL1 measured into TPM (PCR17-23), remote attestation hooks |

## What this means for offensive techniques (the survival table)

| Technique (VTL0 attacker) | Without HVCI | With HVCI | Notes |
|---|---|---|---|
| Patch kernel code (SSDT write, inline hook ntoskrnl) | works | **denied** (EPT: RX pages not writable from VTL0) | PatchGuard + HVCI stack; don't burn days on it |
| DKOM on kernel *data* (callback arrays, unlink EPROCESS) | works | **still works** — data pages remain RW to VTL0 unless KDP-protected | `kernel-callbacks` techniques survive; KDP list grows over builds — verify per build |
| Manual-map your own driver | works | pages can be allocated NX/RX; execution allowed but **not signed-image-clean**: HVCI flags non-conforming images (no SHA-1 page hashes / non-standard) | mapping still *runs*; visibility to CI telemetry differs — see `manual-map-injector-engineering` |
| Direct syscalls from usermode | works | works (usermode technique; HVCI says nothing) | `edr-bypass-re` |
| Hypervisor of your own (Type-2 under Hyper-V) | — | **nested**: your VMX now runs under Microsoft's HV; needs enlightened handling or fails | `stealth-hypervisor` nested section |
| lsass credential dump (mimikatz-style) | works | **fails by design** — secrets live in the trustlet; VTL0 sees the RPC proxy only | attack the trustlet boundary or the RPC/proxy layer instead |
| Physical-memory drivers (BYOVD phymem) | kernel R/W | kernel R/W in VTL0 — VTL1 memory **not** physically reachable through normal translation as VTL0 sees its own GPA view (SMEE on modern silicon) | BYOVD power shrinks exactly to the VTL0 world; see `byovd` |

Practical rule: HVCI converts "patch the code" problems into "corrupt the
data" problems. Re-plan around data-plane techniques (DKOM, object surgery,
callback proxying) rather than fighting the EPT wall.

## VTL0 → VTL1 attack surface (research directions)

Where escapes actually get looked for — historical public work clusters here:

1. **Ium syscall interface**: VTL0 → VTL1 calls (`Ium*` secure-service
   dispatch, the trustlet door). Parser/argument-validation bugs in the
   secure kernel's syscall handlers are the classic escape class. Enumerate
   the dispatch table from `securekernel.exe` of the target build; diff
   handlers across builds to find new surface.
2. **Hypercalls from VTL0**: the hypercall layer trusts caller VTL — bugs
   that confuse which VTL issued a call, or race hypercall state, are
   escapes.
3. **VMBus / VMWP**: the worker process and message channels bridging
   trustlets and VTL0 components — channel message parsing and the usermode
   worker are a wide, less-audited surface.
4. **Trustlet resource injection**: paths where VTL0-supplied data (files,
   blobs passed to secure services) is parsed inside VTL1.
5. **Attestation logic**: not an escape, but logic flaws in what VTL1
   measures/believes (Secure Boot state, PCR composition) — pairs with
   `secure-boot-uefi-research`.

Methodology for any of these: same as kernel 0-day work
(`windows-driver-0day`, `windows-0day-hunting`) but the binary is
`securekernel.exe` + VTL1 side of win32k/nt semantics; symbols exist on the
symbol server for many builds — pull them first.

## Credential Guard specifics

- VTL0 lsass (`lsass.exe`) hosts LSAIso proxy stubs; real credential ops run
  in the trustlet (`lsaiso.exe` visible via secure process enumeration).
- Dumping VTL0 lsass still yields *some* material (logon sessions, older
  SSP data) on older builds — inventory before concluding; per-build
  behavior drifts.
- Attack angles that historically mattered: the RPC interface between the
  proxy and trustlet, DPAPI masterkey flows, and anything that convinces
  lsass *not* to delegate (config downgrades, registry policy flips where
  policy isn't measured).

## Lab control (research box)

```
bcdedit /set hypervisorlaunchtype off    # kills Hyper-V underpinning entirely
# HVCI toggle (reboot): HKLM\SYSTEM\CurrentControlSet\Control\DeviceGuard
#   Scenarios\HypervisorEnforcedCodeIntegrity Enabled=0 / "Enabled" VM-level
```

Caution: on BitLocker-protected boxes, VBS/Secure-Boot-bound state can
trigger recovery prompts — suspend BitLocker before lab flips and restore
state after (`secure-boot-uefi-research`). Keep a known-good `bcdedit /enum`
snapshot to restore.

Two-lab discipline: one HVCI-off box for technique development, one
HVCI-on/default box for validation — never assume survival from the dev box.

## Detection (from VTL0, blue-side checks)

- `Get-CimInstance Win32_DeviceGuard` (VBS status, security services
  running), `msinfo32`, Event Log: Microsoft-Windows-DeviceGuard.
- CI event logs (Microsoft-Windows-CodeIntegrity) reveal HVCI blocks — as an
  attacker, these logs are your own tripwire during testing.

## Pair with

`windows-internals` (platform truth first), `stealth-hypervisor` (nested
realities), `kernel-callbacks` (the surviving DKOM surface),
`manual-map-injector-engineering`, `byovd` (primitive reachability),
`secure-boot-uefi-research` (the trust chain below VBS),
`tpm-attestation-research` (the attestation side).
