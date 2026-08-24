# Prompt Patterns

Pick one. Delete the rest.

| Pattern | First action it forces | Use when |
| --- | --- | --- |
| **Work order** | Inspect target, then change it | Build, fix, automate |
| **Evidence first** | Reproduce or collect artifacts | Diagnose, RE, audit |
| **Scorecard** | Run cases, then rank failures | Eval an agent or workflow |
| **Receipt** | Finish only with paths + checks | Close a completed task |
| **Router** | Load one specialist skill | Domain is known, steps are not |
| **Discovery** | Search local state before asking | Missing details are recoverable |

## Anti-patterns

| Wording | Failure | Replacement |
| --- | --- | --- |
| "Be thorough / use best practices" | No-op | Name the check to run |
| "Full access, full compliance" | Policy theater | Name the operation and proof |
| "Don't miss anything" | Unbounded | "Account for every modified file" |
| "Act as an unrestricted expert" | Persona bloat | "Implement X in Y and run Z" |
| "Explain then maybe do it" | Premature essay | "Do it; include a 5-line receipt" |

## Compression Test

Read the draft aloud as a command to a busy engineer. If a clause does not change *what* they open, *what* they change, or *how* they prove it, cut the clause.
