---
name: llvm-lift-deobfuscation
description: "Use when deobfuscating or devirtualizing x86/x64 by lifting assembly to LLVM IR and optimizing: Mergen lifter, Dna/Remill CFG recovery, GAMBA/Simplifier MBA, Polaris as the inverse obfuscator. Distinct from VM-bytecode recovery (virtualization-deobfuscation) and CFF/opaque-predicate rewriting (binary-obfuscation-deconstruction)."
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: re
  author: Admin
  source: https://github.com/CR3Swapper
  upstream:
    - https://github.com/NaC-L/Mergen
    - https://github.com/Colton1skees/Dna
    - https://github.com/DenuvoSoftwareSolutions/GAMBA
    - https://github.com/mazeworks-security/Simplifier
    - https://github.com/za233/Polaris-Obfuscator
  triggers:
    - mergen
    - lift to llvm
    - llvm deobfuscation
    - GAMBA
    - MBA simplifier
    - dna remill
    - polaris obfuscator
---

# LLVM-IR lift deobfuscation

CR3Swapper’s forks cluster on one pipeline: decode x86 → LLVM IR → let the optimizer eat VMProtect/Themida/OLLVM mush → inspect or recompile the result. That is a different job from recovering VM bytecode (`virtualization-deobfuscation`) or rewriting CFF/opaque predicates in place (`binary-obfuscation-deconstruction`).

| Tool | Role | Upstream |
|---|---|---|
| Mergen | x86 → LLVM IR + custom fixpoint opts; VMP 3.4–3.8 | NaC-L/Mergen |
| Dna | SATURN-style iterative CFG + Remill + Souper jump tables; C# | Colton1skees/Dna |
| GAMBA + SiMBA | general / linear MBA simplify, optional Z3 | DenuvoSoftwareSolutions/GAMBA |
| Simplifier | faster MBA, hash-cons DAG, ISLE rewrite; Windows | mazeworks-security/Simplifier |
| Polaris | LLVM 16 obfuscator (flatten, MBA, indirect br/call, MIR junk) — the inverse | za233/Polaris-Obfuscator |

## Choose the product first

1. **MBA formula** — expression in, shorter equivalent out (GAMBA/Simplifier).
2. **Lifted function IR** — Mergen `main` with GPRs + `memory` ptr.
3. **Recovered CFG + recompile** — Dna safe translator.
4. **Readable Hex-Rays of the optimized object** — compile Mergen IR, reopen.

Write the product and the I/O boundary (regs, memory, flags) before running a lifter.

## Workflow

1. **Pin the sample.** Hash, arch (x64 only for Dna; Mergen is x86/x64), protector/version guess, function VA, ABI (RCX/RDX/R8/R9 vs Mergen’s raw GPR list).
   - Done when VA + hash + chosen product are written.

2. **MBA-only? Stay out of LLVM.**

```bash
python src/simplify_general.py "expr" -b 64 -z
# or
Simplifier.exe "expr" -b 64 -z
```

`-z` is Z3 equivalence. `-v N` brute-forces all N-bit inputs. `-m` modulo-reduces constants.
   - Done when simplified expr ≠ input and Z3 (or `-v`) agrees, or the solver result is recorded as unknown.

3. **Virtualized / flattened native? Lift with Mergen.** Build (Windows + Ninja + clang-cl + LLVM 18 + Rust iced backend): see [references/mergen-pipeline.md](references/mergen-pipeline.md).

```bat
lifter.exe target.exe 0x140001000
opt -O2 mergen.ll -o opt.ll
clang -c opt.ll -o opt.obj
```

Mergen does **not** apply the Microsoft x64 ABI. `maths(test, b, c)` shows up as `main(rax, rcx, rdx, ...)`. Adjust the signature before judging Hex-Rays.
   - Done when `opt.ll` is smaller than the raw lift and a decompiler shows the expected arithmetic, or a named unresolved diagnostic is on disk.

4. **Unresolved indirects / jump tables.** Mergen will pause on a symbolic key and ask for a concrete value — supply one, emit both sides, merge. Nested MSVC tables or SEH: switch to Dna (Remill + Souper + z3). Dna does not build out of the box on Windows without the author’s remill/souper patches; treat it as a specialist lane, not the default.
   - Done when each taken virtual branch has a contrasting input, or the missing edge is listed.

5. **Equivalence.** Differential-test the original bytes vs lifted IR on the stated I/O. GAMBA `-z` for MBA. For VMP, Mergen `python test.py vmp` requires `blocks_completed > 0` on pinned 3.8.x targets. “Looks like the source” is not a pass.
   - Done when fixtures cover both sides of every recovered branch plus a negative case.

6. **Polaris is the generator, not the solver.** If the job is to *emit* OLLVM-style IR/MIR junk, use Polaris passes (Alias Access, Flattening, Indirect Br/Call, String Encrypt, BCF, ISel sub, Merge Function, Linear MBA, Dirty Bytes, Function Split, Junk ISel). Do not feed Polaris output into Mergen and call it a new protector.
   - Done when the requested Polarise pass list is what actually ran.

## When not to use this skill

- Handler-table / VIP/VSP recovery of a custom VM → `virtualization-deobfuscation`
- Opaque predicate / CFF rewrite without LLVM → `binary-obfuscation-deconstruction`
- Trace-first self-modifying code → `trace-guided-deobfuscation` first, then lift the slice

## Pair with

- `virtualization-deobfuscation`, `binary-obfuscation-deconstruction`
- `zydis-disassembly-engineering` — Mergen iced/Zydis backends
- `ida-reverse` — inspect `opt.obj`
- `symbolic-execution-tools` — Souper/z3 lane inside Dna

## Verification

- [ ] Product and I/O boundary written before the first lift
- [ ] MBA path used GAMBA/Simplifier + Z3 or numeric `-v`
- [ ] Mergen IR compiled; ABI mismatch not mistaken for a wrong lift
- [ ] Unresolved ret/jmp diagnostics named, not ignored
- [ ] Differential or solver check on the stated boundary
- [ ] Protector/version + sample hash recorded
