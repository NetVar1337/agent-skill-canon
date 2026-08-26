---
name: awesome-offsec-advanced-recon
description: Elite methodology for discovering maximum attack surface with minimal detection (5-Layer Approach).
author: 1ikeadragon
license: GPLv3
---

# Advanced Reconnaissance

## Skill Overview

**Prerequisites**: DNS, HTTP, command-line tools  
**Goal**: Discover maximum attack surface with minimal detection

---

## Core Philosophy
Breadth → Depth → Exploitation
Wide net → Focus → Attack

**Key Principle**: 80% of bugs come from assets others miss.

---

## The 5-Layer Approach
1.  **Layer 1: Organization Intelligence** (scope understanding)
2.  **Layer 2: Passive Subdomain Discovery** (no target contact)
3.  **Layer 3: Active Discovery** (DNS queries, brute-forcing)
4.  **Layer 4: Asset Enumeration** (ports, services, tech stack)
5.  **Layer 5: Deep Content Discovery** (endpoints, parameters)

---

## Layer 1: Organization Intelligence

### ASN & IP Range Discovery
- Use `whois` and `amass intel` to find ASNs and IP ranges.
- **Reference**: Use results to feed into `active_discovery` workflows.

---

## Layer 2: Passive Subdomain Discovery

### Certificate Transparency & DNS Aggregators
- Tools: `subfinder`, `amass`, `crt.sh`.
- **Goal**: Build a seed list for active resolution.

### Search Engine Dorking
- Google/GitHub dorks to find shadow IT and dev environments.

---

## Layer 3: Active Subdomain Discovery

### DNS Brute-Forcing
- Tools: `puredns`, `shuffledns`.
- **Strategy**: Use massive wordlists (Jhaddix/SecLists) + Permutations (`altdns`).

---

## Layer 4: Asset Discovery

### Port Scanning & Service Enumeration
- Tools: `naabu`, `nmap`.
- **Output**: `live.txt` (Httpx results)
- **Integration**: Feed live web services to `web_application_security` skills.

### Technology Detection
- Tools: `nuclei -t technologies`, `whatweb`.
- Identify WAFs early (refer to `waf_bypass` skill if blocked).

---

## Layer 5: Deep Content Discovery

### Crawling & JavaScript Analysis
- Tools: `katana`, `hakrawler`.
- **Action**: Extract API endpoints from JS files (refer to `javascript_analysis` skill for deobfuscation).

---

## Cloud Asset Discovery
- **AWS/Azure/GCP**: Bucket enumeration and cloud-specific pattern scanning.

---

## Automation
- Build pipelines using `tmux` or `axiom` for distributed scanning.
- **Continuous Monitoring**: Alert on new subdomains (Cron + Subfinder).

---

## Data Organization
Maintain a standardized directory structure (`recon/target/subdomains`, `recon/target/web`, etc.) as defined in the overarching methodology.
