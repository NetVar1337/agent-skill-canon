---
name: code-review-verifier
description: Independently validate code-review findings with alternate methodology, severity calibration, and blind-spot discovery.
---

# Code Review Verifier

## Purpose
Increase finding quality by reducing false positives and identifying missed sibling issues.

## Inputs
- `reported_findings`
- `code_path`
- `original_analysis_notes` (optional)

## Verification Rules
- A disputed verdict requires concrete counter-evidence.
- Inconclusive means blocker exists, not analyst disagreement.
- Severity must reflect actual reachable impact.

## Workflow
### Phase 1: Independent Re-trace
1. Reconstruct each path without reusing original assumptions.
2. Validate source control and sink reachability.

### Phase 2: Mitigation Audit
1. Confirm sanitization is context-correct.
2. Confirm authz checks are in enforceable location.
3. Confirm defensive code is not bypassable by alternate path.

### Phase 3: Severity Recalibration
1. Recalculate exploit preconditions.
2. Recalculate required privilege.
3. Recalculate business impact and blast radius.

### Phase 4: Adjacent Pattern Hunt
1. Search for sibling sinks and parallel routes.
2. Compare validation/auth patterns across files.
3. Add missed findings when evidence is sufficient.

## Verdict Model
- `confirmed`: reproducible and exploitable.
- `disputed`: mitigation or non-reachability proven.
- `inconclusive`: technical blocker or uncertainty remains.

## Output Contract
```json
{
  "verification_results": [],
  "severity_changes": [],
  "new_related_findings": [],
  "evidence_notes": [],
  "open_questions": []
}
```

## Quality Checklist
- [ ] Every verdict has explicit evidence.
- [ ] Severity changes are justified.
- [ ] Blind-spot scan completed.

## Detailed Operator Notes
### Cross-Layer Trace Requirements
- Include controller, service, data access, and sink layers.
- Include serialization/deserialization boundary handling.
- Include async boundaries (queue/job/event) where data crosses trust zones.

### Access-Control Audit Rules
- Verify policy check location relative to resource fetch.
- Verify policy check occurs on every variant path.
- Verify tenant scoping is enforced at data query layer.

### Sanitization Audit Rules
- Context-match sanitizer to sink type.
- Confirm canonicalization happens before validation.
- Check for alternate branch paths that skip sanitizer.

### Reporting Rules
- Include function-level path with file and symbol names.
- Include bypass narrative for missing or weak control.
- Include a precise fix location and test recommendation.

## Quick Scenarios
### Scenario A: Access Check Placement
- Trace data fetch point.
- Trace policy check point.
- Determine whether check occurs before use.
- Identify alternate path without check.

### Scenario B: Sanitization Mismatch
- Map sink execution context.
- Map sanitizer type and location.
- Validate context compatibility.
- Find branch that bypasses sanitizer.

### Scenario C: Adjacent Pattern Sweep
- Identify sibling handlers/sinks.
- Compare guard and validation parity.
- Flag inconsistent control patterns.
- Prioritize high-impact siblings.

## Conditional Decision Matrix
| Condition | Action | Evidence Requirement |
|---|---|---|
| Source passes through helper wrappers | inline helper logic into trace | wrapper-expanded path |
| Policy check exists after data fetch | test prefetch exposure and side-effects | order-of-operations trace |
| Sanitizer exists but context mismatch | craft context-correct exploit hypothesis | sink-context mismatch proof |
| Async boundary carries tainted data | trace serialization and consumer validation | producer-consumer trace |
| Sibling route has weaker guards | run parity scan across sibling handlers | guard parity matrix |

## Advanced Coverage Extensions
1. Compare DTO/schema validation between create and update paths.
2. Scan migration scripts and admin tasks for latent unsafe operations.
3. Validate cache-layer authorization consistency.
4. Validate feature-flagged code paths for missing controls.
5. Validate error handling paths for secret leakage.
