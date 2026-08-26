---
name: memory-safety-analyst
description: Classify memory-safety defects, evaluate exploitability, and prioritize remediation based on primitive quality and mitigation interaction.
---

# Memory Safety Analyst

## Purpose
Provide precise, exploit-informed analysis of memory corruption and memory safety failures.

## Inputs
- `target_context` (binary or source)
- `crash_data` (trace, core, sanitizer output)
- `mitigation_profile`

## Workflow
### Phase 1: Fault Classification
1. Identify bug class:
- stack overflow
- heap overflow
- out-of-bounds read/write
- use-after-free
- double free
- integer-driven misallocation
2. Identify crash point and corruption origin.

### Phase 2: Control Surface Analysis
1. Determine control over corrupted bytes.
2. Determine control over target object or return/control metadata.
3. Determine trigger repeatability.

### Phase 3: Mitigation Interaction
1. Evaluate hardening controls in effect.
2. Evaluate whether bug class can bypass controls.
3. Separate crash-only from exploit-capable conditions.

### Phase 4: Exploitability Grading
1. `E0`: non-exploitable with current evidence.
2. `E1`: crashable, limited attacker control.
3. `E2`: meaningful data or pointer control.
4. `E3`: viable control-flow or sensitive data compromise path.

### Phase 5: Remediation Prioritization
1. Recommend root-cause fix.
2. Recommend short-term guardrails.
3. Recommend regression tests.

## Required Evidence
- faulting instruction context
- memory state before/after trigger
- control granularity description
- mitigation interaction notes

## Output Contract
```json
{
  "bug_classification": {},
  "control_surface": {},
  "mitigation_analysis": {},
  "exploitability_grade": "",
  "remediation_plan": []
}
```

## Constraints
- Use exact primitive terminology.
- Avoid overstating exploitability from a single crash.

## Quality Checklist
- [ ] Classification is unambiguous.
- [ ] Control analysis is evidence-backed.
- [ ] Remediation targets root cause.

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
