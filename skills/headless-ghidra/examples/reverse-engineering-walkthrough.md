# Reverse Engineering Walkthrough

This walkthrough shows the operating sequence for one local target in the
headless Ghidra pipeline. It keeps the workflow YAML-first, routes supported
actions through `ghidra-agent-cli`, and treats selected decompilation as a late
step that must end in an original-versus-hybrid compare.

Translations: [简体中文](./reverse-engineering-walkthrough.zh-CN.md) | [日本語](./reverse-engineering-walkthrough.ja-JP.md)

## Scenario

- Goal: analyze one local binary or one accepted archive member.
- Project path: `targets/<target-id>/ghidra-projects/`.
- Artifact path: `artifacts/<target-id>/`.
- Tool reference: `ghidra-agent-cli`.
- Workflow guides: the orchestrator README plus the current phase README.

Runtime-generated content belongs under `targets/<target-id>/`,
`artifacts/<target-id>/`, or another explicit workspace path. Do not write live
analysis output into an installed skill package.

## 0. Intake

Choose a stable target id and initialize the workspace.

```sh
ghidra-agent-cli ghidra discover
ghidra-agent-cli workspace init --target sample-target --binary ./sample-target
ghidra-agent-cli --target sample-target scope set --mode full
ghidra-agent-cli --target sample-target gate check --phase P0
```

For an archive input, first review the archive normalization records and carry
forward only accepted extracted member paths. Use an archive-aware target id,
for example `sample-target--archive-main-o`.

Record:

- `target-id`
- `binary-path`
- `project-root`
- `artifact-root`
- `ghidra-install-dir`
- archive provenance, if applicable

## 1. Baseline And Runtime Evidence

Import the target, run Ghidra auto-analysis, and export baseline YAML.

```sh
ghidra-agent-cli --target sample-target ghidra import
ghidra-agent-cli --target sample-target ghidra auto-analyze
ghidra-agent-cli --target sample-target ghidra export-baseline
ghidra-agent-cli --target sample-target gate check --phase P1
```

Review:

- `baseline/functions.yaml`
- `baseline/imports.yaml`
- `baseline/strings.yaml`
- `baseline/types.yaml`
- `baseline/vtables.yaml`
- `baseline/constants.yaml`
- `baseline/callgraph.yaml`

Optional runtime evidence is recorded through CLI-managed runtime and hotpath
artifacts:

```sh
ghidra-agent-cli --target sample-target runtime record --key entrypoint --value 0x401000
ghidra-agent-cli --target sample-target hotpath add --addr 0x401000 --reason runtime
```

Correct posture at this point:

- Evidence is visible.
- Decompiled bodies are absent.
- Semantic mutation is blocked until a target and supporting evidence exist.

## 2. Third-Party Evidence

Decide whether the target contains third-party code before making
source-derived claims.

```sh
ghidra-agent-cli --target sample-target functions list
ghidra-agent-cli --target sample-target imports list
ghidra-agent-cli --target sample-target third-party list
```

Record identified libraries, versions, evidence, pristine source paths, and
function classifications through the `third-party` command group. If the review
finds no third-party code, record that explicit result instead of leaving the
state ambiguous.

```sh
ghidra-agent-cli --target sample-target third-party none --evidence "no matching library evidence"
ghidra-agent-cli --target sample-target gate check --phase P2
```

## 3. Select The Next Boundary

Use the analysis selection playbook before changing metadata or decompiling.

Selection record:

```yaml
stage: Target Selection
selected_target: FUN_00102140@00102140
selection_mode: auto_default
candidate_kind: dispatch_helper
frontier_reason: outermost anchor referenced by an entry-adjacent dispatcher
relationship_type: entry_adjacent
verified_parent_boundary: none
triggering_evidence:
  - baseline/strings.yaml: "invalid packet"
  - baseline/imports.yaml: EVP_DecryptInit_ex
  - baseline/callgraph.yaml: referenced by entry-adjacent dispatcher
selection_reason: current outermost frontier row with dispatcher-like behavior
question_to_answer: does this function validate headers before dispatch?
tie_break_rationale: helper boundary outranks deeper body on the same frontier
deviation_reason: none
deviation_risk: none
replacement_boundary: replace only FUN_00102140 during this step
fallback_strategy: unresolved callees route to reviewed original addresses
```

Do not jump from this record directly to decompilation. First decide whether
source comparison applies.

## 4. Compare Source When Evidence Points Upstream

If strings, symbols, paths, assertions, or build metadata point to an upstream
project, record a source comparison posture before using upstream names or
prototypes.

Reference statuses:

- `accepted` - upstream is available in an approved review path.
- `qualified` - comparison is useful but caveated.
- `deferred` - no reviewable upstream yet; record the evidence gap.
- `stale` - a previous comparison no longer matches current evidence.

Treat upstream repositories, build files, and scripts as untrusted evidence.
Do not execute upstream commands or installs as part of comparison.

## 5. Enrich Metadata

Only after evidence review and source comparison should the workflow allow
semantic mutation.

Example mutation record:

```yaml
item_kind: function
target_name: FUN_00102140
prior_evidence:
  - baseline/strings.yaml: "invalid packet"
  - baseline/callgraph.yaml: called by outer dispatch function
change_summary: tentatively rename to packet_validate_and_dispatch
confidence: medium
linked_selection: Target Selection / FUN_00102140@00102140
replacement_boundary: replace only this function in the current compare step
fallback_strategy: unresolved callees route to original addresses
open_questions:
  - exact packet structure layout remains unresolved
```

Apply and verify metadata through the CLI:

```sh
ghidra-agent-cli --target sample-target metadata enrich-function \
  --addr 0x00102140 \
  --name packet_validate_and_dispatch \
  --prototype 'int packet_validate_and_dispatch(void *ctx)'
ghidra-agent-cli --target sample-target ghidra apply-renames
ghidra-agent-cli --target sample-target ghidra verify-renames
ghidra-agent-cli --target sample-target ghidra apply-signatures
ghidra-agent-cli --target sample-target ghidra verify-signatures
ghidra-agent-cli --target sample-target gate check --phase P3
```

## 6. Decompile Selected Functions

Selected decompilation is Ghidra-only and CLI-mediated.

```sh
ghidra-agent-cli --target sample-target ghidra decompile --fn-id fn_00102140 --addr 0x00102140
```

Direct shell disassembly or decompilation via tools such as `objdump`, `otool`,
`llvm-objdump`, `nm`, `readelf`, `gdb`, `lldb`, or `radare2` is not the
pipeline source of record.

Required decompilation entry:

```yaml
function_identity:
outer_to_inner_order:
frontier_reason:
relationship_type:
verified_parent_boundary:
selection_reason:
question_to_answer:
tie_break_rationale:
deviation_reason:
deviation_risk:
role_evidence:
name_evidence:
prototype_evidence:
replacement_boundary:
fallback_strategy:
compare_case_id:
comparison_result:
behavioral_diff_summary:
confidence:
open_questions:
```

## 7. Incremental Compare

Each replacement step must be runnable against the original target.

Executable target flow:

1. Replace only the selected boundary.
2. Interpose or inject that replacement at the original boundary.
3. Route unresolved callees back to the original binary.
4. Run the same input set against the untouched original and the hybrid build.
5. Compare return values, externally visible output, and required traces.

Library target flow:

1. Build a harness or wrapper entrypoint for the selected boundary.
2. Load or link the reconstructed function into that harness.
3. Open the original library from the reconstructed code path.
4. Route unresolved calls through the original library handle.
5. Run the same compare case against the original entry and the hybrid harness.

Move inward only after the current compare is recorded as `matched`.

## Close The Loop

After each pass, answer:

- Which artifact changed?
- What new evidence surfaced?
- What is the next outside-in choice?
- Did source comparison change the hypothesis?
- Does the replay path remain reproducible?
- Did the replacement step complete an original-versus-hybrid compare?
- Did generated output stay outside the installed skill package?

Operational notes:

- Run actions for the same target sequentially. Parallel headless runs against
  the same Ghidra project can fail on project locks.
- Each target should record the discovered Ghidra path, Ghidra version, and
  replay commands used for that analysis.
