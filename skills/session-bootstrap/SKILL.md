---
name: session-bootstrap
description: Use at session start, after context loss, or when the agent seems lost in the environment. Map cwd, repo state, relevant skills, and constraints, then take the first action on the operator's actual request.
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: execution
  author: Admin
  triggers:
    - where am I
    - bootstrap this session
    - you lost context
    - start here
    - orient
---
# Session Bootstrap

## Purpose

Rebuild just enough environment to act. Orientation is a prelude, not the deliverable.

## Workflow

1. **Probe the workspace.** Determine cwd, whether it is a git repo, dirty state, and the operator's current request. Use [references/environment-probe.md](references/environment-probe.md) for the command set.
   - Done when you can name the working tree and whether uncommitted work exists.
2. **Select the live instructions.** Read the nearest `AGENTS.md` / project instructions only if they apply to this tree. Load one specialist skill for the request, not a survey.
   - Done when the next action is pointed at a real file or command.
3. **State the map in five lines or fewer.** cwd, repo/branch/dirty, relevant skill, constraint, first action.
   - Done when the map fits in five lines and contains no capability advertisement.
4. **Start the request.** The bootstrap is finished only after the first request-advancing tool call.
   - Done when inspection or mutation of the actual target has begun.

## Rules

- Do not list installed skills, models, or policies unless the request is about those things.
- Prefer local evidence over asking the operator to restate files you can read.
- If context was lost, recover from a handoff, receipt, or git state before asking for a recap.

## Completion Gate

- [ ] cwd and repo dirty-state are known.
- [ ] One relevant skill or none, with a reason.
- [ ] First request-advancing action happened in the same turn.
- [ ] No inventory dump.

## Common Failure Modes

- **Capability brochure** instead of a map.
- **Stopping after ls.**
- **Reloading every adjacent skill.**
