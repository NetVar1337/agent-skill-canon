---
name: frida-instrumentation
description: Use when dynamically instrumenting native or managed code on Windows, Linux, macOS, Android, or iOS — Frida Interceptor hooks, Stalker tracing, frida-trace CLI, Java.perform and ObjC bridges, gadget embedding, script RPC, spawn-vs-attach gating, and defeating anti-Frida detection (port 27042 scans, frida-agent artifacts, ptrace checks). Covers hook design, call reconstruction, and runtime data capture for RE, mobile app testing, game clients, and anti-cheat research.
license: MIT
---
# Frida instrumentation

Use this skill when the target must be observed or modified at runtime and static analysis alone cannot answer the question: hooking a specific function, tracing execution, dumping decrypted buffers, bypassing root/jailbreak or integrity checks, or instrumenting stripped binaries where no symbols exist. Route platform packaging to `apk-reverse` / `mobile-reverse` for Android/iOS repack concerns, `js-reverse` for browser JS targets, and `game-hacking` for engine-specific work; this skill owns the instrumentation layer itself.

## When to use

- You need arguments, return values, or on-stack data of a known function (`Interceptor`).
- You need a full instruction/call trace or coverage of a region (`Stalker`).
- Target is stripped: no exports, but you have a signature/offset (`Module.findBaseAddress` + pattern scan).
- You must instrument without a persistent agent on device (`frida-gadget` embedded in a repack).
- The target detects Frida and you need cloaking before anything else runs (early instrumentation at spawn).
- Managed runtimes: hook into Java classes (`Java.perform`) or Objective-C selectors (`ObjC.classes`).

## Core workflow

### 1. Choose the injection model first

| Situation | Model | Command |
| --- | --- | --- |
| Debugging loop, agent installed | attach | `frida -p <pid>` / `frida -n <process>` |
| Early instrumentation, anti-debug races | spawn | `frida -f <package.exe> -l script.js --no-pause` |
| No agent allowed on device | gadget | embed `frida-gadget` lib, config `.so`/`.json` beside it |
| Quick discovery, no script yet | CLI trace | `frida-trace -i "*CreateFile*" -n target.exe` |

Spawn beats attach whenever the interesting code runs before you could attach (unpacking, integrity setup, anti-debug init). `--no-pause` (or `resume()` in-script) after setup.

### 2. Resolve the target address

```
// Exported symbol
const f = Module.getExportByName(null, "ws2_32.dll!send");  // or Module.getExportByName("ws2_32.dll", "send")
// Stripped binary: pattern scan once, then pin
const base = Module.findBaseAddress("libgame.so");
const hit = Memory.scanSync(base, 0x1000000, "48 8B ?? ?? 48 85 ?? 74")[0]?.address;
```
Cache resolved addresses; re-resolve only after confirmed module reload. For versioned targets, record pattern + module + build in the case notes so the offset is reproducible.

### 3. Hook with Interceptor

```
Interceptor.attach(f, {
  onEnter(args) { this.buf = args[1]; this.len = args[2].toInt32(); },
  onLeave(retval) {
    if (this.len > 0) send({tag: "send", len: this.len}, this.buf.readByteArray(Math.min(this.len, 0x1000)));
  }
});
```
Rules that keep hooks alive:
- Read/clone everything you need in `onEnter`; arguments may be freed by `onLeave`.
- Never block in a handler for long — heavy work goes to `send()` + host-side processing, or `setTimeout(..., 0)`.
- `Interceptor.replace` for full redirection (keep a `NativeFunction` binding to the original via `new NativeFunction(f, retType, argTypes)` saved BEFORE replacing).
- Hook count discipline: every `attach` costs a trampoline; attach the minimal set, and use `Interceptor.flush()` after bulk attach before triggering the target path.

### 4. Trace with Stalker only where needed

`Stalker.follow(threadId, { events: { call: true, ret: false, exec: false } , onReceive(events) {...} })`
- `exec` events are extremely high volume: restrict with `Stalker.follow(tid, { transform: iterator => {...} })` and only emit for a range.
- Use `Stalker.exclude(Module.findRange("libllvm.so"))` for hot libraries you don't care about — exclusion is the single biggest perf lever.
- `Stalker.unfollow(tid)` + `Stalker.garbageCollect()` when a phase ends; long-running follows corrupt timing-sensitive targets.
- Typical recipe: follow only the thread that hit an `Interceptor` breakpoint (grab `this.threadId` in `onEnter`), trace between two trigger functions, unfollow at the second.

### 5. Managed runtimes

```
Java.perform(() => {
  const Cipher = Java.use("javax.crypto.Cipher");
  Cipher.doFinal.overload("[B").implementation = function (input) {
    const out = this.doFinal(input);
    send({tag:"cipher", algo: this.getAlgorithm().toString()}, out);  // Java array -> auto-converted
    return out;
  };
});
```
- iOS: `ObjC.classes.UIViewController["- viewDidAppear:"].implementation`-style hooking; `ObjC.choose({classes})` walks the heap for live instances.
- Overloads matter: always `.overload(...)` on Java, and `.implementation = function(){...}` must call through with the same arg count.
- For Unity/IL2CPP targets, hook through `il2cpp_` exports (`il2cpp_runtime_invoke`) — see `game-hacking` for engine specifics.

### 6. Script RPC and control loops

Expose stable operations instead of rewriting scripts:
```
rpc.exports = { settarget(addr) { configured = ptr(addr); }, dump() { return readStruct(); } };
```
Host side: `script.exports.dump()` (async). Combine with `frida -l agent.js --eternalize` for long-lived agents; use `frida-compile` to bundle multi-file TypeScript agents.

### 7. Gadget embedding (no-agent devices)

Drop `frida-gadget-<arch>.so` renamed to something innocuous beside/inside the target package, with a matching `<libname>.config.json` choosing interaction: `"type": "script"` with `"path"` for headless auto-run scripts (on-load hooking), or `"type": "listen"` for interactive `frida -H`. On Android this pairs with LSPatch/repack flows (`apk-reverse`); verify the loader namespace and `dlopen` chain so the gadget loads before detection code.

## Key structures & interfaces

- `Module`: `findBaseAddress`, `getExportByName`, `enumerateImports/Exports/Symbols/Ranges` — inventory before pattern scanning.
- `Memory`: `readByteArray/readCString/readPointer/writePointer/scanSync` — all raw memory access; wrap with try/catch, bad reads throw.
- `NativePointer`, `NativeFunction`, `NativeCallback` — calling conventions handled for you; declare signature once, call anywhere.
- `Java` / `ObjC` bridges: `perform`, `use`, `choose`, `enumerateLoadedClasses`, scheduler-safe wrappers.
- `send(message, data)` / `recv` — structured channel to host; binary payloads as second arg (ArrayBuffer), never as strings.

## Tooling

`frida`, `frida-trace`, `frida-ls-devices`, `frida-ps -Uai` (USB apps), `frida-compile`, `frida-server` (matching version on device — mismatch is the #1 cause of silent attach failure), `objection` (ready-made runtime exploration: `objection -g com.x explore`, memory lists, keystore dumps), `frida-gadget` builds. Pin one frida-tools version per case; server and tools major versions must match.

## Pitfalls & OPSEC

- **Detection surfaces**: default TCP 27042 listener (use `frida-server -l 0.0.0.0:PORT` on an odd port, or `udid`/USB only), process maps showing `frida-agent`/`gum-js-loop` mappings, `gmain` thread names, agent strings in memory, and `ptrace` self-attach checks. Countermeasures: spawn-time cloaking, gadget rename, `pthread` name spoofing, or hooking the detection APIs themselves (`Module.getExportByName(null,"ptrace")` first).
- **Crash discipline**: one bad hook can kill the process before your logging lands — flush `send()` eagerly, wrap risky reads, and attach `Process.setExceptionHandler` to capture context instead of dying silently.
- **Timing distortion**: Stalker and heavy `onEnter` work change race behavior; re-verify any race-condition finding without instrumentation before reporting it.
- **Root/frida coupling**: on Android, `frida-server` requires root or a repack; no-root flows go gadget/LSPatch (`apk-reverse`).
- **Loop protection**: re-entrant calls from inside a hook (e.g. logging via a function you also hooked) deadlock — guard with a busy flag.
- Never ship a one-off script without recording: frida-tools version, device/OS build, target build ID, and the resolved addresses used.

## Routing

- Android/iOS packaging, repack, signing: `apk-reverse`, `mobile-reverse`.
- Browser/JS targets, anti-debug JS: `js-reverse`.
- Game engines, offset dumps, anti-cheat interplay: `game-hacking`, `anti-cheat-bypass`.
- Static decompilation to find hook targets: `ida-reverse` / `ghidra-reverse`.
- Native crash triage after instrumentation: `debugging`.
- Implant-grade persistence of a hook: `c2-implant-engineering` (Frida is research tooling, not the delivery layer).
