> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/debug-and-heap.md).

# Debug & Heap

## Debug Hooks

Callback fires at every source line:

```cpp
void my_debug_hook(const char* file, uint32_t line, int64_t* frame) {
    printf("[%s:%d]\n", file, line);
}

set_debug_hook(mod, my_debug_hook);
```

`frame` indexes into the current function's locals by slot number. Enable debug mode for source maps:

```cpp
set_debug(e, true);
```

## Execution Budget

```cpp
set_budget(mod, 1000000);  // max 1 million instructions; 0 = disabled
```

When exhausted, `execute()` returns `false`.

## Memory Model

Deterministic. No tracing collector. See [Lifecycle & RAII](/perception/enma-lang/sdk-guide/memory-and-raii.md) for the full model.

`heap_*` SDK entry points exist for stats and budget control only:

```cpp
heap_set_memory_budget(mod, ...);  // hard limit on live heap bytes
heap_stats stats = heap_get_stats(mod);
```

### Stats

```cpp
heap_stats stats = heap_get_stats(mod);
printf("live allocs: %llu\n", stats.alloc_count);   // currently-alive heap objects
printf("live bytes:  %llu\n", stats.total_bytes);
printf("total freed: %llu\n", stats.freed_count);
printf("freed bytes: %llu\n", stats.freed_bytes);
```

| Field         | Description                         |
| ------------- | ----------------------------------- |
| `alloc_count` | Currently-live heap allocations     |
| `total_bytes` | Currently-live heap bytes           |
| `freed_count` | Cumulative frees since engine start |
| `freed_bytes` | Cumulative bytes freed              |

Unbounded `alloc_count` growth indicates a leak, typically a `new` whose handle isn't stored in a scope-tracked local or a pointer global that isn't reset.

### Memory Budget

Set a hard limit on total live heap bytes. `alloc()` returns null when exceeded.

```cpp
heap_set_memory_budget(mod, 1024 * 1024 * 64);  // 64 MB cap
heap_set_memory_budget(mod, 0);                  // unlimited (default)
```

## Stack Traces

Requires `set_debug(e, true)`.

```cpp
stack_frame frames[32];
uint32_t count = get_stack_trace(ctx, frames, 32);
for (uint32_t i = 0; i < count; ++i) {
    printf("  %s (%s:%d)\n", frames[i].function, frames[i].file, frames[i].line);
}
```

`stack_frame` fields: `function` (`const char*`), `file` (`const char*`), `line` (`uint32_t`).

`get_stack_trace` is best-effort. Compiled code doesn't push fixed frame metadata, so what you get back is the set of functions the module can report on. Treat it as a "what functions are visible in this module" snapshot rather than a true call-frame walk - it does not reflect real recursion depth or invocation order.

## Last-Executed Line

For pinpointing exactly where a native fault hit, use `get_last_executed_line(mod)`:

```cpp
set_debug(e, true);              // must be set BEFORE compile so line tracking is emitted
module_t* mod = compile_file(e, "script.em");
context_t* ctx = create_context(mod);

if (!execute(ctx, "main")) {
    int64_t line = get_last_executed_line(mod);
    printf("crashed at line %lld\n", line);
}
```

Compiled code records the source line before each statement runs, so the value persists across a fault — pointing at the exact source line that was executing. Returns `-1` when no line has executed yet, `0` when debug wasn't enabled at compile time. More reliable than `get_stack_trace` for crash diagnostics.

## IDE Debugger SDK

A debugger SDK on top of the per-line hook foundation. All opt-in via `set_debug(e, true)` BEFORE compile.

For full local-variable visibility you should ALSO call `set_optimize(e, false)`. Otherwise the optimizer promotes locals to registers and they never land in their declared frame slots, so `read_local_*` returns garbage.

The surface is a source-map lookup (`find_fn_at`, `find_code_offsets`), local reads (`read_local_int` / `_float` / `_string` / `_pointer`), breakpoint and step control (`set_step_mode`, `pause`, `resume`, `should_pause_at`) and the call-depth helpers. Every signature is in [API Reference](/perception/enma-lang/sdk-guide/api-reference.md#debug-heap). State is per-context, so multi-thread routines each get independent step/pause state. A debugger front-end implements its DAP / VS Code adapter on top of these primitives.

Typical hook:

```cpp
void on_line(const char* file, uint32_t line, int64_t* frame) {
    context_t* ctx = active_context();
    module_t* mod = my_module;
    if (!should_pause_at(ctx, mod, frame, file, line)) return;

    auto* fn = find_fn_at(mod, file, line);
    em_local_info infos[64];
    uint32_t n = enumerate_locals(fn, infos, 64);
    for (uint32_t i = 0; i < n; ++i) {
        switch (infos[i].type) {
            case type_id::t_int64:   inspect(read_local_int   (frame, infos[i].slot)); break;
            case type_id::t_float64: inspect(read_local_float (frame, infos[i].slot)); break;
            default:                 inspect(read_local_pointer(frame, infos[i].slot));
        }
    }
    // ... wait for IDE command, then either:
    set_step_baseline_depth(ctx, get_call_depth(frame, mod));
    set_step_mode(ctx, step_mode::step_over);   // or step_in / step_out / step_none
    resume(ctx);   // clears pause_requested
}
```
