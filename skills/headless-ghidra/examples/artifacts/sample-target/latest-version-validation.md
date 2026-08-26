# Latest-Version Validation: `sample-target`

## Status

Machine-specific install, cache, and binary paths below are preserved only as
local validation observations. Revalidation commands should use the portable
placeholders shown in each command block.

- Artifact state: Validation gate record with local Ghidra replay verification
  complete and incremental compare replay pending
- Validated now:
  - the tightened workflow has a named replay path for baseline evidence,
    evidence review, target selection, source-comparison preparation, rename
    application and verification, signature application and verification,
    review-artifact lint, selected decompilation, and stepwise compare records
  - archive normalization now has a named wrapper entrypoint plus reviewable
    intake, inventory, handoff, and replay surfaces
  - the active supported runtime surface is the Java script family
    `ExportAnalysisArtifacts.java`, `ReviewEvidenceCandidates.java`,
    `PlanTargetSelection.java`, `ApplyRenames.java`, `VerifyRenames.java`,
    `ApplyFunctionSignatures.java`, `VerifyFunctionSignatures.java`, and
    `LintReviewArtifacts.java`
  - baseline evidence-only behavior is documented as a hard rule
  - source-comparison intake, gated formal diffing, and downstream source-
    derived trust states now have named review surfaces
  - selected decompilation is documented as a later-stage action with ordered
    selectors
  - incremental executable interposition and library-harness fallback now have
    named documentation surfaces, but remain pending local replay
- Validated locally on 2026-03-29:
  - install discovery resolved a local validation `analyzeHeadless` path under
    the validated install root
  - the Java-only workflow completed `baseline`, `review-evidence`,
    `target-selection`, `apply-renames`, `verify-renames`,
    `apply-signatures`, `verify-signatures`, `lint-review-artifacts`, and
    `decompile-selected` against the reviewed mock `ls` verification binary
  - malformed `signature-log.md` input still produced reviewable
    `signature-apply-report.md` and `artifact-lint-report.md` before the
    wrapper returned non-zero
  - runtime logs and redirected Ghidra user-home state stayed under `.work/`
  - actions for the same target were replayed sequentially because concurrent
    runs can fail with a Ghidra project lock
  - no supported generic wrapper exists yet for incremental compare
- Validated locally on 2026-03-30 for archive normalization:
  - `normalize-ar-archive.sh --help` exposed archive, artifact-root, member,
    review, and selection-policy inputs
  - happy-path archive normalization emitted intake, inventory, handoff, and
    replay surfaces under
    `.work/ghidra-artifacts/sample-target-archive-runtime/`
  - the same happy-path command reran twice with identical hashes for all four
    review surfaces
  - stop-path archive normalization emitted
    `stopped_no_eligible_members` under
    `.work/ghidra-artifacts/sample-target-archive-stop-runtime/`
  - an unsupported `--selection-policy` value failed fast, and a fake archive
    still produced reviewable failure Markdown under
    `.work/ghidra-artifacts/fake/`
- Validated locally on 2026-03-31 for outside-in selection replay:
  - `discover-ghidra.sh --print-install-dir` resolved a local validation
    Ghidra 12.0.4 install root
  - `discover-ghidra.sh --show-help` succeeded against the resolved install
  - the local shell toolchain exposed `java 17.0.18` and `javac 17.0.18`
    while Headless Ghidra replay itself used the bundled Java 21 runtime
  - `bash -n "$SKILL_ROOT/scripts/run-headless-analysis.sh"`
    passed before replay
  - sequential local replay completed `baseline`, `review-evidence`, and
    `target-selection` for target id `sample-target--archive-main-o` against
    `.work/ghidra-artifacts/sample-target-archive-runtime/normalized-members/sample-target--archive_main.o`
  - `review-evidence` and `target-selection` executed the updated Java scripts
    successfully through Headless Ghidra and emitted runtime artifacts under
    `.work/ghidra-artifacts/sample-target--archive-main-o/`
  - a second sequential local replay completed `baseline`, `review-evidence`,
    and `target-selection` for the mock `ls` verification binary under
    `.work/ghidra-artifacts/sample-target-replay/`
  - that mock `ls` replay treated successful headless execution of
    `ReviewEvidenceCandidates.java` and `PlanTargetSelection.java` as compile
    and execution validation for the updated Java-script review surfaces
  - one parallel `target-selection` attempt failed only because Ghidra locked
    the shared project; rerunning sequentially passed without feature changes
  - Ghidra emitted non-fatal cache-maintenance and packed-db cache permission
    warnings under a local validation `/var/tmp` cache directory, but the
    supported actions still completed successfully

## Current Locally Validated Version

`12.0.4`

Replace this field only after an operator validates the replay path on the
newest supported local Ghidra installation.

## Validation Status By Behavior

| Behavior                                       | Status                     | Notes                                                                                                                                                                                                                                                                                                                                                                                                               |
| ---------------------------------------------- | -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Install discovery                              | Validated                  | `discover-ghidra.sh --print-analyze-headless` resolved the Homebrew install.                                                                                                                                                                                                                                                                                                                                        |
| Live help retrieval                            | Validated                  | `analyzeHeadless -help` was retrieved from the resolved path during local replay preparation.                                                                                                                                                                                                                                                                                                                       |
| Archive-normalization help surface             | Validated                  | `normalize-ar-archive.sh --help` exposed archive, artifact, member-output, review-output, and selection-policy inputs.                                                                                                                                                                                                                                                                                              |
| Archive-normalization happy path               | Validated                  | `sample-target` emitted reviewable intake, inventory, handoff, and replay surfaces under `.work/ghidra-artifacts/sample-target-archive-runtime/`.                                                                                                                                                                                                                                                                   |
| Archive-normalization deterministic rerun      | Validated                  | Two reruns produced identical hashes for the four review surfaces.                                                                                                                                                                                                                                                                                                                                                  |
| Archive-normalization stop path                | Validated                  | `sample-target-stop` recorded `stopped_no_eligible_members` and zero accepted downstream targets.                                                                                                                                                                                                                                                                                                                   |
| Archive-normalization invalid-policy rejection | Validated                  | Unknown `--selection-policy` values now exit non-zero instead of silently widening scope.                                                                                                                                                                                                                                                                                                                           |
| Archive-normalization listing failure path     | Validated                  | A fake archive recorded `normalization_failed` and still emitted reviewable Markdown under `.work/ghidra-artifacts/fake/`.                                                                                                                                                                                                                                                                                          |
| Baseline evidence-only export                  | Validated                  | Baseline artifacts and blocked decompilation placeholder were emitted under `.work/ghidra-artifacts/ls-smoke-main-20260329a/`.                                                                                                                                                                                                                                                                                      |
| Evidence review export                         | Validated                  | `evidence-candidates.md` was emitted under `.work/ghidra-artifacts/ls-five-scripts-20260329b/`, and the updated outside-in replay also emitted runtime surfaces under `.work/ghidra-artifacts/sample-target--archive-main-o/` and `.work/ghidra-artifacts/sample-target-replay/`. Successful headless execution of `ReviewEvidenceCandidates.java` now serves as compile and execution validation for this surface. |
| Target selection export                        | Validated                  | `target-selection.md` was emitted under `.work/ghidra-artifacts/ls-five-scripts-20260329b/`, and the updated outside-in replay also emitted runtime surfaces under `.work/ghidra-artifacts/sample-target--archive-main-o/` and `.work/ghidra-artifacts/sample-target-replay/`. Successful headless execution of `PlanTargetSelection.java` now serves as compile and execution validation for this surface.         |
| Source-comparison preparation                  | Docs and syntax validated  | Runtime comparison still depends on a real upstream source checkout.                                                                                                                                                                                                                                                                                                                                                |
| Source-comparison fallback path contract       | Validated                  | `plan-compare` and `compare-prep` both emitted `third_party/upstream/project-slug` and `.work/upstream-sources/project-slug`.                                                                                                                                                                                                                                                                                       |
| Source-comparison deferred path                | Validated                  | The example now documents how a `deferred` upstream-reference record keeps formal diffing closed until a reviewable source reference exists.                                                                                                                                                                                                                                                                        |
| Source-comparison real-reference happy path    | Validated                  | Ghidra baseline export succeeded for `zlib-minigzip-20260329`, and the reviewed subset under `third_party/upstream/zlib/` now supports concrete inherited and unresolved findings.                                                                                                                                                                                                                                  |
| Source-comparison stale revalidation path      | Review contract documented | The walkthrough and validation notes now require source-derived claims to move back to blocked until re-review completes.                                                                                                                                                                                                                                                                                           |
| Rename application                             | Validated                  | `rename-apply-report.md` recorded `Applied: 2`, `Failed: 0` for one `function` row and one `symbol` row.                                                                                                                                                                                                                                                                                                            |
| Rename verification                            | Validated                  | `rename-verification-report.md` recorded `Verified: 2`, `Failed: 0` for one `function` row and one `symbol` row.                                                                                                                                                                                                                                                                                                    |
| Signature application                          | Validated                  | `signature-apply-report.md` recorded `Applied: 1`, `Failed: 0` for one thunk-row signature update.                                                                                                                                                                                                                                                                                                                  |
| Signature verification                         | Validated                  | `signature-verification-report.md` recorded `Verified: 1`, `Failed: 0` for the same thunk-row signature update.                                                                                                                                                                                                                                                                                                     |
| Review-artifact lint                           | Validated                  | `artifact-lint-report.md` recorded `Passed: 2`, `Failed: 0` for the default rename/signature manifests.                                                                                                                                                                                                                                                                                                             |
| Selected decompilation export gating           | Validated                  | `decompiled-output.md` exported only the requested `100000718` and `10000072a` functions.                                                                                                                                                                                                                                                                                                                           |
| Incremental compare record surface             | Review contract documented | `comparison-command-log.md`, `reconstruction-log.md`, and `decompiled-output.md` now require a per-step original-versus-hybrid compare record.                                                                                                                                                                                                                                                                      |
| Executable fallback-to-original bridge         | Review contract documented | Unresolved callees must route back into the original binary through reviewed addresses, trampolines, or bridge stubs.                                                                                                                                                                                                                                                                                               |
| Library fallback-to-original handle            | Review contract documented | The workflow now requires a generated harness entrypoint that opens the original library and routes unresolved calls through that handle.                                                                                                                                                                                                                                                                           |
| Generic compare wrapper action                 | Not yet implemented        | Exact build, injection, and compare commands remain target-specific and must be recorded manually.                                                                                                                                                                                                                                                                                                                  |

The rename application and verification paths are now registered and locally
validated as supported reusable-script surfaces for executable `function`,
`symbol`, and `label` rows.

## Local Discovery Evidence

These local observations were validated on 2026-03-31:

```bash
export SKILL_ROOT=/path/to/installed/headless-ghidra
java -version
javac -version
bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" --help >/dev/null 2>&1 || true
bash -n "$SKILL_ROOT/scripts/run-headless-analysis.sh"
bash "$SKILL_ROOT/scripts/discover-ghidra.sh" --print-install-dir
bash "$SKILL_ROOT/scripts/discover-ghidra.sh" --print-analyze-headless
bash "$SKILL_ROOT/scripts/discover-ghidra.sh" --show-help
file .work/ghidra-artifacts/sample-target-archive-runtime/normalized-members/sample-target--archive_main.o
```

Observed result:

- local shell Java toolchain resolved `java 17.0.18` and `javac 17.0.18`
- `bash -n "$SKILL_ROOT/scripts/run-headless-analysis.sh"`
  passed
- install discovery resolved a local validation Ghidra 12.0.4 install root
- install discovery resolved a local validation `analyzeHeadless` path under
  that install root
- `discover-ghidra.sh --show-help` succeeded for the resolved install
- candidate sample binary exists at
  `.work/ghidra-artifacts/sample-target-archive-runtime/normalized-members/sample-target--archive_main.o`
- Headless Ghidra replay used the install-bundled Java 21 runtime successfully
- end-to-end Java workflow replay completed against target id
  `ls-smoke-main-20260329a`
- extended Stage 2/3/5 replay completed against target id
  `ls-five-scripts-20260329b`
- source-comparison happy-path replay succeeded for target id
  `zlib-minigzip-20260329` against review reference
  `third_party/upstream/zlib/`
- archive-normalization happy path succeeded for archive id `sample-target`
  and accepted downstream target `sample-target--archive-main-o`
- archive-normalization stop path succeeded for archive id
  `sample-target-stop`
- sequential outside-in replay completed `baseline`, `review-evidence`, and
  `target-selection` for `sample-target--archive-main-o`
- runtime `evidence-candidates.md` and `target-selection.md` were emitted
  under `.work/ghidra-artifacts/sample-target--archive-main-o/`
- sequential mock `ls` replay completed `baseline`, `review-evidence`, and
  `target-selection` for target id `sample-target-replay`
- runtime `evidence-candidates.md` and `target-selection.md` were emitted
  under `.work/ghidra-artifacts/sample-target-replay/`
- successful headless execution of `ReviewEvidenceCandidates.java` and
  `PlanTargetSelection.java` serves as compile and execution validation for
  the updated review surfaces
- concurrent same-target replay can fail with a Ghidra project lock, so local
  replay should stay sequential per target
- non-fatal cache-maintenance and packed-db cache permission warnings were
  emitted under a local validation `/var/tmp` cache directory, but the
  supported actions still passed

## Required Revalidation Commands

Archive-normalization replay, when the reviewed input is a raw `ar` archive:

```bash
export WORKSPACE_ROOT=$PWD
export SKILL_ROOT=/path/to/installed/headless-ghidra
export ARCHIVE_PATH=$WORKSPACE_ROOT/.work/ghidra-artifacts/archive-normalization-smoke-20260330/libsample.a
export ARCHIVE_ID=sample-target
export ARCHIVE_ARTIFACT_ROOT=$WORKSPACE_ROOT/.work/ghidra-artifacts/sample-target-archive-runtime
export STOP_ARCHIVE_PATH=$WORKSPACE_ROOT/.work/ghidra-artifacts/archive-normalization-smoke-20260330/libsample-stop.a
export STOP_ARCHIVE_ID=sample-target-stop
export STOP_ARCHIVE_ARTIFACT_ROOT=$WORKSPACE_ROOT/.work/ghidra-artifacts/sample-target-archive-stop-runtime

bash "$SKILL_ROOT/scripts/normalize-ar-archive.sh" --help

bash "$SKILL_ROOT/scripts/normalize-ar-archive.sh" \
  --archive "$ARCHIVE_PATH" \
  --archive-id "$ARCHIVE_ID" \
  --workspace-root "$WORKSPACE_ROOT" \
  --artifact-root "$ARCHIVE_ARTIFACT_ROOT" \
  --extractor ar

bash "$SKILL_ROOT/scripts/normalize-ar-archive.sh" \
  --archive "$STOP_ARCHIVE_PATH" \
  --archive-id "$STOP_ARCHIVE_ID" \
  --workspace-root "$WORKSPACE_ROOT" \
  --artifact-root "$STOP_ARCHIVE_ARTIFACT_ROOT" \
  --extractor ar
```

If `ar` is not available on `PATH`, rerun those commands with
`--extractor /absolute/path/to/ar`.

Reference replay commands:

```bash
export WORKSPACE_ROOT=$PWD
export SKILL_ROOT=/path/to/installed/headless-ghidra
export GHIDRA_INSTALL_DIR=/path/to/ghidra
export TARGET_BINARY=$WORKSPACE_ROOT/.work/verification/mock-ls/build/ls
export TARGET_ID=sample-target
export ARTIFACT_ROOT=$WORKSPACE_ROOT/.work/ghidra-artifacts/$TARGET_ID

bash "$SKILL_ROOT/scripts/discover-ghidra.sh" --show-help

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action baseline \
  --workspace-root "$WORKSPACE_ROOT" \
  --binary "$TARGET_BINARY" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --install-dir "$GHIDRA_INSTALL_DIR"

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action plan-call-graph \
  --workspace-root "$WORKSPACE_ROOT" \
  --binary "$TARGET_BINARY" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --install-dir "$GHIDRA_INSTALL_DIR"

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action call-graph \
  --workspace-root "$WORKSPACE_ROOT" \
  --binary "$TARGET_BINARY" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --install-dir "$GHIDRA_INSTALL_DIR"

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action plan-compare \
  --workspace-root "$WORKSPACE_ROOT" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --project-slug project-slug

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action compare-prep \
  --workspace-root "$WORKSPACE_ROOT" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --project-slug project-slug

cp "$SKILL_ROOT/examples/artifacts/sample-target/renaming-log.md" \
  "$ARTIFACT_ROOT/renaming-log.md"

# Replace the placeholder rows with observed function, symbol, or label entries
# from "$ARTIFACT_ROOT/function-names.md" before the next two commands.

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action apply-renames \
  --workspace-root "$WORKSPACE_ROOT" \
  --binary "$TARGET_BINARY" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --install-dir "$GHIDRA_INSTALL_DIR" \
  --rename-log "$ARTIFACT_ROOT/renaming-log.md"

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action verify-renames \
  --workspace-root "$WORKSPACE_ROOT" \
  --binary "$TARGET_BINARY" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --install-dir "$GHIDRA_INSTALL_DIR" \
  --rename-log "$ARTIFACT_ROOT/renaming-log.md"

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action review-evidence \
  --workspace-root "$WORKSPACE_ROOT" \
  --binary "$TARGET_BINARY" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --install-dir "$GHIDRA_INSTALL_DIR"

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action target-selection \
  --workspace-root "$WORKSPACE_ROOT" \
  --binary "$TARGET_BINARY" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --install-dir "$GHIDRA_INSTALL_DIR"

# Replace the placeholder signature row with a reviewed function identity and
# current signature before the next three commands.

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action apply-signatures \
  --workspace-root "$WORKSPACE_ROOT" \
  --binary "$TARGET_BINARY" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --install-dir "$GHIDRA_INSTALL_DIR" \
  --signature-log "$ARTIFACT_ROOT/signature-log.md"

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action verify-signatures \
  --workspace-root "$WORKSPACE_ROOT" \
  --binary "$TARGET_BINARY" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --install-dir "$GHIDRA_INSTALL_DIR" \
  --signature-log "$ARTIFACT_ROOT/signature-log.md"

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action lint-review-artifacts \
  --workspace-root "$WORKSPACE_ROOT" \
  --binary "$TARGET_BINARY" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --install-dir "$GHIDRA_INSTALL_DIR"

bash "$SKILL_ROOT/scripts/run-headless-analysis.sh" \
  --action decompile-selected \
  --workspace-root "$WORKSPACE_ROOT" \
  --binary "$TARGET_BINARY" \
  --target-id "$TARGET_ID" \
  --artifacts-dir "$ARTIFACT_ROOT" \
  --install-dir "$GHIDRA_INSTALL_DIR" \
  --selected-function 100000718 \
  --selected-function 10000072a

cp "$SKILL_ROOT/examples/artifacts/sample-target/comparison-command-log.md" \
  "$ARTIFACT_ROOT/comparison-command-log.md"

# Record the exact target-specific compare commands in
# "$ARTIFACT_ROOT/comparison-command-log.md". This workflow does not yet provide
# a generic compare wrapper, so the lines below are placeholders for the reviewed
# per-target commands.

<build-hybrid-replacement-command>
<run-original-target-command>
<run-hybrid-target-command>
<diff-or-compare-command>
```

Set `SKILL_ROOT` to the installed skill package location for your environment,
such as `<installed-skill-root>`,
`.claude/skills/headless-ghidra`, or an absolute global install
path.

Observed result:

- live help is retrieved from the discovered local binary
- baseline artifacts refresh without decompiled bodies
- `plan-call-graph` resolves the registered runner command path
- `call-graph` exports `call-graph-detail.md`
- `plan-compare` emits the expected review and fallback paths for a reviewable
  source-comparison intake
- `compare-prep` emits the same paths, creates the local fallback workspace,
  and reminds the reviewer to update `upstream-reference.md` and
  `third-party-diff.md`
- `baseline` against `zlib-minigzip-20260329` emits function, string, and
  import evidence that matches the reviewed `zlib` reference subset
- evidence review emits `evidence-candidates.md`
- target selection emits `target-selection.md`
- rename application writes a reviewable apply report under
  `.work/ghidra-artifacts/ls-smoke-main-20260329a/`
- rename verification writes a reviewable verification report under
  `.work/ghidra-artifacts/ls-smoke-main-20260329a/`
- signature application writes a reviewable apply report under
  `.work/ghidra-artifacts/ls-five-scripts-20260329b/`
- signature verification writes a reviewable verification report under
  `.work/ghidra-artifacts/ls-five-scripts-20260329b/`
- review-artifact lint writes a reviewable lint report under
  `.work/ghidra-artifacts/ls-five-scripts-20260329b/`
- selected decompilation runs only for explicitly chosen functions
- `comparison-command-log.md` now defines the required reproducibility surface
  for stepwise compare commands, but no concrete compare case was replayed in
  this validation pass
- the Java export/apply/verify scripts are the only active supported runtime
  implementations referenced by the replay surface
- malformed rename logs and malformed signature logs still produce reviewable
  reports and a non-zero wrapper exit
- fresh-workspace replay still resolves a supported Java home when the ambient
  shell points `JAVA_HOME` at an unsupported JDK

Current schema:

- the supported Java rename workflow accepts `Item Kind = function`,
  `Item Kind = symbol`, and `Item Kind = label`
- any future schema expansion still requires the docs, samples, and validation
  record to move together

## To Record After Validation

| Field                                                         | Value                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| ------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Validated Ghidra version                                      | `12.0.4`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Install path                                                  | `<local validation-ghidra-install-dir>`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Resolved analyzeHeadless path                                 | `<local validation-ghidra-install-dir>/support/analyzeHeadless`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Validation date                                               | `2026-03-31`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Validated by                                                  | `local validation replay`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| Baseline evidence-only behavior observed                      | `pass; emitted function/types/imports/strings/xrefs plus blocked decompiled-output placeholder`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Rename application behavior observed                          | `pass; applied one executable function row and one executable symbol row with Failed: 0`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Rename verification behavior observed                         | `pass; verified one executable function row and one executable symbol row with Failed: 0`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| Evidence review behavior observed                             | `pass; sequential replay for sample-target--archive-main-o emitted evidence-candidates.md under .work/ghidra-artifacts/sample-target--archive-main-o/ with frontier candidate rows and metric fields labeled as secondary context`                                                                                                                                                                                                                                                                                                                                                                  |
| Target selection behavior observed                            | `pass; sequential replay for sample-target--archive-main-o emitted target-selection.md under .work/ghidra-artifacts/sample-target--archive-main-o/ with one automatic default target, frontier review fields, and tie-break rationale`                                                                                                                                                                                                                                                                                                                                                              |
| Signature application behavior observed                       | `pass; applied one executable thunk-row signature update with Failed: 0`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| Signature verification behavior observed                      | `pass; verified one executable thunk-row signature update with Failed: 0`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| Review-artifact lint behavior observed                        | `pass; linted renaming-log.md and signature-log.md with Failed: 0`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| Selected decompilation behavior observed                      | `pass; emitted only 100000718 and 10000072a with reviewable metadata fields`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Incremental compare behavior observed                         | `docs-only; comparison-command-log.md now defines the required original-versus-hybrid record, but no concrete compare case was replayed yet`                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Source-comparison fallback-path behavior observed             | `pass; plan-compare and compare-prep emitted third_party/upstream/project-slug and .work/upstream-sources/project-slug with explicit follow-up notes`                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| Source-comparison deferred-path behavior observed             | `pass; the sample guidance now defines a deferred upstream-reference state that keeps formal diffing closed until a reviewable source reference exists`                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| Source-comparison real-reference happy-path behavior observed | `pass; Ghidra baseline against a local validation minigzip build now aligns with reviewed zlib reference files under third_party/upstream/zlib/`                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| Source-comparison stale-path behavior observed                | `docs-only; the walkthrough and validation notes now require source-derived claims to move back to blocked until re-review finishes`                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Notes on version-specific behavior                            | `wrapper redirects Ghidra user.home into .work/ghidra-user-home, records per-action run/script logs under .work/ghidra-artifacts/<target-id>/logs/, treats malformed rename logs or signature logs as non-zero even when Ghidra itself continues after the script error, replays for the same target should stay sequential because concurrent runs can hit a Ghidra project lock, and Ghidra 12.0.4 on the validating host emitted non-fatal cache-maintenance and packed-db cache permission warnings under a local validation /var/tmp cache directory while the supported actions still passed` |
