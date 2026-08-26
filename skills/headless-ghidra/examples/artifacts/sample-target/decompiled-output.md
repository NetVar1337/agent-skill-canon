# Decompiled Output: `sample-target`

## Status

- Artifact state: Implemented selected-decompilation schema
- Validated now:
  - this artifact is explicitly blocked during `Baseline Evidence`
  - the required selected-function and compare-trace fields are defined
  - the companion decompilation record must declare Ghidra-only provenance
- Pending local verification:
  - any actual decompiled body
  - any selected-function export for `sample-target`
  - any hybrid compare result for `sample-target`

## Stage Gate

- `Baseline Evidence`: decompiled bodies must be absent
- `Selected Decompilation And Incremental Compare`: allowed only after role,
  candidate name, and candidate prototype evidence has been recorded and the
  current step has a runnable compare boundary

## Required Entry Fields

| Field                     | Required | Notes                                                   |
| ------------------------- | -------- | ------------------------------------------------------- |
| `function_identity`       | Yes      | Function name, address, or `name@address`.              |
| `outer_to_inner_order`    | Yes      | Ordered from outermost reviewed function inward.        |
| `selection_reason`        | Yes      | Why this function was chosen now.                       |
| `role_evidence`           | Yes      | Evidence supporting the function's likely role.         |
| `name_evidence`           | Yes      | Evidence supporting the candidate name.                 |
| `prototype_evidence`      | Yes      | Evidence supporting the candidate prototype.            |
| `replacement_boundary`    | Yes      | Which function boundary is replaced in this step.       |
| `fallback_strategy`       | Yes      | How unresolved calls route back to the original target. |
| `compare_case_id`         | Yes      | Matching entry in `comparison-command-log.md`.          |
| `comparison_result`       | Yes      | `matched`, `diverged`, or `blocked`.                    |
| `behavioral_diff_summary` | Yes      | Output, return-code, or trace comparison summary.       |
| `confidence`              | Yes      | `low`, `medium`, or `high`.                             |
| `open_questions`          | Yes      | Remaining uncertainty after the decompilation review.   |

## Template

````text
### Function 1: pending_local_verification

- function_identity: pending_local_verification
- outer_to_inner_order: 1
- selection_reason: pending_local_verification
- role_evidence: pending_local_verification
- name_evidence: pending_local_verification
- prototype_evidence: pending_local_verification
- replacement_boundary: pending_local_verification
- fallback_strategy: pending_local_verification
- compare_case_id: pending_local_verification
- comparison_result: pending_local_verification
- behavioral_diff_summary: pending_local_verification
- confidence: pending_local_verification
- open_questions: pending_local_verification

```c
/* Selected decompilation pending local verification. */
```
````

## Review Rules

- Do not add a function here unless its role, candidate name, and candidate
  prototype are already evidence-backed.
- Do not batch-export exploratory decompilation.
- Do not mark a step complete unless the matching compare case has been
  recorded in `comparison-command-log.md`.
- The companion `decompilation-record.yaml` must declare
  `decompilation_backend: ghidra_headless` and
  `decompilation_action: decompile-selected`.
- Reject entries derived from `objdump`, `otool`, `llvm-objdump`, `nm`,
  `readelf`, `gdb`, `lldb`, `radare2`, or other non-Ghidra disassembly tools.
- Keep function order aligned with the outside-in traversal queue in
  `reconstruction-log.md`.

## Current Local Observation

No decompiled functions were exported during this pass because the sample still
lacks a locally replayed compare case and remains at an unvalidated runtime
state for incremental replacement.
