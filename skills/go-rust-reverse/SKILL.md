---
name: go-rust-reverse
description: "Use for reverse engineering stripped Go and Rust binaries including runtime recognition, pclntab/moduel data recovery, panic strings, and idiomatic decompilation recovery."
version: 1.0.0
license: MIT
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: reverse-skill
  upstream: https://github.com/zhaoxuya520/reverse-skill
---

> Bundled with Unleash skills pack. Upstream: https://github.com/zhaoxuya520/reverse-skill

# Go / Rust Binary Reverse Engineering

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read `../field-journal/precedent-reverse.md`
2. `NOW`: confirm the sample is a Go/Rust build (`file`/strings/runtime signatures)
3. `NEXT`: check whether GoReSym / related plugins are available
4. `ACT`: runtime identification → symbol/metadata recovery → business logic

## Applicable Scenarios

- Symbol-stripped Go malware/tools
- Rust release binaries, panic-string-driven analysis
- Language-specific methods complementing generic ida/ghidra

## Workflow

### Go

```text
□ Identify go.buildid, runtime symbol remnants, pclntab
□ Recover function names with GoReSym / redress / IDA Go plugins
□ Watch how interface, slice, string structures appear in decompilation
□ Network/crypto library paths: crypto/* net/http
```

### Rust

```text
□ Panic strings, rust_begin_unwind, crate path hints
□ Code bloat from generic instantiation; locate string xrefs first
□ Async/tokio state machines need cross-references
```

### Dynamic

```text
□ Frida still works; mind the Go stack and scheduler
□ Prefer breakpoints driven by logs and config strings
```

## Toolchain

| Tool | Purpose |
|------|------|
| GoReSym | Go metadata |
| IDA/Ghidra + Go/Rust plugins | Decompilation |
| radare2 | Quick strings |
| strings / rabin2 | Triage |

## References

- `references/go-rust-notes.md`
- `../reverse-engineering/go-reverse.md` `../ida-reverse/` `../ghidra-reverse/`
- seed: `field-journal/seed-002_go-malware-stripped.md`

## Routing Context

**Upstream**: MASTER R33  
**Downstream**: malicious sample workflow `malware-analysis`; generic RE `reverse-engineering`

## Task Completion Self-Check

- [ ] Recovered key function names or an equivalent mapping?
- [ ] Language-runtime evidence annotated?
- [ ] Checklist?
