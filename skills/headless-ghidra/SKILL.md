---
name: "headless-ghidra"
description: "Entry skill for the Headless Ghidra YAML-first reverse-engineering pipeline. Use when the user asks to analyze, decompile, triage, resume, or iterate on a binary target with Ghidra/headless-ghidra. Reads artifacts/<target>/pipeline-state.yaml, routes P0–P4 phase skills, runs gate checks, and manages review pauses. Performs zero analysis work itself."
---

# Headless Ghidra — Global Orchestrator

This skill is the workflow coordinator for the skill family. It defines the
P0–P4 sequence, dispatch rules, and artifact hand-off points.
`ghidra-agent-cli` remains the tool reference for command syntax and YAML
artifact semantics.

## Required Shared Tool Contract

- `ghidra-agent-cli` is the mandatory shared interface for supported workspace,
  metadata, Ghidra, Frida, progress, validation, and gate operations.
- Phase skills must name the exact `ghidra-agent-cli` subcommands they use.
- Lower-level shell scripts and Java helpers are backend details. They must not
  replace the CLI as the primary interface when the CLI already supports the
  action.
- All workflow artifacts must live under `artifacts/<target-id>/`.
- YAML artifacts must be created, updated, and validated by `ghidra-agent-cli`.
- The CLI must not automatically create git commits.
- Gate transitions require relevant artifacts to exist on disk and be ready for
  user review.
- All Ghidra project operations must go through `ghidra-agent-cli`. If the CLI
  lacks a required capability, pause and ask the user before creating or running
  a new Ghidra script.

## Pipeline

```text
P0 Intake → P1 Baseline+Runtime → P2 Third-Party → [P3 Metadata Enrichment → P4 Function Substitution]*
```

| Phase | Skill                                                                            | Purpose                                                                             | Primary outputs                                                                                                       |
| ----- | -------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| P0    | [`headless-ghidra-intake`](../headless-ghidra-intake/SKILL.md)                   | Initialize target workspace, discover prerequisites, and define scope               | `pipeline-state.yaml`, `scope.yaml`, `targets/<id>/ghidra-projects/`                                                  |
| P1    | [`headless-ghidra-baseline`](../headless-ghidra-baseline/SKILL.md)               | Run Ghidra import/analysis, export baseline YAML, and prepare runtime observations  | `baseline/*.yaml`, `runtime/run-manifest.yaml`, `runtime/run-records/*.yaml`, `runtime/hotpaths/call-chain.yaml`      |
| P2    | [`headless-ghidra-evidence`](../headless-ghidra-evidence/SKILL.md)               | Identify and record third-party libraries and pristine sources                      | `third-party/identified.yaml`, `third-party/pristine/<library>@<version>/`, `third-party/compat/<library>@<version>/` |
| P3    | [`headless-ghidra-discovery`](../headless-ghidra-discovery/SKILL.md)             | Enrich names, signatures, types, constants, strings, and selected hotpath metadata  | `metadata/*.yaml`, `metadata/apply-records/`                                                                          |
| P4    | [`headless-ghidra-batch-decompile`](../headless-ghidra-batch-decompile/SKILL.md) | Substitute selected functions through metadata application and Ghidra decompilation | `substitution/next-batch.yaml`, `substitution/functions/<fn_id>/`                                                     |

## Shared Artifact Contract

All phases work inside this active workspace layout:

```text
targets/<target-id>/ghidra-projects/

artifacts/<target-id>/
├── pipeline-state.yaml
├── scope.yaml
├── intake/
├── baseline/
├── runtime/
│   ├── project/
│   ├── fixtures/
│   ├── run-manifest.yaml
│   ├── run-records/
│   └── hotpaths/call-chain.yaml
├── third-party/
│   ├── identified.yaml
│   ├── pristine/<library>@<version>/
│   └── compat/<library>@<version>/
├── metadata/
│   ├── renames.yaml
│   ├── signatures.yaml
│   ├── types.yaml
│   ├── constants.yaml
│   ├── strings.yaml
│   └── apply-records/
├── substitution/
│   ├── template/
│   ├── next-batch.yaml
│   └── functions/<fn_id>/
└── gates/
```

The orchestrator treats `pipeline-state.yaml` as the current target-level state
record and relies on the phase-owned YAML artifacts above for hand-offs.

## Orchestrator Responsibilities

1. Detect or resume the active target.
2. Read `artifacts/<target-id>/pipeline-state.yaml`.
3. Dispatch the correct phase skill for the current stage.
4. Run `ghidra-agent-cli gate check --phase ...` at each transition.
5. Advance phase state only after the gate passes.
6. Handle user dialogs such as resume/restart, optional Frida supplementation,
   batch confirmation, divergence review, and completion.

## Gate Policy

- P0–P4 are the only primary pipeline transitions.
- `ghidra-agent-cli gate check` is the authoritative gate validation for all
  pipeline phases (P0–P4). The legacy `gate-check.sh` has been removed.

## Required ghidra-agent-cli Commands

- `ghidra-agent-cli context use`
- `ghidra-agent-cli context show`
- `ghidra-agent-cli context clear`
- `ghidra-agent-cli workspace state show`
- `ghidra-agent-cli workspace state set-phase`
- `ghidra-agent-cli gate check`
- `ghidra-agent-cli validate`
- `ghidra-agent-cli progress compute-next-batch`
- `ghidra-agent-cli progress show`

## Strict Prohibitions

- Must not execute analysis work itself.
- Must not edit baseline, evidence, decompilation, or verification artifacts
  directly except for explicit state updates it owns.
- Must not bypass `ghidra-agent-cli` for supported state, progress, context,
  validation, or gate operations.
- Must not accept alternate decompilation backends in place of Ghidra.
- Must not create git commits automatically.
- Must not create or run new Ghidra scripts when the CLI lacks a capability;
  pause and ask the user first.

## Next Skill Routing

- P0 complete → `headless-ghidra-baseline`
- P1 complete → `headless-ghidra-evidence`
- P2 complete → `headless-ghidra-discovery`
- P3 complete → `headless-ghidra-batch-decompile`
- P4 complete for all selected functions → either loop back to P3 or finish

## Independent Skills

The following skills operate outside the P0–P4 pipeline and can be invoked directly:

| Skill | Purpose | Invocation |
| ----- | ------ | --------- |
| [`headless-ghidra-analyze-function`](../headless-ghidra-analyze-function/SKILL.md) | Thorough single-function analysis following the strict five-step recovery order: types → constants → vtables → function identity → decompilation. Use when you need complete analysis of one specific function with full type/constant/vtable context before decompilation results are interpreted. | Invoked when user asks to "analyze this function thoroughly", "decompile function at 0x... with full context", or requests complete per-function analysis. |
