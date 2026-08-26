---
name: threat-hunting
description: Use for blue-team threat hunting, detection engineering with Sigma/YARA, SIEM query design, and incident detection validation.
---

# Threat Hunting & Detection Engineering

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: Confirm blue-team/hunting authorization and the data source scope (SIEM, EDR exports)
2. `NOW`: Form a hypothesis before querying data; avoid blindly grinding through alerts
3. `NEXT`: Tools and data onboarding approach
4. `ACT`: Hypothesis → query → validate → turn into rules

## Applicable Scenarios

- Threat hunting (hypothesis-driven)
- Sigma / YARA detection engineering
- Alert tuning, false-positive analysis
- With `malware-analysis/`: sample-side IOCs → this skill lands them as detections
- With `digital-forensics/`: case artifacts → lateral hunting

## Workflow

### 1. Form a hypothesis

```text
Example: attackers use living-off-the-land for lateral movement
→ Data sources: Sysmon 1/3/10, Windows Security 4624/4648
→ Success criteria: find anomalous parent processes or rare account log sources
```

### 2. Query and stacking

```text
□ Baseline: normal administrator behavior time windows and hosts
□ Anomalies: new services, encoded PowerShell, unusual egress
□ Correlation: the same account logging into many hosts in a short window
```

### 3. Turn into rules

```yaml
# Sigma skeletons are in malware-analysis; this skill emphasizes:
# - False-positive surface
# - Data source field mapping
# - Response playbook linkage
```

### 4. Validation

```text
□ Atomic tests (Atomic Red Team) only in an authorized lab
□ Replay historical logs to verify recall
```

## Toolchain

| Tool | Purpose |
|------|------|
| Sigma CLI / sigmac | Rule conversion |
| YARA | Files/memory |
| SIEM (ELK/Splunk etc.) | Querying |
| osquery | Endpoint hunting |
| Atomic Red Team | Detection validation (lab) |

## References

- `references/hunting-loop.md`
- `../malware-analysis/references/yara-sigma-rules.md`
- `../digital-forensics/`

## Routing Context

**Upstream**: MASTER R27  
**Downstream**: confirmed intrusion → forensics; malicious sample → malware-analysis  
**MUST NOT**: run attack simulations in unauthorized production environments

## Task Completion Self-Check

- [ ] Is there a clear hypothesis and conclusion?
- [ ] Do the rules document false positives and data sources?
- [ ] Checklist?
