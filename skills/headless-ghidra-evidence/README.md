# Headless Ghidra Third-Party Evidence — P2

P2 reviews baseline and runtime evidence to decide whether the target contains
third-party code. It records libraries, versions, confidence, pristine source
locations, local adaptation areas, and function classifications for later
metadata recovery.

Translations: [简体中文](./README.zh-CN.md) | [日本語](./README.ja-JP.md)

## Place In The Pipeline

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P2 is an evidence and classification phase. It does not apply names or
signatures to Ghidra, and it does not decompile functions.

## When To Use

Use this skill when:

- Baseline imports, strings, constants, types, vtables, and callgraph evidence
  are ready for review.
- A possible upstream project or vendored library needs to be recorded.
- Pristine third-party source needs to be registered.
- Functions need first-party or third-party classification.
- The review needs to explicitly record that no third-party code was found.

## Phase Boundaries

- Baseline reads go through `functions`, `imports`, `constants`, `strings`,
  `vtables`, `types`, and `callgraph` commands.
- Third-party writes go through `third-party` commands.
- Source acquisition itself is outside the CLI. The CLI records source paths and
  evidence after acquisition is reviewed.
- Pristine source must remain unmodified. Local adaptation notes or patches
  belong under `third-party/compat/`.

## Inputs

- `artifacts/<target-id>/baseline/functions.yaml`
- `artifacts/<target-id>/baseline/callgraph.yaml`
- `artifacts/<target-id>/baseline/types.yaml`
- `artifacts/<target-id>/baseline/constants.yaml`
- `artifacts/<target-id>/baseline/vtables.yaml`
- `artifacts/<target-id>/baseline/strings.yaml`
- `artifacts/<target-id>/baseline/imports.yaml`
- `artifacts/<target-id>/runtime/run-manifest.yaml`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- Existing `artifacts/<target-id>/third-party/identified.yaml`, if present

## Outputs

- `artifacts/<target-id>/third-party/identified.yaml`
- `artifacts/<target-id>/third-party/pristine/<library>@<version>/`
- Optional `artifacts/<target-id>/third-party/compat/<library>@<version>/`
- Execution log entries describing evidence decisions

## Commands The Skill Uses

These examples show the CLI calls the skill may make. In normal use, ask the agent to run the phase; do not run these commands by hand unless you are troubleshooting.

```sh
ghidra-agent-cli --target sample-target functions list
ghidra-agent-cli --target sample-target imports list
ghidra-agent-cli --target sample-target strings list
ghidra-agent-cli --target sample-target constants list
ghidra-agent-cli --target sample-target types list
ghidra-agent-cli --target sample-target vtables list
ghidra-agent-cli --target sample-target callgraph list
ghidra-agent-cli --target sample-target third-party add \
  --library zlib \
  --version 1.2.13 \
  --confidence high \
  --evidence "import and string evidence"
ghidra-agent-cli --target sample-target third-party vendor-pristine \
  --library zlib \
  --source-path ./third_party/upstream/zlib
ghidra-agent-cli --target sample-target third-party classify-function \
  --addr 0x401000 \
  --classification third-party \
  --evidence "matches zlib inflate path"
ghidra-agent-cli --target sample-target gate check --phase P2
```

If no third-party code is present:

```sh
ghidra-agent-cli --target sample-target third-party none --evidence "review found no library match"
```

## Phase Flow

1. Review imports, strings, constants, types, vtables, functions, and callgraph.
2. Form library/version hypotheses from recorded evidence.
3. Acquire or identify pristine source without executing upstream scripts.
4. Register each accepted library and source path.
5. Classify functions when evidence supports classification.
6. Record explicit no-third-party review when applicable.
7. Append an execution log note for non-obvious decisions.
8. Run the P2 gate.

## Evidence Standards

Use concrete anchors: symbol names, import names, strings, file paths, type
names, version strings, callgraph shape, or runtime traces. Avoid unsupported
claims such as "looks like" without a recorded reason. Confidence should reflect
evidence quality, not convenience.

## Exit Criteria

- `identified.yaml` records libraries when third-party code is present.
- `identified.yaml` records `libraries: []` when none was found.
- Each library has version, confidence, evidence, and local source information.
- Pristine source paths are separated from local adaptation changes.
- P3 has enough classification evidence to recover names, signatures, and types.
- P2 gate passes.

## Blockers

Stop before P3 when:

- Third-party status is ambiguous.
- A library is named without evidence.
- Source has been modified in the pristine directory.
- Version confidence is too weak for downstream source-derived names.
- Function classification contradicts baseline evidence.

## Handoff To P3

After P2 passes, route to `headless-ghidra-discovery`. The handoff should
summarize accepted libraries, uncertain libraries, pristine paths, local
adaptation paths, classified functions, and evidence gaps.
