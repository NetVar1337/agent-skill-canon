> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/quick-start.md).

# Quick Start

Minimal code to embed Enma in your application.

## Files You Need

* `enma.h` (umbrella) or `include/sdk.h` (just the SDK)
* One Windows lib - pick the one matching your project's CRT flag:
  * `windows/enma_x64static_mt.lib` for `/MT` (static CRT)
  * `windows/enma_x64static_md.lib` for `/MD` (dynamic CRT)
  * Mixing them produces a `RuntimeLibrary` mismatch at link time.
* Linux: `enma_langx64.lib`
* The addons you intend to register, from `addons/em_addon_*.cpp`

**The addons are not inside the lib.** They ship as standalone translation units depending only on `sdk.h`, so a call to `register_all_addons` without them fails to link with `undefined symbol: enma::register_addon_core` and one line per addon. Compile the ones you want alongside your own sources — that is also how you get a smaller binary, by leaving out the ones you do not register.

**Link with a bigger stack.** The compiler needs more than the default 1 MB Windows stack; without it the program links but fails on the first `compile()` call. Pass `/STACK:8388608`.

## Minimal Example

```cpp
#include "sdk.h"
using namespace enma;

int main() {
    // 1. Create engine
    engine_t* e = create();
    register_all_addons(e);
    set_optimize(e, true);

    // 2. Compile a script
    const char* src = R"(
        int32 main() {
            println("Hello from Enma!");
            return 42;
        }
    )";
    module_t* mod = compile(e, src, strlen(src), "hello.em");

    // 3. Execute
    context_t* ctx = create_context(mod);
    execute(ctx, "main");
    int64_t result = return_value(ctx);
    printf("result: %lld\n", result);  // 42

    // 4. Cleanup
    destroy_context(mod, ctx);
    module_destroy(mod);
    destroy(e);
    return 0;
}
```

## Compile & Link

The app, the addon sources, and the lib, with a larger stack.

```bash
clang-cl /std:c++latest /O2 /EHa /MT /Iinclude ^
    app.cpp addons/em_addon_*.cpp ^
    windows/enma_x64static_mt.lib ^
    /Fe:app.exe /link user32.lib /STACK:8388608
```

Swap `/MT` + `enma_x64static_mt.lib` for `/MD` + `enma_x64static_md.lib` to match a dynamic-CRT project. MSVC's `cl` takes the same arguments.

```bash
# Linux
g++ -std=c++23 -O2 main.cpp addons/em_addon_*.cpp -lenma -o app
```

Naming the addon sources individually instead of globbing is what keeps a binary small — an addon you never register costs nothing if you never compile it.

## What Just Happened

```cpp
engine_t*  create()
void       register_all_addons(engine_t* engine, bool enable_filesystem = true)
void       set_optimize(engine_t* engine, bool enabled)
module_t*  compile(engine_t* engine, const char* source, size_t len, const char* filename)
context_t* create_context(module_t* mod, size_t stack_size = 0)
bool       execute(context_t* ctx, const char* fn_name)
int64_t    return_value(context_t* ctx)
void       destroy_context(module_t* mod, context_t* ctx)
void       module_destroy(module_t* mod)
void       destroy(engine_t* engine)
```

`register_all_addons` registers the eleven built-in addons — core, math, simd, atomic, bits, time, regex, file, thread, json, and std, the string/container/algorithm prelude. Pass `false` to suppress the file addon, or register selectively instead.

`compile` produces a module of native x64 machine code; there is no bytecode and no interpreter. `create_context` allocates the execution context — stack, locals, TLS — and `execute` runs one named function in it. Read the result with `return_value`, or `return_string()` / `return_float()` for the other shapes.

Clean up in reverse: context, module, engine.

## Selective Addon Registration

If you don't need all addons:

```cpp
engine_t* e = create();
register_addon_core(e);     // print functions
register_addon_math(e);     // math functions
register_addon_std(e);      // std::string / std::vector / std::map / ... (the std prelude)
// skip simd, regex, file, thread, json
```

## Error Handling

`compile()` returns `nullptr` on failure and `last_error()` describes it.

```cpp
error_info  last_error(engine_t* engine)          // code, message, file, line, column
const char* last_error_message(engine_t* engine)  // the message alone
```

```cpp
module_t* mod = compile(e, src, len, "script.em");
if (!mod) {
    error_info err = last_error(e);
    printf("error: %s at %s:%d:%d\n",
        err.message.c_str(), err.file.c_str(), err.line, err.column);
    // error: expected ; got identifier at script.em:1:23
}
```
