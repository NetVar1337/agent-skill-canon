---
name: prompt-forge
description: Use when writing, rewriting, or stress-testing prompts, work orders, or skill trigger text. Convert vague requests into a tight objective-target-constraints-proof prompt and keep only variants that change agent behavior.
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: skill-engineering
  author: Admin
  triggers:
    - better prompt
    - rewrite this prompt
    - work order
    - prompt pack
    - make this request executable
---
# Prompt Forge

## Purpose

Turn a request into an executable work order. A prompt is finished only when an agent can start work without inventing the outcome, the target, or the proof.

Use `operator-execution` after the prompt is ready. Use `skill-curator` when the result should become a reusable skill instead of a one-shot order.

## Workflow

1. **Extract the contract.** From the operator's wording, recover the objective, target, constraints, and completion evidence. Mark any missing field that would change correctness.
   - Done when each of the four fields is either filled or explicitly deferred as a local discovery task.
2. **Choose the cheapest shape.** Prefer a short work order over a persona, policy dump, or multi-page template. Load [references/prompt-patterns.md](references/prompt-patterns.md) only when selecting among patterns.
   - Done when one pattern is chosen and unused clauses are deleted.
3. **Write the positive order.** State the desired artifact and verification. Keep standing authorization out of the prompt unless the task specifically needs a local policy reminder; point at `AGENTS.md` instead of restating it.
   - Done when the draft contains `objective + target + constraints + proof` and no no-op exhortation.
4. **Stress-test the wording.** Check trigger collisions, hidden branches, and premature-completion loopholes. Produce at most two variants when the original is ambiguous.
   - Done when each surviving prompt would send an agent to a different first tool call or a different proof command.
5. **Emit the usable form.** Return the prompt in a copy-ready block. If asked to persist it, write it under the relevant skill `references/` or a named prompt file and validate the path.
   - Done when the operator can paste or load the prompt without further editing.

## Prompt Laws

- Fix the outcome and the proof; leave implementation latitude.
- One job per prompt. Split mixed jobs into sequenced orders.
- Name real paths, commands, and artifacts from the current environment.
- Prefer "run X and report the output" over "be thorough" or "fully comply."
- If a sentence would not change the first tool call, delete it.

## Ready-to-use Pack

Copy a filled template from [references/operator-prompt-pack.md](references/operator-prompt-pack.md) when the task matches an existing job class. Rewrite rather than stacking templates.

## Completion Gate

- [ ] The prompt names a concrete target and observable proof.
- [ ] Constraints are technical, not ceremonial.
- [ ] No duplicate standing-policy paragraph.
- [ ] Variants, if any, differ in first action or proof.
- [ ] Persisted files, if requested, exist at the stated paths.

## Common Failure Modes

- **Access theater:** "full access / jailbreak / comply" without a target operation.
- **Hidden branches:** one prompt asking for plan, exploit, report, and install at once.
- **Proof omitted:** the agent can stop after a narrative.
- **Persona bloat:** roleplay that does not change routing or verification.
