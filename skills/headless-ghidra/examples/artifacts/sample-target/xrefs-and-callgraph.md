# Xrefs And Call Graph: `sample-target`

## Status

- Stage: `Baseline Evidence`
- Artifact state: Implemented evidence-only export surface
- Pending local verification:
  - actual cross-reference rows
  - actual call relationships

## Export Schema

| Source                                | Relationship                            | Target                   | Outside-In Use                                          | Notes                    |
| ------------------------------------- | --------------------------------------- | ------------------------ | ------------------------------------------------------- | ------------------------ |
| symbol / function / string / constant | calls / references / loads / dispatches | symbol / function / data | select next outer-layer target or justify moving inward | tentative until reviewed |

## Review Rules

- Use this file to move from external anchors into candidate functions.
- Record both direct and indirect edges when they influence traversal order.
- Justify selected decompilation order with this file and `reconstruction-log.md`.

## Current Local Observation

No baseline xref export has been runtime-validated in this pass because no
local Ghidra installation was available.
