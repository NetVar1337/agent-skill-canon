# Intake Planning Brief

## Brief Metadata

| Field               | Value                                                                                                                          |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Phase id            | `intake_init`                                                                                                                  |
| Purpose             | Define the portable intake and initialization contract used to shape speckit planning and review generated planning artifacts. |
| Primary consumer    | Downstream user                                                                                                                |
| Constrained outputs | `spec.md`, `plan.md`, `tasks.md`                                                                                               |
| Audit scope         | `spec.md`, `plan.md`, `tasks.md`                                                                                               |
| Example handoff     | [Intake Speckit Handoff](./examples/intake-speckit-handoff.md)                                                                 |

## When To Use This Phase Skill

Use this brief before deeper reverse-engineering work starts, when the team
needs to normalize the target, define the project-init surface, and make the
first planning request portable across workspaces.

## Non-Negotiable Reverse-Engineering Constraints

- Headless-only scope. Do not depend on GUI walkthroughs or manual Ghidra UI
  exploration.
- Evidence-backed claims. Intake facts must come from the provided target,
  reviewed manifests, or other observable evidence.
- Reproducible workflow expectations. The planning artifacts must preserve
  replayable setup assumptions, workspace naming, and analyst inputs.
- Reviewable Markdown outputs. The resulting `spec.md`, `plan.md`, and
  `tasks.md` must be readable and reviewable as Markdown.
- No downstream extension or constitution change is required. The brief must
  remain usable as-is in another project, with local rules only able to
  tighten the contract.

## Required Planning Inputs

- `target_identity`: target name, sample type, and provenance
- `workflow_scope`: what the intake phase needs the plan to cover
- `non_negotiable_constraints`: headless-only, evidence-backed,
  reproducibility, and Markdown reviewability requirements
- `deliverable_types`: which planning artifacts are expected from `speckit`
- `validation_expectations`: what a reviewer must be able to confirm after
  generation
- `local_rule_overlay`: optional local rules that supplement or tighten the
  contract without weakening it

## Planning Brief Body

Use this body directly or adapt it with the same meaning:

```md
Prepare planning artifacts for a headless-only Ghidra reverse-engineering
workflow.

Target identity:

- [fill in normalized target name]
- [fill in sample or binary provenance]

Workflow scope:

- Define intake and initialization expectations for the target.
- Keep project setup reproducible and suitable for later evidence extraction.

Non-negotiable constraints:

- Headless-only workflow. No GUI dependency.
- Evidence-backed claims only.
- Reproducible setup and replay expectations.
- Reviewable Markdown outputs for spec.md, plan.md, and tasks.md.
- No downstream speckit extension or constitution change required.

Deliverable types:

- spec.md
- plan.md
- tasks.md

Validation expectations:

- A reviewer can identify the target, scope, and setup assumptions in one pass.
- The generated artifacts preserve the intake constraints without weakening
  them.
```

## How To Supply The Brief To Speckit

- Supply [`./planning-brief.md`](./planning-brief.md) as the canonical artifact.
- Or paste the `Planning Brief Body` inline into the `speckit` request.
- The transport mode must not change the contract meaning or remove any intake
  constraint.

## Audit Checklist For Generated Artifacts

- `spec.md` preserves target identity, scope boundaries, and deliverable types.
- `plan.md` keeps setup assumptions replayable and consistent with a
  headless-only workflow.
- `tasks.md` reflects reviewable Markdown outputs rather than hidden steps or
  GUI-only actions.
- None of the generated artifacts imply that a downstream extension or
  constitution change is required.
- If a generated artifact weakens the intake contract, the reviewer must refine
  or regenerate the planning artifacts rather than soften this contract.

## Local Rule Policy

- Local project rules may add stricter naming, validation, or documentation
  requirements.
- Local project rules may not replace or weaken the intake contract's
  headless-only, evidence-backed, reproducible, or Markdown-reviewable
  constraints.
- Treat a stricter local rule as an additive overlay.
- Treat any attempt to remove or soften these constraints as an invalid
  weakening attempt.
