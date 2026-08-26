# Detailed Call Graph: `sample-target`

## Status

- Stage: `Baseline Evidence Follow-Up`
- Artifact state: Registered focused export surface with local runtime validation recorded
- Use this file when `xrefs-and-callgraph.md` is too coarse to justify the
  next outside-in target

## Export Schema

| Caller                 | Caller Entry  | Callee Kind                            | Callee                            | Callee Entry                        | Ref Type             | Call Sites                |
| ---------------------- | ------------- | -------------------------------------- | --------------------------------- | ----------------------------------- | -------------------- | ------------------------- |
| observed function name | entry address | `function` / `external` / `unresolved` | observed callee or external label | callee entry or destination address | Ghidra call ref type | aggregated callsite count |

## Function Summary Schema

| Function               | Entry         | Body Size     | Incoming Call Refs          | Unique Outgoing Targets                          | Outgoing Call Sites                | Name Status                              |
| ---------------------- | ------------- | ------------- | --------------------------- | ------------------------------------------------ | ---------------------------------- | ---------------------------------------- |
| observed function name | entry address | address count | call refs to function entry | unique internal/external/unresolved call targets | total call refs from function body | `default` or `analyst_named_or_imported` |

## Review Rules

- Run this export only after baseline evidence has refreshed the coarse
  `xrefs-and-callgraph.md` surface.
- Treat `external` and `unresolved` targets as follow-up prompts rather than
  settled semantics.
- Re-run after meaningful rename or signature changes if call adjacency still
  drives target selection.

## Current Local Observation

The local 2026-03-29 validation pass exported reviewable caller/callee rows to
`.work/ghidra-artifacts/ls-java-cache-test3/call-graph-detail.md` via
`ExportCallGraph.java`.
