---
name: bluepill-type2-hv
description: "Use when building or debugging a Xeroxz/CR3Swapper Bluepill-class type-2 Intel hypervisor: host-owned GDT/TSS/IDT, self-referencing PML4 PTE map, VMX-root SEH, VDM physical R/W, or VMCS invalid-guest-state / control-error #7 bring-up."
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: hypervisor
  author: Admin
  source: https://github.com/CR3Swapper/bluepill
  upstream: https://githacks.org/_xeroxz/bluepill
  triggers:
    - bluepill
    - xeroxz hypervisor
    - type-2 intel hv
    - vmxroot SEH
    - self-ref PML4
    - VDM hypervisor
---

# Bluepill-class type-2 Intel HV

Xeroxz Bluepill (CR3Swapper fork, archived) is a WDK type-2 Intel VMM for Windows 10. Use this skill for that lineage. Generic VMX/EPT scaffolding is `hypervisor-dev`. Detectability budget is `stealth-hypervisor`. Guest memory analysis from a VMM is `hypervisor-memory-introspection`.

Primary tree: `https://github.com/CR3Swapper/bluepill` (copy of `githacks.org/_xeroxz/bluepill`).

## Scope

Route here when the next artifact is one of:

- VMXON / VMLAUNCH bring-up that dies with VM-instruction error 7 (invalid controls) or exit reason `0x80000021` (invalid guest state)
- Host-owned GDT, TSS with extra IST stacks, IDT that forwards `#DB`/`INT3` to the guest
- Self-referencing PML4 used as a two-PTE physical map (no PDPT/PD/PT walk)
- VMCALL command block for `translate` / `copy_virt` / `read_phys` / `write_phys` / `dirbase`
- SEH that must work in VMX-root whether the driver is `sc start` or manually mapped

## Tooling

| Need | Local |
|---|---|
| WDK / MSVC | VS 2022 Community + Kits `10.0.26100.0` / `10.0.28000.0` (`tool-index.md`) |
| Kernel debug | WinDbg classic `C:\Program Files (x86)\Windows Kits\10\Debuggers\x64\` |
| Symbols | `windows-symbols-debugging` |
| Guest RE of the demo | IDA 9.4 / radare2 6.2 |

## Workflow

1. **Pin the lineage.** Confirm the tree is Bluepill-shaped: `vmxon.cpp`, `vmcs.cpp`, `mm.cpp`, `idt.cpp`, `command.hpp`, `demo/vdm_ctx`. Record Windows build and CPU (`IA32_VMX_BASIC`, `IA32_VMX_PROCBASED_CTLS2` high/low).
   - Done when revision ID, allowed-1 / allowed-0 control masks, and whether Intel PT / XSAVES exist are written down.

2. **Fix VMCS controls before VMLAUNCH.** Apply high/low MSR masks *after* setting desired bits. Never force `conceal_vmx_from_pt` or `enable_xsaves` on a CPU whose `CTLS` mask rejects them. See [references/vmcs-bringup.md](references/vmcs-bringup.md).
   - Done when a dump of pin/primary/secondary/exit/entry controls matches `(desired & allowed1) | allowed0`.

3. **Write guest state to Intel Vol 3C ch. 24 + 26.** Set `VMCS_GUEST_ACTIVITY_STATE = 0`. Copy live CR0/CR3/CR4, segment bases/limits/AR bytes from `SGDT`/`SIDT`/segment reads, RIP after the launch site, RSP, RFLAGS. Invalid guest state (`0x80000021`) is almost always activity-state, unrestricted-guest CR0, or a segment AR-byte fail — not “segmentation is hard.”
   - Done when first VMEXIT reason is a handled exit (CPUID/VMCALL/EPT), not `0x80000021`.

4. **Install host identity.** Per-CPU host GDT is a copy of the guest GDT with a new TR base pointing at a host TSS. IST[4..6] get dedicated NonPaged stacks for `#PF` / `#GP` / `#DE`. Host IDT is a copy of the guest IDT except those three vectors route to VMX-root SEH (RIP → catch block). Forward `#DB` and `INT3` to the guest so WinDbg still works.
   - Done when a deliberate `#PF` inside a `__try` in VMX-root lands in the except block, and a guest `int 3` still breaks WinDbg.

5. **Build the self-ref map.** Clone system PML4 into a host CR3. Reserve PML4[0..254] as *PTE slots*, not directory pointers. Install a self-ref at PML4[255]. Each logical processor owns two slots (`apic_id * 2 + map_type`) for src/dst physical pages. `map_page(phys, src|dest)` writes the PTE PFN, `__invlpg`, returns a VA in the self-ref window. Never share a slot across src and dest in one copy.
   - Done when `read_phys` / `write_phys` copy a known 4K pattern without an intermediate buffer.

6. **Expose VMCALL commands.** Guest fills `vmcall_command_t` (`command.hpp`): `translate`, `copy_virt`, `write_phys`, `read_phys`, `dirbase`. Host reads the command through the self-ref map using the caller dirbase, executes, writes `result` back. Wire the demo through VDM lambdas so usermode can R/W physical and foreign CR3 virtual memory.
   - Done when the demo reads a known usermode buffer via `copy_virt` and a known physical page via `read_phys`.

7. **Only then add EPT.** Bluepill’s original README lists EPT as a to-do. Identity-map + hooks live in `hypervisor-dev`. Do not start EPT until steps 2–6 are green.
   - Done when this skill’s bring-up checklist is complete *or* the task has handed off to `hypervisor-dev` / `stealth-hypervisor`.

## VMCALL contract

```
enum vmcall_option { translate, copy_virt, write_phys, read_phys, dirbase }

translate:   dirbase, virt_addr  -> phys_addr
copy_virt:   virt_src, dirbase_src, virt_dest, dirbase_dest, size
write_phys:  virt_src, dirbase_src, phys_dest, size
read_phys:   phys_src, dirbase_dest, virt_dest, size
dirbase:     -> current guest CR3
```

`present`/`result` are host-written. Treat a missing `result=true` as a failed map, not a successful zero-fill.

## Pair with

- `hypervisor-dev` — EPT split, VMEXIT table, teardown
- `stealth-hypervisor` — CPUID/TSC/MSR hide
- `hypervisor-memory-introspection` — GVA→HPA analysis, MTF, VMFUNC
- `windbg-ttd` — KDNET on the guest; `#DB`/`INT3` must stay guest-forwarded
- `kernel-dev` — WDK IRQL, NonPaged, manual-map hygiene

## Verification

- [ ] `IA32_VMX_BASIC` revision matches VMCS/VMXON header
- [ ] Control fields survive high/low mask; no error 7
- [ ] First exit is not `0x80000021`
- [ ] Host `#PF/#GP/#DE` SEH works in VMX-root
- [ ] Guest `int 3` still hits WinDbg
- [ ] Two-PTE map copies a known pattern both directions
- [ ] Demo VMCALL `read_phys` / `copy_virt` match a usermode ground truth
