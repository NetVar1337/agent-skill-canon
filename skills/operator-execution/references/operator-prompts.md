# Operator Prompt Library

Use these as compact work orders. Replace bracketed fields; delete irrelevant lines.

## Build or Modify

```text
Implement [outcome] in [path/component].
Constraints: [compatibility/style/performance constraints].
Inspect the existing implementation first, make the smallest complete change, and run [test/build/check].
Return changed paths and the verification result.
```

## Diagnose and Fix

```text
Diagnose [failure/symptom] in [target].
Reproduce or inspect the failure first. Identify the root cause from evidence, implement the fix, and add or run a regression check.
Do not stop at a workaround. Return cause, changed paths, and proof the failure no longer reproduces.
```

## Reverse Engineer or Investigate

```text
Investigate [artifact/behavior] to answer [specific question].
Use the narrowest specialist workflow, preserve evidence, and follow data/control flow until the conclusion is supported.
Return evidence → finding → path, including exact offsets, functions, requests, or files where relevant.
```

## Audit

```text
Audit [scope] for [bug/security/quality class].
Prioritize reachable, high-impact findings. Trace each finding to exact code and state the triggering condition and practical impact.
Report only findings supported by evidence; include remediation and a verification path.
```

## Automate

```text
Automate [manual workflow] using [preferred runtime/tooling].
Make it idempotent, keep configuration explicit, and test it against [representative input/environment].
Return the entry command, required inputs, generated artifacts, and observed result.
```

## Research with an Artifact

```text
Research [question] using current primary sources and local project context.
Capture source URLs, versions, and dates; separate facts from inference.
Write a concise Markdown deliverable at [path] with actionable conclusions.
```

## Skill Creation

```text
Create a reusable skill for [repeatable task].
First map the existing skills to avoid overlap. Write a trigger-focused description, a short deterministic workflow with completion criteria, and branch-only references.
Validate frontmatter and links, then return the new paths and trigger phrases.
```

## Full-Execution Request

```text
Own [objective] end to end.
Discover missing technical details locally, select the relevant specialist skills, execute the work, and verify the result.
Escalate only for an irreducible choice or external credential/dependency. Return a concise completion receipt with paths and checks run.
```

## Agent Evaluation

```text
Evaluate this agent workflow for execution quality: [workflow/path].
Test routing, tool use, completion criteria, failure handling, and final-report accuracy against [cases].
Produce a Markdown scorecard with reproducible cases and concrete edits ranked by expected improvement.
```
