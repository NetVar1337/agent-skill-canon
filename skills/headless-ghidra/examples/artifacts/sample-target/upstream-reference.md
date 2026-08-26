# Upstream Source Reference: `sample-target`

## Purpose

This sample shows the canonical source-comparison intake record. The current
worked example now points to one runtime-validated happy path using a reviewed
`zlib` source reference and a locally reviewed `minigzip` binary.

## Current Review Record

| Field              | Value                        | Review Notes                                                                                            |
| ------------------ | ---------------------------- | ------------------------------------------------------------------------------------------------------- |
| `project_slug`     | `zlib`                       | Runtime-validated against a review subset under `third_party/upstream/zlib/`.                           |
| `probable_version` | `1.2.11`                     | Derived from the reviewed `README` and `zlib.h` in the source subset.                                   |
| `reference_mode`   | `review_reference`           | The current happy path uses the preferred review surface.                                               |
| `reference_path`   | `third_party/upstream/zlib/` | The reviewed subset includes `README`, `zlib.h`, and `test/minigzip.c`.                                 |
| `reference_status` | `accepted`                   | The current review uses a reviewed source reference with concrete binary-side and source-side evidence. |
| `fallback_reason`  | `not_applicable`             | The happy path does not depend on a fallback local source tree.                                         |

## Discovery Evidence

- `third_party/upstream/zlib/REFERENCE.md` records the reviewed source subset
  copied from local `zlib` commit `cdb955ce`.
- Ghidra baseline export succeeded on 2026-03-29 for a local validation
  `minigzip` build with target id `zlib-minigzip-20260329`.
- `.work/ghidra-artifacts/zlib-minigzip-20260329/function-names.md` records
  `_gz_compress`, `_gz_uncompress`, `_file_compress`, `_file_uncompress`, and
  `_error`, which align with `third_party/upstream/zlib/test/minigzip.c`.
- `.work/ghidra-artifacts/zlib-minigzip-20260329/strings-and-constants.md`
  records `%s: can't gzopen %s`, `_gzopen`, `_gzread`, and `_gzwrite`, which
  align with `third_party/upstream/zlib/test/minigzip.c` and
  `third_party/upstream/zlib/zlib.h`.
- `.work/ghidra-artifacts/zlib-minigzip-20260329/imports-and-libraries.md`
  records `@rpath/libz.1.dylib` imports for the `gz*` API family used by the
  source reference.

## Required Follow-Up

1. Expand `third_party/upstream/zlib/` if future reviews need claims about
   deeper library internals beyond `minigzip.c` and `zlib.h`.
2. Re-run the baseline export if the validated `minigzip` binary or reviewed
   source subset changes.
3. Record any future modified findings if a rebuilt binary diverges from the
   review reference.

## Status Decision Guide

| `reference_status` | When To Use It                                                                                                                                          | Downstream Effect                                                                                         |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| `accepted`         | The probable upstream is reviewable through `third_party/upstream/<project-slug>/` and the current evidence is strong enough for normal comparison use. | Source-derived claims may become `allowed` once the difference record is current.                         |
| `qualified`        | The reference is useful but still limited by fallback sourcing, incomplete lineage confidence, or another explicit caveat.                              | Source-derived claims need explicit caveats.                                                              |
| `deferred`         | No confident upstream match is reviewable yet.                                                                                                          | Record the evidence gap and `required_follow_up`; do not imply a formal `third-party-diff.md` exists yet. |
| `stale`            | A previously reviewed path, version note, or evidence set no longer matches the current state.                                                          | Block source-derived claims until re-review.                                                              |

## Reviewer Notes

- `upstream-reference.md` is always the first required source-comparison
  artifact.
- `third-party-diff.md` begins only after this record is reviewable with status
  `accepted` or `qualified`.
- Upstream similarity is not proof of equivalence; keep local modification risk
  explicit.
