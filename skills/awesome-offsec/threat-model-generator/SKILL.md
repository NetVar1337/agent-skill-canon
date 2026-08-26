---
name: threat-model-generator
description: Generate feature-grounded threat scenarios and executable security test cases with prioritized risk rationale.
---

# Threat Model Generator

## Purpose
Translate architecture and feature behavior into an actionable security test backlog.

## Inputs
- `system_description`
- `feature_inventory`
- `data_flows`
- `roles_permissions`
- `deployment_context` (optional)

## Modeling Workflow
### Phase 1: Asset and Boundary Mapping
1. Identify sensitive assets and trust boundaries.
2. Map data ingress, processing, and egress points.
3. Identify privileged operations and administrative paths.

### Phase 2: Threat Enumeration
1. Enumerate attacker objectives per feature.
2. Enumerate abuse primitives per parameter and state transition.
3. Enumerate systemic risks from shared components.

### Phase 3: Scenario Construction
1. Build concrete scenario with attacker preconditions.
2. Define target operation and exploit mechanism.
3. Define success signal and defensive expectation.

### Phase 4: Prioritization
1. Score by likelihood, impact, and detectability.
2. Tag fast-win vs deep-investigation cases.
3. Highlight assumptions and missing architecture details.

## Mandatory Coverage Areas
- authentication and session handling
- authorization and object access
- injection and parser abuse
- workflow/state manipulation
- file and data handling
- configuration and deployment weaknesses

## Output Contract
```json
{
  "threat_scenarios": [],
  "test_cases": [],
  "risk_priorities": [],
  "assumptions": [],
  "unknowns": []
}
```

## Constraints
- Ground scenarios in provided architecture.
- Flag unsupported assumptions explicitly.

## Quality Checklist
- [ ] Each scenario maps to a real asset.
- [ ] Test cases are executable.
- [ ] Prioritization rationale is clear.

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
