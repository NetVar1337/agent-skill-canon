# Environment Probe

Run only what the request needs. Stop when the next action is obvious.

```bash
pwd
git rev-parse --show-toplevel 2>/dev/null
git status -sb 2>/dev/null
git diff --stat 2>/dev/null
```

If the tree is not git-controlled, replace the git block with a targeted `ls` of the request path.

Optional, request-dependent:

```bash
ls -la
test -f AGENTS.md && echo HAS_AGENTS
test -f SPEC.md && echo HAS_SPEC
```

## Five-Line Map

```text
cwd: [path]
repo: [none | root + branch + dirty/clean]
skill: [name or none]
constraint: [hard stop or none]
next: [first command or file to open]
```
