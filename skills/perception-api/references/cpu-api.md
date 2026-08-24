> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/cpu-api.md).

# CPU API

All CPU natives are auto-registered into every loaded script.

Stuff that doesn't fit cleanly into other host APIs and isn't already in Enma's preshipped addons. For wall-clock time, ISO formatting, and `unix_seconds()` see the preshipped [Time](https://enma-1.gitbook.io/enma/addons/time) addon. For `popcount`/`clz`/`bswap` etc., the preshipped [Bits](https://enma-1.gitbook.io/enma/addons/bits) addon.

## CPU identification

```cpp
std::string cpu_vendor();   // CPUID leaf 0, e.g. "GenuineIntel"
std::string cpu_brand();    // CPUID leaves 0x80000002..4, e.g. "Intel(R) Core(TM) i9-..."
```

## Timing

```cpp
int64 rdtsc();              // raw cycle counter; not stable across cores or sleep
int64 perf_time();          // QueryPerformanceCounter
int64 perf_frequency();     // counter ticks per second
int64 get_tickcount64();    // ms since system boot (monotonic, 64-bit safe)
```

`perf_time / perf_frequency` together give sub-microsecond timestamps:

```cpp
int64 t0 = perf_time();
do_work();
float64 secs = static_cast<float64>(perf_time() - t0) / static_cast<float64>(perf_frequency());
```

## Datetime helpers

Companions to the preshipped `time` addon's `year`/`month`/`day`/`hour`/`day_of_week`/etc. decoders. The `time` addon takes a unix timestamp; these convert intermediate fields:

```cpp
int64       now_millisecond();          // 0..999, current local time
std::string day_name(int64 dow);        // 0..6 -> "Sunday".."Saturday"; "Unknown" out of range
std::string month_name(int64 month);    // 1..12 -> "January".."December"; "Unknown" out of range
int64       hour12(int64 hour24);       // 0..23 -> 1..12 (12-hour wall format)
std::string ampm(int64 hour24);         // 0..23 -> "AM" / "PM"
```

## Bitcasts (float ↔ int)

Use the language built-in `reinterpret_cast<T>(val)`. Reinterprets the bit pattern; not a value conversion. Source and target must be the same byte size; emits a compile error otherwise.

```cpp
uint32  u    = reinterpret_cast<uint32>(1.5f);            // 0x3FC00000
float32 f    = reinterpret_cast<float32>(0x3FC00000u);    // 1.5
uint64  bits = reinterpret_cast<uint64>(3.14);            // IEEE 64-bit pattern
uint32  sign = reinterpret_cast<uint32>(-3.14f) >> 31;    // 1
```

Compiles to at most 2 mov instructions (`narrow_f32` + `mov` at the f32 boundary, plain `mov` elsewhere) — zero call overhead. Generalizes to any same-size pair: pointers ↔ `int64`, mixed signed/unsigned narrow ints, etc.

For narrow-int → wider-int (no float involved), `static_cast<uint32>(some_int8)` etc. works directly — Enma keeps narrow ints zero/sign-extended in 64-bit slots, so the cast is a free rename.

## Thread priority

Affects whatever thread invokes the call. Routine callbacks run each tick on their own ticker thread, so calling from a routine adjusts that ticker thread (NOT the script's main thread).

```cpp
bool set_thread_priority(thread_priority p);
```

`thread_priority` enum values: `lowest`, `below_normal`, `normal`, `above_normal`, `highest`.

```cpp
set_thread_priority(thread_priority::highest);
```
