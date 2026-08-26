# Imports And Libraries: `sample-target`

## Status

- Stage: `Baseline Evidence`
- Artifact state: Implemented evidence-only export surface
- Pending local verification:
  - actual imported libraries
  - actual external symbols

## Export Schema

| Library                      | Symbol                       | Address Or Slot  | Anchor Quality      | Why It Matters                                                  |
| ---------------------------- | ---------------------------- | ---------------- | ------------------- | --------------------------------------------------------------- |
| `pending_local_verification` | `pending_local_verification` | `0x...` or `n/a` | low / medium / high | likely subsystem, protocol, parser, crypto, IO, or runtime clue |

## Review Rules

- Use this file as an outside-in starting point.
- Highlight libraries or symbols that suggest parsing, crypto, network,
  filesystem, allocator, or runtime behavior.
- Promote stable anchors into `reconstruction-log.md` before selecting deeper
  targets.

## Current Local Observation

No baseline import export has been runtime-validated in this pass because no
local Ghidra installation was available.
