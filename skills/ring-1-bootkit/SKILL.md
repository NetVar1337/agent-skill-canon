---
name: ring-1-bootkit
description: "Use when reversing or detecting a Ring-1.io-class bootkit cheat: bootmgfw implant, Hyper-V SLAT/MTF hooks, cloned game page tables, EPT-hidden inject, or the Aftermath Labs ring-1.io corpus and 2026 writeup."
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: game
  author: Admin
  source: https://github.com/aftermathlabs/ring-1.io
  upstream: https://aftermathlabs.net/blog/04/02/2026/
  triggers:
    - ring-1
    - ring-1.io
    - bootloader implant
    - aftermath ring
    - bootmgfw cheat
---

# Ring-1.io bootkit analysis

Aftermath Labs (xeroxz / noahware / Eggsy / AVX) dumped and deobfuscated the Ring-1.io commercial cheat stack. PRIMARY here is *analysis and detection of that architecture*, not a rebuild.

Corpus: `https://github.com/aftermathlabs/ring-1.io` (also `backengineering/ring-1.io`)  
Writeup: `https://aftermathlabs.net/blog/04/02/2026/`  
Deobf engine they used: in-house BLARE2 (`https://back.engineering/blare/`) — not public. Use `virtualization-deobfuscation` + `llvm-lift-deobfuscation` as the stand-in.

## Artifact map

| File | Role |
|---|---|
| `bootmgfw.bin` | Tampered Windows boot manager |
| `bootloader-implant-obfuscated.bin` | On-disk implant |
| `bootloader-implant-deobfuscated.bin` + `.i64` | Decompressed/decrypted implant + IDA DB |
| `loader-deobfuscated.bin` | Usermode/chain loader |
| `*-cheat.bin` | Injected game modules (Apex, BattleBit, COD6, EFT, Grayzone, R6) |

Hash every blob before opening. Work offline.

## Workflow

1. **Confirm the chain, do not start in the game DLL.** Order on the writeup: Loader → bootloader implant → map into Hyper-V → SLAT + MTF → GPA redirect → VMEXIT hooks → implant comms → clone target CR3 → insert PTEs → hypercall to load tables → hide tables + contents via EPT → usermode hooks.
   - Done when a one-page chain diagram names the file that implements each hop.

2. **Bootloader implant.** Open `bootloader-implant-deobfuscated.i64` (IDA 9.4). Recover: how `bootmgfw.ImgArchStartBootApplication` / `BlImgStartBootApplication` is hooked, where winload/hvloader hooks land, how the Hyper-V image is found. This is Voyager’s hookchain (`hypervisor-memory-introspection`).
   - Done when hook sites have names + RVAs in the IDB.

3. **Hyper-V payload.** Recover SLAT setup, EPT-violation handler, MTF single-step, VMEXIT table. GPA redirect = the implant’s physical R/W. Compare to Voyager (append-to-hv image) vs hyper-reV (standalone attachment, timestamps restored).
   - Done when EPT-violation and MTF paths are commented and the comms ABI (CPUID/VMCALL/shared page) is written down.

4. **Process injection.** Clone the game’s page tables, insert malicious PTEs, ask the HV to switch/load them, hide the new tables and the payload pages with EPT execute/read split. Game module `*-cheat.bin` is the payload, not the persistence.
   - Done when one cheat.bin is mapped, its imports/entry named, and the PTE-clone step is cited to a function in the implant.

5. **Detections (write these as a lab checklist).** Aftermath section “Possible Detections”:
   - `bootmgfw.efi` hash / Authenticode vs known-good
   - Measured-boot / TPM PCR for the replaced boot manager (`tpm-attestation-research`)
   - Hyper-V image size / allocation-after-hvix64 shift (Voyager-class)
   - EPT split on game or ntos pages → `ept-hook-detection`
   - Extra CR3 / cloned PML4 not backing a real process
   - Unexpected VMEXIT rate on the game’s `.text`
   - Done when each detector has a command or probe and a pass/fail on the corpus (static) or a lab VM.

6. **Do not treat vmp2 handler tables as the deobf plan.** Aftermath retired that. Themida (2026-05-09) and Tencent VM (2026-07-31) posts: incremental lift, const/memory model, DCE, branch fold, lower 1:1. Route the `*-cheat.bin` protectors through `virtualization-deobfuscation`.
   - Done when the protector on each cheat.bin is labeled and the recovery product is chosen.

## Pair with

- `hypervisor-memory-introspection` — Voyager / hyper-reV mechanics
- `ept-hook-detection` — guest-side SLAT-hook probes
- `secure-boot-uefi-research` / `analyzing-uefi-bootkit-persistence`
- `virtualization-deobfuscation` / `llvm-lift-deobfuscation`
- `anti-cheat-bypass` / `eac-kernel-driver-re` — what Ring-1 is hiding from

## Verification

- [ ] All corpus hashes recorded
- [ ] Boot hook RVAs named in the IDB
- [ ] SLAT/MTF/comms ABI written
- [ ] One game module’s entry + inject primitive identified
- [ ] Detection checklist has an observable per row
- [ ] No live multiplayer client was instrumented
