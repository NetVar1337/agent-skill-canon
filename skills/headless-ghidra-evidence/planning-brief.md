# Evidence Planning Brief

## Brief Metadata

| Field               | Value                                                                                                                               |
| ------------------- | ----------------------------------------------------------------------------------------------------------------------------------- |
| Phase id            | `evidence_replay`                                                                                                                   |
| Purpose             | Define the portable evidence extraction and replay contract used to shape speckit planning and review generated planning artifacts. |
| Primary consumer    | Downstream user                                                                                                                     |
| Constrained outputs | `spec.md`, `plan.md`, `tasks.md`                                                                                                    |
| Audit scope         | `spec.md`, `plan.md`, `tasks.md`                                                                                                    |
| Example handoff     | [Evidence Speckit Handoff](./examples/evidence-speckit-handoff.md)                                                                  |

## When To Use This Phase Skill

Use this brief when the intake phase is already stable and the planning request
must preserve how evidence is extracted, replayed, and validated.

## Non-Negotiable Reverse-Engineering Constraints

- Headless-only scope. Evidence and replay steps must not require a GUI-only
  session.
- Evidence-backed claims. Planned outputs must reference observable artifacts,
  manifests, or replay outputs rather than unsupported assertions.
- Reproducible workflow expectations. The generated plan must preserve enough
  command, input, and artifact detail to replay the workflow.
- Reviewable Markdown outputs. `spec.md`, `plan.md`, and `tasks.md` must keep
  evidence expectations visible in Markdown.
- Frontier-first selection expectations. Planning must preserve frontier
  eligibility, matched-only gating, automatic default selection, helper
  priority, and secondary-metric labeling.
- No downstream extension or constitution change is required. The contract
  remains portable and only allows stricter local overlays.

## Required Planning Inputs

- `target_context`: normalized target and scope from intake
- `evidence_sources`: exports, manifests, decompiled output, or other artifacts
- `replay_surface`: command or manifest expectations needed for regeneration
- `non_negotiable_constraints`: headless-only, evidence-backed,
  reproducibility, and Markdown reviewability requirements
- `validation_expectations`: how reviewers confirm the generated artifacts kept
  the evidence contract intact
- `local_rule_overlay`: optional stricter local rules

## Planning Brief Body

Use this body directly or adapt it with the same meaning:

```md
Prepare planning artifacts for a headless-only Ghidra reverse-engineering
workflow that preserves evidence extraction and replay expectations.

Target context:

- [fill in normalized target]
- [fill in scope already agreed during intake]

Evidence sources:

- [fill in reviewed exports, manifests, or artifact surfaces]

Replay surface:

- [fill in command or manifest expectations needed for regeneration]

Non-negotiable constraints:

- Headless-only workflow. No GUI dependency.
- Evidence-backed claims only.
- Reproducible replay expectations.
- Reviewable Markdown outputs for spec.md, plan.md, and tasks.md.
- Frontier-first selection expectations, matched-only gating, and secondary
  metrics.
- No downstream speckit extension or constitution change required.

Validation expectations:

- A reviewer can identify evidence sources and replay expectations in one pass.
- The generated artifacts preserve those requirements without weakening them.
- The generated artifacts keep outside-in frontier eligibility, matched-only
  advancement, automatic default selection, and secondary metrics visible.
```

## How To Supply The Brief To Speckit

- Supply [`./planning-brief.md`](./planning-brief.md) directly.
- Or paste the `Planning Brief Body` inline into the `speckit` request.
- The transport mode must preserve the same evidence and replay obligations.

## Audit Checklist For Generated Artifacts

- `spec.md` identifies the evidence sources and the reason they matter.
- `plan.md` preserves replayable commands, manifests, or regeneration surfaces.
- `tasks.md` includes work needed to collect, validate, and review evidence in
  Markdown-visible form.
- None of the generated artifacts weaken headless-only, evidence-backed, or
  reproducibility expectations.
- None of the generated artifacts weaken frontier-first selection, matched-only
  gating, or secondary-metric labeling.
- If the evidence contract is weakened, refine or regenerate the planning
  artifacts instead of weakening the contract.

## Local Rule Policy

- Local project rules may require extra artifact review, evidence notes,
  or naming conventions.
- Local project rules may not remove evidence requirements, replace replay
  obligations with informal notes, or hide outputs outside reviewable Markdown.
- Treat stricter local rules as additive overlays.
- Treat any local rule that weakens the evidence contract as invalid.
