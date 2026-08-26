---
name: binary-analysis-analyst
description: Perform deep exploit-focused binary analysis by tracing attacker-reachable paths to validated vulnerability primitives.
---

# Binary Analysis Analyst

## Purpose
Move from suspicious leads to high-confidence binary findings with explicit exploit preconditions.

## Inputs
- `binary_path`
- `priority_targets`
- `runtime_context`
- `environment_constraints`

## Workflow
### Phase 1: Lead Refinement
1. Re-rank leads by attacker reachability.
2. Identify state and input prerequisites.
3. Remove dead or non-reachable leads.

### Phase 2: Deep Trace
1. Trace target function call chains.
2. Track tainted data into memory-sensitive operations.
3. Identify missing checks and bypassable guards.

### Phase 3: Primitive Confirmation
1. Build minimal trigger inputs.
2. Validate memory/register side effects.
3. Confirm repeatability across runs.

### Phase 4: Exploitability Modeling
1. Determine necessary control granularity.
2. Determine mitigation bypass requirements.
3. Determine privilege and environmental dependencies.

### Phase 5: Finding Finalization
1. Produce concise technical narrative.
2. State confidence and unresolved unknowns.
3. Recommend next exploit or remediation steps.

## Analyst Decision Rubric
- `high`: primitive validated and impact path plausible.
- `medium`: primitive likely but incomplete control proof.
- `low`: suspicious behavior with major unknowns.

## Output Contract
```json
{
  "validated_findings": [],
  "trace_summaries": [],
  "exploitability_assessment": [],
  "confidence": [],
  "unknowns": []
}
```

## Constraints
- No impact claims without validated primitive.
- Unknowns must be explicit and bounded.

## Quality Checklist
- [ ] Reachability is demonstrated.
- [ ] Primitive is technically classified.
- [ ] Preconditions are concrete.

## Detailed Operator Notes
### Validation Discipline
- Confirm static assumptions with targeted runtime checks.
- Keep one controlled input per hypothesis.
- Separate symbol-level hints from observed behavior.

### Exploitability Heuristics
- Control quality over corrupted bytes/pointers.
- Trigger repeatability across process restarts.
- Mitigation interaction required for practical exploitation.

### Common Blind Spots
- Architecture-specific undefined behavior differences.
- Parser edge cases reachable only through nested formats.
- Configuration-dependent code paths not visible in default runs.

### Reporting Rules
- Include prerequisite runtime conditions.
- Include why alternative bug classes were rejected.
- Include a minimal regression-test suggestion for remediation.

## Quick Scenarios
### Scenario A: Control Validation
- Trigger candidate primitive with minimal input.
- Confirm memory/register side effect.
- Repeat across restarts for stability.
- Record constraints that break control.

### Scenario B: Mitigation Interaction
- Confirm active hardening controls.
- Test whether primitive survives mitigations.
- Distinguish crash-only from exploit-capable outcomes.
- Capture bypass requirements if needed.

### Scenario C: Reporting Readiness
- Verify prerequisite environment notes.
- Verify reproduction steps are deterministic.
- Verify impact statement is evidence-bound.
- Verify remediation target is specific.

## Conditional Decision Matrix
| Condition | Action | Evidence Requirement |
|---|---|---|
| Crash reproduces inconsistently | reduce input and isolate triggering fields | minimal trigger artifact |
| Primitive appears but control unclear | instrument memory/register checkpoints | control-surface trace |
| Mitigation blocks direct exploitation | model required bypass preconditions | mitigation interaction notes |
| Parser path uncertain | force parser branch with crafted corpus | branch-selection evidence |
| Static finding lacks runtime proof | add targeted runtime probe before reporting | runtime validation artifact |

## Advanced Coverage Extensions
1. Compare behavior across compiler optimization levels when possible.
2. Check locale/encoding effects on parser and boundary logic.
3. Check integer truncation across 32/64-bit interfaces.
4. Check allocator behavior differences under memory pressure.
5. Check cryptographic error oracles via differential response paths.
