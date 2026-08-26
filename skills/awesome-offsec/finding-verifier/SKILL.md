---
name: finding-verifier
description: Verify vulnerability findings using independent replay, confounder control, and strict acceptance criteria.
---

# Finding Verifier

## Purpose
Ensure reported findings are accurate, reproducible, and correctly classified.

## Inputs
- `finding_report`
- `evidence_bundle`
- `environment_notes`

## Verification Workflow
### Phase 1: Evidence Integrity
1. Verify artifact completeness and timestamps.
2. Verify request-response pairing and context consistency.

### Phase 2: Independent Replay
1. Reproduce with original method.
2. Reproduce with alternate method when possible.
3. Compare behavior consistency.

### Phase 3: Confounder Analysis
1. Caching and stale session effects.
2. Timing and infrastructure noise.
3. Seed-data drift and race artifacts.

### Phase 4: Final Status
1. `confirmed` if replayable with clear impact.
2. `disputed` if strong counter-evidence exists.
3. `inconclusive` if unresolved blockers remain.

## Acceptance Criteria by Class
| Class | Confirmed Requires |
|---|---|
| Injection | parser/engine effect + attacker control |
| XSS | controlled script execution in target context |
| Authz | unauthorized action/object access proven |
| SSRF | outbound request influence or protected target reach |

## Output Contract
```json
{
  "verification_status": [],
  "replay_results": [],
  "confounder_notes": [],
  "required_follow_up": []
}
```

## Constraints
- Do not confirm from single unstable run.
- Do not dispute on intuition alone.

## Quality Checklist
- [ ] Independent replay attempted.
- [ ] Confounders addressed.
- [ ] Status rationale is explicit.

## Detailed Operator Notes
### Consistency Rules
- Normalize terminology before scoring or chaining.
- Separate prerequisite uncertainty from exploit uncertainty.
- Treat environmental blockers independently from mitigation strength.

### Risk Scoring Inputs
- attacker starting privilege
- required chain length
- probability of reliable execution
- blast radius if successful

### Prioritization Output
- `immediate`: low-effort high-impact chains/findings.
- `next`: moderate effort with clear payoff.
- `watch`: plausible but currently low confidence.

### Reporting Rules
- Include one-line executive summary per chain/finding.
- Include exact blocker needed to move an inconclusive item forward.
- Include confidence rationale in plain technical language.

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
