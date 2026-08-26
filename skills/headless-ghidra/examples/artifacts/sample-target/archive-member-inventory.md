# Archive Member Inventory: `sample-target`

## Status

- Artifact state: Tracked sample archive member inventory derived from the
  local happy-path replay on 2026-03-30 plus one reviewed failed-member example
- Wrapper surface: `normalize-ar-archive.sh`
- Status vocabulary: `accepted`, `deferred`, `unsupported`, `failed`
- Current outcome summary:
  - accepted: `1`
  - deferred: `2`
  - unsupported: `1`
  - failed: `1`

## Member Inventory

| Member Id                       | Member Name      | Member Kind         | Member Status | Normalized Target Id            | Collision Key    | Extracted Runtime Path                                                                                  | Reason                                                                                                         | Architecture Notes           |
| ------------------------------- | ---------------- | ------------------- | ------------- | ------------------------------- | ---------------- | ------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------- | ---------------------------- |
| `sample-target--symdef`         | `__.SYMDEF`      | `metadata_only`     | `unsupported` | `not_applicable`                | `not_applicable` | `not_applicable`                                                                                        | metadata member is not a downstream Ghidra import target                                                       | `not_applicable`             |
| `sample-target--archive-main-o` | `archive_main.o` | `importable_object` | `accepted`    | `sample-target--archive-main-o` | `not_applicable` | `.work/ghidra-artifacts/sample-target-archive-runtime/normalized-members/sample-target--archive_main.o` | member classified as an importable object and selected for downstream intake                                   | `Mach-O 64-bit object arm64` |
| `sample-target--common-o-dup01` | `common.o`       | `unknown`           | `deferred`    | `not_applicable`                | `dup01`          | `not_applicable`                                                                                        | duplicate member name requires explicit collision review before deterministic extraction proceeds              | `not_applicable`             |
| `sample-target--common-o-dup02` | `common.o`       | `unknown`           | `deferred`    | `not_applicable`                | `dup02`          | `not_applicable`                                                                                        | duplicate member name requires explicit collision review before deterministic extraction proceeds              | `not_applicable`             |
| `sample-target--faulty-thunk-o` | `faulty_thunk.o` | `unknown`           | `failed`      | `not_applicable`                | `not_applicable` | `not_applicable`                                                                                        | sample failure row: extractor stderr must remain reviewable when a member does not yield a stable runtime file | `not_applicable`             |

## Rules

- Duplicate member names must not silently overwrite one another.
- `accepted` members become the only candidates for downstream Ghidra intake.
- `deferred`, `unsupported`, and `failed` members remain visible here even when
  the archive-level workflow continues.
- Runtime-generated extracted members live under
  `.work/ghidra-artifacts/sample-target-archive-runtime/normalized-members/`,
  never under the installed skill directory.
- The failed sample row documents the required review posture for extraction
  failures even when the local happy-path replay completed without a failed
  member.
