# Ghidra Script Review Checklist

Use this checklist before treating a reusable headless Ghidra script as part of
the supported analysis workflow. If a required item fails, keep the script
target-specific or experimental until the gap is resolved.

## CLI First

- [ ] The needed behavior is not already covered by `ghidra-agent-cli`.
- [ ] The script's purpose is narrower and clearer than "custom analysis".
- [ ] Users know when to use this script instead of an existing command.

## Headless Operation

- [ ] The script does not require the Ghidra GUI.
- [ ] The invocation path is documented.
- [ ] Required inputs are explicit.
- [ ] Target id, binary path, and artifact root are discoverable from the
      documented command or target state.
- [ ] The script does not depend on hidden local machine state.

## Evidence And Outputs

- [ ] Every output path is explicit.
- [ ] Outputs are deterministic enough to compare across reruns.
- [ ] Empty or unavailable result sets are reported honestly.
- [ ] Output content is useful without opening the GUI.
- [ ] Live output does not go into the installed skill directory.
- [ ] Example artifact paths are not reused as live output destinations.

## Side Effects

- [ ] The script declares one side-effect class: read-only, export-only,
      metadata-updating, or mixed-wrapper.
- [ ] Read-only scripts do not mutate program metadata.
- [ ] Export-only scripts write reports but do not change names, prototypes, or
      types.
- [ ] Metadata-updating scripts explain every intended mutation.
- [ ] Mixed wrappers keep created files under explicit workspace paths.
- [ ] The weakest side-effect class that solves the task was chosen.

## Metadata Mutation

- [ ] Renames, signatures, types, or symbols come from reviewable evidence.
- [ ] The input manifest is reviewable.
- [ ] Apply and verify reports are separate.
- [ ] Failed rows are reported rather than silently skipped.
- [ ] The script does not widen the supported schema beyond what has been
      validated locally.

## Archive Handling

- [ ] Archive path and archive id are explicit.
- [ ] Member output and review output paths are explicit.
- [ ] Duplicate archive members remain visible.
- [ ] Only accepted extracted members are handed to later Ghidra analysis.
- [ ] Unsupported selection policies fail closed.

## Program Data Access

- [ ] The script states which program objects it reads.
- [ ] It handles stripped or partially recovered binaries honestly.
- [ ] It reports missing imports, strings, types, or references instead of
      inventing them.
- [ ] It does not overstate confidence for decompiler or type-recovery output.

## Validation

- [ ] The script has been run locally against the target, or the documentation
      clearly says validation is pending.
- [ ] The exact command used for validation is recorded.
- [ ] Any local limitation, missing dependency, or failed check is recorded.
- [ ] Runtime claims are based on observed behavior, not assumptions.

## Final Decision

- [ ] The reviewer can explain why this is reuse, extension, replacement, or a
      justified new script.
- [ ] Another user can rerun the documented command path.
- [ ] The script fits the outside-in analysis flow instead of bypassing it.
- [ ] The next workflow step is clear from the output.
