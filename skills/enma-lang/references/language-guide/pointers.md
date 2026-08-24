> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/pointers.md).

# Pointers

Pointers are typed handles to heap objects. Allocate with `new`, free with `delete`. Access fields and methods with `->`. Use `.` on values and `->` on pointers as a style convention — the parser accepts either form for both, but mixing makes code harder to read and auto-formatting may rewrite to the canonical form.

Storing the address of a local somewhere it could outlive the local is a compile error — the escape analyzer rejects anything that could dangle. Pointer arithmetic on typed pointers is supported (see below).

A pointer does not convert to an integer on its own. Use `reinterpret_cast` in either direction — both are 8-byte slots, so the round trip is lossless on x64.

```c
Node* p = new Node(42);

int64  a = reinterpret_cast<int64>(p);      // hand the address to a native API
Node*  q = reinterpret_cast<Node*>(a);      // and back
println(std::to_string(q->val));            // 42

int64 bad = p;      // compile error: cannot implicitly convert pointer to int64
```

The one exception is a function reference: `&fn` yields an address that binds to `int64` with no cast, which is how a script hands a callback to `register_event`.

Casting an address around is not a way past the escape analyzer — allocation and lifetime rules still apply to whatever the address points at.

## Allocation & Deletion

```c
struct Node {
    int32 val;
    Node(int32 v) { val = v; }
    int32 get() { return val; }
}

Node* p = new Node(42);
println(std::to_string(p->val));     // 42
println(std::to_string(p->get()));   // 42; idiomatic for pointers

delete p;            // manual free
```

Placement `new (addr) T(args)` constructs into storage you already own, running the constructor without allocating.

`delete null` and double-delete are no-ops. `delete` on a non-pointer is a compile error.

## Null

```c
Node* p = null;
if (p == null) { /* ... */ }
p = new Node(1);
if (p != null) { delete p; }
```

`null` is assignable to any pointer type.

## Aliasing

Two pointers can refer to the same object. `delete` on a non-null pointer fires the dtor, frees the heap memory, and zeroes the source slot — so a subsequent `delete p;` is a no-op via the same null guard. Aliased pointers in OTHER slots still hold the freed address; accessing them is undefined behaviour.

```c
Node* a = new Node(1);
Node* b = a;          // same object
b->val = 99;
println(std::to_string(a->val));      // 99
delete a;             // b is now dangling, don't touch
```

## Copy via `*pt`

Dereferencing a pointer gives you a fresh independent copy of the pointee. Field-by-field memberwise copy (matching C++ default copy ctor semantics): primitive fields copy by value, inline struct fields recurse into their layout, pointer-typed fields stay shallow (alias).

```c
class Pt {
public:
    int32 x; int32 y;
    Pt() { x = 0; y = 0; }
    Pt(int32 a, int32 b) { x = a; y = b; }
}

Pt* a = new Pt(1, 2);
Pt b = *a;           // independent copy
b.x = 99;
println(std::to_string(a->x));       // still 1, unaffected by b.x = 99
```

For class types (reference types), `*pt` allocates a new heap instance — `b` is a heap object distinct from `*a`. For value-type structs you'd typically copy by assignment from a struct local rather than dereferencing a pointer; `*pt` is most useful for breaking aliasing on class types.

For deep copy of heap-managed sub-objects (string fields, array fields, nested class held by reference), define an explicit `clone()` method on the class — `*pt` is intentionally shallow to match C++ defaults.

## Move via `std::move(x)`

`std::move(x)` is a cast to an rvalue reference. On a raw pointer it hands back the same address and leaves `x` alone — a pointer copy either way, so both names refer to the same object and only one of them should delete it.

```c
Pt* a = new Pt(1, 2);
Pt* b = std::move(a);                // b and a hold the same address
println(std::to_string(b->x));       // 1
println(std::to_string(a->x));       // 1 — a is unchanged
delete b;                            // delete once, through one name
```

Distinct from `*pt`, which makes an independent copy.

Where the cast earns its keep is a value class: it selects the move constructor or move assignment instead of the copy. See [Structs and classes](/perception/enma-lang/language-guide/structs-and-classes.md#user-defined-copy-and-move-constructors).

On a value class the argument must be a simple variable name — `std::move(arr[i])` and `std::move(s.field)` are compile errors.

## Pointer Fields & Globals

Heap pointers can escape, they can be stored in globals, fields, or returned from functions. Stack structs cannot.

```c
struct Bag { Node* head; }

Node* g_root = null;

void install() {
    g_root = new Node(7);   // OK (heap)
}

Bag make_bag() {
    Bag b;
    b.head = new Node(1);   // OK (heap) pointer in a struct
    return b;
}
```

## Heap Arrays

Raw contiguous blocks — distinct from `std::vector<T>`, which manages its own storage.

```c
struct Point { int32 x; int32 y; Point() { x = 0; y = 0; } }

Point* ps = new Point[10];          // default-ctor each
ps[0].x = 1;                        // element access: ps[i] is the Point value
ps[9].y = 2;
delete[] ps;                        // dtor each, then free

Point* ys = new Point[4](3, 5);     // every element constructed with (3, 5)
delete[] ys;
```

Args after `[N]` are evaluated once and forwarded to each element's ctor. Wrong arity is a compile error.

For a growable sequence with automatic cleanup use `std::vector<T>` instead — `push_back`/`size`, RAII, no `delete[]`. See [Std Library](/perception/enma-lang/addons/std-library.md).

## Fixed-Size Arrays

C++ declarator syntax — the size follows the name: `T name[N]`. `N` is an integer literal or a `constexpr`. The array lives for its scope and is released automatically; there is no `delete`.

```c
int32 buf[3];                 // primitive: lives in the stack frame, zero heap
buf[0] = 4; buf[1] = 5; buf[2] = 6;
int32 sum = 0;
for (int32 i = 0; i < 3; i++) sum = sum + buf[i];   // 15

int32 vals[2] = { 10, 20 };   // brace init (primitive elements)

int64 xs[] = { 1, 2, 3 };     // bound deduced from the initializer
char  s[]  = "hi";            // sized from the literal — 3, including the NUL
```

A fixed array decays to `T*`: pass it to a function, take `&buf[0]` (or `&buf[i]`), and index/write through the pointer.

```c
int64 sum_n(int64* p, int64 n) {
    int64 s = 0;
    for (int64 i = 0; i < n; i++) s = s + p[i];
    return s;
}

int64 b[3];
b[0] = 10; b[1] = 20; b[2] = 30;
println(std::to_string(sum_n(b, 3)));     // 60 — decays to &b[0]

int64 total = 0;
for (int64 e : b) total = total + e;   // range-for
```

Struct/class elements work the same way. Each element is default-constructed at the declaration; `name[i].field` accesses it. At scope exit every element's destructor runs and the storage is freed — no `delete[]`.

```c
struct Pt { int64 x; int64 y; }

Pt pts[2];
pts[0].x = 3; pts[1].y = 4;
println(std::to_string(pts[0].x + pts[1].y));   // 7

class Node { public: int64 v; std::string tag; Node() { v = 0; tag = ""; } }
Node ns[4];                     // ctor runs for each; dtors + frees at scope end
```

A braced list initializes struct elements too, one nested list per element, and `T name[] = {…}` deduces the bound from it. A whole element assigns as a unit, which dispatches the element type's `operator=` when it declares one.

```c
struct Pt { int64 x; int64 y; }

Pt a[3] = { {1, 2}, {3, 4}, {5, 6} };
Pt src; src.x = 90; src.y = 91;
a[2] = src;                     // whole-element assignment
println(std::to_string(a[2].x));               // 90
```

The type-prefix spelling `T[N] name` is a compile error — write `T name[N]`. For a heap array you manage yourself, use `new T[N]` / `delete[]` (above).

Two-dimensional fixed arrays use the same declarator: `T m[N][M]`, indexed `m[i][j]` (row-major, both extents literal). Narrow element types are packed at their C width. Struct elements take a braced list of rows here as well; assigning a whole element (`m[i][j] = v`) is a compile error at two dimensions — write the fields.

A struct or class can hold a fixed array of structs as a **member** (`struct Bag { Pt items[2]; }`) or declare one at **global** scope (`Pt gps[3];`): each element is default-constructed up front, so `bag.items[i].field` / `gps[i].field` always indexes a live element. Both take a braced list, and `bag.items[i] = v` assigns a whole element through the element type's `operator=`. Members are destroyed with their containing object; globals live for the program.

Indexing carries no runtime checks — an out-of-bounds index is UB, exactly C++. A **provable constant** index outside the bounds is a compile error (`a[4]` on `int64 a[4]`, checked per-dimension for 2D); the one-past-the-end address `&a[N]` stays legal.

An array is not an object you can call members on — `a.size()` rejects; use the extent you declared, or a container. `auto b = a;` deduces a pointer, exactly as any other array-to-pointer decay does, striding by the element's own width.

```c
int32 a[4] = {10, 20, 30, 40};
auto b = a;                    // int32*
println(std::to_string(b[2]));                 // 30
```

## Pointer-to-Array `T (*p)[N]`

A pointer whose pointee is a whole array of `N` elements — the row-pointer type C++ gives you from `&arr` and from a decaying 2D array. Declared with the C++ declarator, primitive element types, locals only.

```c
int64 arr[3];
arr[1] = 42;
int64 (*p)[3] = &arr;      // &arr is the whole-array pointer (same address as &arr[0])
println(std::to_string((*p)[1]));           // 42

int64 m[2][3];
m[1][2] = 30;
int64 (*rows)[3] = m;       // a 2D array decays to its row pointer
rows[1][2] = 31;            // writes m[1][2]
++rows;                     // steps a whole row: 3 elements
println(std::to_string((*rows)[2]));        // 31
```

`p[j][i]` and `(*p)[i]` read and write elements at their packed width; `++p` / `p + n` stride `N * sizeof(element)`. A bare `p[j]` or `*p` is the row itself — it decays to the row's address and is **not assignable** (`p[j] = v` and `*p = v` are compile errors; arrays are not assignable in C++).

Initialization follows `[conv.ptr]` exactly: `&arr` of a 1D array with a **matching extent**, a decaying 2D array with a **matching row length**, another pointer of the same shape, or `null`. Everything else is a compile error — including a bare 1D `arr` (that decays to `T*`, a different type) and any plain `T*`.

Not yet supported (each is a clear compile error): struct element types, namespace-scope globals of this shape, function parameters, more than one extent, and `auto` deduction from `&arr`.

## Pointer Arithmetic

Typed pointers support arithmetic, scaled by the element. `p + n` / `p - n` advance by n elements, `++p` / `--p` step one, `p - q` gives the element distance, and `p[i]` is `*(p + i)`.

```c
int64* p = new int64[4];
p[0] = 10; p[1] = 20; p[2] = 30;

int64* q = p + 1;      // points at p[1]
int64 v = *q;          // 20
q++;                   // now p[2]
int64 d = q - p;       // 2 — the difference is an int64 element count
delete[] p;
```

Works on any typed pointer — a `new T[N]` block, a pointer field, or a pointer returned from a function. `void*` has no element type; cast it to a typed pointer before arithmetic or deref. Multi-level pointers (`T**`) and `alignof(T)` are also supported.

## Reference Parameters

`T& param` passes by reference. The callee mutates the caller's variable. Not a pointer; no `&` at the call site.

```c
void swap(int32& a, int32& b) {
    int32 t = a; a = b; b = t;
}

int32 x = 1;
int32 y = 2;
swap(x, y);      // x=2, y=1, no & needed
```

Struct references avoid copying:

```c
void add_in_place(vec3& dst, vec3 other) {
    dst.x = dst.x + other.x;
    dst.y = dst.y + other.y;
    dst.z = dst.z + other.z;
}
```

`out T` is the same ABI but signals the param is write-only and skips reading its initial value.

## Local References

`T& r = x;` binds `r` as an alias of `x` — reads and writes go straight to `x`. The referent must be a variable, field, or another reference (an initializer is required; an expression or temporary is rejected).

```c
int64 x = 5;
int64& r = x;
r = 9;            // x is now 9

const int64& cr = x;   // read-only alias
```

## Return by Reference

`T& f()` returns a reference to a variable, field, or member the caller can read or assign through.

```c
class Counter {
public:
    int64 v;
    Counter() { v = 0; }
    int64& slot() { return v; }   // hand out a reference to the field
}

Counter c;
c.slot() = 42;            // writes c.v
c.slot() += 8;            // and every compound form
int64 n = c.slot();       // reads c.v (auto-dereferenced)
int64& r = c.slot();      // bind it
```

A `T&`-returning call is an lvalue, so it takes plain assignment and every compound form — `+= -= *= /= %= &= |= ^= <<= >>=` — each reading and writing through the same reference.

## Pointer-to-Member Functions

A pointer-to-member-function holds a method address; the receiver is supplied at the call. Take one with `&Cls::method`; invoke with `(obj.*pmf)(args)` or `(ptr->*pmf)(args)`, which pass `obj`/`ptr` as the implicit receiver.

```c
class Foo {
public:
    int64 v;
    int64 dbl(int64 a) { return a * 2; }
}

typedef int64 (Foo::*Fn)(int64);

Foo f;
Fn pmf = &Foo::dbl;
int64 r = (f.*pmf)(21);          // 42 — f is the receiver

Foo* p = new Foo();
int64 s = (p->*pmf)(21);         // 42
delete p;
```

The call is checked exactly as `obj.method(args)` is: the bound method's arity window, each argument's conversion, and the parameter's own type. A defaulted trailing parameter supplies its default here too. The call returns what the method declares — a by-value struct or a `std::string` included, since it is the same function and the same ABI.

```c
class Fmt {
public:
    std::string tag(int64 n) { return "n" + std::to_string(n); }
    int64 add(int64 a, int64 b = 5) { return a + b; }
}

Fmt f;
auto tp = &Fmt::tag;
std::string s = (f.*tp)(7);      // "n7"

auto ap = &Fmt::add;
int64 n = (f.*ap)(10);           // 15 — the default applies
```

The value is a plain method address — store it in a `Fn` typedef, in `auto`, or in an `int64`, keep it in a struct field, or pass it to pick a method at runtime. Dispatch is non-virtual (the named method, not a derived override). Bind it where the method is visible (`auto p = &Cls::method;`): a slot holding a bare address carries no declared return type, and calling through one is refused where the receiver's class has members returning a class value by value — the two return conventions are not interchangeable and the address does not say which applies.

## Escape Errors

Stack structs can't escape. The compiler rejects anything that would dangle:

```c
struct Point { int32 x; int32 y; }
Point g_p;
Point* g_ptr = null;

int32 bad_return() {
    Point p;
    return &p.x;           // compile error: pointer to local
}

int32 bad_store() {
    Point p;
    g_ptr = &p;            // compile error: stack pointer stored globally
    return 0;
}

int32 ok_copy() {
    Point p;
    g_p.x = p.x;           // OK (value copy)
    return 0;
}

int32 ok_heap() {
    Point* p = new Point();
    g_ptr = p;              // OK (heap) pointer
    return 0;
}
```

## What's Rejected

| Operation                              | Result                                                        |
| -------------------------------------- | ------------------------------------------------------------- |
| `T* p = &local;` stored to global      | Compile error (escape)                                        |
| `return &local;`                       | Compile error (escape)                                        |
| `T x = new T();`                       | Compile error (pick stack or heap)                            |
| `delete x;` where `x` is value         | Compile error                                                 |
| Escaping closure captures stack struct | Compile error                                                 |
| `int64 n = p;` on a pointer            | Compile error — use `reinterpret_cast`                        |
| `T* p = some_int;`                     | Compile error — use `&variable`, `null` or `reinterpret_cast` |

## Runtime Traps

These are caught by the runtime fault handler and surface as `execute()` returning `false`:

* Null deref through a pointer
* An out-of-bounds access wild enough to hit unmapped memory — near-miss indexing is UB exactly like C++ (`p[i]`, `a[i]`, `v[i]` carry no bounds checks; the checked forms `v.at(i)` / `s.at(i)` throw instead)
* Use-after-free, where it can still be detected

(Double `delete` / `delete[]` and `delete null;` are safe no-ops on the tracked heap, not faults.)

Inside a native C++ function, dereferences are not trapped. Validate inputs on the native side. Use `heap_is_tracked(ptr)` if the pointer came from Enma's heap.

## Quick Reference

| Pattern                                | Meaning                                          |
| -------------------------------------- | ------------------------------------------------ |
| `T* p = new T(args);`                  | Heap allocate with ctor                          |
| `T* p = new T[N];`                     | Heap array, default-ctor each element            |
| `T* p = new T[N](ctor_args);`          | Heap array, every element ctor'd with ctor\_args |
| `delete p;`                            | Free single-object heap allocation               |
| `delete[] p;`                          | Free heap array (runs per-element dtor)          |
| `p->field`                             | Field access on a pointer                        |
| `p->method()`                          | Method call on a pointer                         |
| `T* q = p;`                            | Alias                                            |
| `T* p = null;` / `p == null`           | Null                                             |
| `void fn(T& x)`                        | Reference parameter (not a pointer)              |
| `void fn(out T x)`                     | Write-only reference parameter                   |
| `T& r = x;`                            | Local reference alias                            |
| `T& f()`                               | Return by reference                              |
| `&Cls::m`                              | Pointer-to-member-function                       |
| `(obj.*pmf)(args)` / `(p->*pmf)(args)` | Call through a pointer-to-member-function        |
| `reinterpret_cast<int64>(p)`           | Pointer to address, and back with `<T*>`         |
