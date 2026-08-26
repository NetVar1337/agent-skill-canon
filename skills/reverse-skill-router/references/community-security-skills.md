# Community Security Skill Ecosystem Comparison (2026-07)

> Source retrieval date: **2026-07-17**  
> Purpose: let reverse-skill **know what exists outside**, borrow as needed, and **not** merge external giant libraries wholesale into this pack.  
> This pack's identity: routing + tool bootstrap + evidence/scope contracts + field-journal (see `ops/IDENTITY.md`).

## 1. External High-Value Repositories (learn from, don't blind-install)

| Repository | Scale/positioning | Value to this pack | Risks |
|------|-----------|------------|------|
| [trailofbits/skills](https://github.com/trailofbits/skills) | ToB security research Claude plugin marketplace | a quality benchmark for audit/vulnerability analysis/RE plugins | must install via the ToB marketplace; don't default-trust non-curated copies |
| [trailofbits/skills-curated](https://github.com/trailofbits/skills-curated) | audited plugin list | preferred over any community skill | same as above |
| [Orizon-eu/claude-code-pentest](https://github.com/Orizon-eu/claude-code-pentest) | 6 pentest lifecycle skills + pure Python scripts | the recon→exploit→report pipeline can benchmark our `attack-chain`+`pentest-tools` | authorization boundaries need self-checking; scripts need a sandbox |
| [trilwu/secskills](https://github.com/trilwu/secskills) | 16 skills + 6 specialist subagents | the multi-role division can benchmark `ops/role-map.md` | plugin form, unlike this pack's monorepo |
| [Masriyan/Claude-Code-CyberSecurity-Skill](https://github.com/Masriyan/Claude-Code-CyberSecurity-Skill) | ~15–19 domain skills (incl. RE/OT/CSOC) | a domain coverage checklist | less deep than this pack's single-domain skills |
| [mukul975/Anthropic-Cybersecurity-Skills](https://github.com/mukul975/Anthropic-Cybersecurity-Skills) | **800+** skills · ATT&CK/NIST mappings | the **framework mapping** and domain catalog are worth referencing, but don't depend on the whole library | huge volume; massive maintenance and poisoning surface |
| [Eyadkelleh/awesome-skills-security](https://github.com/Eyadkelleh/awesome-claude-skills-security) | SecLists packaged as agent skills | a dictionary/payload entry point | overlaps with the seclists bootstrap |
| [securityfortech/awesome-security-skills](https://github.com/securityfortech/awesome-security-skills) | curated list of security skills | an index for discovering new skills | list-type; each needs individual audit |
| [VoltAgent/awesome-agent-skills](https://github.com/VoltAgent/awesome-agent-skills) | 1000+ cross-vendor skill index | discovering official/community skills | not security-specific |
| [anthropics/claude-code-security-review](https://github.com/anthropics/claude-code-security-review) | PR security review GitHub Action | can benchmark our docs/report-side "change audit" scenario | a CI product, not an RE router |
| [agentskills.io](https://agentskills.io) | the Agent Skills open standard | frontmatter/directory convention alignment | the standard itself has no offense/defense content |

### 1.1 Second-Round Retrieval Additions (searched again 2026-07-17)

| Repository / resource | Positioning | Where it lands in this pack |
|-------------|------|----------|
| [trailofbits/skills](https://github.com/trailofbits/skills) plugins: `audit-context-building` `differential-review` `semgrep-rule-creator` `sharp-edges` `dwarf-expert` `burpsuite-project-parser` | audit context, differential security review, dangerous APIs, DWARF, Burp project parsing | compare against `ida-reverse`/`docs-generator`/audit workflows; do **not** merge the whole library |
| [HexRaysSA/ida-claude-code-plugins](https://github.com/HexRaysSA/ida-claude-code-plugins) | official IDA Claude plugins (incl. domain automation, marked unsafe) | compare against the `ida-reverse` MCP path; unsafe plugins not enabled by default |
| [P4nda0s/reverse-skills](https://github.com/P4nda0s/reverse-skills) | IDA-NO-MCP: export decompilation then analyze; rev-frida/dex-dump/u3d | complements "offline export when MCP is unavailable" |
| [2389-research/binary-re](https://github.com/2389-research/binary-re) | triage→static(r2/Ghidra)→dynamic(QEMU/GDB/Frida)→synthesis | see the `reverse-engineering` stage gates in `re-agent-workflow.md` |
| [incogbyte/android-reverse-engineering-claude-skill](https://github.com/incogbyte/android-reverse-engineering-claude-skill) | APK unpacking, endpoint extraction, adaptive Frida bypasses | compare against `apk-reverse`; dynamic scripts need scope |
| [OwenPawl/cerberus-re-skill](https://github.com/OwenPawl/cerberus-re-skill) | Apple-oriented Ghidra+LLDB+Frida three-loop | can reference the macOS/iOS dynamic loop |
| [ljagiello/ctf-skills](https://github.com/ljagiello/ctf-skills) | CTF reverse/pwn; tools installed on demand | compare against CTF-Sandbox + `pwn-chain` |
| [shuvonsec/claude-bug-bounty](https://github.com/shuvonsec/claude-bug-bounty) | /recon→/hunt→/validate→/report | compare against `recon-pipeline.md` + scope gates |
| [PayloadsAllTheThings](https://github.com/swisskyrepo/PayloadsAllTheThings) | web payloads + Prompt Injection chapter | `pentest-tools/payloads` takes priority; LLM see `llm-security` |
| [HackTricks](https://hacktricks.wiki/) | pentest methodology + **AI/MCP abuse** | see the MCP section of skill-supply-chain |
| [appsecsanta AI pentesting agents 2026](https://appsecsanta.com/research/ai-pentesting-agents-2026) | taxonomy of 39+ open-source AI pentest agent architectures | multi-agent ≠ mandatory; we use role-map |
| Snyk evaluation "more skills ≠ better" | skill stacking can reduce audit quality | reinforces the "deep skills + routing" strategy |

## 2. Security Standards and Threats (2025–2026)

| Source | Key points | Where it lands in this pack |
|------|------|----------|
| [OWASP Agentic Skills Top 10](https://owasp.org/www-project-agentic-skills-top-10/) | malicious skills, supply chain, permission abuse, memory poisoning, etc. | `ops/skill-supply-chain.md` |
| [Anthropic Agent Skills engineering post](https://www.anthropic.com/engineering/equipping-agents-for-the-real-world-with-agent-skills) | install only trusted sources; review scripts and dependencies | same, plus bootstrap forbidden from guessing paths |
| ClawHavoc-style poisoning campaigns (recorded in AST10) | registry-wide malicious skills | one-click installs into this pack from unknown registries are forbidden |

## 3. This Pack vs External "Broad" Coverage

| Domain | reverse-skill | What external packs often have, and why we don't merge wholesale |
|------|---------------|--------------------------------|
| APK/JS/IDA/r2/firmware/pwn | **deep** skills + scripts | preserve depth and the tool-index binding |
| Pentest/attack chain/SRC | pentest-tools + attack-chain + src-hunter | Orizon-class packs serve as methodology comparisons |
| LLM/Agent security | llm-security | AST10 enhances the skills' own security |
| Evidence/scope/roles | **ops/** (distinctive) | most skill packs have no case contracts |
| OT/ICS / pure GRC / fraud F3 | no standalone skill | routing miss → propose an addition or link out, don't force it |
| 800+ micro-skills | not copied | replaced by MASTER routing + domain skills instead of fragmentation |

## 4. Borrowing Rules (MUST)

```text
1. git submodule pulling an entire 800+ skill library as a runtime dependency is forbidden
2. When borrowing: extract "stages/checklists/command patterns" into this pack's references or existing skills
3. External scripts: first inspect dependencies and network behavior in an isolated environment, then consider bootstrap-manifest
4. New scenarios: add skills via CONTRIBUTING, and update routing + RULES keywords
5. Note the source URL + retrieval date (this file's format)
6. Walk the ops/skill-supply-chain.md checklist before installing/merging
7. At runtime, load only MASTER-ROUTING's PRIMARY (+ necessary secondaries) to avoid skill-stacking overload
```

## 4.1 "Borrowed Artifacts" Already Captured in This Pack (no external library dependency)

| Artifact | Path |
|------|------|
| Four RE phases | `reverse-engineering/references/re-agent-workflow.md` |
| Authorized recon | `pentest-tools/references/recon-pipeline.md` |
| Attack chain gates | `attack-chain/references/lifecycle-checklist.md` |
| Skill supply chain | `ops/skill-supply-chain.md` |
| Domain coverage | `references/domain-coverage-map.md` |

## 5. Suggested Priorities (future iterations)

| Priority | Action |
|--------|------|
| P0 done | ops contracts, MASTER routing, skill supply chain security documents |
| P1 | compare against Orizon/ToB to add pentest stage checklists to attack-chain references |
| P2 | an optional "external-link skill allowlist" config, not on the default path |
