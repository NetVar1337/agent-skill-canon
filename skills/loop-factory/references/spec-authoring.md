# Writing Specs

A spec is one markdown file in `factory/specs/inbox/`. It has YAML frontmatter and a structured body. The spec is the source of truth for the whole loop — the implementation prompt, the verification, and the review all derive from it.

## Frontmatter

```yaml
---
id: lf-0001                      # unique id, also the filename stem
title: Add email login           # short, human title
agent: any                       # codex | claude | any
risk: medium                     # low | medium | high — how much caution/review
grill: required                  # required | completed
verification:                    # commands that PROVE the work is done
  - python3 -m unittest discover
  - python3 bin/loop-factory doctor
---
```

`verification` is the most important field after the criteria. These are the exact commands the implementer must run and the reviewer must see passing. Make them runnable and deterministic.

## Body sections

### `# Grill Gate`
Fill this in *before* dispatching. Interrogate the spec one question at a time and record the decisions:

- Who owns this decision?
- What user/customer problem does this solve, and why now?
- What is explicitly out of scope?
- What would make this spec fail review?
- Which assumption is riskiest?
- What is the smallest acceptable implementation?

`loop-factory doctor` flags specs that still lack a grill gate. Skipping it is the single biggest cause of off-target agent output.

### `# Context`
What problem is being solved and why now. Enough for someone new to understand the motivation.

### `# Acceptance Criteria`
The contract. A specific, checkable list. Each item should be something a reviewer can verify as done or not done — ideally tied to a verification command.

```markdown
# Acceptance Criteria

- `loop-factory scan` lists this spec while it is in inbox.
- `loop-factory dispatch --stage` moves it to active and writes a prompt.
- Unit tests cover frontmatter parsing and prompt creation.
```

### `# Constraints`
What *not* to do. Scope guards.

```markdown
# Constraints

- Do not add external runtime dependencies.
- Keep changes scoped to the spec.
- Automate implementation, not product decisions.
```

### `# Review Notes`
What the reviewer should inspect, plus known risks and edge cases.

## Full example

```markdown
---
id: lf-0002
title: Add JSON output to scan
agent: any
risk: low
grill: completed
verification:
  - python3 -m unittest discover
---

# Grill Gate

- Owner: repository operator.
- Problem: tooling needs machine-readable queue state for CI.
- Out of scope: changing the human table format.
- Review failure: JSON missing id/state/agent fields, or tests fail.
- Riskiest assumption: callers want a stable key order.
- Smallest acceptable: add a --json flag to the existing scan command.

# Context

CI needs to read the spec queue programmatically. The current scan output is human-only.

# Acceptance Criteria

- `loop-factory scan --json` prints a JSON array of specs.
- Each entry includes id, state, agent, and title.
- Output is sorted by a stable key.
- A unit test asserts the JSON shape.

# Constraints

- Do not change the default human output.
- No new dependencies.

# Review Notes

- Confirm key ordering is deterministic.
- Check empty-queue behavior returns `[]`, not an error.
```

## Tips

- One spec = one shippable change. If it needs sub-tasks, it's probably two specs.
- Write acceptance criteria you could hand to a stranger and get the same result.
- Copy `factory/templates/spec.md` from the Loop-Factory repo as a starting point.
