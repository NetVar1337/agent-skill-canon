---
name: "headless-ghidra-intake"
description: "P0 phase skill for Headless Ghidra intake. Use when a target binary/archive needs identity confirmation, workspace initialization, Ghidra discovery, binary inspection, or analysis scope setup before any Ghidra analysis runs."
phase: "P0"
---

# Headless Ghidra Intake — P0

P0 prepares a target for the rest of the workflow. It is responsible for
turning an input binary or archive path into a valid `ghidra-agent-cli`
workspace target and recording the initial analysis scope.

## Required ghidra-agent-cli Commands

- `ghidra-agent-cli workspace init`
- `ghidra-agent-cli ghidra discover`
- `ghidra-agent-cli inspect binary`
- `ghidra-agent-cli scope show`
- `ghidra-agent-cli scope set`
- `ghidra-agent-cli scope add-entry`
- `ghidra-agent-cli scope remove-entry`
- `ghidra-agent-cli gate check --phase P0`

Archive normalization and any target-specific bootstrap scripts may still
run as backend details until the CLI exposes them directly.

## Inputs

- User-provided binary or archive path
- Workspace root
- Local environment needed for Ghidra discovery

## Outputs

- `targets/<target-id>/ghidra-projects/`
- `artifacts/<target-id>/pipeline-state.yaml`
- `artifacts/<target-id>/scope.yaml`
- Optional phase-owned intake records under `artifacts/<target-id>/intake/`

## Exit Expectations

- Target workspace exists and is addressable through `--target <id>`.
- `pipeline-state.yaml` records the selected binary path.
- `scope.yaml` records the selected functions, addresses, symbols, or explicit
  whole-target scope.
- Ghidra discovery and binary inspection have been run and reviewed.

## Constraints

- Do not run actual Ghidra analysis in this phase.
- Do not write baseline, evidence, decompilation, or verification outputs.
- Do not bypass `ghidra-agent-cli workspace init`, `ghidra discover`, or
  `inspect binary` for supported steps.
- Do not create or run a new Ghidra script if the CLI lacks a capability; pause
  and ask the user first.

## Next Step

- P0 gate passes → `headless-ghidra-baseline`
