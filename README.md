# Agent Skill Canon

A large, executable skill library for coding agents, reverse-engineering assistants, and security-research harnesses. The repository combines **1,200+ skill files** with deterministic indexing, quality checks, Pi extensions, workstation themes, and a safe synchronization utility.

## Start here

| Goal | Entry point |
|---|---|
| Route a cross-domain security task | [`reverse-skill-router`](skills/reverse-skill-router/SKILL.md) |
| Reverse a native binary | [`core-reverse-engineering`](skills/reverse-engineering/SKILL.md), [`ida-reverse`](skills/ida-reverse/SKILL.md), [`radare2`](skills/radare2/SKILL.md) |
| Research game internals or anti-cheat | [`game-hacking`](skills/game-hacking/SKILL.md), [`game-internals`](skills/game-internals/SKILL.md), [`ac-bypass-source-index`](skills/ac-bypass-source-index/SKILL.md) |
| Work on Windows internals or drivers | [`windows-internals`](skills/windows-internals/SKILL.md), [`kernel-dev`](skills/kernel-dev/SKILL.md), [`windows-driver-0day`](skills/windows-driver-0day/SKILL.md) |
| Run a penetration test | [`pentest-tools`](skills/pentest-tools/SKILL.md), [`attack-chain`](skills/attack-chain/SKILL.md), [`web-pentest`](skills/offensive-claude/web-pentest/SKILL.md) |
| Develop or assess an exploit | [`exploit-dev`](skills/exploit-dev/SKILL.md), [`pwn-chain`](skills/pwn-chain/SKILL.md), [`vuln-research`](skills/vuln-research/SKILL.md) |
| Browse everything | [Generated catalog](catalog/CATALOG.md) · [Domain guide](docs/DOMAIN-GUIDE.md) · [Quality dashboard](catalog/QUALITY.md) · [JSON catalog](catalog/catalog.json) |

## What is included

- **Security research:** RE, exploit development, game security, Windows internals, web/API, cloud/Kubernetes, AD, malware, DFIR, mobile, firmware, hardware, OT/ICS, wireless, AI-agent security, cryptography, and Web3.
- **Systems engineering:** C/C++, Rust, Go, Zig, assembly, compilers, kernels, virtualization, build systems, debugging, and performance.
- **Agent engineering:** execution, review, orchestration, documentation, prompt and skill authoring, repository operations, and verification.
- **Pi assets:** `extensions/` and `themes/` for research-focused agent sessions.

The catalog is intentionally multi-source. Some upstream packs retain nested layouts and may declare the same skill name. The generated catalog records every path and makes duplicate names visible; root-level skills take precedence when selecting a canonical workstation entrypoint.

## Install

### Pi

```powershell
pi install .
```

### Mirror skills to local harnesses

Dry-run first:

```powershell
pwsh -NoProfile -File .\sync-skills.ps1 -WhatIf
pwsh -NoProfile -File .\sync-skills.ps1
```

The script mirrors `skills/` into Claude, Codex, Pi, OpenCode, and OMO skill roots. Existing destination-only directories are preserved unless `-Prune` is explicitly supplied.

### Manual

Copy an individual skill directory, including its `references/`, `scripts/`, `templates/`, and `assets/`, into a skill root scanned by your agent.

## Quality controls

The canon uses a dependency-free validator and deterministic catalog generator:

```bash
py -3 tools/canon.py validate --baseline config/quality-baseline.json
py -3 tools/canon.py catalog --check
py -3 -m unittest discover -s tests -v
```

Validation checks frontmatter, required routing fields, merge markers, local Markdown links, duplicate names, and generated artifacts. Legacy warnings are baseline-controlled so new changes cannot silently increase debt.

Search canonical entrypoints without loading the full catalog:

```bash
py -3 tools/canon.py search game --domain game-security
py -3 tools/canon.py search kerberos --limit 10
```

To regenerate after changing skills:

```bash
py -3 tools/canon.py catalog
py -3 tools/canon.py validate --snapshot config/quality-baseline.json
```

Only update the baseline after reviewing every changed metric.

## Repository map

```text
skills/                 skill trees and upstream packs
catalog/                generated human and machine-readable indexes
config/                 domain rules and quality baseline
extensions/             Pi extensions and extension source
extensions/pi-antigravity/src/
themes/                 Pi themes
docs/                   routing and maintenance guidance
tools/canon.py          validator + catalog generator
tests/                  validator regression tests
sync-skills.ps1         deterministic multi-harness mirror
```

## Contributing

Read [`CONTRIBUTING.md`](CONTRIBUTING.md). A useful skill must have a distinct routing trigger, an executable workflow, observable completion criteria, and valid local references. Prefer improving an existing skill over adding another synonym.

Each imported skill retains its own frontmatter license and upstream attribution where present. Repository-level metadata does not override those terms.
