---
name: r2mcp-basic
description: "Use the local r2mcp MCP server (radare2 6.2 backend at ~/Tools/radare2/bin/r2mcp.exe) for scripted binary analysis: open/analyze, disassembly, decompile, search, xrefs, patching. Covers server setup, MCP registration, command economy (aa vs aaa), session hygiene, and when to prefer r2mcp over IDA or raw r2."
version: 2.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: re
---

# r2mcp — radare2 over MCP

Local reality (verified in `tool-index.md`): radare2 6.2.0 at
`C:\Users\Admin\Tools\radare2\bin\` — **`r2.bat`, not `r2.exe`** — plus
`r2mcp.exe` (MCP server) and `r2agent.exe` in the same directory. Python
bindings (r2pipe) available via pip if needed.

## When to use which

| Need | Tool |
|---|---|
| Quick recon, strings, header dump, entropy, patch | raw `r2 -qc '...' file` one-liners / `rabin2` |
| Iterative interactive RE, decompile-heavy, type work | IDA 9.4 / `ida-reverse` |
| Agent/scriptable analysis without leaving the harness | **r2mcp** |
| Full r2 command coverage in scripts | r2pipe over r2agent |

## Server setup

`r2mcp` speaks MCP (stdio). It is *not* pre-registered in every harness —
check `~/.omo/agent/mcp.json` (OMO) / `.zcode` MCP config for an `r2mcp`
entry; if missing, add:

```json
{
  "mcpServers": {
    "r2mcp": {
      "command": "C:\\Users\\Admin\\Tools\\radare2\\bin\\r2mcp.exe",
      "args": ["-m"]
    }
  }
}
```

(Exact flag set: run `r2mcp.exe --help` once and mirror it — do not guess
flags from memory; tools list below assumes the standard build.)

## Workflow (the discipline that keeps it fast)

1. **Open first**: one session per binary; note the session/file handle the
   server returns; every later call targets it.
2. **Minimum analysis**: `aa` for functions-by-prologue; `aaa` only under ~50 MB
   binaries or when xrefs matter; `aaaa` is almost always a mistake.
   For one-function questions, skip global analysis: `s <addr>; pdf`.
3. **Structured tools over raw exec** when both exist (open/analyze/disasm/
   decompile/xrefs/search/data commands) — raw `r2 -c` strings escape quoting
   bugs and lose structure.
4. **Summarize in RE terms**: function purpose, calling convention, arguments
   recovered, flags/structures touched, xref graph neighborhood — not raw
   disassembly dumps.
5. **Close the session** when done — r2 holds the whole file mapped;
   abandoned sessions leak across a long agent session.

## Command economy (cheat sheet)

| Question | r2 |
|---|---|
| What is this file | `rabin2 -I` (arch, bits, endian, hash, compiler) |
| Imports/exports | `rabin2 -i` / `-E`; in-session `ii` / `iE` |
| Strings of interest | `iz~keyword` (data), `/ keyword` (raw) |
| Entry/disasm a function | `s sym.xxx; pdf` or `pdf @ 0x401000` |
| Cross-refs | `axt @ addr` (to), `axf` (from) |
| Local structs/fields | `pf.` formats; `afl` to list functions |
| Patch bytes | `wa nop` / `wx 90` + `wc` (write cache) — commit to a **copy** |
| Diff two builds | `radiff2 -A old new` (scripted; pairs with `binary-diff`) |
| Decompile | r2ghidra plugin if installed (`pdg`); otherwise `pdc` pseudo-C — check `r2pm -l` and install via `r2pm -ci r2ghidra` when needed |

## Pitfalls

- Windows paths in commands: use forward slashes or escaped backslashes;
  spaces in paths need quoting at *both* layers (MCP arg + r2 string).
- PIE binaries: RVAs ≠ VAs; confirm with `i` base before reporting addresses.
- Packed samples: entropy gate first (`rabin2 -l`, section entropy) — analysis
  of a packed image wastes the session; unpack (`advanced-packer-unpacking`)
  then re-open.
- `pdg` quality ≠ IDA Hex-Rays; for court-grade decompilation route to
  `ida-reverse`/`ida-pro-mcp`.

## Pair with

`radare2-terminal-re` (raw CLI flows), `ghidra-reverse` (alternate open-source
decompiler), `pattern-scanner` (sig creation from r2 findings), `pe-tools`
(structural work), `binary-diff` (radiff2-driven version comparison).
