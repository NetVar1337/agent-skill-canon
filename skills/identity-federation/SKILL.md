---
name: identity-federation
description: Use for authorized assessment of federated identity systems including SAML, OIDC, OAuth2 flows, SSO misconfiguration, and token confusion issues.
---

# Identity Federation (SAML / OIDC / OAuth)

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read precedent-pentest; put SSO test accounts and the IdP/SP scope into scope
2. `NOW`: brute-force attempts that could lock out real user accounts are forbidden
3. `NEXT`: traffic-capture tools and documentation (metadata URLs)
4. `ACT`: protocol-flow mapping → common misconfigurations → verification

## Applicable Scenarios

- SAML Response signature/assertion tampering surface (classic defect patterns)
- OIDC implicit/authorization code + missing PKCE
- redirect_uri / state / nonce issues
- IdP and SP metadata, multi-tenant issuer confusion
- Complements JWT attacks in `api-security` (this skill leans federation and SSO flows)

## Workflow

```text
□ Map it clearly: User → SP → IdP → Token → SP
□ Collect: /.well-known/openid-configuration, SAML metadata
□ Check: redirect_uri exact match, state binding, PKCE
□ Check: SAML signature coverage, algorithm downgrade
□ Session fixation and logout invalidation
```

## Toolchain

| Tool | Purpose |
|------|------|
| Burp + SAML Raider etc. | Assertion editing (authorized) |
| jwt_tool | JWT segments |
| Browser DevTools | Redirect chains |
| IdP admin logs | Auditing |

## References

- `references/sso-flow-checklist.md`
- `../api-security/` `../windows-ad/` (enterprise IdP)

## Routing Context

**Upstream**: MASTER R37  
**Downstream**: pure API JWT → api-security; cloud IdP → cloud-k8s

## Task Completion Self-Check

- [ ] Full SSO flow mapped?
- [ ] Every Finding has reproduction and impact?
- [ ] Checklist?
