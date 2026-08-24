---
name: pattern-scanner
description: "Byte-pattern scanning for static and runtime RE: signature formats (IDA/x64dbg), mask design for cross-build stability, section constraints, Horspool and AVX2 scanner implementations, relative-reference resolution (E8/E9/RIP-LEA), uniqueness validation and signature databases. Use when locating functions/data in versioned or stripped binaries."
version: 2.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: re
---

# Pattern scanner

A signature is a contract: it must hit exactly once in the target module, on
every build you claim to support, and resolve to the same logical entity.
Everything below serves that contract.

## Formats

- IDA style: `48 8B 05 ? ? ? ? 48 85 C0 74 05` (`?` = wildcard byte).
- Code + mask: `{ bytes[], mask[] }` with mask chars `x` (fixed) / `?` (skip).
  Canonical on-disk form for a sig DB: JSON
  `{ name, module, pattern, mask, offset, resolve: "none|lea|e8e9|ptr", builds: { "<buildid>": "<rva>" } }`.
- When sharing with tooling: record which tool's dialect you used. Converters
  are two lines of Python; ambiguity is the enemy, not syntax.

## Mask design (what to fix vs wildcard)

Stable across builds: opcode bytes, ModRM, register choices, short displacements
that reference nearby code. Volatile: immediates tied to struct sizes, absolute
addresses (ASLR + version churn), string addresses, anything the compiler
renumbers per build.

- Prefer 12–24 fixed bytes over long signatures; more bytes ≠ more stable.
- Never sign against padding/int3 runs — they move.
- Sign the *body* after the prologue: prologues are the most reused byte
  sequences in the binary (comdat/COLD clones).
- Anchor on a call/jmp target or a distinctive constant table reference, then
  use `resolve` to compute the real address (below).

## Section constraints

- Scan `IMAGE_SCN_MEM_EXECUTE` sections for code; `.rdata`/`.data` for vtables
  and tables. Skip `IMAGE_SCN_MEM_DISCARDABLE`.
- Honor `VirtualSize` not `SizeOfRawData` (tail padding); when scanning on disk,
  map raw→RVA via section headers; when scanning memory, walk via PEB
  `InMemoryOrderModuleList` (internal) or `EnumProcessModules` (external).
- Page-aligned start; never cross section boundaries mid-pattern.

## Resolution: from hit to address

| Resolve | Encoding | Formula |
|---|---|---|
| none | — | `addr = hit + offset` |
| e8e9 | `E8/E9 disp32` at `hit+offset` | `target = hit + offset + 5 + *(int32*)(hit+offset+1)` |
| lea | `48 8D 0D disp32` (RIP-relative) | `target = hit + offset + 7 + *(int32*)(hit+offset+3)` |
| ptr | absolute pointer at `hit+offset` | `target = *(uintptr*)(hit+offset)` (rebase on disk scans) |

Always verify the resolved target: disassemble its first bytes (capstone/
zydis) and sanity-check they look like the expected function start (or point
into the expected data section). A sig that resolves into the middle of
another function is a broken sig even if the scan is unique.

## Scanner implementations

Naive `memchr`-seeded scan is fine for offline tools. For runtime hot paths:

```c
// Boyer-Moore-Horspool skip table over the last fixed byte of the pattern
// (mask-aware build): worst case degrades on all-wildcard tails, so pick the
// shift anchor from the LAST 'x' mask index at build time, not runtime.
size_t last_fixed = /* computed from mask */;
for (i = 0; i + plen <= len; ) {
    if (maskcmp(base + i, pat, mask)) return base + i;
    i += skip[base[i + last_fixed]];
}
```

- SIMD: AVX2 compare 32 bytes (`_mm256_cmpeq_epi8` + `movemask`) against the
  first fixed pattern byte, candidate-check full mask on hits. Shave the seed
  to a rare byte (statistically pick the least common fixed byte) to cut
  false candidates.
- Multi-pattern: Aho-Corasick over seed bytes or a sorted seed table; worth it
  past ~100 patterns per module.
- Cache scan results keyed by `(module base, module size, timestamp)` tuple or
  PE hash — re-scan only when the module identity changes.

## Uniqueness and validation pipeline

1. Scan the whole target module — assert exactly one hit. Log near-miss
   distances; two hits 0x10 apart usually means you signed a duplicated
   thunk/stub.
2. Scan *other* loaded modules for cross-module collisions (thunks, shared
   statics) — especially ntdll/kernel32 forwarding stubs.
3. Resolve → disassemble-verify → record `builds[buildid] = rva`.
4. Regression corpus: keep ≥3 builds of the target; every sig must pass all of
   them before it enters the DB. A sig updated for a new build must still pass
   the old ones, or it gets a new version entry, not a silent edit.

## Build identification

`buildid` = one of (module PE timestamp + SizeOfImage), product version
resource, SHA-256 prefix, or the game's own build/manifest number. Pick one,
use it everywhere (`offset-dumper` consumes it). Never key a DB on load base.

## Tooling notes on this workstation

- IDA: create sigs from the disassembly (`Create signature` / SDK `sigmake`
  flow); AiDAPrivate tooling in `C:\Users\Admin\Tools\AiDAPrivate\` may assist.
- radare2 (`~/Tools/radare2/bin/r2.bat`): `/x` with masks (`/x 488b..?` style),
  `/c` for call search; scripted via r2mcp (`r2mcp-basic`).
- Runtime validation hooks: `frida-dbi` `Memory.scan` for quick live checks
  before committing a sig to C++.

## Failure modes

- Build bump → 0 hits: immediates churn. Widen wildcards, re-anchor on opcode
  bytes or a call target.
- Build bump → 2+ hits: compiler duplicated/cloned the function. Add a
  second anchor further into the body, or resolve via a distinctive xref.
- Works on disk, fails in memory: packed/encrypted sections — scan after
  unpacking (see `advanced-packer-unpacking`), or hook the OEP.
- Works in memory, wrong result: rebased module — always compute RVA =
  hit − module base, never absolute.

## Pair with

`offset-dumper` (DB + versioning layer above this), `pe-tools` (section/raw
mapping), `binary-diff` (choosing stable anchors across builds), `frida-dbi`
(live validation).
