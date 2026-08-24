> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/regex.md).

# Regex

Registered with `register_addon_regex(engine)`.

ECMAScript syntax. `regex` compiles the pattern once at construction and frees it in its destructor at scope exit, so build it outside the loop that uses it.

## Construction

```c
regex(const std::string& pattern)
```

```c
regex re = regex("[0-9]+");
regex word("\\w+");             // constructor-form declaration
```

A pattern that fails to compile yields a null handle rather than throwing. Every method is safe on one — they return `false`, `""` or an empty vector.

```c
regex bad = regex("[");
bad.matches("x");          // false
bad.first("x");            // ""
bad.find_all("x").size();  // 0
```

## Methods

```c
bool                     re.matches(const std::string& text)     // the WHOLE string matches
bool                     re.has_match(const std::string& text)   // any substring matches
std::string              re.first(const std::string& text)       // first match, or ""
std::vector<std::string> re.find_all(const std::string& text)    // every match
std::string              re.replace(const std::string& text, const std::string& replacement)
std::vector<std::string> re.split(const std::string& text)       // split on each match
std::vector<std::string> re.groups(const std::string& text)      // [full, group1, group2, …]
```

The single-match accessor is `first` because `match` is a language keyword.

```c
regex re = regex("[0-9]+");

re.matches("12345");            // true
re.matches("abc123");           // false — matches is whole-string
re.has_match("abc 123");        // true
re.first("abc 123 def");        // "123"
re.first("abc");                // ""
re.replace("a12b34", "#");      // "a#b#" — every match is replaced
```

```c
std::vector<std::string> all = re.find_all("a12 b345 c6");
// size 3: "12", "345", "6"

regex comma = regex(",");
std::vector<std::string> parts = comma.split("a,b,c,d");
// size 4: "a", "b", "c", "d"
```

## Capture groups

`groups` returns the full match at index 0 and each capture in order after it. An empty vector means the pattern did not match.

```c
regex kv = regex("([a-z]+)=([0-9]+)");
std::vector<std::string> g = kv.groups("age=30");
// g[0] == "age=30", g[1] == "age", g[2] == "30"
```
