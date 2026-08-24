# CLI Command Reference

All commands operate on the git repo you run them from. The CLI finds the repo root by walking up to the nearest `.git`, then works inside `factory/`.

Run as `loop-factory <command>` (installed) or `python3 bin/loop-factory <command>` (from a checkout).

## `init`
Create the `factory/` directory layout. Idempotent.
```bash
loop-factory init
```

## `scan`
List inbox and active specs as `state  id  agent  title`.
```bash
loop-factory scan
loop-factory scan --json     # machine-readable
```

## `dispatch`
Generate an implementation prompt for inbox specs and record a run manifest. Optionally stage and/or execute.
```bash
loop-factory dispatch --agent claude --stage
```
| Flag | Default | Meaning |
|---|---|---|
| `--agent {codex,claude}` | `codex` | which agent the prompt targets |
| `--limit N` | `1` | how many inbox specs to process |
| `--stage` | off | move each processed spec from `inbox/` to `active/` |
| `--execute` | off | actually run the local agent CLI after writing the prompt (also implies staging) |

Without `--execute` it only writes files: a prompt in `factory/prompts/` and a manifest in `factory/runs/`.

## `prompt`
Generate a single implementation prompt for a specific spec, without staging or manifests.
```bash
loop-factory prompt factory/specs/inbox/lf-0001.md --agent claude
```
Accepts a path, a spec id, or a filename stem.

## `review`
Generate a review prompt for an active spec.
```bash
loop-factory review lf-0001 --agent claude
loop-factory review lf-0001 --agent claude --execute   # run the agent on it
```

## `archive`
Move an accepted active spec to `factory/specs/archive/<year>/` and write a review record. `--accepted` is mandatory.
```bash
loop-factory archive lf-0001 --accepted
```
Only `active` specs can be archived. There is no flag to archive without acceptance — that is by design.

## `backprop`
Capture the current `git diff` and generate a prompt asking the agent to update specs/docs to match the implementation, without changing product intent.
```bash
loop-factory backprop --agent claude
loop-factory backprop --agent claude --execute
loop-factory backprop --agent claude --max-diff-bytes 120000   # default 80000; large diffs are truncated
```

## `doctor`
Health-check the setup: repo/factory roots, presence of `git`/`codex`/`claude` on PATH, inbox/active counts, and specs missing a grill gate.
```bash
loop-factory doctor
loop-factory doctor --json
```

## State transitions at a glance
```
inbox ──dispatch --stage──► active ──archive --accepted──► archive/<year>
              │                  │
         (writes prompt)    (failed review stays active)
              └──────► backprop ──► specs/docs updated
```
