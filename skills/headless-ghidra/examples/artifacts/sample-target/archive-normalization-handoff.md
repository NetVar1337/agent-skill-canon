# Archive Normalization Handoff: `sample-target`

## Status

- Artifact state: Tracked sample downstream handoff record derived from local
  replay on 2026-03-30
- Current archive outcome: `members_ready`
- Stop condition: `not_applicable`

## Accepted Downstream Targets

- `sample-target--archive-main-o`

## Deferred Members

- `sample-target--common-o-dup01`
- `sample-target--common-o-dup02`

## Unsupported Members

- `sample-target--symdef`

## Failed Members

- `sample-target--faulty-thunk-o`

## Downstream Entry Rule

- One accepted member becomes one downstream target identity.
- The accepted runtime member path for this sample is
  `.work/ghidra-artifacts/sample-target-archive-runtime/normalized-members/sample-target--archive_main.o`.
- Start later Ghidra stages with `run-headless-analysis.sh --binary <accepted-member-path> --target-id sample-target--archive-main-o`.
- Do not continue into baseline evidence when the current archive outcome is
  not `members_ready`.

## Provenance Anchors

- `<installed-skill-root>/examples/artifacts/sample-target/archive-intake-record.md`
- `<installed-skill-root>/examples/artifacts/sample-target/archive-member-inventory.md`
- `<installed-skill-root>/examples/artifacts/sample-target/archive-replay-command-record.md`

## Reviewer Notes

- The accepted target keeps the archive id in the downstream target identity.
- Deferred, unsupported, and failed members remain visible even though the
  archive-level workflow can continue with one accepted member.
- The failed sample row documents how extractor failures must stay reviewable;
  the 2026-03-30 happy-path replay itself did not need that row to reach
  `members_ready`.
