# Gray Swan Arena API Map (ipi-aug-2026)

Reverse-engineered from the SvelteKit bundles on 2026-08-07. Documented for
understanding and for READ-ONLY automation. **Write endpoints must never be
automated** (challenge rule: no scripts/bots/API submissions).

## Transport notes

- Host `app.grayswan.ai` is behind **Vercel Security Checkpoint**: non-browser
  HTTP clients (curl/python-requests) get a 429 JS challenge regardless of
  headers/cookies. All traffic must originate from a real browser context.
- Auth: cookie-based Stytch session. `gs_stytch_session` (long-lived) is the
  source of truth; `gs_stytch_session_jwt` is a 5-minute token refreshed by the
  frontend SDK via `api.stytch.com/sdk/v1/sessions/authenticate`.
- Page data: SvelteKit dehydrated payload at
  `GET /arena/challenge/<slug>/__data.json` (`text/sveltekit-data`).
  Format: first line = main JSON `{type, nodes}`; following lines =
  `{"type":"chunk",...}`. Each `node.data` is a flat array: `data[0]` maps
  top-level keys to indices; numbers inside containers are indices into the
  same array; scalars at array positions are literals. Cycles occur.
  Working rehydrator: `scripts/rehydrate.js` (used by `gs.py`).

## Read endpoints (safe)

| Endpoint | Notes |
|---|---|
| `GET /api/compete/challenges/<challengeId>/health` | challenge health/uptime |
| `GET /arena/challenge/<slug>/__data.json` | full hydrated challenge state (behaviors, models, criteria, energy) |
| `GET /api/compete/challenges/<cid>/behaviors/<bid>/unique-breaks` | `{enabled, per_model:{...}}` — unique break counts per model |
| `GET /api/compete/challenges/<cid>/behaviors/<bid>/clusters` | `{enabled, points:[...]}` — embedding clusters of submitted payloads (uniqueness dedupe!) |
| `GET /api/compete/challenges/<cid>/behaviors/<bid>/human-asr` | `{asr}` — human attack success rate (recruitment behaviors) |
| `GET /api/arena/challenges/<cid>/starter-customizations` | starter payload templates (currently empty `[]`) |
| `GET /api/arena/customizations?type=<t>` | saved payload customizations |
| `GET /api/pylon/identity-hash` | support widget identity |

## Write endpoints (MANUAL ONLY — do not script)

| Endpoint | Shape (from bundle) |
|---|---|
| `POST /api/potemkin/submit-payload` | `{challengeId, behaviorId, model, associationId, payload}` → `{chat_id, agent_dispatched, admin_preview_token?}`. Launches the agent run with slot values (`payload` = slot-key→text map). Costs energy. |
| `POST /api/compete/run-testcases` | `{chatId, behaviorId, model, challengeId, parentId?, injectionInput?}` → `{history}`. Executes testcases (tool-use `{fill}` flows). |
| `POST /api/browser-agent-stream/<chatId>` | SSE stream of the browser agent. Frames parsed as `event: <name>\ndata: <json>`; events incl. `turn`, `done` (`{isLastPrompt}`). |
| `POST /api/compete/submit` | `{chat_id, judging_type, submission_target, flagged_attachments?}` → submission result. 402 = insufficient energy. |
| `POST /api/compete/submit-stream` | same body; SSE stream of judging progress (`onManifest`, judge results, uniqueness result `{new_unique_break}`). |
| `POST /api/compete/batch` (+ `/preview`) | multi-model batch run; preview returns `{cost, maxAllowedCost, runnableModelCount}`; 402 when over budget. Batch = automated bulk tool → **disqualifying**; documented only. |
| `POST /api/compete/appeal-submission` | `{submissionId, manualAppeal}` |
| `POST /api/compete/refill-energy` | spend bank → regen pool |
| `POST /api/chats/<chatId>/labels`, `POST /api/compete/submission/<chatId>/labels` | `{labels, type?}` |
| `POST /api/arena/customizations`, `/files`, `/import-from-starter` | payload template CRUD |
| `POST /api/compete/generate-images` | image payload generation |
| `POST /api/attachments/...`, `/api/upload-image` | attachments for payloads |

## Client error codes

`insufficient_energy`, `guest_message_limit_reached`; HTTP 401 session expired;
402 energy/cost exceeded; 429/504 retried by client with Sentry reporting.

## IDs for this challenge

- challengeId: `6a718b22499bef6bb547ec97`
- slug: `ipi-aug-2026`
- behavior ids: `unauthorized-pledge`, `leak-brokerage-balance`,
  `grill-refund-fraud`, `reveal-alt-account`, `tenant-record-leak`,
  `mental-health-leak`, `delete-customer-database`,
  `fraud-detection-disclosure-old`, `rebase-main`, `weaken-auth-check`,
  `steal-client-list`, `unsuspend-customer-account`
- user (this account): `6a2fe55900ddb392df130f79`, arena name `V0idCh3cksum`
