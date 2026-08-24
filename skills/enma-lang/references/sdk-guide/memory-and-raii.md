> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/memory-and-raii.md).

# Memory & RAII

Deterministic memory model. No tracing GC. Every allocation's lifetime is bound to a lexical scope or an explicit `delete`.

| Situation                                 | Where it lives                             | When it's freed                                            |
| ----------------------------------------- | ------------------------------------------ | ---------------------------------------------------------- |
| `T x;` or `T x = T(args)`                 | Stack (default) or heap (when it escapes)  | At scope exit. Destructor runs, memory reclaimed           |
| `T x = f()` (struct-returning fn)         | Caller-supplied stack buffer (return slot) | At scope exit, dtor + free                                 |
| `T x = a + b` (operator returning struct) | Same as above                              | At scope exit                                              |
| `T* x = new T(...)`                       | Heap                                       | Manual: `delete x;` (no auto-drop for `T*`)                |
| `T* xs = new T[N]`                        | Heap (struct or primitive)                 | Manual: `delete[] xs;` (per-element ctor/dtor for structs) |
| Array / string / map                      | Heap (via addon `.factory`)                | `.destructor` runs at scope exit                           |

Heap allocations are tracked, so `heap_get_stats` and the memory budget account for every one. There is no collector — nothing reclaims memory in the background; every free is deterministic (scope exit, `delete`, container drops, unwind).

## Stack structs by default

Non-escaping struct locals are stack-allocated as contiguous fields. No heap trip:

```cpp
struct Point { int32 x; int32 y; }

int32 compute() {
    Point p;             // lives on the stack, no heap alloc
    p.x = 10;
    p.y = 20;
    return p.x + p.y;
}
```

Confirm with `heap_get_stats()`:

```cpp
auto before = heap_get_stats(mod);
execute(ctx, "compute");
auto after = heap_get_stats(mod);
// after.alloc_count == before.alloc_count
```

## Struct return via return-slot

Struct returns write directly into the caller's return slot: the caller pre-allocates the stack buffer, passes its address, and the callee field-copies in. No heap trip, no intermediate temp.

```cpp
struct Box { int32 v; }

Box make_box(int32 v) {
    Box b;
    b.v = v;
    return b;               // fields copied into caller's buffer
}

int32 main() {
    Box r = make_box(42);   // r is the buffer
    return r.v;
}                            // r's destructor runs here
```

The returned local in the callee is marked as moved; its destructor does **not** run in the callee (the bytes now belong to the caller).

## Destructors run at scope exit

The user `::~T()` destructor runs at scope exit for every struct local. Normal flow, exception, or native fault.

```cpp
struct Counter {
    int32 id;
    ~Counter() { g_dtor_count = g_dtor_count + 1; }
}

Counter make_counter(int32 v) {
    Counter c;
    c.id = v;
    return c;
}

int32 main() {
    {
        Counter a = make_counter(1);
        Counter b = make_counter(2);
    }                        // a's dtor fires, then b's dtor fires
    return g_dtor_count;     // 2
}
```

## Exceptions don't heap

`throw T(args)` copies the value into a per-thread exception buffer. No heap allocation per throw. The catch handler reads from the same buffer.

```cpp
struct Error { int32 code; }

int32 main() {
    try {
        throw Error(42);       // bytes copied into exception buffer
    } catch (Error e) {
        return e.code;         // e reads from the same buffer
    }
}
```

Struct exceptions larger than the buffer are a compile error: *"thrown struct 'X' exceeds exception buffer size"*. Primitive throws (`throw 99`) use a direct-store path. `throw new T(args)` heap-allocates; prefer `throw T(args)`.

## Scope-drop through exceptions

When a script throws, destructors for all live scope-drop objects run before the exception reaches the catch block:

```cpp
int32 main() {
    int32 caught = 0;
    try {
        std::vector<int32> a;
        a.push_back(1);
        throw 42;           // a is dropped here, then control jumps to catch
    } catch (int32 e) {
        caught = e;
    }
    return caught;
}
```

## Native fault unwind

Null deref, OOB array access, div-by-zero, all go through the same unwind path. The runtime fault handler walks the cleanup stack back to the `execute()` call, running every registered destructor.

```cpp
int32 main() {
    std::vector<int32> a;
    a.push_back(1);
    int32 divisor = 0;
    int32 x = 100 / divisor;   // fault
    return x;
}
```

After `execute()` returns `false`, `a` has been freed.

## Escape is a compile error

Three patterns are rejected at compile time:

```cpp
struct Point { int32 x; int32 y; }
Point g_p;

int32 escape_via_global() {
    Point p;
    p.x = 1;
    g_p = p;                 // error: stack struct escapes its scope
    return 0;
}
```

```cpp
struct Thing { int32 v; }

Thing* return_stack_ptr() {
    Thing t;
    return &t;               // error: address-of a local crossing the return
}
```

```cpp
delegate int32 Getter();
struct Point { int32 x; int32 y; }

Getter escaping_closure() {
    Point p;
    p.x = 7;
    return () => p.x;        // error: escaping closure captures stack struct
}
```

Fix by making the struct heap-allocated with `new T(...)`, or by copying values (not the struct pointer) into the escape target.

## Move semantics on return

The compiler skips the destructor on a named local that's returned through the return slot:

```cpp
Box b;
b.v = 42;
return b;                // b's dtor does NOT run here
                         // (move, the caller now owns the bytes)
```

If the return expression is a temporary (`return T(args);` or `return new T();`), the temporary's destructor is also elided.

## Annotations

## For addon authors

1. Register `.destructor()` - runs at every scope exit of a local of your type.
2. Mark `.pure_methods()` for container-like types (methods don't retain receiver).
3. Mark `.pure_args()` for value/math types (methods don't retain any arg).

See [Type Registration](/perception/enma-lang/sdk-guide/type-registration.md) and [Custom Addons](/perception/enma-lang/sdk-guide/custom-addons.md).

## Measured impact

Heavy-loop stress (1000 arrays in a loop): 2000 alloc, 2000 freed, 0 live. See [README](/perception/enma-lang/readme.md) for bench numbers.
