> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/api-reference.md).

# API Reference

All functions are in the `enma` namespace.

### Engine

| Function                    | Signature                                                          | Description                                                                                                                                                                       |
| --------------------------- | ------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `create`                    | `engine_t* create()`                                               | Create a new engine instance                                                                                                                                                      |
| `destroy`                   | `void destroy(engine_t*)`                                          | Destroy engine and free resources                                                                                                                                                 |
| `shutdown`                  | `void shutdown()`                                                  | One-shot process-global teardown - removes the runtime fault handler before a DLL hosting enma unloads. Call once from `DllMain` `DLL_PROCESS_DETACH`. Safe with no live engines. |
| `set_optimize`              | `void set_optimize(engine_t*, bool)`                               | Enable/disable optimizer (default: on, set by `create()`)                                                                                                                         |
| `set_debug`                 | `void set_debug(engine_t*, bool)`                                  | Enable/disable debug info                                                                                                                                                         |
| `define`                    | `void define(engine_t*, const char* name, const char* value)`      | Set preprocessor define                                                                                                                                                           |
| `add_include_path`          | `void add_include_path(engine_t*, const char*)`                    | Add `#include` search path                                                                                                                                                        |
| `add_module_path`           | `void add_module_path(engine_t*, const char*)`                     | Add `import` search path                                                                                                                                                          |
| `set_permissions`           | `void set_permissions(engine_t*, uint32_t)`                        | Set permission flags                                                                                                                                                              |
| `get_permissions`           | `uint32_t get_permissions(engine_t*)`                              | Get current permission flags                                                                                                                                                      |
| `set_include_resolver`      | `void set_include_resolver(engine_t*, resolve_fn, void* userdata)` | Override `#include` file resolution                                                                                                                                               |
| `set_import_resolver`       | `void set_import_resolver(engine_t*, resolve_fn, void* userdata)`  | Override `import` module resolution                                                                                                                                               |
| `set_global_allocators`     | `void set_global_allocators(allocators)`                           | Override module/runtime allocator pair (set before `create()`)                                                                                                                    |
| `get_global_allocators`     | `allocators get_global_allocators()`                               | Read current global allocator pair                                                                                                                                                |
| `set_stack_allocators`      | `void set_stack_allocators(allocators)`                            | Override per-context script stack allocator pair (set before `create_context` with non-zero stack\_size)                                                                          |
| `get_stack_allocators`      | `allocators get_stack_allocators()`                                | Read current stack allocator pair                                                                                                                                                 |
| `set_heap_allocators`       | `void set_heap_allocators(engine_t*, heap_allocators)`             | Override the script heap — the third allocator pool. `override_for_strings_only` narrows it to string storage                                                                     |
| `get_heap_allocators`       | `heap_allocators get_heap_allocators(engine_t*)`                   | Read the current heap allocator set                                                                                                                                               |
| `set_directory_isolate`     | `void set_directory_isolate(engine_t*, const char* root)`          | Confine every file-addon path to `root`                                                                                                                                           |
| `current_directory_isolate` | `const char* current_directory_isolate()`                          | The active isolate root, as the file addon reads it                                                                                                                               |

### Compilation

| Function         | Signature                                                                         | Description                                               |
| ---------------- | --------------------------------------------------------------------------------- | --------------------------------------------------------- |
| `compile`        | `module_t* compile(engine_t*, const char* src, size_t len, const char* filename)` | Compile source to module                                  |
| `compile_file`   | `module_t* compile_file(engine_t*, const char* path)`                             | Compile file to module                                    |
| `module_destroy` | `void module_destroy(module_t*)`                                                  | Destroy compiled module                                   |
| `reload`         | `bool reload(module_t*, const char* src, size_t len, const char* filename)`       | Hot-reload module with new source                         |
| `is_precompiled` | `bool is_precompiled(const char* src, size_t src_len)`                            | Whether a buffer is an `.emb` artifact rather than source |

### Execution

| Function          | Signature                                                                         | Description                                                                                                                                                                                                                                                                                     |
| ----------------- | --------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `create_context`  | `context_t* create_context(module_t*, size_t stack_size = 0)`                     | Create an execution context. `stack_size > 0` allocates a separate script stack; see [Compile & Run](/perception/enma-lang/sdk-guide/compile-and-run.md#creating-a-context) for the unwinding caveat.                                                                                           |
| `destroy_context` | `void destroy_context(module_t*, context_t*)`                                     | Destroy execution context                                                                                                                                                                                                                                                                       |
| `execute`         | `bool execute(context_t*, const char* fn_name)`                                   | Execute a function by name                                                                                                                                                                                                                                                                      |
| `call`            | `bool call(context_t*, const char* fn_name, const int64_t* args, uint32_t count)` | Call function with arguments                                                                                                                                                                                                                                                                    |
| `return_value`    | `int64_t return_value(context_t*)`                                                | Get integer return value                                                                                                                                                                                                                                                                        |
| `return_string`   | `const char* return_string(context_t*)`                                           | Get string return value                                                                                                                                                                                                                                                                         |
| `return_float`    | `double return_float(context_t*)`                                                 | Get float return value                                                                                                                                                                                                                                                                          |
| `alloc_string`    | `int64_t alloc_string(context_t*, const char*)`                                   | Allocate heap string for passing to scripts                                                                                                                                                                                                                                                     |
| `set_userdata`    | `void set_userdata(context_t*, void*)`                                            | Attach userdata (slot 0)                                                                                                                                                                                                                                                                        |
| `get_userdata`    | `void* get_userdata(context_t*)`                                                  | Retrieve slot 0 userdata                                                                                                                                                                                                                                                                        |
| `set_userdata_at` | `void set_userdata_at(context_t*, uint32_t slot, void*)`                          | Attach userdata to slot (0-15)                                                                                                                                                                                                                                                                  |
| `get_userdata_at` | `void* get_userdata_at(context_t*, uint32_t slot)`                                | Retrieve slot userdata                                                                                                                                                                                                                                                                          |
| `fn_address`      | `void* fn_address(module_t*, const char* name)`                                   | Get raw native function pointer for direct calls                                                                                                                                                                                                                                                |
| `execution_scope` | `explicit execution_scope(context_t* ctx)` (RAII)                                 | Set up per-thread TLS for the lifetime of the scope. Required around **any** invocation of script code from a thread not already inside `execute()`/`call()`. Nestable. See [Custom Addons](/perception/enma-lang/sdk-guide/custom-addons.md#invoking-script-closures-from-background-threads). |

### Globals

| Function         | Signature                                                         | Description                          |
| ---------------- | ----------------------------------------------------------------- | ------------------------------------ |
| `set_global`     | `bool set_global(module_t*, const char* name, int64_t value)`     | Set global variable                  |
| `get_global`     | `int64_t get_global(module_t*, const char* name)`                 | Get global variable                  |
| `has_global`     | `bool has_global(module_t*, const char* name)`                    | Check if global exists               |
| `get_global_ptr` | `int64_t* get_global_ptr(module_t*, const char* name)`            | Get direct pointer to global storage |
| `list_globals`   | `void list_globals(module_t*, vector<string>&, vector<int64_t>&)` | List all globals                     |

### Native Registration

| Function                     | Signature                                                                                                                                                                    | Description                                                                                                                                                                                                                           |
| ---------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `register_native_sret`       | `void register_native_sret(...)`                                                                                                                                             | Register a native that returns a struct by value through a hidden first-argument buffer (its own ABI)                                                                                                                                 |
| `mark_native_deallocates`    | `void mark_native_deallocates(engine_t*, const char* name, int param_index)`                                                                                                 | Tell the engine this native frees the block passed in `param_index`, so the tracked heap stops following it                                                                                                                           |
| `register_native`            | `void register_native(engine_t*, const char* signature, void* fn, uint32_t perm = 0, const char* description = nullptr)`                                                     | Signature string form with inline name (preferred), e.g. `"float64 pow(float64, float64)"`. Optional description surfaces via `extract_documentation`.                                                                                |
| `register_native`            | `void register_native(engine_t*, const char* name, const char* signature, void* fn, uint32_t perm = 0, const char* description = nullptr)`                                   | Signature string form, name separate                                                                                                                                                                                                  |
| `register_native`            | `void register_native(engine_t*, const char* name, void* fn, type_id ret, const native_param* params, uint32_t count, uint32_t perm = 0, const char* description = nullptr)` | `native_param` array form                                                                                                                                                                                                             |
| `register_typed<&fn>`        | `void register_typed<&fn>(engine_t*, const char* signature, uint32_t perm = 0)`                                                                                              | Thin template wrapper around `register_native(engine, sig, (void*)Fn)`. The native is called directly via Win64 ABI; no trampoline, no int64 bit-casting. Any arity, any type mix.                                                    |
| `register_natives`           | `void register_natives(engine_t*, const native_desc* descs, uint32_t count)`                                                                                                 | Batch-register via descriptor array                                                                                                                                                                                                   |
| `register_module`            | `void register_module(engine_t*, const char* name, const char* source)`                                                                                                      | Register a built-in Enma module so scripts can `import "<name>";`. Stores a view into `source` — no copy — so it must outlive the engine. See [Custom Addons](/perception/enma-lang/sdk-guide/custom-addons.md#source-level-modules). |
| `mark_native_returns_borrow` | `void mark_native_returns_borrow(engine_t*, const char* name)`                                                                                                               | Mark a native fn as returning a borrowed (non-owning) handle. The compiler skips scope-end dtor for `T x = fn();`. Required when the native returns a handle owned by the engine/host.                                                |
| `ENMA_SIG(s)`                | `consteval` macro wrapping a sig literal                                                                                                                                     | Compile-time syntax validator; invalid sigs fail via `static_assert`                                                                                                                                                                  |

Signature-string features — overloading, variadics, default args, const and reference params, containers, enums and delegates — are covered in [Native Functions](/perception/enma-lang/sdk-guide/native-functions.md).

### Type Builder

| Method                                                                                                          | Description                                                                                                                                                                                                                                |
| --------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `type_builder(engine_t*, const char* name, type_id id)`                                                         | Start building a type                                                                                                                                                                                                                      |
| `type_builder(engine_t*, const char* name, type_id id, const char* description)`                                | Start building a type with a docs description                                                                                                                                                                                              |
| `.field(name, offset, type)` / `.field(name, offset, type, description)`                                        | Add a field, optionally with a description                                                                                                                                                                                                 |
| `.method_typed<&Fn>(signature)`                                                                                 | **Typed wrapper** - write the method with real types. Forwards to `.method(sig, (void*)Fn)`; the native is called directly via Win64 ABI. Any arity, any type mix.                                                                         |
| `.method(signature, fn)` / `.method(signature, fn, description)`                                                | Signature string + raw `int64_t`-ABI fn, optionally with a description                                                                                                                                                                     |
| `.method(name, signature, fn)`                                                                                  | Signature string, name separate                                                                                                                                                                                                            |
| `.method(name, fn, ret, {arg_types...}, elem_ret?)`                                                             | Typed arg list form                                                                                                                                                                                                                        |
| `.method(name, fn, ret, param_count, elem_ret?)`                                                                | Count-based form (no per-arg type check)                                                                                                                                                                                                   |
| `.property(name, getter, setter, type)` / `.property(name, getter, setter, type, description)`                  | Add a property, optionally with a description                                                                                                                                                                                              |
| `.property_typed<&Get, &Set>(name, type)`                                                                       | **Typed wrapper** - property with real types; default `Set = nullptr` = read-only                                                                                                                                                          |
| `.inline_property(name, getter, setter, int32_t offset, type_id type = t_int64)`                                | Fast-path property — the compiler emits a direct load/store at the given offset instead of a native call. Use for trivial field accessors. Native fns stay registered as fallbacks.                                                        |
| `.value_type(uint32_t size, uint32_t align = 0)`                                                                | Opt this typereg into value-type semantics. Non-escaping locals stack-allocate; `T[]` stores values inline (`byte_size` per slot). Requires the C struct to be POD-shaped. Pair with `.factory_in_place(...)` and `.inline_property(...)`. |
| `.factory_in_place(fn)`                                                                                         | Write-into-buffer ctor: `int64_t fn(int64_t dst, args...)`. Used when the typereg is `.value_type`-marked; skips the per-instance `heap_alloc`. Regular `.factory(...)` still fires for explicit `new T(...)`.                             |
| `.subscript_typed<&Get, &Set>(elem_ret?)`                                                                       | **Typed wrapper** - subscript with real types; default `Set = nullptr` = read-only                                                                                                                                                         |
| `.factory_typed<&Fn>(param_count)`                                                                              | **Typed wrapper** - factory with real arg types                                                                                                                                                                                            |
| `.factory(fn, param_count)`                                                                                     | Set constructor (raw `int64_t` ABI)                                                                                                                                                                                                        |
| `.factory(fn, {arg_types...})` / `.factory(fn, {arg_types...}, description)`                                    | Typed arg list form, optionally with a description                                                                                                                                                                                         |
| `.destructor_typed<&Fn>()`                                                                                      | **Typed wrapper** - destructor takes typed pointer                                                                                                                                                                                         |
| `.destructor(fn)` / `.destructor(fn, description)`                                                              | Set scope-drop destructor (raw), optionally with a description                                                                                                                                                                             |
| `.pure_methods()`                                                                                               | Methods don't retain receiver (container types)                                                                                                                                                                                            |
| `.pure_args()`                                                                                                  | Methods don't retain any argument (value types)                                                                                                                                                                                            |
| `.subscript(get_fn, set_fn, bool elem_ret = true)`                                                              | Enable `[]` access                                                                                                                                                                                                                         |
| `.subscript(get_fn, set_fn, bool elem_ret, type_id elem_type)`                                                  | Same, naming the element type so a concrete scalar (notably a float) travels in the right register class. `subscript_typed` fills this in from the getter's return type                                                                    |
| `.iterable(len_fn, get_fn)`                                                                                     | Enable `for (v : obj)`                                                                                                                                                                                                                     |
| `.kv_iterable(len_fn, key_fn, val_fn)`                                                                          | Enable `for (k, v : obj)`                                                                                                                                                                                                                  |
| `.owns_elements()`                                                                                              | The container owns heap elements — emits the element-walk at cleanup                                                                                                                                                                       |
| `.borrows_element()` / `.transfers_element()`                                                                   | Overrides whether the LAST-registered method's returned element is the caller's to destroy. A signature states it directly: `element&` borrows, `element` transfers                                                                        |
| `.init_push(fn)`                                                                                                | `void fn(U* self, element)` — append one raw element. Backs `T v = {a, b, c}`, and rebuilds a set-shaped container during a deep copy                                                                                                      |
| `.init_push(fn, type_id elem_type)`                                                                             | Same, naming the element type so a concrete scalar (notably a float) travels in the right register class                                                                                                                                   |
| `.init_push_typed<&Push>()`                                                                                     | **Typed wrapper** — element type read off the push function's own parameter                                                                                                                                                                |
| `.bin_add(fn)` ... `.bin_mod(fn)`                                                                               | `+ - * / %` operators                                                                                                                                                                                                                      |
| `.bin_eq(fn)` `.bin_lt(fn)` `.bin_gt(fn)` `.bin_le(fn)` `.bin_ge(fn)`                                           | Comparison operators, each `bool fn(U*, U*)`                                                                                                                                                                                               |
| `.compare(fn)`                                                                                                  | Three-way `opCmp`, `int32_t fn(U*, U*)` returning `-1`/`0`/`+1` (fallback for any unset comparison)                                                                                                                                        |
| `.bin_add_assign(fn)` ... `.bin_mod_assign(fn)`                                                                 | Compound assignment `+= -= *= /= %=`. Required — a registered type without the `_assign` op is a compile error; it is never synthesized from the binary operator                                                                           |
| `.bit_and_assign(fn)` `.bit_or_assign(fn)` `.bit_xor_assign(fn)` `.shl_assign(fn)` `.shr_assign(fn)`            | Compound `&= \|= ^= <<= >>=`, same no-synthesis rule                                                                                                                                                                                       |
| `.increment(fn)` `.decrement(fn)`                                                                               | Prefix `++x` / `--x`                                                                                                                                                                                                                       |
| `.post_increment(fn)` `.post_decrement(fn)`                                                                     | Postfix `x++` / `x--`. Separate registration — postfix is never dispatched through the prefix operator                                                                                                                                     |
| `.unary_neg(fn)` `.unary_bit_not(fn)`                                                                           | Unary `-` and `~`                                                                                                                                                                                                                          |
| `.unary_log_not(fn)`                                                                                            | Unary `!`, `bool fn(U*)`                                                                                                                                                                                                                   |
| `.unary_deref(fn)`                                                                                              | Unary `*` (smart-pointer style)                                                                                                                                                                                                            |
| `.call(fn)`                                                                                                     | `obj(args...)` call operator — variadic                                                                                                                                                                                                    |
| `.arrow(fn)`                                                                                                    | `obj->member`, `void* fn(U*)` returning a struct/class instance                                                                                                                                                                            |
| `.bit_and(fn)` `.bit_or(fn)` `.bit_xor(fn)` `.shl(fn)` `.shr(fn)`                                               | Bitwise operators                                                                                                                                                                                                                          |
| `.bin_add_in_place(fn)` `.bin_sub_in_place(fn)` `.bin_mul_in_place(fn)` `.unary_neg_in_place(fn)`               | SRET binop variants for value types: `void fn(void* sret, void* lhs, void* rhs)`                                                                                                                                                           |
| `.in_place(fn)`                                                                                                 | SRET variant of the LAST-registered method: `void fn(void* sret, void* self, args...)`                                                                                                                                                     |
| `.bin_*_typed<&Fn>()`, `.unary_*_typed<&Fn>()`, `.bit_*_typed<&Fn>()`, `.shl_typed<&Fn>()`, `.shr_typed<&Fn>()` | Typed wrappers for every operator hook, write with real types                                                                                                                                                                              |
| `.compare_typed<&Fn>()`, `.increment_typed<&Fn>()`, `.decrement_typed<&Fn>()`                                   | Typed wrappers for opCmp / `++` / `--`                                                                                                                                                                                                     |
| `.hash(fn)` / `.hash_typed<&Fn>()`                                                                              | Hash function for map keys                                                                                                                                                                                                                 |
| `.copy(fn)` / `.copy_typed<&Fn>()`                                                                              | Copy hook - called on `T b = a;` copy-init. `fn(int64 src) -> int64`                                                                                                                                                                       |
| `.serialize(fn)`                                                                                                | Serialize hook. `fn(int64 value) -> int64 char_ptr`                                                                                                                                                                                        |
| `.deserialize(fn)`                                                                                              | Deserialize hook. `fn(int64 str_ptr) -> int64 value`                                                                                                                                                                                       |
| `.implements(name)`                                                                                             | Declare that this type implements an interface                                                                                                                                                                                             |
| `.as_interface()`                                                                                               | Mark this type as an interface (no instances; used as a parameter type)                                                                                                                                                                    |
| `.generic_param(name)`                                                                                          | Declare a type parameter (e.g. `"T"`, `"K"`, `"V"`); bound at var-decl site                                                                                                                                                                |
| `.requires_iface(param, iface)`                                                                                 | Constrain a generic param - its bound type must `.implements(iface)`                                                                                                                                                                       |
| `.convert(from_type, fn)`                                                                                       | Incoming implicit conversion (fires at binary ops, native call args, var-decl)                                                                                                                                                             |
| `.cast_to(target_type, fn)`                                                                                     | Outgoing conversion, `T fn(U*)` — used by `static_cast<T>(obj)` and the implicit sites                                                                                                                                                     |
| `.boxes_args()`                                                                                                 | Let registered converters apply at method-argument checks (wrapper/variant types); off by default                                                                                                                                          |
| `.copy_assign(fn)`                                                                                              | `b = a;` for an already-constructed `b`, `void fn(U* lhs, U* rhs)` — lets the type release its old resources                                                                                                                               |
| `.permission(flags)`                                                                                            | Permission gate on last-registered method                                                                                                                                                                                                  |
| `.finish()`                                                                                                     | Finalize registration                                                                                                                                                                                                                      |

Use `type_id::t_element` in arg types as a placeholder for the receiver's element type (arrays). Falls back to `t_auto` (match anything) when element type can't be determined.

### Struct Builder

| Method                                        | Description                                                |
| --------------------------------------------- | ---------------------------------------------------------- |
| `struct_builder(engine_t*, const char* name)` | Start building a struct layout                             |
| `.field(name, type, type_name?)`              | Add a field with type and optional struct type name        |
| `.packed()`                                   | C-compatible packed layout (no per-field 8-byte padding)   |
| `.finish()`                                   | Finalize registration (also auto-registers on destruction) |

```cpp
enma::struct_builder(engine, "Vec3")
    .field("x", enma::type_id::t_float64)
    .field("y", enma::type_id::t_float64)
    .field("z", enma::type_id::t_float64);
```

### Enum Builder

| Method                                      | Description                                                |
| ------------------------------------------- | ---------------------------------------------------------- |
| `enum_builder(engine_t*, const char* name)` | Start building an enum                                     |
| `.value(name, int64_t val)`                 | Add a named constant (accessed as `EnumName::ValueName`)   |
| `.finish()`                                 | Finalize registration (also auto-registers on destruction) |

```cpp
enma::enum_builder(engine, "Direction")
    .value("North", 0)
    .value("East",  1)
    .value("South", 2)
    .value("West",  3);
```

### Serialization & Linking

| Function      | Signature                                                                        | Description                                                                                                      |
| ------------- | -------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| `serialize`   | `bool serialize(module_t*, vector<uint8_t>&, bool keep_debug = true)`            | Serialize module to bytes; pass `keep_debug=false` to strip source paths + debug tables (marketplace publishing) |
| `deserialize` | `module_t* deserialize(engine_t*, const uint8_t*, size_t)`                       | Deserialize module from bytes                                                                                    |
| `link`        | `module_t* link(engine_t*, const char** names, module_t** mods, uint32_t count)` | Link multiple modules                                                                                            |

### Introspection

| Function                  | Signature                                                                       | Description                                                                                                                    |
| ------------------------- | ------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `has_function`            | `bool has_function(module_t*, const char*)`                                     | Check if function exists                                                                                                       |
| `function_count`          | `uint32_t function_count(module_t*)`                                            | Total function count                                                                                                           |
| `function_param_count`    | `uint32_t function_param_count(module_t*, const char*)`                         | Parameter count for function                                                                                                   |
| `list_functions`          | `void list_functions(module_t*, vector<string>&)`                               | List all function names                                                                                                        |
| `get_annotated_functions` | `uint32_t get_annotated_functions(module_t*, const char* ann, vector<string>&)` | Find functions with annotation                                                                                                 |
| `get_annotations`         | `uint32_t get_annotations(module_t*, const char* fn, vector<annotation_info>&)` | Get annotations on function                                                                                                    |
| `has_type`                | `bool has_type(engine_t*, const char*)`                                         | Check if a type is registered                                                                                                  |
| `list_types`              | `uint32_t list_types(engine_t*, vector<string>&)`                               | List all registered type names                                                                                                 |
| `has_struct`              | `bool has_struct(engine_t*, const char*)`                                       | Check if a struct layout is registered                                                                                         |
| `list_structs`            | `uint32_t list_structs(engine_t*, vector<string>&)`                             | List all registered struct names                                                                                               |
| `tokenize_dump`           | `char* tokenize_dump(engine_t*, const char*, size_t, const char*)`              | Dump token stream                                                                                                              |
| `ir_dump`                 | `char* ir_dump(engine_t*, const char*, size_t, const char*)`                    | Dump IR                                                                                                                        |
| `free_string`             | `void free_string(char*)`                                                       | Free dump string                                                                                                               |
| `extract_documentation`   | `std::string extract_documentation(engine_t*)`                                  | Dump every registered native + type as a C++-ish pseudo-header with descriptions                                               |
| `extract_intellisense`    | `std::vector<doc_entry_t> extract_intellisense(engine_t*)`                      | Same data as a structured vector (kind, name, parent\_type, return\_type, params, signature, description) for IDE autocomplete |

### Debug & Heap

| Function                 | Signature                                                                  | Description                                                                                                             |
| ------------------------ | -------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `set_debug_hook`         | `void set_debug_hook(module_t*, void(*)(const char*, uint32_t, int64_t*))` | Set per-line debug callback                                                                                             |
| `set_budget`             | `void set_budget(module_t*, int64_t)`                                      | Set instruction budget                                                                                                  |
| `heap_get_stats`         | `heap_stats heap_get_stats(module_t*)`                                     | Get heap statistics (alloc\_count, total\_bytes, freed\_count, freed\_bytes)                                            |
| `heap_set_memory_budget` | `void heap_set_memory_budget(module_t*, size_t)`                           | Set heap memory limit in bytes (0 = unlimited)                                                                          |
| `get_stack_trace`        | `uint32_t get_stack_trace(context_t*, stack_frame*, uint32_t max)`         | Best-effort — the functions visible in the module, NOT actual call frames                                               |
| `get_last_executed_line` | `int64_t get_last_executed_line(module_t*)`                                | Last source line executed. Survives a native fault. Returns `-1` if no line yet, `0` if `set_debug` was off at compile. |

### IDE Debugger SDK

Opt-in via `set_debug(true)` BEFORE compile. Pair with `set_optimize(false)` for full local-variable visibility. See [Debug & Heap](/perception/enma-lang/sdk-guide/debug-and-heap.md#ide-debugger-sdk) for usage.

| Function                                              | Signature                                                                                                            | Description                                                           |
| ----------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| `find_fn_at`                                          | `const em_debug_fn_t* find_fn_at(module_t*, const char* file, uint32_t line)`                                        | Containing fn for a source line                                       |
| `debug_fn_name`                                       | `const char* debug_fn_name(const em_debug_fn_t*)`                                                                    | Fn name                                                               |
| `debug_fn_local_count`                                | `uint32_t debug_fn_local_count(const em_debug_fn_t*)`                                                                | Count of locals                                                       |
| `debug_fn_param_count`                                | `uint32_t debug_fn_param_count(const em_debug_fn_t*)`                                                                | Count of params                                                       |
| `find_code_offsets`                                   | `void find_code_offsets(module_t*, const char* file, uint32_t line, size_t* out, uint32_t* out_count, uint32_t max)` | Reverse source map. `out_count` reports the total even when truncated |
| `enumerate_locals`                                    | `uint32_t enumerate_locals(const em_debug_fn_t*, em_local_info* out, uint32_t max)`                                  | Per-fn locals (name, type, slot)                                      |
| `read_local_int`                                      | `int64_t read_local_int(int64_t* frame, uint32_t slot)`                                                              | `frame` is the rbp the hook receives                                  |
| `read_local_float`                                    | `double read_local_float(int64_t* frame, uint32_t slot)`                                                             |                                                                       |
| `read_local_string`                                   | `const char* read_local_string(int64_t* frame, uint32_t slot)`                                                       |                                                                       |
| `read_local_pointer`                                  | `void* read_local_pointer(int64_t* frame, uint32_t slot)`                                                            |                                                                       |
| `set_breakpoint`                                      | `int32_t set_breakpoint(module_t*, const char* file, uint32_t line)`                                                 | Returns id                                                            |
| `clear_breakpoint`                                    | `void clear_breakpoint(module_t*, int32_t bp_id)`                                                                    |                                                                       |
| `enable_breakpoint`                                   | `void enable_breakpoint(module_t*, int32_t bp_id, bool enabled)`                                                     |                                                                       |
| `is_breakpoint_at`                                    | `bool is_breakpoint_at(module_t*, const char* file, uint32_t line)`                                                  | Hot-path check for hooks                                              |
| `list_breakpoints`                                    | `uint32_t list_breakpoints(module_t*, em_breakpoint_info* out, uint32_t max)`                                        |                                                                       |
| `set_step_mode` / `get_step_mode`                     | `void/step_mode (context_t*, step_mode)`                                                                             | `step_none`/`over`/`in`/`out`                                         |
| `set_step_baseline_depth` / `get_step_baseline_depth` | `void/int32_t (context_t*, ...)`                                                                                     | Baseline for step over/out                                            |
| `get_call_depth`                                      | `int32_t get_call_depth(int64_t* frame, module_t*)`                                                                  | rbp-walk frame counter                                                |
| `request_pause` / `resume` / `is_pause_requested`     | `void/void/bool (context_t*)`                                                                                        | One-shot pause flag; `should_pause_at` clears it on observation       |
| `should_pause_at`                                     | `bool should_pause_at(context_t*, module_t*, int64_t* frame, const char* file, uint32_t line)`                       | Combines breakpoint + pause + step-mode                               |

### Exceptions

| Function            | Signature                            | Description                      |
| ------------------- | ------------------------------------ | -------------------------------- |
| `exception_pending` | `bool exception_pending(module_t*)`  | Check if an exception is pending |
| `exception_value`   | `int64_t exception_value(module_t*)` | Get the thrown exception value   |
| `exception_type`    | `int64_t exception_type(module_t*)`  | Get the exception type hash      |
| `exception_clear`   | `void exception_clear(module_t*)`    | Clear pending exception state    |

### Events

| Function         | Signature                                                       | Description                    |
| ---------------- | --------------------------------------------------------------- | ------------------------------ |
| `register_event` | `void register_event(context_t*, int64_t id, int64_t callback)` | Register event callback        |
| `fire_event`     | `void fire_event(context_t*, int64_t id)`                       | Fire all callbacks for event   |
| `clear_events`   | `void clear_events(context_t*)`                                 | Remove all event registrations |

### Heap Allocation (for addon authors)

Enma has no tracing collector. These allocate from the tracked heap and feed the alloc/free stats.

| Function          | Signature                                    | Description                                                                                               |
| ----------------- | -------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| `heap_alloc`      | `void* heap_alloc(size_t size)`              | Allocation from the tracked heap                                                                          |
| `heap_realloc`    | `void* heap_realloc(void* ptr, size_t size)` | Resize a tracked allocation                                                                               |
| `heap_free`       | `void heap_free(void* ptr)`                  | Free a tracked allocation                                                                                 |
| `heap_is_tracked` | `bool heap_is_tracked(void* ptr)`            | Validity check — safe to call `heap_free` if true                                                         |
| `alive_token`     | `uint64_t* alive_token(void* ptr)`           | Shared liveness flag - non-zero while alive, flipped to 0 on `heap_free`. Foundation for weak references. |

### Per-Context Helpers (for addon authors)

Read the engine, context, and per-thread RNG driving the current `execute()`/`call()`. Thread-safe across concurrent engines.

| Function           | Signature                                          | Description                                               |
| ------------------ | -------------------------------------------------- | --------------------------------------------------------- |
| `active_engine`    | `engine_t* active_engine()`                        | Engine currently driving execute/call; `nullptr` outside  |
| `active_context`   | `context_t* active_context()`                      | Context currently driving execute/call; `nullptr` outside |
| `random_u64`       | `uint64_t random_u64()`                            | Raw 64-bit draw from the per-context mt19937              |
| `random_double`    | `double random_double()`                           | Uniform `[0, 1)` draw                                     |
| `random_int_range` | `int64_t random_int_range(int64_t lo, int64_t hi)` | Uniform `[lo, hi)` draw                                   |
| `random_seed`      | `void random_seed(uint64_t seed)`                  | Seed the per-context rng                                  |

### Error Handling

| Function             | Signature                                                     | Description                                           |
| -------------------- | ------------------------------------------------------------- | ----------------------------------------------------- |
| `last_error`         | `error_info last_error(engine_t*)`                            | Get last error details                                |
| `last_error_message` | `const char* last_error_message(engine_t*)`                   | Get last error message                                |
| `last_warnings`      | `uint32_t last_warnings(engine_t*, std::vector<error_info>&)` | Diagnostics the last compile reported without failing |
| `runtime_error`      | `void runtime_error(const char* msg)`                         | Addon: raise a runtime error from native code         |
| `runtime_exception`  | `void runtime_exception(const char* msg)`                     | Addon: raise an exception from native code            |

### Addon Registration

| Function                                                        | Description                                                                                                                                                             |
| --------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `register_all_addons(engine_t*, bool enable_filesystem = true)` | Register the eleven standard addons; pass `false` to suppress the file addon                                                                                            |
| `register_addon_core(engine_t*)`                                | `print` / `println`                                                                                                                                                     |
| `register_addon_math(engine_t*)`                                | Scalar math, vec2/vec3/vec4, quat, mat4                                                                                                                                 |
| `register_addon_simd(engine_t*)`                                | SIMD operations                                                                                                                                                         |
| `register_addon_atomic(engine_t*)`                              | Atomic int32 / int64 types and barriers                                                                                                                                 |
| `register_addon_bits(engine_t*)`                                | Bit intrinsics (popcount/clz/ctz/rotl/rotr/bswap/parity/bit\_reverse)                                                                                                   |
| `register_addon_time(engine_t*)`                                | Timestamps, calendar, ISO 8601, sleep                                                                                                                                   |
| `register_addon_regex(engine_t*)`                               | Regex addon type                                                                                                                                                        |
| `register_addon_file(engine_t*)`                                | File I/O, gated by `PERM_FILE`                                                                                                                                          |
| `register_addon_thread(engine_t*)`                              | mutex, lock\_guard, cond\_var                                                                                                                                           |
| `register_addon_json(engine_t*)`                                | JSON parse/stringify + json\_value type                                                                                                                                 |
| `register_addon_std(engine_t*)`                                 | The `std` prelude — `std::string`/`std::wstring`, `std::vector`, `std::map`/`std::unordered_map`, `std::set`/`std::unordered_set`, `std::list`, `std::any` (pure `.em`) |

### Reflection

| Function                               | Signature                                                                           | Description                                                             |
| -------------------------------------- | ----------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| `find_type_reg`                        | `const type_reg_t* find_type_reg(engine_t*, type_id)`                               | Lookup type registration by id                                          |
| `find_type_reg_by_name`                | `const type_reg_t* find_type_reg_by_name(engine_t*, const char*)`                   | Lookup type registration by name                                        |
| `type_reg_name`                        | `const char* type_reg_name(const type_reg_t*)`                                      | Name of type                                                            |
| `type_reg_id`                          | `type_id type_reg_id(const type_reg_t*)`                                            | Type ID                                                                 |
| `type_reg_factory`                     | `void* type_reg_factory(const type_reg_t*)`                                         | Factory fn pointer (nullable)                                           |
| `type_reg_factory_param_count`         | `uint32_t type_reg_factory_param_count(const type_reg_t*)`                          | Factory arity                                                           |
| `type_reg_dtor`                        | `void* type_reg_dtor(const type_reg_t*)`                                            | Destructor fn pointer (nullable)                                        |
| `type_reg_copy`                        | `void* type_reg_copy(const type_reg_t*)`                                            | Copy hook fn pointer (nullable)                                         |
| `type_reg_hash`                        | `void* type_reg_hash(const type_reg_t*)`                                            | Hash fn pointer (nullable)                                              |
| `type_reg_compare`                     | `void* type_reg_compare(const type_reg_t*)`                                         | Three-way compare fn pointer (nullable)                                 |
| `type_reg_op_eq`                       | `void* type_reg_op_eq(const type_reg_t*)`                                           | Equality fn pointer (nullable)                                          |
| `type_reg_serialize`                   | `void* type_reg_serialize(const type_reg_t*)`                                       | Serialize hook fn pointer (nullable)                                    |
| `type_reg_deserialize`                 | `void* type_reg_deserialize(const type_reg_t*)`                                     | Deserialize hook fn pointer (nullable)                                  |
| `type_reg_convert_from`                | `void* type_reg_convert_from(const type_reg_t*, type_id from)`                      | Conversion fn for a given source type (nullable)                        |
| `type_reg_method`                      | `void* type_reg_method(const type_reg_t*, const char* name)`                        | Method fn pointer by name (nullable)                                    |
| `type_reg_method_count`                | `uint32_t type_reg_method_count(const type_reg_t*)`                                 | Number of registered methods                                            |
| `type_reg_method_name_at`              | `const char* type_reg_method_name_at(const type_reg_t*, uint32_t idx)`              | Method name by index                                                    |
| `type_reg_implements`                  | `bool type_reg_implements(const type_reg_t*, const char* iface)`                    | Type implements a named interface                                       |
| `type_reg_implements_count`            | `uint32_t type_reg_implements_count(const type_reg_t*)`                             | Number of declared interfaces                                           |
| `type_reg_implements_at`               | `const char* type_reg_implements_at(const type_reg_t*, uint32_t idx)`               | Interface name by index                                                 |
| `type_reg_is_interface`                | `bool type_reg_is_interface(const type_reg_t*)`                                     | True if this type is itself an interface                                |
| `type_reg_generic_param_count`         | `uint32_t type_reg_generic_param_count(const type_reg_t*)`                          | Number of declared generic parameters                                   |
| `type_reg_generic_param_at`            | `const char* type_reg_generic_param_at(const type_reg_t*, uint32_t idx)`            | Generic parameter name by index                                         |
| `type_reg_generic_constraint_count`    | `uint32_t type_reg_generic_constraint_count(const type_reg_t*)`                     | Number of declared (param, iface) constraints                           |
| `type_reg_generic_constraint_param_at` | `const char* type_reg_generic_constraint_param_at(const type_reg_t*, uint32_t idx)` | Constrained parameter name by index                                     |
| `type_reg_generic_constraint_iface_at` | `const char* type_reg_generic_constraint_iface_at(const type_reg_t*, uint32_t idx)` | Required interface name by index                                        |
| `interface_method_fn`                  | `void* interface_method_fn(engine_t*, type_id concrete, const char* method)`        | Resolve concrete type's method fn - used inside interface-typed natives |

All reflection accessors are null-safe (pass `nullptr` → return empty/0/nullptr as appropriate).

### Fault Diagnosis

| Function                 | Signature                              | Description                                                                                                                                     |
| ------------------------ | -------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| `budget_exhausted`       | `bool budget_exhausted(module_t*)`     | Whether the last run stopped on the instruction budget — an exhausted budget reports failure, so the returned value is not the program's answer |
| `last_runtime_error_msg` | `const char* last_runtime_error_msg()` | Message for the most recent runtime error                                                                                                       |
| `last_jit_fault`         | `jit_fault_info last_jit_fault()`      | Register state and faulting `rip` for the last native fault                                                                                     |
| `text_base`              | `void* text_base(module_t*)`           | Base of the module's generated code; `rip - text_base` gives the offset                                                                         |
| `text_size`              | `size_t text_size(module_t*)`          | Size of that region                                                                                                                             |
| `debug_dump_source_map`  | see header                             | Dump the IR-to-source mapping                                                                                                                   |
| `debug_dump_text_bytes`  | see header                             | Dump raw generated bytes                                                                                                                        |

### Heap Allocation

| Function           | Signature                                             | Description                                              |
| ------------------ | ----------------------------------------------------- | -------------------------------------------------------- |
| `heap_alloc_typed` | `void* heap_alloc_typed(size_t size, heap_kind kind)` | Allocate from the tracked heap tagged with a `heap_kind` |

### Type IDs

| Name            | Value | Description                                                |
| --------------- | ----- | ---------------------------------------------------------- |
| `t_void`        | 0     | Void                                                       |
| `t_bool`        | 1     | Boolean                                                    |
| `t_char`        | 2     | 8-bit character                                            |
| `t_wchar`       | 3     | 16-bit character                                           |
| `t_int32`       | 4     | 32-bit signed int                                          |
| `t_int64`       | 5     | 64-bit signed int                                          |
| `t_uint8`       | 6     | 8-bit unsigned int                                         |
| `t_uint16`      | 7     | 16-bit unsigned int                                        |
| `t_uint32`      | 8     | 32-bit unsigned int                                        |
| `t_uint64`      | 9     | 64-bit unsigned int                                        |
| `t_aint8`       | 10    | Atomic int8                                                |
| `t_aint16`      | 11    | Atomic int16                                               |
| `t_aint32`      | 12    | Atomic int32                                               |
| `t_aint64`      | 13    | Atomic int64                                               |
| `t_float32`     | 14    | 32-bit float                                               |
| `t_float64`     | 15    | 64-bit float                                               |
| `t_class`       | 20    | Class                                                      |
| `t_struct`      | 21    | Struct                                                     |
| `t_enum`        | 22    | Enum                                                       |
| `t_function`    | 23    | Function pointer                                           |
| `t_lambda`      | 24    | Lambda/closure                                             |
| `t_pointer`     | 25    | Raw pointer                                                |
| `t_auto`        | 26    | Auto-inferred                                              |
| `t_null`        | 27    | Null literal                                               |
| `t_element`     | 28    | Receiver's element type (container methods)                |
| `t_int16`       | 40    | Signed 16-bit (distinct from `t_wchar`, which is unsigned) |
| `t_custom_base` | 128   | Base for custom types                                      |

### Constants

| Name        | Value  | Description                                   |
| ----------- | ------ | --------------------------------------------- |
| `PERM_FFI`  | `0x01` | Permission flag for `[[dll(...)]]` FFI access |
| `PERM_FILE` | `0x02` | Permission flag for the file addon            |
