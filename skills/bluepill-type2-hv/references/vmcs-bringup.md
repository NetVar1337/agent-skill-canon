# Bluepill VMCS bring-up notes

Source: xeroxz Bluepill README (`VMCS-CONTROLS.md`, `VMCS-GUEST.md`) plus Intel SDM Vol 3C.

## Control-field order

Wrong:

```
apply IA32_VMX_* high/low
set conceal_vmx_from_pt = 1
set enable_xsaves = 1
```

Right:

```
set desired bits (including PT conceal / XSAVES if you want them on capable CPUs)
controls = (desired & allowed1) | allowed0
VMWRITE
```

Xeon / older client parts without Intel PT reject `entry_ctls.conceal_vmx_from_pt` after the mask is applied → VM-instruction error 7. Same for `enable_xsaves` vs `IA32_VMX_PROCBASED_CTLS2`.

Dump every control field after mask. Compare against the CPU’s true allowed-1 bits, not a laptop you brought up last week.

## Guest-state landmines

First successful `VMLAUNCH` that immediately exits with `0x80000021` is **invalid guest state**, not a missing EPT.

Checklist from SDM 26.3 + 24.4:

| Field | Required |
|---|---|
| `GUEST_ACTIVITY_STATE` | `0` (active). Bluepill’s first-exit bug was leaving this unset. |
| `GUEST_CR0` | Must satisfy fixed bits; PE/PG consistent with unrestricted-guest setting |
| `GUEST_CR4` | VMXE may be host-only; guest-visible CR4 is a stealth concern (`stealth-hypervisor`) |
| `GUEST_RFLAGS` | Bit 1 set |
| `GUEST_RIP` | Canonical, after the launch trampoline |
| `GUEST_RSP` | Canonical |
| CS/SS AR bytes | Type, S, P, DPL vs SS, L/DB mutual exclusion in 64-bit |
| TR | Busy 64-bit TSS, present |
| GDTR/IDTR limits | From `SGDT`/`SIDT` |
| `VMCS_LINK_POINTER` | `~0ull` unless shadowing |

Walk SDM 26.3 *in order* before inventing a segmentation theory.

## Host state

- `HOST_RIP` = VMEXIT stub that saves GPRs then calls C
- `HOST_RSP` = per-CPU stack, 16-byte aligned
- `HOST_CR3` = Bluepill self-ref PML4, not `System` CR3
- `HOST_TR` = selector whose descriptor base is the *host* TSS page
- `HOST_GDTR` / `HOST_IDTR` = host-owned pages

## Host GDT / TSS / IDT

Windows GDT is < 4K. Copy one page per CPU. Patch only TR.base.

TSS is 1:1 with the guest except IST entries 4–6:

```
ist[pf] = NonPaged stack top
ist[gp] = NonPaged stack top
ist[de] = NonPaged stack top
```

IDT copy, then:

```
table[#GP] = host handler, IST = gp
table[#PF] = host handler, IST = pf
table[#DE] = host handler, IST = de
```

Leave `#DB` and vector 3 as guest handlers so WinDbg keeps working.

Host handlers implement SEH by rewriting RIP to the except cookie. This is why Bluepill SEH works when the image is manually mapped (no normal runtime table walk in VMX-root).

## Self-ref PML4 map

```
result.va = vmxroot_pml4_va
result.pt_index = apic_id * 2 + (src ? 0 : 1)
PML4[result.pt_index].pfn = phys >> 12   // treated as a PTE, not a PDPTE
invlpg(result.va)
result.offset = phys & 0xFFF
return result.va
```

PML4E / PDPTE / PDE are the same 64-bit shape. Combined with a self-ref entry, one table can encode every walk level. Cost: only 255 usable PTE slots (0..254), two per CPU.

Copy rule: every translate+copy of a foreign page must stay inside one `map_type`. If dest is already mapped, src must use the other slot.

## First-exit debug

1. Serial or `DbgPrint` the raw `VM_EXIT_REASON` and `VM_INSTRUCTION_ERROR`
2. Error 7 → dump controls vs MSRs
3. `0x80000021` → dump activity-state, CRs, CS/SS/TR AR bytes
4. Exit 10 (CPUID) or 18 (VMCALL) → bring-up is alive; implement the handler
