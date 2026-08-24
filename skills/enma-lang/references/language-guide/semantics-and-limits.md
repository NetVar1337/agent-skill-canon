> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/semantics-and-limits.md).

# Semantics & Limits

Single-page answer to "can I do X?" grouped by area, with the verdict and where the rule lives.

## Pointers

Pointers are C++ pointers: address-of, dereference, arithmetic, comparisons, and the cast family all behave per the standard. The one addition is escape analysis — a stack object's address cannot outlive the object.

| Operation                                                   | Allowed | Notes                                                                                         |
| ----------------------------------------------------------- | :-----: | --------------------------------------------------------------------------------------------- |
| `T* p = new T(args);` / `delete p;`                         |    ✓    | Heap allocation with ctor; manual free                                                        |
| `T* p = new T[N];` / `delete[] p;`                          |    ✓    | Contiguous block, per-element ctor/dtor                                                       |
| `p->field`, `(*p).field`                                    |    ✓    | Use `->` on pointers, `.` on values                                                           |
| `T* q = p;`, `T* q = null;`                                 |    ✓    | Copies / null literal                                                                         |
| `&local`, `&arr[0]`, `&obj.field`                           |    ✓    | Address-of; array decay via `&a[0]`                                                           |
| `p + n`, `p - n`, `++p`, `p[i]`                             |    ✓    | `p[i]` **is** `*(p + i)` (\[expr.sub]); strides by element size                               |
| `q - p`                                                     |    ✓    | Element count (\[expr.add])                                                                   |
| `p < q`, `p == q`, …                                        |    ✓    | Relational + equality                                                                         |
| `int64 (*p)[N] = &arr;`                                     |    ✓    | Pointer-to-array (\[dcl.ptr]): `p[j][i]`, `(*p)[i]`, `++p` strides whole rows. Function-local |
| `reinterpret_cast<int64>(p)` / `reinterpret_cast<T*>(addr)` |    ✓    | The pointer ↔ integer round-trip                                                              |
| `static_cast<bool>(p)`                                      |    ✓    | The only `static_cast` a pointer source allows (C++)                                          |
| `p * 2`, `p / 2`                                            |    ✗    | Compile error (no such C++ operation)                                                         |
| `p == someInt`                                              |    ✗    | Compile error; compare against `0`/`null` or `reinterpret_cast`                               |
| `int64* p = someInt;`                                       |    ✗    | Compile error; `reinterpret_cast<T*>(...)` for an integer address                             |
| `T x = new T();`                                            |    ✗    | Pick stack (`T x;`) or heap (`T* x = new T();`)                                               |
| `delete` on non-pointer                                     |    ✗    | Compile error                                                                                 |
| Double `delete` / `delete null;`                            |    –    | Safe no-ops on the tracked heap                                                               |
| Storing `&local` in a global/field                          |    ✗    | Compile error (would dangle)                                                                  |
| Returning a pointer to a local                              |    ✗    | Compile error (would dangle)                                                                  |
| Escaping closure capturing a stack struct                   |    ✗    | Compile error                                                                                 |

The escape analyzer is conservative: if it can't prove the pointer is safe, it rejects. Moving to `new T()` is the escape hatch.

## Memory model

| Feature                                    | Status                                                                                                                              |
| ------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------- |
| Stack-allocated structs (`T x;`)           | Default for value types                                                                                                             |
| Heap-allocated structs (`T* x = new T();`) | Explicit via `new` / `delete`                                                                                                       |
| RAII / scope-drop                          | Structs, classes, and the `std` containers run `~T()` deterministically at scope exit (normal flow, exception unwind, fault unwind) |
| Tracing garbage collector                  | **None**. Cleanup is deterministic.                                                                                                 |
| Double-free / `delete null`                | Safe no-ops on the tracked heap                                                                                                     |
| Use-after-free                             | Detected on a best-effort basis; `execute()` returns `false`                                                                        |
| Budget (instruction / memory)              | Opt-in per module (`set_budget` / `set_memory_budget`)                                                                              |

## Conversions (\[conv.arith] — exactly C++)

| Behavior                      | Rule                                                                     |
| ----------------------------- | ------------------------------------------------------------------------ |
| Usual arithmetic conversions  | Operands promote and convert per C++; results wrap at the promoted width |
| Signed ↔ unsigned (any width) | **Implicit**, value wraps (`uint32 u = -1;` is C++)                      |
| `float → int`                 | **Implicit**, truncates toward zero                                      |
| `float64 → float32`           | **Implicit**, rounds to nearest float32                                  |
| `int64 → int32` (narrowing)   | **Implicit**, wraps at 32 bits                                           |
| `int → float`                 | Implicit                                                                 |
| Unscoped `enum → int`         | Implicit (\[dcl.enum])                                                   |
| `enum class → int`            | **Compile error** without `static_cast` (\[dcl.enum]/10)                 |
| `function → int64`            | Implicit (closure handle) — pass script fns to `int64` native params     |
| Division by zero              | Runtime trap                                                             |
| Shift ≥ bit-width             | x64 semantics (count modulo width)                                       |
| NaN / Infinity                | Standard IEEE-754                                                        |
| `1.5f` / `1.5F` literal       | float32 literal at parse time; bare `1.5` is float64                     |

**Casts** — the C++ family: `static_cast<T>(x)` (value conversions, user `operator T()`, enum↔int; a pointer source only converts to `bool`), `reinterpret_cast<T>(x)` (pointer ↔ integer, pointer reinterpretation), `const_cast<T*>(p)`, `dynamic_cast<T*>(p)` (hierarchy casts), and C-style `(T)x`.

## Exact-width storage

`int8/int16/int32/float32` load and store at their natural width everywhere — locals, fields, builtin arrays (byte-packed), `new T[N]` blocks — so decayed pointers walk the same bytes natives see. Narrow signedness follows the spelling (`int8`/`uint8` alias `char` semantics at width 1, `int16`/`uint16` at width 2).

## Type system

| Feature                                                | Supported                                                                                                                                                                                                                                                                                                                                                              |
| ------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Structs (value type, stack-default)                    | ✓                                                                                                                                                                                                                                                                                                                                                                      |
| Classes (reference type with vtable)                   | ✓                                                                                                                                                                                                                                                                                                                                                                      |
| Single inheritance + virtual methods                   | ✓ (classes; structs don't vtable unless marked)                                                                                                                                                                                                                                                                                                                        |
| Multiple inheritance (C++-style)                       | ✓ — ctor/dtor chain in declaration / reverse order; this-adjusted dispatch; vtable thunks for override-via-non-primary-base. A non-virtual diamond gives two independent base subobjects; `virtual` bases share one ([Inheritance](/perception/enma-lang/language-guide/inheritance.md#diamonds))                                                                      |
| Templates (`template<typename T>`)                     | ✓ (fn + class templates, partial/explicit specialization, value params, \[temp.func.order] partial ordering)                                                                                                                                                                                                                                                           |
| Variadic templates (`typename... Ts`)                  | ✓ — pack expansion in call args and brace lists (`{args...}`, including `int64 xs[] = {args...}` with deduced bound), recursion, `sizeof...(args)`, fold expressions (`(args + ...)`)                                                                                                                                                                                  |
| CTAD (`Box b(5)` deducing `Box<int64>`)                | ✓ — steered where needed by an explicit deduction guide (`Box(T) -> Box<T>;`)                                                                                                                                                                                                                                                                                          |
| Concepts / `requires` clauses                          | ✓ — concept-ids, `&& \|\| !`, boolean literals and `requires(T x){ x.member; x.method(); }`; leading, trailing and type-constraint spellings, each carrying its argument list. No `requires`-expression in expression position, no type or compound requirements, no subsumption ordering ([Templates](/perception/enma-lang/language-guide/templates.md#constraints)) |
| Template-template parameters                           | ✓ — deduced from the argument; the argument's parameter list must match exactly                                                                                                                                                                                                                                                                                        |
| `decltype(expr)`                                       | ✓                                                                                                                                                                                                                                                                                                                                                                      |
| Designated initializers (`{.x=1, .y=2}`)               | ✓                                                                                                                                                                                                                                                                                                                                                                      |
| Aggregate init (\[dcl.init.aggr])                      | ✓ — nested rows for 2D arrays, brace elision, braces-around-scalar, zero-fill                                                                                                                                                                                                                                                                                          |
| User-defined literals (`42_km`)                        | ✓ (rewritten to `_km(42)` at parse)                                                                                                                                                                                                                                                                                                                                    |
| RTTI                                                   | `dynamic_cast` ✓; `typeid(T)` / `typeid(expr)` ✓ (stable id; name via the host)                                                                                                                                                                                                                                                                                        |
| rvalue references / move semantics                     | ✓ — `T&&` params, move ctor / move assignment selected on rvalues, `move(x)`                                                                                                                                                                                                                                                                                           |
| `const` correctness                                    | ✓ — const locals/params/globals, const methods + receivers, **const data members** (\[dcl.type.cv] write gates, implicitly-deleted copy-assign)                                                                                                                                                                                                                        |
| Fixed-size arrays (`T buf[N]`)                         | ✓ — 1D any scope, primitive or struct elements, with full \[dcl.init.aggr] brace init and whole-element assignment; 2D (`T m[N][M]`) frame-local (struct elements: brace init + `[i][j].field` access; a whole-element assign rejects); `T buf[] = {…}` deduces the bound                                                                                              |
| Modules (`import`) / Namespaces / `using`              | ✓                                                                                                                                                                                                                                                                                                                                                                      |
| Operator overloading                                   | ✓ (script structs + addon types)                                                                                                                                                                                                                                                                                                                                       |
| Exceptions (`try` / `catch` / `throw`)                 | ✓ (typed catch clauses, derived-to-base handler matching \[except.handle])                                                                                                                                                                                                                                                                                             |
| Match / ternary / delegates / defer                    | ✓                                                                                                                                                                                                                                                                                                                                                                      |
| Lambdas / closures                                     | ✓ (closure escape rules enforced)                                                                                                                                                                                                                                                                                                                                      |
| Properties / `std::optional` / out-of-line member defs | ✓                                                                                                                                                                                                                                                                                                                                                                      |
| Structured bindings (`auto [k, v]`)                    | ✓ — by value and by reference; map iteration                                                                                                                                                                                                                                                                                                                           |

## Unsafe operations (rejected at compile time)

These produce a compile error, not a runtime fault:

* `.` or `->` on a non-class left operand — `f().field` where `f()` returns `int64`. The error names the member and the base's type; in a chain it names the step applied to the non-class base, so `f().a.b` reports `a`
* Assigning to a name that declares nothing — `ghost = 1`, `ghost.field = 1`, `ghost[0] = 1`. Ordinary unqualified lookup decides, so inside a member function a bare name still reaches a field, a base's field, a static member or a global; a name that reaches none of those is an error where it is written. A name that *does* resolve but is not an object says so instead: a function, a type, an enumerator, a `constexpr` binding, or a member named where there is no object (a static member function)
* Pointer multiplication / division (`p * 2`)
* Comparing a pointer against an integer (use `0`/`null` or `reinterpret_cast`)
* Raw integer → pointer assignment without `reinterpret_cast`
* Escaping a stack local's address (into a global, a field, a return, or a closure)
* Calling a non-`const` method on a `const` receiver
* Writing through a `const` object, parameter, or **const data member** outside construction
* Scoped-enum → arithmetic without `static_cast`
* Native call with wrong-typed arg / wrong struct identity / wrong enum identity
* Template method call with wrong element/key/value type
* A struct value crossing a boundary as the wrong type — into a parameter, out of a `return`, or through the pointer a `new` initializes. It has to be that type, a class derived from it, or a name that aliases it. Two instantiations of one template are two types, so `vector<float64>` does not bind a `vector<int64>` parameter, and CTAD deducing `Box<int32>` does not initialize a `Box<int64>*`
* A class-to-class user conversion used as a function argument — it is not applied there; convert at the call site
* `T name();` inside a function body — that spelling declares a function taking no arguments. Write `T name;` or `T name = T();`
* `explicit` on anything but a constructor or a conversion function; `override` before the return type instead of after the parameter list
* `alignas` on a variable, a parameter or a function — it applies to a type or a data member, and is refused rather than dropped
* A constraint whose form is not decidable — an arithmetic or relational expression, a trait, a `sizeof` — is reported at the use rather than assumed either way
* Two differently constrained candidates that both apply: ambiguous, since constraints carry no subsumption order
* A template-template argument whose own parameter list does not match the parameter's, or a plain type where a class template is required
* `typename` on an unqualified name — it applies to a qualified name only
* A namespace alias whose target is not a declared namespace, including an alias of an imported module: a module and a namespace are different kinds of entity
* `new` on an unknown type; `delete` on a non-pointer; `T x = new T();`
* A declaration naming a type that does not exist — a local, a struct field, a field's pointee, a function parameter, or a return type. The error names the position and the type. A template's own parameters are dependent and are checked when it is instantiated, so `f<Missing>(x)` reports the type at the instantiation
* Excess initializers anywhere in an aggregate init (\[dcl.init.aggr])
* `[[noescape]]` violation; missing/ambiguous overload; permission-gated native without the grant

## Compiler limits

| Limit                      | Value                                                                                                                             |
| -------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| Expression nesting depth   | 24 — a call inside a call, a chain of unary operators, a ternary tree. Chained arithmetic and parentheses do not count against it |
| `#if` expression nesting   | 2048                                                                                                                              |
| `constexpr` call recursion | 256                                                                                                                               |

A program past the first two gets a compile error naming the limit; past the third, the `constexpr` initializer is reported as not constant. Bind a subexpression to a local to nest further. None of these bounds a *runtime* recursion — that is the context's own stack, sized by the host.

## Runtime traps

Caught at runtime and reported via `execute()` returning `false`:

* Null deref through a pointer (script-catchable via `try`/`catch (std::runtime_error& e)` when using `->`; see [Catching runtime faults](/perception/enma-lang/language-guide/exceptions.md#catching-runtime-faults))
* An out-of-bounds access wild enough to hit unmapped memory — near-miss indexing is UB exactly like C++ (`p[i]`, `a[i]`, `v[i]` carry no bounds checks; `v.at(i)` / `s.at(i)` throw instead)
* Division by zero
* Stack overflow
* Use-after-free, where it can still be detected

Inside a native function: **not trapped.** Validate your inputs before dereferencing; use `heap_is_tracked(ptr)` for heap-allocated values.

## Permissions

A script cannot call a permission-gated native unless the permission was granted via `set_permissions(engine, flags)`:

| Flag        | Value  | Gates                       |
| ----------- | ------ | --------------------------- |
| `PERM_FFI`  | `0x01` | `[[dll(...)]]` extern decls |
| `PERM_FILE` | `0x02` | All file-addon operations   |

The check happens at module compile. A precompiled module carries the set its calls require and is checked again when it loads.

## Thread safety

* Each engine is independent; no shared mutable state across engines
* Multiple contexts off the same module run concurrently
* Per-thread heap (cleaned up when the thread exits)
* The `thread` addon provides mutex / cond\_var / lock\_guard. Native functions that touch Enma's heap must be called on a thread with an active context; raw mutex operations don't need one
* Tested under ASAN and TSAN with 32+ concurrent threads

## Things that simply don't exist

* **Module system beyond `import`** — no separately-compiled binary modules at the language level (the SDK's `link()` joins modules)
* **Async / await / futures / coroutines** — use the thread addon
* **Networking addon** — none is provided; write one as a standalone addon
* **Custom allocators per scope** — one heap per thread, not pluggable
* **Built-in annotations**: `[[inline]]`, `[[noinline]]`, `[[noopt]]`, `[[dll]]`, `[[packed]]`, `[[align]]`, `[[serialize]]`, `[[reflect]]`, `[[noescape]]`. Custom names parse and are host-queryable but carry no compiler semantics
* **Inline assembly** — fixed whitelist of intrinsics only: `__asm_rdtsc`, `__asm_pause`, `__asm_mfence`, `__asm_nop`, plus the `__simd_*` builtins behind the `simd` module. Arbitrary user asm is not supported
