---
name: apk-reverse
description: Use when doing Android APK reverse engineering in a CLI environment. Covers APK unpacking, Java decompilation, smali modification, repacking, Frida dynamic hooking, and switching to so/native analysis as needed. Prefer the locally installed jadx, apktool, frida, adb, ida-reverse, and radare2.
---

## ACTION REQUIRED (execute immediately after reading)

> Endpoint extraction/Frida adaptation community cross-check: ../references/community-security-skills.md; dynamic analysis requires scope-authorized devices.

1. `NOW`: Read `../field-journal/precedent-reverse.md` — confirm that this skill's operations are pre-authorized routine operations
2. `NOW`: Confirm whether the current task falls within this skill's scope
3. `NEXT`: Read `../tool-index.md`, verify tool availability and actual paths
4. `NEXT`: If tools are missing, invoke bootstrap — do not guess paths
5. `ACT`: Enter step one of the "Workflow" and execute; do not stop at a confirmation state

# APK Reverse Engineering CLI Working Standard

## Scope

Prefer this skill when the task falls under these scenarios:

- Analyzing an APK's Java business logic
- Locating login, signing, risk control, certificate validation, and root detection
- Viewing and modifying `AndroidManifest.xml`
- Viewing and modifying smali
- Repacking an APK
- Using Frida for Java/native dynamic hooking
- Switching to native analysis when the APK contains `.so`

## CLI Tools Verified Available on This Machine

- `jadx` `1.5.5`
- `apktool` `3.0.2`
- `frida-ps` `17.9.6`
- `adb`
- `java`

## Scenarios Where Scripts Are Preferred

The following flows are high-frequency and error-prone to parameterize; prefer the skill's bundled scripts:

- One-shot `jadx + apktool` output to disk with a summary: `scripts/decode.ps1`
- Frida device checks, process listing, spawn/attach injection: `scripts/frida-run.ps1`
- Rebuild, align, sign, and install an APK: `scripts/rebuild-sign-install.ps1`
- Quick extraction of key Manifest components and permissions: `scripts/manifest-summary.ps1`

The following one-liners stay as direct invocations, with no separate wrapper:

- `adb devices`
- `adb logcat`
- `frida-ps -U`
- `jadx --version`
- `apktool --version`

## Bundled Scripts

### `scripts/decode.ps1`

Purpose:

- Run `jadx` and `apktool` uniformly
- By default create a task output directory alongside the original APK
- Output a summary of `package`, `java_files`, `smali_dirs`, `so_files`, etc.
- Tolerate cases where `jadx` partially fails decompilation but still yields usable artifacts

Examples:

```powershell
pwsh -File "<skill-root>\apk-reverse\scripts\decode.ps1" -ApkPath "D:\DOWNLOAD\app.apk" -Clean
pwsh -File "<skill-root>\apk-reverse\scripts\decode.ps1" -ApkPath "D:\DOWNLOAD\app.apk" -Name demo -SkipJadx
```

### `scripts/frida-run.ps1`

Purpose:

- Unify Frida's device, process, and spawn/attach entry points
- Avoid mixing up `-f`, `-n`, `-U` when writing parameters by hand

Examples:

```powershell
pwsh -File "<skill-root>\apk-reverse\scripts\frida-run.ps1" -ListDevices
pwsh -File "<skill-root>\apk-reverse\scripts\frida-run.ps1" -Usb -ListProcesses
pwsh -File "<skill-root>\apk-reverse\scripts\frida-run.ps1" -Usb -Spawn -Package com.example.app -ScriptPath "D:\hooks\test.js"
```

### `scripts/rebuild-sign-install.ps1`

Purpose:

- `apktool b` to rebuild the APK
- `zipalign` to align
- `apksigner` to sign and verify
- Optionally install directly with `adb install`

Examples:

```powershell
pwsh -File "<skill-root>\apk-reverse\scripts\rebuild-sign-install.ps1" -ProjectDir "C:\work\apktool_out" -Clean
pwsh -File "<skill-root>\apk-reverse\scripts\rebuild-sign-install.ps1" -ProjectDir "C:\work\apktool_out" -Install -Reinstall -DeviceSerial "127.0.0.1:7555"
```

Notes:

- Generates and reuses a debug keystore by default
- Outputs to the same directory as `ProjectDir` by default, making it easy to keep next to the original package and the unpacked directory

### `scripts/manifest-summary.ps1`

Purpose:

- Extract the package name
- List permissions
- List activity/service/receiver/provider
- Mark the main launcher activity

Example:

```powershell
pwsh -File "<skill-root>\apk-reverse\scripts\manifest-summary.ps1" -ManifestPath "C:\work\apktool_out\AndroidManifest.xml"
```

If you need to analyze `.so`, `lib/arm64-v8a/*.so`, `lib/armeabi-v7a/*.so`, combine with:

- `ida-reverse`
- `radare2`

## Tool Division of Labor

### `jadx`

Used for:

- Java decompilation reading
- Package, class, and method name searches
- Understanding the APK from high-level logic first

Common commands:

```bash
jadx -d jadx_out app.apk
jadx --single-class com.example.LoginActivity -d jadx_out app.apk
jadx --deobf -d jadx_out app.apk
```

### `JEB Pro` (optional commercial tool)

Used for:

- Cross-validation and deep decompilation of Android DEX / APK / ARM
- Supplementing static analysis when JADX output is incomplete or heavily obfuscated
- Second-toolchain verification of classes, methods, and call relationships for the same target

Boundaries:

- JEB Pro is commercial software; the user must obtain and install a valid license themselves; this pack will not download, crack, or circumvent licensing.
- Invoke it only when `tool-index` confirms JEB is available on this machine; otherwise continue with `jadx`, `apktool`, Ghidra, IDA, or radare2.
- Third-party JEB MCP bridges are not a dependency of this pack. Before installation you must review source code, permissions, network behavior, and versions per `../ops/skill-supply-chain.md`, then have the user explicitly confirm registration.

### `apktool`

Used for:

- Unpacking an APK
- Viewing and modifying `AndroidManifest.xml`
- Viewing and modifying smali
- Rebuilding an APK

Common commands:

```bash
apktool d app.apk -o apktool_out
apktool b apktool_out -o rebuilt.apk
```

### `frida`

Used for:

- Dynamically observing Java method calls
- Hooking native exported functions
- Bypassing root detection, certificate validation, and debug detection

Common commands:

```bash
frida-ps -U
frida -U -f com.example.app -l hook.js
frida-trace -U -f com.example.app -j '*!*certificate*'
```

### `adb`

Used for:

- Device connections
- Installing APKs
- Viewing logs
- Pulling files

Common commands:

```bash
adb devices
adb install -r app.apk
adb shell pm list packages
adb logcat
adb pull /data/local/tmp/file .
```

## Recommended Workflow

### 1. Triage

First determine the APK's overall composition; do not rush into patching or hooking.

Suggested actions:

1. Export Java code with `jadx -d jadx_out app.apk`
2. Export smali and resources with `apktool d app.apk -o apktool_out`
3. First look at:
   - `AndroidManifest.xml`
   - The main `package`
   - `application`, `activity`, `service`, `receiver`
   - Whether `lib/` contains `.so`
4. Issue #65 threat-pattern quick reference (authorized samples/devices; see `../reverse-engineering/references/nonpe-format-cookbook.md` §7–8):
   - Transparent/hidden icon (AU): `aapt dump badging` + manifest theme/label/icon → `E-android-hidden-icon-manifest`
   - Magisk/script device-wipe signatures and remote curl|sh (AR/AS) → record signatures and URLs as evidence; **do not execute** destructive commands
   - Persistence paths (AT): `service.d` / `priv-app` etc. → `E-android-persistence`

### 2. Java Logic Observation

Read from `jadx_out` first:

- `MainActivity`
- `Application`
- Login, network, crypto, and risk-control related classes
- Third-party SDK initialization classes

Common keywords:

- `login`
- `sign`
- `encrypt`
- `cipher`
- `token`
- `root`
- `certificate`
- `trust`
- `okhttp`
- `retrofit`
- `webview`

If the Java code is readable, locate the business logic here first.

### 3. Smali and Resource Layer Confirmation

When `jadx` results are incomplete, heavily obfuscated, or an actual patch is needed, switch to `apktool_out`:

- Look at `smali*/`
- Look at `res/values/strings.xml`
- Look at `AndroidManifest.xml`

Preferred patch targets:

- `android:exported`
- Debug flags
- Root detection return values
- Login validation logic
- Certificate validation branches

### 4. Rebuild and Install

After modifying:

```bash
apktool b apktool_out -o rebuilt.apk
```

Or close the loop directly with the script:

```powershell
pwsh -File "<skill-root>\apk-reverse\scripts\rebuild-sign-install.ps1" -ProjectDir "apktool_out" -Install -Reinstall -DeviceSerial "127.0.0.1:7555"
```

Notes:

- This skill only guarantees the `apktool` rebuild chain
- If you later need to formally install to a device, a signing flow is usually also required
- If the task enters signing/alignment, add `apksigner` / `zipalign`

### 5. Dynamic Hooking

When static analysis is insufficient, use Frida:

- Hook login functions
- Hook key `OkHttp` / `Retrofit` / `WebView` points
- Hook `javax.crypto`, `MessageDigest`
- Hook root detection functions
- Hook SSL pinning logic

Principles:

- Hook the Java layer first, then decide whether native hooks are needed
- Print arguments and return values first, then decide whether to actively modify return values

Suggestions:

- For simple one-shot commands, use `frida-*` directly
- For injection flows needing stable reuse, prefer `scripts/frida-run.ps1`

### 6. Native `.so` Split-Off

If the APK contains critical `.so`:

- Find `lib/**/*.so` with `apktool` or `jadx`
- If you only need exported symbols, strings, or quick triage, use `radare2`
- For long-term deep analysis, decompilation, renaming, and type recovery, use `ida-reverse`

Switch to native as soon as you see these signals:

- The Java layer is just a JNI wrapper
- The core signing logic is not in Java
- Key logic disappears after `System.loadLibrary()`
- Certificate validation/risk control is in the `.so`

## Output Requirements

At minimum, state in the end:

- Entry components and key classes
- Whether the key logic is in Java, smali, or `.so`
- Confirmed sensitive points: login, signing, root, SSL, WebView, JNI
- If you patched, explain what changed
- If you hooked, explain which class/method/exported function was hooked

## Prohibited Actions

- Do not blindly modify smali from the start
- Do not write hooks before reading the manifest and main entry
- Do not equate incomplete Java decompilation with "logic is unanalyzable"
- Do not keep grinding the Java layer when the `.so` clearly carries the core logic

## Quick Command Memo

```bash
# Decompile Java
jadx -d jadx_out app.apk

# Unpack the APK
apktool d app.apk -o apktool_out

# Rebuild the APK
apktool b apktool_out -o rebuilt.apk

# Devices and processes
adb devices
frida-ps -U

# Launch and inject
frida -U -f com.example.app -l hook.js
```

---

## Routing Context

**Upstream entries**: `skills/SKILL.md` (master control), `routing.md`
**Downstream exits**:
- Core logic in `.so` → `ida-reverse/` or `radare2/`
- Dynamic hooking/verification needed → `reverse-engineering/tools-dynamic.md` (Frida section)
- General reverse engineering methodology → `reverse-engineering/SKILL.md`

**Peer related modules**: `reverse-engineering/` (.so analysis and advanced Frida usage)

---

## On-Demand Bootstrap

This skill's entry scripts are wired into the unified bootstrap system. When tools are missing, it will not simply error out but will attempt automatic installation.

### Automation Capability Boundaries

| Tool | Auto-installable | Install method | Notes |
|------|-----------|---------|------|
| jadx | ✓ | GitHub Release ZIP | Auto-downloads and extracts to `%USERPROFILE%\Tools\jadx\` |
| apktool | ✓ | GitHub Release JAR + wrapper | Auto-downloads the jar and generates a bat in `%USERPROFILE%\Tools\apktool\` |
| JEB Pro | ✗ | User installs manually with a valid license | Optional Android / ARM cross-validation tool; third-party MCP bridges need separate auditing |
| frida / frida-ps | ✓ | pip install frida-tools | Requires Python to be installed |
| adb | ✓ | winget / fallback path | Auto-installs Android Platform-Tools |
| zipalign | ✗ | Requires manual install of Android Build-Tools | `sdkmanager "build-tools;35.0.0"` |
| apksigner | ✗ | Requires manual install of Android Build-Tools | Same as above |

### Bootstrap Trigger Points

- `scripts/decode.ps1`: automatically calls `bootstrap-reverse.ps1` when jadx or apktool is missing
- `scripts/rebuild-sign-install.ps1`: automatically calls bootstrap when adb or apktool is missing
- `scripts/frida-run.ps1`: still a manual check (frida is usually already installed via pip)

### When Bootstrap Fails

If automatic installation fails, the script throws a clear error with a manual installation link. Common causes:
- No network (GitHub API / PyPI unreachable)
- winget unavailable (Windows version too old)
- Java not installed (apktool depends on the JDK)


## Task Completion Self-Check (MUST pass before claiming completion)

- [ ] Did I execute every step in the workflow (not just read it)?
- [ ] Did I use real tool paths based on `tool-index`?
- [ ] Did I produce reproducible evidence (commands/scripts/screenshots/reports)?
- [ ] Did I complete and write back the Checklist items required by RULES?
- [ ] If hidden icon/device-wipe/persistence clues were hit: did I record E-android-* Evidence per the U–AV cookbook (within authorized scope)?
