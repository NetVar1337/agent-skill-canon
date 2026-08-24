> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/compile-time.md).

# Compile-Time Evaluation

Mark a function or variable `constexpr` to evaluate it at compile time. References to the result fold into the IR as a literal, so there is no runtime cost. `static_assert` checks a folded value and fails the build if it is false.

## static\_assert

```c
static_assert(sizeof(int32) == 4, "int32 must be 4 bytes");
static_assert(1 + 1 == 2);              // message optional
```

Works at module scope and inside function bodies.

```c
static_assert(MAX_PLAYERS <= 64);  // module scope

int32 main() {
    static_assert(sizeof(Packet) == 32, "packet abi");  // function scope
    return 0;
}
```

A failed assertion reports the user message verbatim, or `static assertion failed` if none was supplied. An expression that is not a compile-time constant reports `static_assert expression must be a compile-time constant` and the line that tripped it.

## consteval and constinit

`consteval` marks a function that must run at compile time — a call that cannot be folded is an error rather than falling back to runtime.

```c
consteval int64 table_size(int64 n) { return n * 4; }
constexpr int64 SZ = table_size(8);   // 32, folded
```

`constinit` requires a variable's initializer to be a constant expression, so it is guaranteed initialized before anything runs — without making the variable itself `const`.

```c
constinit int64 g_counter = 0;   // must fold; still mutable at runtime
```

## if constexpr

`if constexpr (cond)` picks a branch at compile time and discards the other, so the discarded branch need not compile for the current instantiation.

```c
template<typename T>
int64 size_of() {
    if constexpr (sizeof(T) > 4) { return 8; }
    else { return 4; }
}
```

## constexpr variables

```c
constexpr int32 MAX = 100;
constexpr int64 MASK = (0xff << 8) | 0xf;
constexpr float64 HALF = 1.0 / 2.0;
```

Initializers may use any expression the folder accepts — literals, integer/float arithmetic, bitwise ops, ternary, casts, `sizeof`/`offsetof`, enum values, other `constexpr` identifiers, and calls to `constexpr` functions. An initializer that cannot be folded reports ``constexpr `X`: initializer is not a compile-time constant``.

A list of values fills a `constexpr` variable of class type member by member only when the type is an **aggregate**: no user-declared constructor, no private or protected non-static data member, no virtual function, and only public non-virtual bases that are themselves aggregates.

```c
struct Point { int64 x; int64 y; }
constexpr Point p = {1, 2};        // aggregate — folds member by member
static_assert(p.x + p.y == 3);

struct Sum {
    int64 first;
    int64 total;
    Sum(int64 a, int64 b) { first = a; total = a + b; }
}
constexpr Sum s = {1, 3};          // error: built by a constructor
```

On any other class the list supplies the **constructor's arguments**, and a constructor body does not run at compile time — so there is no value to fold and the declaration is rejected. Supplying more values than the aggregate has members reports ``too many initializers for `Point` — 3 value(s) for 2 member(s)``.

The three spellings of one initialization read identically and reach the same answer:

```c
constexpr Point a = {3, 4};        // all three fold
constexpr Point b = Point(3, 4);
constexpr Point c = Point{3, 4};

constexpr Sum d = Sum(1, 3);       // all three are rejected
constexpr Sum e = Sum{1, 3};
```

`= default` and `= delete` are declarations, so either one disqualifies:

```c
struct D { int64 a; D() = default; }
constexpr D d = {1};               // error: D declares a constructor
```

A user-declared **destructor**, a non-virtual member function, a private member function, and a `static` data member all leave the class an aggregate — none of them is a non-static data member, and a `static` member has no per-instance storage for a value to land on.

```c
struct Keep { int64 a; int64 b; ~Keep() { } }
constexpr Keep k = {1, 2};         // still an aggregate
static_assert(k.b == 2);
```

A member the list does not reach is value-initialized — its **default member initializer** when it has one, and zero otherwise.

```c
struct Cfg { int64 width = 80; int64 height; }
constexpr Cfg c = {};
static_assert(c.width == 80);      // the default initializer
static_assert(c.height == 0);      // no initializer — zero
```

A `union` is an aggregate whose list initializes its **first** member only. Reading any other member of the folded value is not a constant expression.

```c
union Word { int64 i; float64 f; }
constexpr Word w = {5};
static_assert(w.i == 5);
```

Inside a namespace, `constexpr` variables and functions are visible to other declarations in that namespace by their **bare** name, and to outside callers by their **qualified** name:

```c
namespace cfg {
    constexpr int64 value = 42;
    int64 get() { return value; }   // bare name — works inside `cfg`
}

int64 main() {
    return cfg::value + cfg::get();  // qualified access from outside
}
```

## constexpr functions

```c
constexpr int32 fact(int32 n) {
    if (n <= 1) return 1;
    return n * fact(n - 1);
}

constexpr int32 FACT_10 = fact(10);
static_assert(FACT_10 == 3628800);
```

They may use:

* arithmetic, bitwise, shift, comparison, logical ops (int and float)
* ternary `cond ? a : b`
* `static_cast<T>(...)`, char literals, enum values
* local variables and assignments (including `+=`, `++`, etc.)
* `if`/`else`, `for` and `while` loops
* recursion and mutual recursion; calls to other `constexpr` functions
* other `constexpr` variables, `sizeof`, `offsetof`
* string-literal subscripts: `"hello"[1]` is a core constant expression (including the terminating NUL at `[len]`; past that is an error) — and `s[i]` on a `const char*` parameter walks a literal argument the same way
* struct construction (`P(3, 4)`, `P{5, 6}`, `= {1, 2}`) and field access, nested structs included
* `constexpr` arrays (`int64 arr[] = {…}`, `constexpr P pts[] = {…}`) — with subscripts

A loop budget caps total compile-time iterations at **100,000** per top-level call, so an accidentally infinite loop trips a clear error instead of hanging the compiler.

## What does not fold

These are not compile-time constants. As a `constexpr` initializer or `static_assert` expression they are an error; anywhere else they fall back to runtime evaluation.

* **any method call**, including `s.size()` on a `std::string` parameter — the folder reports *"`size` is a method call, and a method call is never evaluated at compile time"*. Take a `const char*` and walk it with `[i]` instead
* method calls on a bare literal — a literal is a `const char*`, which has no members (`"hello".size()` is an error even at compile time; use `[i]`)
* `==` between two literals (that is a pointer-identity compare)
* method calls on `constexpr` arrays (`arr.size()` — an array has no members; the `[i]` subscript folds)
* pointer operations (`new`, `delete`, `&`, `*`)
* calls to non-`constexpr` functions
* a class that is built by a **constructor**, including every container: `constexpr std::vector<int64> v = {1, 4, 9};` reports that the type *"is initialized by a constructor, and a constructor is not run at compile time"*. Only an **aggregate** folds from a braced list — see below

If you hit one of these, hoist the work into a helper that returns the final scalar.

## Compile-time hashing

Hash a string at compile time so identifier lookups become constant integer comparisons.

```c
constexpr int64 fnv1a(const char* s) {
    int64 hash = 0xcbf29ce484222325;
    for (int32 i = 0; s[i] != 0; i = i + 1) {
        hash = hash ^ s[i];
        hash = hash * 0x100000001b3;
    }
    return hash;
}

constexpr int64 H_PLAYER = fnv1a("player");
constexpr int64 H_ENEMY  = fnv1a("enemy");

static_assert(fnv1a("hello") == 0xa430d84680aabd0b, "fnv reference");

int32 dispatch(int64 h) {
    if (h == H_PLAYER) return 1;
    if (h == H_ENEMY)  return 2;
    return 0;
}
```

Both constants collapse to immediates in the emitted IR, and the `static_assert` costs nothing at runtime.
