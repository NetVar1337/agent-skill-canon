> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/modules.md).

# Modules & Namespaces

## Namespaces

```c
namespace math {
    int32 square(int32 x) { return x * x; }
    int32 cube(int32 x) { return x * x * x; }
}

int32 r = math::square(5);  // 25
```

Pull names in with `using namespace`:

```c
using namespace math;
int32 r = square(5);
```

Namespaces nest, can be reopened, and hold everything the language has — free functions, structs and classes (including inheritance and ctor init lists), enums, globals, and methods.

```c
namespace cfg { int64 max_iter = 1000; }
namespace col { enum class Color { Red = 1, Green = 2, Blue = 4 } }
namespace shapes {
    class Shape { int64 size; Shape(int64 s){ size = s; } virtual int64 area(){ return 0; } }
    class Square : Shape {                                     // bare base in same ns
        Square(int64 s) : Shape(s) {}
        virtual int64 area() override { return size * size; }
    }
}
namespace base    { class A { int64 v; A(int64 x){ v = x; } } }
namespace derived { class B : base::A { B(int64 x) : base::A(x + 50) {} } }   // cross-ns
```

Inside a namespaced class you may refer to other types in the same namespace by bare name — the compiler walks the enclosing chain to qualify return types, parameter types, base names, and ctor-init-list base names. So `Wrap make()` inside `namespace n { struct Maker { … } }` finds `n::Wrap`.

Arrays of namespaced structs, constructor calls in expression context, and operator overloads on namespaced types all work as expected:

```c
namespace n {
    struct V {
        int64 v;
        V(int64 x){ v = x; }
        V operator+(V o){ V r = V(0); r.v = v + o.v; return r; }
    }
}

int64 main() {
    std::vector<n::V> arr;
    arr.push_back(n::V(1));
    n::V c = n::V(10) + n::V(32);
    return arr[0].v + c.v;   // 1 + 42 = 43
}
```

### Namespace aliases

`namespace short = a::b::c;` binds a second name to an existing namespace, so a deep path gets a usable handle. It names the same namespace rather than declaring a new one — the alias and its target are interchangeable, and aliasing the same target twice is not a redefinition.

```c
namespace engine { namespace render { namespace detail { const int64 kFlags = 3; } } }

namespace rd = engine::render::detail;

int64 main() {
    return rd::kFlags;      // same entity as engine::render::detail::kFlags
}
```

Legal at namespace scope and inside a block:

```c
int64 main() {
    namespace d = engine::render::detail;   // block scope
    return d::kFlags;
}
```

The target has to be a declared namespace. An alias of a name nothing declares is an error at the alias, and it names the alias: *"`al` cannot alias `nosuchnamespace`: no namespace of that name is declared"*.

This is separate from an aliased `import` (below), which names a module rather than a namespace. A module is reached as `lib::name` and cannot be aliased — a module and a namespace are different kinds of entity:

```c
import "libs/math_utils.em" as lib

namespace al = lib;   // error: `lib` is an imported module, not a namespace;
                      // `al` cannot alias it. Use `lib::` to reach its members,
                      // or import it under the name you want.
```

## Importing Modules

The `import` statement loads another Enma module. Imported symbols are namespaced.

```cpp
import scanner

int32 main() {
    scanner::init();
    scanner::run();
    return 0;
}
```

### Aliased Imports

```cpp
import scanner as sc

sc::init();
```

### Path Imports

Import from an explicit file path:

```cpp
import "libs/math_utils.em"
```

## Module Resolution

When you write `import scanner`, the compiler searches for:

1. `scanner.emb` (precompiled binary) in module paths
2. `scanner.em` (source file) in module paths and include paths

`.emb` files are tried first, then `.em` source as fallback.

Configure search paths from the SDK:

```cpp
add_module_path(engine, "modules/");
add_include_path(engine, "includes/");
```

## Precompiled Modules (.emb)

Enma modules can be compiled to a binary format (`.emb`) for distribution without source code. The SDK provides `serialize()` and `deserialize()` for this.

Precompiled modules expose their functions as `extern` declarations in the importer's namespace.

## Linking Multiple Modules

The SDK's `link()` function combines multiple compiled modules into a single unit, resolving cross-module references:

```cpp
import math_lib
import string_lib

int32 main() {
    float64 r = math_lib::sqrt(2.0);
    std::string s = string_lib::format(r);
    return 0;
}
```
