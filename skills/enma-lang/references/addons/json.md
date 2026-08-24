> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/json.md).

# JSON

Registered with `register_addon_json(engine)`.

A DOM with value semantics over the RFC 7159 core — null, bool, number, string, array, object — with the `\n \t \r \" \\ \/` escapes. The whole document is parsed up front; there is no streaming API.

## Parsing and building

```c
json_value json_parse(const std::string& text)
json_value json_object()      // empty object
json_value json_array()       // empty array
```

Malformed input yields a value whose `is_valid()` is false rather than throwing. Every method is safe on one: they return `0`, `""`, `false` or an empty container.

```c
json_value j = json_parse("{\"name\":\"Alice\",\"age\":30,\"tags\":[1,2,3]}");
j.is_valid();      // true

json_value bad = json_parse("{oops");
bad.is_valid();    // false
bad.size();        // 0
bad.as_str();      // ""
```

## Queries

```c
bool  v.is_valid()
bool  v.is_null()
bool  v.is_bool()
bool  v.is_num()
bool  v.is_str()
bool  v.is_array()
bool  v.is_obj()
int64 v.kind_id()     // 0 null · 1 bool · 2 number · 3 string · 4 array · 5 object
```

```c
bool    v.as_bool()   // false unless it is a bool
float64 v.as_num()    // 0.0 unless it is a number
int64   v.as_int()    // the number truncated toward zero
std::string v.as_str()  // "" unless it is a string
```

```c
int64 v.size()                        // array or object length; 0 for a primitive
bool  v.has_key(const char* key)
std::vector<std::string> v.keys()     // object keys, in insertion order
```

```c
j.kind_id();          // 5 — an object
j["age"].as_int();    // 30
j["tags"].size();     // 3
j.has_key("nope");    // false
j.keys();             // "name", "age", "tags"
```

## Navigation

```c
json_value& v.operator[](const char* key)   // on an object; creates a null slot if absent
json_value  v.operator[](int64 index)       // on an array; invalid if out of range
json_value  v.get_key(const char* key)      // deep copy of the subtree
json_value  v.get_at(int64 index)           // deep copy of the element
```

Subscript is the everyday spelling and chains, so a path reads as one expression.

```c
json_value j = json_parse("{\"users\":[{\"id\":1},{\"id\":2}]}");
j["users"][0]["id"].as_int();      // 1

j["tags"][9].is_valid();           // false — out of range
```

The string-key subscript hands back a reference, so assigning through it writes into the document.

```c
j["port"] = 8080;
j["name"] = "svc";       // json_value / int32 / int64 / float64 / bool / string / char*
```

`get_key` and `get_at` copy the subtree out instead, so the result outlives its parent and edits to it do not touch the original.

```c
json_value tags = j.get_key("tags");
tags.get_at(2).as_int();     // 3
```

## Mutation

```c
void v.set_key(const char* key, json_value value)
void v.set_key(const char* key, int32 value)
void v.set_key(const char* key, int64 value)
void v.set_key(const char* key, float64 value)
void v.set_key(const char* key, bool value)
void v.set_key(const char* key, const std::string& value)
void v.set_key(const char* key, const char* value)

void v.remove_key(const char* key)
void v.push_value(...)      // the same overload family, appending to an array
```

Each takes the value by copy, so the source can be dropped afterwards.

```c
json_value o = json_object();
o.set_key("a", 1);
o.set_key("b", "two");
o.set_key("c", true);
o.stringify();                  // {"a":1,"b":"two","c":true}

o.remove_key("b");
o.stringify();                  // {"a":1,"c":true}

json_value arr = json_array();
arr.push_value(1);
arr.push_value("x");
arr.stringify();                // [1,"x"]
```

## Output

```c
std::string v.stringify()     // compact
std::string v.pretty()        // indented, one level per nesting depth
```

Numbers round-trip as integers when they fit exactly in `[-1e15, 1e15]`, otherwise as `%.17g` floats. Strings re-escape quotes, backslashes and control characters below `0x20`. Object keys emit in insertion order, and parsing preserves that order.

## Limits

* `\uXXXX` escapes are not decoded, so surrogate pairs are left as written.
* No schema validation.
