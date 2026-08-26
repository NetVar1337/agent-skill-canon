---
name: "headless-ghidra-evidence"
description: "P2 phase skill for Headless Ghidra third-party evidence. Use after P1 to review baseline/runtime artifacts, identify or rule out third-party code, record pristine sources, classify functions, and capture evidence before metadata recovery."
phase: "P2"
---

# Headless Ghidra Third-Party — P2

P2 reviews baseline and runtime YAML to identify third-party libraries, record
local pristine source directories, and classify functions for later metadata
enrichment. Source download or acquisition is outside the CLI; the CLI records
`source_path`, `pristine_path`, version, confidence, and evidence.

## Required ghidra-agent-cli Commands

- `ghidra-agent-cli functions list`
- `ghidra-agent-cli functions show`
- `ghidra-agent-cli imports list`
- `ghidra-agent-cli constants list`
- `ghidra-agent-cli strings list`
- `ghidra-agent-cli vtables list`
- `ghidra-agent-cli types list`
- `ghidra-agent-cli callgraph list`
- `ghidra-agent-cli callgraph callers`
- `ghidra-agent-cli callgraph callees`
- `ghidra-agent-cli third-party add`
- `ghidra-agent-cli third-party none`
- `ghidra-agent-cli third-party set-version`
- `ghidra-agent-cli third-party list`
- `ghidra-agent-cli third-party classify-function`
- `ghidra-agent-cli third-party vendor-pristine`
- `ghidra-agent-cli execution-log append`
- `ghidra-agent-cli gate check --phase P2`

## Inputs

- `artifacts/<target-id>/baseline/functions.yaml`
- `artifacts/<target-id>/baseline/callgraph.yaml`
- `artifacts/<target-id>/baseline/types.yaml`
- `artifacts/<target-id>/baseline/constants.yaml`
- `artifacts/<target-id>/baseline/vtables.yaml`
- `artifacts/<target-id>/baseline/strings.yaml`
- `artifacts/<target-id>/baseline/imports.yaml`
- `artifacts/<target-id>/runtime/run-manifest.yaml`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- Existing `artifacts/<target-id>/third-party/identified.yaml` if present

## Outputs

- `artifacts/<target-id>/third-party/identified.yaml`
- `artifacts/<target-id>/third-party/pristine/<library>@<version>/`
- Optional local adaptation changes under
  `artifacts/<target-id>/third-party/compat/<library>@<version>/`

## Exit Expectations

- `identified.yaml` records at least one medium-or-higher confidence library
  when third-party code is present.
- `identified.yaml` records `libraries: []` when review finds no third-party
  code.
- Each recorded third-party library has a local `source_path` and a pristine
  directory under `third-party/pristine/`.
- Pristine source directories are kept unmodified; local adaptation edits live
  under `third-party/compat/`.
- The next phase has enough version and function-classification evidence to
  recover names, signatures, and types.

## Constraints

- Do not mutate baseline YAML exports directly.
- Do not claim unsupported evidence without recording the supporting source.
- Do not bypass `ghidra-agent-cli` for supported baseline reads, third-party
  writes, or execution logging.
- Do not create or run a new Ghidra script if the CLI lacks a capability; pause
  and ask the user first.

## Next Step

- P2 gate passes → `headless-ghidra-discovery`
