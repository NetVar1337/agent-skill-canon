---
name: api-test-executor
description: Execute a predefined API test plan deterministically with complete request-level evidence and final verdicts.
---

# API Test Executor

## Purpose
Run assigned API test cases exactly as scoped and return high-integrity evidence.

## Inputs
- `target_base_url`
- `test_plan`
- `auth_material`
- `data_seeds`
- `retry_policy`

## Preflight
- [ ] Test plan identifiers are unique.
- [ ] Required accounts/tokens are valid.
- [ ] Seed data exists and is not stale.
- [ ] Retry policy is defined.

## Execution Workflow
### Phase 1: Case Preparation
1. Resolve each case precondition.
2. Attach correct role context.
3. Build request template and expected baseline.

### Phase 2: Deterministic Execution
1. Run case with exact payload and headers.
2. Capture full response metadata and body hash.
3. Apply retries only under policy.

### Phase 3: Outcome Classification
1. `pass` when expected secure behavior observed.
2. `fail` when expected secure behavior breaks.
3. `blocked` when environment prevents valid execution.
4. `inconclusive` when signal is unstable.

### Phase 4: Evidence Packaging
1. Store request/response artifacts.
2. Map artifact to case ID.
3. Add concise analyst note for anomalies.

## Required Logging Fields
- `case_id`
- `timestamp_utc`
- `role_context`
- `request_signature`
- `status_code`
- `response_signature`
- `verdict`

## Output Contract
```json
{
  "case_results": [],
  "evidence_index": [],
  "blocked_cases": [],
  "environment_notes": []
}
```

## Constraints
- Do not expand scope.
- Do not mutate payloads outside case definition.

## Quality Checklist
- [ ] Every case has terminal status.
- [ ] Evidence references are complete.
- [ ] Blockers include concrete unblock requests.

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

## Quick Scenarios
### Scenario A: Authorization Drift
- Baseline with owned resource.
- Replay with foreign resource identifier.
- Repeat with role shift and fresh session.
- Confirm read/write/delete differences.

### Scenario B: Input Handling Weakness
- Send syntactically valid control payload.
- Send semantically malicious variant.
- Verify parser or execution side effect.
- Re-test with content-type variation.

### Scenario C: Workflow Bypass
- Execute expected state sequence.
- Attempt out-of-order transition.
- Attempt repeated action replay.
- Confirm server-side state enforcement.

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
