# Signature Log: `sample-target`

## Status

- Artifact state: Registered review manifest for signature replay
- Consumed by:
  - `ApplyFunctionSignatures.java` as a supported `metadata_updating` script
  - `VerifyFunctionSignatures.java` as a supported `verification_audit` script
- Runtime contract:
  - default runtime path is `.work/ghidra-artifacts/<target-id>/signature-log.md`
  - current validation is recorded in `latest-version-validation.md`

## Intended Runtime Inputs And Outputs

- Input manifest: `signature-log.md`
- Apply report: `signature-apply-report.md`
- Verification report: `signature-verification-report.md`

## Signature Schema

| Target Address           | Expected Current Name  | Expected Current Signature                                         | New Function Name                         | Return Type           | Parameter List                | Calling Convention                                        | Prior Evidence               | Change Summary               | Confidence          | Linked Selection             | Open Questions               | Status                                |
| ------------------------ | ---------------------- | ------------------------------------------------------------------ | ----------------------------------------- | --------------------- | ----------------------------- | --------------------------------------------------------- | ---------------------------- | ---------------------------- | ------------------- | ---------------------------- | ---------------------------- | ------------------------------------- |
| `pending_target_address` | `pending_current_name` | `return=<type> \| params=<name:type; ...> \| calling=<convention>` | `no_change` / `pending_new_function_name` | `pending_return_type` | `ctx:void *; length:uint32_t` | default / cdecl / stdcall / thiscall / fastcall / unknown | `pending_local_verification` | `pending_local_verification` | low / medium / high | `pending_local_verification` | `pending_local_verification` | blocked / ready / approved / complete |

## Rules

- `Target Address`, `Expected Current Name`, `Expected Current Signature`,
  `Return Type`, `Parameter List`, `Calling Convention`, `Prior Evidence`, and
  `Linked Selection` are required before the signature scripts may execute a
  row.
- `New Function Name` may be `no_change` when the row is intended only for
  prototype refinement.
- `Expected Current Signature` must use the canonical form
  `return=<type> | params=<name:type; ...> | calling=<convention>`.
- `Parameter List` should use a conservative text format such as
  `ctx:void *, length:uint32_t` rather than a full C parser contract.
- Use `void` for zero-parameter functions and `...` as the final token for
  varargs.
- `Calling Convention` must stay explicit whenever the change depends on it.
- `Prior Evidence` must cite reviewed artifacts, not intuition alone.
- `Linked Selection` must identify the target-selection record that made this
  mutation the next allowed step.
- Runtime replay should use a workspace copy under
  `.work/ghidra-artifacts/<target-id>/signature-log.md`.

## Current Local Observation

This example file records the registered manifest surface. Local replay
validation is captured separately in `latest-version-validation.md`.
