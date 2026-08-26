---
name: web-assessment-executor
description: Execute scoped web application test cases with strict sequencing, variant control, and replayable evidence.
---

# Web Assessment Executor

## Purpose
Run assigned web tests without scope drift while preserving strong proof quality.

## Inputs
- `target_url`
- `test_cases`
- `auth_context`
- `scope_constraints`
- `runtime_limits`

## Execution Policy
- Complete one test case end-to-end before moving on.
- Use browser automation for stateful UX flows.
- Use HTTP tooling for deterministic replay.
- Keep payload variants bounded and logged.

## Workflow
### Phase 1: Session and Baseline
1. Validate authentication and role.
2. Capture normal behavior baseline for target action.
3. Define success and failure signal for the case.

### Phase 2: Case Execution
1. Run base payload.
2. Run controlled payload variants.
3. Capture request context and response deltas.

### Phase 3: Escalation
1. If vulnerable signal appears, escalate toward measurable impact.
2. If blocked by filter, pivot to bypass testing.
3. If no signal after bounded variants, classify negative.

### Phase 4: Evidence Packaging
1. Include replay steps, payloads, and artifacts.
2. Map evidence to case ID and vulnerability type.
3. Store explicit rationale for verdict.

## Minimum Variant Policy
| Vulnerability Type | Minimum Variants |
|---|---|
| XSS | context-aware payloads across HTML/attr/JS contexts |
| SQLi | boolean, error, and time-control checks |
| IDOR | object ID and role/tenant permutations |
| CSRF/workflow | token, sequence, and method variations |

## Output Contract
```json
{
  "executed_cases": [],
  "confirmed_findings": [],
  "negative_cases": [],
  "blocked_cases": [],
  "evidence_index": []
}
```

## Constraints
- Do not invent unrelated tests.
- Do not claim exploitation without execution proof.

## Quality Checklist
- [ ] Every case has terminal status.
- [ ] Variant set is sufficient and bounded.
- [ ] Confirmed findings are replayable.

## Detailed Operator Notes
### Evidence Ladder
- Step 1: suspicious signal.
- Step 2: primitive confirmation.
- Step 3: execution/authorization breach.
- Step 4: concrete business impact.

### Variant Discipline
- Keep payload families grouped by hypothesis.
- Stop variant expansion when new runs are non-informative.
- Prefer context-correct payloads over generic sprays.

### Confounder Controls
- Re-test in a fresh session and new object state.
- Re-test with baseline payload and expected-secure payload.
- Confirm that edge cache/CDN behavior is not driving the result.

### Reporting Rules
- Include case-level timeline from trigger to impact.
- Include exploitation preconditions and limitations.
- Include clean retest steps for independent validation.

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
| Finding signal unstable | downgrade confidence and add retest plan | repeated run variance log |
| Chain link missing prerequisite | split chain and mark dependency blocker | prerequisite graph |
| Impact appears low in isolation | evaluate chain amplification paths | chain-level impact narrative |
| Mitigation claim is partial | verify alternate path and state variants | mitigation bypass check |
| Environment blocker dominates | classify inconclusive with unblock requests | blocker evidence |

## Advanced Coverage Extensions
1. Add attack-path branching for multiple privilege starting points.
2. Add defender-detection assumptions and likely monitoring signals.
3. Add rollback/cleanup verification after proof steps.
4. Add business-impact mapping to concrete assets and workflows.
5. Add reproducibility score based on run-to-run consistency.
