# Function Names: `sample-target`

## Status

- Stage: `Baseline Evidence`
- Artifact state: Implemented evidence-only export surface
- Pending local verification:
  - actual function rows
  - any analyst-approved rename mapping

## Export Schema

| Function Identity            | Entry Address | Current Name                | Kind         | Selection Priority  | Notes                                    |
| ---------------------------- | ------------- | --------------------------- | ------------ | ------------------- | ---------------------------------------- |
| `pending_local_verification` | `0x...`       | `FUN_...` or recovered name | body / thunk | low / medium / high | outside-in anchor or follow-up candidate |

## Review Rules

- Treat this file as a baseline evidence surface, not a semantic truth table.
- Use imports, strings, constants, and xrefs to justify any later rename.
- Mirror stable semantic changes into `renaming-log.md`.

## Current Local Observation

No baseline function export has been runtime-validated in this pass because no
local Ghidra installation was available.
