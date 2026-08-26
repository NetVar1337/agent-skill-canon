# Renaming Log: `sample-target`

## Status

- Artifact state: Implemented semantic-mutation record
- Validated now:
  - mutation is explicitly gated behind prior evidence review
  - every stable semantic change requires a linked selection
  - the Java replay surface accepts `function`, `symbol`, and `label` rows
  - runtime validation now exists for baseline, apply, verify, and selected
    decompilation against a local Ghidra 12.0.4 install
- Pending local verification:
  - any concrete rename, prototype recovery, or type recovery decision for this
    specific example target

## Mutation Schema

| Item Kind | Target Address           | Expected Current Name  | New Name           | Prior Evidence               | Change Summary               | Confidence          | Linked Selection             | Open Questions               | Status                                |
| --------- | ------------------------ | ---------------------- | ------------------ | ---------------------------- | ---------------------------- | ------------------- | ---------------------------- | ---------------------------- | ------------------------------------- |
| function  | `pending_target_address` | `pending_current_name` | `pending_new_name` | `pending_local_verification` | `pending_local_verification` | low / medium / high | `pending_local_verification` | `pending_local_verification` | blocked / ready / approved / complete |
| symbol    | `pending_target_address` | `pending_current_name` | `pending_new_name` | `pending_local_verification` | `pending_local_verification` | low / medium / high | `pending_local_verification` | `pending_local_verification` | blocked / ready / approved / complete |
| label     | `pending_target_address` | `pending_current_name` | `pending_new_name` | `pending_local_verification` | `pending_local_verification` | low / medium / high | `pending_local_verification` | `pending_local_verification` | blocked / ready / approved / complete |

## Rules

- Record semantic changes only after evidence review.
- `Target Address`, `Expected Current Name`, and `New Name` are required before
  generic rename scripts may consume a row.
- Only rows marked `ready`, `approved`, or `complete` are executable by the
  generic apply/verify rename scripts.
- The current supported Java schema accepts `Item Kind = function`,
  `Item Kind = symbol`, and `Item Kind = label`.
- `Prior Evidence` must cite reviewed artifacts, not intuition alone.
- `Linked Selection` must identify the target-selection record that made this
  mutation the next allowed step.
- If confidence is too low to keep the change stable across reruns, keep the
  mutation out of this file and record the uncertainty in `reconstruction-log.md`.

## Current Local Observation

Local replay validation on 2026-03-29 confirmed that the Java rename workflow
can apply and verify reviewable `function` and `symbol` rows against a real
Ghidra 12.0.4 headless project. `label` remains a documented alias on the same
symbol-target runtime surface and should be called out explicitly if a later
replay validates it as a separate executable sample row.

Treat this example file as reference material only. Live rename replay
should use a workspace copy under `.work/ghidra-artifacts/<target-id>/`.
