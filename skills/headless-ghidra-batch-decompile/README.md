# Headless Ghidra Function Substitution — P4

P4 applies enriched metadata, decompiles selected functions through Ghidra, and
records per-function substitution artifacts. It is the only primary phase that
creates selected decompilation and substitution records.

Translations: [简体中文](./README.zh-CN.md) | [日本語](./README.ja-JP.md)

## Place In The Pipeline

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P4 is batch-oriented. After a batch, the workflow either returns to P3 for more
metadata enrichment or finishes when selected functions are complete.

## When To Use

Use this skill when:

- P3 has validated names and signatures for selected functions.
- `substitution/next-batch.yaml` identifies the current worklist.
- Selected functions need Ghidra decompilation.
- Function-local captures, substitutions, and follow-up records need to be
  written.
- A reconstructed boundary must be compared with the original target.

## Phase Boundaries

- Ghidra is the only approved decompilation backend.
- Apply, verify, decompile, rebuild, and substitution writes go through
  `ghidra-agent-cli`.
- Work only on functions in the active batch.
- Pristine third-party source must not be modified.
- Unresolved callees should route back to the original target during compare.

## Inputs

- `artifacts/<target-id>/baseline/*.yaml`
- `artifacts/<target-id>/runtime/fixtures/`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- `artifacts/<target-id>/third-party/identified.yaml`
- `artifacts/<target-id>/third-party/pristine/<library>@<version>/`
- `artifacts/<target-id>/third-party/compat/<library>@<version>/`, if needed
- `artifacts/<target-id>/metadata/renames.yaml`
- `artifacts/<target-id>/metadata/signatures.yaml`
- `artifacts/<target-id>/substitution/next-batch.yaml`

## Outputs

- `artifacts/<target-id>/substitution/functions/<fn_id>/capture.yaml`
- `artifacts/<target-id>/substitution/functions/<fn_id>/substitution.yaml`
- Optional function-local blocked, injected, comparison, or follow-up YAML
- Gate reports for P4

## Commands The Skill Uses

These examples show the CLI calls the skill may make. In normal use, ask the agent to run the phase; do not run these commands by hand unless you are troubleshooting.

```sh
ghidra-agent-cli --target sample-target ghidra apply-renames
ghidra-agent-cli --target sample-target ghidra verify-renames
ghidra-agent-cli --target sample-target ghidra apply-signatures
ghidra-agent-cli --target sample-target ghidra verify-signatures
ghidra-agent-cli --target sample-target ghidra decompile --fn-id fn_001 --addr 0x401000
ghidra-agent-cli --target sample-target substitute add \
  --fn-id fn_001 \
  --addr 0x401000 \
  --replacement './reconstructed/fn_001.c' \
  --note 'selected boundary replacement'
ghidra-agent-cli --target sample-target substitute validate
ghidra-agent-cli --target sample-target ghidra rebuild-project
ghidra-agent-cli --target sample-target gate check --phase P4
```

Batch decompilation:

```sh
ghidra-agent-cli --target sample-target ghidra decompile --batch
```

## Phase Flow

1. Confirm P3 gate has passed.
2. Review `substitution/next-batch.yaml`.
3. Re-apply and verify metadata if the Ghidra project changed.
4. Capture function-local fixtures and original behavior.
5. Decompile only selected batch functions.
6. Record substitution provenance, status, and replacement boundary.
7. Rebuild or prepare the hybrid target when required.
8. Run original-versus-hybrid compare.
9. Validate substitutions and run the P4 gate.

## Incremental Compare Contract

Each step replaces only the current outside-in boundary. Still-unreconstructed
callees must route back to the original binary or original library handle. Move
inward only after the current compare case is recorded as `matched`.

## Exit Criteria

- Every processed function has capture and substitution records.
- Decompilation provenance names Ghidra/CLI as the source of record.
- Replacement boundary and fallback route are explicit.
- Comparison result is recorded or the block reason is explicit.
- P4 gate passes for the batch.

## Blockers

Stop before completing P4 when:

- A function is not in the active batch.
- A selected function lacks P3 name or signature evidence.
- Decompilation did not run through Ghidra/CLI.
- Capture fixtures are missing.
- Unresolved callees cannot route back to the original target.
- Pristine third-party source would need modification.

## Handoff After P4

If the batch is matched and more functions remain, return to
`headless-ghidra-discovery` for the next outside-in selection. If divergence or
missing metadata appears, return to P3 with explicit follow-up questions. If all
selected functions are complete, finish the pipeline.
