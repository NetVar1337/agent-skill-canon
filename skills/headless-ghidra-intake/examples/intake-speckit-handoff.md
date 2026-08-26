# Example: Intake Speckit Handoff

## When To Use This Example

Use this example when the team still needs to normalize the target, define
project initialization expectations, and make the first `speckit` handoff
portable across workspaces.

## Purpose

Show how the intake phase contract is carried into `speckit` without requiring
any downstream extension or constitution edit, and how the same contract is
used immediately afterward for audit.

## Source Contract

- [`../planning-brief.md`](../planning-brief.md)

## Example Context

- Target: `sample-target.bin`
- Goal: define initial scope, setup assumptions, and expected planning outputs
- Known constraint: the workflow must stay headless-only and evidence-backed

## Handoff Pattern

Example request shape:

```md
Use the intake phase planning brief for this request.

Prepare `spec.md`, `plan.md`, and `tasks.md` for a headless-only Ghidra
reverse-engineering workflow.

Target identity:

- sample-target.bin from the reviewed intake bundle

Workflow scope:

- normalize the target
- capture initialization assumptions
- preserve setup details needed for later evidence extraction

Non-negotiable constraints:

- headless-only workflow
- evidence-backed claims
- reproducible setup expectations
- reviewable Markdown outputs
- no downstream speckit extension or constitution change required

Local rule overlay:

- local project rules may require an extra intake summary section
- local project rules may not replace the intake contract with GUI notes or informal
  setup steps
```

## Expected Observations

- The generated planning artifacts keep the intake constraints explicit.
- The artifacts remain portable across workspaces.
- A reviewer can understand the target and scope in one pass.

## Audit Walkthrough

- Check `spec.md` for target identity and scope boundaries.
- Check `plan.md` for replayable setup assumptions.
- Check `tasks.md` for Markdown-reviewable deliverables and no GUI-only steps.
- If any generated artifact drops a non-negotiable intake constraint, refine or
  regenerate the planning artifacts rather than weakening the phase contract.

## Local Rule Interpretation

- Valid additive overlay: a local project requires one more Markdown checklist
  item describing analyst ownership.
- Invalid weakening attempt: a local project says target setup can be captured as
  informal GUI notes after planning.

## Next Step Routing

- Stay with intake if target identity, scope, or deliverable definitions are
  still moving.
- Move to evidence when replay surfaces, artifact capture, or validation gates
  need to become explicit.
- Move to script authoring and review when the plan introduces reusable
  headless Ghidra scripts.

## Cross-Links

- Umbrella routing:
  [`../../headless-ghidra/SKILL.md`](../../headless-ghidra/SKILL.md)
- Evidence next step:
  [`../../headless-ghidra-evidence/SKILL.md`](../../headless-ghidra-evidence/SKILL.md)
