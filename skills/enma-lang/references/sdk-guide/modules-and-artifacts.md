> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/sdk-guide/modules-and-artifacts.md).

# Modules & Artifacts

## Precompiled Binaries (.emb)

Compile once, distribute the binary; no source required at runtime.

### Serialize

```cpp
module_t* mod = compile_file(e, "library.em");

std::vector<uint8_t> data;
serialize(mod, data);  // keep_debug=true by default

// write to file
FILE* f = fopen("library.emb", "wb");
fwrite(data.data(), 1, data.size(), f);
fclose(f);
```

#### keep\_debug — strip source paths for distribution

`serialize(module_t*, vector<uint8_t>&, bool keep_debug = true)`

With `keep_debug=false` the artifact carries no source positions and no local variable names. Use it for distribution, so an author's absolute source paths are not baked into the binary. The trade is at runtime: a module deserialized from it reports `get_last_executed_line` as 0 and produces empty stack traces.

```cpp
serialize(mod, data, /*keep_debug*/ false);
```

### Deserialize

```cpp
// read from file
std::vector<uint8_t> data = read_binary_file("library.emb");

module_t* mod = deserialize(e, data.data(), data.size());
context_t* ctx = create_context(mod);
execute(ctx, "main");
```

`deserialize` returns `nullptr` and sets `last_error_message` when the bytes are truncated, corrupt, built by a different compiler build, or need a permission this engine does not grant. Check it before creating a context.

## Linking Multiple Modules

Combine separately compiled modules, resolving cross-module calls.

```cpp
module_t* math_mod = compile_file(e, "math.em");
module_t* game_mod = compile_file(e, "game.em");

const char* names[] = { "math", "game" };
module_t*   mods[]  = { math_mod, game_mod };
module_t*   linked  = link(e, names, mods, 2);

context_t* ctx = create_context(linked);
execute(ctx, "main");
```

In the script, linked modules are accessed by their name prefix:

```c
// game.em can call:
int32 r = math::sqrt_int(16);
```

## Hot Reload

Replace a module's code at runtime without destroying the engine or losing registered types.

### Basic Reload

```cpp
const char* new_src = R"(
    int32 main() {
        println("updated version!");
        return 2;
    }
)";
bool ok = reload(mod, new_src, strlen(new_src), "script.em");
```

### Running the New Code

Existing contexts pick up the new code on the next `execute()` call, no need to recreate them:

```cpp
execute(ctx, "main");                        // runs v1
reload(mod, new_src, len, "script.em");
execute(ctx, "main");                        // runs v2 on the same ctx
```

### Typical Hot Reload Loop

```cpp
module_t* mod = compile_file(e, "game.em");
context_t* ctx = create_context(mod);

while (running) {
    if (file_changed("game.em")) {
        std::string src = read_file("game.em");
        reload(mod, src.c_str(), src.size(), "game.em");
    }
    execute(ctx, "update");
}
```

Reload is the teardown of one program followed by the startup of another: the old program's globals are **destroyed**, then the new program's initializers run. No value is carried across — to keep one, `get_global` it before and `set_global` it after, where you know its type.

Either the whole reload succeeds or nothing changes; on failure the module keeps running the program it was already running. It fails while any of the module's functions is executing. Budget, debug hook and allocator hooks belong to the host, not the program, and stay in force. Every borrowed pointer from the old program is invalidated, including reflected annotation strings.

## Module Cleanup

```cpp
module_destroy(math_mod);
module_destroy(game_mod);
module_destroy(linked);
```

Destroy each module separately. The linked module does not own its inputs.
