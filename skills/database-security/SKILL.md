---
name: database-security
description: Use for authorized database security assessment covering PostgreSQL/MySQL/MSSQL/Mongo/Redis exposure, authz, UDF/command paths, and misconfiguration review.
---

# Database Security Assessment

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read precedent-pentest; **destructive statements against production databases are forbidden** unless explicitly allowed
2. `NOW`: write scope clearly: instances, account permissions, whether writes/deletes are allowed
3. `NEXT`: client tool paths
4. `ACT`: exposure surface → authentication → authorization → configuration → (safe) exploit-chain verification

## Applicable Scenarios

- Unauthenticated databases/weak passwords/wrongly bound 0.0.0.0
- Excessive privileges, dangerous features (xp_cmdshell, COPY PROGRAM, UDF)
- Lateral movement: from app account to DBA
- NoSQL injection and Redis file writes etc. (authorized environments)

## Workflow

```text
□ Network exposure and TLS
□ Account roles and grantees
□ Sensitive table access controls
□ Dangerous configuration: file_priv, xp_cmdshell, load_file
□ Whether audit logging is enabled
□ Backup and snapshot permissions
```

## Toolchain

| Tool | Purpose |
|------|------|
| Official CLIs | Connection and enumeration |
| sqlmap | Injection verification (authorized) |
| nuclei | Known exposure templates |
| Cloud RDS console audit | Configuration |

## References

- `references/db-misconfig-checklist.md`
- `../pentest-tools/` `../cloud-k8s/`

## Routing Context

**Upstream**: MASTER R35  
**Downstream**: OS command obtained → attack-chain; cloud-managed → cloud-k8s

## Task Completion Self-Check

- [ ] Avoided unauthorized writes/deletes?
- [ ] Distinguished configuration issues from exploitable chains?
- [ ] Checklist?
