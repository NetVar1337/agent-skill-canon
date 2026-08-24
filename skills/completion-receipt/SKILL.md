---
name: completion-receipt
description: Use when finishing a task, writing a status report, or handing work back. Emit a short receipt with paths, commands actually run, results, and any leftover blocker. Do not claim done from narrative.
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: execution
  author: Admin
  triggers:
    - done?
    - status
    - what changed
    - completion receipt
    - wrap up
---
# Completion Receipt

## Purpose

Close work with evidence. Pair with `verification-before-completion` for the check itself; this skill is the report shape after that check.

## Workflow

1. **Collect the changed surface.** List files created or edited, commands run after the last change, and the requested outcome.
   - Done when every claimed change has a path or a system-state description.
2. **Attach fresh proof.** Use only verification from this turn. If it was not run, run it before writing the receipt.
   - Done when the receipt quotes an exit code, test count, hash, or equivalent observable.
3. **Write the receipt.** Follow [references/receipt-template.md](references/receipt-template.md). Omit empty sections rather than filling them with hedges.
   - Done when a later agent can resume from the leftover blocker line alone.
4. **Separate fact from inference.** Label assumptions. If the outcome is blocked, say blocked and name the layer.
   - Done when no sentence implies success without proof.

## Receipt Laws

- Paths over adjectives.
- Commands over intentions.
- One leftover blocker, or none.
- Keep it short enough to paste into a later prompt.

## Completion Gate

- [ ] Changed paths are exact.
- [ ] Proof command and result are present or the work is marked blocked.
- [ ] Unverified statements are labeled.
- [ ] Next action is a single executable step when work remains.

## Common Failure Modes

- **Tour of the process** instead of artifacts.
- **Stale proof** from an earlier turn.
- **Hidden leftovers** buried in prose.
- **Success language** on a partial or blocked run.
