---
name: router-reverse-skill-router-dotnet-reverse
description: .NET / C# binary reverse engineering. Use when the target is a .NET assembly (PE header containing CLR, .exe/.dll managed program), C# compiler output (including NativeAOT), red team Sharp* tools (Rubeus / SharpHound / etc.), .NET obfuscated programs (ConfuserEx / SmartAssembly / Babel / Eazfuscator), or .NET loaders / info-stealers / wrapped malware. Prefer dnSpyEx + de4dot; integrate with dnSpy MCP when the AI needs to operate directly. Not for pure native binaries (use reverse-engineering / ida-reverse).
license: MIT
compatibility: Requires a filesystem-based code agent or CLI with shell access, Windows host preferred (dnSpyEx is a Windows GUI); on Linux/macOS use ILSpy/de4dot CLI + mono/dotnet runtime.
allowed-tools: Bash Read Write Edit Glob Grep Task WebFetch WebSearch
metadata:
  user-invocable: "false"
---

# .NET / C# Reverse Engineering Operating Standard

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: Use DIE/`file`/CLR header to confirm the target is .NET managed (otherwise SWITCH to `ida-reverse/` / `reverse-engineering/`)
2. `NOW`: If obfuscation is suspected → first unpack with `de4dot`, producing `*-clean.exe`, keep the original sample
3. `NEXT`: dnSpyEx (or dnSpy MCP / `ilspycmd`) static: browse C# + use the **IL view** for key decision logic
4. `ACT`: Use dynamic debugging when plaintext/C2 is needed; when changing logic, prefer **IL patch** over C# recompilation
5. At the end of a phase, give the user a 3–6 item next-step menu (including report export)

## Scope

Prefer this skill when the task falls under these scenarios:

- Identifying and reverse engineering .NET / C# compiler output (managed PE / .exe / .dll)
- Analyzing red team Sharp* toolchains (Rubeus, SharpHound, SharpShell, etc.)
- Deobfuscating shells such as ConfuserEx / SmartAssembly / Babel / Eazfuscator / .NET Reactor
- Reverse engineering .NET loader / info-stealer / RAT decryption and C2 logic
- Patching C# programs (changing checks, changing constants, keygens)
- Analyzing the Mono/Unity managed layer prior to IL2CPP (note: IL2CPP output is native after compilation, use `reverse-engineering/` + seed-014)

If the target is a pure native binary (C/C++/Go/Rust compiled, no CLR), use `reverse-engineering/`, `ida-reverse/`, or `radare2/` instead.

## Core Principles

- **Identify before acting**: first confirm it is a .NET managed program (PE header CLR + `#~` / `#Strings` streams + mscoree `_CorExeMain`), then decide to go with dnSpy rather than IDA
- **IL over C#**: dnSpyEx's C# decompiler loses/distorts information (compiler-generated state machines, async/await, yield); for key logic and patches you must switch to the **IL editor**; the C# view is only for quick browsing
- **de4dot first**: when encountering an obfuscator, first run a `de4dot` unpacking pass before static analysis, otherwise the strings/control flow are all garbled
- **MCP integration**: if a dnSpy MCP is registered in the environment (`dnspy_*` tools), prefer the MCP surface for decompile / IL inspection to avoid switching back and forth to the GUI
- **Evidence-based output**: deobfuscated artifacts, extracted config/C2/keys, and patch diffs must all be saved to disk

## Toolchain Mapping

| Capability | First choice | Notes |
|------|------|------|
| Decompile + debug + patch | **dnSpyEx** | The ace; the only GUI with an IL editor; the old dnSpy is unmaintained, use the Ex fork |
| Lightweight CLI / headless decompilation | **ILSpy** (`ilspycmd`) | Suited for batch, scripted, Linux/macOS |
| Deobfuscation | **de4dot** | The default solution for the whole ConfuserEx family, SmartAssembly, and other mainstream shells |
| Obfuscator identification | **Detect It Easy (DIE)** / **file** | Determine the shell type first, then decide de4dot arguments |
| Programmatic IL manipulation | **dnlib** | Write C# scripts to batch-edit metadata / string decryptors |
| Direct AI operation | **dnSpy MCP** | `dnspy_decompile` / `dnspy_inspect_il` tool surface |

> Prerequisite: on a Windows host install dnSpyEx + de4dot (choco or release); on Linux/macOS use `ilspycmd` + `dotnet runtime`. See the installation matrix in `references/sharp-tools.md`.

## Six-Phase Workflow

### 1. Identify (identify .NET)

Confirm the target is a managed program; do not analyze a native PE as .NET:

```powershell
# Windows
file target.exe                       # "PE32 executable ... for MS Windows" is not enough
# Key: check for CLR
powershell -c "[System.Reflection.AssemblyName]::GetAssemblyName('target.exe')"
# Or
drag it straight into dnSpyEx —— if it opens, it is managed

# Generic
strings target.exe | grep -iE "mscoree|_CorExeMain|mscorlib|System\\."
```

**.NET identification markers:**
- PE header `Data Directory[14]` (CLR Runtime Header) non-zero
- `mscoree.dll` import / `_CorExeMain` entry point
- `#~`, `#Strings`, `#US`, `#GUID`, `#Blob` metadata streams
- `mscorlib` / `System.Private.CoreLib` strings

**NativeAOT exception:** compiled to native, no CLR header, but has `System.Private.CoreLib` strings and reconstructed type metadata —— route these to `reverse-engineering/` (IDA/r2); this skill only provides identification hints.

### 2. Detect (detect the obfuscator)

```powershell
# Quick identification with DIE
diec target.exe                        # Detect It Easy CLI
# Or drag into dnSpyEx and check for massive garbled class names / control flow deformation
```

Common obfuscators → unpacking strategy (see `references/obfuscators.md` for details):

| Obfuscator | Signatures | de4dot handling |
|--------|------|------------|
| ConfuserEx (1.0.0 / 2.x) | `<module>` anti-tamper, control flow deformation, string encryption | `de4dot target.exe` usually auto-detected |
| SmartAssembly | `circular`/`string encoding`, resource compression | `de4dot target.exe` |
| Babel.NET | method body encryption, control flow | `de4dot target.exe` |
| Eazfuscator.NET | string/resource encryption | `de4dot`, some versions need manual work |
| .NET Reactor | anti-tamper + necrobit | `de4dot`, newer versions may fail and need manual work |

### 3. Deobfuscate

```powershell
# de4dot auto-identifies most shells by default
de4dot target.exe -o target-clean.exe

# Specify the type (when auto-detection fails)
de4dot --type cfze target.exe          # ConfuserEx
de4dot --type sa target.exe            # SmartAssembly

# Multi-layer obfuscation / de4dot reports unknown
de4dot --detect target.exe             # see what it identifies it as
# You may need to patch anti-tamper first, then de4dot (see references/obfuscators.md)
```

Output: `target-clean.exe`; use it for subsequent analysis. **Keep the original sample** for comparison.

### 4. Static Analyze

Load the unpacked sample in dnSpyEx:

- **C# view**: quickly browse class structure, method signatures, strings (for orientation)
- **IL view**: key decision logic, encryption logic, and state machines must be inspected in IL (right-click → Edit IL or IL view)
- Find the entry point: `Main` / `Startup` / module initializers (`Module .cctor`)
- Find key logic: search for `flag`, `password`, `verify`, `check`, `encrypt`, `http`, `Config`

```text
Locate a string → follow references back → find the method using it → inspect decision logic in IL view
```

### 5. Dynamic (dynamic debugging)

dnSpyEx debugger: attach to process / start debugging, set breakpoints on key methods, observe at runtime:
- Decrypted plaintext strings (many obfuscators only decrypt strings at runtime)
- C2 addresses, config decryption results
- Exception-driven control flow (anti-debug often hides the real path with `try/catch`)

> .NET dynamic debugging is far friendlier than native —— you can directly see object values and string contents. Prefer dynamic over grinding on static analysis.

### 6. Patch (modify as needed)

```text
dnSpyEx → right-click method → Edit Method (C#) or Edit IL
  - Change a check: ldc.i4.0 → ldc.i4.1 (false→true)
  - Change a constant: edit the string/number directly
  - Remove validation: nop out the whole block
File → Save Module → replace the original file
```

**IL patch reliability > C# patch**: C# recompilation may fail (missing references, bad syntax), while IL editing almost never distorts. See `references/common-workflow.md`.

## Trigger Scenario Routing

Enter this skill when the user says:
- ".NET / C# binary reverse engineering" / "decompile a C# program"
- "analyze with dnSpy" / "patch with dnSpyEx"
- "ConfuserEx / SmartAssembly / Babel deobfuscation / unpacking"
- "analyze Sharp* tools" (Rubeus / SharpHound / SharpShell)
- "reverse a .NET malware / loader / info-stealer"
- "patch a C# program / keygen / modify a check"

## When to Switch Out

- Unity games compiled with IL2CPP → `reverse-engineering/` + `seed-014_unity-il2cpp-reverse.md` (IL2CPP is native, not for dnSpy)
- NativeAOT output → `reverse-engineering/` (same as above, native)
- Pure native PE (no CLR) → `reverse-engineering/` / `ida-reverse/`
- Need to migrate symbols/functions in batch to another version → `binary-diff/`
- Need to draw attack path / call chain diagrams → `diagram-generator/`

## Routing Context

**Upstream entry points**: `skills/SKILL.md` (master control), `routing.md`
**Downstream exits**:
- IL2CPP / NativeAOT (native) → `reverse-engineering/`
- Deep analysis of native .so/.dll sections → `ida-reverse/` / `radare2/`
- Need the AI to operate dnSpy directly → register and integrate the dnSpy MCP (see `references/sharp-tools.md`)

**Peer related modules**:
- `reverse-engineering/languages-compiled.md` (the .NET intro points to this module)
- `apk-reverse/` (for Xamarin/MAUI Android reverse engineering you can switch back to this module to inspect the C# layer)

## Reference Documents

- [references/obfuscators.md](references/obfuscators.md) — detailed ConfuserEx / SmartAssembly / Babel / Eazfuscator / .NET Reactor deobfuscation + anti-tamper bypass
- [references/common-workflow.md](references/common-workflow.md) — full workflow, IL patch reliability, string decryptor extraction, state machine identification
- [references/sharp-tools.md](references/sharp-tools.md) — red team Sharp* tool analysis, tool installation matrix, dnSpy MCP integration, community resource index

## Task Completion Self-Check

- [ ] Was CLR / managed identity confirmed (or was this skill SWITCHED out of)?
- [ ] For obfuscated samples, was de4dot / equivalent unpacking done before deep analysis?
- [ ] Was key logic verified in the IL view (rather than only reading C# pseudocode)?
- [ ] Were artifacts (clean sample / config / patch diff) saved to disk and reproducible?
- [ ] Was a next-step menu or report exit provided?
