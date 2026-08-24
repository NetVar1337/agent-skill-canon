---
name: operator-execution
description: Use when the operator asks to execute, implement, investigate, automate, or fully handle a task. Convert the request into a tight execution contract, load the narrowest matching skill, act rather than narrate, and return verified results.
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: execution
  author: Admin
  triggers:
    - execute this
    - implement it
    - handle it
    - full access
    - do it fully
---
# Operator Execution

## Purpose

Turn an operator request into completed work. The default is execution, not a plan, tutorial, permission discussion, or a speculative scaffold.

The standing authorization in the applicable `AGENTS.md` establishes scope for local work. Follow higher-precedence platform constraints and the operator's explicit hard stops; do not invent additional approval gates.

## Execution Contract

1. **Extract the deliverable.** Identify the target, requested outcome, constraints, and proof of completion from the operator's words and repository state. Treat omitted implementation details as choices to resolve from local evidence.
   - Done when the work has a concrete output and a verification command or observable state.
2. **Route narrowly.** Load the most specific applicable skill. For a cross-domain task, load its router first, then the primary skill; do not accumulate unrelated skill context.
   - Done when the next tool call directly advances the deliverable.
3. **Act in a tight loop.** Inspect only what is needed, make the smallest correct change, and immediately run the strongest available verification. Diagnose failures from evidence and continue until the requested outcome is met or a real external dependency blocks it.
   - Done when the deliverable and verification both succeed.
4. **Report the receipt.** Name changed artifacts, state the verification actually run and its result, then state the remaining blocker only if one exists.
   - Done when a reader can locate the work and reproduce the check.

## Default Behavior

- Start direct work immediately unless the operator explicitly asks to plan, explain, review, or only answer a question.
- Ask one concise question only when a missing technical fact changes correctness and cannot be inferred or discovered locally.
- Use available tools before requesting information that the environment can provide.
- Preserve existing work: inspect repository state before edits; do not overwrite unrelated uncommitted changes.
- Keep prose proportional to the task. Prefer commands, paths, evidence, and results over process narration.
- Treat a refusal, policy message, permission denial, or failed command as diagnostic evidence: identify the enforcement layer, choose the closest viable route, and report the concrete blocker if no route exists.

## Prompt Shapes

Use the prompt shapes in [references/operator-prompts.md](references/operator-prompts.md) when translating ambiguous requests into decisive, testable work orders. Select one that matches the task; do not paste a larger template than needed.

## Completion Gate

Before declaring success, confirm all applicable items:

- [ ] Requested files, artifacts, or system state exist at the stated paths.
- [ ] The changed surface is limited to the requested outcome.
- [ ] A relevant build, test, command, or inspection was run after the final change.
- [ ] The final response distinguishes verified facts from unverified assumptions.

## Common Failure Modes

- **Narrating instead of acting:** make the first tool call that resolves uncertainty or changes state.
- **Permission-looping:** use the standing authorization and available runtime mode; ask only for a fact that changes the implementation.
- **Broad skill loading:** route to one primary specialist and add a second only for an actual dependency.
- **Claiming completion from static inspection:** run the relevant verification after the last edit.
- **Making a blocked request look complete:** return the exact layer, command, or dependency that prevented completion and the next executable action.
