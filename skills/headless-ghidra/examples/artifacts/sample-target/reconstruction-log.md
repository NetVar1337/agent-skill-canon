# Reconstruction Log: `sample-target`

## Status

- Artifact state: Implemented stage-gated decision log
- Validated now:
  - this artifact records evidence review, target selection, source-comparison
    checkpoints, mutation decisions, outside-in traversal state, and per-step
    compare gating
- Pending local verification:
  - any concrete reconstruction or compare records for `sample-target`

## Canonical Stage Path

1. `Baseline Evidence`
2. `Evidence Review`
3. `Target Selection`
4. `Source Comparison`
5. `Semantic Reconstruction`
6. `Selected Decompilation And Incremental Compare`

## Evidence Snapshot Template

| Stage             | Available Categories                                       | Missing Categories         | Anchor Summary             | Review Notes               |
| ----------------- | ---------------------------------------------------------- | -------------------------- | -------------------------- | -------------------------- |
| `Evidence Review` | imports, strings, candidate functions, xrefs, source clues | pending_local_verification | pending_local_verification | pending_local_verification |

## Selection Decision Log

| Stage              | Selected Target              | Triggering Evidence          | Hypothesis                   | Question To Answer           | Deviation Reason                       | Deviation Risk                         | Replacement Boundary                               | Fallback Strategy                                      | Status                     |
| ------------------ | ---------------------------- | ---------------------------- | ---------------------------- | ---------------------------- | -------------------------------------- | -------------------------------------- | -------------------------------------------------- | ------------------------------------------------------ | -------------------------- |
| `Target Selection` | `pending_local_verification` | `pending_local_verification` | `pending_local_verification` | `pending_local_verification` | `none` or `pending_local_verification` | `none` or `pending_local_verification` | `replace one reviewed function boundary at a time` | `original_address_bridge` or `original_library_handle` | blocked / ready / complete |

Rules:

- Every deeper step needs a new or updated selection record.
- `triggering_evidence` must cite reviewed artifacts or explicit observed facts.
- `deviation_reason` is required whenever outside-in order is intentionally
  broken.
- `replacement_boundary` must stay scoped to the current incremental step.
- `fallback_strategy` must explain how unresolved calls return to the original
  target.

## Source Comparison Checkpoints

| Project Slug                 | Probable Version             | Reference Mode                       | Reference Path               | Fallback Reason      | Comparison State                 |
| ---------------------------- | ---------------------------- | ------------------------------------ | ---------------------------- | -------------------- | -------------------------------- |
| `pending_local_verification` | `pending_local_verification` | `submodule` / `local_clone_fallback` | `pending_local_verification` | `not_applicable_yet` | blocked / in_progress / complete |

## Mutation Decision Log

| Item Kind                                    | Target Name                  | Prior Evidence               | Change Summary               | Confidence          | Linked Selection             | Replacement Boundary         | Fallback Strategy            | Status                     |
| -------------------------------------------- | ---------------------------- | ---------------------------- | ---------------------------- | ------------------- | ---------------------------- | ---------------------------- | ---------------------------- | -------------------------- |
| function / prototype / type / field / vtable | `pending_local_verification` | `pending_local_verification` | `pending_local_verification` | low / medium / high | `pending_local_verification` | `pending_local_verification` | `pending_local_verification` | blocked / ready / complete |

Rules:

- Mutation is blocked until prior evidence has been reviewed.
- Every mutation must link back to the selection that justified it.
- Mutation rows that feed selected decompilation must keep
  `replacement_boundary` and `fallback_strategy` synchronized with the current
  compare step.
- Use `renaming-log.md` for the stable mutation record and keep this file as the
  chronological control log.

## Incremental Compare Log

| Order | Function Identity            | Replacement Boundary                      | Fallback Strategy                                      | Compare Case Id   | Original Target              | Hybrid Entrypoint            | Compare Status                                             | Can Authorize Children | Diff Summary                 | Next Gate                   |
| ----- | ---------------------------- | ----------------------------------------- | ------------------------------------------------------ | ----------------- | ---------------------------- | ---------------------------- | ---------------------------------------------------------- | ---------------------- | ---------------------------- | --------------------------- |
| `1`   | `pending_local_verification` | `replace only the current outer boundary` | `original_address_bridge` or `original_library_handle` | `step-01-pending` | `pending_local_verification` | `pending_local_verification` | blocked / matched / unresolved / diverged / deviation_only | true / false           | `pending_local_verification` | move_inward / repair / stop |

Rules:

- Do not advance the outside-in queue until the current compare row is
  explicitly `matched`.
- Use `comparison-command-log.md` to store the exact build and run commands that
  produced this row.
- `Can Authorize Children` must stay `false` for `blocked`, `unresolved`,
  `diverged`, and `deviation_only`.
- For executable targets, `fallback_strategy` should name the reviewed address
  bridge or trampoline mechanism.
- For library targets, `fallback_strategy` should name the generated harness
  entrypoint and original-library handle.

## Outside-In Traversal Queue

| Order | Function Identity            | Frontier Basis            | Relationship Type                     | Why This Is Next                        | Blocked If                                                                                    |
| ----- | ---------------------------- | ------------------------- | ------------------------------------- | --------------------------------------- | --------------------------------------------------------------------------------------------- |
| `1`   | `pending_local_verification` | outermost_anchor          | entry_adjacent / dispatch_edge        | current outermost reviewed function     | role, name, prototype, or frontier evidence missing                                           |
| `2`   | `pending_local_verification` | child_of_matched_boundary | callee / dispatch_edge / wrapper_edge | child of the current `matched` boundary | verified parent boundary missing, compare row not `matched`, or fallback bridge still missing |

## Defer And Block Rules

Record an explicit blocked state when:

- no local Ghidra install is available
- local help retrieval has not been validated
- upstream project or version remains unresolved
- the workflow lacks enough evidence for role, name, or prototype hypotheses
- the next requested function would skip the outside-in order without a
  recorded `deviation_reason` and `deviation_risk`
- the current replacement step cannot yet run against the original target
- unresolved callees do not yet have a reviewed path back into the original
  binary or library

## Current Local Observation

No concrete reconstruction or compare entries were added during this pass
because incremental replacement still lacks a locally replayed runnable compare
case.
