---
name: plouton-smm
description: "Use when building or extending a Plouton-class SMM (ring -2) game or memory framework: EDK2 SMM module, XHCI SMI on USB, Windows physical memory walk, SPI firmware implant, or PiSmmCpuDxeSmm page-table patch."
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: firmware
  author: Admin
  source: https://github.com/CR3Swapper/plouton
  upstream: https://github.com/pRain1337/plouton
  triggers:
    - plouton
    - SMM cheat
    - ring -2
    - SMM game framework
    - XHCI SMI
    - PiSmmCpuDxeSmm
---

# Plouton SMM framework

pRain1337 Plouton (CR3Swapper fork) is an Intel SMM module that runs as a DXE/SMM implant: it walks Windows physical memory, fires on XHCI USB SMIs, and talks to mice/headsets from ring -2. OS-visible drivers, EPT hooks, and DMA are different products (`kernel-dev`, `hypervisor-dev`, `ags-dma-attack`).

Upstream: `https://github.com/pRain1337/plouton`  
Related: `https://github.com/jussihi/SMM-Rootkit`, `https://github.com/Oliver-1-1/SmmInfect`

## Constraints

- Intel only for XHCI-generated SMIs. AMD can reuse the memory walk, not the USB event path.
- Tested chipsets: 200-series through 700-series. 800-series offsets exist in the datasheet, unverified.
- Windows guest only.
- Post-~2021 boards need `PiSmmCpuDxeSmm` patched or SMM page faults kill OS-memory access.
- Flash via external SPI. Onboard OS flashes are blocked on current Intel.

## Workflow

1. **Get EDK2 vUDK2018 and the Plouton module building.** `git submodule update --init`. Docker: `docker pull jussihi/edk-builder` then `docker run --privileged -v .:/root -u root -w /root jussihi/edk-builder /bin/bash docker_build.sh`. Windows: `ACTIVE_PLATFORM=OvmfPkg/OvmfPkgX64.dsc`, `TARGET=RELEASE`, `TARGET_ARCH=X64`, add `Plouton/Plouton.inf` to the DSC, `edksetup.bat rebuild`, `build`.
   - Done when `Plouton.efi` exists (OVMF path: `edk2/Build/OvmfX64/RELEASE_GCC5/FV` plus the module EFI).

2. **Decide lab vs real SPI.** QEMU/OVMF is the first target. Real hardware requires a dump (`flashrom` + CH341A or Pi SOIC-8), UEFITool 0.28.0 body replace, and a verified restore image.
   - Done when the chosen path is written (OVMF file *or* SPI dump hash + programmer).

3. **On real firmware after ~2021, patch `PiSmmCpuDxeSmm`.** Find `SmmInitPageTable` via the page-fault error strings in `ArchExceptionHandler.c`. Neutralize the SMM paging lockdown variable (hard-code 0) so SMM can map OS RAM. Replace the PE32 body. See jussihi `SMM-Rootkit/UefiCpuPkg`.
   - Done when the modified `PiSmmCpuDxeSmm` is in the image and the original body is archived.

4. **Insert Plouton.** In UEFITool, replace a non-critical SMM module’s PE32 body with `Plouton.efi`. Save. Flash:

```
flashrom -p linux_spi:dev=/dev/spidev0.0,spispeed=512 -n -r backup.bin
flashrom -p linux_spi:dev=/dev/spidev0.0,spispeed=512 -n -w patched.bin
```

Power off before clipping the chip.
   - Done when the board POSTs and Plouton serial or memory log is visible. Windows `serial.sys` will swallow SMM serial — rename it or open a local COM client.

5. **Add or select a target.** `TargetEntry` in `target/targets.h`: process name, `InitCheat`, `CheatLoop`. `InitCheat` finds EPROCESS/modules. `CheatLoop` runs every SMI. Register `.c` in `Plouton.inf`. Stock targets: CS2 (aim + sound ESP) and Hermes (SMM debugger).
   - Done when `InitCheat` returns TRUE on the live process and one `CheatLoop` tick reads a known guest value.

6. **Bind hardware.** Mouse/audio go through XHCI endpoint rings. Collect Context Type, Max Packet Size, Average TRB Length, and a packet magic. Procedure is in [references/xhci-device.md](references/xhci-device.md). Guard the new `.c` with `#ifdef` in `general/config.h`.
   - Done when `getEndpointRing` returns a live ring and a test move/beep is observed.

## Memory primitive

`memory/memory.c` + `os/windows/NTKernelTools.c`: physical read/write from SMM, then Windows kernel structure walk (EPROCESS, modules, CR3). Hermes is the generic dump/debug target — prefer it over forking CS2 when the job is introspection.

## Pair with

- `auditing-uefi-firmware-with-chipsec` — SMM_BWP / SMRR / SPI lock *before* you flash
- `analyzing-uefi-bootkit-persistence` — implant detection after the fact
- `secure-boot-uefi-research` — Secure Boot / measured boot will see the volume
- `ags-dma-attack` — out-of-band RAM when SMM is locked down
- `game-hacking` / `offset-dumper` — CS2 offsets, not the SMM loader

## Verification

- [ ] `Plouton.efi` built from vUDK2018, not random edk2-HEAD
- [ ] OVMF path boots and logs, *or* SPI backup hash exists before write
- [ ] Post-2021 images include the `PiSmmCpuDxeSmm` page-table patch
- [ ] Target `InitCheat` succeeds against the named process
- [ ] One confirmed physical read of a known guest pattern
- [ ] USB device path has Type / MPS / TRB / magic recorded
- [ ] Serial or `PloutonLogViewer` captured the SMI tick
