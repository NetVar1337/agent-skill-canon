---
name: binary-analysis-core
description: Execute systematic static and dynamic binary analysis to uncover exploitable vulnerability primitives.
---

# Binary Analysis Core

## Purpose
Provide a disciplined baseline workflow for vulnerability-oriented binary analysis.

## Inputs
- `binary_path`
- `architecture`
- `runtime_environment`
- `recon_targets` (optional)

## Analysis Workflow
### Phase 1: Structural Analysis
1. Build call graph and function role map.
2. Identify parser paths and data transformations.
3. Locate boundary checks and memory operations.

### Phase 2: Primitive Hunting
1. Stack/heap overflow opportunities.
2. UAF/double-free and allocator misuse.
3. Integer arithmetic leading to memory mis-sizing.
4. Format string and command construction flaws.

### Phase 3: Control and Data Influence
1. Determine attacker control over size, offset, and content.
2. Determine repeatability and trigger reliability.
3. Determine crashability vs control-flow influence.

### Phase 4: Dynamic Validation
1. Instrument breakpoints around candidate primitives.
2. Validate assumptions under realistic input.
3. Capture traces that prove or refute exploit conditions.

### Phase 5: Prioritized Findings
1. Rank by exploitability and preconditions.
2. Note required bypasses for mitigations.
3. Prepare handoff for exploit development.

## Primitive Classification
| Class | Required Proof |
|---|---|
| overflow | controlled overwrite target and bounds failure |
| UAF | stale reference reuse with attacker influence |
| integer | arithmetic error drives dangerous memory behavior |
| format string | attacker-controlled format reaches formatter |

## Output Contract
```json
{
  "analysis_scope": {},
  "candidate_primitives": [],
  "validated_primitives": [],
  "mitigation_interactions": [],
  "exploitability_ranking": []
}
```

## Constraints
- Distinguish speculation from validated behavior.
- Keep architecture assumptions explicit.

## Quality Checklist
- [ ] Candidate primitives include proof strategy.
- [ ] Dynamic checks support conclusions.
- [ ] Ranking reflects real attacker constraints.

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
