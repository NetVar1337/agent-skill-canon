---
name: router-reverse-skill-router-ida-reverse
description: |
  IDA Pro reverse engineering assistance skill. Whenever the user mentions reverse engineering, decompilation, analyzing binaries/PE/ELF/APK/DLL/SO, cracking, finding passwords, vulnerability analysis, virus analysis, or firmware analysis, or needs to analyze exe/dll/so/elf/macho/sys files, you MUST use this skill.

  Ensure to use this skill when the user wants to analyze any binary file, regardless of whether they explicitly mention "IDA" or "reverse engineering". This includes requests like "take a look at this exe", "analyze this dll", "help me crack this", "find the password", "how does this software register", etc.

  Use the bundled scripts (scripts/start.ps1, scripts/open.ps1) for deterministic server management and file opening — do NOT write ad-hoc PowerShell commands for these operations.
---

# IDA Pro Reverse Engineering Skill

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: Read `../field-journal/precedent-reverse.md` — confirm that this skill's operations are pre-authorized routine operations
2. `NOW`: Confirm whether the current task falls within this skill's scope
3. `NEXT`: Read `../tool-index.md`, verify tool availability and actual paths
4. `NEXT`: If tools are missing, invoke bootstrap — do not guess paths
5. `ACT`: Enter step one of the "Workflow" and execute; do not stop at a confirmation state

## Known Issues and Reflections (must read)

### Pitfalls We Have Hit

1. **Do not rely directly on `idb_open` (formerly `idalib_open`) via MCP calls from some AI clients**
   - Some coding AI clients' MCP clients have a BUG in output schema validation for open-type tools
   - Error: `Structured content does not match the tool's output schema`
   - **Solution**: use the `scripts/open.ps1` script to call the HTTP API directly, bypassing the MCP validation layer
   - Current ida-pro-mcp 2.x tool names are `idb_open` / `idb_list` / `idb_save` (no longer `idalib_*`)
   - After opening, a `session_id` (database) is returned; subsequent tool calls must carry that session

2. **No permission to open files under `C:\Windows\System32\`**
   - idalib cannot directly read files in the System32 directory
   - **Solution**: `open.ps1` auto-detects and copies them to a `temp directory` before opening

3. **Starting the server blocks the conversation**
   - After starting, `idalib-mcp` continuously outputs INFO logs to the console
   - **Solution**: use `scripts/start.ps1` (silent background start with `-WindowStyle Hidden`)
   - The script waits for the service to be ready then exits automatically, without blocking the conversation

4. **The MCP server name cannot contain a hyphen**
   - Previously `ida-pro-mcp` was used as the server name, which could cause tool registration issues
   - **Current configuration**: server name `idapro`, tool prefix `idapro_*`

5. **Remote HTTP vs Local Stdio**
   - `type:"local"` (stdio) mode: `idalib_open` has the same schema validation problem
   - `type:"remote"` (HTTP) mode: you can open the file first via script, then use MCP tools
   - **Current approach**: Remote HTTP mode

6. **PR #389 fixed part of the schema problem**
   - Author mrexodia merged a fix via PR #389 after issue #388
   - It fixed the structuredContent schema in HTTP mode, but validation on some coding AI client sides still has issues
   - The latest `main` branch version is installed

7. **idalib timeouts leave orphan worker processes holding lock files**
   - After the first `open.ps1` times out, idalib's python worker child process may become an orphan, clinging to `.id0`/`.id1`/`.nam`
   - Any subsequent tool or manual drag into the IDA GUI will report "insufficient permissions"
   - **Forbidden**: `taskkill /F /T` on the process tree — `/T` would also kill the GUI `ida.exe` child process
   - **Solution**: `start.ps1` replaces the managed supervisor only when nobody is listening on the port, or when `tools/list` returns quickly but lacks `py_eval` (old supervisor); an RPC timeout while 13337 is still listening counts as busy — do not kill
   - **Fallback**: when `open.ps1` detects the old database is locked, it automatically copies it to Temp with a GUID prefix

8. **Opening with auto-analysis can look like a hang**
   - `idalib_open(run_auto_analysis=true)` may not respond for a long time, but the backend is actually still opening and analyzing
   - Previously the user side saw "PowerShell with no output forever", easily misjudged as the script hanging
   - **Current solution**: `open.ps1` adds `-TimeoutSeconds`, and switches to background request + foreground polling + periodic progress output
   - When polling shows the session is ready, it returns `OK:filename:session_id` early; on timeout it returns `ERR:open_timeout_xxs`

9. **HTTP MCP silently exits after login**
   - Cursor/Claude's `type: http` does not spawn the process on your behalf; the old scheduled task ran only once at login
   - `pythonw` has no console; on crash the Application log is also empty
   - **Solution**: `start.ps1` reuses when healthy by default; `watchdog.ps1` inspects every minute; logs are in `%LOCALAPPDATA%\reverse-skill\ida-mcp\`
   - Install: `scripts/install-autostart.ps1`. If Cursor starts before the port is up, you still need to manually refresh once in the MCP panel

### Workflow Principles

| Step | What to do | What to use |
|------|--------|--------|
| 1 | Ensure the HTTP server is running | `scripts/start.ps1` (no arguments) |
| 2 | Open the target binary | `scripts/open.ps1 -Path "xxx.exe"` |
| 3 | Use MCP analysis tools | Call `idapro_*` / HTTP tools directly (about 65, depending on version) |
| 4 | Analysis done | Tools are automatically available |

## Script Resources

### start.ps1 — Start the MCP HTTP Server

Path: `scripts/start.ps1`

- Automatically resolves `IDADIR` (environment variable / portable desktop path / common install paths)
- Prefers IDA's bundled `Python314\python.exe -m ida_pro_mcp.idalib_supervisor`
- By default probes `http://127.0.0.1:13337/mcp` first; if healthy, outputs `OK:<n>:reuse` and exits
- 13337 listening but `tools/list` times out → `WARN:busy` / `OK:busy:reuse`, **do not kill** (the supervisor is single-threaded and cannot respond while opening a database)
- Replaces the managed supervisor only when nobody is listening on the port, or when it returns quickly but lacks `py_eval`; **never kills `ida.exe`, never uses `taskkill /T`**
- When the GUI occupies 13337, outputs `WARN:gui_busy` and exits without starting another supervisor
- On success outputs `OK:<tool count>` (currently about 66); on failure outputs `ERR:timeout`
- Supervisor log: `%LOCALAPPDATA%\reverse-skill\ida-mcp\supervisor.log`
- The server runs in the background and does not block the conversation

**Invocation**:
```
powershell -File "<skill-root>\ida-reverse\scripts\start.ps1"
```

### watchdog.ps1 / install-autostart.ps1 — Keep-Alive

- `watchdog.ps1`: probes 13337; if healthy, `OK:<n>:reuse`; only calls `start.ps1` if it is down
- `install-autostart.ps1`: registers the scheduled task `reverse-skill-ida-mcp` (at login + every minute)
- Log: `%LOCALAPPDATA%\reverse-skill\ida-mcp\watchdog.log`

### open.ps1 — Open a Binary File

Path: `scripts/open.ps1`

- Calls `idb_open` directly via the HTTP API, bypassing MCP schema validation
- Auto-detects System32 paths and copies to a temp directory
- Automatically cleans up old database files with the same name (`.id0`/`.id1`/`.nam`/`.til`/`.i64`)
- When the old database is locked, automatically degrades: copies to Temp with a GUID prefix and opens, without erroring
- Runs the open request in the background, avoiding the script becoming unresponsive during a long synchronous wait
- Supports `-TimeoutSeconds`; on timeout returns `ERR:open_timeout_xxs`, never hangs forever
- Outputs `INFO:opening:elapsed/timeout seconds` every 10 seconds, to help tell that analysis is still running
- On success outputs `OK:filename:session_id`; on degradation adds the `(temp copy)` marker
- On failure automatically retries with the Temp copy

**Invocation**:
```
powershell -File "<skill-root>\ida-reverse\scripts\open.ps1" -Path "C:\path\to\file.exe"
```

**Optional parameters**:
```
# Specify a SessionId
powershell -File "scripts\open.ps1" -Path "file.exe" -SessionId "my_session"

# Skip auto-analysis (recommended for large files)
powershell -File "scripts\open.ps1" -Path "large.exe" -NoAutoAnalysis

# Set a timeout to avoid long no-response periods with auto-analysis enabled
powershell -File "scripts\open.ps1" -Path "file.exe" -TimeoutSeconds 600
```

**Output conventions**:
```
# Analysis in progress (output every 10 seconds)
INFO:opening:11/600s

# Opened successfully
OK:sample.exe:abcd1234

# Opened successfully, but degraded to a Temp copy due to locked files
OK:1234abcd-sample.exe:abcd1234 (temp copy)

# Timeout limit reached
ERR:open_timeout_600s
```

**Field-tested notes**:
- `Snipaste.exe` with auto-analysis took about `324s` in real testing to return success — that is "analyzing for a long time", not "the script deadlocked"
- So for GUI programs or more complex samples, prefer explicitly setting `-TimeoutSeconds 600`

## Core Tool List

### Overview Analysis (first step)
- `idapro_survey_binary(detail_level="minimal")` — quick summary: function count, strings, segments, entry point, import classification (crypto/network/file IO)
- `idapro_list_funcs(queries)` — list functions (paginated, filter by name)
- `idapro_list_globals(queries)` — list global variables
- `idapro_entity_query(kind, filter)` — unified query: functions/globals/imports/strings/names

### Decompilation and Disassembly
- `idapro_decompile(addr)` — decompile to pseudocode
- `idapro_disasm(addr, max_instructions=N)` — disassemble
- `idapro_analyze_function(addr, include_asm=false)` — comprehensive analysis (pseudocode+strings+constants+callers+callees+blocks)
- `idapro_func_profile(queries)` — function profile metrics

### Cross References and Data Flow
- `idapro_xrefs_to(addrs)` — find who references the target addresses
- `idapro_xref_query(addr, direction)` — advanced xref query (direction/type filtering)
- `idapro_callees(addrs)` — callee list
- `idapro_callgraph(roots, max_depth)` — call graph
- `idapro_trace_data_flow(addr, direction, max_depth)` — data flow tracing (forward/backward)

### Search
- `idapro_find_regex(pattern, limit)` — regex string search
- `idapro_search_text(pattern)` — search text in the disassembly listing
- `idapro_find_bytes(patterns, limit)` — byte pattern search (supports ?? wildcards)
- `idapro_find(type, targets)` — advanced search (immediates/strings/references)

### Memory and Data
- `idapro_get_bytes(addrs)` — read raw bytes
- `idapro_get_string(addrs)` — read strings
- `idapro_get_int(queries)` — read integer values
- `idapro_get_global_value(queries)` — read global variable values
- `idapro_read_struct(queries)` — read structure field values
- `idapro_search_structs(filter)` — search structures

### Modification Operations
- `idapro_set_comments(items)` — add comments (two-way sync between disassembly and decompilation)
- `idapro_append_comments(items)` — append comments
- `idapro_rename(batch)` — batch rename (functions/globals/locals/stack variables)
- `idapro_patch_asm(items)` — patch assembly instructions
- `idapro_patch(patches)` — patch bytes
- `idapro_define_func(items)` — define functions
- `idapro_undefine(items)` — undefine
- `idapro_define_code(items)` — convert bytes to code

### Type System
- `idapro_declare_type(decls)` — declare C structures/enums/unions
- `idapro_set_type(edits)` — apply types to functions/globals/locals
- `idapro_infer_types(addrs)` — infer types
- `idapro_type_query(queries)` — query declared types
- `idapro_type_inspect(queries)` — inspect type details

### Stack Frames
- `idapro_stack_frame(addrs)` — view stack frame variables
- `idapro_declare_stack(items)` — declare stack variables
- `idapro_delete_stack(items)` — delete stack variables

### Signatures
- `idapro_make_signature(addrs)` — generate a unique byte signature for an address
- `idapro_make_signature_for_function(addrs)` — generate a signature for a function
- `idapro_find_xref_signatures(addrs)` — generate signatures for code referencing an address

### Debugger (requires ?ext=dbg)
- `idapro_open_file(file_path)` — open a file in the GUI IDA instance
- Debugger tools are hidden by default and can be enabled via the URL parameter `?ext=dbg`

### Session Management (ida-pro-mcp 2.x)
- `idapro_idb_open` / HTTP `idb_open` — ⚠️ prefer opening with `open.ps1`
- `idapro_idb_list` / HTTP `idb_list` — list all sessions
- `idapro_idb_save` / HTTP `idb_save` — save the database
- Most analysis tools need the `database=<session_id>` parameter (the session output by open.ps1)

### Others
- `idapro_int_convert(inputs)` — base conversion (**always use this; never convert bases yourself!**)
- `idapro_export_funcs(addrs, format)` — export functions (json/c_header/prototypes)
- `idapro_py_eval(code)` — execute Python in the IDA context
- `idapro_server_health()` — server health check
- `idapro_server_warmup()` — warm up subsystems (string cache, Hex-Rays, etc.)

## Complete Reverse Engineering Workflow

### Step 1: Start the Server

**Path A — Headless idalib (requires a valid license)**
```
powershell -File "scripts/start.ps1"
```
Output `OK:<tool count>` (currently about 65) means ready.

**Path B — GUI + plugin (when the idalib license fails or interactive analysis is needed)**
```
powershell -File "scripts/start-gui.ps1" -Path "C:\target.exe"
```
Or double-click the portable `Launch-IDA-Pro.cmd` and open the sample in IDA.

After confirming the Output window shows `[MCP] ... port=13337`, the MCP tools are available.

For general integration steps see `LOCAL-SETUP.md`.

### Step 2: Open the File

Headless:
```
powershell -File "scripts/open.ps1" -Path "C:\target.exe" -TimeoutSeconds 600
```
Output `OK:filename:session_id` means success (a trailing `(temp copy)` means automatic degradation to a temp copy).

If `ERR:idalib_license:...` appears, switch to Path B (GUI mode); do not repeatedly retry open.ps1.

GUI mode: just Open the sample directly in IDA; open.ps1 is not needed.

### Step 3: Global Overview (including the import table hard gate)
```
idapro_survey_binary(detail_level="minimal")
```
Watch for:
- Architecture (x86/x64/ARM)
- Entry point (main/WinMain/DllMain)
- Interesting strings (URLs, paths, error messages)
- **Import classification (MUST)**: crypto functions / network APIs / file operations / process injection / registry — must be recorded as Evidence (suggested id: `E-imports`); use `idapro_entity_query(kind="imports")` or the imports section of the survey output
- **DLL/SYS**: export table alongside the import table (Evidence `E-exports`)
- **.NET**: with no traditional IAT, use a module/metadata/managed-reference summary as the equivalent anchor written into the E-imports semantic slot
- **Clean import table**: note the dynamic-loading suspicion and push for dynamic API breakpoint verification
- Hot functions (functions with high xref counts are usually key logic)

**Hard gate**: before writing the imports view/classification summary (or a legitimate equivalent anchor) into Evidence, you MUST NOT enter Step 4 deep-digging conclusions, and MUST NOT claim the survey is complete. If the import table is empty or the query fails, you still MUST record the failure symptoms. When packed IAT repair fails, you MUST record `E-iat-repair-fail` and switch to dynamic debugging to capture APIs; grinding statically is forbidden. When the user requests a redo of the import table/IAT check, you MUST redo the named step (if blocked, use the feasibility gate: explain + confirm; if forced, mark quality=unreadable); swapping in unrelated steps is forbidden.

### Step 4: Dig Into Key Functions
```
idapro_analyze_function(addr="key function name")
```
Or:
```
idapro_decompile(addr="function name")
idapro_disasm(addr="function name", max_instructions=50)
```

### Step 5: Data Flow and Cross References
```
idapro_xrefs_to(addrs="key address/string")
idapro_callgraph(roots=["key function"], max_depth=3)
idapro_trace_data_flow(addr="key address", direction="backward", max_depth=5)
```

### Step 6: Record and Refine
```
idapro_set_comments(items=[{"addr": "0x140001000", "comment": "your understanding"}])
idapro_rename(batch={"func": [{"addr": "function address", "name": "meaningful name"}]})
```

### Step 7: Output the Report
After analysis, generate `report.md` recording findings and steps.

## Prompt Engineering Guidelines

1. **Never convert bases manually** — whenever a number must be converted, use `idapro_int_convert`
2. **Survey first, then dig deep** — look at the overview first, then analyze in a targeted way
3. **Keep adding comments and renaming** — continuously update function and variable names during analysis to improve later accuracy
4. **Follow cross references** — when you find interesting data/strings, use `xrefs_to` to see who references them
5. **On obfuscated code** — first do preprocessing such as string decryption, import hash removal, and control flow flattening removal
6. **C++ STL code** — identify library functions with FLIRT/Lumina first, then analyze business logic
7. **Do not brute force** — analysis should derive the solution from the disassembly, with simple Python for auxiliary computation
8. **On "No database bound"** — no binary has been opened yet; run `open.ps1` first
9. **On "Failed to open database"** — old database files may be locked; `open.ps1` automatically degrades to a Temp copy (output contains the `(temp copy)` marker)
10. **Opening GUI/complex samples with auto-analysis** — add `-TimeoutSeconds 600` by default; do not misjudge a long `INFO:opening:...` as the script hanging

---

## Routing Context

**Upstream entries**: `skills/SKILL.md` (master control), `routing.md`
**Upstream alternatives**: `radare2/` (if you do not want to launch IDA, do a quick r2 reconnaissance first)
**Downstream exits**:
- Frida dynamic verification needed → `reverse-engineering/tools-dynamic.md`
- Symbolic execution/angr needed → `reverse-engineering/tools-dynamic.md`
- General reverse engineering methodology → `reverse-engineering/SKILL.md`

**Peer related modules**: `radare2/` (fallback when IDA is unavailable)

---

## On-Demand Bootstrap

This skill's entry scripts are wired into the unified bootstrap system.

### Automation Capability Boundaries

| Tool | Auto-installable | Install method | Notes |
|------|-----------|---------|------|
| idalib-mcp | ✓ | pip install (from GitHub) | Auto-installed by `start.ps1` when missing |
| IDA Pro itself | ✗ | Commercial software, manual install required | Set the `IDADIR` environment variable to the install directory |

### Installation Steps (verified)

```cmd
# 1. Set the IDA path (replace with your actual IDA install directory)
setx IDADIR "<your IDA install directory>"

# 2. Install ida-pro-mcp from GitHub (the ida-mcp on PyPI is a different project — do not install the wrong one!)
pip install git+https://github.com/mrexodia/ida-pro-mcp.git

# 3. Install the IDA plugin (choose Streamable HTTP + Global + select all clients)
ida-pro-mcp --install

# 4. Restart IDA Pro and open the target file
# The plugin automatically listens on 127.0.0.1:13337

# 5. Verify
ida-pro-mcp --config
```

> ⚠️ **Note**: the `ida-mcp` package on PyPI (author jtsylve) is a different project, not the one we need.
> You must install `mrexodia/ida-pro-mcp` from GitHub.

### Bootstrap Trigger Points

- `scripts/start.ps1`: automatically calls `bootstrap-reverse.ps1` when `idalib-mcp` is missing
- MCP registration: bootstrap automatically writes `idapro` into the Claude MCP configuration

### Prerequisites

- IDA Pro installed and the `IDADIR` environment variable set (or the default path inside the script is correct)
- Prefer the `ida-pro-mcp` from IDA's bundled Python314 (already built into the portable version)
- Common local configuration:
  - User env `IDADIR` → IDA install directory (containing `ida.exe`)
  - Optional `~\Tools\bin\idalib-mcp.cmd` / `ida-pro-mcp.cmd` wrappers
  - The client MCP server name kept as only `idapro` → `http://127.0.0.1:13337/mcp`


## Task Completion Self-Check (MUST pass before claiming completion)

- [ ] Did I execute every step in the workflow (not just read it)?
- [ ] Was survey/imports written to Evidence (E-imports or equivalent)? Do DLL/SYS include E-exports? Was E-iat-repair-fail recorded on IAT failure?
- [ ] If the user requested a redo of the import table/IAT, did I redo the same step?
- [ ] Did I use real tool paths based on `tool-index`?
- [ ] Did I produce reproducible evidence (commands/scripts/screenshots/reports)?
- [ ] Did I complete and write back the Checklist items required by RULES?
