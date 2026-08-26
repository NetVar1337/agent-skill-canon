# Third-Party Difference Record: `sample-target`

## Purpose

This sample shows the formal difference surface that begins only after a
reviewable upstream reference exists. The current happy path is runtime-
validated against `third_party/upstream/zlib/` and the locally reviewed
`minigzip` binary.

## Upstream Reference Gate

- Upstream record:
  [`upstream-reference.md`](./upstream-reference.md)
- Required status before this artifact exists: `accepted` or `qualified`
- Current sample status: `accepted`
- Current reference path: `third_party/upstream/zlib/`
- Reviewer caveat: the review reference is a reviewed subset, so deeper
  library-internal claims should expand the reference before they are treated
  as fully inherited or modified.

## Evidence Index

| Anchor  | Evidence                                                                 | Why It Matters                                                                     |
| ------- | ------------------------------------------------------------------------ | ---------------------------------------------------------------------------------- |
| `SC-E1` | `.work/ghidra-artifacts/zlib-minigzip-20260329/function-names.md`        | Records named helper functions recovered from the validated binary.                |
| `SC-E2` | `.work/ghidra-artifacts/zlib-minigzip-20260329/strings-and-constants.md` | Records strings and symbol names observed in the validated binary.                 |
| `SC-E3` | `.work/ghidra-artifacts/zlib-minigzip-20260329/imports-and-libraries.md` | Records the imported `gz*` API family and linked `libz` path.                      |
| `SC-E4` | `third_party/upstream/zlib/test/minigzip.c`                              | Contains the reviewed helper functions and user-facing strings used by the binary. |
| `SC-E5` | `third_party/upstream/zlib/zlib.h`                                       | Contains the reviewed `gz*` API reference used by the binary.                      |
| `SC-E6` | `third_party/upstream/zlib/REFERENCE.md`                                 | Records the provenance and scope of the review reference subset.                   |

## Inherited Findings

| finding_id      | scope_area                           | claim_summary                                                                                                                                                                                                    | supporting_evidence                | reviewer_confidence | follow_up_question |
| --------------- | ------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- | ------------------- | ------------------ |
| `inherited-001` | `minigzip user-facing error strings` | The binary string `%s: can't gzopen %s` matches the reviewed `fprintf(stderr, "%s: can't gzopen %s\\n", ...)` call sites in `test/minigzip.c`.                                                                   | `SC-E2`, `SC-E4`                   | `high`              | none               |
| `inherited-002` | `minigzip helper function layout`    | The recovered helper names `_gz_compress`, `_gz_uncompress`, `_file_compress`, `_file_uncompress`, and `_error` align with the reviewed source declarations and definitions in `test/minigzip.c`.                | `SC-E1`, `SC-E4`                   | `high`              | none               |
| `inherited-003` | `zlib API usage`                     | The binary imports `_gzopen`, `_gzread`, `_gzwrite`, `_gzclose`, `_gzdopen`, and `_gzerror` from `@rpath/libz.1.dylib`, matching the reviewed call sites in `test/minigzip.c` and the API reference in `zlib.h`. | `SC-E2`, `SC-E3`, `SC-E4`, `SC-E5` | `high`              | none               |

## Modified Findings

No concrete modified findings were observed in this runtime validation pass.

## Unresolved Findings

| finding_id       | scope_area                         | claim_summary                                                                                                                                                                                                             | supporting_evidence                | reviewer_confidence | follow_up_question                                                                                                |
| ---------------- | ---------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------- | ------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `unresolved-001` | `linked libz implementation depth` | This pass validates the `minigzip` entry program and the reviewed `gz*` API surface, but it does not yet claim that every internal `libz` implementation detail has been compared against the full upstream library tree. | `SC-E3`, `SC-E4`, `SC-E5`, `SC-E6` | `medium`            | Should the review reference expand beyond `minigzip.c` and `zlib.h` before making deeper library-internal claims? |

## Reviewer Rules

- Do not classify behavior as inherited without concrete evidence from both the
  target and the reviewable upstream reference.
- Keep `modified_findings` explicit even when only a small divergence is known.
- Keep `unresolved_findings` explicit when the workflow still lacks enough
  evidence to classify the relationship confidently.
- If the upstream reference becomes `deferred` or `stale`, stop treating this
  artifact as sufficient for source-derived claims and move the gate back to
  `blocked`.
