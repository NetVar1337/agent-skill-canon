> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/proc-api.md).

# Proc API

All proc natives are auto-registered into every loaded script.

`proc_t` is a value-type handle. Construct it via `ref_process(...)`; the host ref is released automatically when the variable goes out of scope.

Some natives are gated by permission flags toggled host-side. Gated calls log and return 0 / false when blocked. See [Permissions](#permissions).

**Address type:** all addresses are `uint64`. Pick `uint64` for any locals that hold an address — `uint64 base = p.base_address();` — and the rest of the chain stays cast-free. Mixing `int64` addresses requires `static_cast<uint64>(...)`.

## `proc_t`

```cpp
proc_t ref_process(uint32 pid);
proc_t ref_process(const char* name);
proc_t ref_process(const std::string& name);
```

Returns an alive handle on success, a null one on failure. Verify with `.alive()`.

### How the wrapper works

`proc_t`, `module_info_t`, and `vad_region_t` are pure `.em` classes defined in perception's proc addon prelude. Each one holds a raw `int64 _h` field (the encrypted native handle) and forwards every method through it to a plain native — same shape as shipped's `file_t` / `regex`. This avoids the copy-on-assignment double-release trap that hits when a type-builder handle is used as a field of an `.em` wrapper.

* **`proc_t`** owns a ref to the host process. Move-only (has an explicit move ctor + move-assign). `~proc_t()` releases the ref exactly once. Returned by `ref_process(...)`; use it locally, or move-assign it into a global.
* **`module_info_t`** and **`vad_region_t`** are pure value types — the ctor extracts the fields it needs from the underlying native handle and releases it immediately. Freely copyable, no lifetime tracking, no dangling references.

Every user-facing operation is a method — `p.rvm<Point3>(addr)`, `mod.name()`, `region.protection()`, including the ones returning `std::vector<T>` / `std::string` / `std::array<T, N>`. No free-function-with-handle pattern anywhere in this API.

## Identity

```cpp
uint64 proc.base_address();
uint64 proc.peb();
uint32 proc.pid();
bool   proc.alive();
bool   proc.is_valid_address(uint64 addr);
uint64 proc.get_eprocess();   // gated: kernel_rw_access — see below
```

`get_eprocess` returns the target's EPROCESS kernel address. Gated behind the `kernel_rw_access` permission — returns `0` and logs when the script doesn't hold it. Use cases: passing the EPROCESS to a custom kernel routine, walking kernel structures the proc API doesn't already expose, etc.

## Read primitives

```cpp
uint8/16/32/64 proc.ru8/ru16/ru32/ru64(uint64 addr);
int8/16/32/64  proc.r8/r16/r32/r64  (uint64 addr);
float32 proc.rf32(uint64 addr);
float64 proc.rf64(uint64 addr);

std::string  proc.rs (uint64 addr, int32 max_chars);   // null-terminated UTF-8, cap 8192
std::wstring proc.rws(uint64 addr, int32 max_chars);   // UTF-16, cap 8192 code units
```

Numeric reads return 0 on failure or out-of-range address; string reads return an empty result. By default, addresses must be usermode. When the script holds `kernel_rw_access`, *safe* kernel addresses are also accepted — see [Permissions](#permissions).

## Write primitives (gated: `write_memory`)

```cpp
bool proc.wu8/wu16/wu32/wu64(uint64 addr, uintN v);
bool proc.w8/w16/w32/w64    (uint64 addr, intN  v);
bool proc.wf32(uint64 addr, float32 v);
bool proc.wf64(uint64 addr, float64 v);

bool proc.ws (uint64 addr, const char* text);
bool proc.ws (uint64 addr, const std::string& text);
bool proc.wws(uint64 addr, const std::wstring& text);   // UTF-16
```

## Bulk read/write

Two families: **raw-pointer** `rvm` / `wvm` (byte-blob buffer + length, the underlying native every other form builds on), and **templated typed** `__rvm<T>` / `__wvm<T>` (the `__` prefix keeps the two visually distinct at the call site).

```cpp
// (1) raw C-style — caller owns the buffer, zero allocation, returns bool
bool proc.rvm(uint64 addr, pointer out, int64 size);
bool proc.wvm(uint64 addr, pointer buf, int64 size);                     // gated: write_memory

// (2) typed by-value — templated, returns T
template<typename T> T    proc.__rvm(uint64 addr);
template<typename T> bool proc.__wvm(uint64 addr, const T& value);       // gated: write_memory

// wvm also has a byte-vector overload for the common "here's a chunk of bytes" case
bool proc.wvm(uint64 addr, const std::vector<uint8>& bytes);             // gated: write_memory
```

Shape (1) is the underlying native everything else builds on — direct process-memory copy into (or out of) whatever buffer address you hand over. Use it when you already have a raw pointer to fill, or when you want the bool success return on a typed read (pass `&local` + `sizeof(local)`):

```cpp
Point3 pos;
if (p.rvm(addr, &pos, sizeof(pos))) {
    println("read ok");
}

// Dynamic-size — hand it a std::vector<uint8> you sized yourself
std::vector<uint8> buf;
buf.resize(size);
p.rvm(addr, buf.data(), size);
```

Shape (2) is the ergonomic default — write once at the call site, no size arithmetic. On failure `__rvm<T>` returns a zero-initialized `T` (chained `.x` / `.m[i]` access stays safe); `__wvm` returns `false`:

```cpp
struct Point3 { float64 x; float64 y; float64 z; }
Point3 pos = p.__rvm<Point3>(base + 0x10A4830);

Point3 target;
target.x = 1.0; target.y = 2.0; target.z = 3.0;
p.__wvm(addr, target);
```

## Vec / quat / mat reads / writes

Handled entirely by the templated `__rvm<T>` / `__wvm<T>` overloads. Two shapes depending on how the source encodes floats:

**Float64 source — layout matches the type directly.** The `vec2`/`vec3`/`vec4`/`quat`/`mat4` types are POD arrays of `float64`, so a raw byte-copy is exactly right:

```cpp
proc_t p = ref_process("game.exe");
vec3 cam_pos = p.__rvm<vec3>(p.base_address() + 0x10A4830);
println("camera at " + to_string(cam_pos.x) + "," + to_string(cam_pos.y));

// write mirrors
cam_pos.x = 0.0;
p.__wvm(target_addr, cam_pos);
```

**Float32 source — needs promotion.** The source pack is three `float32`s (12 bytes) but `vec3` is three `float64`s (24 bytes), so a raw copy would misalign. Read the raw floats, then construct — the `float32` → `float64` promotion in the ctor happens implicitly:

```cpp
float32 f[3];
p.rvm(base + 0x10A4830, f, 12);
vec3 cam_pos = vec3(f[0], f[1], f[2]);

// write mirror — narrow each component and pack:
float32 out[3];
out[0] = static_cast<float32>(cam_pos.x);
out[1] = static_cast<float32>(cam_pos.y);
out[2] = static_cast<float32>(cam_pos.z);
p.wvm(target_addr, out, 12);
```

Same `quat` / `mat4` pattern. `mat4` is row-major 16 floats; `quat` is `x, y, z, w`.

The bool-returning forms (`rvm`, `wvm`, `__wvm`) return `false` on bad address / dead proc / kernel-RW gate denial. Same kernel-RW gate applies — see [Permissions](#permissions).

## SIMD-width reads/writes

```cpp
std::array<uint8, 16> proc.r128(uint64 addr);   // 16 bytes
std::array<uint8, 32> proc.r256(uint64 addr);   // 32 bytes
std::array<uint8, 64> proc.r512(uint64 addr);   // 64 bytes

bool proc.w128(uint64 addr, const std::vector<uint8>& bytes);   // gated: write_memory
bool proc.w256(uint64 addr, const std::vector<uint8>& bytes);   // gated: write_memory
bool proc.w512(uint64 addr, const std::vector<uint8>& bytes);   // gated: write_memory
```

The fixed-N read returns a caller-owned `std::array` — no heap traffic on the hot path.

## Modules and exports

```cpp
uint64 proc.get_module_base(const char* name);              // 0 if missing
uint64 proc.get_module_base(const std::string& name);
uint64 proc.get_module_size(const char* name);              // 0 if missing
uint64 proc.get_module_size(const std::string& name);
std::vector<module_info_t> proc.get_module_list();          // every loaded module
uint64 proc.get_proc_address(uint64 module_base, const char* export_name);
uint64 proc.get_proc_address(uint64 module_base, const std::string& export_name);
uint64 proc.get_import_rdata_address(uint64 module_base, const char* import_name);
uint64 proc.get_import_rdata_address(uint64 module_base, const std::string& import_name);
```

`module_info_t`:

```cpp
std::string m.name();   // base DLL filename, e.g. "kernel32.dll"
uint64      m.base();   // DllBase
uint64      m.size();   // SizeOfImage
```

Example — list every module loaded in the target:

```cpp
std::vector<module_info_t> mods = p.get_module_list();
for (module_info_t m : mods) {
    println(format("{s}  base=0x{x}  size=0x{x}", m.name(), m.base(), m.size()));
}
```

## Pattern scanning

```cpp
uint64 proc.find_code_pattern(uint64 search_start, uint64 search_size, const char* sig);
uint64 proc.find_code_pattern(uint64 search_start, uint64 search_size, const std::string& sig);

std::vector<uint64> proc.find_all_code_patterns(uint64 search_start, uint64 search_size, const char* sig);
std::vector<uint64> proc.find_all_code_patterns(uint64 search_start, uint64 search_size, const std::string& sig);
```

Sig syntax: hex bytes separated by spaces, `??` is a wildcard. Example: `"48 8B ?? ?? 48 89"`.

## Threads

```cpp
std::vector<uint64> proc.get_all_tebs();
```

## Pointer arrays

```cpp
std::vector<uint64> proc.read_pointer_array(uint64 base, int64 count, int64 offset_delta);
```

Reads `count` consecutive `uint64`s starting at `base`. `offset_delta` is added to each value before storing (useful when the target stores relative offsets).

## VAD / virtual\_query

Both calls **exclude PE-image regions** (modules, exes). Use `get_module_base` / `get_module_size` for those.

```cpp
vad_region_t              proc.virtual_query(uint64 address);
std::vector<vad_region_t> proc.get_vad_snapshot(bool heap_likely_only);
```

`virtual_query` returns a `vad_region_t` wrapper (its internal `_h` is null on a miss).

### `vad_region_t`

```cpp
uint64 region.start();
uint64 region.size();
uint64 region.protection();   // host page-protection bits (PAGE_READWRITE, PAGE_EXECUTE, etc.)
bool   region.heap_likely();  // host's heuristic for heap allocations
```

```cpp
std::vector<vad_region_t> snap = p.get_vad_snapshot(false);
for (int64 i = 0; i < snap.size(); i = i + 1) {
    vad_region_t r = snap[i];
    uint64 start = r.start();
    uint64 size  = r.size();
    uint64 prot  = r.protection();
    bool   heap  = r.heap_likely();
}
```

## Memory scans

All scans walk the VAD snapshot (so module memory is excluded — same caveat as above). `heap_only=true` restricts to heap-likely regions.

```cpp
std::vector<uint64> proc.scan_string (const char* text,        bool heap_only);
std::vector<uint64> proc.scan_string (const std::string& text, bool heap_only);
std::vector<uint64> proc.scan_wstring(const char* text,        bool heap_only);   // text is UTF-8, converted to UTF-16
std::vector<uint64> proc.scan_wstring(const std::string& text, bool heap_only);
std::vector<uint64> proc.scan_pointer(uint64 target,  bool heap_only);
std::vector<uint64> proc.scan_u64    (uint64 value,   bool heap_only);
std::vector<uint64> proc.scan_u32    (uint32 value,   bool heap_only);
std::vector<uint64> proc.scan_float  (float32 value,  bool heap_only);
std::vector<uint64> proc.scan_double (float64 value,  bool heap_only);
```

## VM alloc / free (gated: `virtual_memory_operations`)

```cpp
uint64 proc.alloc_vm(uint64 size);   // 0 on failure
bool   proc.free_vm (uint64 address);
```

Allocation itself is safe. To execute code from the returned page, the target must have Control Flow Guard (CFG) disabled — CFG kills the process on indirect calls/jumps to non-bitmap addresses. Reads + writes are unaffected.

## Permissions

Three flags gate sensitive operations. All default to off; the user grants them per script via the host UI.

| Flag                        | Gates                                                                                 |
| --------------------------- | ------------------------------------------------------------------------------------- |
| `write_memory`              | `wu*`, `w*`, `wf*`, `ws`, `wws`, `wvm` (all overloads incl. `wvm<T>`), `w128/256/512` |
| `virtual_memory_operations` | `alloc_vm`, `free_vm`                                                                 |
| `kernel_rw_access`          | `get_eprocess`; expands every other read/write to also accept *safe* kernel addresses |

When a gated call runs without permission it logs `[ENMA] ... blocked: '<flag>' permission not granted` and returns 0 / false.

### `kernel_rw_access` semantics

Without it, every read/write address must pass `is_usermode_address` — i.e. canonical user-range, non-null, non-tiny. This is the default.

With it, addresses are accepted when **either**:

* The address is a valid usermode address (same check as before), **or**
* The address is a *safe kernel address* — canonical kernel range AND not in any host-protected critical region (the host's own EPROCESS / ETHREAD / kernel state used for privilege escalation).

The "safe kernel" denylist is enforced by `is_safe_kernel_address` in the host. Scripts can't bypass it: a kernel write to a denied address returns `false` and logs, just like any other refused op.

Use this flag when a script genuinely needs to inspect or modify kernel structures of the target process (Win32 thread state, KPCR fields, driver-side game state, etc.). Don't grant it casually — kernel writes to the wrong address bugcheck the box.

## Lifetime and cleanup

`proc_t` releases its host ref via the destructor when the variable goes out of scope. If a script forgets (e.g. leaks a `proc_t*` heap-allocation), the host sweeps remaining refs at script unload — no permanent leak.

```cpp
int64 main() {
    proc_t p = ref_process("notepad.exe");
    if (!p.alive()) return 0;

    uint64 base = p.base_address();
    println(to_string(p.r32(base + 0x3C)));    // e_lfanew

    return 0;
    // p drops here; host ref released
}
```

## Conventions

* **Addresses are `uint64`.** Use `uint64` for any local that holds an address — hex literals like `0x7FF000000000` work directly. Mixing in an `int64` requires `static_cast<uint64>(...)`.
* **Failed reads return 0**, not an exception. Check `is_valid_address` first if you need certainty.
* **`std::string` / `std::wstring` returned by `rs` / `rws`** own their storage — drop normally at scope exit.
* **`std::vector<T>` returns are length-correct.** `v.size()` is the actual element count, not a max; index with `v[i]` or iterate with a range-for.
* **All ops are methods.** `proc_t`, `module_info_t`, and `vad_region_t` are `.em` wrapper classes over an internal `__native_*` handle stored in `_h`. Call everything with dot syntax: `p.__rvm<Point3>(addr)`, `p.get_module_list()`, `mod.name()`, `region.protection()`. Templated bulk r/w uses the `__rvm` / `__wvm` names to stay distinct from the raw-pointer `rvm` / `wvm` overloads. The `_h` field is public but `__`-marked — never touch it unless you're forwarding into a native (`cpu_create_process` etc. do this internally).
