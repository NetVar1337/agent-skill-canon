> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/readme.md).

# Introduction - Enma

Enma is a full-module AOT scripting language targeting x64. Compiles to native machine code through an optimizing pipeline. Designed to embed in native applications.

```cpp
int32 fib(int32 n) {
    if (n <= 1) return n;
    return fib(n - 1) + fib(n - 2);
}

int32 main() {
    println(std::to_string(fib(30)));
    return 0;
}
```

### Features

**Primitives:** `bool`, `char`, `wchar`, `int8`/16/32/64, `uint8`/16/32/64, `aint8`/16/32/64 (atomic), `float32`, `float64`, `string`, `wstring`, `void`, `null`, `auto`

**Variables:** `const`, `constexpr`, `auto` inference, `std::optional<T>` maybe-values

**Operators:** arithmetic, comparison, logical, bitwise, compound assign (`+=` etc.), `++`/`--`, ternary, `static_cast<T>()`, `sizeof`, `offsetof`

**Control flow:** `if`/`else`, `while`, `do-while`, `for`, `for-each`, `switch`, `match`, `break`, `continue`, `goto`, `defer`

**Functions:** default args, reference (`&`), out (`out`), variadic (`...` + `__va_count` / `__va_arg`), `extern`, `const` methods, function references

**Lambdas:** `[caps](p) -> T { }` and arrow `(p) => expr`

**Arrays:** builtin fixed `T a[N]` (1D/2D, brace init) + heap `new T[N]`/`delete[]` + `std::vector<T>` (`push_back`/`size`/`insert`/`erase`/iterators) + `std::array<T, N>`; `std::sort`/`transform`/`filter`/`join`

**Maps & sets:** `std::map<K,V>` (sorted) / `std::unordered_map<K,V>` with `m[k]`/`at`/`insert`/`contains`/`erase`/`size`, iteration via `for (auto [k, v] : m)`; `std::set<T>` / `std::unordered_set<T>`

**Strings:** `std::string`/`std::wstring`, concat with `+`, `size`/`length`, `substr`, `find`/`rfind`, `contains`, `starts_with`/`ends_with`, `at`/`s[i]`; free algorithms `std::split`/`trim`/`to_upper`/`to_lower`/`repeat`, and `std::stoi`/`stod`/`to_string`/`replace_all`/`format`

**Structs:** value types, fields, ctor, dtor, methods, operator overloading, bitfields, packed/aligned

**Classes:** reference types, access control (`public`/`private`/`protected`, base-clause access), single and multiple inheritance, both diamond forms, virtual dispatch with vtable thunks for non-primary base overrides, `override`, abstract classes, `dynamic_cast`, RAII

**Interfaces:** abstract method contracts

**Properties:** getter/setter syntax

**Templates:** generic structs and functions, monomorphization, reference params, nesting

**Enums:** `Enum::Value` access with preserved identity

**Typedefs:** `using Alias = Type` or `typedef Type Alias`

**Delegates:** typed function references

**Namespaces:** `namespace`, `using namespace`, `::`

**Exceptions:** `try`/`catch`/`throw` with stack unwinding, dtors and `defer` run during unwind

**Heap allocation:** `new T(args)`, `new T[N]`, `new T[N](ctor_args)`, `delete`, `delete[]`

**Inline asm intrinsics:** `__asm_rdtsc`, `__asm_pause`, `__asm_mfence`, `__asm_nop`

**Annotations:** `[[packed]]`, `[[align(N)]]`, `[[reflect]]`, `[[serialize]]`, `[[noopt]]`, `[[noinline]]`, `[[inline]]`, `[[dll(...)]]` (FFI), custom annotations queryable from host

**Modules:** `import` with aliasing, precompiled `.emb`, source fallback, multi-module linking

**Preprocessor:** `#define`/`#undef`, `#ifdef`/`#ifndef`/`#elif`/`#else`/`#endif`, `#include`, `#pragma`

**FFI:** `[[dll("lib.so")]]` gated by `PERM_FFI`

**Static assert:** `static_assert(expr, "message")`

**SDK:** C++ embedding API. Type registration with fields, methods, properties, subscript, iteration, factory, destructor, full operator set (arithmetic, comparison, three-way `compare()`, compound assign, ++/--, bitwise), implicit conversion, hash, copy. Typed `_typed<&Fn>` wrappers for every hook. Interfaces with auto-injected `type_id`. Generic type parameters with interface constraints (`.requires_iface`). Native function binding with sig strings supporting `array<T>`, `map<K,V>`, `const T&`, default args, variadic, overloading by arity / types / element type. Hot reload, serialization, introspection, debug hooks, sandboxing, permission gating.

### Current Benchmark

Median of 11 runs, Windows x64. Six workloads: `fib(35)`, `sum 100M`, `nested 2K²`, `sieve 1M`, `collatz 100K`, `bubble 3K`. Pure execution time (script-internal `time_ms()`).

| Benchmark    | Rust -O | C++ -O2 | **Enma** | Node V8 | LuaJIT | Lua 5.4 | AngelScript |
| ------------ | ------- | ------- | -------- | ------- | ------ | ------- | ----------- |
| fib(35)      | 14.9    | 11.3    | **26**   | 44.9    | 50     | 316     | 444.8       |
| sum 100M     | 21.5    | 9.1     | **20**   | 52.9    | 51     | 208     | 537.8       |
| nested 2K²   | 0.8     | 9.9     | **1**    | 2.5     | 2      | 8       | 19.1        |
| sieve 1M     | 0.6     | 10.9    | **6**    | 4.5     | 6      | 30      | 30.4        |
| collatz 100K | 6.5     | 10.2    | **25**   | 13.3    | 31     | 147     | 156.7       |
| bubble 3K    | 1.7     | 9.6     | **11**   | 5.0     | 2      | 55      | 159.8       |
| **Total**    | 46.0    | 61.0    | **89**   | 123.1   | 142    | 764     | 1348.6      |

Matches or beats LuaJIT on 5/6 workloads (only bubble trails).

### Safety

**Fault trapping:** Null deref, invalid memory access caught via SIGSEGV/SEH and mapped to source. Host doesn't crash.

**Execution budget:** Per-loop instruction count via `set_budget()`. Prevents infinite loops.

**Memory budget:** Cap total allocations with `set_memory_budget()`.

**Permission gating:** `PERM_FFI` controls `[[dll(...)]]` access; `PERM_FILE` controls file-addon calls; per-method permissions on registered types.

**Sandboxing:** Scripts only see functions explicitly registered by the host.

**Type verification:** Native sig types checked at script call sites: wrong arg type, wrong struct identity, wrong enum, or wrong container element type are all rejected at compile time. Const correctness for parameters and methods. Implicit conversion via registered `convert_from` functions.

**Memory model:** Deterministic. Structs stack-allocated by default; `new T(...)` for explicit heap. Destructors run at scope exit (normal flow, exception unwind, native fault unwind). No tracing GC. Escape patterns (return pointer to stack struct, store to struct-typed global, closure capturing a stack struct) are compile errors.

**Thread safety:** Per-thread TLS, independent engines, concurrent contexts from same module. Tested under ASAN/TSAN with 32+ threads.

### Getting Started

Writing scripts: [basics](/perception/enma-lang/language-guide/basics.md)

Embedding: [SDK Quick Start](/perception/enma-lang/sdk-guide/quick-start.md)
