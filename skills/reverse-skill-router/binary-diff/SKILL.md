---
name: router-reverse-skill-router-binary-diff
description: |
  Cross-version symbol migration and binary diffing. Use when you have symbols/reverse engineering results from an old version and need to migrate them quickly to a new version.
  Applicable scenarios: deriving new kernel offsets from old symbols when the PDB is missing, batch-migrating function names after a program update, quickly locating new offsets after an app update.
  Core method: LLM-driven structured diff comparison with programmatic input/output, at extremely low cost (~1 CNY for 200 functions).
  Trigger keywords: symbol migration, bindiff, cross-version, missing PDB, function offset migration, symbol migration, binary diff, version comparison.
---

# Cross-Version Symbol Migration (Binary Diff)

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read `../field-journal/precedent-reverse.md` — confirm this skill's operations are pre-authorized routine operations
2. `NOW`: confirm whether the current task falls within this skill's scope
3. `NEXT`: read `../tool-index.md`, verify tool availability and actual paths
4. `NEXT`: when tools are missing, invoke bootstrap — do not guess paths
5. `ACT`: enter step one of the "Workflow" and execute; do not stop at the confirmation stage

## Scope

Use this skill when the task falls under these scenarios:

1. **Kernel/driver missing a PDB** — you have symbols for an old ntoskrnl.exe, the new PDB was pulled by Microsoft, and you need to derive addresses of non-exported functions in the new version from the old symbols
2. **Symbol migration after a program update** — you previously reverse engineered a program, it updated, and you want to batch-migrate the old results instead of redoing everything
3. **Protection mechanism update** — the old version has complete reversing results, the new version needs the new offset of the same function located quickly
4. **Any binary comparison scenario of "old version with symbols + new version without symbols"**

### Division of Labor with Other Skills

| Scenario | What to Use |
|------|--------|
| Reverse a binary from scratch | `ida-reverse/` or `radare2/` |
| Have old-version results, migrate to the new version | **this skill** |
| Compare two completely different binaries | BinDiff / Diaphora (traditional tools) |

### Core Advantages

Compared to traditional approaches:

| Approach | Cost for 200 Functions | Time | Accuracy |
|------|--------------|------|--------|
| Manual comparison in two IDA windows | free but soul-crushing | hours | high |
| BinDiff auto-matching | free | fast | medium (fails on large structural changes) |
| Fully delegated to an Agent (CC/Codex) | 50-100 CNY | slow | high |
| **This skill (LLM batch comparison)** | **~1 CNY** | **~10 seconds/function** | **high** |

## Core Principles

```text
Old-version function (with symbols)     Same function in the new version (no symbols)
    ↓                              ↓
Export disassembly + pseudocode      Export disassembly + pseudocode
    ↓                              ↓
    └──────── LLM structured comparison ────────┘
                    ↓
         Output YAML (symbol mapping table)
                    ↓
         Programmatic parsing → batch-apply to the new-version IDB
```

Key points:
- The prompt is a fixed template, filled programmatically
- Input and output formats are deterministic and parsed programmatically
- The LLM only handles the "look at two pieces of code, find the correspondence" step
- Time and token costs are minimal

## Prompt Template

### Standard Comparison Prompt

```text
I have disassembly outputs and procedure code of the same function.

This is the function for reference:

**Disassembly for Reference**
```c
{disasm_for_reference}
```

**Procedure code for Reference**
```c
{procedure_for_reference}
```

This is the function you need to reverse-engineering:

**Disassembly to reverse-engineering**
```c
{disasm_code}
```

**Procedure code to reverse-engineering**
```c
{procedure}
```

What you need to do is to collect all references to "{symbol_name_list}" in the function you need to reverse-engineering and output those references as YAML.

Example:
```yaml
found_vcall: # This is for indirect call to virtual function or virtual function pointer fetching.
  - insn_va: '0x180777700' # Always be the instruction with displacement offset
    insn_disasm: call [rax+68h] # Always be the instruction with displacement offset
    vfunc_offset: '0x68'
    func_name: ILoopMode_OnLoopActivate
  - insn_va: '0x180777778' # Always be the instruction with displacement offset
    insn_disasm: mov rax, [rax+80h] # Always be the instruction with displacement offset
    vfunc_offset: '0x80'
    func_name: INetworkMessages_GetNetworkGroupCount

found_call: # This is for direct call to non-virtual regular function.
  - insn_va: '0x180888800'
    insn_disasm: call sub_180999900
    func_name: CLoopMode_RegisterEventMapInternal
  - insn_va: '0x180888880'
    insn_disasm: call sub_180555500
    func_name: CLoopMode_SetSystemState

found_funcptr: # This is for non-virtual regular function pointer.
  - insn_va: '0x180666600' # Must load/reference the function pointer target address
    insn_disasm: lea rdx, sub_15BC910 # Must load/reference the function pointer target address
    funcptr_name: CLoopMode_OnClientPollNetworking

found_gv: # This is for reference to global variable.
  - insn_va: '0x180444400'
    insn_disasm: mov rcx, cs:qword_180666600 # Must load/reference the global variable
    gv_name: g_pNetworkMessages
  - insn_va: '0x180333300'
    insn_disasm: lea rax, unk_180222200 # Must load/reference the global variable
    gv_name: s_EventManager

found_struct_offset: # This is for reference to struct offset. NOTE THAT virtual function pointer should not be here! virtual function pointer should ALWAYS be in found_vcall !
  - insn_va: '0x1801BA12A' # Always be the instruction with displacement offset
    insn_disasm: mov rcx, [r14+58h] # Always be the instruction with displacement offset
    offset: '0x58'
    size: 8
    struct_name: CResourceService
    member_name: m_pEntitySystem
```

If nothing found, output an empty YAML. DO NOT output anything other than the desired YAML. DO NOT collect unrelated symbols.
```

### Variable Reference

| Variable | Source | Description |
|------|------|------|
| `{disasm_for_reference}` | exported from the old version in IDA | disassembly with symbols |
| `{procedure_for_reference}` | exported from the old version in IDA | pseudocode with symbols |
| `{disasm_code}` | exported from the new version in IDA | disassembly without symbols |
| `{procedure}` | exported from the new version in IDA | pseudocode without symbols |
| `{symbol_name_list}` | extracted from the old version | list of symbols to locate in the new version |

## Workflow

### Full Pipeline

```text
Step 1: prepare the data
  - Load the old-version binary into IDA (with PDB/symbols)
  - Load the new-version binary into IDA (without symbols)
  - Find anchor functions present in both versions (exported functions, string references, etc.)

Step 2: batch export
  - From the old version: disassembly + pseudocode of anchor functions (with symbol names)
  - From the new version: disassembly + pseudocode of the same anchor functions (without symbol names)

Step 3: LLM comparison
  - Fill the prompt template with the data
  - Call the LLM API (recommended: deepseek for volume and cheapness; switch to gpt for huge functions)
  - Parse the returned YAML

Step 4: apply the results
  - Batch-apply the symbol mappings from the YAML to the new-version IDB
  - Batch rename with idapro_rename or an IDAPython script

Step 5: iterate
  - Functions migrated in the first round become new anchors
  - Enter those functions and continue comparing their internal calls
  - Repeat until all target functions are covered
```

### Anchor Selection Strategy

| Anchor Type | Reliability | Notes |
|---------|--------|------|
| Exported functions | highest | names unchanged, addresses may shift |
| String references | high | string content unchanged, reference sites may shift |
| Constants/magic numbers | medium | signature values unchanged |
| Code patterns | medium | similar function structure but all addresses change |

### Batch Processing Advice

- Compare 1 function at a time (avoid context blowup)
- Use deepseek for medium functions (<200 lines)
- Switch to gpt-4o or claude for very large functions (>500 lines)
- Make concurrent calls for speed (10-20 in parallel)
- Cache results to avoid duplicate calls

## Output Format

### The 5 Symbol Types in the YAML Output

| Type | Meaning | Key Fields |
|------|------|---------|
| `found_vcall` | virtual function call (indirect call) | `vfunc_offset`, `func_name` |
| `found_call` | direct function call | `insn_va`, `func_name` |
| `found_funcptr` | function pointer reference | `insn_va`, `funcptr_name` |
| `found_gv` | global variable reference | `insn_va`, `gv_name` |
| `found_struct_offset` | struct offset reference | `offset`, `struct_name`, `member_name` |

### Application Actions After Parsing

```text
found_call → idapro_rename(addr=call_target, name=func_name)
found_vcall → idapro_set_comments(addr=insn_va, comment="vcall: {func_name} @ +{offset}")
found_funcptr → idapro_rename(addr=funcptr_target, name=funcptr_name)
found_gv → idapro_rename(addr=gv_addr, name=gv_name)
found_struct_offset → idapro_set_comments(addr=insn_va, comment="{struct_name}.{member_name}")
```

## Typical Scenario Examples

### Scenario 1: ntoskrnl.exe Missing a PDB

```text
Have: ntoskrnl.exe 10.0.26100.2000 + complete PDB
Target: ntoskrnl.exe 10.0.26100.2605 (PDB pulled from the symbol server)
Need: locate the new address of PspSetCreateProcessNotifyRoutine

Steps:
1. Load both versions into IDA
2. Find the exported function PsSetCreateProcessNotifyRoutine (present in both)
3. In the old version it calls PspSetCreateProcessNotifyRoutine (with symbol)
4. In the new version it calls sub_140822108 (no symbol)
5. The LLM sees at a glance: sub_140822108 = PspSetCreateProcessNotifyRoutine
6. Batch-apply
```

### Scenario 2: Migration After an App Update

```text
Have: complete reversing results for target.exe v1.0 (200+ functions named)
Target: target.exe v1.1 (all symbols lost)
Need: batch-migrate 200 function names

Steps:
1. Export disassembly + pseudocode of all named functions from the old version
2. Find the corresponding anchors in the new version via exported functions/strings
3. Call the LLM in batches for comparison
4. Parse the YAML, batch rename
5. Iterate deeper
```

## LLM Selection Advice

| Model | Suitable Scenarios | Cost | Speed |
|------|---------|------|------|
| DeepSeek V3 | small/medium functions (<200 lines), batch processing | extremely low | fast |
| GPT-4o | very large functions, complex control flow | medium | fast |
| Claude Sonnet | medium/large functions, reasoning needed | medium | fast |
| Claude Opus | extremely complex functions, deep understanding needed | high | slow |

Recommended strategy: DeepSeek by default; auto-upgrade when context limits are hit or results are inaccurate.

## Caveats

- **Do not throw the entire binary at the LLM** — compare one function at a time
- **Anchors must be reliable** — if an anchor is itself mismatched, everything downstream is wasted
- **Results need manual spot checks** — the LLM is not 100% accurate; verify key symbols
- **Cache intermediate results** — avoid wasting tokens on repeated calls
- **Mind context limits** — very large functions (>1000 lines of disassembly) must be split or use a large-context model

---

## On-Demand Bootstrap

### Tool Dependencies

| Tool | Purpose | Auto-installable |
|------|------|-----------|
| IDA Pro | exporting disassembly/pseudocode | ✗ (commercial software) |
| Python | script execution, API calls | ✓ |
| PyYAML | parsing the LLM's returned YAML | ✓ (pip install pyyaml) |
| LLM API | performing the comparison | requires an API key |

### Notes

The core of this skill does not depend on heavy tool installs; it mainly relies on:
- IDA Pro already present (managed via the `ida-reverse/` skill)
- Python + requests/httpx (API calls)
- An LLM API endpoint

---

## Routing Context

**Upstream entry**: `skills/SKILL.md` (master control), `routing.md`
**Trigger conditions**: you have old-version symbols/reversing results that need migrating to a new version
**Downstream exits**:
- Need to open the binary first → `ida-reverse/`
- Need quick recon to confirm version differences → `radare2/`

**Peer related modules**: `ida-reverse/` (data export and symbol application both go through IDA)


## Task Completion Self-Check (MUST pass before claiming completion)

- [ ] Did I execute every step of the workflow (not just read it)?
- [ ] Did I use real tool paths based on `tool-index`?
- [ ] Did I produce reproducible evidence (commands/scripts/screenshots/reports)?
- [ ] Did I complete and write back the Checklist items required by RULES?
