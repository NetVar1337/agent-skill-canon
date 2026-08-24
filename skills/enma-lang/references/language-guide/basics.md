> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/basics.md).

# Basics

## Types

| Type                                     | Size | Description            |
| ---------------------------------------- | ---- | ---------------------- |
| `bool`                                   | 1B   | `true` / `false`       |
| `char`                                   | 1B   | ASCII character        |
| `wchar` / `wchar_t`                      | 2B   | Wide character         |
| `int8` / `int16` / `int32` / `int64`     | 1-8B | Signed integers        |
| `uint8` / `uint16` / `uint32` / `uint64` | 1-8B | Unsigned integers      |
| `aint8` / `aint16` / `aint32` / `aint64` | 1-8B | Atomic integers        |
| `float32`                                | 4B   | Single-precision float |
| `float64`                                | 8B   | Double-precision float |
| `void`                                   | 0    | No value               |
| `null`                                   | -    | Null literal           |
| `auto`                                   | -    | Type inference         |
| `void*`                                  | 8B   | Untyped pointer        |

**Aliases.** `float` / `double` spell `float32` / `float64`. The `<cstdint>` family is available too — `int8_t` … `int64_t`, `uint8_t` … `uint64_t`, plus `size_t`, `ptrdiff_t`, `intptr_t` and `uintptr_t` at their LLP64 widths.

`std::string` / `std::wstring` are prelude value structs, not language types — see [Std Library](/perception/enma-lang/addons/std-library.md).

**Literals.** `"text"` is a `const char[N]` and `L"text"` a `const wchar[N]`; `'c'` is a `char` and `L'c'` a `wchar`. `wchar_t` is an accepted spelling of `wchar`. (String/char escapes like `\n`, `\t`, `\xHH` apply to both narrow and wide forms.)

## Variables & Constants

```c
int32 x = 42;
int32 y = {42};           // same thing, braced
int32 z = {};             // value-initialized: 0
int32 w{42};              // and the brace-only spelling
const float64 PI = 3.14;
constexpr int32 MAX = 100;
auto y2 = x + 1;
std::optional<int32> n;   // maybe-value, starts disengaged
```

A braced initializer on a single value holds exactly one value, or none. Two or more is an error, and `auto` cannot deduce a type from braces — name the type.

`const` prevents reassignment. `constexpr` evaluates at compile time and folds into the IR as a literal — see [Compile-Time Evaluation](/perception/enma-lang/language-guide/compile-time.md) for what's foldable and how to write `constexpr` functions and `static_assert`s. `std::optional<T>` holds a `T` or nothing — see [Std Library](/perception/enma-lang/addons/std-library.md#stdoptionalt). `auto` infers the type from the right-hand side.

### Multi-declarator syntax

You can declare multiple variables of the same type on one line, separating each declarator with a comma. Each declarator may have its own initializer or be left uninitialized:

```c
int64 i, j, k;                       // three uninitialized locals
int64 a = 10, b = 20, c = 30;        // three with initializers
int64 x = 1, y, z = 3;               // mixed: y has no initializer
float64 px, py, pz;                  // works for any type
std::string s1 = "a", s2 = "b", s3;       // strings too
```

The same form works at every declaration site:

```c
// Globals
uint32 g_a, g_b, g_c;

// Namespace globals
namespace cfg { int64 width, height; std::string title; }

// Struct / class fields
struct Vec3 { float64 x, y, z; }
class Foo {
    private: int64 a, b, c;
    public: Foo() { a = 1; b = 2; c = 3; }
}

// For-init clause
for (int64 i = 0, j = 100; i < 5; i = i + 1) { /* ... */ }
```

The type applies uniformly to every declarator, but **declarators bind per declarator, exactly like C++** — `int64* p, q;` declares one `int64*` and one plain `int64`. Bit-fields (`int8 x : 4`) are single-declarator only — declare each bit-field on its own line.

## String literals

A double-quoted literal is a `const char[N]` that decays to `const char*`; `std::string` is built from one. Adjacent literals concatenate, exactly like C++ — `"a" "b"` is `"ab"` — and `+` joins at runtime.

Supported escape sequences:

| Escape              | Produces                                                                                                                                 |
| ------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `\n`                | newline (LF, 0x0A)                                                                                                                       |
| `\t`                | tab (0x09)                                                                                                                               |
| `\r`                | carriage return (0x0D)                                                                                                                   |
| `\\`                | backslash                                                                                                                                |
| `\0`                | null byte                                                                                                                                |
| `\xHH`              | one byte from up to two hex digits — useful for embedding raw UTF-8 sequences (e.g. `"\xEE\xA9\xB0"` is a single 3-byte UTF-8 codepoint) |
| `\a` `\b` `\f` `\v` | bell, backspace, form feed, vertical tab                                                                                                 |
| `\'` `\"`           | quote characters                                                                                                                         |
| `\<other>`          | the literal character (the backslash is dropped)                                                                                         |

Raw strings process no escapes, and are the only multi-line literal form:

```c
std::string path = R"(C:\dir\file)";      // no escaping needed
std::string body = R"DELIM(has a )" inside)DELIM";
std::wstring w   = LR"(wide raw)";
```

```c
std::string newline = "line one\nline two";
std::string utf8    = "\xEE\xA9\xB0";    // 3-byte UTF-8 sequence, length() == 3
```

## Number Literals

```c
int32 a = 42;            // integer literal → default int32
int64 b = 42;            // fits context
int64 c = 0xFF;          // hex
int64 d = 0b1010;        // binary
int64 o = 017;           // octal (leading zero) = 15
float64 e = 3.14;        // float literal → default float64
float32 f = 3.14f;       // f/F suffix → float32
float64 g = 1.5e-3;      // scientific notation
float64 h = .5;          // leading dot; 1. and 1e5 also parse
float64 x = 0x1p3;       // hex float = 8.0
int64 big2 = 100L;       // L / LL / U / UL / ULL suffixes

int64 big = 1'000'000;    // digit separator (any numeric form)
int64 mask = 0xFF'FF'FF;  // separator works inside hex too
int64 bin  = 0b1010'1100; // and binary
float64 pi = 3.141'592'6; // and floats

int64 km = 42_km;         // user-defined literal suffix (calls _km(42))
float64 m = 1.5_m;        // same for float literals
int64 mix = 1'500_km;     // separator + UDL: 1500 with the _km suffix
```

Float literals use `f` / `F` suffix for `float32`; bare `3.14` is `float64`. Integer literals default to `int32` in contexts that accept any integer but adapt when assigned to a wider type. Digit separators use `'` (`1'000'000`) and work in all numeric forms — they're stripped at lex time so parsers see clean digits. User-defined literal suffixes (`_km`, `_m`, etc.) parse as calls to a same-named function — see [user-defined literals](/perception/enma-lang/language-guide/functions.md#user-defined-literals).

## Operators

* **Arithmetic:** `+` `-` `*` `/` `%` — modulo follows C semantics (result takes the sign of the dividend: `-7 % 2 == -1`).
* **Comparison:** `==` `!=` `<` `>` `<=` `>=`
* **Logical:** `&&` `||` `!`
* **Bitwise:** `&` `|` `^` `~` `<<` `>>`
* **Assignment:** `=` `+=` `-=` `*=` `/=` `%=` `&=` `|=` `^=` `<<=` `>>=`
* **Increment / Decrement:** `++` `--` (prefix and postfix)
* **Three-way comparison:** `<=>` — yields a `<compare>` category, never a bare integer. Built-in arithmetic operands give `std::strong_ordering`, except floating-point, which gives `std::partial_ordering` so a NaN operand is `unordered`.
* **Ternary:** `cond ? a : b`

Arithmetic on unsigned narrow integer types (`uint8`, `uint16`, `uint32`) wraps modulo the type's width on assignment — `uint8 b = 255; b = b + 1;` leaves `b == 0`. Internally Enma's arithmetic operates on 64-bit values, but stores back to narrow unsigned locals mask the result to the declared width. Signed narrow types (`int8` / `int16` / `int32`) carry through int64 arithmetic with sign-extend on read.

* **Cast:** Five C++-style variants:
  * `static_cast<T>(val)` — value conversions with C++'s rules: numeric widen/narrow/truncate, enum ↔ int, bool, and user `operator T()` overloads. A pointer source only converts to `bool` (exactly C++).
  * `reinterpret_cast<T>(val)` — pointer ↔ pointer, pointer ↔ 64-bit integer, and own-type identity only. A float ↔ int bit reinterpret is refused; `std::bit_cast<T>(x)` is the bit-level spelling and `static_cast<T>(x)` the value conversion.
  * `const_cast<T>(val)` — identity at the IR level; same byte size required. Strips const, e.g. copying a const local to a mutable one.
  * `dynamic_cast<T*>(ptr)` — runtime-checked downcast for polymorphic class pointers. Returns `ptr` if its runtime type IS-A `T`, or `null` on failure. Uses **vtable identity** rather than RTTI strings, so no type-name metadata is emitted. A null source yields null. Idiom: `if (D* d = dynamic_cast<D*>(b)) { … }`.
  * C-style `(T)x` is also accepted.
* **Sizeof / alignof:** `sizeof(type)` is the byte size, and `sizeof` also takes an unparenthesized expression (`sizeof x`, `sizeof arr[0]`, `sizeof *p`) — the operand is never evaluated. `alignof(T)` is the alignment.
* **Bit-cast:** `std::bit_cast<T>(x)` is the only float ↔ int bit reinterpret.
* **Offsetof:** `offsetof(Struct, field)` returns the byte offset of a field.
* **Heap:** `new T(args)`, `new T[N]`; `delete ptr`, `delete[] ptr`.

## Slice expressions

`a[lo : hi]` copies the half-open element range `[lo, hi)`. The result is itself a fixed array, so its extent is part of its type.

```c
int64 v[5] = {1, 2, 3, 4, 5};
auto mid = v[1 : 4];      // {2, 3, 4}
```

The operand is a fixed array of primitive elements whose extent is known where the slice is written — a local, or a struct member — and both bounds are constant expressions. Any other receiver, a container or a struct-element array among them, is a compile error naming the requirement. A container's range is spelled with its own members.

## Control Flow

### if / else

```c
if (x > 0) {
    println("positive");
} else if (x == 0) {
    println("zero");
} else {
    println("negative");
}
```

### while / do-while

```c
while (x > 0) {
    x = x - 1;
}

do {
    x = x + 1;
} while (x < 10);
```

An assignment is a condition — its value is the value assigned — so the read-loop idiom is spelled directly. Write `(x = f()) != 0` when the comparison is what you mean.

```c
int64 n = 0;
while ((n = next()) > 0) { total = total + n; }
```

### for

```c
for (int32 i = 0; i < 10; i++) {
    println(std::to_string(i));
}
```

### for-each

```c
int32 arr[] = {1, 2, 3};
for (int32 v : arr) {
    println(std::to_string(v));
}
```

Works over builtin fixed arrays and the `std` containers alike. Maps iterate with a structured binding per entry:

```c
std::map<std::string, int64> m;
m["a"] = 1;
m["b"] = 2;
for (auto [k, v] : m) {
    println(k);          // k is a std::string
}
```

### switch

```c
switch (x) {
    case 1: println("one"); break;
    case 2: println("two"); break;
    default: println("other");
}
```

`switch` and `if` both take an initializer before the condition. The name scopes over the whole statement and nowhere else.

```c
switch (int64 code = classify(v); code) {
    case 1: println("one"); break;
    default: println(std::to_string(code));
}
```

### match

A match expression returns a value. The `case` keyword is optional.

```c
int32 result = match (x) {
    1 => 10,
    2 => 20,
    3 => 30,
    _ => 0
};
```

Match arms can have block bodies. Blocks execute as statements (they don't yield a value), so use the expression form when you need a result.

```c
match (x) {
    case 1 => { println("one"); },
    _ => { println("other"); }
};
```

### defer

`defer` schedules a statement to run when the enclosing scope exits, including during stack unwinding from exceptions.

```c
int64 handle = open_resource();
defer { close_resource(handle); }
// handle is always closed, even if an exception is thrown
```

### goto

```c
goto done;
println("skipped");
done:
println("reached");
```

### break / continue

`break` exits the innermost loop. `continue` skips to the next iteration.

## Enums

Enumerators number from 0 and continue from the last explicit value.

```c
enum Color { Red, Green = 5, Blue }    // Red=0, Green=5, Blue=6

int32 c = Red;                          // unscoped: visible in the enclosing
                                        // scope, promotes to the underlying type
```

`enum class E` (equivalently `enum struct E`) is a **scoped** enum: its enumerators are reachable only as `E::A`, and it does not convert to an integer implicitly — `static_cast` in both directions.

```c
enum class Status { Idle, Running, Done }

Status s = Status::Running;
int32 n = static_cast<int32>(s);        // 1 — explicit both ways
```

Either form may fix its underlying type; unfixed, it is `int32`.

```c
enum Flags : int64 { None = 0, Read = 1, Write = 2 }
```

`using enum E;` brings an enum's enumerators into the current scope — at namespace scope or inside a block — so a `switch` can name them bare.

```c
using enum Status;
switch (s) {
    case Idle:    break;
    case Running: break;
    case Done:    break;
}
```

## Type aliases

```c
using ID = int32;
using Point = Vec2;

ID player_id = 42;
```

`typedef int32 ID;` also works.

An alias names the same type as its target everywhere that type may appear — as a local, a parameter, a return type, a member. An alias-typed by-value parameter is copied and destroyed exactly as the type it names:

```c
struct Cent { int64 v; Cent(int64 x) { v = x; } /* copy ctor + dtor */ }

using Money = Cent;

int64 take(Money m) { return m.v; }     // one copy in, one destroy out

int64 main() {
    Money m = Money(42);
    return take(m);
}
```

## decltype

`decltype(expr)` is a type specifier that infers the type from an expression at compile time. Useful in generic code where a variable's type should follow an existing one.

```c
int64 x = 42;
decltype(x) y = x + 1;           // y is int64

int32 a = 10;
decltype(a + a) sum = 100;       // sum is int32 (type of a + a)

int64 double_it(int64 v) { return v * 2; }
decltype(double_it(0)) r = 99;   // r is int64 (the return type)
```

Works anywhere a type specifier is accepted: variable declarations, function parameters, and return types.

## Inline asm intrinsics

Four preapproved x64 intrinsics, emitted as their literal opcode bytes with no native-call overhead. There are no user-provided bytes and no arbitrary memory access.

```c
int64 tsc = __asm_rdtsc();   // rdtsc, composed rdx:rax → int64
__asm_pause();               // F3 90 — spin-loop hint
__asm_mfence();              // 0F AE F0 — full memory barrier
__asm_nop();                 // 90
```

Use them for tight timing (`__asm_rdtsc`), spin-wait loops (`__asm_pause`), and cross-thread memory ordering (`__asm_mfence`) without paying the call-boundary cost.
