---
name: wifi-wireless
description: Use for authorized wireless security assessment including Wi-Fi capture, WPA handshake analysis, rogue AP detection research, and lab-only deauth testing.
---

# Wi-Fi / Wireless Security

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read precedent-pentest; **wireless attacks carry high legal risk** — written authorization and a physical boundary are mandatory
2. `NOW`: scope states target SSID/BSSID/site; scanning neighbors' networks is forbidden
3. `NEXT`: confirm the adapter's monitor-mode capability
4. `ACT`: reconnaissance → capture → analysis (lab preferred)

## Applicable Scenarios

- Authorized Wi-Fi security assessment
- WPA/WPA2 handshake capture and offline evaluation
- Rogue AP / phishing hotspot detection research
- Enterprise wireless isolation and captive-portal security

## Workflow

```text
□ iwconfig / airmon-ng to enter monitor mode (legal environment)
□ airodump-ng to lock the target BSSID channel
□ Handshake or PMKID capture (target only)
□ hashcat/aircrack offline evaluation of the password policy
□ Report: encryption type, isolation, portal bypass, recommendations
```

## Toolchain

| Tool | Purpose |
|------|------|
| aircrack-ng suite | Capture/evaluation |
| hcxdumptool / hcxtools | PMKID |
| hashcat | Password evaluation |
| Wireshark | Management frame analysis |

## References

- `references/wireless-lab-rules.md`
- `../pentest-tools/` `../attack-chain/` (near-source chapters)

## Routing Context

**Upstream**: MASTER R29  
**MUST NOT**: unauthorized deauth, operations against non-target customer networks

## Task Completion Self-Check

- [ ] Strictly locked onto the target BSSID?
- [ ] Hardening recommendations in the report?
- [ ] Checklist?
