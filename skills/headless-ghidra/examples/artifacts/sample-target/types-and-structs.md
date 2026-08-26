# Types And Structs: `sample-target`

## Status

- Stage: `Baseline Evidence`
- Artifact state: Implemented evidence-only export surface
- Pending local verification:
  - actual recovered type rows
  - any later semantic refinement

## Export Schema

| Kind                               | Symbol Or Type Name          | Location                 | Current Guess             | Why It Matters                         | Notes                                   |
| ---------------------------------- | ---------------------------- | ------------------------ | ------------------------- | -------------------------------------- | --------------------------------------- |
| struct / enum / prototype / vtable | `pending_local_verification` | address or function name | auto-recovered or unknown | supports later semantic reconstruction | tentative until cited by later evidence |

## Review Rules

- Baseline type recovery is descriptive, not final.
- Use this file to capture early object-shape clues before recording semantic
  names or field meanings.
- Stable semantic refinements belong in `renaming-log.md` and
  `reconstruction-log.md`.

## Current Local Observation

No baseline type export has been runtime-validated in this pass because no
local Ghidra installation was available.
