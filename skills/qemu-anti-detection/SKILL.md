---
name: qemu-anti-detection
description: "Use when hiding a QEMU/KVM or Proxmox guest from anti-cheat or packer VM checks: apply zhaodice qemu-anti-detection patches, libvirt SMBIOS/CPU XML, RDTSC-KVM-Handler, and remaining WMI/timing holes. Not for hiding a custom type-2 HV (use stealth-hypervisor)."
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: stealth
  author: Admin
  source: https://github.com/CR3Swapper/qemu-anti-detection
  upstream: https://github.com/zhaodice/qemu-anti-detection
  triggers:
    - qemu anti-detection
    - hide qemu
    - kvm hidden
    - proxmox anti-detection
    - qemu eac
    - qemu themida
---

# QEMU / KVM anti-detection

zhaodice `qemu-anti-detection` (CR3Swapper fork) rewrites QEMU-reported identity so guest AC and packers stop matching “QEMU keyboard”, QEMU serials, the UEFI VM bit, and BGRT. It does **not** fix RDTSC/KVM timing. Custom type-2 HV hide is `stealth-hypervisor`. Guest-side probe construction is `hypervisor-detection`.

Upstream: `https://github.com/zhaodice/qemu-anti-detection`  
PVE sibling: `https://github.com/zhaodice/proxmox-ve-anti-detection`  
Timing sibling: `https://github.com/WCharacter/RDTSC-KVM-Handler`

## Claimed vs not

| Engine | Patch-only | Also needs host kernel RDTSC hook |
|---|---|---|
| ACE, EAC, Mhyprot, nProtect, Enigma, Shielden, Themida, VMProtect, VProtect | usually | no |
| Gepard Shield | no | yes |
| Roblox | maybe + Hyper-V *inside* the guest | — |
| Vanguard | fail (“Incorrect function”) | out of scope for this patch |

Unfixed WMI (these return “No instance(s) available” and leak VM):

```
Win32_Fan  Win32_CacheMemory  Win32_VoltageProbe
Win32_PerfFormattedData_Counters_ThermalZoneInformation
CIM_Memory  CIM_Sensor  CIM_NumericSensor
CIM_TemperatureSensor  CIM_VoltageSensor
```

## Workflow

1. **Pick the QEMU series and keep distro QEMU installed.** Patches: `qemu-6.2.0`, `7.0.0`, `7.2.0`, `8.0.2`, `8.0.5`, `8.1.0`, `8.2.0`, `10.2.2`. Build lands in `/usr/local/bin` and must shadow, not replace, package QEMU.
   - Done when `qemu-system-x86_64 --version` from `/usr/local/bin` matches the patched tarball.

2. **Apply and build.**

```bash
git clone https://github.com/zhaodice/qemu-anti-detection.git
wget https://download.qemu.org/qemu-10.2.2.tar.xz
tar xvJf qemu-10.2.2.tar.xz
cd qemu-10.2.2
git apply ../qemu-anti-detection/qemu-10.2.2.patch
./configure
sudo make install -j$(nproc)
```

Arch deps: `git wget base-devel glib2 ninja python`. Ubuntu: `git build-essential ninja-build python-venv libglib2.0-0 flex bison`.
   - Done when `git apply` is clean and `make install` produces the new binary.

3. **Rewrite the libvirt domain.** Required shape is in [references/libvirt-xml.md](references/libvirt-xml.md): `smbios mode=host`, `kvm hidden`, `hypervisor` CPU feature disabled, Hyper-V vendor_id `GenuineIntel`, `-cpu host,...,hypervisor=off`, unique UUID, ASUS-like type 0/1/2/3/4/17 SMBIOS.
   - Done when `virsh dumpxml <name>` shows `hypervisor=off`, `kvm hidden`, and no leftover `QEMU` / `BOCHS` / `SeaBIOS` strings.

4. **Close timing if the target is Gepard or a TSC-delta AC.** Install `RDTSC-KVM-Handler` on the *host* kernel. Re-measure `CPUID` vs `FYL2XP1` and `RDTSC` around `CPUID` from the guest (`hypervisor-detection`).
   - Done when guest `CPUID` duration is not systematically ≥ `FYL2XP1` across 5-run majority vote.

5. **Run the guest detector suite.** From the guest: `hypervisor-detection` probes, `wmic bios / computer / baseboard / diskdrive get *`, Device Manager names, UEFI VM bit, BGRT. Record every remaining `QEMU` / `VirtIO` / `Red Hat` string.
   - Done when a written before/after table exists. Residual WMI sensor holes stay listed as known leaks.

6. **Do not claim Vanguard or a custom HV is hidden.** Vanguard is a documented fail. A Bluepill/hyper-reV/Valthrun stack is a different product.
   - Done when the report names the engines actually retested and the holes that remain.

## Pair with

- `hypervisor-detection` — guest probes that this patch is scored against
- `stealth-hypervisor` — hide a *custom* VMM, not QEMU device identity
- `hwid-identifier-surfaces` — disk/NIC/SMBIOS serials the XML must not reuse
- `tpm-attestation-research` — measured-boot / TPM PCR will still see the host
- `eac-ban-stack` — EAC is more than SMBIOS strings

## Verification

- [ ] Patched QEMU version matches the applied `.patch` file
- [ ] Distro QEMU package still present
- [ ] Domain XML has unique UUID, `hypervisor=off`, `kvm hidden`, host SMBIOS
- [ ] Guest strings no longer contain QEMU/BOCHS/SeaBIOS in BIOS, disk, keyboard
- [ ] Detector suite recorded; WMI sensor holes explicit
- [ ] Gepard/TSC targets have host `RDTSC-KVM-Handler` or are marked unfixed
- [ ] Vanguard not listed as bypassed
