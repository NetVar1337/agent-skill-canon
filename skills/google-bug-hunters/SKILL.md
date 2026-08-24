---
name: google-bug-hunters
description: "Use when researching, validating, or reporting a security finding to the Google Bug Hunters Program. Requires current official scope/rules verification before testing or report preparation."
version: 1.0.0
license: MIT
metadata:
  package: local-skills
  author: Admin
  category: security
  sources:
    - https://bughunters.google.com/
    - https://bughunters.google.com/about/rules/about-this-section
    - https://www.google.com/about/appsecurity/
  related_skills:
    - src-hunter
    - offensive-reporting
    - vuln-research
---

# Google Bug Hunters

Use this workflow for Google Bug Hunters work: program reconnaissance, finding validation, report drafting, triage response, and post-submission evidence maintenance. Treat the live program page and applicable product rules as authoritative over this skill.

## Before testing

1. Open `https://bughunters.google.com/` and the applicable product/program rules page.
2. Record the check date, exact program, allowed assets, prohibited activity, test-account requirements, and disclosure terms in the case notes.
3. Confirm the asset is explicitly in scope. If the program page is ambiguous, do not infer permission from Google ownership alone.
4. Preserve the original page URLs and a short scope quote or screenshot. Re-check scope whenever a campaign spans multiple days or a new asset is introduced.

## Evidence-led validation

Build a minimal, reproducible case:

- Exact product, endpoint, account role, region, version/build, and prerequisites.
- Numbered reproduction steps from a clean starting state.
- Sanitized request/response pairs, console output, screenshots, traces, or a minimal proof of concept.
- The observed security effect and the smallest reliable demonstration.
- A clear distinction between observed facts, reasoned impact, and unverified assumptions.

Avoid unnecessary data access, persistence, destructive actions, and broad scanning. Keep raw user data, production secrets, session cookies, access tokens, and unrelated third-party data out of notes and attachments.

## Impact statement

Describe impact from the attacker's perspective:

1. Required access and privileges.
2. Attacker-controlled input or action.
3. Affected user, data, system, or security boundary.
4. Demonstrated confidentiality, integrity, availability, or privilege effect.
5. Reliability, prerequisites, and realistic limitations.

Do not claim a severity as final. State the evidence supporting impact and let Google triage determine its classification and reward.

## Report assembly

Prepare a concise report with:

1. Specific title and affected Google product/asset.
2. Executive impact statement.
3. Reproduction prerequisites and exact numbered steps.
4. Expected versus observed behavior.
5. Minimal evidence/PoC and required attachments.
6. Security impact, limitations, and any suggested remediation only when supported by the evidence.
7. Testing date and the rule/scope URLs used.

Before submission, independently replay the steps from a clean state. The case is ready only when another analyst can reproduce the demonstrated effect without interpretation.

## Triage and disclosure

- Submit only through the official Google Bug Hunters reporting surface.
- Preserve the original evidence and answer triage questions with additive, reproducible detail.
- Keep the finding, affected URLs, evidence, exploit details, and correspondence confidential until Google expressly authorizes disclosure.
- When new evidence changes the case, label what changed, when it changed, and whether it affects reproduction or impact.

## Official sources

- Program home: https://bughunters.google.com/
- Rules: https://bughunters.google.com/about/rules/about-this-section
- Google Application Security: https://www.google.com/about/appsecurity/
