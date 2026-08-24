---
name: skill-curator
description: Use when adding, improving, consolidating, or evaluating agent skills and their Markdown references. Find real workflow gaps, avoid duplicate skills, write trigger-precise SKILL.md files, and validate each skill as an executable instruction surface.
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: skill-engineering
  author: Admin
  triggers:
    - make a skill
    - add skills
    - improve skills
    - skill gap
    - better agent prompts
---
# Skill Curator

## Purpose

Build a small set of high-leverage skills rather than a pile of overlapping Markdown. A skill earns its permanent description only when it changes routing or execution behavior for a distinct, recurring workflow.

For the authoring mechanics and context-load rules, read `writing-for-agents` first. Use `operator-execution` when the skill work itself is part of a larger task.

## Workflow

1. **Inventory the behavior.** Search installed skills, project instructions, scripts, and recurring requests for an actual workflow gap.
   - Done when the gap is stated as a trigger → outcome → verification triplet and existing coverage is known.
2. **Choose the cheapest shape.** Extend a current skill when the trigger and workflow overlap; otherwise create a new `SKILL.md`. Put long examples, prompt collections, checklists, and branch-only material in `references/`.
   - Done when the new context load is justified and every file has one job.
3. **Write the execution surface.** Use YAML frontmatter with a trigger-specific `description`; then provide an ordered workflow, checkable completion criteria, failure handling, and a verification gate. Prefer concrete verbs, paths, commands, and observable outputs.
   - Done when an unfamiliar agent can execute the workflow without generic advice.
4. **Validate and prune.** Parse frontmatter, check links, test the trigger phrases against likely user wording, and remove duplicate, stale, or no-op instructions.
   - Done when every rule changes behavior and every reference resolves.
5. **Register only what is discoverable.** Ensure the intended loader scans the location. Update an index or router only when it is the authoritative discovery surface; avoid repeating the same skill list across global instructions.
   - Done when a fresh session can discover the skill by the intended trigger.

## Quality Rules

- The description is a routing contract: lead with `Use when`, name distinct trigger branches, and state the resulting action.
- One skill has one primary job. Split only when the trigger or execution path is independently reusable.
- Put universal workflow steps in `SKILL.md`; progressively disclose reference tables and templates.
- End steps with an observable done condition, not "be thorough" or "understand the task."
- Prefer positive instructions describing the desired behavior over large prohibitory lists.
- Add prompts only when they create a reusable, parameterized work order; a prompt must specify outcome, constraints, and proof.

## Review Matrix

Apply [references/skill-quality-gate.md](references/skill-quality-gate.md) before keeping a skill. A skill that fails the overlap, trigger, or verification checks is revised or deleted rather than added to the catalog.

## Completion Gate

- [ ] Existing skills were searched before creating a sibling.
- [ ] Frontmatter begins at byte zero and `name` plus `description` are present.
- [ ] Trigger phrases distinguish the skill from adjacent skills.
- [ ] Each workflow step has a checkable completion condition.
- [ ] Every Markdown link resolves locally.
- [ ] The skill was placed in a directory scanned by its target agent.
- [ ] The final reply names every added or changed path and validation performed.

## Common Failure Modes

- **Taxonomy bloat:** consolidate adjacent skills under one strong trigger and use headings or references for branches.
- **Generic prompts:** replace exhortations with inputs, an expected artifact, and a verification command.
- **Stale indexes:** point to the canonical skill root instead of copying a catalog into multiple global files.
- **Prompt-only skills:** write a workflow around the template so the agent knows when and how to validate its output.
- **Unverifiable guidance:** express the expected observable result or a command that proves it.
