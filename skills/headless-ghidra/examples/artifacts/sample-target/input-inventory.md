# Input Inventory: `sample-target`

## Status

- Artifact state: Implemented inventory template
- Validated now:
  - runtime artifact root and generated project root are fixed
  - upstream-source, decompilation, and incremental-compare inputs have named
    fields
- Pending local verification:
  - actual binary path
  - actual Ghidra install path
  - actual upstream project and version

## Required Inputs

| Input                                                                 | Required | Current State              | Notes                                                                                             |
| --------------------------------------------------------------------- | -------- | -------------------------- | ------------------------------------------------------------------------------------------------- |
| `TARGET_ID=sample-target`                                             | Yes      | Validated                  | Stable identifier for project and artifact paths.                                                 |
| `TARGET_BINARY`                                                       | Yes      | Pending local verification | Absolute path to the analyzed binary.                                                             |
| `GHIDRA_INSTALL_DIR`                                                  | Yes      | Not found locally          | Install root used for headless replay.                                                            |
| `GHIDRA_HEADLESS`                                                     | Yes      | Not found locally          | Resolved dynamically by `discover-ghidra.sh`.                                                     |
| `WORKSPACE_ROOT`                                                      | Yes      | Validated                  | Defaults to the current git repo root or current working directory; may be overridden explicitly. |
| `PROJECT_ROOT=$WORKSPACE_ROOT/.work/ghidra-projects/sample-target/`   | Yes      | Validated                  | Disposable local project root.                                                                    |
| `ARTIFACT_ROOT=$WORKSPACE_ROOT/.work/ghidra-artifacts/sample-target/` | Yes      | Validated                  | Runtime artifact root for generated analysis output.                                              |
| `SKILL_ROOT=<installed-skill-root>`                                   | Yes      | Validated                  | Installed skill package root, which may be project-local or global.                               |
| `SCRIPT_ROOT=$SKILL_ROOT/ghidra-scripts/`                             | Yes      | Validated                  | Reusable script root.                                                                             |

## Source Comparison Inputs

| Field              | Required When                         | Current State              | Notes                                                                               |
| ------------------ | ------------------------------------- | -------------------------- | ----------------------------------------------------------------------------------- |
| `project_slug`     | upstream clues exist                  | Pending local verification | Stable identifier for the likely upstream source project.                           |
| `probable_version` | upstream clues exist                  | Pending local verification | Use uncertainty markers when exact version is unknown.                              |
| `reference_mode`   | upstream clues exist                  | Pending local verification | `submodule` or `local_clone_fallback`.                                              |
| `reference_path`   | upstream clues exist                  | Pending local verification | `third_party/upstream/<project-slug>/` or `.work/upstream-sources/<project-slug>/`. |
| `fallback_reason`  | `reference_mode=local_clone_fallback` | Not applicable yet         | Required whenever the workflow cannot use a submodule.                              |

## Selected Decompilation And Incremental Compare Inputs

| Field                         | Required When                        | Current State              | Notes                                                                                  |
| ----------------------------- | ------------------------------------ | -------------------------- | -------------------------------------------------------------------------------------- |
| `selected_target`             | selected decompilation requested     | Pending local verification | One automatic default target for the current frontier step.                            |
| `selection_record_path`       | selected decompilation requested     | Validated                  | Use `target-selection.md` plus `reconstruction-log.md`.                                |
| `frontier_reason`             | selected decompilation requested     | Pending local verification | Must identify whether the row is an outermost anchor or child of a `matched` boundary. |
| `relationship_type`           | selected decompilation requested     | Pending local verification | `entry_adjacent`, `dispatch_edge`, `callee`, `wrapper_edge`, or `none`.                |
| `verified_parent_boundary`    | child of `matched` boundary selected | Pending local verification | Required for any deeper child row.                                                     |
| `selection_reason`            | selected decompilation requested     | Pending local verification | Must explain why this row won the current frontier step.                               |
| `question_to_answer`          | selected decompilation requested     | Pending local verification | States what the next reconstruction step is intended to resolve.                       |
| `deviation_reason`            | documented deviation selected        | Not applicable yet         | Required whenever the workflow breaks the default frontier order.                      |
| `deviation_risk`              | documented deviation selected        | Not applicable yet         | Required whenever the workflow breaks the default frontier order.                      |
| `comparison_command_log_path` | selected decompilation requested     | Validated                  | Use `comparison-command-log.md`.                                                       |
| `replacement_boundary`        | selected decompilation requested     | Pending local verification | Replace only one reviewed outside-in boundary at a time.                               |
| `fallback_strategy`           | selected decompilation requested     | Pending local verification | `original_address_bridge`, `original_library_handle`, or `none`.                       |
| `hybrid_entrypoint`           | selected decompilation requested     | Pending local verification | Injected boundary for executables or generated harness entry for libraries.            |
| `original_target_path`        | selected decompilation requested     | Pending local verification | Original binary or library used as the compare baseline.                               |

## Tracked Inputs

- sample surfaces under `<installed-skill-root>/examples/artifacts/sample-target/`
- `command-manifest.md`
- `call-graph-detail.md`
- `evidence-candidates.md`
- `target-selection.md`
- `reconstruction-log.md`
- `comparison-command-log.md`
- `upstream-reference.md`
- `third-party-diff.md`
- `renaming-log.md`
- `signature-log.md`
- `decompiled-output.md`

## External Inputs

- generated Ghidra project files under `.work/ghidra-projects/sample-target/`
- generated analysis artifacts under `.work/ghidra-artifacts/sample-target/`
- fallback local upstream clones under `.work/upstream-sources/<project-slug>/`
- transient logs unless intentionally summarized into a reviewed artifact

## Analyst Checklist

Before baseline replay:

1. confirm `TARGET_BINARY`
2. confirm the install discovery path or blocked state
3. confirm the generated project can be deleted and recreated

Before source comparison:

1. record the likely upstream project slug
2. record the probable version or explicit uncertainty
3. record whether submodule or fallback clone mode is being used

Before selected decompilation:

1. record the selected target and its `frontier_reason`
2. confirm the row is either an outermost anchor or a child of a `matched`
   boundary
3. confirm `selection_reason` and `question_to_answer` are populated
4. confirm any deviation records both `deviation_reason` and `deviation_risk`
5. confirm the current step can run a hybrid compare against the original
   target
