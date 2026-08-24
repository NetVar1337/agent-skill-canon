> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/quick-access.md).

# Quick Access

### Language Guide

[**Basics**](/perception/enma-lang/language-guide/basics.md) - Types, variables, constants, operators, control flow

[**Functions**](/perception/enma-lang/language-guide/functions.md) - Parameters, defaults, ref/out, variadic, lambdas, closures

[**Pointers**](/perception/enma-lang/language-guide/pointers.md) - Heap pointers, address-of, `.`/`->`, refs, null, fixed arrays, escape rules

[**Structs & Classes**](/perception/enma-lang/language-guide/structs-and-classes.md) - Value types, constructors, destructors, initialization order, const members, operator overloading, layout

[**Operators**](/perception/enma-lang/language-guide/operators.md) - Operator overloading: arithmetic, comparisons, `<=>`, subscript, call, conversion

[**Inheritance & Access**](/perception/enma-lang/language-guide/inheritance.md) - public/private/protected, base clauses, virtual dispatch, multiple inheritance, both diamond forms, interfaces

[**Templates**](/perception/enma-lang/language-guide/templates.md) - Generic structs and functions, monomorphization, smart pointers

[**Compile-Time Evaluation**](/perception/enma-lang/language-guide/compile-time.md) - static\_assert, constexpr variables and functions, compile-time hashing

[**Exceptions**](/perception/enma-lang/language-guide/exceptions.md) - throw, typed catch clauses, unwinding, catchable null dereferences

[**Annotations**](/perception/enma-lang/language-guide/annotations.md) - noescape, packed, align, reflect, serialize, noopt, dll, custom

[**Modules & Namespaces**](/perception/enma-lang/language-guide/modules.md) - namespaces, import, .emb precompiled binaries, linking

[**Preprocessor**](/perception/enma-lang/language-guide/pre-processor.md) - #define, #ifdef, #include

[**Semantics & Limits**](/perception/enma-lang/language-guide/semantics-and-limits.md) - What is supported, what is not, and the exact rules

### SDK Guide

[**Quick Start**](/perception/enma-lang/sdk-guide/quick-start.md) - Minimal embed example

[**Engine Lifecycle**](/perception/enma-lang/sdk-guide/engine-lifecycle.md) - create, configure, destroy

[**Compile & Run**](/perception/enma-lang/sdk-guide/compile-and-run.md) - Compile, contexts, execute, arguments, return values, globals

[**Type Registration**](/perception/enma-lang/sdk-guide/type-registration.md) - Full type\_builder API with fields, methods, operators, subscript, iteration

[**Native Functions**](/perception/enma-lang/sdk-guide/native-functions.md) - Register host functions callable from scripts

[**Custom Addons**](/perception/enma-lang/sdk-guide/custom-addons.md) - Build your own addon from scratch

[**Modules & Artifacts**](/perception/enma-lang/sdk-guide/modules-and-artifacts.md) - .emb binaries, multi-module linking, hot reload

[**Introspection**](/perception/enma-lang/sdk-guide/introspection.md) - List functions, query annotations, IR dump

[**Memory & RAII**](/perception/enma-lang/sdk-guide/memory-and-raii.md) - Memory model, scope-drop, destructors, escape rules

[**Debug & Heap**](/perception/enma-lang/sdk-guide/debug-and-heap.md) - Debug hooks, execution budget, heap stats

[**Error Handling**](/perception/enma-lang/sdk-guide/error-handling.md) - Compile and runtime error reporting

[**Safety**](/perception/enma-lang/sdk-guide/safety.md) - Fault trapping, sandboxing, permissions, threading

[**API Reference**](/perception/enma-lang/sdk-guide/api-reference.md) - Every SDK function in one page

### Addons

[**Core**](/perception/enma-lang/addons/core.md) - Output (print functions for ints, floats, strings, bools, chars)

[**STD Library**](/perception/enma-lang/addons/std-library.md) - The auto-imported `std` prelude: strings, vector, array, list, map, set, any, optional, and the free algorithms

[**Math**](/perception/enma-lang/addons/math.md) - Trigonometry, power, rounding, float/int utilities, constants, random

[**Math:3D**](/perception/enma-lang/addons/math-3d.md) - vec2 / vec3 / vec4, quat, mat4 with operators + scalar helpers

[**SIMD**](/perception/enma-lang/addons/simd.md) - SSE2 vector arithmetic + packed ops on stride-1/2/4 arrays (int8/16/32, float32), bitwise

[**JSON**](/perception/enma-lang/addons/json.md) - parse, stringify, navigable `json_value` tree

[**Regex**](/perception/enma-lang/addons/regex.md) - matches, has\_match, first, find\_all, replace, split, groups

[**Filesystem**](/perception/enma-lang/addons/file.md) - `file_t` + free fns (gated by `PERM_FILE`)

[**Thread**](/perception/enma-lang/addons/thread.md) - mutex, lock\_guard, cond\_var

[**Atomic**](/perception/enma-lang/addons/atomic.md) - `atomic_int32` / `atomic_int64` + memory barriers

[**Bits**](/perception/enma-lang/addons/bits.md) - popcount, clz, ctz, rotl, rotr, bswap, parity, bit\_reverse

[**Time**](/perception/enma-lang/addons/time.md) - µs-since-epoch, calendar, ISO 8601, sleep, arithmetic
