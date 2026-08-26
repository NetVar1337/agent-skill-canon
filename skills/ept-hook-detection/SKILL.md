---
name: ept-hook-detection
description: "Use when detecting or scoring Intel EPT (or AMD NPT) split-view hooks from the guest: write-reflect fail, RDTSC exit-cost, two-thread race clock. Complements hypervisor-detection (HV present?) and stealth-hypervisor (hide the hook)."
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: stealth
  author: Admin
  source: https://github.com/momo5502/ept-hook-detection
  upstream: https://momo5502.com/blog/?p=255
  triggers:
    - ept hook detection
    - detect ept hook
    - slat hook detect
    - hypervisor-assisted hooking
    - momo5502 ept
---

# EPT / SLAT hook detection

momo5502 `ept-hook-detection` + the 2022 blog *Detecting Hypervisor-assisted Hooking*. These probes answer “is this *page* split-view hooked?”, not “is a hypervisor present?” (`hypervisor-detection`). AMD NPT cannot do execute-only, so the classic swap is weaker there.

Repo: `https://github.com/momo5502/ept-hook-detection`  
Article: `https://momo5502.com/blog/?p=255`  
Companion HV: `https://github.com/momo5502/hypervisor`

## Hook model under test

1. Clone the 4K page (clean vs hooked).
2. EPT: execute → hooked PFN, read/write → clean PFN (or the reverse).
3. Each permission miss VMEXITs and swaps the PFN.

Integrity hashes that only *read* the page always see clean bytes. Detect the swap, not the bytes.

Hyper-V/VBS is a legitimate SLAT user — do not treat “a hypervisor exists” as a hook.

## Workflow

1. **Pick pages.** `.text` of ntdll, the game, the AC usermode module, and any page you suspect is hooked. Scan per 4K. Whole-AS scans are expensive; start with exports the AC itself hashes.
   - Done when a page list (module+RVA) is written.

2. **Write-reflect (weak, kills sloppy HVs).** Find 2+ consecutive `CC` alignment bytes (MSVC) or `90` nops — disassemble so you are not in an immediate. `__try` execute the `CC` (expect `#BP`). Overwrite with `C3`. Execute again.
   - Clean page: returns.
   - Hook whose execute-view is a stale clone: still `#BP`.
   - Repeat the `C3` on another core (hooks are per-vCPU).
   - Done when each page is `reflects | stale-clone | no-safe-pad`.

   Mitigation (HV side): on write, copy into *every* core’s hooked view (`stealth-hypervisor`).

3. **RDTSC timing.** Find a `C3` on the page. Warm up. Measure:
   - A: N reads of that byte, no execute
   - B: N alternations execute ↔ read
   - Clean: B ≈ 2× A
   - Hooked: B ≫ A (often 10–100×) because every flip VMEXITs and swaps
   - Done when A/B ratio is recorded. RDTSC offsetting can hide this — fail-open to step 4.

4. **Thread-race clock (best of the three).** Two threads, affinity on different physical cores, high priority. Thread 0 increments a counter. Thread 1 runs A then B. Time = counter delta. Average many trials.
   - False positives *and* false negatives from context switch; majority vote.
   - Hard for the HV to subtract a cost it cannot see (no RDTSC).
   - Done when majority vote is logged with trial count.

5. **Score, then hide or hunt.** A hit on step 3/4 is an EPT hook candidate even if CPUID is clean. Implement the hide in `stealth-hypervisor` (TSC offset by measured exit cost, reflect writes, coalesce hooks). Confirm with `hypervisor-detection` so you did not just leak the HV some other way.
   - Done when before/after tables exist for the same page list.

## Pair with

- `hypervisor-detection` — vendor leaf / FYL2XP1 / WMI
- `stealth-hypervisor` / `hypervisor-dev` / `bluepill-type2-hv`
- `hypervisor-memory-introspection` — you are the one installing the hook
- `ring-1-bootkit` — ring-1 hides injected pages with the same EPT swap

## Verification

- [ ] Page list is 4K-aligned module+RVA
- [ ] Write check used a disassembly-validated pad
- [ ] Timing A vs B ratios recorded
- [ ] Thread check used two cores + N-trial majority
- [ ] VBS/Hyper-V-root not scored as “malicious hook” from CPUID alone
- [ ] Hide work re-ran this suite
