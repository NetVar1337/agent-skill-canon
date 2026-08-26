---
name: "headless-ghidra-baseline"
description: "P1 phase skill for Headless Ghidra baseline and runtime evidence. Use after P0 when the target must be imported into Ghidra, auto-analyzed, exported to baseline YAML, and given reproducible runtime or hotpath observations without decompiling function bodies."
phase: "P1"
---

# Headless Ghidra Baseline+Runtime — P1

P1 runs the initial Ghidra import and auto-analysis, exports baseline YAML
metadata, makes the target reproducibly runnable, and records runtime/hotpath
YAML that later phases consume.

## Required ghidra-agent-cli Commands

- `ghidra-agent-cli ghidra import`
- `ghidra-agent-cli ghidra auto-analyze`
- `ghidra-agent-cli ghidra export-baseline`
- `ghidra-agent-cli frida device-list`
- `ghidra-agent-cli frida device-attach`
- `ghidra-agent-cli frida io-capture`
- `ghidra-agent-cli frida trace`
- `ghidra-agent-cli runtime record`
- `ghidra-agent-cli runtime validate`
- `ghidra-agent-cli hotpath add`
- `ghidra-agent-cli hotpath validate`
- `ghidra-agent-cli gate check --phase P1`

The shell wrappers and Java Ghidra scripts remain the backend implementation for
these commands. The workflow contract is the CLI surface plus the YAML outputs
below.

## Inputs

- `artifacts/<target-id>/pipeline-state.yaml`
- `artifacts/<target-id>/scope.yaml`
- `targets/<target-id>/ghidra-projects/`

## Outputs

- `artifacts/<target-id>/baseline/functions.yaml`
- `artifacts/<target-id>/baseline/callgraph.yaml`
- `artifacts/<target-id>/baseline/types.yaml`
- `artifacts/<target-id>/baseline/vtables.yaml`
- `artifacts/<target-id>/baseline/constants.yaml`
- `artifacts/<target-id>/baseline/strings.yaml`
- `artifacts/<target-id>/baseline/imports.yaml`
- `artifacts/<target-id>/runtime/run-manifest.yaml`
- `artifacts/<target-id>/runtime/run-records/*.yaml`
- `artifacts/<target-id>/runtime/fixtures/**`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- `artifacts/<target-id>/runtime/project/**` when a library harness is needed

## Exit Expectations

- All required baseline YAML files exist and are readable.
- Runtime availability or unavailability is recorded with reproducible
  executable args or a C++/CMake library harness.
- The P1 hotpath call-chain is available as the initial P3/P4 priority source.
- No P4 decompilation artifacts are created in this phase.

## Constraints

- Do not decompile function bodies in P1.
- Do not apply renames or signatures in P1.
- Do not modify `pipeline-state.yaml` except through sanctioned state changes.
- Do not bypass `ghidra-agent-cli` for import, analysis, export, or supported
  gate checks.
- Do not create or run a new Ghidra script if the CLI lacks a capability; pause
  and ask the user first.

## Next Step

- P1 gate passes → `headless-ghidra-evidence`
