> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/std-library.md).

# STD Library

The `std` prelude is in every script with no `import` and no host registration. It is not an addon — the host cannot turn it off. With `using namespace std;` the bare spellings (`string`, `vector`, …) work too.

Two conventions hold across the whole prelude:

* **Sizes and indices are signed `int64`**, so `npos` is `-1` and a negative count means "to the end" wherever a length is taken.
* **`operator=` returns `T&`**, exactly C++.

## std::string

`std::string` holds UTF-8 bytes, `std::wstring` holds UTF-16. Both are value structs, and `wstring` mirrors every member with `wchar` / `std::wstring` spellings.

```c
string()
string(const char* text)
string(const std::string& other)
```

```c
int64 s.size()                    int64 s.length()      // both, exactly C++
int64 s.max_size()
bool  s.empty()
char  s.at(int64 index)           // bounds-checked; throws std::out_of_range
char& s.operator[](int64 index)   // unchecked; writes through
const char* s.c_str()
const char* s.data()
```

```c
int64 s.find(const std::string& needle)                     // and const char*, char
int64 s.find(const std::string& needle, int64 start)
int64 s.rfind(const std::string& needle)                    // last occurrence
bool  s.contains(const std::string& needle)
bool  s.starts_with(const std::string& prefix)
bool  s.ends_with(const std::string& suffix)
```

A search that does not match returns `std::string::npos`, which is `-1`.

```c
std::string  s.substr(int64 pos, int64 count)   // count defaults to npos
void         s.append(const std::string& text)  // and const char*
void         s.push_back(char ch)
std::string& s.insert(int64 pos, const std::string& text)
std::string& s.erase(int64 pos, int64 count)
void         s.clear()
```

Operators: `+` `+=` `==` `!=` `<` `>` `<=` `>=`, against a `std::string` or a `const char*` on either side.

```c
std::string s = "hello";
s += " world";

s.size();                    // 11
s.starts_with("hello");      // true
s.find("o");                 // 4
s.rfind("o");                // 7
s.find("zz");                // -1
s.substr(0, 5);              // "hello"
s.substr(6);                 // "world"
s.at(1);                     // 101 — 'e'
s == "hello world";          // true
"lit " + s;                  // const char* on the left works too
```

A string literal is a `const char[N]` decaying to `const char*` — a wide literal `L"..."` to `const wchar*`. Literals bind `const char*` parameters and comparisons directly, so `s == "hi"`, `s + "x"` and `"lit" + s` allocate no temporary. There is no implicit `std::string` → `char*`; call `.c_str()` where a native wants a C string.

Cross-width conversion is explicit, exactly like C++:

```c
std::wstring wstring_from_str(const std::string& s)   // UTF-8 -> UTF-16, surrogate-aware
std::string  wstring_to_str(const std::wstring& w)    // UTF-16 -> UTF-8
```

## std::vector\<T>

Growable, contiguous, value-semantic.

```c
vector()
vector(const vector<T>& other)                 // deep copy
std::vector<T> v = {a, b, c};                  // rides std::initializer_list<T>

int64 v.size()          int64 v.capacity()     int64 v.max_size()
bool  v.empty()
T     v.at(int64 index)                        // throws std::out_of_range
T&    v.operator[](int64 index)                // unchecked; writes through
T&    v.front()         T& v.back()
T*    v.data()
void  v.reserve(int64 n)
void  v.resize(int64 n)                        // grows with value-initialized elements
void  v.resize(int64 n, const T& fill)         // grows with copies of `fill`
void  v.push_back(T value)                     void v.pop_back()
vector_iterator<T> v.insert(vector_iterator<T> pos, T value)
vector_iterator<T> v.erase(vector_iterator<T> pos)
void  v.clear()
vector_iterator<T> v.begin()                   vector_iterator<T> v.end()
```

```c
std::vector<int64> v = {1, 2, 3};
v.push_back(4);
v[0] = 10;

v.at(3);                       // 4
v.front();                     // 10
v.insert(v.begin() + 1, 99);   // {10, 99, 2, 3, 4}
v.erase(v.begin() + 1);        // {10, 2, 3, 4}

for (int64 x : v) { /* 10, 2, 3, 4 */ }
```

Copying a vector deep-copies its elements, and `push_back` copies the argument in — one user copy-constructor fire per push, exactly C++.

Vectors nest to any depth; every level is a real `std::vector`, value-semantic all the way down.

```c
std::vector<std::vector<int64>> grid = {{1, 2}, {3, 4}};
grid.push_back(row);       // push_back COPIES the row in
grid[0][1] = 20;           // subscript chains are T&, so writes go through
```

For a fixed-extent array with no heap, see the builtin `T a[N]` in [Pointers](/perception/enma-lang/language-guide/pointers.md).

## std::array\<T, N>

Fixed-size value struct with C++-exact layout: `sizeof == N * sizeof(T)`, elements at offset 0, usable wherever a flat `T[N]` blob is expected.

```c
int64 a.size()          int64 a.max_size()      bool a.empty()
T     a.at(int64 index)                         // throws
T&    a.operator[](int64 index)                 // unchecked
T&    a.front()         T& a.back()
T*    a.data()
void  a.fill(T value)
void  a.swap(array<T, N>& other)
vector_iterator<T> a.begin()                    vector_iterator<T> a.end()
```

```c
std::array<int64, 4> a;
a.fill(7);
a[1] = 5;
a.back();      // 7
```

## std::list\<T>

Doubly linked — constant-time insert and remove at both ends and at any iterator, no random access.

```c
list()
list(const list<T>& other)                     // deep copy
std::list<T> l = {a, b, c};

int64 l.size()          bool l.empty()
T     l.front()         T l.back()             // value copies
void  l.push_back(T value)                     void l.push_front(T value)
void  l.pop_back()                             void l.pop_front()
list_iterator<T> l.insert(list_iterator<T> pos, T value)
list_iterator<T> l.erase(list_iterator<T> pos)   // iterator past the removed node
void  l.clear()                                void l.reverse()
list_iterator<T> l.begin()                     list_iterator<T> l.end()
```

```c
std::list<int64> l = {1, 2, 3};
l.push_front(0);
l.pop_back();
l.reverse();
l.front();                              // 2

l.insert(std::next(l.begin()), 99);     // insert at index 1
```

Elements are stored by value: inserting copies, and removing a node destroys its element exactly once.

## std::stack\<T> / std::queue\<T>

```c
int64 st.size()      bool st.empty()
void  st.push(T value)
void  st.pop()
T&    st.top()

int64 q.size()       bool q.empty()
void  q.push(T value)
void  q.pop()
T     q.front()      T q.back()
```

```c
std::stack<int64> st;
st.push(1); st.push(2);
st.top();        // 2
st.pop();
st.top();        // 1
```

## std::pair\<K, V>

Two public fields, `first` and `second`. It is the map storage type and works with structured bindings. There is no `std::tuple` — destructure any struct's fields with `auto [a, b, c]` instead.

## std::map / std::unordered\_map

`std::map<K, V>` keeps keys sorted by `operator<`; `std::unordered_map<K, V>` hashes and iterates in unspecified order. The two share a surface.

```c
map()
map(const map<K, V>& other)
std::map<K, V> m = {{k1, v1}, {k2, v2}};       // first key wins on duplicates

int64 m.size()          bool m.empty()
V&    m.operator[](K key)      // inserts a default-constructed V on a miss
V     m.at(K key)              // throws std::out_of_range on a miss
void  m.insert(K key, V value) // first-wins: a no-op if the key exists
bool  m.contains(K key)
map_iterator<K, V> m.find(K key)     // == m.end() on a miss
bool  m.erase(K key)                 // true if something was removed
void  m.clear()
map_iterator<K, V> m.begin()         map_iterator<K, V> m.end()
```

`std::map` only:

```c
map_iterator<K, V> m.lower_bound(K key)   // first key >= key
map_iterator<K, V> m.upper_bound(K key)   // first key >  key
```

`std::unordered_map` only:

```c
void  m.reserve(int64 n)          // presize the bucket array
int64 m.bucket_count()            // buckets currently allocated
```

```c
std::map<int64, int64> m = {{1, 10}, {2, 20}};
m[3] = 30;

m.at(2);            // 20
m.insert(1, 99);
m[1];               // 10 — insert does not overwrite; assign through [] for that
m.contains(3);      // true
m.erase(9);         // false

for (auto [k, v] : m) { /* sorted by key */ }
```

Values are stored by value: inserting copies, `at` returns a copy, and `m[k] = v` assigns through the slot with memberwise `operator=` and no copy constructor, exactly C++. A key type needs only `<` for `std::map`.

## std::set / std::unordered\_set

`std::set<T>` sorts ascending by `operator<`, `std::unordered_set<T>` hashes.

```c
set()
set(const set<T>& other)
std::set<T> s = {a, b, c};             // duplicates collapse

int64 s.size()          bool s.empty()
bool  s.insert(T value)                // false if it was already present
bool  s.contains(T value)
vector_iterator<T> s.find(T value)     // == s.end() on a miss
bool  s.erase(T value)                 // true if something was removed
void  s.clear()
void  s.reserve(int64 n)
vector_iterator<T> s.begin()           vector_iterator<T> s.end()
```

```c
std::set<int64> s = {3, 1, 2, 1};
s.size();          // 3 — the duplicate collapsed
s.insert(2);       // false
s.insert(9);       // true
```

`std::set` orders and looks up through `operator<` alone — equivalence is `!(a<b) && !(b<a)`, exactly the C++ Compare contract.

## Iterators and complexity

Each container's iterator is a type of its own — `vector_iterator<T>` (also `array`, `set` and `unordered_set`), `list_iterator<T>`, `map_iterator<K, V>`, `unordered_map_iterator<K, V>` — which is what `begin()` / `end()` return and what `insert`, `erase` and `find` take and give back. Name it that way or let `auto` deduce it; the containers declare no nested type aliases. Every iterator supports `*`, `++` and `==`; a contiguous container's iterator also supports `+ n`.

```c
std::vector<int64> v;
v.push_back(1); v.push_back(2); v.push_back(3);

vector_iterator<int64> it = v.begin();
v.insert(it + 1, 9);              // 1 9 2 3
v.erase(v.begin());               // 9 2 3

std::map<int64, int64> m;
m[1] = 10;
auto f = m.find(1);               // map_iterator<int64, int64>
if (f != m.end()) { }
```

Dereferencing yields a reference, so `*it = v` writes through, and a map iterator yields `pair<const K, V>&`, which is what makes `for (auto& [k, v] : m)` mutate in place.

Complexity is part of the contract: `vector` and `array` index in constant time and `vector::push_back` is amortized constant; `map` and `set` are logarithmic for lookup, insert and erase; the unordered pair are average constant for the same three, which is what `reserve` presizes for; `list` splices in constant time and searches linearly.

## std::hash\<T>

Supplies key hashing for the hashed containers, specialized for the integer and floating primitives, `bool`, `char` / `wchar`, every pointer type, `std::string` and `std::wstring`.

A user type becomes a hashed-container key by specializing it alongside an `operator==`. Equal keys must hash equal; unequal keys may collide.

```c
namespace std {
    template<> struct hash<MyKey> {
        uint64 operator()(const MyKey& k) const { return static_cast<uint64>(k.id); }
    }
}
```

A qualified declarator declares the same specialization:

```c
template<> struct std::hash<MyKey> {
    uint64 operator()(const MyKey& k) const { return static_cast<uint64>(k.id); }
}
```

The key binds by const reference, which is the standard signature and hashes without copying. Pointer keys are covered already — `hash<T*>` hashes the address. A key type with no specialization is refused at the call, naming the type.

## std::any

Type-erased holder for one value of any type. Identity is `typeid`-based and exact.

```c
any()                                  // empty
any(T value)                           // boxes any T, classes included

bool  a.has_value()
int64 a.type()                         // compare against typeid(T)
void  a.reset()                        // destroy the payload, back to empty
void  a.emplace(T value)               // replace the payload with any type

std::any std::make_any<T>(T value)
T        std::any_cast<T>(std::any& a) // copies out; throws std::bad_any_cast
```

```c
std::any a = 42;
a.has_value();                    // true
a.type() == typeid(int32);        // true — an integer literal is int32
std::any_cast<int32>(a);          // 42
std::any_cast<int64>(a);          // throws — identity is exact
a.reset();
```

Class payloads are managed properly: boxing copies via the copy constructor, destroying the box runs `~T()`, and copying an `any` duplicates the payload.

## std::optional\<T>

A maybe-value. Default-constructs disengaged, engages on assignment, and `std::nullopt` disengages it.

```c
optional()                             // disengaged

bool  o.has_value()
explicit operator bool()               // so `if (o)` works
T&    o.value()                        // throws std::bad_optional_access
T     o.value_or(const T& fallback)    // never throws
T&    o.emplace(const T& value)
void  o.reset()
void  o.swap(optional<T>& other)
```

Assignment takes a `T`, another `optional<T>`, or `std::nullopt`, and `*o` reads the value.

```c
std::optional<int32> n;
n.has_value();          // false

n = 42;
n.value_or(0);          // 42
n.value();              // 42
*n;                     // 42

n = std::nullopt;       // disengaged again
```

The value lives inline beside the engaged flag, so `T` needs a default constructor. Comparisons are the full C++ set — `== != < <= > >=` between two optionals, between an optional and a bare value in either order, and against `std::nullopt`. A disengaged optional orders before every engaged one, and two disengaged compare equal.

## Comparison categories

`std::strong_ordering`, `std::weak_ordering` and `std::partial_ordering` are the result types of `<=>`. Each carries `less`, `greater` and an equality value — `equal` for the strong category, `equivalent` for the weak one — and `partial_ordering` adds `unordered`, which is what a floating-point `<=>` involving NaN yields.

A category compares against the literal `0` and against another value of its own type. The weaker categories convert from the stronger ones, never the reverse.

```c
std::strong_ordering c = a <=> b;
if (c < 0) { /* ... */ }
if (std::is_lt(c)) { /* is_eq is_neq is_lt is_lteq is_gt is_gteq */ }
```

## String algorithms

Free functions in `namespace std`, so `std::string`'s member surface stays exactly C++'s. Each has a wide twin taking `std::wstring`.

```c
std::vector<std::string> std::split(const std::string& s, const std::string& delim)
std::string std::to_upper(const std::string& s)
std::string std::to_lower(const std::string& s)
std::string std::reverse(const std::string& s)
std::string std::trim(const std::string& s)
std::string std::trim_left(const std::string& s)
std::string std::trim_right(const std::string& s)
std::string std::repeat(const std::string& s, int64 times)
std::string std::pad_left(const std::string& s, int64 width, char fill)
std::string std::pad_right(const std::string& s, int64 width, char fill)
bool std::starts_with_i(const std::string& s, const std::string& prefix)   // case-insensitive
bool std::ends_with_i(const std::string& s, const std::string& suffix)
std::vector<int64> std::chars(const std::string& s)    // code units; narrow only
```

## Conversion and formatting

```c
int64       std::stoi(const std::string& s)      // also std::wstring
float64     std::stod(const std::string& s)
std::string std::to_string(T value)              // int32/int64/uint32/uint64,
                                                 // float32/float64, bool, string, char*
std::string std::replace_all(const std::string& s, const std::string& from, const std::string& to)
std::string std::replace_first(const std::string& s, const std::string& from, const std::string& to)
std::string std::join(const std::vector<std::string>& parts, const std::string& sep)
std::string std::format(const char* pattern, ...)   // only {} holes; {{ and }} escape
```

```c
std::stoi("42");                          // 42
std::to_string(1.5);                      // "1.5"
std::replace_all("a-b-c", "-", "+");      // "a+b+c"
std::join(parts, "-");                    // "a-b"
std::format("a={} b={}", 1, 2);           // "a=1 b=2"
```

Overload `to_string` for a user type and `std::format`'s `{}` holes pick it up — the format machinery stringifies each argument through `to_string`.

## Container algorithms

Free function templates in `namespace std`; they instantiate only where called.

```c
void  std::sort(V& v)                          // elements need operator<; O(N log N)
int64 std::sum(const std::vector<int64>& v)
int64 std::min_of(const std::vector<int64>& v)     int64 std::max_of(const std::vector<int64>& v)
int64 std::min_idx(const std::vector<int64>& v)    int64 std::max_idx(const std::vector<int64>& v)
V     std::transform(V& v, fn)                 // fn: (T) => U
V     std::filter(V& v, pred)                  // pred: (T) => bool
bool  std::any_of(V& v, pred)                  bool std::all_of(V& v, pred)
It    std::find_if(V& v, pred)
int64 std::reduce(V& v, fn, int64 init)        // fn: (int64, int64) => int64
It    std::next(It it)                         It std::next(It it, int64 n)
```

```c
std::vector<int64> v = {3, 1, 2};
std::sort(v);                                    // {1, 2, 3}

std::sum(v);                                     // 6
std::max_idx(v);                                 // 2
std::transform(v, (int64 x) => x * 2);           // {2, 4, 6}
std::filter(v, (int64 x) => x > 1);              // {2, 3}
std::reduce(v, (int64 a, int64 b) => a + b, 0);  // 6
*std::next(v.begin(), 2);                        // 3
```

## Global helpers

Enma extensions rather than standard names, so they live at global scope and are called bare.

```c
std::string int_to_str(int64 v)          std::string float_to_str(float64 v)
std::string bool_to_str(bool v)          std::string char_to_str(char c)
std::string chr(int64 code)              int64 ord(char c)

std::string hex_encode(const std::string& s)     std::string hex_decode(const std::string& s)
int64       hex_to_int(const std::string& s)     std::string hex_encode(int64 v)
std::string base64_encode(const std::string& s)  std::string base64_decode(const std::string& s)
std::string url_encode(const std::string& s)     std::string url_decode(const std::string& s)

std::wstring wstring_from_str(const std::string& s)
std::string  wstring_to_str(const std::wstring& w)
std::wstring wstring_from_wchar_ptr(int64 addr)
std::wstring wstring_from_utf8_ptr(int64 addr)
```
