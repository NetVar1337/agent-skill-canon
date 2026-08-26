---
name: browser-automation
description: |
  Unified automation entry point. Covers browser automation (Playwright) and Windows desktop application automation (OpenReverse).
  Browser scenarios: opening web pages, clicking, filling forms, crawling, screenshots, automated login, pentest page interaction.
  Desktop scenarios: operating GUI tools like IDA/x64dbg, Windows UI Automation, vision-driven interaction, desktop application traffic capture.
  Trigger keywords: browser automation, desktop automation, open web page, fill form, crawl, screenshot, automated login, Playwright, agent-browser, headless, OpenReverse, UIA, CUA, desktop operation, Windows automation.
---

# Automation Operations (Desktop & Browser Automation)

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: confirm whether the current task falls within this skill's scope
2. `NOW`: read `../tool-index.md`, verify tool availability and actual paths
3. `NEXT`: when tools are missing, invoke bootstrap; do not guess paths
4. `ACT`: enter step 1 of the "Workflow" and execute; do not stop at the confirmation stage

## Scope

Use this skill when the task falls into one of these scenarios:

### Browser Scenarios (Playwright / agent-browser)
- Opening web pages and operating page elements (clicking, filling forms, submitting)
- Crawling page content or taking screenshots
- Automating login flows
- Interacting with web pages during penetration testing (submitting payloads, triggering XSS)
- Automated handling of CAPTCHA pages
- Bulk form submission

### Desktop Application Scenarios (OpenReverse)
- Operating Windows desktop applications (IDA Pro, x64dbg, Wireshark, etc.)
- Vision-driven interaction needed (CUA mode)
- Structured UI operations needed (UIA mode)
- Network traffic observation of desktop applications (built-in mitmproxy)
- Automated GUI operation of reverse engineering tools
- Black-box testing of desktop software

### Division of Labor with Other Tools

| Scenario | Use |
|------|--------|
| Operating web pages (inside the browser) | **Playwright / agent-browser** |
| Operating desktop applications (Windows GUI) | **OpenReverse** |
| Traffic capture analysis, HTTP request capture | anything-analyzer or OpenReverse network lane |
| JS breakpoints, hooks, CDP debugging | jshookmcp |
| Locating signature algorithms, environment-patched reproduction | js-reverse |

Simple rule:
- Target is a web page → Playwright
- Target is a Windows desktop application → OpenReverse
- Both needed → combine them

---

## Part 1: Browser Automation (Playwright / agent-browser)

### Core Workflow

```bash
# 1. Open a page
agent-browser open <url>

# 2. Get interactable elements (returns @e1, @e2... references)
agent-browser snapshot -i

# 3. Operate elements via references
agent-browser click @e1
agent-browser fill @e2 "text"

# 4. Close when done
agent-browser close
```

### Command Reference

```bash
# Navigation
agent-browser open <url>
agent-browser close

# Page snapshot
agent-browser snapshot        # full accessibility tree
agent-browser snapshot -i     # interactable elements only (recommended)

# Interaction
agent-browser click @e1
agent-browser fill @e2 "text"
agent-browser type @e2 "text"
agent-browser press Enter
agent-browser scroll down 500

# Get information
agent-browser get text @e1
agent-browser get title
agent-browser get url

# Waiting
agent-browser wait @e1
agent-browser wait 2000
agent-browser wait --load networkidle
```

### Notes
- You must run `agent-browser close`, otherwise processes leak
- Snapshot before operating; do not guess element references
- After submitting a form, use `wait --load networkidle` to let the page settle

---

## Part 2: Desktop Application Automation (OpenReverse)

### Overview

[OpenReverse](https://github.com/zhexulong/openreverse) is a desktop interaction and evidence-collection framework for AI agents, supporting:
- **UIA mode**: Windows UI Automation, structured desktop control operations
- **CUA mode**: vision-driven interaction (Computer Use Agent), suited for complex GUIs
- **Network observation**: built-in mitmproxy proxy + local capture

### Interaction Mode Selection

| Mode | Suited Scenario | Underlying |
|------|---------|------|
| UIA | Target app has standard Windows controls (buttons, text boxes, lists) | Windows UI Automation API |
| CUA | Target app has a complex UI or non-standard controls (IDA's disassembly view, custom-rendered interfaces) | Vision recognition + mouse/keyboard |

### Network Observation Modes

| Mode | Suited Scenario |
|------|---------|
| Proxy Lane | Target app can be configured with a proxy (recommended) |
| Local Lane | Target app cannot go through a proxy; local capture needed |

### Installation and Configuration

```bash
# 1. Clone the project
git clone https://github.com/zhexulong/openreverse.git
cd openreverse

# 2. Install dependencies
npm install

# 3. Hook into agent hosts (Claude Code / Codex / Zed)
npm run init:agents -- --target=all /path/to/project

# 4. Install the CUA runtime (if vision-driven mode is needed)
npm run install:cua-runtime
npm run doctor:cua-runtime

# 5. Install network observation dependencies (if traffic capture is needed)
npm run install:mitmproxy
npm run doctor:network
```

### Common Combinations

| Need | Configuration |
|------|------|
| Only operate desktop applications | UIA or CUA, no network lane |
| Operate desktop apps + capture traffic | UIA/CUA + proxy lane |
| Operate desktop apps + local capture | UIA/CUA + local lane |

### Reverse Engineering Scenario Examples

```text
Scenario: automating IDA Pro for batch analysis

1. Open IDA Pro via OpenReverse CUA mode
2. Automatically load the target binary
3. Wait for analysis to finish
4. Export the function list via UI operations
5. Meanwhile use the network lane to observe IDA's network behavior (e.g., Lumina requests)
```

```text
Scenario: automating x64dbg debugging

1. Launch x64dbg via OpenReverse UIA mode
2. Load the target program
3. Set breakpoints
4. Run and observe register/memory changes
5. Save screenshots as evidence
```

---

## On-Demand Bootstrap

### Automation Capability Boundaries

| Tool | Auto-installable | Install Method | Notes |
|------|-----------|---------|------|
| Playwright | ✓ | npm + npx playwright install | Browser automation engine |
| agent-browser CLI | ✓ | npm install -g agent-browser | Browser operation CLI |
| Node.js | ✓ | winget | Prerequisite dependency |
| OpenReverse | ✗ | Manual clone + npm install | Experimental stage, heavy dependencies |
| mitmproxy | ✗ | Manual install | OpenReverse network observation dependency |

### Bootstrap Triggers

- Browser operation missing Playwright → auto bootstrap
- Desktop operation needs OpenReverse → guide the user through manual installation (provide full steps)

### OpenReverse Manual Installation Guide

If the AI detects that desktop application automation is needed but OpenReverse is not installed:

```markdown
⚠️ **OpenReverse is needed for desktop application automation**

**Installation steps**:
1. `git clone https://github.com/zhexulong/openreverse.git`
2. `cd openreverse && npm install`
3. `npm run init:agents -- --target=all <your project path>`
4. If vision mode is needed: `npm run install:cua-runtime`
5. If network observation is needed: `npm run install:mitmproxy`

**Verify**: `npm run doctor:cua-runtime` and `npm run doctor:network`
```

---

## Routing Context

**Upstream entry points**: `skills/SKILL.md` (master control), `routing.md`
**Applicable scenarios**: any task that requires automating a browser or desktop application
**Downstream exits**:
- Captured requests need analysis → `anything-analyzer` or `js-reverse`
- JS debugging/hooks needed → `jshookmcp`
- Signature algorithm restoration needed → `js-reverse`
- Desktop app is a reverse engineering tool → `ida-reverse/`

**Related sibling modules**: `js-reverse` (after browser operations, JS analysis may be needed), `ida-reverse` (OpenReverse can automate the IDA GUI)


## Task Completion Self-Check (MUST pass before claiming completion)

- [ ] Did I execute every step of the workflow (rather than just reading it)?
- [ ] Did I use real tool paths based on `tool-index`?
- [ ] Did I produce reproducible evidence (commands/scripts/screenshots/reports)?
- [ ] Did I complete and write back the Checklist items required by RULES?
