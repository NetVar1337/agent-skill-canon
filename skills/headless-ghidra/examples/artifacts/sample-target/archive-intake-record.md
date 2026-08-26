# Archive Intake Record: `sample-target`

## Status

- Artifact state: Tracked sample archive-normalization intake surface derived
  from local replay on 2026-03-30
- Wrapper surface: `normalize-ar-archive.sh` as a supported
  `orchestration_wrapper`
- Direct import status: `blocked_requires_normalization`
- Overall outcome: `members_ready`
- Runtime artifact root:
  `.work/ghidra-artifacts/sample-target-archive-runtime/`
- Review surface root:
  `.work/ghidra-artifacts/sample-target-archive-runtime/review/`

## Current Review Record

| Field                      | Value                                                                                      | Review Notes                                                                            |
| -------------------------- | ------------------------------------------------------------------------------------------ | --------------------------------------------------------------------------------------- |
| `archive_id`               | `sample-target`                                                                            | Stable identifier for this normalization review set.                                    |
| `archive_path`             | `.work/ghidra-artifacts/archive-normalization-smoke-20260330/libsample.a`                  | Local smoke-test archive used to validate the wrapper surface.                          |
| `provenance_notes`         | `local_archive_input`, `reviewed_local_smoke_archive`                                      | Review the caller-provided archive path and local file metadata before deeper analysis. |
| `direct_import_status`     | `blocked_requires_normalization`                                                           | The raw archive is not treated as the downstream Ghidra program target.                 |
| `archive_observation`      | `current ar archive`                                                                       | Captured from the local `file` observation during replay.                               |
| `normalization_wrapper_id` | `normalize-ar-archive`                                                                     | Canonical wrapper entrypoint for archive normalization.                                 |
| `runtime_artifact_root`    | `.work/ghidra-artifacts/sample-target-archive-runtime/`                                    | Runtime-generated members and review outputs stay under `.work/`.                       |
| `replay_record_path`       | `<installed-skill-root>/examples/artifacts/sample-target/archive-replay-command-record.md` | Use the example replay record to rerun the same normalization pass.                     |
| `overall_status`           | `members_ready`                                                                            | Must match the handoff summary and any stop condition.                                  |

## Recognition Evidence

- `file` observation: `current ar archive`
- Extractor used for listing and extraction: `ar`
- The wrapper listed `4` archive member(s) before status classification.
- Duplicate basenames `common.o` were deferred instead of being overwritten.

## Required Follow-Up

1. Review `archive-member-inventory.md` before choosing any downstream target.
2. Review `archive-normalization-handoff.md` to confirm which member may
   proceed into the existing headless workflow.
3. Use `archive-replay-command-record.md` to reproduce the same extractor path,
   output layout, and stop-path companion run.

## Reviewer Notes

- Runtime review surfaces under
  `.work/ghidra-artifacts/sample-target-archive-runtime/review/` are live
  outputs.
- Tracked sample files under `examples/artifacts/sample-target/` are reviewed
  examples, not live output destinations.
- When `overall_status` is not `members_ready`, stop before any Ghidra import
  attempt.
