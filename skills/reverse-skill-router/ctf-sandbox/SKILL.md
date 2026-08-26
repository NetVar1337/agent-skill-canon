---
name: router-reverse-skill-router-ctf-sandbox
description: Thin PRIMARY for CTF / AWD / range multi-type orchestration. Hands off to the sidecar CTF-Sandbox-Orchestrator. Use when the user says CTF, AWD, range, or competition task and no more specific pwn/APK/IDA route already won.
---

# CTF sandbox entry (sidecar, not a second router)

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: run `../scripts/case-init.ps1`; acting against the real external internet is forbidden until `auth.status=granted`. Competitions/ranges use `-NetworkProfile lab` or `offline`.
2. `NOW`: if `../../CTF-Sandbox-Orchestrator/ctf-sandbox-orchestrator/SKILL.md` exists at the package root, open it and continue under its sandbox assumptions. **When that sidecar is absent (current workstation state)**: skip this step — this skill acts as the orchestrator directly, routing by problem type: `pwn-chain` (pwn/ROP), `apk-reverse` (APK), `ida-reverse`/`r2mcp-basic` (RE), web classes to `api-security`/`pentest-tools`; range boundaries still follow the scope's NetworkProfile.
3. `MUST NOT` write the 40+ `competition-*` sub-skills into `routing.json`. This entry is only a PRIMARY gate.
4. `ACT`: the orchestrator picks a downstream `competition-*`. When the problem type is already explicit (pwn/ROP, APK, IDA), an earlier `routing.json` rule should have won — don't steal it.

## Why a separate layer

`CTF-Sandbox-Orchestrator/` is a **GPL sidecar pack**; its authorization default is inside the sandbox. The core routing pack stays MIT + the `scope.md` gate. This skill is only a keyword entry point — it does not merge the competition tree into the core.

## Pre-completion self-check (MUST pass before claiming done)

- [ ] Did I run case-init / scope first, instead of treating "user said CTF" as authorization for the open internet?
- [ ] Did I open the sidecar orchestrator (or take over via the routing table above when the sidecar is missing), rather than treating the 40 sub-skills as PRIMARY?
- [ ] If the task was really pwn/APK/IDA, did I let the more specific PRIMARY take over?
