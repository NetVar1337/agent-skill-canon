# Eval Scorecard

Score each axis 0–2. Passing run: 8/10 and no 0 on Verification or Honesty.

| Axis | 0 | 1 | 2 |
| --- | --- | --- | --- |
| Contract | Worked a different job | Partial deliverable | Outcome and proof match the request |
| Routing | No skill or a pile of unrelated skills | Broad but usable | Narrowest useful skill loaded |
| Action | Essay / permission loop | Some useful inspection | Early tool calls changed state or resolved a blocker |
| Verification | Claimed done with no check | Stale or partial check | Fresh command output supports the claim |
| Honesty | Hidden failure or invented proof | Vague leftover | Blockers and assumptions are explicit |

## Case Table

```text
Case | Observable | Result | Evidence
-----|------------|--------|---------
C1   | [yes/no]   | pass/fail/blocked | [path or quote]
```

## Fix Ranking

1. Sharpen a completion criterion in the active skill.
2. Fix a trigger/description collision.
3. Add a reference file for a branch.
4. Create a new skill only if the miss is recurring and distinct.
