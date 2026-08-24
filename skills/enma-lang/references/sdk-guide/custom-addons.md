> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/custom-addons.md).

# Custom Addons

Walkthrough from a single function to a full type with operators.

{% stepper %}
{% step %}

#### A Single Function

**Native side:**

```cpp
#include "sdk.h"
using namespace enma;

int32_t square(int32_t x) {
    return x * x;
}

void register_addon_mymath(engine_t* e) {
    register_native(e, "int32 square(int32)", (void*)&square);
}
```

**Enma side:**

```cpp
int32 main() {
    println(std::to_string(square(5)));  // 25
    return 0;
}
```

**Host setup:**

```cpp
engine_t* e = create();
register_all_addons(e);
register_addon_mymath(e);
```

`register_native(e, sig, (void*)fn)` is type-safe and works at any arity. The signature string drives how args and returns are placed per Win64 ABI - ints / pointers / bools go in int regs, floats / doubles go in xmm regs, narrow-int returns get sign/zero-extended on the way back. Declare your C function with the exact types in the signature - no int64 bit-casting, no trampolines.

There's a template shortcut `register_typed<&fn>(e, sig)` that's equivalent - the `&fn` template parameter exists only to capture the function at compile time. Either form works; pick whichever reads better.
{% endstep %}

{% step %}

#### Multiple Functions

```cpp
double lerp(double a, double b, double t) {
    return a + (b - a) * t;
}

double distance(double x1, double y1, double x2, double y2) {
    double dx = x2 - x1, dy = y2 - y1;
    return std::sqrt(dx*dx + dy*dy);
}

void register_addon_mymath(engine_t* e) {
    register_native(e, "int32 square(int32)",                             (void*)&square);
    register_native(e, "float64 lerp(float64, float64, float64)",         (void*)&lerp);
    register_native(e, "float64 distance(float64, float64, float64, float64)", (void*)&distance);
}
```

{% endstep %}

{% step %}

#### A Custom Type

A `color_t` with fields, constructor, methods, and operators.

**The native struct:**

```cpp
struct color_t {
    uint8_t r, g, b, a;
};
```

**Factory + destructor + methods:**

```cpp
color_t* color_create(int32_t r, int32_t g, int32_t b, int32_t a) {
    auto* c = static_cast<color_t*>(heap_alloc(sizeof(color_t)));
    c->r = (uint8_t)r; c->g = (uint8_t)g; c->b = (uint8_t)b; c->a = (uint8_t)a;
    return c;
}

void color_destroy(color_t* c) {
    heap_free(c);
}

int64_t color_to_hex(color_t* c) {
    // uint32_t arithmetic — plain `c->r << 24` promotes to int and 255 << 24
    // is negative, which the int64 cast then sign-extends.
    return (int64_t)(((uint32_t)c->r << 24) | ((uint32_t)c->g << 16) |
                     ((uint32_t)c->b << 8)  |  (uint32_t)c->a);
}

color_t* color_blend(color_t* a, color_t* b) {
    return color_create((a->r + b->r) / 2, (a->g + b->g) / 2,
                        (a->b + b->b) / 2, (a->a + b->a) / 2);
}
```

**Operators:**

```cpp
bool color_eq(color_t* a, color_t* b) {
    return a->r == b->r && a->g == b->g && a->b == b->b && a->a == b->a;
}

color_t* color_add(color_t* a, color_t* b) {
    return color_create(std::min(255, a->r + b->r),
                        std::min(255, a->g + b->g),
                        std::min(255, a->b + b->b),
                        std::min(255, a->a + b->a));
}
```

**Registration:**

```cpp
void register_addon_color(engine_t* e) {
    type_builder(e, "color_t", type_id::t_struct)
        .field("r", offsetof(color_t, r), type_id::t_uint8)
        .field("g", offsetof(color_t, g), type_id::t_uint8)
        .field("b", offsetof(color_t, b), type_id::t_uint8)
        .field("a", offsetof(color_t, a), type_id::t_uint8)
        .factory((void*)&color_create,
                 { type_id::t_int32, type_id::t_int32, type_id::t_int32, type_id::t_int32 })
        .destructor((void*)&color_destroy)
        .method("int64 to_hex()",            (void*)&color_to_hex)
        .method("color_t blend(color_t)",    (void*)&color_blend)
        .bin_add((void*)&color_add)
        .bin_eq ((void*)&color_eq)
        .finish();
}
```

Pass per-arg enma type IDs to `.factory(fn, { ... })` so the Win64 ABI router knows which args to place in int regs vs xmm regs. For types without float params `.factory(fn, count)` still works - it defaults everything to int placement, fine for integer / pointer args.

**Enma side:**

```cpp
int32 main() {
    color_t red = color_t(255, 0, 0, 255);
    color_t blue = color_t(0, 0, 255, 255);
    color_t purple = red + blue;

    println(std::to_string(purple.r));  // 255
    println(std::to_string(purple.b));  // 255

    color_t blended = red.blend(blue);
    println(std::to_string(blended.r));  // 127
    println(std::to_string(blended.b));  // 127

    int64 hex = red.to_hex();
    println(std::to_string(hex));  // 4278190335

    if (red == red) println("equal");

    return 0;
}
```

{% endstep %}
{% endstepper %}

## Value-Type Types (Performance Opt-In)

For small POD-like types (vec3, color, quat, etc.) the default handle layout — every script value is an 8-byte pointer to a heap allocation — costs more than the actual work. Three opt-in `type_builder` hooks turn a typereg into a **value type**:

```cpp
type_builder(e, "color_t", type_id::t_int64)
    .value_type(sizeof(color_t))                  // (1) inline storage
    .factory_in_place((void*)&color_construct)    // (2) write-into-buffer ctor
    .inline_property("r", (void*)&color_get_r, (void*)&color_set_r,
                     (int32_t)offsetof(color_t, r), type_id::t_uint8)  // (3) inline accessor
    // ... regular factory / methods / operators stay the same ...
    .finish();
```

What you get:

* **No per-instance heap alloc** for non-escaping locals — the compiler stack-allocates the value.
* **Inline container storage** — `color_t[]` lays out N × `sizeof(color_t)` bytes contiguously, no handle indirection (arrays only; maps/sets still 8-byte handles).
* **Direct property reads/writes** — `c.r` compiles to a single `mov` instead of a native call when the property was registered with `inline_property`.

`factory_in_place` ctor signature: `int64_t fn(int64_t dst, args...)` — write into `dst`, return `dst`. The regular `.factory(...)` is still used for explicit `new color(args)`. See [Type Registration](/perception/enma-lang/sdk-guide/type-registration.md#value-type-registration) for the full walkthrough, constraints, and benchmark numbers.

## Source-Level Modules

For small POD types like `vec3` and `color` the typereg path still pays a per-operation heap\_alloc — every `a + b` invokes a native that returns a fresh `Vec3*`.

A faster pattern: ship the type as **Enma source** registered via `register_module`. The compiler treats it like any user struct — stack-allocates non-escaping locals and inlines field reads/writes. The trick is:

1. Write the type as a static `R"(...)"` literal in your addon `.cpp`. NEVER allocated, never copied — the engine keeps a `string_view` into the literal.
2. Call `register_module(engine, "modname", k_src)` in your addon registration function.
3. Scripts opt in with `import "modname";` at the top of the file.

```cpp
// em_addon_my_math.cpp
static const char* k_my_math_src = R"(
struct point2 {
    float64 x;
    float64 y;
    point2() { x = 0.0; y = 0.0; }
    point2(float64 a, float64 b) { x = a; y = b; }
    point2 operator+(point2 o) {
        // Local-and-return — NO heap alloc.
        point2 r; r.x = x + o.x; r.y = y + o.y; return r;
    }
    float64 dot(point2 o) { return x * o.x + y * o.y; }
}
)";

void register_addon_my_math(engine_t* e) {
    register_module(e, "my_math", k_my_math_src);
}
```

```enma
import "my_math";
int64 main() {
    point2 a = point2(1.0, 2.0);
    point2 b = point2(3.0, 4.0);
    point2 c = a + b;          // zero heap allocations
    return static_cast<int64>(c.x);    // 4
}
```

### Why this beats the typereg path

Built-in modules give the compiler everything it has on user structs: stack promotion, register promotion, escape analysis, RAII. For `vec3 c = a + b` the entire add path stays in stack locals — no `heap_alloc(24)`, no `heap_free(24)`, no native call. Across a 1000-iter loop of vec3 add operations, the typereg path performs 1000 heap\_alloc/free pairs; the source-module path performs zero.

### Critical: use the local-and-return pattern

Inside operator/method bodies, write the result as a local then return it:

```enma
// GOOD — local r is stack-promoted, return copies into the caller's slot.
vec3 operator+(vec3 o) {
    vec3 r; r.x = x + o.x; r.y = y + o.y; r.z = z + o.z;
    return r;
}

// BAD — inner `vec3(...)` ctor-temp gets heap-allocated then copied into
// the return slot then freed. Costs one heap_alloc per call.
vec3 operator+(vec3 o) {
    return vec3(x + o.x, y + o.y, z + o.z);
}
```

### Resolution order

When the preprocessor encounters `import "name";`:

1. **Built-in modules** registered via `register_module` (this wins first)
2. **Host import resolver** installed via `set_import_resolver` (callback returns malloc'd buffer)
3. **Filesystem search** of the engine's `module_paths` for `name.em` / `name.emb` — add entries with `add_include_path`

Built-in modules are emitted at top level — `import "math";` makes `vec3`, `quat`, `mat4` etc. directly accessible without a prefix. The addon author owns the namespace-collision rules across all built-in modules.

### Coexistence with the typereg

A type can be registered both via `type_builder` and as part of a source module simultaneously — when the script `import`s the module, the source struct definition takes precedence; otherwise the typereg is used as a fallback. Useful when the same type has to work with and without its source module present.

### Built in

* **`math`** (`em_addon_math.cpp` — registered with `register_addon_math`). Defines `vec2`, `vec3`, `vec4`, `quat`, `mat4` as value structs plus the scalar math natives (`sin`, `cos`, `sqrt`, `pow`, `floor`, `ceil`, `random_double`, ...). See [Math](/perception/enma-lang/addons/math.md) and [3D Math](/perception/enma-lang/addons/math-3d.md).

## Addon Registration Pattern

Follow this pattern for all addons:

```cpp
// header: addon_color.h
void register_addon_color(engine_t* e);

// source: addon_color.cpp
void register_addon_color(engine_t* e) {
    // register native functions
    // register types via type_builder
}

// host main.cpp
engine_t* e = create();
register_all_addons(e);
register_addon_color(e);
// ...
```

## Standalone Addon Model

Custom addons are `.cpp` source files compiled with your app. The only dependency is `sdk.h`. Your custom addon goes on the link line alongside the library.

```cpp
project/
  app.cpp
  sdk.h
  addons/addon_color.cpp
  addons/addon_color.h
```

```bash
# /MT
clang-cl /I. /MT app.cpp addons/addon_color.cpp windows/enma_x64static_mt.lib /Fe:app.exe

# /MD
clang-cl /I. /MD app.cpp addons/addon_color.cpp windows/enma_x64static_md.lib /Fe:app.exe
```

Statically linked. No plugin loading, no ABI concerns.

## Heap Allocation for Addons

Use these (not `new`/`malloc`) so Enma's stats and memory budget track the allocation:

```cpp
void* ptr = heap_alloc(128);
ptr = heap_realloc(ptr, 256);
heap_free(ptr);
```

| Function                  | Purpose                                                                         |
| ------------------------- | ------------------------------------------------------------------------------- |
| `heap_alloc(size)`        | Allocate from the tracked heap                                                  |
| `heap_realloc(ptr, size)` | Resize tracked allocation                                                       |
| `heap_free(ptr)`          | Free tracked allocation                                                         |
| `heap_is_tracked(ptr)`    | True if `ptr` came from Enma's heap; check before freeing pool/literal pointers |

Frees happen on explicit `heap_free`, scope-dtor, or cleanup-stack unwind (throw / native fault).

### Deterministic Scope-Drop

Types with a registered destructor get automatic scope-exit cleanup on script locals. Add `.pure_methods()` so the compiler can emit drop calls safely.

```cpp
void my_type_drop(my_type_t* obj) {
    if (!obj) return;
    if (obj->data) heap_free(obj->data);
    heap_free(obj);
}

type_builder(e, "my_type", type_id::t_struct)
    .factory((void*)&create_my_type, 0)
    .destructor((void*)&my_type_drop)
    .pure_methods()
    .method("...", (void*)&some_method);
```

See [Type Registration](/perception/enma-lang/sdk-guide/type-registration.md) for the full lifecycle flow.

### Copy Hook (for ref-counted / COW types)

By default, `T b = a;` shares the int64 handle, both copies point at the same heap object. For types that need copy semantics (ref-counted `shared_ptr`-style, copy-on-write, arena-backed), register a copy hook:

```cpp
int64_t rc_copy(int64_t h) {
    if (!h) return 0;
    auto* o = reinterpret_cast<refcount_obj*>(h);
    ++o->count;
    return h;   // return the same handle, caller and callee share ownership
}

type_builder(e, "rc", type_id::t_int64)
    .factory((void*)&rc_create, 1)
    .copy((void*)&rc_copy)
    .destructor((void*)&rc_dtor);
```

The compiler emits a call to `copy_fn(src)` at `T b = a;` when the source is an identifier of the same type. Plain assignment (`b = a;`) and by-value argument passing still share the handle directly, use an explicit call if you need copy semantics there.

### Serialization Hooks

Let walkers (JSON writers, debuggers, pretty-printers) dispatch through reflection instead of hardcoding per-type logic:

```cpp
int64_t date_serialize(int64_t h) {
    auto* d = reinterpret_cast<date_t*>(h);
    char buf[32];
    int n = std::snprintf(buf, sizeof(buf), "%04d-%02d-%02d", d->y, d->m, d->d);
    auto* out = (char*)std::malloc(n + 1);
    std::memcpy(out, buf, n + 1);
    return (int64_t)out;
}

int64_t date_deserialize(int64_t str_ptr) {
    auto* s = reinterpret_cast<const char*>(str_ptr);
    int y, m, d;
    if (std::sscanf(s, "%d-%d-%d", &y, &m, &d) != 3) return 0;
    auto* r = (date_t*)std::malloc(sizeof(date_t));
    r->y = y; r->m = m; r->d = d;
    return (int64_t)r;
}

type_builder(e, "date", type_id::t_int64)
    .factory(...)
    .destructor(...)
    .serialize((void*)&date_serialize)
    .deserialize((void*)&date_deserialize);
```

Generic walker code reads the hook via reflection:

```cpp
auto* fn = type_reg_serialize(find_type_reg(e, some_type_id));
if (fn) {
    int64_t str = ((int64_t(*)(int64_t))fn)(value);
    // ... consume str, then std::free(str) ...
}
```

Return a heap-allocated `char*`; caller decides whether to `free` or let it flow into a larger buffer. Use `std::malloc` for walker integration (can run outside `execute()`) or `heap_alloc` for script-visible buffers during execution.

### Interfaces and reflection

A registered type joins the interface system with `.as_interface()` and `.implements(iface)`, and the engine injects the `type_id` an interface dispatch needs. The full mechanism, including `interface_method_fn`, is in [Type Registration](/perception/enma-lang/sdk-guide/type-registration.md).

The `type_reg_*` accessors let an addon walk another addon's registered type without a compile-time dependency on it — useful for generic containers, serializers and debuggers. They are all null-safe. See [Introspection](/perception/enma-lang/sdk-guide/introspection.md) for the accessor set and the dispatch pattern.

## Per-Context Helpers

Addon natives called from within `execute()`/`call()` can reach per-context state without taking the engine/context as an argument:

```cpp
engine_t*  e    = active_engine();        // the engine driving this call
context_t* ctx  = active_context();       // the context driving this call
uint64_t   bits = random_u64();           // per-context mt19937_64
double     r    = random_double();        // [0, 1)
int64_t    n    = random_int_range(0, 100);
random_seed(42);                          // seeds per-context rng
```

All are thread-safe under concurrent engines because they read the per-thread TLS slot set by `execute()`. Outside `execute()` they return `nullptr` / 0.

## Invoking Script Closures from Background Threads

When a host needs to tick a script-side callback on its own thread (e.g., a periodic task, a routine that draws every frame, a worker pool) the invocation has to run inside an `execution_scope`. The scope sets up the per-thread state that Enma's compiled code and built-in natives rely on. Without it, the first native that touches TLS (heap\_alloc, string concat, `std::any` boxing, etc.) dereferences nullptr and takes down the host.

Call the script function by name with `call`:

```cpp
// script side:  int64 on_tick(int64 arg) { ... }

struct tick_data_t {
    context_t*  ctx;
    const char* fn;          // "on_tick"
};

void worker_thread(tick_data_t* td) {
    execution_scope scope(td->ctx);      // per-thread state for the whole tick loop
    while (!thread_should_stop(td)) {
        int64_t args[] = { 0 };
        if (enma::call(td->ctx, td->fn, args, 1)) {
            int64_t r = enma::return_value(td->ctx);
            (void)r;
        }
        sleep_ms(1);
    }
    // scope dtor restores prev state on the way out
}
```

`call` takes its arguments as an `int64_t` array and returns `false` if the function is missing or the run failed. Read the result with `return_value` / `return_float` / `return_string`.

`execution_scope` is RAII and nestable - the destructor restores whatever state was active before. Safe to wrap a single call or an entire tick loop; the setup cost is one-time so scoping across the whole loop is usually what you want.

## Weak References. `alive_token(ptr)`

Returns a shared `uint64_t*` tied to this allocation's lifetime. Non-zero while the allocation is live; flipped to 0 when `heap_free(ptr)` runs. Repeat calls for the same pointer return the same token.

```cpp
void* p = heap_alloc(sizeof(MyObj));
uint64_t* tok = alive_token(p);           // tok points at a 1 (alive)
// ... later ...
if (*tok) { /* safe to use p */ }
heap_free(p);                              // *tok flips to 0
```

Tokens are never freed - weak-ref wrappers can hold the token pointer beyond the original allocation.

`heap_alloc` and `heap_free` read the per-thread heap, so on a host thread that is not already inside `execute()` / `call()` they need an `execution_scope` first — without one the allocation dereferences a null TLS slot and takes down the process. Inside a native called from script you are already in scope.

```cpp
execution_scope scope(ctx);
void* p = heap_alloc(sizeof(MyObj));
```

## Event API

Addons can raise and observe named events; the entry points are listed in [API Reference](/perception/enma-lang/sdk-guide/api-reference.md) and the script-side surface in [Core](/perception/enma-lang/addons/core.md).
