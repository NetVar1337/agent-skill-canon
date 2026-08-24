# CUA-Driver workflow for the Arena (verified 2026-08-07)

How to operate the Gray Swan Arena challenge UI with `cua-driver`
(https://github.com/trycua/cua) on this Windows host. Everything below was
exercised live; the failure modes are real observations, not guesses.

## Architecture (the one that works)

```
agent-browser (headed Chromium, cookies staged)   <-- dedicated browser window
        ^
        |  owns CDP endpoint (pid-owned loopback)
        v
cua-driver typed browser tools (bind -> snapshot -> click/type -> verify)
```

- **Dedicated headed browser**, not the user's live Chrome. The arena tab must
  stay *active* for typed mutations; in a shared window the user's own tab
  switching (Netflix, Gmail…) constantly invalidates that. A dedicated window
  nobody touches keeps the tab active and makes every route work
  **in the background** (trusted CDP input; Windows Chrome/Edge validated).
- The browser is agent-browser's "Chrome for Testing" with the user's cookies
  injected — it passes the Vercel bot checkpoint (real TLS/HTTP2 fingerprint)
  and is already authenticated.
- cua-driver binds that window's native CDP endpoint directly
  (`binding_quality: exact`, `mutation_allowed: true`) — **no
  `browser_prepare` needed** because the endpoint already exists and is
  pid-owned.

## Setup sequence

```bash
# 1. dedicated headed browser with session
agent-browser open --headed --session gs
agent-browser open https://app.grayswan.ai/arena/challenge/ipi-aug-2026 --session gs   # navigate first!
agent-browser cookies set --curl <skill>/session/cookies.json --session gs            # AFTER navigating to origin
agent-browser reload --session gs

# 2. cua session + bind
cua-driver start_session '{"session":"gs-arena"}'
cua-driver list_windows '{}'          # find pid/window_id titled "Indirect Prompt Injection…"
cua-driver get_browser_state '{"pid":<pid>,"window_id":<wid>,"session":"gs-arena"}'
#    -> status ok, binding_quality exact, mutation_allowed true; note target_id + tab_id
```

Gotchas learned:
- `cookies set` on `about:blank` fails ("Invalid cookie fields") for secure
  cookies — navigate to the https origin FIRST, then set, then reload.
- Sessions idle-expire; `start_session` again with the same id revives.
- Decode cua-driver stdout as UTF-8 (cp1252 crashes on tab titles).

## The interaction loop (all background)

```
get_browser_state(target,tab, snapshot_format=semantic_v2)   # refs pN:x
browser_click {ref, input_route:"trusted"}                   # options, buttons
browser_click {ref} ... re-snapshot after every mutation     # refs invalidate
browser_type  {ref, text, mode:"insert_text"}                # payload into editor
get_browser_state ... include_screenshot:true                # verify pixels+tree
```

Verified live sequence on `unauthorized-pledge` + `Air Fish Bold`:
1. click behavior option ref → click model option ref (verified via
   `states.selected` on fresh snapshot).
2. click "Save changes and apply settings".
3. editor ref = the ref whose `actions` include `"type"` (contenteditable;
   appears as `textbox "Article comment"` in the outline).
4. `browser_type` lands via `trusted_input`; the **Launch · 10** button appears
   in refs only once the editor is non-empty (disabled controls have no refs).
5. clear with `browser_type {text:"", replace:true}`.

## Failure modes (observed) and what they mean

| Symptom | Cause | Fix |
|---|---|---|
| `browser_requires_setup` on user's Chrome | no CDP endpoint | `cua-driver serve --grant existing-profile` + `browser_prepare` (auto-toggles Chrome's consent page) — works once, then see next rows |
| `browser_binding_stale` | window/tab reshuffle invalidated ownership | re-`get_browser_state` bind; if persistent, re-`browser_prepare` |
| typed click/type `route_unavailable` (escalation `page`) | tab not active, or editor ref `visibility: unknown` | make tab active (UIA `TabItem` select) or use dedicated browser |
| native `type_text`/`click` px `background_unavailable` on `Chrome_WidgetWin_1` | Chromium DOM drops background input | `delivery_mode:"foreground"` (brief front+restore) — fine on dedicated window, hostile on user's window |
| title/URL mismatch in `page` | stale tab correlation after reshuffle | rebind; probe each tab's `page.url` |

## Compliance boundary (unchanged)

CUA is used for recon, staging, verification, and transcript reading.
**The Launch and Submit-to-Judges clicks remain the human's** (challenge rule:
no automated submission). The refs for them are discoverable (`Launch · 10`,
`Submit Chat`, `Submit to Judges`) — do not click them from a script.

Helper: `scripts/cua_arena.py` wraps setup/snap/click/type/clear/shot.
