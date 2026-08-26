---
name: router-reverse-skill-router-windows-ad
description: Use for authorized Active Directory and Windows identity attacks including Kerberos, AD CS, BloodHound paths, NTLM relay, and domain privilege escalation research.
---

# Windows / Active Directory Security

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read `../field-journal/precedent-pentest.md`
2. `NOW`: **domain/AD testing requires a clearly authorized scope** (including DCs, and whether poisoning/relaying is allowed)
3. `NOW`: case-init; write the network_profile and forbidden actions clearly
4. `NEXT`: tool-index (impacket/certipy/bloodhound etc. are often manual)
5. `ACT`: start from identity enumeration and the BloodHound graph, not destructive exploitation

## Applicable Scenarios

- Domain penetration, Kerberoasting, AS-REP, delegation
- AD CS (ESC1–ESC8 etc.) certificate attacks
- BloodHound / SharpHound attack paths
- NTLM Relay / Coercer forced authentication
- Local privilege escalation to domain paths (Potato etc. as a pivot)

## Relationship with attack-chain

- **Multi-stage from internet to domain controller** → PRIMARY can remain `attack-chain/`, this skill is the **AD specialist**
- **Already inside the domain, focused on identity** → PRIMARY = this skill

## Workflow

### 1. Enumeration

```bash
# Impacket examples / built-ins (require credentials and authorization)
nxc smb <range> -u user -p pass
bloodhound-python -d domain.local -u user -p pass -c All -ns <DC>
```

### 2. Common Paths (map first, shoot later)

```text
□ Kerberoast / AS-REP → offline cracking
□ ACL abuse (GenericAll/WriteDacl)
□ Delegation (unconstrained/constrained/resource-based)
□ AD CS template misconfiguration → Certipy
□ Relay: LLMNR/NBT-NS + ntlmrelayx (confirm authorization)
```

### 3. Credentials and Lateral Movement

```text
□ secretsdump / lsassy / mimikatz (strict authorization and cleanup)
□ PtH / PtT / golden tickets only within the authorized red-team scope
□ Write Evidence at every step; wait for user confirmation on high-risk actions
```

## Toolchain

| Tool | Purpose |
|------|------|
| BloodHound / SharpHound | Path graph |
| Certipy | AD CS |
| Impacket / NetExec | Lateral movement and enumeration |
| Rubeus / Mimikatz | Tickets and credentials (authorized) |
| Coercer / Responder | Forced authentication / poisoning |

## References

- `references/ad-attack-paths.md`
- `../pentest-tools/references/network-attack-defense.md`
- `../attack-chain/`
- seeds: `field-journal/seed-005_ad-certipy-esc1.md` `seed-007_ntlm-relay-coercer.md` `seed-013_kerberoasting-spn.md`

## Routing Context

**Upstream**: MASTER R24  
**Downstream**: reporting `docs-generator`; EDR research needed → `edr-bypass-re`  
**MUST NOT**: unauthorized DCSync / golden tickets against production

## Task Completion Self-Check

- [ ] Graph/enumeration before exploitation?
- [ ] Reproducible commands recorded and sanitized?
- [ ] Scope prohibitions respected?
- [ ] Checklist?
