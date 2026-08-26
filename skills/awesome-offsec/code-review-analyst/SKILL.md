---
name: code-review-analyst
description: Perform exploit-oriented code review by proving attacker-controlled paths from source to sink and validating impact.
---

# Code Review Analyst

## Purpose
Produce high-confidence, exploitable code findings using source-to-sink proof.

## Inputs
- `code_path`
- `priority_trace_paths`
- `recon_context`
- `runtime_assumptions`

## Analysis Rules
- No sink-only findings.
- No framework-trust assumptions without verification.
- No severity claims without exploitability context.

## Workflow
### Phase 1: Trace Construction
1. Follow untrusted input through transformations.
2. Confirm boundary checks at each transition.
3. Identify bypass opportunities.

### Phase 2: Exploitability Validation
1. Verify attacker control over critical parameters.
2. Verify sink reachability under realistic control flow.
3. Build exploit narrative with prerequisites.

### Phase 3: Vulnerability Class Testing
1. Injection and query/command/template abuse.
2. Access-control bypass (IDOR/BOLA/BFLA).
3. Mass assignment and object property abuse.
4. Path traversal and file handling flaws.
5. Deserialization and parser confusion.
6. Workflow and race-condition vulnerabilities.

### Phase 4: Impact Assessment
1. Data exposure and integrity damage potential.
2. Privilege escalation and lateral movement potential.
3. Blast radius and tenant isolation impact.

### Phase 5: Reporting
1. Provide concise exploit narrative.
2. Provide precise root-cause location.
3. Provide remediation direction tied to trust boundary.

## Required Evidence per Finding
- Source location and attacker input path.
- Sink location and dangerous operation.
- Missing/insufficient control explanation.
- Reproduction logic and impact statement.

## Output Contract
```json
{
  "confirmed_findings": [],
  "exploit_paths": [],
  "impact_assessment": [],
  "remediation_notes": [],
  "confidence": []
}
```

## Failure Modes
- Treating dead code as reachable.
- Confusing validation with authorization.
- Missing multi-step state dependencies.

## Quality Checklist
- [ ] Source-to-sink chain is complete.
- [ ] Reachability is justified.
- [ ] Impact is realistic and bounded.

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
