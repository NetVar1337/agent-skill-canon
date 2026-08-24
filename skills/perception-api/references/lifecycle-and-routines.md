> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/lifecycle-and-routines.md).

# Lifecycle and Routines

## Entry point

Every script needs a `main()` function. It runs once when the script is loaded.

```cpp
int64 main() {
    // setup state, load resources, register routines
    return 1;
}
```

`main()`'s return value decides what happens next:

| Return | Behavior                                           |
| ------ | -------------------------------------------------- |
| `> 0`  | Script stays loaded.                               |
| `<= 0` | Script unloads immediately after `main()` returns. |

Use `return 1;` for any normal long-lived script. Return `0` for one-shot scripts that just wanted to do work in `main()` and exit.

## Routines

A routine is a script function that runs continuously after `main()` returns. Routines are how your script keeps doing work over time.

```cpp
int64 register_routine(int64 fn_handle, int64 data);
bool  unregister_routine(int64 routine_handle);
```

### Callback shape

The function you register takes one `int64` parameter:

```cpp
void my_callback(int64 data) {
    // data: the value you passed as the second arg to register_routine
}
```

Pass the function as a closure handle via `reinterpret_cast<int64>(fn_name)` — this is a real C++ reinterpret cast; the bare ember `cast<int64>` form is retired:

```cpp
int64 main() {
    int64 r = register_routine(reinterpret_cast<int64>(my_callback), 42);
    return 1;
}
```

`register_routine` returns a handle. Keep it if you intend to unregister later, or discard.

### Multiple routines

Register as many as you need.

```cpp
void on_render(int64 data) { /* draw */ }
void on_tick(int64 data)   { /* update logic */ }

int64 main() {
    register_routine(reinterpret_cast<int64>(on_render), 0);
    register_routine(reinterpret_cast<int64>(on_tick), 0);
    return 1;
}
```

### Unregistering

```cpp
unregister_routine(my_handle);
```

A routine can also unregister itself from inside its own callback:

```cpp
int64 g_handle;

void my_callback(int64 data) {
    if (should_stop()) {
        unregister_routine(g_handle);
        return;
    }
    // normal work
}

int64 main() {
    g_handle = register_routine(reinterpret_cast<int64>(my_callback), 0);
    return 1;
}
```

## Unload

A script unloads when `main()` returns `<= 0`, when the user unloads it from the UI, or when the host shuts down. On unload all routines stop and any GPU resources you created via the render API are destroyed automatically.

## Exceptions

Routines automatically catch uncaught throws and faults. The error is logged to `<my_games>\exceptions\enma.log` with a timestamp, the routine id, the thrown value, and the source line where it happened. The script keeps running.

## Diagnostic helpers

Quick tracing without touching the renderer:

```cpp
void heartbeat();                          // log "heartbeat called"
void dbg_print(const char* s);             // log a string
void dbg_print(const std::string& s);      // std::string overload
void dbg_print_int(int64 x);               // log an int value (dec + hex)
void take_int(int64 x);                    // log an int value
void take_ptr(int64 p);                    // log a pointer in hex
```

Useful for confirming a code path is reached or sanity-checking a value.
