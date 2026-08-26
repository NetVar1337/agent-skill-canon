# Headless Ghidra Orchestrator

`headless-ghidra` is the entry skill for the Headless Ghidra skill family. Use
it when you want an agent to run the full YAML-first reverse-engineering
pipeline and pause at each phase gate for review.

Translations: [简体中文](./README.zh-CN.md) | [日本語](./README.ja-JP.md)

Install the full skill family from the [top-level README](../README.md) before
using this skill.

## What It Does

- Detects or resumes the active target.
- Reads `artifacts/<target-id>/pipeline-state.yaml`.
- Routes work to the P0-P4 phase skills.
- Runs gate checks before moving to the next phase.
- Keeps user-facing decisions explicit: resume or restart, optional runtime
  evidence, batch selection, divergence review, and completion.

It does not perform Ghidra analysis itself. The phase skills and the bundled
`ghidra-agent-cli` do the concrete work.

## Typical Requests

```text
Use headless-ghidra to analyze ./sample-target from P0. Stop after each phase
gate and show me the artifacts to review.
```

```text
Resume the existing target and continue to the next valid phase. Show the
current pipeline state first.
```

```text
Run the next P3/P4 iteration for the selected hotpath functions. If metadata is
missing, return to P3 instead of decompiling.
```

## Phase Map

| Phase | Skill README                                                    | Purpose                                                                     |
| ----- | --------------------------------------------------------------- | --------------------------------------------------------------------------- |
| P0    | [Intake](../headless-ghidra-intake/README.md)                   | Confirm the target, initialize the workspace, and set scope.                |
| P1    | [Baseline](../headless-ghidra-baseline/README.md)               | Import into Ghidra, export baseline artifacts, and record runtime evidence. |
| P2    | [Evidence](../headless-ghidra-evidence/README.md)               | Identify third-party code and evidence sources.                             |
| P3    | [Discovery](../headless-ghidra-discovery/README.md)             | Enrich names, signatures, types, constants, and strings.                    |
| P4    | [Batch Decompile](../headless-ghidra-batch-decompile/README.md) | Apply metadata and decompile selected functions.                            |

## Review Points

- After P0: target identity, binary path, Ghidra discovery, and scope.
- After P1: baseline exports, runtime status, and hotpath evidence.
- After P2: third-party decisions, source evidence, and function
  classifications.
- After P3: proposed names, signatures, types, and Ghidra apply verification.
- After P4: per-function decompilation capture, substitution record, and
  comparison result.

Runtime output belongs in the active workspace under `targets/<target-id>/` and
`artifacts/<target-id>/`.
