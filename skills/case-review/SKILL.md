---
name: case-review
description: "Reviews a reverse-skill case package for scope readiness, Evidence to Finding to Path traceability, work item coverage, timeline references, and optional artifact hash integrity before report handoff."
version: 1.0.0
license: MIT
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: reverse-skill
  upstream: https://github.com/zhaoxuya520/reverse-skill
---

> Bundled with Unleash skills pack. Upstream: https://github.com/zhaoxuya520/reverse-skill

# Evidence Graph Review

Use this skill when a reverse engineering, forensics, CTF, or authorized security case needs a defensible handoff. It audits the existing `work/<case>/` package without changing the case or touching a target.

## Scope

This skill covers:

- Scope metadata and target-activity readiness
- Evidence record structure and reproducibility fields
- References from work items and timeline entries to Evidence
- Structured Findings and Paths in report Markdown
- Optional SHA-256 verification for case-local artifacts
- A Markdown or JSON review result for a report handoff

It MUST NOT perform reconnaissance, exploitation, dynamic instrumentation, or target changes. Those actions belong to the routed analysis skill and require the case scope gate.

## ACTION REQUIRED

1. `NOW`: read `../field-journal/precedent-reverse.md` and confirm that this is a review of an existing authorized case package.
2. `NOW`: confirm the case path and choose read-only review mode.
3. `NEXT`: read `../tool-index.md`; this skill uses only Python 3 standard library and does not require bootstrap.
4. `NEXT`: run `python3 scripts/review_case.py <case-root> --format markdown`.
5. `ACT`: resolve every error, then rerun the review before claiming a handoff is complete.

## Tool dependencies

| Tool | Required | Purpose | Auto-bootstrap |
|------|----------|---------|---------------|
| Python 3.9+ | Yes | Runs the read-only case review script | No, use the platform Python installation |

No network access or third-party package is required.

## Workflow

### Phase 1: Intake

Run the review against the existing case directory:

```bash
python3 skills/case-review/scripts/review_case.py work/<case> --format markdown
```

Confirm that `scope.md`, `timeline.md`, `workitems.md`, and `evidence/` are present. A non-strict review reports scope warnings while a strict review treats warnings as handoff blockers.

## Suggested next steps (pick one number)

1. Fix the authorization, scope, or network_profile fields in scope.md
2. Continue checking the reproducible commands and sources of Evidence records
3. Export the current review result and attach it to the phase report
4. Switch to JSON output to integrate with CI or other review tools
5. Pause and confirm the review scope first

### Phase 2: Traceability

Review the checks for:

- Evidence IDs that do not exist
- Findings without `evidence_ids`
- Paths without an allowed `path_type` or Evidence reference
- Work items and timeline entries pointing to unknown Evidence
- Unlinked Evidence records
- Validated Findings with low confidence

An offline observation may use `repro_command: n/a` only when its `notes` field explicitly documents the offline limitation.

Use JSON when another tool needs stable fields:

```bash
python3 skills/case-review/scripts/review_case.py work/<case> --format json
```

## Suggested next steps (pick one number)

1. Write the missing Evidence and keep the original commands
2. Bind candidate Findings to Evidence and re-review
3. Add P-ids and Path steps for the call chain or attack chain
4. Generate a Markdown handoff summary
5. Switch back to the PRIMARY skill to continue analysis

### Phase 3: Fixity verification

When an Evidence record contains both `content_hash` and `artifact_path`, verify the case-local artifact:

```bash
python3 skills/case-review/scripts/review_case.py work/<case> --verify-hashes --strict
```

The script accepts `sha256:<64 hex characters>` and checks that the artifact remains inside the case root. A hash mismatch is a hard failure.

The PowerShell Evidence helper can record a hash while appending a record:

```powershell
powershell -File skills/scripts/append-evidence.ps1 -CaseRoot work\<case> -Id E-001 -Title "Sample hash" -ReproCommand "sha256sum evidence/sample.bin" -ArtifactPath "evidence\sample.bin"
```

## Suggested next steps (pick one number)

1. Fix the hash mismatch or replace the contaminated working copy
2. Add SHA-256 and artifact_path for unfixed original files
3. Continue into the report generation phase
4. Export the JSON result for CI retention
5. Pause and request manual review

### Phase 4: Handoff

Use strict mode before a final report or specialist handoff:

```bash
python3 skills/case-review/scripts/review_case.py work/<case> --strict --format markdown > work/<case>/report/case-review.md
```

The command is read-only with respect to the case unless shell redirection is explicitly used to save its output. The review is not legal advice and does not replace organizational evidence handling procedures.

## Suggested next steps (pick one number)

1. Hand the passing review result to `docs-generator/` to produce the formal report
2. Return to the PRIMARY skill to fill in new analysis evidence
3. Archive the Markdown and JSON review results
4. Pause and request manual review

## Language behavior contract

- Internal reasoning, tool selection, and phase control: English.
- User-visible messages, section labels, reports, and next-step menus: Chinese unless the user requests another language.
- Default bilingual labels place Chinese first and English second, separated by `/`.

## Bootstrap boundary

This skill has no third-party dependency. If Python 3 is unavailable, the only allowed recovery action is the repository bootstrap path when a Python capability is registered for the current platform. If no such capability is registered, stop and report the missing runtime. Do not guess executable paths, download packages, or perform a manual install from inside this skill.

## Routing context

**Upstream entry**: any reverse, forensics, CTF, or authorized security skill that has produced a case package.

**Downstream exit**: `docs-generator/` for a formal report, or the original PRIMARY skill when the graph is incomplete.

**Related modules**: `ops/evidence-finding-path.md`, `ops/timeline-workitem.md`, `digital-forensics/`, `reverse-engineering/`, and `docs-generator/`.

## References

- [NIST SP 800-86: Guide to Integrating Forensic Techniques into Incident Response](https://csrc.nist.gov/pubs/sp/800/86/final)
- [SWGDE Best Practices for Computer Forensic Acquisitions](https://www.swgde.org/documents/published-complete-listing/17-f-002-2-1/)
- [SWGDE Best Practices for Archiving Digital and Multimedia Evidence](https://www.swgde.org/documents/published-complete-listing/19-f-003-best-practices-for-archiving-digital-and-multimedia-evidence/)

## Task Completion Self-Check

- [ ] Did I review scope.md, timeline.md, workitems.md, and evidence/?
- [ ] Do all Findings reference existing Evidence?
- [ ] Do all Paths contain a valid path_type and Evidence reference?
- [ ] Did I run hash verification, or record the reason it was not run?
- [ ] Did I rerun in strict mode and save the review result?
