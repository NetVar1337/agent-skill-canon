---
name: "headless-ghidra-discovery"
description: "P3 phase skill for Headless Ghidra metadata discovery. Use after P2, or after a P4 batch exposes missing context, to enrich function names, signatures, types, constants, strings, and hotpath metadata in YAML before serialized CLI apply."
phase: "P3"
---

# Headless Ghidra Metadata Enrichment — P3

P3 enriches function metadata from third-party evidence and the P1 runtime
hotpath call-chain. Analysis work may be parallelized while producing YAML, but
all writes back to the Ghidra project must go through serialized
`ghidra-agent-cli ghidra ...` commands.

## Required ghidra-agent-cli Commands

- `ghidra-agent-cli workspace state show`
- `ghidra-agent-cli functions list`
- `ghidra-agent-cli callgraph callers`
- `ghidra-agent-cli callgraph callees`
- `ghidra-agent-cli metadata enrich-function`
- `ghidra-agent-cli metadata validate`
- `ghidra-agent-cli hotpath validate`
- `ghidra-agent-cli ghidra apply-renames`
- `ghidra-agent-cli ghidra verify-renames`
- `ghidra-agent-cli ghidra apply-signatures`
- `ghidra-agent-cli ghidra verify-signatures`
- `ghidra-agent-cli gate check --phase P3`

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

## Exit Expectations

- Every function in `runtime/hotpaths/call-chain.yaml` has an explicit recovered
  name and signature before P4 starts.
- Metadata enrichment YAML is applied to the Ghidra project only through CLI
  commands under the CLI lock.
- The enrichment is reproducible from recorded baseline, runtime, and
  third-party evidence.

## Constraints

- Do not decompile anything in this phase.
- Do not rewrite historical per-function outputs.
- Do not bypass `ghidra-agent-cli` for supported state, baseline, callgraph,
  metadata, Ghidra apply, or gate operations.
- Do not create or run a new Ghidra script if the CLI lacks a capability; pause
  and ask the user first.

## Next Step

- P3 gate passes → `headless-ghidra-batch-decompile`
