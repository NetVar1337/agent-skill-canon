---
name: hypervisor-memory-introspection
description: "Hypervisor-powered memory introspection / Hyper-RE: SLAT-based R/W, stealth reads, guest instrumentation, AC/game RE from VMM, or noahware hyper-reV (Hyper-V boot implant + CPUID hypercalls under HVCI)."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: stealth
  triggers:
    - "Hyper-RE"
    - "memory introspection"
    - "EPT read"
    - "VMM introspection"
---

# Hypervisor memory introspection (Hyper-RE)

Use a VMM to observe/modify guest memory and control flow for RE and AC research.

## Capabilities
- EPT/NPT violate-on-access → shadow pages / split view
- Invisible reads of game/AC memory from host
- Breakpoints without guest-visible INT3 if carefully designed
- Hide VMM artifacts (CPUID, timing) — `stealth-hypervisor`

## Workflow
1. Bring up minimal HV with logging
2. Locate target EPROCESS/CR3
3. Translate GVA→HPA via guest walk or EPT identity map strategy
4. Install execute/read hooks on modules of interest
5. Export traces into IDA/AiDA / offline analysis

## GVA→HPA translation (the load-bearing primitive)
```
// host side, per target CR3
u64 table = cr3 & PHYS_MASK;
for (lvl = PML4; lvl <= PT; lvl++) {
  entry = readPhys(table + ((gva >> shift(lvl)) & 0x1FF) * 8);
  if (entry & PTE_LARGE) { resolve huge-page; break; }
  if (!(entry & PTE_PRESENT)) return ACCESS_FAULT;  // swapped: bail, let guest fault
  table = entry & PHYS_MASK;
}
return table + (gva & OFFSET_MASK(lvl));
```
- Host physical read: `MmMapIoSpace`/`MmGetVirtualForPhysical` on Windows VMM drivers, or prehost map of full guest RAM ranges in a type-1 research HV.
- Cache: L1 = last (CR3, PML4 slot), L2 = resolved PDEs; invalidation on MOV_CR3 exits (check CR3 target != cached, not every switch — PCID-aware).

## Split-view EPT via VMFUNC (leaf 0)
- Build N EPTP pointers (view 0 = clean/true, view 1 = patched). Each EPTP needs distinct memory; VMCS `EPTP_LIST` address in `VMCS_CTRL` + `secondary_proc_based_ctl.VMFUNCTIONS` enabled, `EPT_VIOLATION #VE` info page optional.
- `VMFUNC(0, view_id)` executes in guest without exit — sub-microsecond view swap from hooked guest code itself (self-concealing payload) or trigger via NMI/MTF from host.
- Canonical stealth read setup: view 0 (exec) has INT3/hook page; view 1 (data/read) serves pristine bytes. AC scanning its own memory reads pristine; execution hits hook.

## MTF-based stealth hooks (no INT3 bytes at all)
```
1. EPT mark target page X-only-on-execute → execute triggers EPT_VIOLATION exit
2. Host: note guest RIP, swap page permissions to RX but replace with *original* + set MTF
3. Resume -> instruction executes (single) -> MTF exit
4. Restore X-only violation state; emulate/redirect as needed; continue
```
- Zero guest-visible byte changes; survives memory-compare integrity checks; cost = 2 exits per hooked instruction (only hot on tight loops).
- MTF exit handling: clear `MONITOR_TRAP_FLAG` in VM-entry interrupts; read guest RIP via VMCS_GUEST_RIP; single-instruction length via decode (Zydis on host — `zydis-disassembly-engineering`).

## Breakpoint classes from the VMM
| Hook class | Mechanism | Guest-visible? |
|---|---|---|
| Page-execute gate | EPT X violation + MTF | none (no bytes touched) |
| Read-gate (detect AC scans) | EPT R violation on specific data page | timing only |
| Full split-view | VMFUNC EPTP switch | none, self-service from guest |
| CR/MSR gates | exit on MOV_CR3/MOV_DR/MSR writes | none; great for anti-debug research |

## Introspection reads that matter
- Find EPROCESS of game: walk guest KPCR → PRCB → current thread → ApcState.Process, or CR3 → physical scan for DirectoryTableBase match with ImageFile_Name string (crash-consistent snapshot first — pause all vCPUs).
- Module list in guest: guest PEB (found via target thread TEB → ProcessEnvironmentBlock) → LDR InLoadOrderModuleList; cache bases, re-verify per build stamp.
- Export resolution: parse guest PE export table through EPT reads; never trust symbols across patches.

## VBS/HVCI reality check
- If target runs with VBS enabled (Hyper-V root), your custom HV cannot coexist — options: nested virt (perf + detection cost), disable VBS (changes system state; server-side attestation may notice), or DMA-based out-of-band read (pcileech on E:\Tools — no HV needed, `ags-dma-attack`).
- Detection of your HV by AC: CPUID leaf inconsistencies, TSC deltas (pair `stealth-hypervisor` countermeasures), EPT-violation latency spikes during heavy hook traffic — budget exits, batch trace points.

## Workflow additions
6. Trace format: RIP + bytes + regs at each gate → export to JSONL; feed IDA via `ida-reverse` import script for auto-annotation.
7. Snapshot/rollback guest RAM before invasive mutation; resume from snapshot for repeated experiments.

## Voyager hookchain (xeroxz, archived)

`aftermathlabs/Voyager` — Windows 10 x64 1507–2004, Intel **and** AMD. Secure Boot / legacy BIOS unsupported. VisualUefi + NASM.

Order:
1. Replace/hook `bootmgfw.efi`
2. `ImgArchStartBootApplication` (2004–1709) or `BlImgStartBootApplication` (1703–1511) → hook winload before it runs
3. winload `BlLdrLoadImage` (or hvloader.efi on 1703–1507) → hook hvloader
4. Inject a module into Hyper-V and hook VMEXIT

Detection leftover hyper-reV later removed: implant appended to the Hyper-V image shifts later allocations. Use Voyager when the sample is that vintage; use hyper-reV on HVCI boxes.

## hyper-reV (Hyper-V parasitic VMM)

noahware/hyper-reV (CR3Swapper fork) is the HVCI-compatible Hyper-RE path: replace `bootmgfw.efi` with `uefi-boot`, restore the original file+timestamps, insert `hyperv-attachment` into Hyper-V, then talk to it from usermode via CPUID-shaped hypercalls. Use this when VBS/HVCI is on and a Bluepill-class type-2 cannot coexist (`vbs-hvci-research`).

Boot: copy `bootmgfw.efi` → overwrite with `uefi-boot.efi` + drop `hyperv-attachment` beside it → next boot restores bootmgfw metadata, allocates a UEFI page heap, identity-maps, hooks `hvloader` launch, loads the attachment, deletes it from disk.

Hypercalls (CPUID with magic regs; unknown CPUID is passed to Hyper-V):

| Call | Effect |
|---|---|
| `guest_physical_memory_operation` | R/W GPA |
| `guest_virtual_memory_operation` | R/W GVA |
| `translate_guest_virtual_address` | GVA→GPA |
| `read_guest_cr3` | current guest CR3 |
| `add_slat_code_hook` / `remove_slat_code_hook` | EPT/NPT hook |
| `hide_guest_physical_page` | hide GPA from guest |
| `log_current_state` / `flush_logs` | trap-frame log |
| `get_heap_free_page_count` | attachment heap |

Usermode CLI: `rgpm`/`wgpm`/`cgpm`, `gvat`, `rgvm`/`wgvm`/`cgvm`, `akh`/`rkh` (kernel SLAT hook + RIP-relative fixup), `hgpp`, `fl`, `lkm`/`kme`/`dkm`. Detour holder is an existing `ntoskrnl` page, not a fresh RX allocation.

Compile: NASM + VisualUefi/EDK2; `#define _INTELMACHINE` in `arch_config.h` or comment it out for AMD. `load-hyper-reV.bat` as admin copies both binaries to EFI. Secure Boot needs a vulnerable bootloader. TPM measured boot will log `uefi-boot` — disable TPM if the target attests PCRs.

Stealth claims to keep: attachment is *not* appended to the Hyper-V image (avoids “allocation after hvix64 shifted by implant size”), hooks apply only to the SLAT-protected final image, bootmgfw timestamps restored.

## Pair with
`stealth-hypervisor`, `hypervisor-dev`, `bluepill-type2-hv`, `kevlar-driver-emulation`, `game-hacking`, `ida-reverse`, `secure-boot-uefi-research`.

## Refs
- UC: Hyper-RE / memory introspection threads
- Local stacks: Hypervisor-SVM trees, Valthrun zenith/kernel loaders
