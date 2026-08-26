# Ghidra Script Authoring

Use this guide when the existing `ghidra-agent-cli` commands cannot export or
apply the evidence needed for a target, and a reusable headless Ghidra script is
being considered. Most analyses should not need new scripts; prefer the CLI
surface whenever it already supports the operation.

## Goal

A script is acceptable only when it keeps the workflow:

- headless-only
- evidence-backed
- replayable from explicit inputs
- clear about every output it writes
- honest about unavailable data

Do not add a script just because it is convenient for one local run.

## First Choice: Reuse The CLI

Before writing a script, check whether the task is already covered by:

- `ghidra-agent-cli ghidra export-baseline`
- `ghidra-agent-cli ghidra analyze-vtables`
- `ghidra-agent-cli ghidra apply-renames`
- `ghidra-agent-cli ghidra apply-signatures`
- `ghidra-agent-cli ghidra import-types-and-signatures`
- `ghidra-agent-cli ghidra decompile`
- baseline readers such as `functions`, `callgraph`, `strings`, `types`, and
  `imports`

If the CLI supports the task, use the CLI. If it does not, document the missing
capability before creating a new script.

## Script Categories

| Category                | Purpose                                                 | Side effects                  |
| ----------------------- | ------------------------------------------------------- | ----------------------------- |
| `analysis_export`       | Export reviewable evidence from the current program.    | Writes artifact files only.   |
| `verification_audit`    | Check whether a prior claim still holds.                | Read-only or report-only.     |
| `metadata_updating`     | Apply justified names, signatures, or type information. | Mutates Ghidra metadata.      |
| `orchestration_wrapper` | Coordinate explicit inputs and outputs across steps.    | May create workspace outputs. |

Use the weakest side-effect class that solves the problem.

## Reuse, Extend, Replace, Or Create

Reuse an existing script when the purpose, inputs, output family, and side
effects already match.

Extend a script when the new behavior is a small deterministic addition to the
same evidence family.

Replace a script only when the old and new scripts have the same role and the
usage documentation can move with the replacement.

Create a new script only when the task has a distinct purpose or output
contract that would be misleading inside an existing script.

Keep a helper target-specific when it depends on one binary, one address set,
one local fixture, or assumptions that are not ready for reuse.

## Where Files Belong

- Reusable Ghidra scripts: `headless-ghidra/ghidra-scripts/`
- Reusable orchestration helpers: `headless-ghidra/scripts/`
- Examples and playbooks: `headless-ghidra/examples/`
- Live target outputs: `artifacts/<target-id>/`
- Disposable generated helpers: a workspace-local scratch path, not the
  installed skill directory

Never use files under `headless-ghidra/examples/artifacts/` as live output
destinations; they are examples.

## Naming Rules

Choose names that describe the durable purpose.

Good:

- `ExportAnalysisArtifacts.java`
- `ApplyRenames.java`
- `VerifyRenames.java`

Avoid:

- `test.py`
- `tmp_export.py`
- `new_script_final2.py`

## Required Script Contract

Every reusable script should document:

```text
Purpose:
Inputs:
Outputs:
Side Effects: read-only | export-only | metadata-updating | mixed-wrapper
Replay Notes:
Failure Behavior:
```

The contract must answer:

- Which target or program state does it read?
- Which files can it write?
- Does it mutate names, prototypes, types, symbols, or analysis state?
- How does a user rerun it?
- What does it do when evidence is missing?

## Output Rules

Outputs should be:

- deterministic
- placed under the target artifact directory or another explicit workspace path
- reviewable as YAML, Markdown, JSON, or another text format
- stable enough to compare across reruns

Each output should say what was inspected, what was found, what remains
ambiguous, and what the next analyst action should be.

## Metadata-Updating Scripts

Scripts that rename functions, refine prototypes, update types, or add symbols
need stricter records:

- the input manifest must be reviewable
- each meaningful mutation needs evidence
- apply and verify reports should be written separately
- failures should leave a report rather than silently changing the project

For rename and signature work, prefer the existing CLI apply/verify commands.

## Archive Helpers

Archive helpers should:

- record archive path and archive id
- expose member output and review output paths
- preserve duplicate-member visibility
- hand off only accepted extracted members
- reject unsupported selection policies instead of widening scope silently

Archive helpers must not pass the raw archive itself to later Ghidra analysis
when the review requires member-level targets.

## Program Data Access

Reusable scripts may inspect:

- functions and entry points
- symbols and namespaces
- strings and constants
- imports, exports, and external libraries
- types, structs, enums, and pointers
- references, xrefs, and call relationships
- decompiler results, when the local workflow has validated that use

Rules:

- Do not assume stripped binaries expose every category.
- Report missing categories honestly.
- Do not overstate confidence for decompiler or type-recovery output.

## Before Treating A Script As Supported

1. Give it a durable name.
2. Define its purpose, inputs, outputs, side effects, and failure behavior.
3. Document how to invoke it headlessly.
4. Point outputs at explicit artifact paths.
5. Review it with `ghidra-script-review-checklist.md`.
6. Run it locally against the target or state clearly that validation is still
   pending.

## Avoid Fabricated Runtime Claims

Do not write:

- "this flag works on all installs"
- "the decompiler always returns usable output"
- "the help text is as follows"

Prefer:

- "run help from the discovered local Ghidra installation"
- "record the exact command and output in the target artifact"
- "if the local run differs, update the target notes before relying on it"

## Minimal Review Questions

- Is this already covered by a CLI command?
- Are all replay inputs explicit?
- Are outputs deterministic and reviewable?
- Does it avoid GUI-only assumptions?
- Is the side-effect class clear?
- Does live output stay under the target workspace?
- Does the documentation distinguish validated behavior from planned behavior?
