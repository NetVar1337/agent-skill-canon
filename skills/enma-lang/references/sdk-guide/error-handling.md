> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/error-handling.md).

# Error Handling

## Error Info

Check the engine's error state after any operation:

```cpp
error_info err = last_error(e);
if (err.code != 0) {
    printf("[%s:%d:%d] %s\n",
        err.file.c_str(), err.line, err.column,
        err.message.c_str());
}
```

## Message-Only Check

```cpp
const char* msg = last_error_message(e);
if (msg && msg[0] != '\0') {
    printf("error: %s\n", msg);
}
```

## Warnings

Diagnostics that did not fail the compile — a `#warning`. Refilled by every compile on the engine, so read it after the compile you care about.

```cpp
std::vector<error_info> warns;
uint32_t n = last_warnings(e, warns);
for (uint32_t i = 0; i < n; ++i) {
    printf("[%s:%u] %s\n",
        warns[i].file.c_str(), warns[i].line,
        warns[i].message.c_str());
}
```

## Error Codes

The `error_info.code` field indicates the error category:

* `0` = no error
* Parser errors = syntax problems in source code
* Type errors = type mismatches, undefined symbols
* Runtime errors = segfaults in compiled code, budget exhaustion

## Compile-Time Errors

```cpp
module_t* mod = compile(e, src, len, "script.em");
error_info err = last_error(e);
if (err.code != 0) {
    // compilation failed
    printf("compile error: %s\n", err.message.c_str());
    printf("  at %s:%d:%d\n", err.file.c_str(), err.line, err.column);
}
```

### Diagnostic shape

Compile errors include a one-line summary followed by a `hint:` line where applicable. Examples:

```cpp
cannot implicitly convert scoped enum `Color` to an arithmetic type; use static_cast

cannot assign raw value to pointer type; use &variable, null, or 0 — or reinterpret_cast<T*>(...) for an integer address

cannot compare a pointer against an integer
  hint: use reinterpret_cast<int64>(ptr) — or 0/null with ==/!= for a null check

file_open() requires PERM_FILE, but engine has only PERM_NONE
  hint: call set_permissions(engine, PERM_FILE) before compile()
```

Permission errors name the missing flag (`PERM_FILE`, `PERM_FFI`) and the current grant.

## Runtime Errors

Runtime errors (segfaults, null dereferences) are caught by the native fault handler and mapped to source locations via the source map:

```cpp
bool ok = execute(ctx, "main");
if (!ok) {
    error_info err = last_error(e);
    printf("runtime error: %s\n", err.message.c_str());
}
```

## Exception Access

```cpp
if (exception_pending(mod)) {
    int64_t val  = exception_value(mod);   // the thrown value
    int64_t tid  = exception_type(mod);    // type hash of the thrown value
    printf("exception: value=%lld type=%llu\n", val, tid);
    exception_clear(mod);
}
```

The runtime fault handler clears exception state before returning control to the host. For uncaught throws after execution, check `last_error()`:

```cpp
bool ok = execute(ctx, "main");
if (!ok) {
    error_info err = last_error(e);
    // err contains the uncaught exception info
}
```
