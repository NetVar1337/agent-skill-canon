# Receipt Template

Copy and delete unused lines.

```text
Outcome: [met / partial / blocked]
Changed:
- [path] — [created|edited|deleted]
Proof:
- [command] → [exit/result]
Assumptions:
- [only if needed]
Leftover:
- [none | one next executable action]
```

## Examples

Met:

```text
Outcome: met
Changed:
- C:\Users\Admin\.agents\skills\prompt-forge\SKILL.md — created
Proof:
- python frontmatter/link check → OK (3523 chars)
Assumptions: none
Leftover: none
```

Blocked:

```text
Outcome: blocked
Changed: none
Proof:
- cargo test -p parser → missing rustc in PATH
Assumptions: none
Leftover: install the project Rust toolchain, then rerun cargo test -p parser
```
