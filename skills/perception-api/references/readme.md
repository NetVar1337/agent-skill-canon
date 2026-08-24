> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/readme.md).

# Enma - Overview

Enma is perception's proprietary full-module AOT and JIT-compiled scripting language. This site covers the APIs perception registers on top of Enma. For the language itself see [enma docs](https://enma-1.gitbook.io/enma).

## What's registered

#### Enma Pre-Shipped

* [Core](https://docs.perception.cx/perception/enma-lang/addons/core)
* [STD Library](https://docs.perception.cx/perception/enma-lang/addons/std-library)
* [Math](https://docs.perception.cx/perception/enma-lang/addons/math)
* [Math:3D](https://docs.perception.cx/perception/enma-lang/addons/math-3d)
* [SIMD](https://docs.perception.cx/perception/enma-lang/addons/simd)
* [JSON](https://docs.perception.cx/perception/enma-lang/addons/json)
* [Regex](https://docs.perception.cx/perception/enma-lang/addons/regex)
* [Filesystem](https://docs.perception.cx/perception/enma-lang/addons/file)
* [Thread](https://docs.perception.cx/perception/enma-lang/addons/thread)
* [Atomic](https://docs.perception.cx/perception/enma-lang/addons/atomic)
* [Bits](https://docs.perception.cx/perception/enma-lang/addons/bits)
* [Time](https://docs.perception.cx/perception/enma-lang/addons/time)

#### **Perception API**

* [Lifecycle and Routines](/perception/lifecycle-and-routines.md)
* [Render](/perception/render-api.md)
* [Proc](/perception/proc-api.md)
* [CPU](/perception/cpu-api.md)
* [Sound](/perception/sound-api.md)
* [Zydis](/perception/zydis-api.md)
* [Win](/perception/win-api.md)
* [Input](/perception/input-api.md)
* [Unicorn](/perception/unicorn-api.md)
* [Net](/perception/net-api.md)
* [GUI](/perception/gui-api.md)

#### AI agent surface

* [MCP](/perception/mcp-api.md) — JSON-RPC over local TCP / HTTP for Claude Code, Cline, etc.

## Minimal example

```cpp
int64 g_tick;

void my_draw(int64 data) {
    g_tick = g_tick + 1;
    color white = color(255, 255, 255, 255);
    color noeffect = color(0, 0, 0, 0);

    std::string text = "tick=" + to_string(g_tick);
    draw_text(text, vec2(40.0, 40.0), white, get_font20(), 0, noeffect, 0.0);
}

int64 main() {
    g_tick = 0;
    register_routine(reinterpret_cast<int64>(my_draw), 0);
    return 1;
}
```

See [Lifecycle and Routines](/perception/lifecycle-and-routines.md) for the entry point, return-value semantics, and how routines tick.

## Conventions

* **Colors and positions**: always wrap. `color(255, 255, 255, 255)`, `vec2(10.0, 20.0)`. Freshly constructed each frame is fine; Enma drops the temporaries at scope exit.
* **Float32 literals**: `0.2f`, not `static_cast<float32>(0.2)`. Required for vertex buffers.
* **Handles**: all `create_*` / `load_*` natives return an encrypted `int64`. Pass it back into draw / bind / destroy. Don't inspect.
* **Casts**: use real C++ variants — `static_cast<T>(x)` for numeric conversions, `reinterpret_cast<int64>(fn)` for function pointers (`register_routine`, hooks), `const_cast<T>` for stripping const. The bare `cast<T>()` form is retired.
* **Strings**: `std::string` for values and returns, `const char*` for read-only params. `to_string(n)` (unqualified — from the shipped stdlib) turns numerics into `std::string`. Concatenation via `+` requires at least one `std::string` operand — `"a" + "b"` is illegal pointer arithmetic, but `std::string("a") + "b"` and `to_string(n) + " suffix"` work.
* **Arrays**: `std::vector<T>` for dynamic (`.push_back()`, `[i]`, `.size()`), `std::array<T, N>` for fixed-size.

## SDK

Perception's Enma SDK is not public yet.
