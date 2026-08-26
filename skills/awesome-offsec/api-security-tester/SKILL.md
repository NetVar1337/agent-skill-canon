---
name: api-security-tester
description: Execute end-to-end API security testing from attack-surface mapping through validated findings and remediation notes.
---

# API Security Tester

## Purpose
Run a complete API assessment cycle with strong evidence discipline and predictable output.

## Inputs
- `target_base_url`
- `api_spec_or_collection`
- `auth_context`
- `engagement_rules`

## Standard Test Order
1. Discovery and endpoint normalization.
2. Auth and authorization checks.
3. Input handling and injection checks.
4. Workflow and state-machine abuse checks.
5. Impact confirmation and verification.

## Execution Workflow
### Phase 1: Discovery
- Build endpoint and trust map.
- Confirm content types, schema validation, and versioning.
- Identify sensitive operations and privileged paths.

### Phase 2: Access Control
- Test object-level access control.
- Test function-level authorization by role.
- Test tenant boundary isolation.

### Phase 3: Input Abuse
- Injection candidates by sink class.
- Mass assignment on create/update.
- Filter/operator abuse on search APIs.

### Phase 4: Workflow Abuse
- Bypass prerequisite steps.
- Replay or reorder transitions.
- Abuse bulk and async operations.

### Phase 5: Verification
- Independently confirm positives.
- Capture remediation-relevant root cause.
- Downgrade or dispute weak findings.

## Minimum Test Matrix
| Category | Required Assertions |
|---|---|
| Authentication | unauthenticated access rejected consistently |
| Authorization | foreign objects and privileged actions are blocked |
| Input validation | malformed and malicious payloads handled safely |
| Error handling | no internal leakage in error bodies |
| State transitions | invalid transitions rejected |
| Rate limiting | sensitive operations throttled |

## Output Contract
```json
{
  "scope_summary": {},
  "test_log": [],
  "confirmed_vulnerabilities": [],
  "verification_notes": [],
  "remediation_guidance": []
}
```

## Constraints
- Keep tests reproducible and proportional.
- Do not overclaim severity without business impact.

## Quality Checklist
- [ ] Coverage includes auth, authz, input, workflow.
- [ ] Findings include clear exploit path.
- [ ] Remediation ties to code/control failure.

## Detailed Operator Notes
### Reproducibility Standard
- Replay each confirmed case in a fresh session.
- Replay with at least one payload or transport variant.
- Keep one negative control request for every positive claim.

### False-Positive Controls
- For timing signals, compare against matched control payloads.
- For authz signals, verify with ownership-correct and ownership-incorrect objects.
- For parser signals, verify semantic effect, not just error shape changes.

### Severity Calibration Inputs
- Required attacker privilege.
- Cross-tenant or single-tenant impact.
- Ability to automate at scale.
- Degree of data sensitivity.

### Reporting Rules
- Include exact request signatures (method, path, key headers, payload hash).
- Include verification run count and consistency notes.
- Include why alternative explanations were rejected.

## Conditional Decision Matrix
| Condition | Action | Evidence Requirement |
|---|---|---|
| Endpoint undocumented but reachable | Add to inventory and prioritize authz checks | request/response baseline + auth behavior |
| Auth behavior inconsistent across methods | Split tests by method and content type | per-method status + body signatures |
| Time-based anomaly only | run matched control timing series | repeated control/test timing traces |
| Object access differs by role | escalate to cross-tenant/cross-role checks | role-tagged replay proof |
| Validation differs by parser | run semantic-equivalent content-type tests | parser-path differential evidence |

## Advanced Coverage Extensions
1. Add negative-object tests for soft-deleted or archived resources.
2. Add replay-window tests for idempotency and duplicate processing.
3. Add bulk endpoint abuse tests for partial authorization failures.
4. Add asynchronous job handoff checks for stale permission snapshots.
5. Add pagination/filter abuse checks for hidden data exposure.
