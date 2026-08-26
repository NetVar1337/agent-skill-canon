---
name: "headless-ghidra-batch-decompile"
description: "P4 phase skill for Headless Ghidra selected function substitution. Use after P3 when an approved batch of functions should have metadata applied, be decompiled through Ghidra, and be recorded as per-function capture/substitution YAML."
phase: "P4"
---

# Headless Ghidra Function Substitution — P4

P4 consumes the current selected batch, applies enriched metadata, runs the
approved Ghidra decompilation path, and records per-function substitution
artifacts.

## Required ghidra-agent-cli Commands

- `ghidra-agent-cli ghidra apply-renames`
- `ghidra-agent-cli ghidra verify-renames`
- `ghidra-agent-cli ghidra apply-signatures`
- `ghidra-agent-cli ghidra verify-signatures`
- `ghidra-agent-cli ghidra decompile`
- `ghidra-agent-cli ghidra rebuild-project`
- `ghidra-agent-cli substitute add`
- `ghidra-agent-cli substitute validate`
- `ghidra-agent-cli gate check --phase P4`

Locking and Ghidra invocation are handled internally by the CLI.
The public workflow surface is the CLI plus the YAML outputs below.
When `substitution/next-batch.yaml` is ready, process only functions with
clear P3 names and signatures.

## Inputs

- `artifacts/<target-id>/baseline/*.yaml`
- `artifacts/<target-id>/runtime/fixtures/`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- `artifacts/<target-id>/third-party/identified.yaml`
- `artifacts/<target-id>/third-party/pristine/<library>@<version>/`
- `artifacts/<target-id>/third-party/compat/<library>@<version>/` if needed
- `artifacts/<target-id>/metadata/renames.yaml`
- `artifacts/<target-id>/metadata/signatures.yaml`
- `artifacts/<target-id>/substitution/next-batch.yaml`

## Outputs

- `artifacts/<target-id>/substitution/functions/<fn_id>/capture.yaml`
- `artifacts/<target-id>/substitution/functions/<fn_id>/substitution.yaml`
- Additional per-function YAML such as blocked, injected, or follow-up records
  when the workflow records them

## Exit Expectations

- Function-level I/O fixtures and capture YAML are recorded before coding a
  substitute.
- Each substituted function has a `substitution.yaml` with provenance, fixtures,
  and status.
- P4 gate material is available under `substitution/functions/<fn_id>/`.

## Constraints

- Use Ghidra as the only decompilation backend.
- Acquire the Ghidra queue/lock before mutating or reading shared Ghidra state
  when the backend requires it.
- Do not modify artifacts for functions outside the active batch.
- Do not bypass `ghidra-agent-cli` for supported apply/verify/decompile,
  substitution, fixture, or gate actions.
- Do not modify pristine third-party source; place local adaptation changes under
  `third-party/compat/`.
- Do not create or run a new Ghidra script if the CLI lacks a capability; pause
  and ask the user first.

## Next Step

- P4 gate passes for the batch → return to P3 for another round or finish.
