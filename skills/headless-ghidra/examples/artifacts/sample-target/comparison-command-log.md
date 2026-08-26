# Comparison Command Log: `sample-target`

## Status

- Artifact state: Implemented reviewable reproducibility surface for incremental
  compare
- Validated now:
  - each selected decompilation step has a place to record how the original and
    hybrid targets were built, run, and compared
  - executable interposition and library-harness fallback both have named
    review fields
- Pending local verification:
  - any concrete compare case for `sample-target`

## Required Fields

| Field                  | Required | Notes                                                                       |
| ---------------------- | -------- | --------------------------------------------------------------------------- |
| `compare_case_id`      | Yes      | Stable step id such as `step-01-outer-dispatch`.                            |
| `selected_function`    | Yes      | Function identity for the current replacement boundary.                     |
| `replacement_boundary` | Yes      | Which recovered function or wrapper is replaced in this step.               |
| `fallback_strategy`    | Yes      | `original_address_bridge`, `original_library_handle`, or `none`.            |
| `original_target`      | Yes      | Original binary or library path used as the compare baseline.               |
| `hybrid_entrypoint`    | Yes      | Injected function boundary or generated harness entrypoint.                 |
| `build_command`        | Yes      | Exact command used to build the hybrid artifact.                            |
| `original_run_command` | Yes      | Exact command used to run the untouched original target.                    |
| `hybrid_run_command`   | Yes      | Exact command used to run the hybrid target.                                |
| `comparison_artifacts` | Yes      | Runtime artifact paths for stdout, stderr, traces, or return-code captures. |
| `compare_status`       | Yes      | `matched`, `diverged`, or `blocked`.                                        |
| `diff_summary`         | Yes      | Concise explanation of the observed match or mismatch.                      |
| `next_gate`            | Yes      | `move_inward`, `repair`, or `stop`.                                         |

## Template

| Compare Case Id   | Selected Function          | Replacement Boundary                      | Fallback Strategy                                      | Original Target     | Hybrid Entrypoint            | Build Command                | Original Run Command         | Hybrid Run Command           | Comparison Artifacts                                         | Compare Status | Diff Summary                 | Next Gate |
| ----------------- | -------------------------- | ----------------------------------------- | ------------------------------------------------------ | ------------------- | ---------------------------- | ---------------------------- | ---------------------------- | ---------------------------- | ------------------------------------------------------------ | -------------- | ---------------------------- | --------- |
| `step-01-pending` | `pending_function@address` | `replace only the current outer boundary` | `original_address_bridge` or `original_library_handle` | `/path/to/original` | `pending_local_verification` | `pending_local_verification` | `pending_local_verification` | `pending_local_verification` | `.work/ghidra-artifacts/<target-id>/compare-runs/<case-id>/` | `blocked`      | `pending_local_verification` | `repair`  |

## Review Rules

- Do not mark a selected decompilation step complete until the matching compare
  case here is `matched` or the deviation is explicitly accepted in
  `reconstruction-log.md`.
- For executable targets, unresolved callees must route back into the original
  binary through reviewed addresses, trampolines, or bridge stubs.
- For static or dynamic library targets, unresolved calls must route through an
  opened handle to the original library.
- Store runtime logs, traces, and compare outputs under
  `.work/ghidra-artifacts/<target-id>/compare-runs/`.

## Current Local Observation

This example file defines the required compare record, but no concrete
compare case has been replayed yet for `sample-target`.
