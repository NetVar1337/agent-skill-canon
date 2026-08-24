> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/file.md).

# Filesystem

Registered with `register_addon_file(engine)`.

Everything lives in `namespace std::filesystem`. Open it once with a using-directive or qualify each call; the examples below assume the directive.

```c
using namespace std::filesystem;
```

Every path argument accepts `const char*` or `const std::string&`.

## Permissions

Without `PERM_FILE` granted, any call that touches the filesystem fails the module compile, and a precompiled module whose calls need it fails to load. That makes sandboxing the default rather than something a host has to remember.

```cpp
// Host code
auto* e = enma::create();
enma::register_all_addons(e);
enma::set_permissions(e, enma::PERM_FILE);   // omit to deny all file access
```

A permitted engine can be confined further to one directory tree:

```cpp
enma::set_directory_isolate(e, "C:/app/data");   // nullptr or "" clears it
```

With an isolate set, script paths resolve relative to that root, and a path that would resolve outside it is rejected at the native boundary. The call then reports ordinary failure (`false`, `-1`, or an empty result) rather than throwing.

## Streams

`file_t` wraps an OS handle and closes it in its destructor at scope exit.

```c
file_t file_open(const std::string& path, const std::string& mode)

bool        f.is_open()      // false if the open failed
bool        f.is_eof()
std::string f.read_all()     // from the current position to the end
std::string f.read_line()    // without the trailing newline
int64       f.write(const std::string& text)   // bytes written
int64       f.size()
int64       f.tell()
void        f.seek(int64 position)
void        f.flush()
void        f.close()
```

Modes are the standard C ones: `"r"`, `"w"`, `"a"`, `"rb"`, `"wb"`, `"ab"`, `"r+"`, `"w+"`.

```c
file_t f = file_open("data.txt", "r");
if (f.is_open()) {
    while (!f.is_eof()) {
        std::string line = f.read_line();
        println(line);
    }
}
f.close();
```

`is_eof` turns true only after a read has run past the end, so the loop above sees one empty line at the bottom. Check the line rather than the flag if that matters.

```c
file_t w = file_open("out.txt", "w");
w.write("hello");        // 5
w.flush();
w.close();

file_t bin = file_open("img.dat", "rb");
int64 total = bin.size();
bin.seek(1024);
int64 pos = bin.tell();   // 1024
```

## Whole files

```c
std::string file_read(const std::string& path)
bool        file_write(const std::string& path, const std::string& text)

std::vector<uint8> file_read_bytes(const std::string& path)
bool               file_write_bytes(const std::string& path, const std::vector<uint8>& data)
```

```c
file_write("data.txt", "line1\nline2\n");     // true

std::string body = file_read("data.txt");     // "line1\nline2\n"

std::vector<uint8> bytes = file_read_bytes("data.txt");
bytes.size();                                  // 12
file_write_bytes("copy.bin", bytes);           // true
```

## Files and paths

```c
bool  exists(const std::string& path)
bool  remove(const std::string& path)          // one file, or one empty directory
int64 remove_all(const std::string& path)      // recursive; entries removed, -1 on error
bool  rename(const std::string& from, const std::string& to)
bool  copy_file(const std::string& src, const std::string& dst)   // false if dst exists
int64 file_size(const std::string& path)       // -1 on error
int64 last_write_time(const std::string& path) // Unix seconds, -1 on error
```

```c
copy_file("data.txt", "copy.txt");     // true
copy_file("data.txt", "copy.txt");     // false — will not overwrite
rename("copy.txt", "moved.txt");       // true
remove("moved.txt");                   // true
file_size("nope.txt");                 // -1
```

## Directories

```c
bool  is_directory(const std::string& path)
bool  create_directory(const std::string& path)      // one level
bool  create_directories(const std::string& path)    // the whole missing chain
std::vector<std::string> dir_list(const std::string& path)   // immediate children, names only
std::vector<std::string> dir_walk(const std::string& path)   // recursive, full paths
```

```c
create_directories("out/logs/today");
is_directory("out/logs");                  // true

std::vector<std::string> names = dir_list("out/logs/today");   // "a.txt"
std::vector<std::string> all   = dir_walk("out");              // "out/logs/today/a.txt"

remove_all("out");                          // number of entries removed
exists("out");                              // false
```
