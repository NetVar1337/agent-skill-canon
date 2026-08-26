---
name: docs-generator
description: |
  Creates task-oriented technical documentation with progressive disclosure. Use when writing READMEs, API docs, architecture docs, or markdown documentation.
  Also use this skill at the END of any completed reverse engineering, penetration testing, CTF, or security analysis task to generate a formal report in the user's project directory.
  Trigger keywords: write report, write docs, produce a report, writeup, technical documentation, report, documentation.
---

# Technical Documentation

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: Confirm whether the current task falls within this skill's scope
2. `NOW`: Read `../tool-index.md`, validate tool availability and actual paths
3. `NEXT`: If tools are missing, invoke bootstrap — do not guess paths
4. `ACT`: Enter step one of the "Workflow" and execute; do not stop at a confirmation state

For writing style, tone, and voice guidance, use `Skill(ce:writer)` with **The Engineer** persona.

## Security / Reverse Engineering Task Documentation Output

When a reverse engineering / penetration testing / CTF / security analysis task is complete, this skill is responsible for generating formal technical documentation in the **user's project directory**.

### Trigger timing

1. Reverse engineering task complete, with core conclusions produced (algorithm recovery, signature cracking, bypass scheme, etc.)
2. Penetration test complete, with vulnerabilities found and verified
3. CTF challenge solved, with the flag obtained
4. The user explicitly asks to "write a report / documentation / writeup"

### Template selection

| Task type | Template to use |
|---------|---------|
| APK/binary/.so reverse engineering | `references/security-report-templates.md` → reverse engineering report |
| Penetration testing / vulnerability hunting | `references/security-report-templates.md` → penetration testing report |
| CTF solve | `references/security-report-templates.md` → CTF Writeup |
| JS/Web signature reverse engineering | `references/security-report-templates.md` → signature reverse engineering report |
| Malware / APT / virus analysis report | `references/security-report-templates.md` + **`references/vendor-report-rules.md`** |
| General technical documentation | `references/templates.md` → README / API docs |

### Vendor report structure (Issue #65)

For formal security reports you **MUST** read `references/vendor-report-rules.md` (take only the structure, do not copy vendor original text). Choose a vendor flavor only when the task evidence or the user explicitly requires it; ordinary reverse engineering and other tasks use `flavor = null`.

| Flavor / Overlay | When to use | Primary reference skeleton |
|------------------|--------|------------|
| `malware` | Confirmed malicious sample, trojan, white-plus-black, phishing payload | Huorong style: overview → process → sample analysis → incident handling → IOC |
| `apt` | APT/campaign/group/multi-stage infection chain/industry-targeted | Kaspersky Securelist style: summary → infection chain → investigation narrative → Interesting findings → technical analysis → detection & mitigation → IOC |
| `flavor = null` | Ordinary APK/ELF/PE/Mach-O reverse engineering, algorithm/firmware analysis, pentest / CTF / JS signature | Original task template + Base common elements; do not apply malware/APT-specific sections |
| thin `vuln` | User explicitly requests vulnerability/patch/CVE technical analysis | Overview → impact/reproduction → crash and patch analysis → protection recommendations (overlaid on null, not a 3rd default full-text flavor) |

Principle: **templates should be precise, not numerous** —— only 2 vendor full-text flavors; `vuln` is only an optional thin overlay, no third default full-text template is created.
Takes effect **simultaneously** with §0 Evidence→Finding→Path; on conflict, the Evidence contract takes precedence.

### Output specification

- **Output location**: the user's current project directory (not the skill package directory)
- **Filename format**: `YYYY-MM-DD_[type]-[target-short-name]-report.md`
- **If the project has a `docs/` directory**: prefer placing it under `docs/`
- **Encoding**: UTF-8
- **Language**: follow the user's conversation language (Chinese conversation → Chinese report, English conversation → English report)

### Quality requirements

- All code blocks must be directly runnable or have clear context
- No placeholders/TODOs
- Key findings must be backed by evidence
- Reproduction steps must allow a third party to independently reproduce
- Sensitive information (real tokens, passwords, internal URLs) replaced with placeholders
- **MUST** include the Evidence → Finding → Path chain (see `../ops/evidence-finding-path.md` and template §0)
- **MUST** read `references/vendor-report-rules.md`: choose `malware` / `apt` or `flavor = null` (vulnerability tasks may add the thin `vuln` overlay); with no flavor, only output the original task template and applicable Base elements, do not force IOC/ATT&CK
- **SHOULD** reference the case `scope.md` / `timeline.md` (`../scripts/case-init.ps1`)

### Diagram integration

When generating reports, the `diagram-generator` skill should be invoked at appropriate places to produce visual diagrams:

| Report type | Suggested diagrams | Diagram types |
|---------|---------|---------|
| Reverse engineering report | Function call graph, data flow diagram | Mermaid flowchart / sequenceDiagram |
| Penetration testing report | Attack path diagram, network topology diagram | Mermaid flowchart / Graphviz |
| CTF Writeup | Solution approach flowchart | Mermaid flowchart |
| JS signature reverse engineering report | Request chain sequence diagram, algorithm flowchart | Mermaid sequenceDiagram / flowchart |

Diagrams are embedded in the report markdown as Mermaid code blocks, ensuring they render directly on GitHub/GitLab.

---

## Core Principles

### 1. Progressive Disclosure

Reveal information in layers:

| Layer | Content | User Question |
|-------|---------|---------------|
| 1 | One-sentence description | What is it? |
| 2 | Quick start code block | How do I use it? |
| 3 | Full API reference | What are my options? |
| 4 | Architecture deep dive | How does it work? |

**Warnings, breaking changes, and prerequisites go at the TOP.**

### 2. Task-Oriented Writing

```markdown
<!-- Bad: Feature-oriented -->
## AuthService Class
The AuthService class provides authentication methods...

<!-- Good: Task-oriented -->
## Authenticating Users
To authenticate a user, call login() with credentials:
```

### 3. Show, Don't Tell

Every concept needs a concrete example.

## Formatting Standards

- **Sentence case headings**: "Getting started" not "Getting Started"
- **Max 3 heading levels**: Deeper means split the doc
- **Always specify language** in code blocks
- **Relative paths** for internal links
- **Tables** for structured data with 3+ attributes

## Quality Checklist

- [ ] Code examples tested and runnable
- [ ] No placeholder text or TODOs
- [ ] Matches actual code behavior
- [ ] Scannable without reading everything
- [ ] Reader knows what to do next

## Anti-Patterns

| Problem | Fix |
|---------|-----|
| Wall of text | Break up with headings, bullets, code, tables |
| Buried critical info | Warnings/breaking changes at TOP |
| Missing error docs | Always document what can go wrong |

## Templates

For README, API endpoint, and file organization templates, see [references/templates.md](references/templates.md).

## Related Skills

- `Skill(ce:writer)` - Writing style, tone, and voice (load The Engineer persona)
- `Skill(ce:visualizing-with-mermaid)` - Architecture and flow diagrams


---

## On-Demand Bootstrap

This skill does not depend on external tools; it is pure text generation. No bootstrap needed.

If diagrams need to be rendered and embedded into a report, the `diagram-generator/` skill will be invoked.

---

## Routing Context

**Upstream entry points**: all security / reverse engineering skills automatically invoke this skill upon task completion
**Trigger methods**:
- Automatic: executed as step 9 of the behavior chain after task completion
- Manual: the user says "write a report", "produce documentation", "writeup"

**Peer related modules**:
- `apk-reverse/` — generate a reverse engineering report after APK reverse engineering completes
- `ida-reverse/` — generate a reverse engineering report after binary analysis completes
- `radare2/` — generate a reverse engineering report after CLI analysis completes
- `js-reverse/` — generate a signature report after JS signature reverse engineering completes
- `reverse-engineering/` — generate a reverse engineering report after general reverse engineering completes
- `field-journal/` — report content also serves as a data source for the evolution log

**Security report templates**: `references/security-report-templates.md`
**Vendor report rules**: `references/vendor-report-rules.md` (flavor: malware | apt | null; optional overlay: vuln)
**General documentation templates**: `references/templates.md`


## Task Completion Self-Check (MUST pass before claiming completion)

- [ ] Did I execute every step of the workflow (rather than just reading it)?
- [ ] Did I use real tool paths based on `tool-index`?
- [ ] Did I produce reproducible evidence (commands/scripts/screenshots/reports)?
- [ ] Does the report contain Evidence / Finding / Path (ops contract)?
- [ ] Did I complete and write back the Checklist items required by RULES?
