# Headless Ghidra Metadata Enrichment — P3

P3 converts baseline, runtime, and third-party evidence into enriched metadata:
function names, signatures, types, constants, strings, and selected hotpath
annotations. It prepares Ghidra for selected decompilation but does not
decompile functions itself.

Translations: [简体中文](./README.zh-CN.md) | [日本語](./README.ja-JP.md)

## Place In The Pipeline

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P3 is the metadata half of the iterative recovery loop. P4 may send work back
to P3 when a batch reveals missing names, signatures, or type information.

## When To Use

Use this skill when:

- P2 has identified third-party status and function classifications.
- Hotpath functions need recovered names and signatures before P4.
- Ghidra needs CLI-mediated rename or signature application.
- Metadata YAML needs validation before selected decompilation.

Do not use it for function body decompilation or substitution records.

## Phase Boundaries

- Metadata writes go through `ghidra-agent-cli metadata ...`.
- Ghidra project mutations go through serialized `ghidra-agent-cli ghidra ...`
  commands under the CLI lock.
- Analysis can happen in parallel while producing candidate YAML, but Ghidra
  apply/verify operations are serialized.
- Historical P4 per-function outputs must not be rewritten from P3.

## Inputs

- `artifacts/<target-id>/pipeline-state.yaml`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- `artifacts/<target-id>/third-party/identified.yaml`
- `artifacts/<target-id>/baseline/functions.yaml`
- `artifacts/<target-id>/baseline/callgraph.yaml`
- `artifacts/<target-id>/baseline/types.yaml`

## Outputs

- `artifacts/<target-id>/metadata/renames.yaml`
- `artifacts/<target-id>/metadata/signatures.yaml`
- `artifacts/<target-id>/metadata/types.yaml`
- `artifacts/<target-id>/metadata/constants.yaml`
- `artifacts/<target-id>/metadata/strings.yaml`
- `artifacts/<target-id>/metadata/apply-records/`

## Commands The Skill Uses

These examples show the CLI calls the skill may make. In normal use, ask the agent to run the phase; do not run these commands by hand unless you are troubleshooting.

```sh
ghidra-agent-cli --target sample-target workspace state show
ghidra-agent-cli --target sample-target functions list
ghidra-agent-cli --target sample-target callgraph callers --addr 0x401000
ghidra-agent-cli --target sample-target callgraph callees --addr 0x401000
ghidra-agent-cli --target sample-target metadata enrich-function \
  --addr 0x401000 \
  --name packet_validate_and_dispatch \
  --prototype 'int packet_validate_and_dispatch(void *ctx)'
ghidra-agent-cli --target sample-target metadata validate
ghidra-agent-cli --target sample-target hotpath validate
ghidra-agent-cli --target sample-target ghidra apply-renames
ghidra-agent-cli --target sample-target ghidra verify-renames
ghidra-agent-cli --target sample-target ghidra apply-signatures
ghidra-agent-cli --target sample-target ghidra verify-signatures
ghidra-agent-cli --target sample-target gate check --phase P3
```

## Phase Flow

1. Confirm P2 gate has passed.
2. Review hotpath priority and callgraph context.
3. For each selected function, record evidence for role, name, and prototype.
4. Write metadata through `metadata enrich-function`.
5. Validate metadata and hotpath records.
6. Apply renames and signatures to Ghidra through the CLI.
7. Verify applied renames and signatures.
8. Run the P3 gate.

## Evidence Standards

Every recovered name or prototype should point back to evidence: third-party
source comparison, strings, imports, type usage, callgraph position, runtime
observations, or already matched boundaries. When evidence is weak, keep the
name provisional and do not send the function to P4.

## Exit Criteria

- Hotpath functions selected for P4 have explicit names and signatures.
- Metadata YAML validates.
- Ghidra rename and signature apply operations have verification records.
- Enrichment is reproducible from baseline, runtime, and third-party evidence.
- P3 gate passes.

## Blockers

Stop before P4 when:

- A selected function lacks role, name, or prototype evidence.
- Metadata validation fails.
- Ghidra apply and verify disagree.
- Source-derived names rely on stale or deferred source comparison.
- Hotpath records reference missing functions or invalid addresses.

## Handoff To P4

After P3 passes, route to `headless-ghidra-batch-decompile`. The handoff should
include selected functions, recovered names, signatures, hotpath priority,
known unresolved callees, and any fallback strategy needed for incremental
compare.
