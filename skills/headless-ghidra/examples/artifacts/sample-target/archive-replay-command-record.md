# Archive Replay Command Record: `sample-target`

## Status

- Artifact state: Tracked sample replay-command surface for archive
  normalization
- Wrapper surface: `normalize-ar-archive.sh`
- Current outcome: `members_ready`
- Local replay validation on 2026-03-30 covered a happy path, a stop path, an
  invalid selection-policy rejection, and a deterministic rerun check

## Environment Contract

```bash
export WORKSPACE_ROOT=$PWD
export SKILL_ROOT=/path/to/installed/headless-ghidra
export ARCHIVE_PATH=.work/ghidra-artifacts/archive-normalization-smoke-20260330/libsample.a
export ARCHIVE_ID=sample-target
export ARCHIVE_ARTIFACT_ROOT=.work/ghidra-artifacts/sample-target-archive-runtime
export ARCHIVE_MEMBER_ROOT=$ARCHIVE_ARTIFACT_ROOT/normalized-members
export ARCHIVE_REVIEW_ROOT=$ARCHIVE_ARTIFACT_ROOT/review
export STOP_ARCHIVE_PATH=.work/ghidra-artifacts/archive-normalization-smoke-20260330/libsample-stop.a
export STOP_ARCHIVE_ID=sample-target-stop
export STOP_ARTIFACT_ROOT=.work/ghidra-artifacts/sample-target-archive-stop-runtime
```

## Exact Commands

| Field               | Value                                                                                                                                                                                                                                                                                                                        |
| ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `wrapper_command`   | `bash "$SKILL_ROOT/scripts/normalize-ar-archive.sh" --archive "$ARCHIVE_PATH" --archive-id "$ARCHIVE_ID" --workspace-root "$WORKSPACE_ROOT" --artifact-root "$ARCHIVE_ARTIFACT_ROOT" --member-output-root "$ARCHIVE_MEMBER_ROOT" --review-output-root "$ARCHIVE_REVIEW_ROOT" --selection-policy accepted-all --extractor ar` |
| `extractor_command` | `ar t "$ARCHIVE_PATH"`                                                                                                                                                                                                                                                                                                       |
| `stop_path_command` | `bash "$SKILL_ROOT/scripts/normalize-ar-archive.sh" --archive "$STOP_ARCHIVE_PATH" --archive-id "$STOP_ARCHIVE_ID" --workspace-root "$WORKSPACE_ROOT" --artifact-root "$STOP_ARTIFACT_ROOT" --selection-policy accepted-all --extractor ar`                                                                                  |

## Input Arguments

- `archive_path`: `.work/ghidra-artifacts/archive-normalization-smoke-20260330/libsample.a`
- `archive_id`: `sample-target`
- `artifact_root`: `.work/ghidra-artifacts/sample-target-archive-runtime/`
- `member_output_root`:
  `.work/ghidra-artifacts/sample-target-archive-runtime/normalized-members/`
- `review_output_root`:
  `.work/ghidra-artifacts/sample-target-archive-runtime/review/`
- `selection_policy`: `accepted-all`

## Output Paths

- `.work/ghidra-artifacts/sample-target-archive-runtime/review/archive-intake-record.md`
- `.work/ghidra-artifacts/sample-target-archive-runtime/review/archive-member-inventory.md`
- `.work/ghidra-artifacts/sample-target-archive-runtime/review/archive-normalization-handoff.md`
- `.work/ghidra-artifacts/sample-target-archive-runtime/review/archive-replay-command-record.md`
- `.work/ghidra-artifacts/sample-target-archive-runtime/normalized-members/`

## Expected Observations

- At least one normalized member target is accepted for downstream Ghidra
  intake.
- Duplicate `common.o` members remain deferred with deterministic collision
  keys `dup01` and `dup02`.
- The stop-path companion command records
  `stopped_no_eligible_members` with zero accepted downstream targets.
- Re-running the same happy-path command without input changes preserves the
  shasum of all four review surfaces.

## Determinism Check

The local replay reran the happy-path wrapper command twice on 2026-03-30 and
observed identical hashes for the four Markdown review surfaces:

```text
ccc34eae35aa41714567ddc183139431566e4f30  archive-intake-record.md
f2e4a92bf7b351d5abdf6c457a7df2b83457bbfc  archive-member-inventory.md
d63e9603aa754bf72fd7338f4ef60a1edc06bce2  archive-normalization-handoff.md
bf4f6a708a455b095839028fa4c343a7b7556e33  archive-replay-command-record.md
```

## Failure Signals

- Extractor listing fails for a non-archive input and the wrapper records
  `normalization_failed` before any downstream handoff.
- Unknown `--selection-policy` values fail fast instead of silently falling
  back to `accepted-all`.
- No member reaches `accepted`, producing a stop condition instead of a
  downstream target.
- A duplicate member name must never overwrite another extracted path without
  an explicit collision rule.

## Extraction Commands Observed

- `ar x "$ARCHIVE_PATH" archive_main.o` ->
  `.work/ghidra-artifacts/sample-target-archive-runtime/normalized-members/sample-target--archive_main.o`

## Reviewer Notes

- Replay the wrapper from a clean or clearly described workspace.
- If `ar` is not available on `PATH`, replace it with an absolute extractor
  path in the recorded commands.
- The review surfaces under
  `.work/ghidra-artifacts/sample-target-archive-runtime/review/` are live
  runtime outputs.
- The example replay record under `examples/artifacts/sample-target/` is
  a reviewed example, not the default live destination.
