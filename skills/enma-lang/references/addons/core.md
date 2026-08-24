> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/core.md).

# Core

Registered with `register_addon_core(engine)`.

## Output

```c
void print(const char* text)
void print(const std::string& text)
void println(const char* text)          // appends a newline
void println(const std::string& text)
```

These take text and nothing else — build the string first. `std::to_string` and `std::format` come from the [std prelude](/perception/enma-lang/addons/std-library.md) and are `std::`-qualified.

```c
print("no newline; ");
println("with newline");

std::string s = std::string("x = ") + std::to_string(42);
println(s);                                  // x = 42
println(std::format("x={} y={}", 1, 2));     // x=1 y=2
```

## Tracked heap

Every allocation here is counted by `heap_count()` and charged against the budgets, which is what makes leak checks and host limits work.

```c
void* malloc(int64 size)
void* strmalloc(int64 size)              // string-tagged variant
void* realloc(void* ptr, int64 size)     // returns the new block
void  free(void* ptr)
```

```c
void* p = malloc(8);
memset(p, 7, 8);
p = realloc(p, 32);            // contents preserved
free(p);
println(std::to_string(heap_count()));   // 0
```

## Raw memory

Free functions, not a namespace. Each picks its width from the CPU — AVX-512, AVX2, SSE, then a byte tail.

```c
void* memcpy(void* dst, const void* src, int64 n)    // returns dst
void* memmove(void* dst, const void* src, int64 n)   // handles overlap
void* memset(void* ptr, int64 value, int64 n)        // only the low byte of value is used
int64 memcmp(const void* a, const void* b, int64 n)  // 0 if equal
```

A null pointer or a non-positive `n` is a no-op rather than a fault.

```c
void* a = malloc(16);
void* b = malloc(16);

memset(a, 65, 16);
memcpy(b, a, 16);
println(std::to_string(memcmp(a, b, 16)));   // 0

free(a); free(b);
```

## Engine builtins

Registered by the engine itself — available with no addon at all.

```c
int64 heap_count()                        // live tracked allocations
int64 set_budget(int64 max_allocations)   // allocation past the cap fails the
int64 set_memory_budget(int64 max_bytes)  //   script, never the host
int64 register_event(int64 id, pointer callback) // pass the function by name
int64 fire_event(int64 id, int64 arg)     // returns how many handlers ran
int64 clear_events()
int64 assert(int64 condition, const char* message)   // fails the script on 0
int64 time_ms()
int64 cpu_features()                      // CPU feature bitmask
```

`cpu_features()` and its bit layout are described under [Builtin Instructions](/perception/enma-lang/language-guide/builtin-instructions.md#cpu-features).

```c
int64 on_tick(int64 arg) {
    println(std::string("tick ") + std::to_string(arg));
    return arg * 2;
}

register_event(7, on_tick);
fire_event(7, 21);      // prints "tick 21", returns 1 — one handler ran
clear_events();
fire_event(7, 21);      // returns 0 — nothing registered
```

The callback is passed by name, as `&name`, or as a lambda. Its return value does not travel back: `fire_event` reports the number of handlers that ran. A registration that does not denote a function of this module is refused when the event fires, as a catchable `runtime_error`.

```c
set_budget(1000);              // at most 1000 live tracked allocations
set_memory_budget(1048576);    // at most 1 MiB of tracked bytes
```
