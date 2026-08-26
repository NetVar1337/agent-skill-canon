---
name: finding-chain-correlator
description: Correlate individual findings into coherent multi-step attack chains with realistic prerequisites and aggregate impact.
---

# Finding Chain Correlator

## Purpose
Reveal compounding risk that isolated findings understate.

## Inputs
- `finding_set`
- `application_context`
- `auth_model`
- `data_sensitivity_map`

## Workflow
### Phase 1: Normalization
1. Standardize findings into capability statements.
2. Extract prerequisites, required role, and affected assets.

### Phase 2: Link Construction
1. Connect findings where output of one enables next.
2. Identify dependency order and branching options.
3. Reject links lacking technical preconditions.

### Phase 3: Chain Validation
1. Validate each step is feasible in target context.
2. Validate session and state transitions between steps.
3. Validate operational reliability of full chain.

### Phase 4: Impact Aggregation
1. Evaluate confidentiality, integrity, and availability impact.
2. Estimate blast radius and tenant crossover risk.
3. Rank by attacker effort vs outcome.

### Phase 5: Defensive Breakpoints
1. Identify minimal controls that break the chain.
2. Prioritize controls by implementation cost and risk reduction.

## Chain Scoring Factors
- prerequisite complexity
- execution reliability
- privilege needed
- detectability
- business impact

## Output Contract
```json
{
  "attack_chains": [],
  "prerequisite_graph": [],
  "aggregate_impact": [],
  "defensive_breakpoints": [],
  "priority_order": []
}
```

## Constraints
- No speculative chain links.
- No additive severity without chain feasibility.

## Quality Checklist
- [ ] Every link has evidence.
- [ ] Chain order is technically valid.
- [ ] Defensive breakpoints are practical.

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
