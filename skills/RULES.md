# RULES — hard rules and completion checklist

> This file is the rule source referenced by the router skill. It applies to every sub-skill in the reverse-skill pack.

## RFC 2119 semantics

- `MUST` / `MUST NOT`: hard gate — violating it is task failure or a security violation.
- `SHOULD`: do it by default; skipping requires a one-line justification.
- `MAY`: optional.

## Authorization gate (before every task)

1. Run `case-init` to create `work/<case>/scope.md` stating the target, its origin, and the authorization basis (the operator's standing workstation authorization can be recorded as "standing").
2. While `auth.status = granted` is not yet recorded in `scope.md`, you must not ACT against targets (no active operations); even read-only analysis requires scope first. ACT is forbidden until ready.
3. Hard stops (inherited from the workstation AGENTS.md): no exfiltrating secrets, no overwriting uncommitted work, no sending messages/opening public PRs on the user's behalf, no force-push to main.

## Tool usage

1. Tool paths are governed exclusively by `tool-index.md` (`~/.agents/skills/tool-index.md`). Missing tool → bootstrap (if in the manifest) or record it as missing; MUST NOT invent paths.
2. Version-sensitive conclusions (offsets, struct layouts, syscall numbers) MUST record the target build identifier.

## Post-task hard checklist (MUST pass all before claiming completion)

- [ ] Routing tri-axis completed: target type × user intent × toolchain, with the PRIMARY actually executed, not just read.
- [ ] Sufficiency gate applied: `ops/analysis-decision-framework.md` checked before declaring analysis complete (`R4*` validated sufficiency; `E-insufficient-evidence` blocks conclusions; ungrounded hypotheses flagged).
- [ ] Every conclusion is traceable: Evidence (command output/file path) → Finding → Path.
- [ ] Target build / version / architecture recorded (where applicable).
- [ ] Unverified inferences explicitly labeled "inferred", separated from facts.
- [ ] Artifacts produced (PoCs, dumps, reports) reported with paths; temp files cleaned up or noted in scope.
- [ ] Experience written back to `field-journal/`: check `_index.md` before new tasks; after completion append an entry per `_template.md` (date_topic.md); update precedent files (precedent-*.md) as needed.
- [ ] If tools were installed/upgraded, `tool-index.md` was updated.

## field-journal write-back format

Filename: `YYYY-MM-DD_short-topic.md` (see `_template.md`). At minimum: target and build, what was done, pitfalls, reusable conclusions, related skill name. Write-back is a `SHOULD` (upgraded to `MUST` when the user wants a deliverable).
