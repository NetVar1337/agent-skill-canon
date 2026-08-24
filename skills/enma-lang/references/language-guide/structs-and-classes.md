> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/structs-and-classes.md).

# Structs & Classes

## Structs

Structs are value types, stack-allocated by default. Only explicit `new T(...)` goes to the heap, and heap pointers are manually managed with `delete`.

A **forward declaration** introduces the name without the body:

```cpp
struct Node;                     // incomplete: the name exists
struct Edge { Node* to; }        // a pointer to it is enough
struct Node { Edge* first; }     // the definition completes it
```

A **member type alias** is a member of its class, named `Class::alias` from outside and unqualified inside:

```cpp
struct Buffer {
    using value = int64;
    value at(int64 i) { value v = 0; return v; }   // unqualified here
}

Buffer::value x = 5;                               // qualified out here
```

| Syntax                                     | Storage                             | Cleanup                                             |
| ------------------------------------------ | ----------------------------------- | --------------------------------------------------- |
| `T x;`                                     | stack                               | auto `::~T()` at scope exit                         |
| `T x = T(args);`                           | stack                               | auto `::~T()` at scope exit                         |
| `T x = f();` (struct-returning call)       | caller's stack buffer (return slot) | auto `::~T()` at scope exit                         |
| `T x = a + b;` (operator returning struct) | stack                               | auto `::~T()` at scope exit                         |
| `T* p = new T(args);`                      | heap                                | **manual**. `delete p;`                             |
| `std::vector<T> v;`                        | heap-managed value container        | automatic (RAII) — elements' `~T()` then the buffer |
| `T* p = new T[N];`                         | heap (contiguous C-style array)     | **manual**. `delete[] p;`                           |

`T x = new T();` is a compile error, pick stack (`T x;`) or heap (`T* p = new T();`). `delete` on a non-pointer is also a compile error. `delete null;` is a no-op. Mixing `delete` and `delete[]` is undefined behavior (match the form you allocated with).

### Contiguous heap arrays

`new T[N]` default-constructs every element and `delete[]` destroys every element before freeing the block. Args after the size (`new T[N](args)`) are evaluated once and forwarded to each element's constructor. If `T` has neither a constructor nor a destructor the bytes are zero-initialized and freed.

See [Pointers](/perception/enma-lang/language-guide/pointers.md#heap-arrays) for the declaration form.

### Containers copy their elements

`std::vector<T>` and the other value containers store copies: `push_back` copies its argument in (one copy-constructor fire per push, exactly C++), so passing a stack struct is fine — the container owns its own element.

```c
std::vector<Point> arr;
{
    Point p;
    p.x = 10;
    arr.push_back(p);   // copies p in
    p.x = 77;           // the stored element still reads 10
}                       // p dies; arr[0] is unaffected
arr[0].x = 11;          // T& operator[] — element writes go through
```

Pointer elements (`std::vector<Point*>`) store the pointer itself; you own the pointees (`new` / `delete`) as usual.

### Basic Struct

```c
struct Vec2 {
    float64 x;
    float64 y;
}

Vec2 v;
v.x = 1.0;
v.y = 2.0;
```

### Constructors & Methods

```c
struct Vec2 {
    float64 x;
    float64 y;
    Vec2(float64 a, float64 b) { x = a; y = b; }
    float64 length_sq() { return x*x + y*y; }
}

Vec2 v = Vec2(3.0, 4.0);        // stack
float64 lsq = v.length_sq();    // 25.0
```

### Implicit Initialization

Without an explicit constructor, positional args map to fields in declaration order:

```c
struct Color {
    int32 r;
    int32 g;
    int32 b;
    int32 sum() { return r + g + b; }
}

Color c = Color(255, 128, 0);  // r=255, g=128, b=0
```

### Designated initializers

Struct literals accept both positional and designated forms. Unlisted fields are zero-initialized.

```c
struct Point { int32 x; int32 y; }
struct Triple { int32 a; int32 b; int32 c; }

Point p = {3, 4};                    // positional: p.x=3, p.y=4
Point q = {.x = 10, .y = 20};        // designated
Point r = {.x = 1, .y = 2};          // must follow declaration order

Triple t = {.b = 42};                // a=0, b=42, c=0

Point bad = {.x = 1, .z = 99};       // compile error: no field z
```

Designators must appear in declaration order; one that runs out of order, or names no member, is an error. This is aggregate initialization — it does not call a constructor. For a type that needs one, use the function-call form: `Point p = Point(3, 4);`.

### Delegating constructors

`T(args) : T(other) { … }` forwards to another constructor of the same type, which runs the full member initialization before the delegating body.

```c
struct Rect {
    int32 w; int32 h;
    Rect(int32 a, int32 b) { w = a; h = b; }
    Rect(int32 side) : Rect(side, side) { }    // delegates
}
```

### A throw escaping a constructor

Every subobject that was fully constructed is destroyed in reverse order and the object's storage is released — but `~T()` itself does **not** run, because the object's lifetime never began. A throw from a member's own constructor leaves the members before it destroyed and those after it never built.

### Reference data members

A reference member names storage somebody else owns. It is bound in the mem-init list and nowhere else; reads and writes reach the referent, and the referent is not destroyed with the object naming it.

```c
struct H {
    int64& r;
    H(int64& x) : r(x) { }
}
```

The three ways of leaving one without a target are all errors: a constructor that does not bind it, default construction (there is no implicit default constructor), and copy-assignment (a reference cannot be reseated).

### Destructors

```c
struct Resource {
    int64 handle;
    Resource(int64 h) { handle = h; }
    ~Resource() { close(handle); }
}
```

Destructors run deterministically at scope exit: normal control flow, exception unwind, and native fault unwind (div-by-zero, OOB, null deref). The `}` is the trigger.

### Chained construction & destruction

Member fields with constructors auto-fire when their parent constructs, in **declaration order**. Destructors fire in **reverse declaration order** between the user dtor body and the base-class dtor chain. Mirrors C++ exactly.

```c
struct Inner {
    int64 v;
    Inner() { v = 42; }              // fires automatically
    ~Inner() { /* fires automatically too */ }
}

struct Outer {
    Inner a;
    Inner b;
    Outer() { /* a.ctor + b.ctor have already fired */ }
    ~Outer() {
        // 1. user dtor body runs first
        // 2. b's ~Inner fires (reverse declaration order)
        // 3. a's ~Inner fires
        // 4. base class dtor chain (if Outer has bases)
    }
}

{ Outer o; }
// Output: a.ctor → b.ctor → Outer.ctor → ~Outer body → b.~Inner → a.~Inner
```

Container fields (`std::string`, `std::vector<T>`, `std::map<K,V>`, and the rest of the `std` containers) are ordinary struct fields: they construct empty with the parent and clean up on destruction. Class-V elements have their `~T()` called per element before the container's storage is freed:

```c
class Player {
    std::string name;
    ~Player() { /* fires automatically per element */ }
}

class Roster {
    std::vector<Player> active;
    std::map<std::string, Player> by_name;
    Roster() {
        active.push_back(Player());
        by_name["p1"] = Player();
    }
}

{ Roster r; }
// Each Player gets its ~Player called as the container is walked,
// then the container itself is freed, then ~Roster body runs.
```

Constructor init lists (`Outer() : a(arg) {}`) override the default no-arg auto-init for that specific field — the explicit `Inner(arg)` runs instead, no double-construction, no leak.

**Init order follows declaration order, not init-list source order.** Just like C++. Given

```c
struct A { A() { mark(1); } }
struct B { B(int64 x) { mark(2); } }
struct C { C() { mark(3); } }

struct Owner {
    A a;
    B b;
    C c;
    Owner() : c(), b(0) { mark(4); }   // init-list mentions c first, then b
}
```

the init order is `A` → `B(0)` → `C` → body → final log = `1234`. The init-list source order (`c(), b(0)`) controls *what* runs for each field but not *when* — declaration order wins. Fields not mentioned in the init list get their default no-arg ctor at their slot in the order. Destructors fire in reverse declaration order.

### Address escape is a compile error

A stack struct's **value** copies freely; its **address** cannot escape the scope that owns it.

```c
struct Point { int32 x; int32 y; }
Point g_last;
Point* g_ptr;

int32 main() {
    Point p;
    g_last = p;          // OK: whole-object value copy, exactly C++
    g_ptr = &p;          // error: stack struct escapes its scope
    return 0;
}
```

Heap-allocate when the object itself must outlive the scope. The full set of escape rules — globals, fields, returns and closure captures — is in [Pointers](/perception/enma-lang/language-guide/pointers.md#escape-errors).

### Passing & Copying

Struct and class values follow C++ value semantics: pass-by-value is a copy, assignment is a copy, return is a copy. Mutating a parameter's fields inside the callee does NOT touch the caller.

```c
struct Box { int64 v; Box(int64 x) { v = x; } }

void mutate(Box b) {
    b.v = 999;             // local to b; caller's Box stays at 7
}

int64 main() {
    Box a = Box(7);
    mutate(a);
    return a.v;            // 7
}
```

```c
Box a = Box(7);
Box c = a;                 // independent copy
c.v = 99;                  // a.v still 7
```

When you want reference semantics, use a pointer:

```c
void mutate_ref(Box* b) {
    b->v = 999;            // dereference modifies the caller's Box
}

Box a = Box(7);
mutate_ref(&a);
// a.v is now 999
```

Same rules inside operator overloads. `operator+(T other)` receives a copy of `other`; reading or modifying its fields has no side effect on the caller.

For deep-copying with non-trivial fields, you can write an explicit `clone()`:

```c
struct Vec3 {
    float64 x; float64 y; float64 z;
    Vec3(float64 a, float64 b, float64 c) { x = a; y = b; z = c; }
    Vec3 clone() { return Vec3(x, y, z); }   // returns by-value (copy)
}
```

### Const Methods

Trailing `const` marks a method as non-mutating. Inside a const method, `this` is read-only.

```c
struct Counter {
    int32 n;
    int32 inc() { this->n = this->n + 1; return this->n; }
    int32 get() const { return this->n; }
}

int32 observe(const Counter c) {
    int32 v = c.get();      // OK
    c.inc();                // compile error: non-const method on const receiver
    return v;
}
```

A `const T` parameter rejects field assignment through it. Non-const methods can't be called on const receivers.

### Const Data Members

`const` on a data member makes it writable only during initialization — a default member initializer or a constructor init-list entry. Every later write is a compile error: direct, compound, `++`/`--`, through any access chain, through pointers, and **in constructor bodies** (use `: c(v)`, exactly as in C++).

```c
struct Id {
    const int64 id;
    int64 uses = 0;
    Id(int64 v) : id(v) {}      // init-list initializes the const member
}

Id a = Id(7);
a.uses = 1;                     // OK
a.id = 9;                       // compile error: assignment to const member
```

A const member also **implicitly deletes the copy assignment operator**: `a = b` between two `Id` values is a compile error unless the struct declares its own `operator=` (which may write the non-const members only). Copy *construction* stays legal.

Members of a `const` object are const themselves, and `static const` members reject writes through both `C::k` and bare in-method spellings. Constness does not travel through pointer members: with `const S s`, a write through `s.p->field` is legal — the member pointer is const, what it points at is not.

### Operator overloading

Structs and classes overload the full operator set — arithmetic, comparisons, `<=>`, subscript, call, conversion and the rest. See [Operators](/perception/enma-lang/language-guide/operators.md).

### User-defined copy and move constructors

`T(const T& other)` overrides the default field-by-field copy. `T(T&& other)` is the move ctor — fires on `T c = std::move(a);`.

```c
struct Counter {
    int64 v;
    Counter(int64 x)             { v = x; }
    Counter(const Counter& other) { v = other.v; }            // copy
    Counter(Counter&& other)      { v = other.v; }            // move
}

Counter a = Counter(7);
Counter b = a;               // copy ctor
Counter c = std::move(a);    // move ctor
```

`std::move(a)` is a cast. It picks the move ctor, and does nothing else — it does not touch `a`, and `a` is still a live object afterwards: assignable, usable, and destroyed at scope exit like any other local.

What `a` holds after the move is whatever your move ctor left it holding. A ctor that steals must empty the source; one that only reads it leaves the source owning its own copy:

```c
struct Buf {
    std::string s;
    Buf() {}
    Buf(Buf&& o) { s = o.s; o.s = ""; }    // steals: o ends empty
}
```

Note `s = o.s` on its own is a **copy** — `o` is a named rvalue reference, so `o.s` is an lvalue. Clearing `o.s` is what makes it a move.

If only a copy ctor is declared, `std::move(a)` falls back to it. If neither is declared, `T c = a;` does default memberwise copy. Move and copy ctors coexist with regular 1-arg ctors like `T(int)` — they resolve as distinct overloads so there's no conflict.

### Explicit constructors and conversions

`explicit` on a constructor or conversion operator blocks implicit conversion through it — only direct construction (`T(x)`) and `static_cast<T>(x)` invoke it.

Conditional `explicit(cond)` takes a constant expression evaluated after constexpr folding and template substitution, so `explicit(sizeof(T) > 4)` answers per instantiation. A condition that cannot be evaluated is an error, not a silent "explicit".

```c
struct Meters {
    float64 v;
    explicit Meters(float64 m) { v = m; }
    explicit operator float64() { return v; }
}

Meters d = Meters(5.0);          // OK — direct construction
Meters e = 5.0;                  // error — implicit conversion through explicit ctor
float64 f = static_cast<float64>(d);    // OK — explicit cast
float64 g = d;                   // error — implicit conversion through explicit operator
```

The same applies to by-value arguments and return values: an implicit conversion through an `explicit` member is a compile error. Without `explicit`, all of these conversions fire implicitly (see the conversion-operator row above).

**Not overloadable:** `&&`, `\|\|` (short-circuit; can't preserve evaluation order), `,` (footgun).

### Static data members and methods

A `static` data member has one storage slot shared by every instance. It can be initialized in-class, or defined out of line; a class template's static gets one definition that initializes every instantiation, each with its own storage.

```c
struct S {
    static int64 count = 0;      // in-class
    static int64 make() { return 1; }
}

int64 S::other = 5;                                  // out-of-line
template<typename T> int64 Box<T>::tag = 7;          // per instantiation
```

Static methods take no receiver - call them `S::make()`.

### friend

`friend class B;` grants `B` access to this class's members. `friend Ret f(Args);` grants it **by signature** - to that one function, not to every overload of the name. Two spellings of one parameter type (qualified, aliased) are one signature.

```c
class Acct {
    int64 bal;
public:
    Acct(int64 b) { bal = b; }
    friend int64 audit(Acct a, int64 scale);   // that signature only
}

int64 audit(Acct a, int64 scale) { return a.bal * scale; }
int64 audit(Acct a) { return a.bal; }          // not a friend - `bal` is private here
```

### union and anonymous unions

```c
union Word {
    int32 i;
    float32 f;
}

struct Packet {
    int32 kind;
    union { int64 a; float64 b; };   // anonymous: members name Packet directly
}
```

Bitfields may appear inside a union.

### \[\[no\_unique\_address]]

On a data member whose type is an **empty** class - no data members, no vptr - stored inline: the member takes zero size and the next member takes its address.

```c
struct Empty { }
struct S {
    [[no_unique_address]] Empty e;
    int64 v;                          // may sit at offset 0
}
```

Two members of the same type never share an address (the second is pushed one byte past); members of different empty types may. Construction and destruction are unaffected.

An empty **base** class costs nothing unconditionally - a base does not have to ask. Two base subobjects of the same type still get distinct addresses; bases of different empty types may share one. A base with a vptr is not empty, since a vptr is storage.

### alignas

`alignas(N)` and `alignas(T)` go before or after the class-key, and on a data member; a type operand resolves to that type's alignment. Several specifiers combine to the strictest, and `alignas` never weakens what a type already requires.

A type and a data member are the entities it applies to - on a variable, a parameter or a function it is refused rather than dropped. See [Annotations](/perception/enma-lang/language-guide/annotations.md#align-n-versus-alignas).

`[[align(N)]]` is Enma's own tool and **sets** the alignment outright, even to something smaller - see [Annotations](/perception/enma-lang/language-guide/annotations.md).

### Bitfields

```c
struct Flags {
    int32 active : 1;
    int32 priority : 4;
    int32 state : 3;
}
```

Multiple values packed into a single 64-bit word.

### Layout control

`[[packed]]` removes padding, `[[align(N)]]` sets a struct's or field's alignment outright, and `[[offset(N)]]` pins a field to an exact byte offset — the combination for matching an externally-defined binary layout. `alignas` is the C++ spelling and only ever strengthens.

See [Annotations](/perception/enma-lang/language-guide/annotations.md) for each one's rules and examples.

## Classes and inheritance

`class` members are private by default and `struct` members public; both support single and multiple inheritance, virtual dispatch and interfaces. See [Inheritance & Access](/perception/enma-lang/language-guide/inheritance.md).

## Out-of-line member definitions

Declare a member in the class body, define it at namespace scope with the qualified spelling. Defining a member the class never declared is a compile error.

```c
struct Rect {
    int32 x;
    int32 y;
    int32 w;
    int32 h;
    int32 area();
    int32 perimeter();
}

int32 Rect::area() { return w * h; }
int32 Rect::perimeter() { return 2 * (w + h); }
```

## Properties

Getter/setter syntax for computed values. Both blocks are optional; supply `get`, `set`, or both.

```c
struct Rect {
    int32 w;
    int32 h;
    property int32 area {
        get { return w * h; }
    }
    property int32 width {
        get { return w; }
        set { w = value; }
    }
}

Rect r = Rect(10, 5);
int32 a = r.area;     // 50, calls getter
r.width = 20;         // calls setter; `value` holds the rhs
r.width += 5;         // read-modify-write: getter then setter
```

Reads and writes dispatch the accessors through any receiver shape — a pointer (`rp->width`), a nested member (`outer.rect.width`), or a chain — and the property type can be anything, including a struct (`property std::string name`). Compound assignment and `++`/`--` desugar to get-then-set. Writing a get-only property, or reading a set-only one, is a compile error.

## Constructor initializer lists

Initialize base classes and value-struct fields before the body runs.

```c
struct Inner { int64 v; Inner(int64 x){ v = x; } }

struct Outer {
    Inner i;             // value-struct field with a ctor
    int64 outer_v;
    Outer(int64 x, int64 y) : i(x) {   // runs Inner's 1-arg constructor on this->i
        outer_v = y;
    }
}

int64 main() {
    Outer o = Outer(10, 32);
    return o.i.v + o.outer_v;   // 10 + 32 = 42
}
```

The init-list runs before the body. For a `class D : Base` with `D(int x) : Base(x + 50) { }`, the base-class ctor is called first; for a `Inner i;` field with `: i(arg)`, the field's inner ctor runs and the result is stored into the field. Fields not mentioned in the init list get default initialization (or whatever the body writes first).
