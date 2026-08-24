---
name: windows-symbols-debugging
description: Use when resolving Windows PDBs, matching symbols to a loaded module or dump, configuring WinDbg/DbgHelp symbol paths, recovering build-specific types, or validating an address, RVA, and structure layout before native debugging or reverse engineering.
license: MIT
---
# Windows symbols and debugger ground truth

Resolve symbols before treating a function name, source line, type, or field offset as fact. This skill is for symbol/build correctness; load `windows-internals`, `windows-driver-0day`, or a binary-analysis skill after the target is identified.

## 1. Capture module identity

For every executable, DLL, driver, or dump module record:

- full path, SHA-256, file/product version, architecture, and PE timestamp;
- image base when loaded, RVA of the address under analysis, and file offset only when needed;
- PDB GUID plus age from the CodeView debug directory;
- loaded-module timestamp, checksum, and debugger-reported symbol status.

A filename and Windows version are insufficient: servicing can replace individual modules.

Completion: an evidence record can distinguish the exact module from a same-named binary on another build.

## 2. Configure a reproducible symbol path

Use a cache, then the Microsoft public symbol server. In WinDbg:

```text
.symfix C:\Symbols
.sympath+ srv*C:\Symbols*https://msdl.microsoft.com/download/symbols
.reload /f <module-name>
!sym noisy
.reload /f <module-name>
!sym quiet
lmvm <module-name>
```

For offline or restricted analysis, retain the downloaded PDB and record its source. Do not point a debugger at an arbitrary PDB merely because its filename matches.

Completion: `lmvm` shows the expected image and the loaded PDB provenance is captured.

## 3. Validate the match

1. Compare the module's RSDS GUID/age with the candidate PDB.
2. Confirm the debugger's loaded image base and calculate `RVA = VA - image base` explicitly.
3. Resolve at least one known exported, public, or disassembly-verified address from symbol to instruction.
4. For private or reconstructed types, corroborate each field with code accesses, object dumps, or independent build-matched evidence.
5. Mark public symbols, inferred labels, and guessed names differently in notes and tooling.

Completion: each address/type assertion states module hash/build, PDB identity, RVA, and validation method.

## 4. Debugger evidence loop

Use the smallest observation that settles the question:

```text
lm m <module>          ; module base/range
lmvm <module>          ; image and symbol details
x <module>!*pattern*   ; symbol lookup
ln <address>           ; nearest symbol/source line
u <address>            ; instruction validation
!lmi <module>          ; image metadata
.dt <type> <address>   ; typed object only after match validation
```

For a dump, first confirm its OS build, kernel image, processor architecture, and dump completeness. A missing page, optimized code, or incorrect context limits the conclusion; record that limit instead of inventing a field value.

## Handoff record

```markdown
| module | sha256 | image base | RVA | PDB GUID/age | symbol state | validation | confidence |
```

## Final gate

- [ ] Module identity and loaded base recorded.
- [ ] PDB GUID/age match verified or mismatch documented.
- [ ] Addresses reported as module + RVA, not bare VAs.
- [ ] Reconstructed type fields have independent evidence.
- [ ] Debugger output and symbol path are preserved with the case.
