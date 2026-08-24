---
name: ctf-sandbox-orchestrator
description: "Sidecar orchestrator for CTF / AWD / 靶场 / sandbox multi-surface engagements. Takes over after the ctf-sandbox PRIMARY routes in: sets up the lab network profile, initializes the case with auth.status=granted, and dispatches each challenge to its specialist (pwn-chain, apk-reverse, ida-reverse/r2mcp-basic, api-security/pentest-tools) without inventing new workflows. Use when a CTF/靶场 task needs multi-challenge orchestration, case init, network profile scoping, or downstream routing decisions."
---

# CTF Sandbox Orchestrator (sidecar)

This is the operational sidecar that `ctf-sandbox` hands off to. It is NOT a second
router and it does NOT duplicate the 40+ `competition-*` playbooks. It sets up the
lab and dispatches each challenge to the specialist that owns it.

## Entry contract

`ctf-sandbox` is the thin PRIMARY. When it fires, it calls this orchestrator with the
intent "orchestrate this CTF/靶场 session." Your job, in order:

1. **Case init.** Run `../scripts/case-init.ps1` with `auth.status=granted` so the
   session is a scoped, granted case. Never skip case init on a multi-challenge task.
2. **Network profile.** Set the sandbox `-NetworkProfile lab` (or `offline` when no
   egress is allowed). Scoping here prevents a challenge from poking outside the lab.
3. **Route, don't solve.** For each challenge, name the owning specialist and hand off:
   - pwn / ROP / kernel pwn → `pwn-chain`
   - APK / Android → `apk-reverse`
   - IDA / deep binary → `ida-reverse` or `r2mcp-basic` / `radare2`
   - web / API → `api-security` or `pentest-tools`
   Do not re-invent the workflow a specialist already defines. Your value is
   sequencing and scoping, not re-deriving methodology.
4. **MUST NOT** enumerate the 40+ `competition-*` playbooks into the prompt. Use
   `routing.json` if it exists; otherwise pick the specialist by challenge type.

## Scope guard

- `CTF-Sandbox-Orchestrator/` is a GPL-licensed third-party sidecar. Treat it as an
  external checkout, not as canon you rewrite. Canon skills are MIT + `scope.md`.
- Never run a challenge against a live third-party network; the lab profile is the
  boundary. `-NetworkProfile lab` and `offline` are the default states.

## Verification gate

- [ ] Case init ran and reported `auth.status=granted`
- [ ] Network profile applied (lab/offline) before any challenge
- [ ] Every challenge mapped to exactly one specialist PRIMARY
- [ ] No `competition-*` enumeration occurred in the handoff
- [ ] Each challenge's completion is checked by its specialist's verification gate

## Pair with

- `ctf-sandbox` — thin entry that routes here
- `attack-chain` — when the sandbox is multi-node and the task is a path, not a single pwn
- `case-review` — when packaging the completed CTF case for a report
