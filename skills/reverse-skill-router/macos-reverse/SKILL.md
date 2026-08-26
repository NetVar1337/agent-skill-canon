---
name: router-reverse-skill-router-macos-reverse
description: Use for authorized macOS and Mach-O reverse engineering including codesign, Objective-C/Swift recovery, endpoint security surfaces, and Apple platform malware analysis.
---

# macOS / Mach-O Reverse Engineering

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read `../field-journal/precedent-reverse.md`
2. `NOW`: confirm the target is macOS/Mach-O/App bundle (iOS IPA → `mobile-reverse/`)
3. `NEXT`: tool-index; jtool2/lldb etc.
4. `ACT`: signature and load info → static → dynamic (lldb/Frida)

## Applicable Scenarios

- Mach-O executables / dylib / framework
- .app bundles, LaunchAgent/Daemon
- Objective-C / Swift symbols and runtime
- Notarization/signature, Hardened Runtime, TCC-related behavior analysis
- macOS malware static/dynamic analysis (combined with malware-analysis)

## Workflow

### 1. Bundle and Signature

```bash
file target
codesign -dv --verbose=4 target
spctl -a -vv target 2>&1
otool -L target
```

### 2. Static

```text
□ class-dump / swift-demangle / Hopper / Ghidra / IDA
□ Strings and XPC service names, TCC-sensitive APIs
□ LC_LOAD_dylib dependencies and rpath
```

### 3. Dynamic

```text
□ lldb / Frida
□ Observe with fs_usage / log stream
□ Network: combine with protocol-reverse or a proxy
```

## Toolchain

| Tool | Purpose |
|------|------|
| otool / nm / codesign | System built-ins |
| Hopper / Ghidra / IDA | Decompilation |
| class-dump / dsdump | ObjC |
| Frida / lldb | Dynamic |
| jtool2 | Mach-O |

## References

- `references/macho-triage.md`
- `../mobile-reverse/` (iOS) `../ghidra-reverse/` `../malware-analysis/`

## Routing Context

**Upstream**: MASTER R31  
**Downstream**: iOS → mobile-reverse; general samples → malware-analysis

## Task Completion Self-Check

- [ ] Signature/Hardened Runtime status recorded?
- [ ] Address-level/symbol-level conclusions present?
- [ ] Checklist?
