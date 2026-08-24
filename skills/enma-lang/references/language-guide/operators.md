> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/operators.md).

# Operators

Structs and classes overload operators as members, with free operators for an asymmetric left-hand side. See [Structs & Classes](/perception/enma-lang/language-guide/structs-and-classes.md) for the types themselves.

```c
struct Vec2 {
    float64 x;
    float64 y;
    Vec2(float64 a, float64 b) { x = a; y = b; }
    Vec2 operator+(Vec2 o) { return Vec2(x + o.x, y + o.y); }
    Vec2 operator-(Vec2 o) { return Vec2(x - o.x, y - o.y); }
    Vec2 operator*(float64 s) { return Vec2(x * s, y * s); }
    bool operator==(Vec2 o) { return x == o.x && y == o.y; }
}

Vec2 a = Vec2(1.0, 2.0);
Vec2 b = Vec2(3.0, 4.0);
Vec2 c = a + b;  // Vec2(4.0, 6.0)
```

**Supported operators**

| Form                  | Operators                                                     | Example signature                                                                                                                                                                                                                                                                                                                                                            |
| --------------------- | ------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Binary arithmetic     | `+`, `-`, `*`, `/`, `%`                                       | `T operator+(T other)`                                                                                                                                                                                                                                                                                                                                                       |
| Comparison            | `==`, `!=`, `<`, `>`, `<=`, `>=`                              | `bool operator==(T other)`                                                                                                                                                                                                                                                                                                                                                   |
| Three-way comparison  | `<=>`                                                         | `std::strong_ordering operator<=>(T other)` — yields a `<compare>` category, never a bare integer. The four relational ops `<` `>` `<=` `>=` synthesize from it; `==` / `!=` do **not** — they synthesize only from a declared `operator==`. `= default` derives it memberwise in declaration order. Derived classes inherit `operator<=>` from any base in the chain.       |
| Bitwise / shift       | `&`, `\|`, `^`, `<<`, `>>`                                    | `T operator^(T other)`                                                                                                                                                                                                                                                                                                                                                       |
| Compound assignment   | `+=`, `-=`, `*=`, `/=`, `%=`, `&=`, `\|=`, `^=`, `<<=`, `>>=` | `void operator+=(T other)` (mutates `*this`)                                                                                                                                                                                                                                                                                                                                 |
| Copy assignment       | `=`                                                           | `void operator=(const T& other)` — fires on `b = a;` for already-constructed `b`. Lets the type release `b`'s old resources before assigning `a`'s. (Distinct from copy ctor `T(const T&)`, which fires on `T b = a;` while constructing `b`.)                                                                                                                               |
| Move assignment       | `=`                                                           | `void operator=(T&& other)` — selected when the RHS is an rvalue: `b = move(a)`, or a fresh temporary (`b = T(args)`, or `b = make()` where `make` returns `T` by value). An lvalue RHS (`b = a`, a container element `b = v[0]`, or a reference-returning call `b = ref_fn()`) stays on copy-assignment. Falls back to copy-assignment when no move-assignment is declared. |
| Increment / decrement | `++`, `--`                                                    | `T operator++()` (prefix, `return *this;`) / `T operator++(int)` (postfix, returns the old value). C++'s int-dummy-param convention. Prefix and postfix are distinct arities — postfix is not synthesized from prefix.                                                                                                                                                       |
| Unary                 | `+`, `-`, `~`, `!`, `*`                                       | `T operator-()` / `bool operator!()` / `U operator*()` (smart-pointer-style deref — when the operand is a value struct, `*obj` calls this; pointer deref `*pt` for `T*` keeps regular memberwise-copy semantics).                                                                                                                                                            |
| Subscript             | `[]`                                                          | `T operator[](int64 i)` — value- or ref-returning. A ref-returning `operator[]` is what makes `obj[i] = v` work.                                                                                                                                                                                                                                                             |
| Function call         | `()`                                                          | `T operator()(args...)` — makes the type callable: `obj(a, b)`                                                                                                                                                                                                                                                                                                               |
| Type conversion       | `operator T()`, `operator T*()`                               | Called by `static_cast<T>(obj)` AND implicitly at variable initialization, function-argument binding, return statements, and arithmetic operands. T can be a primitive (int64, bool, ...), another struct, or a pointer. A pointer target is a separate function from its value twin, so one class may declare both `operator char()` and `operator char*()`.                |
| Smart-pointer arrow   | `->`                                                          | `U* operator->()` — `obj->member` reads `member` on the returned pointer; `obj->method(...)` calls a method on it                                                                                                                                                                                                                                                            |

`!=` automatically negates `==` if you don't define it explicitly. Compound assignment is **not** synthesized from the binary operator — declare `operator+=` yourself; a defined `operator+` does not make `a += b` compile. From a declared `<=>` the four relational operators `<` `>` `<=` `>=` are synthesized; `==` and `!=` are not, because equality and ordering are separate questions. An explicitly declared relational operator always wins over the synthesized one.

**Conversions have a direction.** A conversion operator is the only way a class value becomes a pointer. A constructor taking `T*` converts the other way — a `T*` into the class — and never out of it, so declaring `Wrap(const char* s)` does not let a `Wrap` bind a `char*` parameter; declare `operator char*()` for that. Conversion operators rank below every built-in conversion, so an exact match or a base upcast always wins the overload.

```c
struct Buf {
    char store[8];
    Buf() { store[0] = 65; store[1] = 0; }
    operator char*() { return &store[0]; }
}

int64 first(char* s) { return s[0]; }

int64 main() {
    Buf b;
    return first(b);        // 65 — through operator char*()
}
```

**Inheritance and operator overloads.** When a derived type doesn't define its own operator, the dispatcher walks the base chain looking for one — `class D : public B {}` with `B::operator+(const B&)` lets `D d1, d2; d1 + d2` compile and run against the inherited `B::operator+`. The rhs is implicitly upcast from `D` to `B`. Inheritance has to be public for that upcast to be accessible, exactly as in C++. Override the operator on `D` to specialise the behavior.

**Operators on globals and ns-qualified globals.** Operator overloads dispatch correctly whether the operands are locals, globals, namespace-qualified globals (`ns::g_a + ns::g_b`), or struct-field receivers (`g_pair.a + g_pair.b`). The compiler routes ref-param ABIs through a temp-spill so the `const T&` indirection matches whatever the source's storage shape is.

**Free-function operators + ADL.** Operators can also be defined as free functions outside the struct:

```cpp
struct P { int64 v; }
bool operator==(P a, P b) { return a.v == b.v; }
P    operator+ (P a, P b) { P r; r.v = a.v + b.v; return r; }
```

When the free operator lives inside a `namespace`, Enma finds it via **Argument-Dependent Lookup (ADL)** — `ns::P x, y; if (x == y)` resolves `ns::operator==(P, P)` even though the call site is unqualified and outside the namespace. The compiler walks the operand type's enclosing namespace chain looking for matching free operators:

```cpp
namespace ns {
    struct P { int64 v; }
    bool operator==(P a, P b) { return a.v == b.v; }
    P    operator+ (P a, P b) { P r; r.v = a.v + b.v; return r; }
}

int64 main() {
    ns::P x; x.v = 42;
    ns::P y; y.v = 42;
    if (x == y) {         // finds ns::operator== via ADL
        ns::P z = x + y;  // finds ns::operator+ via ADL, returns ns::P by value
        return z.v;
    }
    return 0;
}
```

Free operators returning a value-struct go through the same NRVO/SRET path as struct-method operators — the result is constructed directly into the destination slot. The return type of an ADL-resolved operator is also visible to `return x + y;` and `auto z = x + y;` so type-checking and inference work without a temp local.

**ADL coverage details:**

* **All operator categories** — arithmetic (`+`, `-`, `*`, `/`, `%`), bitwise (`&`, `|`, `^`, `<<`, `>>`), comparison (`==`, `!=`, `<`, `>`, `<=`, `>=`), unary (`-`, `~`, `!`, `++`, `--`), spaceship (`<=>`), and compound assignment (`+=`, `-=`, etc.).
* **All param ABI shapes** — by-value `P`, by-ref `P&`, const-ref `const P&`, mixed. The compiler spills + leas references automatically.
* **Inheritance** — if `class D : ns::B`, then `D` instances can dispatch through `ns::operator==(B, B)` via ADL. The compiler walks the operand type's base chain when resolving free operators.
* **Member operators take priority over free operators** — if both exist, the member version is selected.
* **`operator<=>` synthesis** — a free `operator<=>(P, P)` declared in a namespace synthesizes the four relational ops (`<`, `>`, `<=`, `>=`) via ADL just like the member form. It yields a `<compare>` category, never a bare integer, and `==`/`!=` are not synthesized from it.
* **Compound assignment is not synthesized** — `a += b` requires `operator+=`; a defined `operator+` does not supply it. The namespace above declares no `operator+=`, so `u += v;` there is a compile error: *"no `operator+=` is defined for `math::V3`"*.

```cpp
namespace math {
    struct V3 { float64 x; float64 y; float64 z; }

    V3   operator+(const V3& a, const V3& b) { V3 r; r.x = a.x + b.x; r.y = a.y + b.y; r.z = a.z + b.z; return r; }
    V3   operator-(const V3& a)              { V3 r; r.x = -a.x; r.y = -a.y; r.z = -a.z; return r; }
    bool operator==(const V3& a, const V3& b){ return a.x == b.x && a.y == b.y && a.z == b.z; }
}

int64 main() {
    math::V3 u; u.x = 1.0; u.y = 2.0; u.z = 3.0;
    math::V3 v; v.x = 1.0; v.y = 2.0; v.z = 3.0;
    math::V3 sum = u + v;     // ADL + const-ref + SRET
    math::V3 neg = -u;        // ADL unary
    if (u == v) { return 0; }
    return 1;
}
```

**Return-by-value (NRVO).** A function returning a value-struct constructs the result directly into the caller's return slot — no intermediate temp, no copy ctor. This matches C++17 mandatory copy elision. Constructors / dtors fire exactly once per object, balanced. `make()` discarded inline still runs `~T()` at end-of-statement.

```c
struct Bits {
    uint64 v;
    Bits(uint64 x) { v = x; }
    Bits operator&(Bits o) { return Bits(v & o.v); }
    Bits operator|(Bits o) { return Bits(v | o.v); }
    Bits operator^(Bits o) { return Bits(v ^ o.v); }
    Bits operator~()       { return Bits(~v); }
    bool operator!()       { return v == 0; }
    bool operator==(Bits o){ return v == o.v; }
}

struct Adder {
    int64 base;
    Adder(int64 b) { base = b; }
    int64 operator()(int64 x) { return base + x; }
}

Adder a = Adder(100);
int64 r = a(5);   // 105

struct Inner {
    int64 v;
    Inner(int64 x) { v = x; }
    int64 doubled() { return v * 2; }
}
struct Holder {
    Inner* p;
    Holder(int64 v) { p = new Inner(v); }
    Inner* operator->() { return this->p; }   // smart-pointer style
}

Holder h = Holder(42);
int64 v = h->v;            // 42 — operator->() then read .v on returned ptr
int64 d = h->doubled();    // 84 — operator->() then call .doubled() on returned ptr
delete h.p;

struct Box {
    int64 v;
    Box(int64 x) { v = x; }
    operator int64() { return v * 2; }
    operator bool()  { return v != 0; }
}

Box b = Box(21);
int64 i = static_cast<int64>(b);   // 42 — calls operator int64()
int64 j = b;                // 42 — same call, fired implicitly at variable init
bool  z = static_cast<bool>(b);    // true — calls operator bool()
bool  w = b;                // true — implicit at variable init

// Cross-struct conversion — T can be another struct, not just a primitive.
struct Celsius   { float64 deg; Celsius(float64 d)   { deg = d; } }
struct Fahrenheit {
    float64 deg;
    Fahrenheit(float64 d) { deg = d; }
    operator Celsius() { return Celsius((deg - 32.0) * 5.0 / 9.0); }
}

Fahrenheit f = Fahrenheit(212.0);
Celsius    c = f;                   // implicit — calls operator Celsius()
Celsius    e = static_cast<Celsius>(f);    // explicit — same call
```
