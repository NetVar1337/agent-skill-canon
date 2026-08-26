---
name: router-reverse-skill-router-email-security
description: Use for authorized email security review including phishing analysis, header authentication (SPF/DKIM/DMARC), BEC patterns, and mailbox token abuse research.
---

# Email Security & Phishing Analysis

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: confirm authorization (sample email analysis / tenant configuration review)
2. `NOW`: do not re-deliver malicious samples to real users
3. `ACT`: header authentication → content/URLs → attachment sandboxing → tenant control-plane recommendations

## Applicable Scenarios

- Phishing email teardown and IOCs
- SPF/DKIM/DMARC configuration assessment
- BEC business email compromise patterns
- OAuth application phishing / mailbox token abuse (combined with llm/cloud identity)
- Security awareness exercise design (authorized)

## Workflow

```text
□ Full raw headers: Received chain, From/Return-Path consistency
□ SPF/DKIM/DMARC alignment results
□ URL sandbox and static attachment analysis (combined with malware-analysis)
□ Impersonated brands and reply-address discrepancies
□ Tenant: anti-phishing policy, external tagging, MFA, OAuth app consent
```

## Toolchain

| Tool | Purpose |
|------|------|
| Mail client "view source" | Headers |
| dig/nslookup | SPF/DMARC records |
| urlscan / sandbox | Links and attachments |
| Tenant admin center | Policy |

## References

- `references/email-auth-checklist.md`
- `../malware-analysis/` `../attack-chain/` (phishing stage) `../windows-ad/` (tokens)

## Routing Context

**Upstream**: MASTER R36  
**MUST NOT**: unauthorized mass test-phishing against third-party domains

## Task Completion Self-Check

- [ ] Header authentication conclusions complete?
- [ ] IOCs made detectable (combined with threat-hunting)?
- [ ] Checklist?
