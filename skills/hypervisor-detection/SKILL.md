---
name: hypervisor-detection
description: "Use when detecting a hypervisor, QEMU/KVM/VMware/Hyper-V, or analysis environment from the guest: CPUID vendor leaves, FYL2XP1 timing, invalid CPUID leaves, SMBIOS/ACPI artifacts, RDTSC deltas. Complements stealth-hypervisor (hide) and qemu-anti-detection (QEMU identity)."
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: stealth
  author: Admin
  source: https://github.com/CR3Swapper/Hypervisor-Detection
  upstream: https://github.com/void-stack/Hypervisor-Detection
  triggers:
    - hypervisor detection
    - detect vm
    - cpuid fyl2xp1
    - hypervisor present bit
    - anti-vm
    - battleye hypervisor
---

# Guest hypervisor / VM detection

void-stack `Hypervisor-Detection` (CR3Swapper fork) plus the Secret Club / revers.engineering probes that anti-cheats actually ship. Use this to *score* a hide job. Implementing the hide is `stealth-hypervisor` or `qemu-anti-detection`.

Upstream: `https://github.com/void-stack/Hypervisor-Detection`  
Primary writeups: secret.club 2020-01-12 (BattlEye), 2020-04-13 (system emulation); revers.engineering “Detecting hypervisor presence on Windows 10”.

## Workflow

1. **Run the four void-stack checks first.** Build the VS solution (x86 and x64; x64 needs `asm64.asm` `FYL2XP1`). Record pass/fail per probe, not a single bool.
   - Done when all four results are written.

2. **Interpret each probe.**

| Probe | Mechanism | Hide implication |
|---|---|---|
| CPUID vs FYL2XP1 | QPC around `CPUID(1)` vs `FYL2XP1`, 5-run majority. VM if FYL2XP1 ≤ CPUID | TSC offsetting that only covers `RDTSC` still fails. Must shrink CPUID exit or add the bare-metal CPUID cost to TSC offset (`stealth-hypervisor`) |
| Highest low leaf | `CPUID(0x40000000)` vs `CPUID(CPUID(1).EAX)` register compare | Hypervisor leaves must alias the last basic leaf when absent |
| Invalid leaf | `CPUID(0x04201337)` vs `CPUID(0x40000000)` | Unknown leaves must return the *same* regs as `0x40000000`, not zeros |
| Known vendor | `CPUID(1).ECX[31]` then `CPUID(0x40000000)` EBX/ECX/EDX string | Clear ECX[31] **and** never return `KVMKVMKVM`, `Microsoft Hv`, `VMwareVMware`, `XenVMMXenVMM`, `prl hyperv  `, `VBoxVBoxVBox` |

   - Done when each fail names the countermeasure skill and the concrete lie to tell.

3. **Add identity probes** (these catch QEMU even when CPUID is clean):

```
CPUID.1 ECX[31], CPUID.40000000 vendor
wmic bios/baseboard/computersystem/diskdrive/keyboard
ACPI / SMBIOS type 0/1/2
registry HKLM\HARDWARE\Description\System\BIOS
timing: rdtsc ; cpuid ; rdtsc
```

Empty `Win32_Fan` / `CIM_Sensor` is a QEMU leak (`qemu-anti-detection`).
   - Done when a table of string/timing artifacts exists.

4. **Pin the environment.** Note HVCI/VBS (`vbs-hvci-research`), whether the box is already a Hyper-V root, and whether a custom type-2 is loaded. A “clean” desktop with Microsoft Hv in leaf `0x40000000` is expected under VBS — do not call that a QEMU fail.
   - Done when the report distinguishes Hyper-V-as-host vs research HV vs QEMU.

5. **Use the suite as a gate.** After any hide change, rerun this skill. One new fail is a regression, not noise.
   - Done when before/after rows match for every probe except the one you intended to flip.

## Vendor strings

```
KVMKVMKVM\0\0\0
Microsoft Hv
VMwareVMware
XenVMMXenVMM
prl hyperv
VBoxVBoxVBox
```

Unknown non-empty vendor at `0x40000000` is still a detect.

## Pair with

- `stealth-hypervisor` — implement the lies
- `qemu-anti-detection` — QEMU device / SMBIOS / RDTSC-KVM-Handler
- `hypervisor-dev` / `bluepill-type2-hv` — the VMM being hidden
- `x64dbg-anti-debugger` — DBVM / clock_watchdog (CR3Swapper `ANTI_DBVM_TRACE`)
- `ags-anti-cheat` — which AC ships which probe

## Verification

- [ ] All four void-stack probes recorded on the target
- [ ] CPUID leaf 0x40000000 bytes dumped, not just “hypervisor bit”
- [ ] SMBIOS/WMI/ACPI strings captured
- [ ] VBS/Hyper-V-root called out so Microsoft Hv is not mis-scored
- [ ] Hide work is gated on a re-run of this suite
