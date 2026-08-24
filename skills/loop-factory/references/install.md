# Install & Setup

Loop Factory has two pieces:

1. The **`loop-factory` CLI** — a dependency-free Python tool that owns state transitions (inbox → active → archive) and generates prompts. This is all you strictly need.
2. The optional **native agent adapters** — skills, subagents, and hooks that make Claude Code and Codex already understand the loop's rules inside a repo.

## 1. Install the CLI

```bash
git clone https://github.com/JuliusBrussee/Loop-Factory.git
python3 -m pip install -e ./Loop-Factory
loop-factory doctor
```

Requirements: Python 3.10+. No other runtime dependencies.

`doctor` prints where the repo and factory roots are, whether `git`, `codex`, and `claude` are on PATH, how many inbox/active specs exist, and any specs missing a grill gate.

### Run without installing

Inside a checkout of the Loop-Factory repo you can skip `pip install` entirely:

```bash
python3 bin/loop-factory <command>
```

## 2. Scaffold a project

From the root of any git repository:

```bash
loop-factory init
```

This creates the file-based state directories:

```
factory/
  specs/{inbox,active,archive}/
  prompts/      # generated agent prompts (artifacts)
  runs/         # one JSON manifest per dispatch (audit trail)
  reviews/      # accepted-review records
  logs/
```

`init` only creates these folders. It does not copy the native agent adapters — do that separately if you want them (next section).

## 3. Optional: copy the native agent adapters

The Loop-Factory repo ships repo-native integrations. Copy whichever your agent uses into your target project:

**Claude Code**
- `.claude/skills/*` — loop-factory, review-and-archive, backpropagate-specs
- `.claude/agents/*` — spec-implementer, spec-reviewer, spec-backpropagator subagents
- `.claude/loop.md` — the loop prompt
- `.claude/settings.json` + `.claude/hooks/*` — status hooks

**Codex**
- `.agents/skills/*`
- `.codex/agents/*`
- `.codex/hooks.json` + `.codex/hooks/*`
- `.github/workflows/codex-review.yml` — optional CI review

These are convenience layers. The CLI + this skill are enough to run the loop by hand.

## 4. Install the agent CLIs (for `--execute`)

You only need these if you want Loop Factory to actually run the agent, not just write prompt files.

**Claude Code**
```bash
curl -fsSL https://claude.ai/install.sh | bash
# or: brew install --cask claude-code
# or: npm install -g @anthropic-ai/claude-code
```

**Codex**
```bash
curl -fsSL https://chatgpt.com/codex/install.sh | sh
# or: npm install -g @openai/codex
# or: brew install --cask codex
```

## 5. GitHub Action (optional CI review)

To use `.github/workflows/codex-review.yml`, add an `OPENAI_API_KEY` repository secret. Keep action permissions narrow and trigger only on trusted events.

## Verify the install

```bash
loop-factory doctor          # setup health
loop-factory scan            # should list any seed specs
python3 -m unittest discover # if working inside the Loop-Factory repo itself
```
