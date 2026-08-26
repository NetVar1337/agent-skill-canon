---
name: api-attack-surface-mapper
description: Build a full API inventory, trust-boundary map, and prioritized test matrix from specification and observed behavior.
---

# API Attack Surface Mapper

## When To Use
Use this skill when you need high coverage before exploitation.

## When Not To Use
Do not use this as a replacement for exploit confirmation. It is a discovery and planning skill.

## Required Inputs
- `target_base_url`
- `api_spec_source` (OpenAPI URL/file, Postman collection, or captured traffic)
- `auth_context` (token types, role accounts, session rules)
- `scope_rules` (in-scope services, forbidden actions)

## Optional Inputs
- `known_business_flows`
- `environment_limits` (rate limits, test windows)
- `seed_ids` (known object identifiers)

## Preflight Checklist
- [ ] Spec is reachable and parseable.
- [ ] Base URL and version path are confirmed.
- [ ] Auth mechanism is known per endpoint family.
- [ ] Scope exclusions are explicit.

## Execution Workflow
### Phase 1: Normalize Inputs
1. Parse spec and resolve path templates, tags, and schema references.
2. Deduplicate routes by method + canonical path.
3. Flag undocumented endpoints observed in traffic.

### Phase 2: Build Trust-Boundary Map
1. Label endpoints as `public`, `user`, `admin`, `internal`, or `unknown`.
2. Map auth styles: cookie session, bearer token, API key, mTLS.
3. Capture identity source and role enforcement points.

### Phase 3: Parameter Risk Profiling
1. Classify parameters by risk type:
- object references
- filter/sort/query operators
- file/blob inputs
- callback URLs
- rich text/template fields
2. Mark whether each parameter is attacker-controlled and persisted.

### Phase 4: Test Matrix Generation
1. Generate baseline tests for each endpoint (auth, method, content type).
2. Generate abuse tests by class:
- BOLA/BFLA
- mass assignment
- injection
- SSRF-style URL handling
- workflow/state abuse
3. Prioritize by business impact and reachable privilege.

### Phase 5: Low-Noise Validation
1. Confirm route liveness and auth expectations.
2. Record response fingerprint per endpoint:
- status bands
- auth error shape
- validation error shape
3. Mark unstable endpoints as low-confidence until retested.

## Coverage Matrix (Minimum)
| Class | Minimum Check |
|---|---|
| BOLA/BFLA | Cross-account object access with role switch |
| Auth/session | Missing token, expired token, token audience mismatch |
| Mass assignment | Hidden fields on create/update |
| Injection | SQL/NoSQL/template/operator contexts |
| SSRF | URL/file fetchers, webhooks, importers |
| Data exposure | Over-broad response fields and debug traces |
| Rate abuse | Lack of throttling on sensitive actions |
| Workflow abuse | Invalid state transitions, skipped approvals |

## Evidence Requirements
- Endpoint inventory with method/path/auth labels.
- Request templates for each high-priority case.
- Response fingerprints for baseline and negative controls.
- Explicit unknowns and blockers.

## Output Contract
Return JSON:
```json
{
  "endpoint_inventory": [],
  "trust_boundaries": [],
  "parameter_risk_profile": [],
  "prioritized_test_matrix": [],
  "baseline_observations": [],
  "coverage_gaps": []
}
```

## Failure Modes
- Treating spec as truth while ignoring runtime drift.
- Assuming role checks from docs instead of testing.
- Ignoring undocumented routes from front-end telemetry.

## Exit Criteria
- Inventory covers all observed and documented routes.
- Each high-risk endpoint has at least one concrete test case.
- Unknowns are explicit and actionable.

## Detailed Operator Notes
### Endpoint Normalization Rules
- Normalize path params to `{id}` style for deduping.
- Split endpoints by functional domain before prioritization.
- Keep undocumented and documented routes as separate sources.

### Prioritization Heuristics
- Highest priority: object-level operations with write capability.
- Next priority: admin-like routes exposed in user-auth context.
- Next priority: import/export and callback endpoints.
- Lower priority: purely informational static metadata routes.

### Common Blind Spots
- Versioned paths that differ only by prefix behavior.
- Bulk endpoints with relaxed validation compared to single-item routes.
- Graph-like query parameters that reach data-layer operators.

### Reporting Rules
- Include `discovery_source` per endpoint (`spec`, `traffic`, `frontend`).
- Include `auth_assumption` and `auth_verified` flags separately.
- Include `priority_reason` for every high-risk endpoint.

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
