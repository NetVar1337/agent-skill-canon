> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/exceptions.md).

# Exceptions

`throw` unwinds to the nearest matching `catch`. Destructors and `defer` blocks run during unwinding, not just on normal scope exit.

```c
try {
    if (x < 0) { throw -1; }
    process(x);
} catch (int64 e) {
    println("error code: " + std::to_string(e));
}
```

A thrown integer literal arrives as `int64`, so that is the handler type to write for one.

## Typed catch clauses

`catch (T t)` fires only when the thrown value's type matches. A `try` block may chain several typed clauses; the first whose declared type matches runs. Handler matching follows \[except.handle], so a base-class clause catches a derived throw.

```c
struct NetErr {
    int32 code;
    std::string msg;
    NetErr(int32 c, std::string m) { this->code = c; this->msg = m; }
}

try {
    throw NetErr(503, "timeout");
} catch (NetErr e) {
    println(std::to_string(e.code));     // 503
    println(e.msg);                      // "timeout"
} catch (std::string e) {
    println(e);
} catch (int64 e) {
    println(std::to_string(e));
}
```

`throw T(args)` copies the struct value into the exception object. `throw new T(args)` is a different thing, not a storage tweak: it **transports the pointer**, so the thrown type is `T*` and the handler must be `catch (T* e)`.

Pointer handlers match on the same terms as class handlers — `catch (Base* e)` catches a thrown `Derived*` — and the qualification conversion goes one way only: `catch (const T*)` catches a thrown `T*`, while `catch (T*)` never catches a thrown `const T*`. That is why `catch (const char*)` catches `throw "x"` and `catch (char*)` does not.

## catch (...) and rethrow

`catch (...)` matches anything. `throw;` inside a handler rethrows the exception it is handling.

```c
try {
    risky();
} catch (NetErr e) {
    if (e.code < 500) { throw; }   // not ours — rethrow unchanged
    recover();
} catch (...) {
    cleanup();
}
```

A `catch (T e)` copy-initializes its parameter from the exception object, so mutating `e` never reaches a rethrown object; `catch (T& e)` binds it directly. A base handler catches a derived throw by the ordinary derived-to-base rule — but a base that is **inaccessible** from the derived type does not match, because access is part of the rule.

`throw` is also an **expression** of type void at assignment-expression precedence, so it may appear as either operand of `?:`:

```c
return n > 0 ? n : throw std::invalid_argument("n must be positive");
```

An uncaught exception surfaces to the host as an execute error carrying the message.

## Exception types

The hierarchy is ordinary library classes in the `std` prelude. `what()` is virtual, so it reports the thrown type's own message.

```
std::exception                virtual ~exception(), virtual what()
├── std::bad_alloc            allocation failed / refused by the budget
├── std::bad_optional_access  value() on a disengaged optional
├── std::bad_cast ─────────── std::bad_any_cast
├── std::logic_error ──┬───── std::length_error     (a size that cannot exist)
│                      ├───── std::out_of_range     (an argument outside range)
│                      ├───── std::domain_error
│                      └───── std::invalid_argument
└── std::runtime_error ┬───── std::range_error
                       ├───── std::overflow_error
                       ├───── std::underflow_error
                       └───── std::format_error     (a bad format string)
```

`logic_error` and `runtime_error` — and every type derived from them — take either `X()` for the class's own default text or `X(msg)` with a `std::string` or `const char*`. Each owns a copy, which `what()` returns.

```c
try {
    v.at(99);
} catch (std::out_of_range e) {
    println(e.what());
}
```

Prelude throws you will meet: `at()` on vector, array, string and wstring raises `std::out_of_range` for an index outside the extent, as does `map::at` / `unordered_map::at` for a missing key. A malformed format string, or too few arguments for one, raises `std::format_error`.

## Exception specifications

`noexcept` and `noexcept(cond)` attach to a function, method, constructor, lambda, `delegate` declaration or function-pointer declarator — the specification is part of the function **type**, so every carrier accepts one.

```c
int64 pure() noexcept { return 1; }
template<typename T> void put(T v) noexcept(sizeof(T) <= 8) { }
```

The condition is a constant expression evaluated once the whole file is known, not at parse time: it may name a `constexpr`, compute (`sizeof(T) > 4`), depend on a template argument, or name a function declared further **down** the file. A condition that still cannot be evaluated is reported.

**An escaping exception terminates the run.** The frame's own locals and `defer` blocks are destroyed first — that funnel is where the promise breaks — and then no handler may take it: not a typed `catch`, not `catch (...)`, not one in an enclosing frame. It reaches the host as a `terminated:` error, distinct from `unhandled exception:`.

Other rules:

* Every declaration of one function must give the same specification.
* An overriding function may not allow what the one it overrides forbids; a stricter override is fine. The specification takes no part in deciding *which* function is overridden — that is signature identity.
* A non-throwing function binds a carrier that allows throwing; the reverse is refused, at initialization, assignment and argument position.
* It is published in the `.emb` export table, so a separately compiled caller re-declares it intact.

`noexcept(expr)` is also an **operator** — a compile-time `bool`, true when the operand cannot throw. The operand is unevaluated, like `sizeof`'s. A call whose callee cannot be resolved counts as throwing, which is the safe direction. That is what makes "throws exactly when `f` does" expressible:

```c
void g() noexcept(noexcept(f())) { f(); }
```

A destructor with no written specification is implicitly non-throwing, unless some potentially-constructed subobject has a potentially-throwing destructor — a base or class-type member declared `noexcept(false)`, followed transitively. It is a property of **types**, not declarations: the destructor's body is never consulted, so throwing out of an unspecified `~T` terminates rather than making it throwing. The other special members carry only a written specification.

## Catching runtime faults

A null-pointer dereference with `->`, and a division or modulo by zero, raise a catchable exception inside a `try` block instead of trapping the process. The check is emitted before each arrow-deref, including every intermediate link, so a chain like `a->b->c` catches at the first null.

The exception is a `runtime_error`, and `what()` describes the fault:

```c
struct N { int64 v; }

int32 main() {
    N* p = null;
    try {
        int64 x = p->v;                 // raises
        return 1;
    } catch (std::runtime_error& e) {
        std::string m = e.what();       // "null pointer dereference"
        return static_cast<int32>(m.size());
    }
}
```

Every spelling of the type names it: `std::runtime_error`, and `runtime_error` under a `using namespace std;`. A by-value handler (`catch (runtime_error e)`) selects it as a reference one does. `catch (std::exception& e)` takes it too, by the ordinary derived-to-base rule, as does `catch (...)`.

The fault's type is `runtime_error` exactly, so the rules that hold for an explicitly thrown one hold here. A handler for a class *derived* from it (`std::range_error`) does not match, nor does one on the other branch of the hierarchy (`std::logic_error`), nor a pointer handler (`catch (std::runtime_error* e)`) — a fault throws an object. `catch (std::string)` and `catch (const char*)` select a thrown string and nothing else.

Outside a `try` block a null deref still traps at runtime and returns control to the host with `execute()` reporting failure — the inline null check is only emitted inside `try` blocks, so the zero-cost path is preserved for code that does not opt into catching.
