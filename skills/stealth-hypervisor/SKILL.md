---
name: stealth-hypervisor
description: "Stealth hypervisor development beyond the basics: full detection-surface model (CPUID consistency, RDTSC deltas, MSR/CR coherence, EPT violation costs, #VE), split-view EPT via VMFUNC EPTP switching, TSC offsetting and exit-cost amortization, nested-virt under Hyper-V, anti-AC artifact hygiene, bring-up and self-test loop. Extends hypervisor-dev when detectability is a first-class requirement."
version: 2.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: stealth
  triggers:
    - "EPT stealth"
    - "hypervisor stealth"
    - "CPUID hide"
    - "SLAT hook"
    - "anti-detect HV"
    - "VMFUNC EPTP"
    - "TSC offsetting"
---

# Stealth hypervisor development

Extends `hypervisor-dev` (bring-up, VMCS plumbing) — that skill first if the
baseline VMX/SVM loop isn't standing. Here the product is *undetectability
budget management*: every feature you add spends budget; every artifact you
leave is a signature.

## Detection surface (attacker's model of the detector)

| Signal | How detectors probe | Countermeasure themes |
|---|---|---|
| CPUID hypervisor bit (leaf 1 ECX[31]) + vendor leaf 0x40000000 | trivial | clear bit, fake vendor string = genuine Intel/Microsoft HV strings *exactly*; check what the guest OS itself reports under your HV |
| Timing: RDTSC/RDTSCP around VMEXIT-heavy ops | `cpuid`-gated RDTSC deltas, `GetTickCount`/QPC cross-checks | TSC offsetting per-guest; minimize handled exits (pass-through aggressively); amortize: answer rare leaves in the guest via #VE handler instead of exits |
| MSR reads | `IA32_FEATURE_CONTROL`, `IA32_VMX_BASIC` readable, DEBUGCTL/LBR state, `IA32_FS/GS_BASE` anomalies | consistent MSR bitmap handling; return bare-metal values for probe MSRs unless the probe is itself a nested check |
| CR0/CR4/EFER | guest reads see HV-required bits (e.g. VMXE) | maintain *shadow* CR views: guest-visible values must match pre-HV bare metal |
| EPT artifacts | execute-only TLB behavior, `INVEPT` storms, split-view coherency bugs | coalesce hooks; validate INVEPT/INVVPID scope per change; self-audit TLB views |
| #VE (EPT violation → suppress) | handlers are observable via timing/exceptions | use info-page correctly; don't turn every fetch into #VE |
| PMU / perf counters | exit counts visible via `IA32_PMC`, APERF/MPERF ratios drift | pin interrupts; keep exit rate near baseline; measure with your own PMU before shipping |
| Nested virt | guest enables Hyper-V/VBS → your HV must nested-trap or bail | detect `CPUID.8000000Ah`/enlightenments early; plan graceful unwind |
| System artifacts | driver objects, EPROCESS links, loaded-module lists | load via manual map, no device object, no registry traces (`kernel-dev` hygiene) |

The meta-rule: pick a *detection suite stack* (public anti-VM tools + custom
probes replicating known AC checks) and gate every change on it staying clean.

## Split-view EPT (the core stealth primitive)

Two EPT pointer targets: "real" view (guest executes normally) and "hooked"
view (pages you redirect for a specific vCPU context). Switching:

- **VMFUNC EPTP switching** (leaf `CPUID.07H:EBX[16]=VMFUNC`,
  `EPTP_LIST` in VMCS): guest-side/ring-0-triggered switch without exit —
  the fast path. All 4 EPTPs in the list must have identical memory types and
  coherency or you eat a shutdown (#SS-like triples).
- Per-core targeting: the *reading* core must be in the hooked view at read
  time and the executing victim never — drive switching from the reader's
  context (target-specific, e.g. anti-cheat's verification thread), not
  globally.
- Data-hide (callback arrays, module pages) vs code-hook (execute traps on
  RX pages): code hooks with execute-only EPT entries deny read access by
  design — but `RtlPcToFileHeader`-style self-introspection then faults;
  know which consumer reads the page before choosing execute-only.
- MTRR coherency: EPT memory types must match MTRR-effective types or you get
  subtle corruption on WC/UC regions (framebuffer, MMIO).

## TSC discipline

- Enable TSC offsetting + TSC scaling (IA32_TSC_MULTIPLIER) so guest-visible
  TSC = physical − offset; keep offsets constant except during exit handling
  windows you want to hide.
- Exit-cost accounting: measure your handler durations (PTMON/your own
  instrumentation), then hide the common ones: for leaves you must lie about,
  add the *median bare-metal cost* of that leaf to the offset instead of
  leaving a negative delta. Drift correction on idle.
- Never let `rdtscp`/`rdtsc` ordering observability change (keep RDTSCP
  intercept decisions static per vCPU).

## Nested-virtualization reality (Hyper-V hosts)

- If the host runs Hyper-V (VBS on), your driver-based HV runs **nested**:
  VMX-on under Hyper-V requires the enlightened path (VP assist, synthetic
  controls) or fails. Two options: (a) support nested operation via
  Hyper-V enlightenments ( HyperPlatform-lineage codebases show the shape),
  (b) require HVCI off in your lab and treat that as a documented constraint.
- Detecting "am I under Hyper-V" at driver init: `CPUID.40000003H:EBX`
  enlightenments + `IVMR` presence; bail cleanly (unload, no artifacts)
  instead of triple-faulting the box.

## Bring-up & self-test loop (non-negotiable order)

1. Bench machine with HVCI off, KDNET attached (`windbg-ttd`), serial/log VMEXIT channel.
2. VMX-on per-core, minimal VMCS, launch loop, exit-to-hypervisor logging.
3. Self-tests: CPUID intercept, EPT RW→RX flip on a test page, VMFUNC switch
   round-trip, TSC offset linearity over 10⁹ cycles.
4. Detection suite run (clean baseline first — record the machine's
   pre-HV fingerprint, then diff).
5. Soak: 24 h under target workload (game + browser), zero exits-unhandled,
   zero MTRR warnings, stable timing deltas.
6. Only then: AC-specific experiments, one artifact at a time, with the suite
   re-run after each.

## Known self-inflicted wounds (checklist)

- [ ] EPT entries: coherency between views on every page you touch; INVEPT
      (single/multi-context) after *every* EPT mutation.
- [ ] VMCS fields on any reload (host state, MSR bitmaps) — stale bitmaps
      after S3/S4 resume.
- [ ] APIC virtualization (TPR shadow) correct or off — wrong virt-APIC =
      intermittent hangs under load, the worst bug class you can have.
- [ ] Interrupt-window exiting storms: measure exit rate; interrupt handling
      that exits per-IRQ will light up timing detectors instantly.
- [ ] Unwind path: VMX-off restores bare-metal CPUID/MSR/CR state *exactly*
      (diff before/after) — a dirty unwind is itself a signature.

## Pair with

`hypervisor-dev` (baseline), `hypervisor-memory-introspection` (read-only
guest analysis from the HV), `kernel-callbacks` (what you hide from whom),
`kevlar-driver-emulation` (emulating a known-good driver surface),
`anti-cheat-stack-walk-stealth` (stack hygiene seen from the guest).
