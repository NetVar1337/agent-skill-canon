---
name: zdi-submission-prep
description: Prepare, quality-check, and package one vulnerability report for TrendAI Zero Day Initiative (ZDI). Use when triaging whether a finding fits ZDI criteria, checking novelty against ZDI advisories or authenticated bulletins, drafting the mandatory English report, assembling PoC attachments, or preparing a case for portal submission.
compatibility: Requires web access for current criteria and novelty checks. Dynamic reproduction must use an appropriate isolated test environment.
---

# ZDI Submission Preparation

Prepare exactly one vulnerability per case. This skill ends with a reviewable case package; use `zdi-portal-submit` only after the package passes the completion gate.

## Supporting skills

Load only what the case needs:

- `vuln-research` for affected-version testing, novelty analysis, crash minimization, and general triage.
- `windows-driver-0day`, `ida-reverse`, `reverse-engineering`, `code-audit`, or another target-specific skill for root-cause evidence.
- `exploit-dev` only after the vulnerability and primitive are confirmed.
- `docs-generator` for the final report when available.

## Required case package

Create or normalize this structure without modifying original evidence:

```text
zdi-case/
├── submission.md             # Portal-ready English report
├── attachments/             # PoC code and portal-uploadable supporting files
├── evidence/                # Logs, traces, dumps, screenshots, debugger output
├── originals/               # Read-only vulnerable product/installers/samples
├── hashes.sha256             # Hashes of submitted files
└── review.md                 # Eligibility, novelty, reproduction, and claim checks
```

Start `submission.md` from [assets/submission-template.md](assets/submission-template.md). Never place portal cookies, passwords, API keys, private keys, or unrelated personal data in the package.

## Phase 1: Eligibility gate

Check current ZDI criteria instead of relying only on this skill. Record dated source URLs in `review.md`.

Mandatory fit:

- The issue exists in the latest available affected-product version.
- The affected product has widespread deployment.
- The issue is not already public or otherwise known.

ZDI preference areas include remote code execution, enterprise and server software, desktop/mobile operating systems, browsers, SCADA/IIoT, sandbox or VM escapes, and security products.

ZDI does not commonly purchase XSS, DLL planting, live-website issues, ActiveX, most consumer-only products (including games), beta/pre-release software, or publicly disclosed/known issues. Treat these as low-fit rather than silently overstating eligibility.

For novelty, search at minimum:

1. Vendor advisories, release notes, issue trackers, and security bulletins.
2. CVE/NVD and relevant ecosystem databases.
3. Public ZDI advisories and blog posts.
4. The authenticated ZDI bulletin page at `https://www.zerodayinitiative.com/portal/bulletins/` when available.
5. Distinctive crash signatures, vulnerable function names, product/version strings, and root-cause patterns.

Record search terms, dates, URLs, and results. A title-only search is insufficient.

## Phase 2: Evidence gate

Before drafting, establish:

- Exact product, edition, architecture, build/version, download source, and SHA-256.
- Latest-version evidence and test date.
- Attacker prerequisites and reachable trust boundary.
- Minimal deterministic reproduction from a clean state.
- Input-to-vulnerable-condition code/data flow.
- Root cause rather than only the terminal crash.
- Demonstrated effect and clearly separated exploitability inference.
- Affected and unaffected versions where practical.

Label important statements as `observed`, `inferred`, or `unverified` while researching. Convert them to plain report prose only when evidence supports the claim.

## Phase 3: Draft the portal fields

Write in English. The vulnerability name must be descriptive, alphanumeric with spaces, and no more than 255 characters. Use the required detailed-description order:

1. Vulnerability title.
2. High-level overview and possible effect.
3. Exact affected product with complete version information.
4. Root-cause analysis: vulnerable condition, code flow, relevant sizes/offsets/injection points, and suggested fix when supportable.
5. Proof of concept: execution prerequisites, exact commands, expected result, and attachment names. Put all PoC code in attachments, not only inline.
6. Official software download link used for vetting.

Do not combine variants or independent root causes merely because they affect the same product. Create a separate case unless one root cause and one fix clearly cover them.

## Phase 4: Package attachments

- Attach all PoC source, build files, trigger files, and concise reproduction instructions needed for independent verification.
- Portal uploads must not be pre-encrypted; the portal encrypts them.
- PoC videos of 50 MB or less may be uploaded unencrypted through the portal encryption flow.
- Videos larger than 50 MB must be encrypted separately with ZDI's current PGP key and shared by cloud-drive link. Never use YouTube or another streaming/player service.
- Additional files sent later by email must be encrypted with ZDI's current PGP key and use the case ID in the subject.
- Remove credentials, portal cookies, unrelated customer data, symbols not redistributable, and avoidable personal information.
- Hash every attachment and record the digest in `hashes.sha256`.

Run:

```bash
python scripts/validate_case.py <path-to-zdi-case>
```

Resolve every error before portal handoff.

## Completion gate

- [ ] One vulnerability/root cause per case.
- [ ] Latest available version reproduced and documented.
- [ ] Widespread deployment evidence recorded.
- [ ] Novelty search includes public advisories and ZDI bulletins.
- [ ] Minimal PoC reproduces from a clean state.
- [ ] Root cause and input-to-sink flow are supported by evidence.
- [ ] Impact wording does not exceed the demonstrated primitive.
- [ ] Mandatory report sections and software link are complete in English.
- [ ] PoC code exists in `attachments/` and all referenced files exist.
- [ ] Attachment hashes and size/encryption routing are correct.
- [ ] Payment method and discovery credit are explicitly selected.
- [ ] `validate_case.py` exits successfully.

After this gate, load `zdi-portal-submit` and perform a dry-run review before any submission.
