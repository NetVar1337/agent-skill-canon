> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/safety.md).

# Safety

Safety mechanisms at every layer.

## Native Fault Trapping

Script faults (null deref, invalid memory access) are caught by a signal/SEH handler and mapped to source via the source map. The host receives a clean error; the application doesn't crash.

```cpp
bool ok = execute(ctx, "main");
if (!ok) {
    // fault was caught, error info has the source location
    error_info err = last_error(e);
    printf("fault at %s:%d\n", err.file.c_str(), err.line);
}
```

## Execution Budget

Prevent infinite loops and runaway scripts:

```cpp
set_budget(mod, 1000000);
```

Loop iterations are metered. An exhausted budget does **not** halt cleanly — the run is reported as a failure, but execution continues past the point the budget ran out, so any values it produced are inconsistent. Use `budget_exhausted(mod)` to tell that case apart from a genuine result, and discard the results of an exhausted run.

## Permission System

Two flags. `PERM_FFI` (`0x01`) gates `[[dll(...)]]` calls. `PERM_FILE` (`0x02`) gates the file addon. Without the matching flag, the script can't call any function gated on it — source is rejected at compile, a precompiled module at load.

```cpp
// user scripts: no FFI, no file I/O
set_permissions(e, 0);

// trusted addons: FFI only
set_permissions(e, PERM_FFI);

// host with file access too
set_permissions(e, PERM_FFI | PERM_FILE);
```

Custom natives can opt into the same gating via the trailing `permission` arg of `register_native` or `.permission(flags)` on a `type_builder` method.

## Memory Safety

Deterministic model. Structs stack-allocated by default. Escape patterns that would produce dangling stack pointers (return-pointer-to-local, store-to-global, escaping-closure-capture) are **compile errors**, not runtime bugs. Heap allocations have deterministic dtor + free. See [Lifecycle & RAII](/perception/enma-lang/sdk-guide/memory-and-raii.md).

## Type Verification

The native sig string drives compile-time type checking at every call site:

* Wrong arg type, rejected.
* Wrong struct/class identity, rejected.
* Wrong enum identity (raw int or cross-enum): rejected.
* Wrong typed-container element (`array<T>` / `map<K, V>`): rejected, including return-side var-decl.
* Calling a non-`const` method on a `const` receiver, rejected.
* Assigning through a `const` parameter, rejected.

Implicit conversion via `.convert()` registered functions fires automatically at native arg, var-decl, and binary-op sites when the source/dest types differ.

## Thread Safety

* Each engine is independent, no shared mutable state between engines.
* Multiple contexts from the same module can execute concurrently.
* Per-thread TLS prevents cross-thread state corruption.
* Tested under ASAN and TSAN with 32+ concurrent threads.

## Sandboxing

For untrusted scripts, combine these techniques:

```cpp
engine_t* e = create();
register_addon_core(e);    // print only
register_addon_std(e);     // the prelude; the rest stay unregistered
set_permissions(e, 0);     // no FFI

module_t* mod = compile(e, src, len, "sandboxed.em");
set_budget(mod, 100000);

register_typed<&get_score_fn>(e, "int32 get_score()");
```

The script only sees what you register. No file I/O, no network, no native library access.

**These controls isolate the host from&#x20;*****accidents*****, not from&#x20;*****intent*****.** Scripts can construct pointers from integers (`reinterpret_cast<T*>(addr)`) and call raw memory operations from the always-registered prelude, so script code that is actively hostile is not contained by them. Run genuinely untrusted code in a separate process.

Permissions travel with the artifact. A module records the permission set its gated calls require, and `deserialize` re-validates that set against the loading engine — an artifact whose calls need `PERM_FFI` does not load on an engine that grants nothing. A precompiled `.emb` is gated exactly as its source would have been.

## Raw Pointer Safety

All values are 64-bit integers at the ABI. Pointers (strings, arrays, structs) are raw addresses. Scripts can pass arbitrary integer values to natives. Validate pointer args before dereferencing:

```cpp
int64_t my_native_fn(MyType* obj) {
    if (!obj) return 0;
    if (!heap_is_tracked(obj)) return 0;   // only for allocations from Enma's heap
    // safe to use obj
}
```

Invalid pointer deref inside compiled code is caught by the runtime fault handler; `execute()` returns `false`, host stays alive. Invalid pointer deref inside a native function is **not** caught, validate on the native side.

Scripts can't call arbitrary addresses as functions. Delegates resolve at compile time to known function indices, a bare integer will not bind to a callback parameter, and a callback handle is validated before it is invoked — one that does not denote a function the module owns is refused rather than called.
