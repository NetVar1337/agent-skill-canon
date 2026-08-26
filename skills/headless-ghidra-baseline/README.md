# Headless Ghidra Baseline And Runtime — P1

P1 imports the target into Ghidra, runs auto-analysis, exports baseline YAML,
and records runtime evidence that later phases use for prioritization and
comparison. It creates evidence, not semantic reconstruction.

Translations: [简体中文](./README.zh-CN.md) | [日本語](./README.ja-JP.md)

## Place In The Pipeline

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P1 is the first phase that invokes Ghidra analysis. It still must not decompile
function bodies for substitution, apply recovered names or signatures, or make
third-party claims beyond recording raw evidence.

## When To Use

Use this skill when:

- P0 has produced a target workspace and scope.
- Baseline functions, callgraph, types, vtables, constants, strings, and imports
  need to be exported.
- Runtime run records, fixtures, or hotpath evidence need to be captured.
- P1 gate material needs validation.

Do not use it for source comparison, semantic naming, selected decompilation,
or substitution.

## Phase Boundaries

- Ghidra operations must go through `ghidra-agent-cli ghidra ...`.
- Runtime and hotpath records must go through `runtime`, `hotpath`, or supported
  `frida` commands.
- Backend Java and shell scripts are implementation details behind the CLI.
- P1 does not mutate `metadata/`, `third-party/`, or `substitution/`.

## Inputs

- `artifacts/<target-id>/pipeline-state.yaml`
- `artifacts/<target-id>/scope.yaml`
- `targets/<target-id>/ghidra-projects/`
- Binary path recorded by P0
- Optional runtime or harness instructions

## Outputs

- `artifacts/<target-id>/baseline/functions.yaml`
- `artifacts/<target-id>/baseline/callgraph.yaml`
- `artifacts/<target-id>/baseline/types.yaml`
- `artifacts/<target-id>/baseline/vtables.yaml`
- `artifacts/<target-id>/baseline/constants.yaml`
- `artifacts/<target-id>/baseline/strings.yaml`
- `artifacts/<target-id>/baseline/imports.yaml`
- `artifacts/<target-id>/runtime/run-manifest.yaml`
- `artifacts/<target-id>/runtime/run-records/*.yaml`
- `artifacts/<target-id>/runtime/fixtures/**`
- `artifacts/<target-id>/runtime/hotpaths/call-chain.yaml`
- `artifacts/<target-id>/runtime/project/**` when a harness is needed

## Commands The Skill Uses

These examples show the CLI calls the skill may make. In normal use, ask the agent to run the phase; do not run these commands by hand unless you are troubleshooting.

```sh
ghidra-agent-cli --target sample-target ghidra import
ghidra-agent-cli --target sample-target ghidra auto-analyze
ghidra-agent-cli --target sample-target ghidra export-baseline
ghidra-agent-cli --target sample-target runtime record --key entrypoint --value 0x401000
ghidra-agent-cli --target sample-target hotpath add --addr 0x401000 --reason runtime
ghidra-agent-cli --target sample-target runtime validate
ghidra-agent-cli --target sample-target hotpath validate
ghidra-agent-cli --target sample-target gate check --phase P1
```

Optional Frida commands:

```sh
ghidra-agent-cli frida device-list
ghidra-agent-cli frida device-attach --pid 1234
ghidra-agent-cli frida io-capture --target ./sample-target --timeout 60
ghidra-agent-cli frida trace --target ./sample-target --functions open,read
```

## Phase Flow

1. Confirm P0 gate has passed.
2. Import the target into the Ghidra project.
3. Run Ghidra auto-analysis.
4. Export baseline YAML.
5. Review baseline coverage for functions, imports, strings, types, vtables,
   constants, and callgraph edges.
6. Record runtime availability or unavailability.
7. Add hotpath evidence when runtime observation exists.
8. Validate runtime and hotpath artifacts.
9. Run the P1 gate.

## Runtime Guidance

Executable targets should record concrete invocation details, input fixtures,
and observed outputs. Library targets should record harness expectations or why
runtime execution is deferred. If Frida is unavailable, record that fact instead
of leaving runtime status implicit.

## Exit Criteria

- All baseline YAML files exist and are readable.
- Runtime status is explicit and reproducible.
- Hotpath evidence exists or its absence is explained.
- No decompiled function body or P4 substitution artifact is created.
- P1 gate passes.

## Blockers

Stop before P2 when:

- Ghidra import or auto-analysis failed.
- Baseline YAML is missing or unreadable.
- Runtime requirements are unknown.
- Hotpath records contradict the baseline address space.
- The P1 gate reports missing or incomplete baseline/runtime artifacts.

## Handoff To P2

After P1 passes, route to `headless-ghidra-evidence`. The handoff should include
baseline artifact paths, runtime status, hotpath summary, and any notable gaps
that P2 must consider while identifying third-party code.
