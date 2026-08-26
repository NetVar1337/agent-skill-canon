# Strings And Constants: `sample-target`

## Status

- Stage: `Baseline Evidence`
- Artifact state: Implemented evidence-only export surface
- Pending local verification:
  - actual string rows
  - actual constant anchors

## Export Schema

| Kind                                            | Value                        | Address Or Usage | Candidate Meaning                                       | Selection Value     |
| ----------------------------------------------- | ---------------------------- | ---------------- | ------------------------------------------------------- | ------------------- |
| string / constant / GUID / path / format string | `pending_local_verification` | `0x...`          | protocol clue / error path / file format / dispatch key | low / medium / high |

## Review Rules

- Prioritize recognizable strings and constants before semantic mutation.
- Use this file to justify candidate-function selection.
- Promote strong anchors into `reconstruction-log.md`.

## Current Local Observation

No baseline string or constant export has been runtime-validated in this pass
because no local Ghidra installation was available.
