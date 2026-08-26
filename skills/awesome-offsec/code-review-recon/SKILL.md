---
name: code-review-recon
description: Build an exhaustive map of attack entry points, trust boundaries, and dangerous sinks before exploit-focused code analysis.
---

# Code Review Recon

## Purpose
Prevent blind spots by mapping how untrusted data enters and moves through the codebase.

## Inputs
- `code_path`
- `language_framework`
- `deployment_notes` (optional)

## Workflow
### Phase 1: Topology Mapping
1. Identify entry layers: HTTP routes, RPC, CLI, cron/jobs, message consumers.
2. Identify boundary layers: auth middleware, policy checks, service interfaces.
3. Identify sink layers: database, templates, OS commands, file system, network calls.

### Phase 2: Route and Handler Inventory
1. Enumerate handlers and parameter parsers.
2. Map per-route auth and role assumptions.
3. Flag routes with weak or missing guards.

### Phase 3: Sink Inventory
1. Query construction paths.
2. File operations and archive extraction.
3. Serialization/deserialization and parser usage.
4. Outbound request constructors.

### Phase 4: Trust Boundary Audit
1. Track user-to-service boundary crossings.
2. Track tenant and organization boundary assumptions.
3. Track privileged action boundaries.

### Phase 5: Handoff Plan
1. Rank high-risk source-to-sink paths.
2. Provide per-path context needed for deep analysis.
3. Note uncertain areas requiring runtime confirmation.

## Recon Coverage Targets
| Target | Minimum Expectation |
|---|---|
| Entry points | all major ingestion vectors mapped |
| Auth boundaries | per-route enforcement identified |
| Sink categories | full inventory with owner file/function |
| Prioritized paths | top attacker-value paths ranked |

## Output Contract
```json
{
  "entry_points": [],
  "auth_boundary_map": [],
  "sink_inventory": [],
  "priority_trace_paths": [],
  "unknowns": []
}
```

## Constraints
- Favor breadth, traceability, and reproducibility.
- Do not claim vulnerabilities in recon phase.

## Quality Checklist
- [ ] Non-HTTP sources are included.
- [ ] Auth assumptions are explicit.
- [ ] Handoff paths are actionable.

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
