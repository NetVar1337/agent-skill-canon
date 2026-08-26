---
name: "headless-ghidra-analyze-function"
description: "Thoroughly analyze a single function by first recovering its type definitions, constant definitions, vtables, function name and signature, then producing the final decompilation. Use when the user wants a complete per-function analysis following the strict recovery order: types → constants → vtables → function identity → decompilation."
---

# Headless Ghidra — Single Function Thorough Analysis

This skill provides a dedicated, strict-order analysis workflow for a single function.
It works against the existing target artifact tree and may update baseline, metadata,
and substitution artifacts while recording provenance for a focused single-function
investigation.

## Resolve Target Function

Resolve the requested function to a unique `addr` before Step 1, then derive a
stable local `fn_id` for this analysis.

### If the input is an address

Use the address directly and confirm the resolved function identity:

```sh
ghidra-agent-cli --target <id> functions show --addr <addr>
```

Confirm the address is valid and use it as the canonical function locator for
the rest of the workflow.

### If the input is a symbol or function name

Look up named baseline functions first:

```sh
ghidra-agent-cli --target <id> functions list --named-only
```

Match the requested name and record the unique `addr` from the matching entry.

### Resolution Rules

- If there are multiple matches, stop and ask the user to clarify which function they want.
- If there is no match, inspect `artifacts/<target-id>/baseline/functions.yaml` to confirm whether the name is missing, ambiguous, or spelled differently.
- Do not guess an address from a partial or approximate name.
- If the user already provided a `fn_id`, reuse it.
- Otherwise derive a safe local `fn_id` from the normalized address, for example `fn_<addr_without_0x>` such as `fn_00102140`.
- Step 1 may only begin after `addr` is confirmed and the working `fn_id` is chosen.

## Strict Recovery Order

Every function analysis **must** follow this five-step sequence:

```
Step 1 → Type Definitions
Step 2 → Constant Definitions
Step 3 → Vtable Recovery
Step 4 → Function Name & Signature
Step 5 → Decompilation Analysis
```

**No step may be skipped or reordered.** Decompilation analysis in Step 5 is only
valid after Steps 1–4 are complete, because types, constants, vtables, and
correct function identity are prerequisites for accurate decompilation interpretation.

---

## Step 1 — Recover Type Definitions

Before anything else, enumerate and recover all data types (structs, unions,
enums, typedefs, pointers, arrays) that the function uses.

### Required ghidra-agent-cli Commands

```sh
# List all types from baseline
ghidra-agent-cli --target <id> types list

# Export fresh baseline types if needed
ghidra-agent-cli --target <id> ghidra export-baseline

# Show resolved function details (check signature/return type)
ghidra-agent-cli --target <id> functions show --addr <addr>
```

### Outputs

- `artifacts/<target-id>/baseline/types.yaml` — full type system
- Function-referenced type subset documented in working analysis

### What to Look For

- Struct definitions referenced by function parameters or local variables
- Enum values used in switch statements or comparisons
- Typedef aliases for readability
- Pointer types to other functions (for callback signatures)

### CLI Adaptation

If `ghidra-agent-cli types list` returns no results, first run `ghidra export-baseline`
to populate the baseline YAML files.

---

## Step 2 — Recover Constant Definitions

After types, recover all constants (magic numbers, bitmasks, flag values,
enumerated constants, string constants) that the function references.

### Required ghidra-agent-cli Commands

```sh
# List all constants from baseline
ghidra-agent-cli --target <id> constants list

# List all strings (format strings, keys, identifiers)
ghidra-agent-cli --target <id> strings list
```

### Outputs

- `artifacts/<target-id>/baseline/constants.yaml` — full constant pool
- `artifacts/<target-id>/baseline/strings.yaml` — string constants
- Function-referenced constant subset documented in working analysis

### What to Look For

- Numeric literals that may be #define macro values
- Bitmask operations and their constituent flags
- Magic numbers with known semantic meaning (e.g., 0x202 for ELF header)
- Protocol-specific constants
- String constants used as keys or identifiers

---

## Step 3 — Recover Vtable Recovery

Identify if the function is a virtual method or interacts with virtual dispatch
tables. Recover all vtables the function reads from or writes to.

### Required ghidra-agent-cli Commands

```sh
# List all vtables from baseline
ghidra-agent-cli --target <id> vtables list

# Analyze vtables in the binary and refresh baseline/vtables.yaml
ghidra-agent-cli --target <id> ghidra analyze-vtables --write-baseline
```

### Outputs

- `artifacts/<target-id>/baseline/vtables.yaml` — all virtual tables
- `artifacts/<target-id>/baseline/vtable-analysis-report.yaml` — vtable analysis results (if produced)

### What to Look For

- Function is a virtual method (thunk to vtable dispatch)
- Function reads from or initializes vtable pointers
- Object construction/destruction sequences involving vtables
- Multiple dispatch paths through vtables

---

## Step 4 — Recover Function Name & Signature

After types, constants, and vtables are understood, recover the function's
canonical name and its complete type signature.

### Required ghidra-agent-cli Commands

```sh
# Show current resolved function info
ghidra-agent-cli --target <id> functions show --addr <addr>

# Name lookup is handled earlier during target resolution when needed
# ghidra-agent-cli --target <id> functions list --named-only

# Write enriched name and signature together.
# This command updates both metadata/renames.yaml and metadata/signatures.yaml.
ghidra-agent-cli --target <id> metadata enrich-function --addr <addr> --name '<recovered_name>' --prototype '<signature>'

# Apply the metadata YAML back into Ghidra without per-function flags
ghidra-agent-cli --target <id> ghidra apply-renames
ghidra-agent-cli --target <id> ghidra apply-signatures
```

### Inputs

- Step 1 output: types that inform the parameter and return types
- Step 3 output: vtable context that may indicate virtual method signature
- `artifacts/<target-id>/metadata/renames.yaml` — existing P3 rename enrichment (if available)
- `artifacts/<target-id>/metadata/signatures.yaml` — existing P3 signature enrichment (if available)

### Metadata Check

Read these files directly if they exist:

1. `artifacts/<target-id>/metadata/renames.yaml`
2. `artifacts/<target-id>/metadata/signatures.yaml`

Use `functions list --named-only` only for baseline name lookup during target
resolution, not as a metadata inspection command.

### Outputs

- `artifacts/<target-id>/metadata/renames.yaml` — function name record
- `artifacts/<target-id>/metadata/signatures.yaml` — function prototype

### What to Look For

- Demangled symbol name from imports/exports
- Calling convention indicators from call site analysis
- Parameter count and types from callee references
- Return type from return value usage analysis

### CLI Adaptation

If you prefer not to use `metadata enrich-function`, edit these YAML files
directly, then run the apply commands without per-function flags:
1. `artifacts/<target-id>/metadata/renames.yaml`
2. `artifacts/<target-id>/metadata/signatures.yaml`
3. `ghidra-agent-cli --target <id> ghidra apply-renames`
4. `ghidra-agent-cli --target <id> ghidra apply-signatures`

If `ghidra-agent-cli metadata list --addr` is needed, instead:
1. Read `artifacts/<target-id>/metadata/renames.yaml` directly
2. Read `artifacts/<target-id>/metadata/signatures.yaml` directly
3. Check `ghidra-agent-cli functions show --addr <addr>` for current Ghidra-assigned name

---

## Step 5 — Decompilation Analysis

**Only after Steps 1–4 are complete**, perform the final decompilation analysis
and present the results with full type and constant context.

### Required ghidra-agent-cli Commands

```sh
# Decompile the function with full context using the chosen local fn_id
ghidra-agent-cli --target <id> ghidra decompile --fn-id <fn_id> --addr <addr>

# Review the generated decompilation, then save a curated function-level
# replacement C file before recording it.
decompiled_c="artifacts/<target-id>/decompilation/functions/<fn_id>/<fn_id>.c"
replacement_c="$(mktemp)"
cp "$decompiled_c" "$replacement_c"
ghidra-agent-cli --target <id> substitute add --fn-id <fn_id> --addr <addr> --replacement "$(cat "$replacement_c")"
```

### Outputs

- `artifacts/<target-id>/baseline/vtables.yaml` — updated earlier if vtable analysis refreshes baseline state
- `artifacts/<target-id>/substitution/functions/<fn_id>/capture.yaml` — pre-decompilation snapshot
- `artifacts/<target-id>/substitution/functions/<fn_id>/substitution.yaml` — final analysis with provenance
- `artifacts/<target-id>/substitution/next-batch.yaml` — substitution queue state updated by `substitute add`

### Analysis Deliverables

1. **Decompiled source** with all types resolved (no raw IDs)
2. **Constant substitution** — all magic numbers replaced with meaningful names
3. **Vtable annotations** — virtual call targets identified
4. **Function prototype** — final signature with parameter names
5. **Provenance chain** — which Step 1–4 artifact informed each resolution

---

## Artifact Outputs Per Function

This skill records provenance in the existing pipeline artifacts and may update:

```
artifacts/<target-id>/
├── baseline/
│   └── vtables.yaml
├── metadata/
│   ├── renames.yaml
│   └── signatures.yaml
└── substitution/
    ├── next-batch.yaml
    └── functions/
        └── <fn_id>/
            ├── capture.yaml
            └── substitution.yaml
```

Optional agent-authored notes may also be written under the chosen local `fn_id`:

```
artifacts/<target-id>/analysis/functions/<fn_id>/
├── step1-types.yaml
├── step2-constants.yaml
├── step3-vtables.yaml
├── step4-identity.yaml
├── step5-decompilation.yaml
└── provenance.yaml
```

These `analysis/functions/<fn_id>/step*.yaml` files are not generated by
`ghidra-agent-cli` automatically. If the user wants per-step analysis notes, the
agent must write and maintain those YAML files explicitly.

---

## Required ghidra-agent-cli Commands Summary

| Step | Purpose                  | Key Commands                                                     |
|------|--------------------------|------------------------------------------------------------------|
| 1    | Type definitions         | `ghidra-agent-cli types list`, `ghidra export-baseline`         |
| 2    | Constant definitions     | `ghidra-agent-cli constants list`, `ghidra-agent-cli strings list` |
| 3    | Vtable recovery          | `ghidra-agent-cli vtables list`, `ghidra analyze-vtables --write-baseline` |
| 4    | Function name & sig      | `ghidra-agent-cli functions show`, `metadata enrich-function`, `ghidra apply-renames/signatures` |
| 5    | Decompilation            | `ghidra-agent-cli ghidra decompile`, `substitute add --replacement ...` |

---

## Invocation

To analyze a specific function by address:

```
Use the headless-ghidra-analyze-function skill with:
- Function address: <hex address>
- Target ID: <target-id>
```

To analyze by symbol name (if exported):

```
Use the headless-ghidra-analyze-function skill with:
- Symbol name: <mangled or demangled name>
- Target ID: <target-id>
```

## Constraints

- **Must follow the five-step order.** Do not skip to Step 5 without Steps 1–4.
- **Resolve `addr` first, then choose `fn_id`.** Use `functions show --addr <addr>` for address input, or `functions list --named-only` for symbol-name lookup. Reuse a user-provided `fn_id` or derive one from the normalized address.
- **Do not decompile first and work backwards.** Decompilation without type/constant/vtable context produces inaccurate analysis.
- **Use ghidra-agent-cli exclusively.** All Ghidra operations must go through the CLI.
- **Record provenance.** The final analysis must explain what each step discovered and how it informed the next step; write optional `analysis/functions/<fn_id>/step*.yaml` notes only when per-step files are needed.
- **Existing artifacts may be updated.** This skill is allowed to modify `baseline/vtables.yaml`, `metadata/renames.yaml`, `metadata/signatures.yaml`, `substitution/functions/<fn_id>/*`, and `substitution/next-batch.yaml`.
- **Read metadata files directly when CLI commands are unavailable.** If a CLI command for metadata listing does not exist, read the YAML files directly using the Read tool.

## Entry Points

This skill is invoked when the user says:

- "analyze this function thoroughly"
- "decompile function at 0x401000 with full context"
- "what does this function do, including all the types and constants it uses"
- "give me a complete analysis of [function name]"
