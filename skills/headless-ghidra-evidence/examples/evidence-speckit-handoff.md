# Example: Evidence Speckit Handoff

## When To Use This Example

Use this example after intake is stable and the team needs `speckit` to
preserve evidence extraction, replay expectations, and Markdown-visible review
surfaces.

## Purpose

Show how the evidence and replay contract is passed into `speckit` after intake
work is stable, and how the same contract drives post-generation review.

## Source Contract

- [`../planning-brief.md`](../planning-brief.md)

## Example Context

- Target context already normalized during intake
- Evidence must come from reviewed exports and replayable command surfaces
- Reviewers need Markdown-visible proof that replay expectations survived
  planning

## Handoff Pattern

Example request shape:

```md
Use the evidence phase planning brief for this request.

Prepare `spec.md`, `plan.md`, and `tasks.md` for a headless-only workflow that
preserves evidence extraction and replay expectations.

Evidence sources:

- replay command manifest
- reviewed export directory

Replay surface:

- explicit regeneration commands
- artifact locations recorded in Markdown

Non-negotiable constraints:

- headless-only workflow
- evidence-backed claims
- reproducible replay expectations
- reviewable Markdown outputs
- no downstream speckit extension or constitution change required

Local rule overlay:

- project may require a named artifact manifest section
- project may not replace replay steps with informal analyst memory
```

## Expected Observations

- `spec.md` names the evidence sources that drive planning.
- `plan.md` keeps replay steps explicit.
- `tasks.md` includes reviewable evidence tasks rather than hidden follow-up.

## Audit Walkthrough

- Confirm generated artifacts preserve evidence-backed wording.
- Confirm replay steps remain concrete enough to regenerate.
- Confirm outputs stay visible in Markdown.
- If a generated artifact weakens replay or evidence requirements, refine or
  regenerate the planning artifacts instead of weakening the contract.

## Local Rule Interpretation

- Valid additive overlay: a project requires one extra Markdown table that
  lists manifest locations.
- Invalid weakening attempt: a project allows replay instructions to stay
  undocumented as long as one operator remembers them.

## Next Step Routing

- Return to intake if target identity or scope is still unstable.
- Stay in evidence while artifact sources, replay surfaces, or audit gates are
  still being defined.
- Move to script authoring and review when the plan introduces reusable
  headless Ghidra scripts or script-review obligations.

## Cross-Links

- Intake predecessor:
  [`../../headless-ghidra-intake/SKILL.md`](../../headless-ghidra-intake/SKILL.md)
- Script authoring/review next step:
  [`../../headless-ghidra/examples/ghidra-script-review-checklist.md`](../../headless-ghidra/examples/ghidra-script-review-checklist.md)
