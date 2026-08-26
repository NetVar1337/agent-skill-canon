---
name: binary-recon
description: Perform fast binary reconnaissance to profile architecture, hardening, interfaces, and high-value analysis targets.
---

# Binary Recon

## Purpose
Create a reliable initial profile that drives deeper vulnerability analysis.

## Inputs
- `binary_path`
- `target_platform`
- `runtime_assumptions` (optional)

## Workflow
### Phase 1: Metadata and Build Context
1. Determine architecture, endianness, and binary format.
2. Identify static vs dynamic linking and dependency footprint.
3. Record compiler and build artifacts when detectable.

### Phase 2: Hardening Profile
1. Check PIE, NX, RELRO, stack canary, Fortify.
2. Check symbol stripping and debug artifact presence.
3. Check obvious sandboxing or seccomp hints.

### Phase 3: Interface Discovery
1. Enumerate exported/imported functions.
2. Extract protocol and command strings.
3. Identify input channels: argv, env, file parsers, network listeners.

### Phase 4: Hotspot Prioritization
1. Parser-heavy code and format handlers.
2. Memory-manipulation and boundary logic.
3. Auth and crypto decision paths.
4. Dangerous call clusters.

### Phase 5: Recon Handoff
1. Build prioritized function list.
2. Add rationale for each priority target.
3. Define proof requirements for deep analysis.

## Recon Artifacts
| Artifact | Why It Matters |
|---|---|
| hardening matrix | exploitability baseline |
| symbol/function map | navigation and targeting |
| string corpus | protocol and feature hints |
| risky function clusters | likely vulnerability density |

## Output Contract
```json
{
  "binary_profile": {},
  "hardening_matrix": {},
  "interface_map": [],
  "priority_targets": [],
  "deep_analysis_requirements": []
}
```

## Constraints
- Keep recon low-cost and repeatable.
- Do not produce exploit claims in recon.

## Quality Checklist
- [ ] Hardening profile is complete.
- [ ] Input channels are mapped.
- [ ] Priority targets are justified.

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
