> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/compile-and-run.md).

# Compile & Run

```cpp
module_t*   compile(engine_t* engine, const char* source, size_t len, const char* filename)
module_t*   compile_file(engine_t* engine, const char* path)
void        module_destroy(module_t* mod)

context_t*  create_context(module_t* mod, size_t stack_size = 0)
void        destroy_context(module_t* mod, context_t* ctx)
context_t*  active_context()

bool        execute(context_t* ctx, const char* fn_name)
bool        call(context_t* ctx, const char* fn_name, const int64_t* args, uint32_t argc)
int64_t     return_value(context_t* ctx)
const char* return_string(context_t* ctx)
double      return_float(context_t* ctx)
int64_t     alloc_string(context_t* ctx, const char* str)

void        set_userdata(context_t* ctx, void* data)              // slot 0
void*       get_userdata(context_t* ctx)
void        set_userdata_at(context_t* ctx, uint32_t slot, void* data)
void*       get_userdata_at(context_t* ctx, uint32_t slot)

bool        has_function(module_t* mod, const char* name)
uint32_t    function_param_count(module_t* mod, const char* name)
void        list_functions(module_t* mod, std::vector<std::string>& out)

bool        set_global(module_t* mod, const char* name, int64_t value)
int64_t     get_global(module_t* mod, const char* name)
bool        has_global(module_t* mod, const char* name)
int64_t*    get_global_ptr(module_t* mod, const char* name)
void        list_globals(module_t* mod, std::vector<std::string>& names,
                         std::vector<int64_t>& values)
```

## Compilation

### Compile from Source

```cpp
const char* src = "int32 main() { return 42; }";
module_t* mod = compile(e, src, strlen(src), "script.em");
```

The filename is used for error messages and source maps; it doesn't need to be a real file.

### Compile from File

```cpp
module_t* mod = compile_file(e, "scripts/game.em");
```

Include and module paths are resolved relative to the engine's configured paths.

### What Compilation Produces

`compile()` produces a module holding native machine code, ready to execute.

### Checking for Errors

Compilation errors surface via the engine's error state:

```cpp
module_t* mod = compile(e, src, len, "script.em");
error_info err = last_error(e);
if (err.code != 0) {
    printf("[%s:%d:%d] %s\n",
        err.file.c_str(), err.line, err.column, err.message.c_str());
}
```

## Execution

### Creating a Context

Holds execution state (stack, locals, TLS). Each context is independent.

```cpp
context_t* ctx = create_context(mod);
```

You can create multiple contexts from the same module for concurrent execution.

The full signature is `create_context(module_t* mod, size_t stack_size = 0)`. Default `stack_size = 0` runs compiled code on the OS thread stack, which is what you want for almost every host - it keeps OS guard pages and full C++ EH unwinding through native boundaries. Pass a non-zero `stack_size` to allocate a separate script stack via `set_stack_allocators`. Caveat: a switched stack breaks C++ exception unwinding across the native boundary because the OS dispatcher validates frames against the thread's TEB stack range. Only use it if every native call your scripts hit catches its own C++ exceptions before returning.

### Running a Function

```cpp
bool ok = execute(ctx, "main");
```

`true` on success; `false` on runtime fault, budget exceeded, missing function, or a function this entry point cannot call — see [What Each Entry Point Can Call](#what-each-entry-point-can-call).

### Reading Return Values

```cpp
int64_t  i = return_value(ctx);   // integer return
const char* s = return_string(ctx); // string return, or nullptr
double   f = return_float(ctx);    // float return
```

Use the function that matches the return type of the called function.

`return_string` answers only for a function that has characters to give, and reports `nullptr` for one that does not — see [Reading String Returns](#reading-string-returns).

### Context Userdata

Attach application state to a context. It's accessible from native functions; 16 slots available so multiple addons can coexist without stomping each other.

```cpp
struct game_state { int32_t level; int32_t score; };
game_state state = { 5, 1000 };

set_userdata(ctx, &state);                 // slot 0 (aliased)
set_userdata_at(ctx, 1, &some_other_ptr);  // slot 1

// inside a native function:
auto* gs = static_cast<game_state*>(get_userdata(ctx));           // slot 0
auto* p  = get_userdata_at(ctx, 1);                                // slot 1

// or via active_context(), no need to thread ctx through:
auto* gs2 = static_cast<game_state*>(get_userdata(active_context()));
```

Slots are 0-15; out-of-range is a no-op / `nullptr`. Slot 0 is the same storage as `set_userdata`/`get_userdata`.

### Calling Script Code from Background Threads

`execute()` and `call()` set up enma's per-thread TLS automatically. To invoke script code from a thread that is *not* already inside one of those, wrap it in an `execution_scope` first — without it, the first native that touches TLS dereferences nullptr. See [Custom Addons](/perception/enma-lang/sdk-guide/custom-addons.md#invoking-script-closures-from-background-threads).

## Calling Functions

### Call with Arguments

```cpp
int64_t args[] = { 10, 20 };
call(ctx, "add", args, 2);
int64_t result = return_value(ctx);  // 30
```

Arguments pass as `int64_t`. Floats must be bit-cast:

```cpp
double val = 3.14;
int64_t bits;
memcpy(&bits, &val, 8);

int64_t args[] = { bits };
call(ctx, "process_float", args, 1);

double result;
int64_t rbits = return_value(ctx);
memcpy(&result, &rbits, 8);
```

### Passing Strings

Allocate via Enma's heap:

```cpp
int64_t str = alloc_string(ctx, "hello world");
int64_t args[] = { str };
call(ctx, "process_text", args, 1);
```

Runtime manages the string's lifetime, don't free it manually.

### What Each Entry Point Can Call

A function reads its arguments from slots the caller fills. `execute` names none and allocates the buffer a by-value struct return travels through; `call` pushes exactly the arguments it is given and nothing else.

Both check the function's declared signature **before** running it, and return `false` with a message on `last_error(e)` when they cannot fill every slot — a function invoked with slots nobody set would read stray values as arguments, and running it first is what does the damage.

```cpp
if (!call(ctx, "damage", nullptr, 0)) {
    printf("not run: %s\n", last_error_message(e));
}
// not run: `damage` takes 1 argument and 0 were supplied — the rest
//          would be read from registers nothing set
```

Supply them and the same function runs:

```cpp
int64_t args[] = { 30 };
if (call(ctx, "damage", args, 1)) {
    printf("hp: %lld\n", (long long)return_value(ctx));   // hp: 70
}
```

Three shapes are refused:

* **more argument slots than the caller filled.** A method counts its receiver as one, so `execute` cannot reach one. Pass the object address yourself and `call` can — the receiver is the first argument:

```cpp
int64_t handle = 0;
if (execute(ctx, "spawn")) handle = return_value(ctx);

if (call(ctx, "player::health", &handle, 1)) {
    printf("hp: %lld\n", (long long)return_value(ctx));   // hp: 80
}
```

* **a by-value struct return through `call`**, which has no result buffer to give it. Read one through `execute`, which allocates that buffer.
* **a variadic function through either.** Its argument count travels beside the arguments, and a script call site is what establishes it. Reach one by invoking a script function that calls it:

```c
int64 total(...)      { return __va_count; }
int64 count_three()   { return total(1, 2, 3); }
```

```cpp
execute(ctx, "count_three");
printf("count: %lld\n", (long long)return_value(ctx));    // count: 3
```

A function whose signature the module publishes nothing for is not refused: with no slot count there is no evidence the call is malformed.

### Reading String Returns

```cpp
execute(ctx, "get_name");
const char* name = return_string(ctx);
if (name) printf("name: %s\n", name);
```

The returned pointer is valid until the next call.

Two return types have characters to give:

* a `char*` return — the result is the buffer itself;
* a string wrapper returned **by value** — a type with a `char*`/`wchar*` converting constructor. The characters are read from the field that type's destructor releases, so members may be declared in any order.

Anything else returns `nullptr`: an integer, `bool` or floating-point result, a wide or non-character pointer, a type whose destructor releases no single owned buffer (it frees several, walks a node chain, or only borrows its characters), and any run that failed. Which case applies is decided from the function's declared return type, so a numeric result is never handed back as text.

An empty string is a string: it comes back as a non-null pointer to `""`. So `nullptr` means "no string to read", never "the string was empty", and a host can tell the two apart:

```cpp
const char* s = return_string(ctx);
if (!s)        printf("this function returns no string\n");
else if (!*s)  printf("it returned an empty string\n");
else           printf("it returned: %s\n", s);
```

A by-value string wrapper needs a result buffer that only `execute` provides, so read one through `execute`; a `char*` return needs no buffer and reads through either. A run the entry point refused has no string either — see [What Each Entry Point Can Call](#what-each-entry-point-can-call).

### Checking Function Existence

```cpp
if (has_function(mod, "on_tick")) {
    execute(ctx, "on_tick");
}
```

### Getting Parameter Count

```cpp
uint32_t params = function_param_count(mod, "add");  // 2
```

## Globals

### Setting Globals

`set_global` writes a global the script already declares — it does not create one, and returns `false` when the name is not found:

```cpp
set_global(mod, "max_hp", 100);
set_global(mod, "player_id", 42);
```

The script must declare them; `set_global` then writes the slot:

```c
int32 max_hp = 0;      // declared here, overwritten by set_global
int32 player_id = 0;

int32 main() {
    println(std::to_string(max_hp));  // 100
    return 0;
}
```

### Reading Globals

```cpp
int64_t score = get_global(mod, "score");
```

### Checking Existence

```cpp
if (has_global(mod, "score")) {
    int64_t score = get_global(mod, "score");
}
```

### Direct Pointer Access

Direct pointer to global storage (skips the call overhead):

```cpp
int64_t* hp_ptr = get_global_ptr(mod, "player_hp");
*hp_ptr = 100;          // write
int64_t hp = *hp_ptr;   // read, no function call overhead
```

The pointer is valid for the lifetime of the module.

### Listing All Globals

```cpp
std::vector<std::string> names;
std::vector<int64_t> values;
list_globals(mod, names, values);

for (size_t i = 0; i < names.size(); ++i) {
    printf("%s = %lld\n", names[i].c_str(), values[i]);
}
```

## Cleanup

```cpp
destroy_context(mod, ctx);
module_destroy(mod);
```

Destroy all contexts created from this module before destroying the module.

## Example

```cpp
module_t* mod = compile(e, src, len, "game.em");

context_t* ctx = create_context(mod);
execute(ctx, "init");

// game loop
while (running) {
    execute(ctx, "update");
    execute(ctx, "render");
}

execute(ctx, "shutdown");
destroy_context(mod, ctx);
module_destroy(mod);
```
