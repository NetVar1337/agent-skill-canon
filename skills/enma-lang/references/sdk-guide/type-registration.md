> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/type-registration.md).

# Type Registration

`type_builder` exposes a native type to scripts: fields, properties, methods, the full operator surface, the container protocol, lifecycle hooks, interfaces and generics.

Every builder call returns `type_builder&`, so registration is one chained expression ending in `.finish()`.

## The Builder Surface

### Construction

```cpp
type_builder(engine_t* engine, const char* name, type_id id)
type_builder(engine_t* engine, const char* name, type_id id, const char* description)
void finish()
```

`id` is the base category: `type_id::t_struct` for a value-shaped type, `type_id::t_int64` for a handle. Every registration is re-slotted to a fresh custom id at `finish()`, so the category is a shape hint, not an identity.

### Data members

```cpp
type_builder& field(const char* name, uint32_t offset, type_id type)
type_builder& field(const char* name, uint32_t offset, type_id type, const char* description)
type_builder& property(const char* name, void* getter, void* setter,
                       type_id type = type_id::t_int64)
type_builder& property(const char* name, void* getter, void* setter,
                       type_id type, const char* description)
type_builder& inline_property(const char* name, void* getter, void* setter,
                              int32_t offset, type_id type = type_id::t_int64)
```

Note the argument order on `inline_property`: **offset comes before type**.

### Methods

```cpp
type_builder& method(const char* signature, void* fn)
type_builder& method(const char* signature, void* fn, const char* description)
type_builder& method(const char* name, const char* signature, void* fn)
type_builder& method(const char* name, void* fn, type_id ret, uint32_t params,
                     bool elem_ret = false)
type_builder& method(const char* name, void* fn, type_id ret,
                     std::initializer_list<type_id> arg_types, bool elem_ret = false)
type_builder& permission(uint32_t flags)      // gates the LAST-registered method
type_builder& in_place(void* fn)              // SRET variant of the LAST method
type_builder& borrows_element()               // LAST method's result is not the caller's to free
type_builder& transfers_element()             // LAST method's result IS the caller's to free
```

A signature says this directly — `element&` borrows, `element` transfers. The two calls above override that reading, and are the channel for the `method()` overloads that take no signature string.

### Lifecycle

```cpp
type_builder& factory(void* fn, uint32_t params)
type_builder& factory(void* fn, std::initializer_list<type_id> types)
type_builder& factory(void* fn, std::initializer_list<type_id> types, const char* description)
type_builder& factory_in_place(void* fn)
type_builder& destructor(void* fn)
type_builder& destructor(void* fn, const char* description)
type_builder& copy(void* fn)             // T b = a;
type_builder& copy_assign(void* fn)      // b = a; for an already-constructed b
type_builder& serialize(void* fn)
type_builder& deserialize(void* fn)
type_builder& hash(void* fn)
type_builder& init_push(void* fn)                     // brace init: T v = {a, b, c}
type_builder& init_push(void* fn, type_id elem_type)  // ... naming the element type
template<auto PushFn> type_builder& init_push_typed() // ... reading it off the fn
```

### Operators

```cpp
// arithmetic — int64_t fn(int64_t lhs, int64_t rhs)
type_builder& bin_add(void* fn)   type_builder& bin_sub(void* fn)
type_builder& bin_mul(void* fn)   type_builder& bin_div(void* fn)
type_builder& bin_mod(void* fn)

// compound assign — no fallback to the binary form; register what you want to allow
type_builder& bin_add_assign(void* fn)   type_builder& bin_sub_assign(void* fn)
type_builder& bin_mul_assign(void* fn)   type_builder& bin_div_assign(void* fn)
type_builder& bin_mod_assign(void* fn)

// relational — bool fn(U* lhs, U* rhs)
type_builder& bin_eq(void* fn)   type_builder& bin_lt(void* fn)
type_builder& bin_gt(void* fn)   type_builder& bin_le(void* fn)
type_builder& bin_ge(void* fn)

// three-way — int32_t fn(U* lhs, U* rhs) returning -1 / 0 / +1
type_builder& compare(void* fn)

// bitwise and shift
type_builder& bit_and(void* fn)   type_builder& bit_or(void* fn)
type_builder& bit_xor(void* fn)   type_builder& shl(void* fn)
type_builder& shr(void* fn)
type_builder& bit_and_assign(void* fn)   type_builder& bit_or_assign(void* fn)
type_builder& bit_xor_assign(void* fn)   type_builder& shl_assign(void* fn)
type_builder& shr_assign(void* fn)

// unary
type_builder& unary_neg(void* fn)       // -v
type_builder& unary_bit_not(void* fn)   // ~v
type_builder& unary_log_not(void* fn)   // !v   — bool fn(U*)
type_builder& unary_deref(void* fn)     // *v

// increment / decrement — prefix and postfix are separate registrations
type_builder& increment(void* fn)        // ++v
type_builder& decrement(void* fn)        // --v
type_builder& post_increment(void* fn)   // v++
type_builder& post_decrement(void* fn)   // v--

// call and arrow
type_builder& call(void* fn)    // obj(args...) — variadic
type_builder& arrow(void* fn)   // obj->member  — void* fn(U*)

// conversions
type_builder& convert(type_id from, void* fn)     // incoming: from -> this type
type_builder& cast_to(type_id target, void* fn)   // outgoing: this type -> target

// SRET variants for value types — void fn(void* sret, void* lhs, void* rhs)
type_builder& bin_add_in_place(void* fn)   type_builder& bin_sub_in_place(void* fn)
type_builder& bin_mul_in_place(void* fn)   type_builder& unary_neg_in_place(void* fn)
```

### Container protocol

```cpp
type_builder& subscript(void* get_fn, void* set_fn, bool elem_ret = true)
type_builder& subscript(void* get_fn, void* set_fn, bool elem_ret, type_id elem_type)
type_builder& iterable(void* length_fn, void* get_fn)
type_builder& kv_iterable(void* length_fn, void* key_fn, void* val_fn)
type_builder& owns_elements()    // elements are heap-allocated and freed at cleanup
```

The four-argument `subscript` names the element type. Give it whenever the element is a concrete scalar — it is what puts a `float64` element in a float register. `subscript_typed` fills it in for you.

### Type system

```cpp
type_builder& implements(const char* interface_name)
type_builder& as_interface()
type_builder& generic_param(const char* name)
type_builder& requires_iface(const char* param, const char* iface)
type_builder& value_type(uint32_t size, uint32_t align = 0)
type_builder& pure_methods()   // no method retains the receiver
type_builder& pure_args()      // no method retains any argument (implies pure_methods)
type_builder& boxes_args()     // registered converters apply at method-arg checks
```

### Typed wrappers

```cpp
template<auto Fn> type_builder& method_typed(const char* signature)
template<auto Fn> type_builder& factory_typed(uint32_t params)
template<auto Fn> type_builder& destructor_typed()
template<auto GetFn, auto SetFn = nullptr>
type_builder& property_typed(const char* name, type_id type)
template<auto GetFn, auto SetFn = nullptr>
type_builder& subscript_typed(bool elem_ret = true)

// one per operator hook
template<auto Fn> type_builder& bin_add_typed()   // sub, mul, div, mod
template<auto Fn> type_builder& bin_eq_typed()    // lt, gt, le, ge
template<auto Fn> type_builder& bin_add_assign_typed()   // sub, mul, div, mod
template<auto Fn> type_builder& increment_typed()        // decrement
template<auto Fn> type_builder& unary_neg_typed()        // unary_bit_not
template<auto Fn> type_builder& bit_and_typed()          // bit_or, bit_xor, shl, shr
template<auto Fn> type_builder& compare_typed()
template<auto Fn> type_builder& copy_typed()
template<auto Fn> type_builder& hash_typed()
```

Each `_typed` form forwards `Fn` to the raw `void*` registration, so you write real C++ signatures and the native is called directly through the Win64 ABI — no trampoline, no `int64` bit-casting.

## Descriptions

Every hook that takes a signature or a name also takes an optional trailing `const char* description`. These surface via [`extract_documentation`](/perception/enma-lang/sdk-guide/introspection.md) and `extract_intellisense`.

```cpp
type_builder(e, "color", type_id::t_int64, "RGBA color, 8 bits per channel")
    .factory((void*)&color_create,
        { type_id::t_uint8, type_id::t_uint8, type_id::t_uint8, type_id::t_uint8 },
        "construct color(r, g, b, a) with each channel in 0..255")
    .destructor((void*)&color_dtor, "free color memory")
    .method("uint8 r()", (void*)&color_r, "red channel")
    .field("pad", offsetof(color_t, pad), type_id::t_uint32, "reserved - do not read")
    .property("rgba", (void*)&color_rgba_get, (void*)&color_rgba_set,
              type_id::t_uint32, "packed 32-bit RGBA form")
    .finish();
```

Descriptions are documentation only — omit them when the name says it.

## Walkthrough: Building a Complete Type

{% stepper %}
{% step %}

#### The Native Type

```cpp
struct vec3_t {
    double x, y, z;
};
```

{% endstep %}

{% step %}

#### Starting the Builder

```cpp
type_builder(e, "vec3_t", type_id::t_struct)
```

Engine, the name scripts will use, and the base category.
{% endstep %}

{% step %}

#### Fields

Bind real struct offsets so scripts read and write the memory directly:

```cpp
    .field("x", offsetof(vec3_t, x), type_id::t_float64)
    .field("y", offsetof(vec3_t, y), type_id::t_float64)
    .field("z", offsetof(vec3_t, z), type_id::t_float64)
```

```c
vec3_t v = vec3_t(1.0, 2.0, 3.0);
float64 x = v.x;      // 1.0
v.y = 5.0;
```

{% endstep %}

{% step %}

#### Factory

Returns a pointer to the new object. Allocate with `heap_alloc` so the runtime tracks it:

```cpp
vec3_t* vec3_create(double x, double y, double z) {
    auto* v = static_cast<vec3_t*>(heap_alloc(sizeof(vec3_t)));
    v->x = x; v->y = y; v->z = z;
    return v;
}
```

```cpp
    .factory_typed<&vec3_create>(3)   // 3 = param count
```

The raw form names the parameter types, which is what lets the ABI router put the three doubles in float registers:

```cpp
    .factory((void*)&vec3_create,
             { type_id::t_float64, type_id::t_float64, type_id::t_float64 })
```

```c
vec3_t v = vec3_t(1.0, 2.0, 3.0);
```

{% endstep %}

{% step %}

#### Destructor

```cpp
void vec3_destroy(vec3_t* self) { heap_free(self); }
```

```cpp
    .destructor_typed<&vec3_destroy>()
```

Fires on scope exit, exception unwind, native fault unwind, and context destroy. All cleanup is deterministic; there is no tracing collector. Without a destructor the memory is still reclaimed, but your teardown does not run.

#### `.pure_methods()`

No method retains the receiver. Enables deterministic scope-drop for collection-like types whose methods may store value arguments but never `self`. Don't set it if any method does `g = self;`.

#### `.pure_args()` (implies `.pure_methods()`)

No method retains *any* argument. Use for value-like types: strings, math types, immutable records. With it set, a local stays droppable even when passed as an argument to its own type's methods:

```c
vec2 a = vec2(1, 2);
vec2 b = vec2(3, 4);
vec2 c = a + b;           // both a and b safely dropped at scope exit
```

Rule of thumb: `.pure_methods()` for containers, `.pure_args()` for values.
{% endstep %}

{% step %}

#### Methods

Methods receive `self` as the first argument.

```cpp
double vec3_length_sq(vec3_t* self) {
    return self->x*self->x + self->y*self->y + self->z*self->z;
}
vec3_t* vec3_scale(vec3_t* self, double f) {
    self->x *= f; self->y *= f; self->z *= f;
    return self;
}
```

```cpp
    .method_typed<&vec3_length_sq>("float64 length_sq()")
    .method_typed<&vec3_scale>    ("vec3_t scale(float64)")
```

The signature string is the canonical registration syntax — the compiler takes the method name and the parameter types from it and checks arity and per-argument type at every call site. `v.scale("hello")` and `v.scale()` both fail to compile.

Any arity and any type mix works: int, bool, char, float, double, pointer, enum, or another registered type.

```cpp
struct_builder(e, "proc_t").field("pid", type_id::t_int64).finish();
// ...
    .method("proc_t make_child(int64)",  (void*)proc_make_child)
    .method("int64 inspect(proc_t)",     (void*)proc_inspect)
```

For generic containers, `element` is a placeholder that resolves to the receiver's element type at the call site. In return position it also marks the method as returning an element:

```cpp
    .method("void push(element)",          (void*)arr_push)
    .method("element pop()",               (void*)arr_pop)
    .method("element& at(int64)",          (void*)arr_at)
    .method("void insert(int64, element)", (void*)arr_ins)
```

The `&` on the return is the ownership declaration, and it means what it means in C++. `element&` hands back an alias into storage the container keeps, so `T x = c.at(0)` does not run `x`'s destructor at scope exit — the container's own teardown owns that element. Plain `element` hands back a value the caller owns and destroys.

Get it wrong in the transferring direction and the element is destroyed twice: once when the caller's local goes out of scope, once when the container tears down. Get it wrong the other way and it leaks. The method's *name* has no bearing on this — `at`, `peek` and `nth` are read exactly alike.

`element*` reads the same as `element&`, matching how a parameter's `&` and `*` are read. Whitespace before either is insignificant.

```c
vec3_t v = vec3_t(3.0, 4.0, 0.0);
float64 l = v.length_sq();   // 25.0
v.scale(2.0);
```

{% endstep %}

{% step %}

#### Properties

Field-style access backed by getter and setter callbacks. The getter is `T(int64_t self)` and the setter `void(int64_t self, T value)` — real C++ types, so a `double` returns in `xmm0` and a `double` setter takes its value in `xmm1`.

```cpp
double vec3_get_magnitude(int64_t self) {
    auto* v = reinterpret_cast<vec3_t*>(self);
    return sqrt(v->x * v->x + v->y * v->y + v->z * v->z);
}
double vec3_get_x(int64_t self)            { return reinterpret_cast<vec3_t*>(self)->x; }
void   vec3_set_x(int64_t self, double d)  { reinterpret_cast<vec3_t*>(self)->x = d; }
```

```cpp
    .property("magnitude", (void*)vec3_get_magnitude, nullptr, type_id::t_float64)
    .property("x", (void*)vec3_get_x, (void*)vec3_set_x, type_id::t_float64)
```

`nullptr` for the setter makes the property read-only. The `type_id` flows into codegen, so `static_cast<int64>(v.x)` performs a real float-to-int conversion rather than reinterpreting bits.

The typed form takes a receiver pointer instead of an `int64_t`, and the type as an argument:

```cpp
double vec3_get_y(vec3_t* self)           { return self->y; }
void   vec3_set_y(vec3_t* self, double d) { self->y = d; }

    .property_typed<&vec3_get_y, &vec3_set_y>("y", type_id::t_float64)
    .property_typed<&vec3_get_magnitude_t>("magnitude", type_id::t_float64)  // read-only
```

```c
float64 m = v.magnitude;   // getter
v.x = 5.0;                 // setter
float64 x = v.x;           // getter
```

{% endstep %}

{% step %}

#### Arithmetic Operators

```cpp
vec3_t* vec3_add(vec3_t* a, vec3_t* b) {
    return vec3_create(a->x + b->x, a->y + b->y, a->z + b->z);
}
```

```cpp
    .bin_add_typed<&vec3_add>()
    .bin_sub_typed<&vec3_sub>()
    .bin_mul_typed<&vec3_mul>()
    .bin_div_typed<&vec3_div>()
    .bin_mod_typed<&vec3_mod>()
```

The raw form takes two `int64_t` handles and returns one:

```cpp
int64_t vec3_add_raw(int64_t a, int64_t b) {
    auto* va = reinterpret_cast<vec3_t*>(a);
    auto* vb = reinterpret_cast<vec3_t*>(b);
    return reinterpret_cast<int64_t>(vec3_create(va->x + vb->x, va->y + vb->y, va->z + vb->z));
}
    .bin_add((void*)vec3_add_raw)
```

```c
vec3_t a = vec3_t(1.0, 2.0, 3.0);
vec3_t b = vec3_t(4.0, 5.0, 6.0);
vec3_t c = a + b;  // (5, 7, 9)
vec3_t d = a * b;  // (4, 10, 18)
```

{% endstep %}

{% step %}

#### Comparison Operators

Each returns `bool`:

```cpp
bool vec3_eq(vec3_t* a, vec3_t* b) {
    return a->x == b->x && a->y == b->y && a->z == b->z;
}
bool vec3_lt(vec3_t* a, vec3_t* b) { return mag_sq(a) <  mag_sq(b); }
bool vec3_gt(vec3_t* a, vec3_t* b) { return vec3_lt(b, a); }
bool vec3_le(vec3_t* a, vec3_t* b) { return !vec3_gt(a, b); }
bool vec3_ge(vec3_t* a, vec3_t* b) { return !vec3_lt(a, b); }
```

```cpp
    .bin_eq_typed<&vec3_eq>()
    .bin_lt_typed<&vec3_lt>()
    .bin_gt_typed<&vec3_gt>()
    .bin_le_typed<&vec3_le>()
    .bin_ge_typed<&vec3_ge>()
```

```c
if (a == b) { println("equal"); }
if (a < b)  { println("a is shorter"); }
```

{% endstep %}

{% step %}

#### Three-Way Compare

One function returning `-1`, `0`, or `+1`, used for `<`, `>`, `<=`, `>=`, `==` and `!=` when the specific operator is not registered. The return type is `int32_t`:

```cpp
int32_t vec3_cmp(vec3_t* a, vec3_t* b) {
    double la = mag_sq(a), lb = mag_sq(b);
    return la < lb ? -1 : la > lb ? 1 : 0;
}
```

```cpp
    .compare_typed<&vec3_cmp>()
```

{% endstep %}

{% step %}

#### Compound Assignment

`+=`, `-=`, `*=`, `/=`, `%=` resolve **only** to their own registration — a type with just `bin_add` rejects `a += b` at compile time.

```cpp
    .bin_add_assign_typed<&vec3_add_assign>()
    .bin_sub_assign_typed<&vec3_sub_assign>()
    .bin_mul_assign_typed<&vec3_mul_assign>()
    .bin_div_assign_typed<&vec3_div_assign>()
    .bin_mod_assign_typed<&vec3_mod_assign>()
```

The bitwise and shift compound forms work the same way:

```cpp
    .bit_and_assign((void*)v_and_assign)   .bit_or_assign((void*)v_or_assign)
    .bit_xor_assign((void*)v_xor_assign)   .shl_assign((void*)v_shl_assign)
    .shr_assign((void*)v_shr_assign)
```

{% endstep %}

{% step %}

#### Increment / Decrement

Arity-1, returning the new value. Prefix and postfix are separate registrations: `++x` resolves only to `increment` and `x++` only to `post_increment`, and a missing form is a compile error.

```cpp
counter_t* counter_inc(counter_t* self);
counter_t* counter_dec(counter_t* self);
```

```cpp
    .increment_typed<&counter_inc>()
    .decrement_typed<&counter_dec>()
    .post_increment((void*)counter_inc_post)
    .post_decrement((void*)counter_dec_post)
```

{% endstep %}

{% step %}

#### Unary Operators

```cpp
vec3_t* vec3_neg(vec3_t* v) { return vec3_create(-v->x, -v->y, -v->z); }
bool    vec3_empty(vec3_t* v) { return v->x == 0 && v->y == 0 && v->z == 0; }
```

```cpp
    .unary_neg_typed<&vec3_neg>()
    .unary_bit_not_typed<&vec3_bit_not>()
    .unary_log_not((void*)vec3_empty)     // !v   — returns bool
    .unary_deref((void*)handle_deref)     // *v
```

```c
vec3_t n = -v;
if (!v) { println("zero vector"); }
```

{% endstep %}

{% step %}

#### Bitwise Operators

```cpp
    .bit_and_typed<&vec3_bit_and>()
    .bit_or_typed<&vec3_bit_or>()
    .bit_xor_typed<&vec3_bit_xor>()
    .shl_typed<&vec3_shl>()
    .shr_typed<&vec3_shr>()
```

{% endstep %}

{% step %}

#### Call and Arrow

`.call` makes the type callable — common for matrix element access. `.arrow` gives smart-pointer-style member access; it returns a pointer to a struct or class instance and the compiler resolves the member against that layout.

```cpp
    .call((void*)mat4_at)      // m(i, j)
    .arrow((void*)ref_target)  // p->field
```

{% endstep %}

{% step %}

#### Subscript

```cpp
double vec3_get_idx(vec3_t* v, int64_t i) {
    return (i == 0) ? v->x : (i == 1) ? v->y : v->z;
}
void vec3_set_idx(vec3_t* v, int64_t i, double d) {
    if (i == 0) v->x = d; else if (i == 1) v->y = d; else v->z = d;
}
```

```cpp
    .subscript_typed<&vec3_get_idx, &vec3_set_idx>()
```

`subscript_typed` reads the element type off the getter's return type, so a `float64` element is passed and returned in a float register. The raw form takes `int64_t` throughout and bit-casts by hand; give it the element type when the element is a concrete scalar:

```cpp
int64_t vec3_get_raw(int64_t self, int64_t idx);
int64_t vec3_set_raw(int64_t self, int64_t idx, int64_t bits);

    .subscript((void*)vec3_get_raw, (void*)vec3_set_raw, true, type_id::t_float64)
```

```c
float64 z = v[2];
v[2] = 99.0;
```

{% endstep %}

{% step %}

#### Iteration

```cpp
int64_t vec3_iter_len(int64_t self) { return 3; }
int64_t vec3_iter_get(int64_t self, int64_t idx);
```

```cpp
    .iterable((void*)vec3_iter_len, (void*)vec3_iter_get)
```

```c
for (float64 component : v) {
    println(std::to_string(component));
}
```

For key-value iteration:

```cpp
    .kv_iterable((void*)len_fn, (void*)key_fn, (void*)val_fn)
```

Add `.owns_elements()` when the elements are heap allocations the container is responsible for — it is what makes the runtime emit the element-walk at cleanup. Types that expose `iterable` purely for read-only traversal leave it off.
{% endstep %}

{% step %}

#### Init-Push

`void fn(U* self, element value)` — append one raw element to the container.

```cpp
    .init_push((void*)set_push)                       // element type unnamed
    .init_push_typed<&set_push>()                     // read off the function
    .init_push((void*)set_push, type_id::t_float64)   // or spelled out
```

Two things need it.

**`T v = {a, b, c}`.** The factory builds the instance, then this is called once per braced element, in order. Without it a braced list is a compile error naming the type — there is no way to fill the instance, so nothing is built.

**Deep-copying a set-shaped registration.** A container that registers `factory`, `iterable` and `init_push` but no `subscript` has no replace-by-key call, so copying one is done by building a fresh container from the factory and pushing a deep clone of each element into it.

Name the element type when it is a concrete scalar. Left unnamed it is the integer-sized handle default, which is right for handles and integers and wrong for floats — a `float32`/`float64` element then travels in an integer register, so a braced list of them is refused rather than silently initialized to zero. `init_push_typed` reads the type off the push function's own parameter; the three-argument `init_push` spells it out.
{% endstep %}

{% step %}

#### Hash

```cpp
int64_t vec3_hash(vec3_t* v);
```

```cpp
    .hash_typed<&vec3_hash>()
```

Required to use the type as a map key.
{% endstep %}

{% step %}

#### Conversions

`.convert(from, fn)` registers an **incoming** conversion, fired at binary operands, native call arguments, and variable declarations:

```cpp
int64_t vec3_from_int(int64_t val) {
    double d = static_cast<double>(val);
    return reinterpret_cast<int64_t>(vec3_create(d, d, d));
}
    .convert(type_id::t_int64, (void*)vec3_from_int)
```

```c
vec3_t v = 9;        // invokes the converter
```

Compatible types fall through — a `t_int64` converter matches a `t_int32` source.

`.cast_to(target, fn)` is the outgoing direction, used by `static_cast<T>(obj)` and the four implicit conversion sites:

```cpp
    .cast_to(type_id::t_float64, (void*)vec3_to_double)
```

By default a registered converter does **not** apply at method-argument checks, so a type like `string` with an int converter doesn't silently accept ints. Opt in with `.boxes_args()` for wrapper types like a variant or JSON value.
{% endstep %}

{% step %}

#### Const Methods

Append `const` to mark a method non-mutating. Calling a non-const method through a `const` receiver is a compile error.

```cpp
    .method("float64 length() const",  (void*)vec3_length)
    .method("void scale(float64)",     (void*)vec3_scale)
```

```c
int32 observe(const vec3_t v) {
    float64 len = v.length();   // OK
    v.scale(2.0);               // compile error: non-const method on const receiver
    return 0;
}
```

{% endstep %}

{% step %}

#### Per-Method Permission

`.permission(flags)` gates the **last-registered** method behind a permission flag.

```cpp
    .method("void secure(int64)", (void*)secure_op).permission(PERM_FFI)
```

A module without that permission is rejected at compile time, and a precompiled one at load.
{% endstep %}

{% step %}

#### Finishing

```cpp
    .finish();
```

{% endstep %}
{% endstepper %}

## Lifecycle Hooks

Beyond factory and destructor:

* **`.copy(fn)`** — `T b = a;` copy-construction. `int64_t fn(int64_t src)` returning the copy. For reference-counted or copy-on-write types.
* **`.copy_assign(fn)`** — `b = a;` where `b` already exists, so it can release its old resources first. `void fn(U* lhs, U* rhs)`.
* **`.serialize(fn)`** — `int64_t fn(int64_t value)` returning a `char*`.
* **`.deserialize(fn)`** — `int64_t fn(int64_t str)` returning a fresh value.

`serialize` and `deserialize` are metadata at the SDK level: callers reach them through `type_reg_serialize` / `type_reg_deserialize`, which lets a generic walker round-trip any registered type without knowing its shape.

## Interfaces

Mark a type as an interface with `.as_interface()`; others declare conformance with `.implements("Name")`:

```cpp
type_builder(e, "Stream", type_id::t_int64).as_interface().finish();

type_builder(e, "file_t", type_id::t_int64)
    .factory_typed<&file_create>(1)
    .destructor_typed<&file_destroy>()
    .implements("Stream")
    .method("int64 write(int64)", (void*)file_write)
    .finish();
```

A native can then accept `Stream` as a parameter type and the compiler admits any type whose `.implements` list contains it. At the ABI boundary the concrete `type_id` is injected before the value, so the native signature is `(int64 concrete_tid, int64 value, ...)`. Resolve the per-type method pointer with:

```cpp
void* fn = enma::interface_method_fn(engine, (type_id)tid, "method_name");
```

An interface-typed local dispatches at runtime, including across reassignment — the compiler keeps a hidden type-id slot beside the local and updates it on every assignment:

```c
Stream s = file_stream("path");
int64 n = s.write(5);        // file_stream.write
s = mem_stream();
int64 m = s.write(5);        // mem_stream.write
```

## Generic Type Parameters

`.generic_param("T")` declares a parameter that method signatures can reference:

```cpp
type_builder(e, "bag_t", type_id::t_int64)
    .generic_param("T")
    .factory_typed<&bag_create>(0)
    .destructor_typed<&bag_destroy>()
    .method("void add(T)",      (void*)bag_add)
    .method("bool contains(T)", (void*)bag_contains)
    .finish();
```

The script binds `T` at the declaration:

```c
bag_t<int64> s = bag_t<int64>();
s.add(42);            // checked: 42 is int64
s.add("oops");        // compile error: expected int64
```

One native serves every binding — there is no monomorphization; the parameter only drives compile-time checking at call sites. Call `.generic_param` once per parameter (a map-shaped type declares both `"K"` and `"V"`).

### Constraining a Parameter

`.requires_iface(param, iface)` rejects bindings whose concrete type does not implement the interface:

```cpp
type_builder(e, "Hashable", type_id::t_int64).as_interface().finish();

type_builder(e, "bag_t", type_id::t_int64)
    .generic_param("T")
    .requires_iface("T", "Hashable")
    .factory_typed<&bag_create>(0)
    .finish();
```

```c
bag_t<my_hashable_t> a;   // OK
bag_t<plain_t> b;         // compile error: type 'plain_t' does not satisfy 'T: Hashable' on bag_t
```

Call it once per (param, iface) pair.

## Complete Registration

```cpp
type_builder(e, "vec3_t", type_id::t_struct)
    .field("x", offsetof(vec3_t, x), type_id::t_float64)
    .field("y", offsetof(vec3_t, y), type_id::t_float64)
    .field("z", offsetof(vec3_t, z), type_id::t_float64)
    .factory_typed<&vec3_create>(3)
    .destructor_typed<&vec3_destroy>()
    .method_typed<&vec3_length_sq>("float64 length_sq()")
    .method_typed<&vec3_normalize>("vec3_t normalize()")
    .property("magnitude", (void*)vec3_get_magnitude, nullptr, type_id::t_float64)
    .bin_add_typed<&vec3_add>()
    .bin_sub_typed<&vec3_sub>()
    .bin_mul_typed<&vec3_mul>()
    .bin_div_typed<&vec3_div>()
    .bin_mod_typed<&vec3_mod>()
    .bin_eq_typed<&vec3_eq>()
    .bin_lt_typed<&vec3_lt>()
    .bin_gt_typed<&vec3_gt>()
    .bin_le_typed<&vec3_le>()
    .bin_ge_typed<&vec3_ge>()
    .unary_neg_typed<&vec3_neg>()
    .subscript_typed<&vec3_get_idx, &vec3_set_idx>()
    .iterable((void*)vec3_iter_len, (void*)vec3_iter_get)
    .hash_typed<&vec3_hash>()
    .finish();
```

## Struct Builder

Lightweight registration for data-only structs — no methods, no custom lifecycle.

```cpp
struct_builder& field(const char* name, type_id type, const char* type_name = nullptr)
struct_builder& packed()
void            finish()
```

```cpp
struct_builder(e, "point_t")
    .field("x", type_id::t_int64)
    .field("y", type_id::t_int64)
    .finish();
```

Each field rounds up to an 8-byte slot by default. `.packed()` gives C-compatible packed layout:

```cpp
struct_builder(e, "point_t").packed()
    .field("x", type_id::t_int32)
    .field("y", type_id::t_int32)
    .finish();
```

```c
point_t p = point_t(10, 20);
int64 x = p.x;
```

The generated constructor takes arguments in field declaration order.

## Enum Builder

Named integer constants under a type name, accessed with `::`.

```cpp
enum_builder& value(const char* name, int64_t val)
void          finish()
```

```cpp
enum_builder(e, "Color")
    .value("Red", 0)
    .value("Green", 1)
    .value("Blue", 2)
    .finish();
```

```c
int32 c = Color::Green;
if (c == Color::Green) { println("green"); }
```

## Value-Type Registration

By default `type_builder` registers a **reference type**: every script value is an 8-byte handle to a heap allocation. `vec3 v` is a pointer, `vec3[]` is an array of pointers, and passing `v` copies the pointer.

For small POD-like types the heap traffic dominates the work. Opt into **value semantics** with three hooks:

```cpp
type_builder(e, "vec3", type_id::t_int64)
    .value_type(sizeof(vec3_t))                  // (1) inline storage
    .factory_in_place((void*)vec3_construct)     // (2) write-into-buffer ctor
    .factory((void*)vec3_construct,
             { type_id::t_float64, type_id::t_float64, type_id::t_float64 })
    .inline_property("x", (void*)vec3_get_x, (void*)vec3_set_x,
                     (int32_t)offsetof(vec3_t, x), type_id::t_float64)   // (3)
    .inline_property("y", (void*)vec3_get_y, (void*)vec3_set_y,
                     (int32_t)offsetof(vec3_t, y), type_id::t_float64)
    .inline_property("z", (void*)vec3_get_z, (void*)vec3_set_z,
                     (int32_t)offsetof(vec3_t, z), type_id::t_float64)
    .finish();
```

1. **`.value_type(N)`** marks the registration inline-storable. Containers allocate `N` bytes per slot and store the bits directly — no handle indirection, no per-element allocation. `N` must equal `sizeof(YourType)`. `align` defaults to 8; pass 16 for SIMD-friendly layouts.
2. **`.factory_in_place(fn)`** is a constructor that writes into a buffer the caller supplies rather than allocating one. `int64_t fn(int64_t dst, args...)`, returning `dst`:

   ```cpp
   int64_t vec3_construct(int64_t dst, double x, double y, double z) {
       auto* v = reinterpret_cast<vec3_t*>(dst);
       v->x = x; v->y = y; v->z = z;
       return dst;
   }
   ```

   For `vec3 v = vec3(1, 2, 3)` the compiler stack-allocates the value when it doesn't escape and calls this once to fill it. `new vec3(...)` still routes through the regular `.factory()`.
3. **`.inline_property(name, getter, setter, offset, type)`** replaces the native call with a direct load or store at `offset`. The function pointers stay registered for host-side and reflection use. Keep the ordinary `.property(...)` for computed or validating accessors.

### What this buys

For `vec3 v = vec3(1.0, 2.0, 3.0); float64 s = v.x + v.y + v.z;`:

|                                                          | Per call cost                                                     |
| -------------------------------------------------------- | ----------------------------------------------------------------- |
| `.property` + `.factory` (handle type)                   | `heap_alloc(24)` + ctor + 3 native getter calls + `heap_free(24)` |
| `.value_type` + `.factory_in_place` + `.inline_property` | stack alloc + ctor + 3 direct field loads                         |

In a 10M-iteration benchmark of `acc + v.x + v.y + v.z` the value-type form runs in \~67 ms against \~145 ms for the handle form. The larger win is in containers: a `vec3[]` of N elements is N × 24 contiguous bytes instead of N handles pointing at N scattered 24-byte blocks.

### Constraints

* The native type must be POD-shaped — no constructor, destructor, or virtual methods on the C++ side. The runtime treats the bytes as opaque storage.
* `.value_type(N)` composes with `.destructor()`; the destructor still fires at scope exit for heap instances, and the runtime distinguishes stack from heap addresses.
* Inline storage applies to sequence-shaped registrations. Map-shaped ones store 8-byte handles.
* Properties inline only through `.inline_property(...)`. Plain `.property(...)` keeps the native-call path.
