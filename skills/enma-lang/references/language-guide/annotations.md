> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/annotations.md).

# Annotations

Metadata on declarations via `[[name]]` or `[[name(args)]]`. Built-in ones drive compiler behavior; custom ones are stored for host querying.

## Compiler Annotations

### `[[noopt]]`

Disable all optimization passes for this function.

```cpp
[[noopt]]
int32 debug_func(int32 x) {
    return x + 1;
}
```

### `[[noinline]]`

Prevent the optimizer from inlining this function into callers.

```cpp
[[noinline]]
int32 always_call(int32 x) { return x * 2; }
```

### `[[inline]]`

Hint that this function should be inlined where possible.

```cpp
[[inline]]
int32 fast_add(int32 a, int32 b) { return a + b; }
```

### `[[noescape]]`

Compile error if any allocation in the function escapes its scope (store to global, return, captured by escaping closure, etc.).

```cpp
Point g_sink;

[[noescape]]
int32 bad() {
    Point p;
    g_sink = p;             // compile error: p escapes via global store
    return p.x;
}
```

### `[[packed]]`

Remove alignment padding from struct fields. Fields use their natural size instead of being padded to 8 bytes.

```cpp
[[packed]]
struct Packet {
    uint8 type;     // offset 0, size 1
    uint8 flags;    // offset 1, size 1
    int32 payload;  // offset 2, size 4
}
// sizeof(Packet) == 6
```

Without `[[packed]]`, each field would be padded to 8 bytes, giving `sizeof == 24`.

### `[[align(N)]]`

Set custom alignment for a struct. `N` is the alignment in bytes. Composable with `[[packed]]`.

```cpp
[[align(16)]]
struct AlignedData {
    int32 x;
    int32 y;
}
// sizeof(AlignedData) == 16
```

Also works on individual fields — forces that field's offset to N-byte alignment AND bumps the struct's overall alignment to at least N:

```cpp
struct Lane {
    int64 hdr;
    [[align(16)]] vec3 pos;     // pos at offset 16, not 8
    int64 ftr;                  // immediately after pos
}
```

### `[[offset(N)]]`

Per-field annotation. Forces the field to land at exactly byte offset `N`. Accepts decimal or hex (`[[offset(0x10)]]`). Compile error if the requested offset would overlap a prior field.

Useful when a struct has to match an externally-defined binary layout with known field offsets:

```cpp
struct FileHeader {
    [[offset(0x00)]] uint32 magic;
    [[offset(0x10)]] int64  length;
    [[offset(0x80)]] int64  checksum;
}
// Field positions are fixed regardless of declaration order
```

Combined for a SIMD-aligned type — 3 float32 fields padded to a 16-byte slot:

```cpp
[[packed]] [[align(16)]] struct Vec3A {
    float32 x;
    float32 y;
    float32 z;
}
// size = 16; offsets x=0, y=4, z=8
```

### `[[reflect]]`

Generate runtime introspection functions for a struct. Creates four callable functions:

```cpp
[[reflect]]
struct Vec3 {
    float64 x;
    float64 y;
    float64 z;
}

int32 count = Vec3::__reflect_count();     // 3
string name = Vec3::__reflect_name(0);     // "x"
int32 type  = Vec3::__reflect_type(0);     // type id for float64
int32 off   = Vec3::__reflect_offset(1);   // 8
```

### `[[serialize]]`

Auto-generate serialization methods for a struct. Creates `to_bytes` and `from_bytes` functions.

```cpp
[[serialize]]
class Config {
    int32 width;
    int32 height;
    int32 flags;
    Config(int32 w, int32 h, int32 f) { width = w; height = h; flags = f; }
}

Config c = Config(1920, 1080, 7);
int64 buf = Config::to_bytes(c);

Config c2 = Config(0, 0, 0);
Config::from_bytes(c2, buf);
// c2.width == 1920, c2.height == 1080, c2.flags == 7
```

### `[[dll(...)]]`

Bind to a native shared library function. Requires `PERM_FFI` permission.

```cpp
[[dll("libc.so.6")]]
extern int64 getpid();

[[dll("mylib.dll", "my_export")]]
extern int32 custom_fn(int32 a, int32 b);
```

Resolved at load time via `dlopen`/`dlsym` on Linux and `LoadLibrary`/`GetProcAddress` on Windows.

### `[[no_vtable]]`

Class/struct. Suppresses the vtable a virtual method would otherwise add.

### `[[no_unique_address]]`

Data member only - it decides placement rather than hinting, so it is refused anywhere else. See [Structs & Classes](/perception/enma-lang/language-guide/structs-and-classes.md#no_unique_address).

## `[[align(N)]]` versus `alignas`

`[[align(N)]]` is Enma's own tool and **sets** an entity's alignment outright, including to something smaller - which is what matching an externally-defined layout needs. `alignas` is the C++ spelling and only ever strengthens.

`alignas` takes an integer literal of any radix (`alignas(16)`, `alignas(0x10)`) or a type (`alignas(Big)`, which asks for that type's alignment). A computed operand such as `alignas(2 * 8)` is an error - an alignment request is never quietly dropped.

It applies to a **type** and to a **data member**. A variable has a slot of one uniform width and a function has no storage, so `alignas` on either - or on a parameter - is refused for the same reason:

```c
struct alignas(32) Wide { int64 a; int64 b; }   // type
struct Holder { alignas(16) int64 hot; }        // data member

alignas(16) int64 x = 5;                        // error
int64 f(alignas(16) int64 a) { return a; }      // error
```

Each names what it is refused on: *"`alignas(16)` on `x` is not honoured here: an alignment specifier applies to a type or a data member. Put it on the type instead."* — and, for the parameter, *"`alignas` on a parameter is not honoured: …"*.

## Standard attributes

The pure-diagnostic standard attributes - `[[nodiscard]]`, `[[maybe_unused]]`, `[[deprecated]]`, `[[likely]]` and friends - parse and are ignored.

## Custom Annotations

A misspelled annotation in a position that **has** a known set is an error, not a silent drop — so a custom name goes only where there is no fixed set to misspell against: data members and globals. Functions, structs and classes each have their own closed set, and an unknown name there is rejected with the set listed.

Custom `[[name(args)]]` annotations are stored on the node and read back through the reflection API (`get_annotated_functions()` / `get_annotations()`).

```c
[[priority(5)]] int64 g_tick_budget = 16;

struct Weapon {
    [[category("combat")]] int64 damage;
}
```
