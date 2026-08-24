> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/introspection.md).

# Introspection

## Listing Functions

```cpp
std::vector<std::string> fns;
list_functions(mod, fns);
for (auto& name : fns) {
    printf("  %s\n", name.c_str());
}
```

## Function Count

```cpp
uint32_t count = function_count(mod);
```

## Checking Function Existence

```cpp
if (has_function(mod, "on_damage")) {
    execute(ctx, "on_damage");
}
```

## Parameter Count

```cpp
uint32_t params = function_param_count(mod, "add");
```

## Querying Annotations

Find all functions with a specific annotation:

```cpp
std::vector<std::string> tagged;
get_annotated_functions(mod, "priority", tagged);
for (auto& fn : tagged) {
    printf("  %s has [[priority]]\n", fn.c_str());
}
```

Get all annotations on a specific function:

```cpp
std::vector<annotation_info> anns;
get_annotations(mod, "attack", anns);
for (auto& ann : anns) {
    printf("  [[%s", ann.name);
    if (ann.arg_count > 0) {
        printf("(");
        for (uint32_t i = 0; i < ann.arg_count; ++i) {
            if (i > 0) printf(", ");
            printf("%s", ann.args[i]);
        }
        printf(")");
    }
    printf("]]\n");
}
```

For a script with:

```cpp
[[priority(5)]]
[[category("combat")]]
void attack(int32 target) { }
```

This prints:

```cpp
  [[priority(5)]]
  [[category(combat)]]
```

## Debug Dumps

Dump the token stream:

```cpp
char* tokens = tokenize_dump(e, src, len, "script.em");
printf("%s\n", tokens);
free_string(tokens);
```

Dump the IR (intermediate representation):

```cpp
char* ir = ir_dump(e, src, len, "script.em");
printf("%s\n", ir);
free_string(ir);
```

Always free dump strings with `free_string()`.

## Type and Struct Queries

Check for registered types and structs, or list all of them:

```cpp
if (has_type(e, "vec3_t")) {
    printf("vec3_t is registered\n");
}

if (has_struct(e, "point_t")) {
    printf("point_t is registered\n");
}
```

```cpp
std::vector<std::string> types;
list_types(e, types);
for (auto& t : types) {
    printf("  type: %s\n", t.c_str());
}

std::vector<std::string> structs;
list_structs(e, structs);
for (auto& s : structs) {
    printf("  struct: %s\n", s.c_str());
}
```

## Native Function Pointer

Get the raw function pointer for a compiled function. Bypasses the context layer.

```cpp
void* addr = fn_address(mod, "compute");
auto fn = reinterpret_cast<int64_t(*)()>(addr);
int64_t result = fn();
```

For functions with parameters:

```cpp
void* addr = fn_address(mod, "add");
auto fn = reinterpret_cast<int64_t(*)(int64_t, int64_t)>(addr);
int64_t result = fn(10, 20);  // 30
```

The returned pointer is valid for the lifetime of the module. If `fn_address` cannot find the function, it returns `nullptr`.

## Type Registry Reflection

Addons can query any registered type's hooks without a compile-time dependency on the concrete type. All accessors are null-safe.

### Lookup

```cpp
const type_reg_t* reg = find_type_reg(engine, some_type_id);
// or by name:
reg = find_type_reg_by_name(engine, "color_t");
```

Both return `nullptr` if the type isn't registered.

### Accessors

```cpp
const char* name            = type_reg_name(reg);
type_id     id              = type_reg_id(reg);
void*       factory_fn      = type_reg_factory(reg);
uint32_t    factory_args    = type_reg_factory_param_count(reg);
void*       dtor_fn         = type_reg_dtor(reg);
void*       copy_fn         = type_reg_copy(reg);
void*       hash_fn         = type_reg_hash(reg);
void*       compare_fn      = type_reg_compare(reg);
void*       op_eq_fn        = type_reg_op_eq(reg);
void*       serialize_fn    = type_reg_serialize(reg);
void*       deserialize_fn  = type_reg_deserialize(reg);
void*       convert_from_fn = type_reg_convert_from(reg, source_type_id);

void*       method_fn       = type_reg_method(reg, "methodName");
uint32_t    method_count    = type_reg_method_count(reg);
const char* method_name     = type_reg_method_name_at(reg, 0);

bool        is_iface        = type_reg_is_interface(reg);
bool        implements      = type_reg_implements(reg, "Stream");
uint32_t    iface_count     = type_reg_implements_count(reg);
const char* iface_name      = type_reg_implements_at(reg, 0);

uint32_t    param_count     = type_reg_generic_param_count(reg);
const char* param_name      = type_reg_generic_param_at(reg, 0);
uint32_t    con_count       = type_reg_generic_constraint_count(reg);
const char* con_param       = type_reg_generic_constraint_param_at(reg, 0);
const char* con_iface       = type_reg_generic_constraint_iface_at(reg, 0);
```

### Typical pattern: dispatch through reflection

```cpp
// Pretty-printer that handles any type with a serialize hook:
std::string pretty(engine_t* e, type_id t, int64_t value) {
    const type_reg_t* reg = find_type_reg(e, t);
    if (!reg) return "<unregistered>";
    auto* ser = type_reg_serialize(reg);
    if (!ser) return std::string("<") + type_reg_name(reg) + ">";
    int64_t str = ((int64_t(*)(int64_t))ser)(value);
    std::string out(reinterpret_cast<const char*>(str));
    std::free(reinterpret_cast<void*>(str));
    return out;
}
```

### Listing generic constraints

```cpp
const type_reg_t* reg = find_type_reg_by_name(e, "color_t");
for (uint32_t i = 0; i < type_reg_generic_constraint_count(reg); ++i) {
    printf("%s must implement %s\n",
        type_reg_generic_constraint_param_at(reg, i),
        type_reg_generic_constraint_iface_at(reg, i));
}
```

No hardcoding of type lists; any addon that registers a `.serialize(fn)` hook participates automatically.

## Documentation Extraction

Two SDK functions dump the full registered API surface - every global native, every registered type and its methods/fields/properties/factory/destructor - in two forms.

### `extract_documentation(engine)`

Returns a C++-ish pseudo-header as a `std::string`. Drop it into a file or render it as a code block in generated docs. Descriptions (attached via the `description` parameter on `register_native`, `.method`, `.factory`, `.destructor`, `.property`, `.field`, and the `type_builder` constructor) appear as `// ...` comments above each entry.

```cpp
std::string doc = enma::extract_documentation(engine);
std::ofstream("enma_api.h") << doc;
```

Example output:

```cpp
// RGBA color, 8 bits per channel
struct color {
    // construct color(r, g, b, a) with each channel in 0..255
    color(uint8, uint8, uint8, uint8);
    // red channel
    uint8 r();
    // green channel
    uint8 g();
    // blue channel
    uint8 b();
    // alpha channel
    uint8 a();
    // free color memory
    ~color();
}

// print a message to the host's stdout
int64 log(const char* msg);
```

### `extract_intellisense(engine)`

Returns a `std::vector<doc_entry_t>` with one entry per registered item. Use this to populate an IDE's autocomplete database, generate tooltips, or diff API shapes across versions.

```cpp
struct doc_param_t {
    std::string type;
    std::string name;
};

enum class doc_entry_kind : uint8_t {
    global_function, type, factory, destructor,
    method, field, property, operator_hook,
};

struct doc_entry_t {
    doc_entry_kind   kind;
    std::string      name;
    std::string      parent_type;   // "" for globals/types themselves
    std::string      return_type;
    std::vector<doc_param_t> params;
    std::string      signature;     // full sig string as registered
    std::string      description;
};
```

Example - filter to methods on the `color` type:

```cpp
auto entries = enma::extract_intellisense(engine);
for (auto& e : entries) {
    if (e.kind == doc_entry_kind::method && e.parent_type == "color") {
        printf("color.%s %s\n", e.name.c_str(), e.description.c_str());
    }
}
```

Both functions are safe to call at any point after the relevant addons, natives, and types have been registered. They don't touch engine state.
