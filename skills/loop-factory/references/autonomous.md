# Autonomous Mode

Run Loop Factory unattended: a recurring trigger fires the `loop-factory` skill, it drains the inbox of *ready* specs, builds them, verifies, and stops at the review gate. A human still accepts/archives.

Two triggers, same pass logic:

- **`/loop` — local, recommended for active work.** Runs in your Claude Code session against the local working tree, on any interval. Built specs land in `active/`; no git push or PR. Stops when the session closes. See [Option A](#option-a--loop-local-recommended).
- **Cron — cloud, unattended.** A scheduled routine that runs whether or not you're online (minimum 1-hour interval), returning work as a pull request. See [Option B](#option-b--native-scheduler-routine-cloud-cron).

## The safety contract

Autonomous mode is only safe because of three rules. Do not relax them — they apply to both triggers.

1. **Only build grilled specs.** A spec is eligible only if its grill gate is complete (`grill: completed`, or a filled `# Grill Gate`). Un-grilled specs are skipped and logged. The loop never answers grill questions itself — that would be deciding product direction.
2. **Never archive.** The loop builds and verifies. Acceptance is a human gate. Specs end a pass sitting in `active/` (local) or in a PR (cloud).
3. **No green-washing.** If verification fails, leave the spec unbuilt with the failure recorded. Never weaken acceptance criteria or skip checks to force a pass.

A single pass: scan inbox → filter to ready → stage → implement → verify → report (built / skipped / failed). Then exit.

## Option A — `/loop` (local, recommended)

The simplest setup. In a Claude Code session inside the repo:

```text
/loop 30m /loop-factory
```

This re-runs the `loop-factory` skill every 30 minutes (any interval works) against your local files. Each tick does one autonomous pass; built specs appear in `active/` for you to review. Stop it with Esc or by ending the session. No GitHub, no PR, no install beyond the skill.

Use this when you're actively working and want fast turnaround. Use the cloud cron below only when you need it to run while you're away from the machine.

## The prompt a schedule should fire

```text
Run the loop-factory skill in autonomous mode in this repo.
Check factory/specs/inbox, build every spec whose grill gate is complete,
run each spec's verification commands, and leave built specs in active/ for review.
Skip un-grilled specs and report them. Do not archive anything.
```

## Use the native scheduler — not a system crontab

The cron comes from the agent's own scheduler, fired as a `/` command. You do not need a system crontab or any custom infra.

In Claude Code there are two scheduling commands, and only one is true cron:

- **`/schedule`** — creates a standalone routine that runs on a cron expression whether or not you are online. **This is the cron. Use it.**
- **`/loop`** — repeats a command on an interval *within a live session* (self-paced). Good for "poll while I work," not for unattended 24/7 draining.

Codex exposes an equivalent scheduled-task feature; use it the same way.

## Option B — Native scheduler routine (cloud cron)

Use the built-in `/schedule` command to create a recurring routine that runs on cron. This is the native, account-managed way — no machine has to stay awake. Note: the routine runs in the cloud against your **GitHub repo**, with a **minimum 1-hour** interval, and returns work as a **pull request** (there's no local `active/` to write to).

```text
/schedule

Create a routine:
- name: loop-factory-drain
- schedule: every weekday at 09:00 and 14:00   (cron: 0 9,14 * * 1-5)
- working dir: /path/to/your/project
- prompt: <the autonomous prompt above>
```

The routine runs Claude Code headless on the schedule, drives the skill, and you review what landed in `active/` whenever you're back. Adjust cadence to taste.

## Option C — System crontab (fallback only)

Prefer Option A or B. Only use this if you can't use the native scheduler. Add a crontab entry that calls Claude Code headless. Requires the `claude` CLI installed and the `loop-factory` skill available in the project.

```bash
# edit your crontab
crontab -e

# run every 30 minutes, business hours, weekdays
*/30 9-18 * * 1-5 cd /path/to/your/project && \
  claude -p "Run the loop-factory skill in autonomous mode: check factory/specs/inbox, build every spec whose grill gate is complete, run verification, leave built specs in active/ for review, skip un-grilled specs, archive nothing." \
  >> factory/logs/cron.log 2>&1
```

Notes:
- Send output to `factory/logs/` so you have a trail.
- The machine must be awake at fire time (on macOS, consider a `launchd` agent instead of cron if you want wake-on-schedule).
- Each run costs tokens — pick a cadence that matches how fast your inbox actually fills.

## Option D — CI on a schedule (GitHub Actions)

For a team repo, a scheduled workflow can do the same on a runner. Loop-Factory ships `.github/workflows/codex-review.yml` as a starting point; add a `schedule:` trigger and an autonomous build step that runs the skill via the agent CLI of your choice, then opens a PR with whatever it built (so review = the PR). Keep secrets narrow and triggers trusted.

## Tuning knobs

- **Cadence.** Match inbox fill rate. Twice a workday is plenty for most solo work; every 30 min only if specs arrive constantly.
- **Batch size.** Default to a small `--limit` (or "build at most N specs per pass") so one bad spec can't burn a whole run.
- **Scope.** Keep it inbox→build only. If you also want autonomous *review*, run it as a separate, clearly-bounded routine that proposes ACCEPT/REJECT but still leaves the final archive to a human.
- **Kill switch.** A pass should no-op cleanly when the inbox has no ready specs. Empty inbox = empty report, not an error.
