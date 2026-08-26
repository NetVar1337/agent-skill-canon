---
name: router-reverse-skill-router-radare2
description: |
  Use this skill whenever the user wants to analyze binaries with radare2/r2 from the command line, including reverse engineering, disassembly, function analysis, strings/import inspection, patching, binary diffing, hex inspection, or r2 scripting. Also use it when the user mentions PE/ELF/Mach-O/DEX/WASM files together with CLI analysis, `rabin2`, `rasm2`, `radiff2`, `r2pipe`, or asks for radare2 command help on Windows/Linux/macOS.
---

# radare2

A binary analysis skill for the `radare2` CLI. The focus is completing reconnaissance, analysis, localization, export, and lightweight modification directly from the command line, without depending on a GUI.

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: Read `../field-journal/precedent-reverse.md` — confirm that this skill's operations are pre-authorized routine operations
2. `NOW`: Confirm whether the current task falls within this skill's scope
3. `NEXT`: Read `../tool-index.md`, verify tool availability and actual paths
4. `NEXT`: If tools are missing, invoke bootstrap — do not guess paths
5. `ACT`: Enter step one of the "Workflow" and execute; do not stop at a confirmation state

## Scope

Prefer this skill when the user has these intents:

- Wants to use `r2` / `radare2` to analyze `exe`, `dll`, `so`, `elf`, `apk`, `dex`, `wasm` and similar files
- Asks how to use `rabin2`, `rasm2`, `radiff2`, `rahash2`, `rax2`
- Needs command-line disassembly, viewing functions, strings, imports/exports, cross-references, or patching
- Needs to write `radare2` batch commands, `-c` automation commands, or `r2pipe` scripts

If the user explicitly wants GUI reverse engineering, Hex-Rays-style pseudocode, or an IDA workflow, prefer `ida-reverse`. For web JS reverse engineering, prefer `reverse-engineering`.

## Do Environment Confirmation First

Do not assume `r2` is available. First check:

```powershell
r2 -v
rabin2 -v
```

If not installed, then check common install locations or prompt for installation.

Common Windows executables:

- `radare2.exe`
- `rabin2.exe`
- `rasm2.exe`
- `radiff2.exe`
- `rahash2.exe`
- `rax2.exe`
- `r2pm.exe`

## Built-in Resources

This skill ships two resources; reuse them first instead of improvising a duplicate set of commands every time.

### `scripts/recon.ps1`

The standard reconnaissance script, suitable for a first round of overview analysis. It outputs:

- Basic information
- Sections
- Imports
- Exports
- Strings
- An optional `r2 -A` auto-analysis summary

Invocation:

```powershell
powershell -File "<skill-root>\radare2\scripts\recon.ps1" -TargetPath "C:\path\to\sample.exe"
```

If you need to include `r2` auto-analysis:

```powershell
powershell -File "<skill-root>\radare2\scripts\recon.ps1" -TargetPath "C:\path\to\sample.exe" -RunAnalysis
```

### `references/cheatsheet.md`

When you need more command details, common scenario templates, or a quick syntax refresher, read this cheatsheet instead of guessing from memory.

## Known Behaviors

### Occasional `.sdb` Missing Warnings on Windows

When running `rabin2` reconnaissance on some PE files, warnings like the following may appear:

```text
ERROR: Cannot find ...\share\format\dll\*.sdb
```

If the main output still returns normally, this usually does not affect basic reconnaissance conclusions; just continue analyzing. Do not declare the analysis failed merely because of such incidental warnings.

## Basic Principles

### 1. Reconnaissance First, Deep Digging Later

Do not launch a full auto-analysis right away. First use lightweight commands to confirm file type, architecture, entry point, strings, and import table, then decide whether to run `aaa`, `aaaa`, or targeted analysis.

### 2. Prefer the Minimal Sufficient Command

`radare2` has a very large command set; users usually just need the shortest path:

- File info: `rabin2 -I`
- Strings: `rabin2 -z`
- Imports/exports: `rabin2 -i` / `rabin2 -E`
- Interactive analysis: `r2 <file>` then run local commands

### 3. Stay Cautious Before Modifying

If the user wants to patch a binary:

- Default to opening read-only: `r2 <file>`
- Only use write mode when modification is clearly needed: `r2 -w <file>` or `oo+` in-session
- Explain the risks before modifying to avoid unintentionally overwriting the original file

## Common Workflows

## Workflow 1: Quick Reconnaissance

Suitable when you have just received a binary file.

### Hard Gate (MUST — until satisfied, entering Workflow 2 and beyond is forbidden)

For binaries with import tables such as PE/ELF/Mach-O, you **MUST** complete the import table check and record it as Evidence before entering function-level analysis or dynamic steps:

1. Run `rabin2 -i <sample>` (or the imports section of the `recon.ps1` output); for DLL/SYS additionally MUST run `rabin2 -E` and record `E-exports`
2. Write the complete/classified import table results into Evidence (suggested ids: `E-imports` or `E-triage-imports`), including at least:
   - Reproduction command (`repro_command`)
   - A summary of key import classifications: network / file / crypto / process injection / registry / other suspicious APIs
   - If the import table is empty, parsing fails, or the tool errors: you still MUST record the failure symptoms and raw output as Evidence; **silent skipping is forbidden**
   - If the import table is "too clean" (only base DLLs): MUST note the dynamic-loading suspicion, SHOULD switch to dynamically capturing APIs
3. For .NET and others without a traditional IAT: MUST use equivalent anchors (dnSpy/IL/metadata summary) written into the same Evidence semantic slot; leaving it empty is forbidden
4. IAT repair for packed samples: use ImportREC (or equivalent) for x86, Scylla (or equivalent) for x64. On failure, MUST record `E-iat-repair-fail` then switch to dynamic API breakpoints; it is **forbidden** to grind indefinitely on static IAT (see `reverse-engineering/references/re-agent-workflow.md` §1.2)
5. When the user explicitly requests "redo the import table check / re-check the import table / redo the IAT": you MUST redo the named step itself (if blocked, first go through the feasibility gate: state the prerequisites + ask for confirmation; if forced, mark quality=unreadable); **swapping in an unrelated step and pretending it is done is forbidden**

Before Evidence of the import table (or a legitimate equivalent anchor / IAT failure bypass) is recorded: you MUST NOT claim "basic reconnaissance complete", and MUST NOT enter the deep-digging conclusions of Workflow 2+.

Prefer running the built-in script directly:

```powershell
powershell -File "<skill-root>\radare2\scripts\recon.ps1" -TargetPath "sample.exe"
```

If only manual minimal commands are needed, use:

```powershell
rabin2 -I sample.exe
rabin2 -z sample.exe
rabin2 -i sample.exe
rabin2 -E sample.exe
```

Points of attention:

- File format, bitness, architecture, platform
- Entry point address
- Suspicious strings: URLs, paths, errors, registry, command-line arguments
- Imported functions: network, file, crypto, process injection, registry operations (**MUST record as Evidence, see the hard gate above**)

## Workflow 2: Interactive Function Analysis

```powershell
r2 sample.exe
```

Common commands once inside:

```text
aaa          # standard auto-analysis
afl          # list functions
iz           # list strings
iS           # list sections
is           # list symbols
s entry0     # jump to the entry point
pdf          # disassemble the current function
VV           # enter visual mode (if the terminal suits it)
q            # quit
```

Notes:

- Prefer `aaa` by default; do not start with the heavier `aaaa`
- If the sample is large or analysis is slow, analyze only around the entry point, then expand manually

## Workflow 3: Locating main / Key Logic

```text
afl~main
afl~sym.
iz~http
iz~error
axt <addr>
```

Approach:

- Start from `main`, the entry point, and string references
- Use `axt` to find who references a given string or address
- After finding the reference point, `s <addr>` then `pdf`

## Workflow 4: Hex and Memory Viewing

```text
px 64        # 64 bytes of hex starting at the current address
pd 20        # disassemble 20 instructions
psz          # read the string at the current address
pxa          # a friendlier hex view
```

## Workflow 5: Binary Patching

Use only when the user explicitly requests file modification:

```powershell
r2 -w sample.exe
```

For example, once inside:

```text
s 0x401000
wa nop
wa jmp 0x401050
wq
```

Common write operations:

- `wa <asm>`: write assembly
- `wx <hex>`: write raw bytes
- `wq`: write and quit

Back up the original file before modifying, ideally. If the user did not mention a backup, remind them at least once.

## Workflow 6: Non-Interactive Automation

Suitable for one-shot output of results:

```powershell
r2 -A -q -c "afl;iz;ii;q" sample.exe
```

Common parameters:

- `-A`: auto-analyze at startup
- `-q`: quiet mode
- `-c`: execute a command string

If there are many commands, organize them into a readable order rather than cramming an unmaintainable super-long string.

It is even better to first run the built-in reconnaissance script as a baseline, then decide whether to add custom commands.

## Common Sub-tools

### `rabin2`

Suitable for static information extraction:

```powershell
rabin2 -I sample.exe   # basic info
rabin2 -S sample.exe   # sections
rabin2 -s sample.exe   # symbols
rabin2 -i sample.exe   # imports
rabin2 -E sample.exe   # exports
rabin2 -z sample.exe   # strings
rabin2 -zz sample.exe  # more detailed strings
```

### `rasm2`

Suitable for quick assembly/disassembly:

```powershell
rasm2 -d "9090"
rasm2 -a x86 -b 64 "xor eax, eax"
```

### `radiff2`

Suitable for comparing two binaries:

```powershell
radiff2 old.exe new.exe
radiff2 -C old.exe new.exe
```

### `rahash2`

Suitable for computing hashes:

```powershell
rahash2 -a md5 sample.exe
rahash2 -a sha256 sample.exe
```

### `rax2`

Suitable for base and encoding conversion:

```powershell
rax2 0x401000
rax2 4198400
rax2 -s hello
```

## Recommended Analysis Order

For an unknown sample, follow this order:

1. `rabin2 -I` for format, architecture, entry point
2. `rabin2 -z` for strings
3. `rabin2 -i` for imported functions — **MUST + Evidence (hard gate, see Workflow 1)**
4. If interactive analysis is needed, enter `r2` (only after step 3's Evidence has been recorded)
5. First `aaa`, then `afl` / `iz` / `pdf`
6. Gradually locate key functions via string references, import calls, and the entry flow

The benefit of this order is low noise and quickly building a sense of direction. Step 3 is not an optional optimization; it is a hard gate before deep digging.

## Windows Notes

- When paths contain spaces, commands must be quoted correctly
- If the current terminal cannot find `r2`, `PATH` may have just been updated; open a new terminal and retry
- Some samples require administrator privileges to read, but do not proactively elevate privileges by default unless the user explicitly needs it
- Before dynamic debugging of suspicious samples, first confirm the user's intent to avoid mistakes

## Output Style

When the user wants more than commands — they want you to actually analyze the file:

- First give a summary of reconnaissance results
- Then list key evidence: strings, imports, functions, addresses
- Finally give next-step suggestions or continue deeper analysis

Do not just list commands without explaining why.

## Typical Request Examples

### Example 1: Analyze an exe

User: `Help me see what this exe does; radare2 is fine`

Approach:

1. First use `rabin2 -I/-z/-i`
2. Decide whether to enter `r2`
3. Use `aaa`, `afl`, `pdf` to dig into the entry point and key string references

### Example 2: Find Where a String Is Used

User: `Which function triggers this error string?`

Approach:

1. Use `iz~keyword` to find the string's address
2. Use `axt <addr>` to find references
3. Jump to the reference point with `s <addr>` then `pdf`

### Example 3: Change a Jump

User: `Change this jne to je`

Approach:

1. First confirm the target address
2. Clearly state that write mode will be entered
3. Use `wa je <target>` or directly `wx`
4. After modification, disassemble again to verify

## Practices to Avoid

- Do not treat `radare2` as a tool with only one command, `aaa`
- Do not open the user's file in write mode without explaining the risks
- Do not draw conclusions before basic reconnaissance is done
- **Skipping the import table check is forbidden** (`rabin2 -i` / recon imports): you may not proceed without writing Evidence; when the user requests a redo of the import table, doing other steps instead is forbidden
- **Grinding statically after IAT repair failure is forbidden**: record `E-iat-repair-fail` then go dynamic; using only ImportREC on 64-bit samples is forbidden
- Do not misroute web JS reverse engineering to this skill; that is `reverse-engineering` territory

## References

- Command cheatsheet: `references/cheatsheet.md`
- Standard reconnaissance script: `scripts/recon.ps1`

## radare2-skills Ecosystem

The radare2-skills project (radareorg/radare2-skills) provides a more complete ecosystem of tools and workflows:

- **r2xsql**: SQL queries over binary import tables / strings / functions
- **r2mcp / r2http**: MCP tools and a stateful HTTP command channel
- **radius2**: symbolic execution, symbolic dynamic analysis
- **r2pm**: plugin management, extensions
- **decompiler plugins**: the radare2 plugin mechanism

**Usage policy**:
- When the user mentions `r2xsql`, `r2mcp`, `r2http`, `radius2`, `r2pm`, `rabin2`, `rasm2`, `radiff2`, `rahash2`, `rax2`, route to this skill first (radare2/SKILL.md)
- These tools are ecosystem accelerators only and **cannot bypass**: the authorization gate, `tool-index` validation, Evidence imports, or write-mode confirmation
- Minimal reproducible command examples:
  - `r2xsql -s <file> -q "SELECT ..."`
  - `curl.exe -sS --data-binary 'aaa' http://127.0.0.1:9393/cmd`
  - `radius2 -p <binary> ...`
  - `r2pm -ci <plugin>`

This skill preserves its existing hard gates and evidence chain integrity; skipping any authorization or Evidence step is not allowed.

---

## Routing Context

**Upstream entries**: `skills/SKILL.md` (master control), `routing.md`
**Upstream alternatives**: `ida-reverse/` (escalate to IDA when decompilation/pseudocode is needed)
**Downstream exits**:
- Dynamic analysis needed → `reverse-engineering/tools-dynamic.md` (Frida/GDB)
- Deep decompilation needed → `ida-reverse/`
- After PAT finds interesting strings and cross-references are needed → `ida-reverse/` (IDA's xrefs are more powerful)

**Peer related modules**: `ida-reverse/` (complementary: r2 recon is fast, IDA decompilation is deep)

## On-Demand Bootstrap

This skill's entry scripts are wired into the unified bootstrap system. When radare2 is missing, it will not simply error out but will attempt automatic installation.

### Automation Capability Boundaries

| Tool | Auto-installable | Install method | Notes |
|------|-----------|---------|------|
| r2 | ✓ | GitHub Release ZIP (w64) | Auto-downloads and extracts to `%USERPROFILE%\Tools\radare2\` |
| rabin2 | ✓ | Same as above (included in the radare2 distribution) | — |
| rasm2 | ✓ | Same as above | — |
| radiff2 | ✓ | Same as above | — |
| rahash2 | ✓ | Same as above | — |
| rax2 | ✓ | Same as above | — |

### Bootstrap Trigger Points

- `scripts/recon.ps1`: automatically calls `bootstrap-reverse.ps1` when `rabin2` or `r2` is missing

### When Bootstrap Fails

If automatic installation fails (no network, GitHub API rate limiting, etc.), the script throws a clear error with a manual installation link.

Manual installation: download `radare2-*-w64.zip` from https://github.com/radareorg/radare2/releases, extract to `%USERPROFILE%\Tools\radare2\` and ensure the `bin\` directory is on PATH.


## Task Completion Self-Check (MUST pass before claiming completion)

- [ ] Did I execute every step in the workflow (not just read it)?
- [ ] Was the import table check executed and written to Evidence (E-imports / E-triage-imports or .NET equivalent)? Do DLL/SYS include E-exports?
- [ ] On IAT repair failure, was E-iat-repair-fail recorded and dynamic analysis used? Did redo requests return to the same step?
- [ ] Did I use real tool paths based on `tool-index`?
- [ ] Did I produce reproducible evidence (commands/scripts/screenshots/reports)?
- [ ] Did I complete and write back the Checklist items required by RULES?
