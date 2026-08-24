> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/native-functions.md).

# Native Functions

Expose host functions to scripts. The sig string drives compile-time type checking at every call site and per-arg ABI routing at the native boundary.

## Registering a Function

Declare the native with real types. `register_native` passes the function through directly - the Win64 ABI placement follows the signature (int-like args in `rcx`/`rdx`/`r8`/`r9`, floats in `xmm0-3`, args 5+ on stack, narrow-int returns sign/zero-extended).

```cpp
int32_t add(int32_t a, int32_t b) { return a + b; }
register_native(e, "int32 add(int32, int32)", (void*)&add);
```

## Attaching a Description

Every `register_native` overload takes an optional trailing `description` string, surfaced via `extract_documentation` and `extract_intellisense` (see [Introspection](/perception/enma-lang/sdk-guide/introspection.md)). Use it for natives whose behavior isn't obvious from the signature alone.

```cpp
register_native(e, "int64 log(const char* msg)", (void*)&log,
    0 /* no permissions */,
    "print a message to the host's stdout");
```

Descriptions are optional. If omitted, the entry still appears in doc output without a comment.

```cpp
double lerp(double a, double b, double t) { return a + (b - a) * t; }
register_native(e, "float64 lerp(float64, float64, float64)", (void*)&lerp);
```

Script usage:

```cpp
int32 r = add(10, 20);              // 30
float64 m = lerp(0.0, 100.0, 0.5);  // 50.0
```

Any arity, any type mix (ints / bools / chars / floats / doubles / pointers / enums / registered structs / `string` / `array` / `map`) works. The native is called directly - no int64 bit-casting, no trampolines.

There's also a template form `register_typed<&fn>(e, sig)` - equivalent to `register_native(e, sig, (void*)&fn)` but captures the function as a template parameter so the binding is one token. Pick whichever reads better.

## Supported Types

Keywords resolve to the built-in type IDs:

| Category    | Keywords                                                                                 |
| ----------- | ---------------------------------------------------------------------------------------- |
| Integers    | `int8`, `int16`, `int32`, `int64`, `uint8`, `uint16`, `uint32`, `uint64`, `char`, `bool` |
| Floats      | `float32`, `float`, `float64`, `double`                                                  |
| Strings     | `string`, `wstring`                                                                      |
| Containers  | `array`, `map`                                                                           |
| Generic     | `struct`, `class`, `pointer`, `lambda`, `function`, `void`                               |
| Placeholder | `element` (in container-method signatures)                                               |

Structs, enums, and types registered via `struct_builder`, `type_builder`, or `enum_builder` are also recognized by name - see the [Custom Types in Signatures](#custom-types-in-signatures) section below.

## Floats

Declare your C function with `float` or `double`. The compiler routes those args through xmm regs per Win64 ABI, and reads float returns from xmm0:

```cpp
double my_sqrt(double x) { return std::sqrt(x); }
register_native(e, "float64 my_sqrt(float64)", (void*)&my_sqrt);
```

```cpp
float mix(float a, float b, float t) { return a + (b - a) * t; }
register_native(e, "float32 mix(float32, float32, float32)", (void*)&mix);
```

Mix int and float args freely - placement is per-position:

```cpp
void draw(int32_t id, double x, double y, int32_t color) { ... }
register_native(e, "void draw(int32, float64, float64, int32)", (void*)&draw);
//                            ^rcx    ^xmm1    ^xmm2     ^r9
```

## Strings

String arguments pass as `const char*`:

```cpp
void greet(const char* s) {
    printf("Hello, %s!\n", s);
}
register_native(e, "void greet(const char*)", (void*)&greet);
```

## No-Parameter Functions

```cpp
int64_t get_time() {
    return static_cast<int64_t>(time(nullptr));
}
register_native(e, "int64 get_time()", (void*)&get_time);
```

## Custom Types in Signatures

After registering a struct, enum, or `type_builder` type, you can reference it by name in subsequent registration calls. Pass struct args as typed pointers:

```cpp
struct_builder(e, "proc_t")
    .field("pid",      type_id::t_int64)
    .field("priority", type_id::t_int64);

struct proc_t { int64_t pid; int64_t priority; };

int64_t inspect_proc(proc_t* p) {
    return p->pid;
}

register_native(e, "int64 inspect_proc(proc_t)", (void*)&inspect_proc);
```

Script usage:

```cpp
proc_t p;
p.pid = 42;
int64 x = inspect_proc(p);  // 42
```

Passing a different struct is a compile error:

```cpp
item_t i;
inspect_proc(i);   // error: inspect_proc() parameter 'arg0' expects proc_t, got item_t
```

The sig accepts `T`, `T&`, `T*` with optional parameter name. The native always receives a pointer:

| Signature | Semantics    | Native sees                              |
| --------- | ------------ | ---------------------------------------- |
| `T`       | by-value     | pointer to a copy of the caller's struct |
| `T&`      | by-reference | pointer to the caller's struct           |
| `T*`      | by-pointer   | pointer to the caller's struct           |

Mutations through `T&` or `T*` are visible to the caller; mutations through bare `T` are not.

```cpp
int64_t mutate_proc(proc_t* p) {
    p->pid += 100;
    return p->pid;
}
```

```c
// sig "int64 mutate_proc(proc_t)" by-value: p.pid stays 5
// sig "int64 mutate_proc(proc_t&)" by-ref: p.pid becomes 105
proc_t p;  p.pid = 5;
int64 r = mutate_proc(p);
```

By-value copies the struct fields at the call site (one load/store per 8 bytes).

Enums resolve to `int64` at the ABI level, but the compiler enforces enum identity at call sites:

```cpp
enum_builder(e, "Priority")
    .value("Low",    1)
    .value("Medium", 5)
    .value("High",  10);
enum_builder(e, "Color")
    .value("Red", 0).value("Green", 1).value("Blue", 2);

register_native(e, "int64 enum_double(Priority)", (void*)&enum_double_fn);
```

```cpp
int64 x  = enum_double(Priority::High);   // OK → 20
int64 y  = enum_double(42);               // compile error: expects Priority, got int32
int64 z  = enum_double(Color::Red);       // compile error: expects Priority, got Color

Priority p = Priority::Medium;
int64 w  = enum_double(p);                // OK, local is typed Priority
```

Raw ints and values from a *different* enum are rejected at compile time. Declaring a local with the enum type (`Priority p = ...;`) preserves the enum identity through the rest of the scope.

Script-side enums work too:

```cpp
enum State { Idle = 0, Running = 1, Done = 2 }
```

```cpp
register_native(e, "int64 on_state(State)", (void*)&on_state);
```

The same call-site checks fire whether the enum was registered via `enum_builder` or declared in the script.

## Delegate-Typed Parameters

Script-declared delegates work directly in native signatures:

```cpp
delegate int64 Handler(int64 x);
```

```cpp
int64_t take_handler(void* fn_ptr) {
    return fn_ptr != nullptr ? 1 : 0;
}

register_native(e, "int64 take_handler(Handler)", (void*)&take_handler);
```

```cpp
int64 doubler(int64 x) { return x * 2; }
int32 main() {
    Handler h = doubler;
    return take_handler(h);   // 1
}
```

Delegate names resolve at IR-build time (after the script is parsed), so registrations that reference delegates declared *later* in the script still work. The compiler will reject mismatched arg types at the call site (`take_handler("hello")` → compile error).

## Overloading

Register two natives with the same name but different **arities** or different **argument types** and they coexist:

```cpp
register_native(e, "int64 combine(int64)",                       (void*)&combine_1);
register_native(e, "int64 combine(int64, int64)",                (void*)&combine_2);
register_native(e, "int64 combine(int64, int64, int64)",         (void*)&combine_3);

register_native(e, "int64 pairsum(int64, int64)",                (void*)&int_sum);
register_native(e, "float64 pairsum(float64, float64)",          (void*)&float_sum);
```

```cpp
combine(5);            // → combine_1
combine(3, 4);         // → combine_2
combine(1, 2, 3);      // → combine_3
combine(1, 2, 3, 4);   // compile error: no overload for 4 arg(s)

pairsum(3, 4);         // → int_sum (int args → int overload)
pairsum(1.5, 2.5);     // → float_sum (float args → float overload)
pairsum("a", "b");     // compile error: no overload matches string
```

Resolution rules:

1. **Exact type match** wins outright — an overload whose parameter types match the argument types exactly is chosen with no further comparison.
2. **Scored compatible match**: otherwise iterate overloads with matching arity and pick the closest. A same-category conversion (int→int64, float→float64) is cheaper than a cross-category one (int→float), so the same-category candidate wins.
3. **Ambiguous tie**: if two overloads have the same score, emit *"call is ambiguous across overloads"* and fail to compile.
4. **No compat match**: emit *"has no overload matching N argument(s) with given types"*.

Same arity + same types = the second registration clobbers the first.

## Generic Types in a Signature

A signature can name any generic the script declares, written as a complete template-id. That covers `std::vector<T>`, your own generics, and qualified, multi-argument or nested ones:

```cpp
register_native(e, "int64 sum_ints(std::vector<int64>)",      (void*)&sum_ints);
register_native(e, "void  get_ints(std::vector<int64>& out)", (void*)&get_ints);
register_native(e, "int64 take_box(Box<int64>)",              (void*)&take_box);
register_native(e, "int64 take_pair(n::PairBox<Box<int64>, int32>)", (void*)&take_pair);
```

The type is resolved from its declaration, so a template-id in a signature denotes the same instantiation the same spelling denotes in a script — and a type of your own named `vector` denotes itself, not the library's. Naming a generic instantiates it, so it does not have to appear anywhere in the script.

A signature naming a type that does not exist is refused, and the refusal names the token:

```
native `int64 bad(Missing<int64>)` was not registered:
    arg 0 `Missing<int64>` names no known type
```

The refusal applies to the compile that could not resolve it; the registration is judged again against the next script compiled on the same engine.

### `std::vector<T>` specifics

Script-side mismatches reject at compile:

```cpp
std::vector<int64> xs;        sum_ints(xs);   // OK
std::vector<std::string> ys;  sum_ints(ys);   // error: sum_ints() parameter 'arg0'
                                              // expects std::vector<int64>, got std::vector<std::string>

std::vector<int64> out;
get_ints(out);                                 // fill via the out-param
```

The native receives the vector **value struct** (24 bytes). Its layout is private — always go through the `enma::vec_*` helpers from `sdk.h`: `vec_view(v)` / `vec_size(v)` / `vec_data<T>(v)` / `vec_at<T>(v, i)` to read, `vec_assign<T>(out, src, n)` to fill an out-param (string elements: `vec_str_at` / `vec_assign_strs`). Returning a fresh vector **by value** from a native is not supported — take a `std::vector<T>& out` parameter and `vec_assign` into it.

## `const` Parameters

Mark a param as read-only:

```cpp
register_native(e, "int64 read_pid(const proc_t)", (void*)&read_pid);
```

Script-side: assigning to a field of a `const` arg inside that fn rejects at compile (`cannot assign through const ...`). Combine with `const T&` for non-mutating reference args.

## Variadic Natives

End the arg list with `...` to accept any number of additional args:

```cpp
int64_t vsum(int64_t argc, int64_t* argv) {
    int64_t s = 0;
    for (int64_t i = 0; i < argc; ++i) s += argv[i];
    return s;
}

register_native(e, "int64 vsum(...)", (void*)&vsum);
```

```cpp
vsum(1, 2, 3, 4, 5);   // 15
vsum();                // 0, no args
```

The native function takes `(int64_t argc, int64_t* argv)`. `argv` is a heap buffer of raw int64 values for every passed arg (including formal-arg prefix). Freed after the call.

Formal args may precede `...`:

```cpp
register_native(e, "int64 vlog(const char* fmt, ...)", (void*)&vlog);
```

```cpp
vlog("hello");              // argc=1, argv[0]=string_ptr("hello")
vlog("%d + %d", 3, 4);      // argc=3, argv=[fmt_ptr, 3, 4]
vlog();                     // compile error: expects at least 1 arg
```

The formal args are still type-checked. The variadic portion accepts any value and is always int64-boxed - use `memcpy(&d, &argv[i], 8)` to recover float args as doubles from the raw buffer.

## Default Arguments

Give natives default values via `= literal` in the sig:

```cpp
register_native(e, "int64 sum3(int64 a, int64 b = 10, int64 c = 100)", (void*)&sum3);
```

```c
sum3(1);         // b = 10, c = 100  → 111
sum3(1, 2);      // c = 100          → 103
sum3(1, 2, 3);   // all explicit     → 6
sum3();          // error: expects 1-3 argument(s), got 0
```

Int and float literals are supported (e.g. `int64 n = 42`, `float64 f = 3.14`). All defaulted args must come after all required args.

## Returning a Struct by Value

Struct returns write into a caller-supplied return slot: the native takes a hidden first arg pointing at the caller's buffer:

```cpp
struct proc_t { int64_t pid; int64_t priority; };

void make_proc(proc_t* out) {
    out->pid = 42;
    out->priority = 7;
}

register_native(e, "proc_t make_proc()", (void*)&make_proc);
```

```c
proc_t p = make_proc();   // Enma allocates the buffer, native fills it
int64  x = p.pid;         // 42
```

The first native arg is the return-slot pointer (matches the standard ABI for structs larger than a register).

## Permission-Gated Functions

Restrict a function to scripts that have specific permissions:

```cpp
register_native(e, "int64 read_sensor(int64, int64, int64)", (void*)&read_sensor_fn, PERM_FFI);
```

On a `type_builder` method, chain `.permission(...)`:

```cpp
type_builder(e, "socket_t", ...)
    .method("int64 send_raw(int64)", (void*)&send_raw_fn).permission(PERM_FFI)
    .finish();
```

Calling a permission-gated function from a script without the matching bit in `set_permissions(engine, ...)` is a compile error.

## Registration-Time Validation

If the sig string references a type that hasn't been registered (typo like `proc_T` for `proc_t`), Enma prints a warning to stderr at registration time:

```cpp
[enma] warning: arg 0 'proc_T' in sig 'int64 inspect(proc_T)' - unresolved type; it will accept any value
```

Catches type-name typos at engine setup rather than at script call time.

## Compile-Time Syntax Validation. `ENMA_SIG(...)`

For structural errors in the sig string (missing paren, empty arg slot, illegal characters), wrap the literal in `ENMA_SIG(...)` to fail at **host compile time** via a `consteval` check:

```cpp
register_native(e, ENMA_SIG("int64 add(int64, int64)"), (void*)&add_fn);  // compiles
register_native(e, ENMA_SIG("int64 bad(,)"),            (void*)&bad_fn);  // static_assert fires
register_native(e, ENMA_SIG("int64 no_close("),         (void*)&fn);      // static_assert fires
```

The checker catches:

* Missing `(` or `)`, or trailing junk after `)`.
* Empty function name (`"() foo"`).
* Empty arg segments: `"bad(,)"`, `"bad(int,,int)"`.
* Non-identifier characters (anything outside `[A-Za-z0-9_&*=.+-]`).

`...` is accepted as a variadic arg. Type-name typos (`"int64 f(int6q)"`) aren't caught here - that's the registration-time stderr warning above. Use `ENMA_SIG` for the syntax-only layer.

## Calling script code from a background thread

A native that calls a script-side closure from a thread not already driving `execute()` / `call()` must set up per-thread state first, or the first native touching TLS dereferences nullptr. `execution_scope` does it:

```cpp
void worker_tick(context_t* ctx, int64_t fn_handle) {
    execution_scope scope(ctx);   // per-thread heap, engine, rng, code range
    // ... invoke the closure ...
}
```

[Custom Addons](/perception/enma-lang/sdk-guide/custom-addons.md#invoking-script-closures-from-background-threads) has the full trampoline pattern.

## Signature-string features

* **Arity + type overloading**: register multiple natives with the same name; call site picks best match. Includes element-type dispatch for typed containers.
* **Variadic**: end with `...` to pass `(int64_t argc, int64_t* argv)` to the native.
* **Default args**. `"int64 f(int64 a, int64 b = 10)"`.
* **Struct args by value / ref / ptr**. `T`, `T&`, `T*`.
* **Const params**. `const T`, `const T&` rejects assignment-through-const in callee.
* **Typed containers**. `array<T>`, `map<K, V>` checked at script call sites and var-decl assignments.
* **Enum-typed args**: compile error on raw int or cross-enum.
* **Delegate names**: script-declared delegates resolved lazily.
* **Custom struct / type\_builder names**: compile error on mismatched name.
