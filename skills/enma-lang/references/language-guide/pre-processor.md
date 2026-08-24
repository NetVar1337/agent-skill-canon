> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/pre-processor.md).

# Pre Processor

A C preprocessor running before parsing. Malformed directives are diagnosed, not skipped.

## Defines

```c
#define MAX_HP 100
#define SQUARE(x) ((x) * (x))

int32 hp = MAX_HP;
int32 area = SQUARE(5);  // 25
```

`#undef` removes a macro, function-like ones included.

Substitution replaces parameter **tokens**, so a parameter's spelling inside a string or character literal in the body is left alone. A macro preserves its argument tokens as spelled, and a macro already being expanded is not re-expanded inside itself.

### Stringize and paste

`#x` turns an argument into a string literal; `a ## b` pastes two tokens into one.

```c
#define NAME(x)    #x               // NAME(hp)      -> "hp"
#define JOIN(a, b) a ## b           // JOIN(pl, ayer) -> player
```

## Conditional compilation

`#if`, `#ifdef`, `#ifndef`, `#elif`, `#else`, `#endif`.

```c
#ifdef DEBUG
    println("debug mode");
#endif

#if MAX_HP > 50 && defined(RANKED)
    // ...
#elif defined(CASUAL)
    // ...
#else
    // ...
#endif
```

`#if` takes the full constant-expression grammar: `defined(X)` / `defined X`, the conditional `?:`, logical `||` `&&`, bitwise `|` `^` `&` `~`, equality, relational, shifts, additive, multiplicative and unary operators — over decimal, octal, hex and binary integers and character constants.

Three rules matter:

* An identifier that survives expansion evaluates to **0**.
* Division or modulo by zero is **diagnosed**.
* A token the grammar cannot consume is an **error**, not a silently truncated expression that quietly picks the wrong branch.

Define symbols from the host:

```cpp
define(engine, "DEBUG", "1");
```

## Include

`#include` inserts a file's contents at the include point. It is textual and repeatable, exactly like C — `#pragma once` in the included file suppresses the repeat. (`import` is deduplicated instead; see [Modules & Namespaces](/perception/enma-lang/language-guide/modules.md).)

```c
#include "common.em"
```

Include paths are configured host-side with `add_include_path`.

## Other directives

| Directive          | Effect                                                   |
| ------------------ | -------------------------------------------------------- |
| `#error "text"`    | Fails compilation with the message                       |
| `#warning "text"`  | Emits a diagnostic and continues                         |
| `#line N ["file"]` | Overrides the reported line, and optionally the filename |
| `#pragma once`     | Suppresses repeat inclusion of this file                 |
| `#`                | The null directive — does nothing                        |

## Lexical rules

These run before anything else sees the source:

* A backslash immediately before a newline **splices** the two lines into one, so a multi-line `#define` body is a single directive.
* Block comments `/* … */` do **not** nest — the first `*/` closes the comment.
* An ordinary string literal may not span source lines; use `\n` or a raw literal `R"(…)"`.
* Source is UTF-8, and a leading byte-order mark is ignored.
