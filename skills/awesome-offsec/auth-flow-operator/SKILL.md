---
name: auth-flow-operator
description: Establish and validate authenticated test access through login, registration, session lifecycle, and role context checks.
---

# Auth Flow Operator

## Purpose
Securely obtain reliable authenticated context for downstream security testing.

## Inputs
- `target_url`
- `known_credentials` (optional)
- `auth_notes` (MFA, email verification, SSO, CAPTCHA)
- `allowed_test_accounts` policy

## Workflow
### Phase 1: Route Discovery
1. Identify login, registration, password reset, token refresh, logout paths.
2. Determine auth mode: local creds, SSO, OTP, magic link, API token.

### Phase 2: Login Path
1. Attempt known credentials in defined order.
2. Validate success via authenticated-only action, not UI guess.
3. Record session artifacts and expiry behavior.

### Phase 3: Registration Path
1. Create dedicated test accounts when permitted.
2. Capture verification dependencies.
3. Validate role assignment and default permissions.

### Phase 4: Session Lifecycle
1. Test logout invalidation.
2. Test token/cookie rotation after privilege change.
3. Test concurrent session behavior.

### Phase 5: Access Validation
1. Confirm protected route gating.
2. Confirm role-sensitive feature differences.
3. Confirm cross-account isolation.

## Anti-Patterns
- Assuming logged-in state from UI text only.
- Reusing stale tokens without validation.
- Mixing account identities in one evidence stream.

## Output Contract
```json
{
  "working_auth_paths": [],
  "accounts": [],
  "session_lifecycle": [],
  "role_validation": [],
  "blockers": []
}
```

## Constraints
- No brute force.
- Respect account-creation and cleanup rules.
- Keep PII and credentials minimized in logs.

## Quality Checklist
- [ ] At least one stable auth path established.
- [ ] Session behavior tested, not inferred.
- [ ] Role boundaries verified with action-level checks.

## Detailed Operator Notes
### Session Validation Tests
- Confirm authenticated access after login and after token refresh.
- Confirm logout invalidates prior session tokens/cookies.
- Confirm password reset invalidates old sessions when expected.

### Role Validation Tests
- Confirm role-specific UI and API behavior differ as expected.
- Confirm privilege elevation requires server-side enforcement.
- Confirm role claims in token align with backend checks.

### Common Failure Patterns
- Partial login success where UI changes but API remains unauthenticated.
- Mixed identity state from stale cookies and new tokens.
- Registration defaults granting broader permissions than intended.

### Reporting Rules
- Keep one identity timeline per account.
- Record account origin (`provided` or `created`) and intended role.
- Record exact blocker cause when auth setup fails.

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
| Credentials succeed in UI but fail in API | validate token audience/session binding | endpoint-level auth proof |
| Registration requires email verification | capture verification state transitions | account timeline with states |
| MFA optional for some flows | compare protected action access with/without MFA | role/action differential |
| Logout appears successful but token works | test token reuse after logout/reset | post-logout replay proof |
| Role appears in UI only | validate backend authorization with privileged actions | server-side denial/allow traces |

## Advanced Coverage Extensions
1. Test session fixation across pre- and post-login states.
2. Test parallel session revocation behavior after password change.
3. Test role downgrade persistence after privilege changes.
4. Test account recovery path for unauthorized account linking.
5. Test SSO fallback paths for local-auth bypass.
