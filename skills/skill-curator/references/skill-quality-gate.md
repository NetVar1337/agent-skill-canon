# Skill Quality Gate

Score each category 0–2. Keep a new skill only when it scores at least 10/12 and scores 2 in **Distinct trigger**, **Executable path**, and **Verification**.

| Category | 0 | 1 | 2 |
| --- | --- | --- | --- |
| Distinct trigger | Duplicates a broad existing skill | Trigger is fuzzy or overlaps | Names a recurring, distinguishable request class |
| Executable path | Advice only | Some actions, unclear sequence | Ordered actions lead to an observable result |
| Verification | No success signal | Vague quality claim | Command, test, artifact, or state proves completion |
| Context discipline | Long, generic description | Some reference is inlined | Description routes; branch detail is disclosed |
| Local fit | Ignores installed tools and style | Mentions them loosely | Uses real paths, tools, and conventions from the target environment |
| Maintenance | Duplicates sources or indexes | Partial ownership is clear | One source of truth; references and update point are explicit |

## Fast Review Questions

1. Which exact user phrases should cause this skill to load?
2. What task does it do that a current skill cannot already do?
3. What is the first evidence-gathering or state-changing action?
4. What artifact or command demonstrates successful completion?
5. Which section is needed on every run, and which should move to a reference?
6. If this skill vanished, what repeated failure would return?

## Prompt Test

For every reusable prompt, check that it contains:

```text
objective + target + constraints + completion evidence
```

A good prompt gives the agent latitude over implementation while fixing the outcome and proof. Examples that merely say "be detailed," "fully comply," or "use full access" are not executable until they state a target operation and a success condition.
