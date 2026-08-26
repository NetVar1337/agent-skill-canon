# Headless Ghidra Single Function Analysis

Perform a thorough analysis of one function using the strict recovery order:
types -> constants -> vtables -> function identity -> decompilation.

Translations: [简体中文](./README.zh-CN.md) | [日本語](./README.ja-JP.md)

## When To Use

Use this skill when:

- One function needs a complete, provenance-aware analysis.
- The target function must be resolved to a unique address before analysis.
- Existing baseline, metadata, and substitution artifacts may need updates.
- Decompilation must happen only after types, constants, vtables, and function identity are recovered.

## Resolve The Target Function First

Resolve a unique `addr` before Step 1, then choose a stable local `fn_id`.

If the user already gives an address:

```sh
ghidra-agent-cli --target <id> functions show --addr <addr>
```

If the user gives a symbol or function name:

```sh
ghidra-agent-cli --target <id> functions list --named-only
```

Match the requested symbol to a single entry and record its unique `addr`.

Resolution rules:

- Reuse a user-provided `fn_id` when one already exists.
- Otherwise derive a safe local id from the normalized address, for example `fn_00102140`.
- Do not claim baseline function entries contain `fn_id`; they do not.
- If multiple matches exist, stop and ask the user which function they mean.

## Strict Recovery Order

Follow this order exactly:

1. Type definitions
2. Constant definitions
3. Vtable recovery
4. Function identity
5. Decompilation

Do not skip or reorder steps. Step 5 is only valid after Steps 1-4 are complete.

## Allowed Artifact Updates

This skill may update existing artifacts under `artifacts/<target-id>/`:

- `baseline/vtables.yaml`
- `metadata/renames.yaml`
- `metadata/signatures.yaml`
- `substitution/functions/<fn_id>/*`
- `substitution/next-batch.yaml`

Optional agent-authored notes may also be written under:

- `analysis/functions/<fn_id>/step1-types.yaml`
- `analysis/functions/<fn_id>/step2-constants.yaml`
- `analysis/functions/<fn_id>/step3-vtables.yaml`
- `analysis/functions/<fn_id>/step4-identity.yaml`
- `analysis/functions/<fn_id>/step5-decompilation.yaml`
- `analysis/functions/<fn_id>/provenance.yaml`

These `analysis/functions/<fn_id>/step*.yaml` files are optional notes written by the agent, not files generated automatically by `ghidra-agent-cli`.

## Command Summary

These are the key commands this skill relies on:

```sh
# Resolve a function by address
ghidra-agent-cli --target <id> functions show --addr <addr>

# Resolve a function name to a unique address
ghidra-agent-cli --target <id> functions list --named-only

# Step 1: types
ghidra-agent-cli --target <id> types list
ghidra-agent-cli --target <id> ghidra export-baseline

# Step 2: constants
ghidra-agent-cli --target <id> constants list
ghidra-agent-cli --target <id> strings list

# Step 3: vtables
ghidra-agent-cli --target <id> vtables list
ghidra-agent-cli --target <id> ghidra analyze-vtables --write-baseline

# Step 4: function identity
ghidra-agent-cli --target <id> metadata enrich-function --addr <addr> --name '<recovered_name>' --prototype '<signature>'
ghidra-agent-cli --target <id> ghidra apply-renames
ghidra-agent-cli --target <id> ghidra apply-signatures

# Step 5: decompilation and substitution
ghidra-agent-cli --target <id> ghidra decompile --fn-id <fn_id> --addr <addr>
ghidra-agent-cli --target <id> substitute add --fn-id <fn_id> --addr <addr> --replacement '<curated replacement source>'
```

## Step Intent

- Step 1 recovers structs, enums, typedefs, pointers, arrays, and other data types used by the function.
- Step 2 recovers magic numbers, flags, string constants, and other referenced constants.
- Step 3 refreshes vtable understanding and can update `baseline/vtables.yaml`.
- Step 4 sets the recovered function name and prototype using a single `metadata enrich-function` call, then applies renames and signatures globally without per-function flags.
- Step 5 decompiles the function with the chosen local `fn_id`, then records a curated substitution via `substitute add --replacement ...`.

## Constraints

- Resolve `addr` before Step 1 and choose `fn_id` before Step 5.
- Use `functions show --addr` for address input and `functions list --named-only` for symbol-name lookup.
- Read `metadata/renames.yaml` and `metadata/signatures.yaml` directly if metadata listing commands are unavailable.
- Keep provenance clear so each later step can be traced back to earlier evidence.
