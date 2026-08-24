# Operator Prompt Pack

Filled, copy-ready work orders. Replace only the bracketed fields.

## 1. Smallest Correct Change

```text
Change [path] so that [observable outcome].
Match existing style. Do not refactor adjacent code.
After the edit, run [command] and return the changed hunks plus the command output.
```

## 2. Root-Cause Fix

```text
Failure: [error/symptom].
Target: [binary/service/test].
Reproduce first. Identify the root cause from evidence, implement the fix, and add or run a regression check that fails without the fix.
Return cause, files, and the passing check.
```

## 3. Binary or Driver Investigation

```text
Artifact: [path].
Question: [one specific question].
Use the narrowest RE skill. Preserve hashes, offsets, and function names.
Return Evidence → Finding → Path. Do not stop at a tool dump.
```

## 4. Reachable Vuln Audit

```text
Scope: [tree or component].
Hunt: [bug class].
Report only findings with a triggering condition, sink, and practical impact.
Include a reproduction or negative test for each kept finding.
```

## 5. Idempotent Automation

```text
Automate [manual steps] as [script/path].
Inputs: [flags/env].
The script must be safe to re-run. Test it against [fixture/environment] and return the command line plus observed output.
```

## 6. Research Note

```text
Question: [decision the note must unlock].
Write [output-path] from primary sources and this repo.
Record versions, dates, and URLs. Separate fact from inference. End with the recommended action.
```

## 7. Skill Authoring

```text
Create or revise a skill for [repeatable trigger].
Search ~/.agents/skills first. If a sibling exists, extend it.
Write trigger-focused frontmatter, ordered steps with completion criteria, and branch-only references.
Validate YAML, links, and description length. Return paths and trigger phrases.
```

## 8. End-to-End Ownership

```text
Own [objective] until [proof] is green.
Discover missing technical details locally. Load only the specialist skills required.
Escalate only for an irreducible choice or missing external credential.
Return a completion receipt: paths, commands, results, leftover blocker.
```

## 9. Agent Scorecard

```text
Evaluate [session/transcript/workflow] against cases: [list].
Score routing, tool use, verification, and report honesty.
Write [output-path] with failing cases and the smallest instruction or skill edits that would have prevented them.
```

## 10. Resume From Artifact

```text
Continue from [handoff/spec/receipt path].
Do not re-litigate completed steps. Verify the last claimed state, then execute the next unchecked item.
Return an updated receipt.
```
