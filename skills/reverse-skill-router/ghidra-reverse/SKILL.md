---
name: router-reverse-skill-router-ghidra-reverse
description: Use for free/open reverse engineering with Ghidra (headless or GUI), including decompile, cross-refs, and optional Ghidra MCP workflows when IDA is unavailable.
---

# Ghidra Reverse Engineering

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read `../field-journal/precedent-reverse.md`
2. `NOW`: confirm **Ghidra** is needed (no IDA / open-source preference / batch headless)
3. `NEXT`: read `../tool-index.md` for ghidra / ghidra-mcp paths
4. `NEXT`: missing tools → bootstrap `ghidra-mcp` (if the manifest supports it) or install Ghidra per manual steps
5. `ACT`: import sample → auto-analyze → export decompilation of key functions

## Applicable Scenarios

- Primary reverse engineering entry when there is no IDA license
- Batch headless analysis / decompilation in CI
- Ghidra script automation (Java/Python Jython/PyGhidra)
- ghidriff integration with `binary-diff` / `patch-diff-exploit`

## Division of Labor with IDA

| Need | Preferred |
|------|------|
| Deep digging with IDA MCP already available | `ida-reverse/` |
| Open source / batch / teaching | **this skill** |
| Quick CLI reconnaissance only | `radare2/` |

## Workflow

### 1. Project and Auto-Analysis

```text
□ New Project → Import file → Analyze (default analyzers)
□ Record language/compiler identification and base address
□ Mark entry point, export table, string xrefs
```

### 2. Key Functions

```text
□ Trace back from strings / imported APIs
□ Restore algorithms in the Decompile window
□ Rename functions/variables; write Plate comments
□ Hand off to Frida/GDB when dynamics are needed (reverse-engineering dynamic chapter)
```

### 3. Headless (batch)

```bash
# Example: analyzeHeadless path varies by install, MUST be taken from tool-index
analyzeHeadless /path/to/project Proj -import sample.bin -postScript ExportDecomp.py
```

### 4. MCP (if configured)

```text
□ Confirm the ghidra MCP port (commonly 8765; tool-index is authoritative)
□ Pull decompilation / xrefs via MCP tools; do not guess ports
```

## Toolchain

| Tool | Purpose | Bootstrap |
|------|------|------|
| Ghidra | Primary decompiler | Manual release / package manager |
| ghidra-mcp | AI bridge | bootstrap capability name `ghidra-mcp` |
| ghidriff | Patch diffing | See `patch-diff-exploit` |

## References

- `references/ghidra-cheatsheet.md`
- `../ida-reverse/` `../radare2/` `../binary-diff/`

## Routing Context

**Upstream**: MASTER R22  
**Downstream**: dynamic validation → Frida/GDB; exploitation → `pwn-chain`  
**Peers**: `ida-reverse` (commercial deep digging)

## Task Completion Self-Check

- [ ] Based on real Ghidra/tool-index paths?
- [ ] Function addresses and renames annotated?
- [ ] Reproducible steps present?
- [ ] Checklist / journal?
