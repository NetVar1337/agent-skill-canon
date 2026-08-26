# Security

## Repository hygiene

This repository is public. Do not commit live credentials, private keys, access tokens, customer data, proprietary samples, captures, crash dumps, or engagement artifacts. Use obvious non-secret placeholders in examples and run local secret scanning before publishing imported material.

If GitHub push protection identifies an example token, replace it with a format that cannot be mistaken for a credential rather than bypassing the protection.

## Reporting repository issues

For exposed credentials or a vulnerability in repository automation, report privately through GitHub Security Advisories for `NetVar1337/agent-skill-canon`. Include the affected path, commit, impact, and a minimal reproduction. Do not place the secret itself in a public issue.

Upstream skill content should also be reported to its attributed upstream project when the defect originates there.
