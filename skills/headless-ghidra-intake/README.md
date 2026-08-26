# Headless Ghidra Intake — P0

P0 turns a user-provided binary or archive member into a valid
`ghidra-agent-cli` target. It establishes target identity, verifies the local
tooling, initializes the workspace, and records the analysis scope before any
Ghidra analysis runs.

Translations: [简体中文](./README.zh-CN.md) | [日本語](./README.ja-JP.md)

## Place In The Pipeline

```text
P0 Intake -> P1 Baseline+Runtime -> P2 Third-Party -> [P3 Metadata -> P4 Substitution]*
```

P0 is a preparation phase. It must not import the binary into Ghidra for
analysis, export baseline metadata, apply names or signatures, or decompile
functions.

## When To Use

Use this skill when:

- A new target binary, library, object file, or accepted archive member needs a
  workspace.
- An existing run needs target identity, binary path, or scope confirmed.
- Ghidra discovery or binary inspection has not been recorded.
- Scope needs to be set before P1 can start.

Do not use it for baseline export, third-party classification, metadata
recovery, or function substitution.

## Phase Boundaries

- Use this README for normal phase review.
- Exact agent workflow rules live in [headless-ghidra/SKILL.md](../headless-ghidra/SKILL.md) when troubleshooting requires them.
- Exact helper command details live in [ghidra-agent-cli/SKILL.md](../ghidra-agent-cli/SKILL.md) when troubleshooting requires them.
- State owner: `artifacts/<target-id>/pipeline-state.yaml` and
  `artifacts/<target-id>/scope.yaml`.
- Analysis backend: none in this phase.

If the CLI lacks a capability needed for intake, pause and decide explicitly
before adding helper scripts. Do not create or run new Ghidra scripts in P0.

## Inputs

- User-provided binary or archive path.
- Workspace root.
- Optional preferred target id.
- Local environment needed for Ghidra discovery.
- User guidance about whole-target, address, symbol, or function scope.

For archive inputs, carry forward only accepted extracted member paths and keep
archive provenance attached to the chosen target id.

## Outputs

- `targets/<target-id>/ghidra-projects/`
- `artifacts/<target-id>/pipeline-state.yaml`
- `artifacts/<target-id>/scope.yaml`
- Optional intake notes under `artifacts/<target-id>/intake/`

## Commands The Skill Uses

These examples show the CLI calls the skill may make. In normal use, ask the agent to run the phase; do not run these commands by hand unless you are troubleshooting.

```sh
ghidra-agent-cli ghidra discover
ghidra-agent-cli inspect binary --target ./sample-target
ghidra-agent-cli workspace init --target sample-target --binary ./sample-target
ghidra-agent-cli --target sample-target scope show
ghidra-agent-cli --target sample-target scope set --mode full
ghidra-agent-cli --target sample-target gate check --phase P0
```

Additional scope commands:

```sh
ghidra-agent-cli --target sample-target scope add-entry --entry 0x401000
ghidra-agent-cli --target sample-target scope remove-entry --entry 0x401000
```

## Phase Flow

1. Choose a stable target id.
2. Run `ghidra discover` and record the discovered installation path.
3. Run `inspect binary` on the actual input that will be analyzed.
4. Initialize the workspace with `workspace init`.
5. Set scope to full target or explicit entries.
6. Review `pipeline-state.yaml` and `scope.yaml`.
7. Run `gate check --phase P0`.

## Scope Guidance

Use `--mode full` when the target should be analyzed as a whole. Use explicit
entries when a review is intentionally limited to known addresses, functions,
symbols, or archive members. Scope must be understandable from the YAML alone;
do not rely on conversation-only rationale.

## Exit Criteria

- Target workspace exists and is addressable with `--target <id>`.
- `pipeline-state.yaml` records the selected binary path.
- `scope.yaml` records whole-target scope or explicit entries.
- Ghidra discovery has been run and reviewed.
- Binary inspection has been run and reviewed.
- P0 gate passes.

## Blockers

Stop before P1 when:

- The binary path is ambiguous or no longer exists.
- Ghidra cannot be discovered.
- Scope is empty or contradicts the user request.
- Archive normalization did not produce accepted members.
- The P0 gate reports missing or incomplete intake artifacts.

## Handoff To P1

After P0 passes, route to `headless-ghidra-baseline`. The P1 handoff should
include the target id, binary path, scope summary, Ghidra discovery result, and
any archive provenance.
