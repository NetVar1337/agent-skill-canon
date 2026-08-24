> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/templates.md).

# Templates

Compile-time monomorphization: each unique type combination produces a separate concrete implementation.

## Template Structs

```cpp
template<typename T>
struct Pair {
    T first;
    T second;
    Pair(T a, T b) { first = a; second = b; }
    T sum() { return first + second; }
}

Pair<int32> p = Pair<int32>(10, 20);
println(std::to_string(p.sum()));  // 30

Pair<float64> fp = Pair<float64>(1.5, 2.5);
println(std::to_string(fp.sum()));  // 4.0
```

## Template Functions

```cpp
template<typename T>
T max_val(T a, T b) {
    if (a > b) return a;
    return b;
}

int32 m = max_val<int32>(10, 20);  // 20
```

## Address of an instantiation

A name that is a template always takes the following `<` as its argument list, so a template-id is usable outside a call. `&f<T>` — or the bare `f<T>` — is the address of that one instantiation, and behaves like any other function pointer.

```cpp
template<typename T>
T twice(T v) { return v + v; }

int64 apply(int64 f(int64), int64 x) { return f(x); }

int64 main() {
    auto p = &twice<int64>;      // address of twice<int64>
    auto q = twice<int64>;       // same thing, no &
    int64 a = p(21);             // 42
    int64 b = apply(&twice<int64>, 20);  // 40
    return a + b;                // 82
}
```

Each instantiation has its own address: `&twice<int64>` and `&twice<int32>` are different pointers.

A name that is *not* a template keeps the relational reading, so `a < b > (c)` is the comparison `(a < b) > (c)`.

## Template with Reference Parameters

```cpp
template<typename T>
void swap_vals(T& a, T& b) {
    T temp = a;
    a = b;
    b = temp;
}

int32 x = 10;
int32 y = 20;
swap_vals<int32>(x, y);  // x=20, y=10
```

## Nested Templates

Template functions can take template-typed args:

```cpp
template<typename T>
struct Box {
    T val;
    Box(T v) { val = v; }
    T get() { return val; }
}

template<typename T>
T unwrap(Box<T> b) {
    return b.get();
}

Box<int32> b = Box<int32>(42);
int32 v = unwrap<int32>(b);  // 42
```

Type args themselves can be template instantiations — `Pair<Pair<int64>>`, `Box<Box<Box<int64>>>`, etc. The closing `>>` parses without spacing in both type position and ctor-call position.

## Templated Base Classes

A class can inherit from a template instantiation:

```cpp
template<typename T> struct Box {
    T value;
    Box(T v) { value = v; }
    T get() { return value; }
}

struct IntBox : Box<int64> {
    IntBox(int64 v) : Box<int64>(v) {}
}

int64 main() {
    IntBox* b = new IntBox(42);
    int64 r = b->get();    // inherited from Box<int64>
    delete b;
    return r;
}
```

The init-list syntax `: Box<int64>(v)` invokes the instantiated base ctor. Override syntax in the derived class works as usual:

```cpp
struct Special : Box<int64> {
    Special(int64 v) : Box<int64>(v) {}
    int64 get() override { return value * 100; }
}
```

The `std` containers are themselves ordinary templates written in `.em` over this same surface — see [Std Library](/perception/enma-lang/addons/std-library.md) for what they offer. Heap accounting treats them like any user code: `heap_count()` reflects their allocations.

## Non-type parameters

A template parameter can be a value, not just a type.

```c
template<typename T, int64 N>
struct Buffer {
    T data[N];
    int64 size() { return N; }
}

Buffer<int64, 8> b;
```

## Specialization

Full specialization replaces the template for one exact argument list; partial specialization matches a shape.

```c
template<typename T> struct Traits { int64 tag() { return 0; } }

template<> struct Traits<int64> { int64 tag() { return 1; } }   // full
template<typename T> struct Traits<T*> { int64 tag() { return 2; } }  // partial
```

Value patterns work too — `template<typename T> struct C<T, 0>`.

A full specialization's declarator may be **qualified**, which declares the same specialization as writing it inside the namespace:

```c
namespace traits {
    template<typename T> struct Tag { int64 id() { return 0; } }
}

template<> struct traits::Tag<int64> { int64 id() { return 42; } }
```

## Variadic packs

`typename... Ts` declares a pack. `sizeof...(Ts)` is its length; a pack expands with `...`, and fold expressions collapse one into a single operation.

```c
template<typename... Ts>
int64 count(Ts... args) { return sizeof...(Ts); }

template<typename... Ts>
int64 total(Ts... args) { return (args + ... + 0); }    // fold
```

Classes take packs as fields (`Ts... name;`); functions expand recursively and accept zero-element packs.

## Alias and template-template parameters

```c
template<typename T> using Vec = std::vector<T>;

template<template<typename> class C>
struct Holder { C<int64> items; }
```

A template-template parameter **deduces** from its argument, so a function can take any one-parameter class template and its element type at once:

```c
template<typename T> struct Box { T v; Box(T x) { v = x; } }
template<typename T> struct Bag { T v; Bag(T x) { v = x; } }

template<template<typename> class C, typename T>
T unwrap(C<T> c) { return c.v; }

int64 main() {
    Box<int64> b = Box<int64>(40);
    Bag<int64> g = Bag<int64>(2);
    return unwrap(b) + unwrap(g);   // 42 — C deduces Box, then Bag
}
```

The argument's own parameter list has to match the parameter's exactly. A two-parameter template bound to a one-parameter template-template parameter is refused rather than guessed at:

```c
template<typename K, typename V> struct Two { K k; V v; }

Holder<Two> h;   // error: template `Two` takes 2 template parameter(s),
                 //        but `C` requires one taking 1
```

## CTAD and deduction guides

Class template arguments deduce from the constructor call, so `Box b(5)` gives `Box<int64>`. Where deduction needs steering, write an explicit guide:

```c
template<typename T> struct Box { T v; Box(T x) { v = x; } }
Box(T) -> Box<T>;

Box b(5);          // Box<int64>
```

A guide may carry its own `template<…>` prefix.

## if constexpr in templates

`if constexpr` discards the untaken branch, so it need not compile for the current instantiation. See [Compile-Time Evaluation](/perception/enma-lang/language-guide/compile-time.md#if-constexpr).

## Member templates

A template member is extracted and instantiated per use, and constructors may be templates.

## A template-id names a scope

A template-id is a type wherever a type is expected, including to the left of `::`, so `Box<int64>::type` resolves the member on **that** instantiation. Two instantiations name two distinct members.

```c
template<typename T> struct Box {
    using type = T;
    static const int64 value = 9;
    T v;
}

template<typename T> struct Wrap {
    Box<T>::type inner;                    // dependent — a field's type
}

template<typename T>
Box<T>::type first(T seed) { return seed; }    // dependent — a return type

int64 main() {
    Box<int64>::type   a = 7;              // int64
    Box<float64>::type b = 2.5;            // float64 — a different member type
    Wrap<int64> w;
    w.inner = 26;
    return a + w.inner + Box<int64>::value + first(0) + static_cast<int64>(b);
}                                          // 44
```

The qualified name is written plainly in every position — local, field, parameter, return type — and a member template is called plainly too (`p->get<int64>(7)`).

The `std` containers declare no member type aliases, so an iterator is named by its own type — `vector_iterator<T>`, `map_iterator<K, V>` — or by `auto`. See [Std Library](/perception/enma-lang/addons/std-library.md#iterators-and-complexity).

### Dependent names

`X<T>::type` names the member type of an instantiation and `T::type` names the member type of a type parameter. `typename` and `template` are accepted where the language requires them — `typename X<T>::type`, `typename T::type`, `p->template get<T>()` — and mean the same as the spelling without them.

```c
struct Plain {
    using type = int64;
    int64 v;
    Plain(int64 x) { v = x; }
    template<typename U> U get(U seed) { return seed; }
}

template<typename T> struct Holder {
    typename T::type slot;                       // member type of a type parameter
}

template<typename T>
typename T::type peel(T b) { return b.v; }

int64 main() {
    Holder<Plain> h;
    h.slot = 2;
    Plain* p = new Plain(0);
    int64 g = p->template get<int64>(40);
    delete p;
    return peel(Plain(0)) + h.slot + g;          // 42
}
```

`typename` applies to a qualified name only — on a bare name it is an error: *"`typename` applies to a qualified name"*.

## SFINAE

When substituting deduced arguments into a candidate's declaration produces an invalid type or expression, that candidate is removed from the overload set rather than failing the program — so trait-style dispatch on a return type, a parameter type, or a default template argument selects the viable overload.

A failure inside a function's **body** is a hard error, not a substitution failure, exactly as in C++.

## Constraints

A `concept` is defined by a constant expression the compiler can decide:

```c
template<typename T> concept Steppable = requires(T x) { x.next(); };  // method
template<typename T> concept Sized     = requires(T x) { x.bytes; };   // member
template<typename T> concept Either    = Steppable<T> || Sized<T>;     // || && !
template<typename T> concept Neither   = !Steppable<T> && !Sized<T>;
template<typename T> concept Anything  = true;                         // literal
```

A `requires`-expression states **member accesses and method calls** on the parameters it binds. A concept-id such as `Steppable<Counter>` is a `bool` prvalue, usable in `static_assert`, `if constexpr`, an initializer or a condition.

A declaration states its constraint three ways, all equivalent — a leading clause, a trailing clause, or a type-constraint parameter, which means `Concept<T>`:

```c
struct Counter { int64 n; int64 next() { return n + 1; } }
struct Blob    { int64 bytes; }

template<Steppable T> int64 advance(T t) { return t.next(); }   // type-constraint
template<typename T>  int64 advance(T t) { return 0; }          // fallback

template<typename T> requires Sized<T>
int64 weigh(T t) { return t.bytes; }                            // leading clause

template<typename T>
int64 weigh(T t) requires Steppable<T> { return -1; }           // trailing clause

int64 main() {
    Counter c; c.n = 41;
    Blob b;    b.bytes = 8;
    static_assert(Steppable<Counter>, "Counter steps");
    static_assert(!Neither<Blob>, "Blob is sized");
    return advance(c) + advance(b) + weigh(b) + weigh(c);   // 42 + 0 + 8 + -1
}
```

Clauses combine with `&&`, `||`, `!` and parentheses. The **argument list** picks which parameters are constrained: `requires Same<T, U>` reads both, and `requires Roomy<U>` on a two-parameter template constrains `U` alone.

```c
struct Src  { int64 pull() { return 40; } }
struct Sink { int64 room; }

template<typename T, typename U> concept Pipes = requires(T a, U b) { a.pull(); b.room; };
template<typename T> concept Roomy = requires(T x) { x.room; };

template<typename T, typename U> requires Pipes<T, U>
int64 move_one(T from, U to) { return from.pull() + to.room; }

template<typename T, typename U>
int64 move_one(T from, U to) { return 0; }

template<typename T, typename U> requires Roomy<U>
int64 fits(T a, U b) { return 1; }

template<typename T, typename U>
int64 fits(T a, U b) { return 0; }

int64 main() {
    Src s; Sink k; k.room = 2;
    return move_one(s, k) + fits(s, k) + move_one(k, s) + fits(k, s);  // 42+1+0+0
}
```

### Unsatisfied, ambiguous, undecidable

An **unsatisfied** constraint removes the candidate from overload resolution without a diagnostic — that is the mechanism by which a fallback overload is chosen. With no other candidate the call is ill-formed: *"no matching function template for call to `f` — the arguments deduce, but every candidate's constraint is not satisfied by them"*.

On a class template there is no overload set, so an unsatisfied constraint is an error at the instantiation: *"`Only` cannot be instantiated with these arguments: its constraint `HasGet<T>` is not satisfied"*.

Two differently constrained candidates that both apply are **ambiguous**, since constraints are not ordered by subsumption: *"call to `f` is ambiguous — no candidate function template is more specialized than every other for these arguments"*.

A constraint whose form is not one of the decidable ones is **reported**, never assumed either way. An arithmetic or relational expression, a trait, a `sizeof` — `concept Small = sizeof(T) <= 8;` — all land here: *"the constraint `Small<T>` states a requirement whose form is not checked, so whether this candidate applies was not decided. A requirement is checked when it names a member or calls a method on the constrained type"*.

### Forms that are not part of the grammar

* a `requires`-expression in expression position — `bool b = requires(T a){ a.f(); };`
* a **type** requirement inside a concept body — `typename T::value_type;`
* a **compound** requirement — `{ a + a } -> Convertible<T>;`
* ordering by subsumption

## Deduction does not peel

Deduction does **not** unwrap `vector<T>&` to its element type. Container algorithms parameterize on the container, or take explicit `<T>`.

## RAII smart pointers

Templates, move semantics and deterministic destructors compose into C++-style smart pointers written in script. A move-only owning pointer:

```c
template<typename T>
struct unique_ptr {
    T* p;
    unique_ptr(T* raw) { p = raw; }
    unique_ptr(const unique_ptr<T>&) = delete;                 // non-copyable
    unique_ptr(unique_ptr<T>&& o) { p = o.p; o.p = null; }     // movable
    ~unique_ptr() { if (p != null) { delete p; } }
    T* get() { return p; }
}

unique_ptr<Node> a = unique_ptr<Node>(new Node(7));
unique_ptr<Node> b = move(a);     // ownership transfers; a is emptied
int64 v = b.get()->val;           // 7
// b's `delete` runs at scope exit — no leak, no double-free
```

A reference-counted `shared_ptr<T>` follows the same shape with a shared count the copy constructor increments and the destructor decrements, freeing the object at zero.
