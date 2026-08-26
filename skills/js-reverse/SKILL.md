---
name: js-reverse
description: Use when doing frontend JavaScript reverse engineering with js-reverse-mcp; applies to signature-chain locating, page observation forensics, runtime sampling, local environment-patched reproduction, and evidence-based output. Prefer the js-reverse_* tools present in the current environment; bring in jshookmcp when a stronger browser/CDP/Hook surface is needed.
---

# MCP Frontend JS Reverse Engineering Operating Standard

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read `../field-journal/precedent-reverse.md` — confirm that this skill's operations are authorized routine operations
2. `NOW`: confirm whether the current task falls within this skill's scope
3. `NEXT`: read `../tool-index.md`, verify tool availability and actual paths
4. `NEXT`: when tools are missing, invoke bootstrap; do not guess paths
5. `ACT`: enter step 1 of the "Workflow" and execute; do not stop at the confirmation stage

## Scope

Prefer this skill when the task falls into one of these scenarios:

- Locating API signatures, encrypted parameters, risk-control fields
- Observing page request chains and script origins
- Capturing function arguments and return values at runtime
- Tracing the trigger point of a specific XHR/Fetch/WebSocket
- Bringing page evidence back to Node for local reproduction and environment patching

If the target is a binary, APK, PE, ELF, DLL, or SO, use `ida-reverse`, `radare2`, or `reverse-engineering` instead.

## Default Tool Mapping in the Current Environment

This skill does not assume bare tool names; it binds by default to the `js-reverse_*` tools available in the current client environment.

If the current task explicitly mentions `jshookmcp`, `JS hook`, `CDP`, browser breakpoints, network interception, SourceMap, or AST deobfuscation, still use this skill; just switch the underlying MCP surface to `jshookmcp`, rather than treating it as a new master entry point.

Precondition: `jshookmcp` is not a local bare command-line tool but an MCP server that must be downloaded, explicitly registered, and enabled first. Only after it is hooked into and enabled in the chosen client's (Claude, Codex, etc.) MCP configuration do the related tool surfaces actually become callable.

Common mappings:

- `list_scripts` -> `js-reverse_list_scripts`
- `get_script_source` -> `js-reverse_get_script_source`
- `search_in_sources` -> `js-reverse_search_in_sources`
- `break_on_xhr` -> `js-reverse_break_on_xhr`
- `evaluate_script` -> `js-reverse_evaluate_script`
- `get_paused_info` -> `js-reverse_get_paused_info`
- `set_breakpoint_on_text` -> `js-reverse_set_breakpoint_on_text`
- `list_network_requests` -> `js-reverse_list_network_requests`
- `get_request_initiator` -> `js-reverse_get_request_initiator`
- `get_websocket_messages` -> `js-reverse_get_websocket_messages`
- `take_screenshot` -> `js-reverse_take_screenshot`
- `new_page` -> `js-reverse_new_page`
- `navigate_page` -> `js-reverse_navigate_page`
- `select_page` -> `js-reverse_select_page`
- `select_frame` -> `js-reverse_select_frame`
- `pause/resume` -> `js-reverse_pause_or_resume`

If the tool-name prefix changes in the future, update this section first; do not improvise guesses at execution time.

### Positioning of jshookmcp

- Role: an enhanced execution surface for `js-reverse`, not an independent master control
- Suited for: browser automation, CDP debugging, JS hooks, network interception, SourceMap reconstruction, AST-assisted comprehension
- Precondition for invocation: first download `@jshookmcp/jshook` and register it into the MCP client configuration, then make sure that server is enabled
- Suggested entry: still follow `Observe → Capture → Rebuild`; just prefer jshookmcp's browser and hook capabilities during the `Observe/Capture` phases
- Relation to anything-analyzer: both can do browser/network-side forensics; anything-analyzer leans toward traffic capture and HTTP analysis, while jshookmcp leans toward the JS runtime, CDP, hooks, and source comprehension

## Core Principles

- `Observe-first`
- `Hook-preferred`
- `Breakpoint-last`
- `Rebuild-oriented`
- `Evidence-first`

Observe the page first, then sample minimally, then do local environment patching; do not skip forensics and guess the environment directly.

## Five-Phase Workflow

### 1. Observe

Goal: first confirm the target request, relevant scripts, and candidate functions; don't guess the environment.

Default actions:

- Open the target page with `js-reverse_new_page` or `js-reverse_navigate_page`
- Find the target request with `js-reverse_list_network_requests`
- Trace the call origin with `js-reverse_get_request_initiator`
- Narrow down scripts with `js-reverse_list_scripts` and `js-reverse_search_in_sources`

Required outputs:

- The target request URL or signature
- Initiator leads
- Suspicious script URLs
- An initial task record

### 2. Capture

Goal: minimally invasive sampling of the target request, obtaining parameter samples, call order, and runtime evidence.

Rules:

- Prefer `js-reverse_break_on_xhr`
- Prefer `js-reverse_evaluate_script` for lightweight runtime observation
- On a hit, first check `js-reverse_get_paused_info`
- Only then use `js-reverse_set_breakpoint_on_text` if necessary

### 3. Rebuild

Goal: organize the page evidence into locally iterable Node reproduction material.

Rules:

- Local environment patching must be grounded in page-observation evidence
- Imaginative patching of `window/document/navigator/crypto/storage` is not allowed
- Record only one minimal causal patch decision at a time

### 4. Patch

Goal: drive environment patching by errors and first divergence until the local script stably produces the target parameter.

Rules:

- See what's missing first, then patch it
- One minimal patch decision at a time
- Retest immediately after every patch
- Write every patch into the task record

### 5. DeepDive

Goal: after it runs locally, do deobfuscation, control-flow restoration, and business-logic refinement.

Rules:

- If the current task only needs the signature output, this phase can be downgraded
- If the algorithm chain will be reused long-term, this phase is mandatory
- Issue #65 obfuscation bypass (U–AV §4): JSVMP (AD) → `E-js-vmp`; CFF + string array (AE) → `E-js-deobf`; DevTools/debugger anti-debug (AF) → `E-js-anti-debug`. Full trigger table in `../reverse-engineering/references/nonpe-format-cookbook.md`; AST details still use `references/ast-deobfuscation.md`

## Execution Requirements

- Write every important step into a local task artifact
- If you cannot explain why a tool is being invoked, do not invoke it
- Prefer collecting evidence directly with the ready-made `js-reverse_*` or jshookmcp MCP capabilities; do not write scripts to reinvent capabilities first
- On failure, fall back per `references/fallbacks.md`
- Output follows `references/output-contract.md`

## Must-Read References

- Automation entry: `references/automation-entry.md`
- Parameter defaults: `references/tool-defaults.md`
- Task input template: `references/task-input-template.md`
- MCP-specific task orchestration: `references/mcp-task-template.md`
- Task artifacts: `references/task-artifacts.md`
- Local reproduction: `references/local-rebuild.md`
- Environment patching: `references/env-patching.md`
- Node reproduction: `references/node-env-rebuild.md`
- Instrumentation: `references/instrumentation.md`
- AST deobfuscation: `references/ast-deobfuscation.md`
- Non-PE/JS obfuscation recipes U–AV: `../reverse-engineering/references/nonpe-format-cookbook.md` (AD/AE/AF)
- Fallbacks: `references/fallbacks.md`
- Output contract: `references/output-contract.md`

---

## Routing Context

**Upstream entry points**: `skills/SKILL.md` (master control), `routing.md`
**Upstream alternatives**:
- The browser tools of the anything-analyzer MCP (port 23816) can serve as a substitute or supplement
- jshookmcp can serve as a stronger browser/CDP/Hook/Network/SourceMap/AST execution surface
- `reverse-engineering/SKILL.md` (if the target is not frontend JS)

**Downstream exits**:
- Environment patching needed → `references/env-patching.md`
- Local reproduction needed → `references/local-rebuild.md` / `references/node-env-rebuild.md`
- Deobfuscation needed → `references/ast-deobfuscation.md`
- When stuck, fall back → `references/fallbacks.md`

**Related sibling modules**: anything-analyzer MCP (browser automation and HTTP capture capabilities can complement each other)

---

## On-Demand Bootstrap

The MCP capabilities this skill depends on can be installed via the unified bootstrap system; MCP client registration must explicitly choose a target, and by default no client global configuration is written.

### Automation Capability Boundaries

| Capability | Auto-registerable | Method | Notes |
|------|-----------|------|------|
| jshookmcp | ✓ | npm-mcp (launched via npx) | Registered after explicitly choosing Claude / Codex / Both |
| anything-analyzer | ✓ | local-http-mcp | Service can be started automatically; client registration requires an explicit choice |
| Node.js | ✓ | winget install | Runtime dependency |

### Bootstrap Method

```powershell
# Install and register jshookmcp; Codex can be replaced with Claude or Both
powershell -File "<skill-root>\scripts\bootstrap-reverse.ps1" -Capability @('jshookmcp') -McpHostTarget Codex

# Register and start anything-analyzer
powershell -File "<skill-root>\scripts\bootstrap-reverse.ps1" -Capability @('anything-analyzer') -StartServices -McpHostTarget Codex
```

### Notes

- After registration, `jshookmcp` still must be **enabled** in the AI client before it can be invoked
- Without `-McpHostTarget`, capabilities are only installed/prepared and registration-required is returned; neither Claude nor Codex configuration is modified
- `anything-analyzer` needs pnpm and the project source; bootstrap clones and installs dependencies automatically
- If Node.js is not installed, bootstrap installs Node.js 22 via winget first

<br><br>## Task Completion Self-Check (MUST pass before claiming completion)

- [ ] Did I execute every step of the workflow (rather than just reading it)?
- [ ] Did I use real tool paths based on `tool-index`?
- [ ] Did I produce reproducible evidence (commands/scripts/screenshots/reports)?
- [ ] Did I complete and write back the Checklist items required by RULES?
