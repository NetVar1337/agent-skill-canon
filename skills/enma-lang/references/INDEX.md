# Enma Language documentation index

Source index: https://docs.perception.cx/perception/llms.txt

Search these local references before relying on memory.

- [Introduction - Enma](readme.md)
- [Quick Access](quick-access.md)
- [Basics](language-guide/basics.md) — Types, variables, constants, operators, control flow
- [Functions](language-guide/functions.md) — Parameters, defaults, references, out params, variadic, lambdas, closures
- [Pointers](language-guide/pointers.md) — Heap pointers, address-of, member access, references, return-by-reference, pointer-to-member functions, null
- [Structs & Classes](language-guide/structs-and-classes.md) — Value types, constructors and destructors, initialization order, const members, operator overloading and layout control
- [Operators](language-guide/operators.md) — Operator overloading on structs and classes — the full operator set and how each dispatches
- [Inheritance & Access](language-guide/inheritance.md) — Access control, base clauses, virtual dispatch, multiple inheritance, both diamond forms, abstract classes and interfaces
- [Templates](language-guide/templates.md) — Generic structs and functions with compile-time monomorphization
- [Compile-Time Evaluation](language-guide/compile-time.md) — static\_assert and constexpr — evaluation that happens during compilation
- [Exceptions](language-guide/exceptions.md) — throw, typed catch clauses, unwinding, and catchable null dereferences
- [Annotations](language-guide/annotations.md) — Compiler annotations for layout, reflection, serialization, optimization, FFI, and custom metadata
- [Builtin Instructions](language-guide/builtin-instructions.md) — Every compiler builtin, with signatures — inline asm, atomics, varargs, bit\_cast, CPU features, vector math, and the 123 SIMD register intrinsics
- [Modules & Namespaces](language-guide/modules.md) — Namespaces, the import system with aliased imports, precompiled .emb binaries, and multi-module linking
- [Pre Processor](language-guide/pre-processor.md) — Macros, conditional compilation, and file inclusion
- [Semantics & Limits](language-guide/semantics-and-limits.md) — What the language guarantees, what it rejects at compile time, and what simply doesn't exist
- [Quick Start](sdk-guide/quick-start.md) — Minimal example to embed Enma in your application
- [Engine Lifecycle](sdk-guide/engine-lifecycle.md) — Create, configure, and destroy the Enma engine
- [Compile & Run](sdk-guide/compile-and-run.md) — Compile a script, create a context, run functions, pass arguments, and read and write globals
- [Type Registration](sdk-guide/type-registration.md) — Expose native types to scripts via type\_builder
- [Native Functions](sdk-guide/native-functions.md) — Register host functions callable from Enma scripts
- [Custom Addons](sdk-guide/custom-addons.md) — Build your own addon with native functions and custom types
- [Modules & Artifacts](sdk-guide/modules-and-artifacts.md) — Compile to .emb binaries, link multiple modules, and replace script code at runtime
- [Introspection](sdk-guide/introspection.md) — List functions, query annotations, and dump IR
- [Memory & RAII](sdk-guide/memory-and-raii.md) — Deterministic memory model - stack-first structs, scope-drop dtors, no GC
- [Debug & Heap](sdk-guide/debug-and-heap.md) — Debug hooks, execution budgets, heap stats, stack traces
- [Error Handling](sdk-guide/error-handling.md) — Compile-time and runtime error reporting
- [Safety](sdk-guide/safety.md) — Fault trapping, sandboxing, permissions, and thread safety
- [API Reference](sdk-guide/api-reference.md) — Complete listing of every SDK function
- [Core](addons/core.md) — Output, the tracked heap, raw memory, and the engine's own builtins
- [STD Library](addons/std-library.md) — The std prelude — strings, containers, any, optional and the free algorithms. Auto-imported into every script.
- [Math](addons/math.md) — Scalar math — trigonometry, powers and logs, rounding, comparison, interpolation, easing, classification, constants and random
- [Math:3D](addons/math-3d.md) — Vector, quaternion and matrix value types — vec2 / vec3 / vec4, quat, mat4
- [SIMD](addons/simd.md) — Vector kernels over std::vector — elementwise arithmetic, reductions, compares, byte operations, bitwise and memory
- [JSON](addons/json.md) — JSON parsing, navigation, mutation and stringifying through json\_value
- [Regex](addons/regex.md) — Regular expressions — match, find, replace, split and capture groups
- [Filesystem](addons/file.md) — File streams, whole-file reads and writes, and directory operations — permission-gated
- [Thread](addons/thread.md) — mutex, lock\_guard and cond\_var — synchronization primitives
- [Atomic](addons/atomic.md) — Atomic integer wrappers, a spinlock and memory barriers for values shared across threads
- [Bits](addons/bits.md) — Bit manipulation — popcount, leading/trailing zeros, rotates, byte swap, parity, bit reverse, single-bit and bit-range ops, power-of-two, alignment
- [Time](addons/time.md) — Timestamps, calendar accessors, ISO 8601, arithmetic and sleep
