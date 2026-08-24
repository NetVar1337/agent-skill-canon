> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/language-guide/builtin-instructions.md).

# Builtin Instructions

Builtins are recognized by the compiler and lowered straight to machine instructions — no call, no addon, no registration. They are always available.

Every name starts with `__`. That prefix is reserved: it marks something the compiler implements directly rather than a library function you could have written.

## Inline assembly

```c
int64 __asm_rdtsc()     // time-stamp counter
void  __asm_pause()     // spin-wait hint (PAUSE)
void  __asm_mfence()    // full memory fence (MFENCE)
void  __asm_nop()       // NOP
```

```c
int64 spin_until(int64* flag) {
    int64 start = __asm_rdtsc();
    while (*flag == 0) { __asm_pause(); }
    __asm_mfence();
    return __asm_rdtsc() - start;
}
```

## Atomics

`slot` is an `aint8` / `aint16` / `aint32` / `aint64` variable, passed by name. These compile to interlocked instructions.

```c
int64 __atomic_load(aint64 slot)
void  __atomic_store(aint64 slot, int64 value)
int64 __atomic_xchg(aint64 slot, int64 value)               // returns the previous value
int64 __atomic_xadd(aint64 slot, int64 delta)               // returns the previous value
int64 __atomic_cmpxchg(aint64 slot, int64 expect, int64 set) // returns the value seen
```

Ordinary reads and writes of an `aint*` are already atomic, so reach for these only when you need the previous value or an explicit compare-and-swap.

```c
aint64 counter = 0;

__atomic_store(counter, 5);
int64 a = __atomic_load(counter);              // 5
int64 b = __atomic_xchg(counter, 9);           // 5, slot is now 9
int64 c = __atomic_xadd(counter, 1);           // 9, slot is now 10
int64 d = __atomic_cmpxchg(counter, 10, 42);   // 10, slot is now 42
```

## Varargs

Inside a function declared `f(...)`:

```c
int64 __va_count       // number of arguments passed (a value, not a call)
int64 __va_arg(int64 index)
```

```c
int64 sum_all(...) {
    int64 total = 0;
    for (int64 i = 0; i < __va_count; i = i + 1) { total = total + __va_arg(i); }
    return total;
}

sum_all(1, 2, 3, 4);   // 10
sum_all();             // 0
```

## Bit reinterpretation

```c
T __bit_cast<T>(U value)
T std::bit_cast<T>(U value)   // the same builtin under its standard spelling
```

Keeps the bit pattern; `static_cast` converts the value. This is the only float↔int bit reinterpretation.

```c
float64 f = 1.5;
int64   b = __bit_cast<int64>(f);          // 4609434218613702656
float64 g = std::bit_cast<float64>(b);     // 1.5
```

## CPU features

```c
int64 cpu_features()          // raw bitmask, one bit per feature
int64 __simd_has_avx()        // nonzero if AVX
int64 __simd_has_avx2()       // nonzero if AVX2
int64 __simd_has_avx512()     // nonzero if AVX-512F
int64 __simd_has_avx512bw()   // nonzero if AVX-512BW
```

The gates return `int64`, not `bool` — compare against zero.

| Bit | Value | Feature                  |
| --- | ----- | ------------------------ |
| 0   | 1     | SSE2 (always set on x64) |
| 1   | 2     | SSE4.1                   |
| 2   | 4     | SSE4.2                   |
| 3   | 8     | AVX                      |
| 4   | 16    | AVX2                     |
| 5   | 32    | FMA                      |
| 6   | 64    | AVX-512F                 |
| 7   | 128   | AVX-512BW                |

```c
if (__simd_has_avx512bw() != 0) { /* 512-bit byte lanes are usable */ }
if ((cpu_features() & 32) != 0) { /* FMA */ }
```

## Vector math

Fixed-size 3- and 4-component math over `float64`. Every operand is the **address** of three (or four) contiguous `float64` — take it with `reinterpret_cast<int64>(&v)`. The destination comes first: these write through it rather than returning a value.

```c
void    __simd_v3_add(int64 dst, int64 a, int64 b)
void    __simd_v3_sub(int64 dst, int64 a, int64 b)
void    __simd_v3_mul(int64 dst, int64 a, int64 b)
void    __simd_v3_scale(int64 dst, int64 src, float64 k)
void    __simd_v3_neg(int64 dst, int64 src)
void    __simd_v3_cross(int64 dst, int64 a, int64 b)
float64 __simd_v3_dot(int64 a, int64 b)

void    __simd_v4_add(int64 dst, int64 a, int64 b)
void    __simd_v4_sub(int64 dst, int64 a, int64 b)
void    __simd_v4_mul(int64 dst, int64 a, int64 b)
void    __simd_v4_scale(int64 dst, int64 src, float64 k)
void    __simd_v4_neg(int64 dst, int64 src)
float64 __simd_v4_dot(int64 a, int64 b)
```

There is no `__simd_v4_cross`.

```c
struct V3 { float64 x; float64 y; float64 z; }

V3 a; a.x = 2.0; a.y = 3.0; a.z = 4.0;
V3 b; b.x = 5.0; b.y = 6.0; b.z = 7.0;
V3 r;

__simd_v3_add(reinterpret_cast<int64>(&r),
              reinterpret_cast<int64>(&a),
              reinterpret_cast<int64>(&b));      // r = (7, 9, 11)

float64 d = __simd_v3_dot(reinterpret_cast<int64>(&a),
                          reinterpret_cast<int64>(&b));   // 56
```

Nearly always you want the `vec3` / `vec4` value types from the [3D math addon](/perception/enma-lang/addons/math-3d.md), which wrap exactly these.

## SIMD registers

Nine builtin types live in a vector register. Declare one like any other value.

| Type      | Lanes       | Type      | Lanes       | Type      | Lanes        |
| --------- | ----------- | --------- | ----------- | --------- | ------------ |
| `__m128`  | 4 × float32 | `__m256`  | 8 × float32 | `__m512`  | 16 × float32 |
| `__m128d` | 2 × float64 | `__m256d` | 4 × float64 | `__m512d` | 8 × float64  |
| `__m128i` | 16 bytes    | `__m256i` | 32 bytes    | `__m512i` | 64 bytes     |

The integer types carry no fixed lane width. `set1`, the lane reads and the operators all work on **32-bit lanes**; the `_epi8` / `_epi16` / `_epi64` forms select another width.

### Arithmetic uses operators

There is no `__m128_add`. Write the operator:

```c
__m128d a = __m128d_set1(10.0);
__m128d b = __m128d_set1(3.0);
__m128d r = a + b;                  // and - * /
```

Float families (`__m128` `__m128d` `__m256` `__m256d` `__m512` `__m512d`) take `+ - * /`. Integer families take `+ - *` on 32-bit lanes — there is no packed integer divide.

### Memory

`load` and `store` take a raw address as `int64`, not a pointer:

```c
std::vector<float64> buf = {1.5, 2.5};
int64   p = reinterpret_cast<int64>(buf.data());
__m128d v = __m128d_load(p);
float64 s = __m128d_lane0(v) + __m128d_lane1(v);   // 4.0
```

### `__m128` — 4 × float32

```c
__m128  __m128_set1(float64 value)
__m128  __m128_load(int64 addr)
void    __m128_store(int64 addr, __m128 value)
__m128  __m128_min(__m128 a, __m128 b)
__m128  __m128_max(__m128 a, __m128 b)
__m128  __m128_sqrt(__m128 v)
__m128  __m128_abs(__m128 v)
float64 __m128_lane0(__m128 v)
float64 __m128_lane1(__m128 v)
float64 __m128_lane2(__m128 v)
float64 __m128_lane3(__m128 v)
float64 __m128_hsum(__m128 v)
```

Lane reads widen the float32 lane back to `float64`, the scalar float type.

### `__m128d` — 2 × float64

```c
__m128d __m128d_set1(float64 value)
__m128d __m128d_load(int64 addr)
void    __m128d_store(int64 addr, __m128d value)
__m128d __m128d_min(__m128d a, __m128d b)
__m128d __m128d_max(__m128d a, __m128d b)
__m128d __m128d_sqrt(__m128d v)
__m128d __m128d_abs(__m128d v)
__m128d __m128d_cmp_eq(__m128d a, __m128d b)
__m128d __m128d_cmp_ne(__m128d a, __m128d b)
__m128d __m128d_cmp_lt(__m128d a, __m128d b)
__m128d __m128d_cmp_le(__m128d a, __m128d b)
__m128d __m128d_cmp_gt(__m128d a, __m128d b)
__m128d __m128d_cmp_ge(__m128d a, __m128d b)
int64   __m128d_movemask(__m128d v)
float64 __m128d_lane0(__m128d v)
float64 __m128d_lane1(__m128d v)
float64 __m128d_hsum(__m128d v)
```

A compare returns a **mask** — every bit of a lane set if the test held, all clear if it did not. `movemask` collapses the lane sign bits into one integer, one bit per lane.

```c
__m128d a  = __m128d_set1(2.0);
__m128d b  = __m128d_set1(2.0);
__m128d eq = __m128d_cmp_eq(a, b);
int64   m  = __m128d_movemask(eq);    // 3 — both lanes matched
```

### `__m128i` — 16 bytes

```c
__m128i __m128i_set1(int64 value)                 // 4 x int32
__m128i __m128i_set1_epi8(int64 value)            // 16 x int8
__m128i __m128i_set1_epi16(int64 value)           // 8 x int16
__m128i __m128i_set1_epi64(int64 value)           // 2 x int64
__m128i __m128i_set_epi64x(int64 hi, int64 lo)    // lo becomes lane 0
__m128i __m128i_load(int64 addr)
void    __m128i_store(int64 addr, __m128i value)
__m128i __m128i_and(__m128i a, __m128i b)
__m128i __m128i_or(__m128i a, __m128i b)
__m128i __m128i_xor(__m128i a, __m128i b)
__m128i __m128i_add_epi8(__m128i a, __m128i b)
__m128i __m128i_add_epi16(__m128i a, __m128i b)
__m128i __m128i_add_epi64(__m128i a, __m128i b)
__m128i __m128i_sub_epi8(__m128i a, __m128i b)
__m128i __m128i_sub_epi16(__m128i a, __m128i b)
__m128i __m128i_sub_epi64(__m128i a, __m128i b)
__m128i __m128i_mul_epi16(__m128i a, __m128i b)
__m128i __m128i_cmp_eq_epi8(__m128i a, __m128i b)
__m128i __m128i_shuffle_epi8(__m128i src, __m128i control)
int64   __m128i_movemask_epi8(__m128i v)
int64   __m128i_lane0(__m128i v)
int64   __m128i_lane1(__m128i v)
int64   __m128i_lane2(__m128i v)
int64   __m128i_lane3(__m128i v)
int64   __m128i_lane_epi8(__m128i v, int64 index)
int64   __m128i_lane_epi16(__m128i v, int64 index)
int64   __m128i_lane_epi64(__m128i v, int64 index)
```

`lane0`…`lane3` read 32-bit lanes and sign-extend. The `index` of a `lane_epi*` read encodes into the instruction, so it must be a compile-time constant. `shuffle_epi8` is PSHUFB: each control byte picks a source byte by its low 4 bits, or zeroes the output byte when its high bit is set.

```c
__m128i v = __m128i_set_epi64x(11, 22);
__m128i_lane_epi64(v, 0);              // 22
__m128i_lane_epi64(v, 1);              // 11

__m128i m = __m128i_cmp_eq_epi8(__m128i_set1_epi8(65), __m128i_set1_epi8(65));
__m128i_movemask_epi8(m);              // 65535 — all 16 bytes matched
```

### `__m256` — 8 × float32

```c
__m256  __m256_set1(float64 value)
__m256  __m256_load(int64 addr)
void    __m256_store(int64 addr, __m256 value)
__m256  __m256_min(__m256 a, __m256 b)
__m256  __m256_max(__m256 a, __m256 b)
__m256  __m256_sqrt(__m256 v)
__m256  __m256_abs(__m256 v)
float64 __m256_hsum(__m256 v)
```

### `__m256d` — 4 × float64

```c
__m256d __m256d_set1(float64 value)
__m256d __m256d_load(int64 addr)
void    __m256d_store(int64 addr, __m256d value)
__m256d __m256d_min(__m256d a, __m256d b)
__m256d __m256d_max(__m256d a, __m256d b)
__m256d __m256d_sqrt(__m256d v)
__m256d __m256d_abs(__m256d v)
__m256d __m256d_cmp_eq(__m256d a, __m256d b)
__m256d __m256d_cmp_lt(__m256d a, __m256d b)
float64 __m256d_hsum(__m256d v)
float64 __m256d_min_reduce(__m256d v)
float64 __m256d_max_reduce(__m256d v)
```

### `__m256i` — 32 bytes

```c
__m256i __m256i_set1(int64 value)                 // 8 x int32
__m256i __m256i_set1_epi64(int64 value)           // 4 x int64
__m256i __m256i_load(int64 addr)
void    __m256i_store(int64 addr, __m256i value)
__m256i __m256i_and(__m256i a, __m256i b)
__m256i __m256i_or(__m256i a, __m256i b)
__m256i __m256i_xor(__m256i a, __m256i b)
__m256i __m256i_add_epi8(__m256i a, __m256i b)
__m256i __m256i_add_epi16(__m256i a, __m256i b)
__m256i __m256i_add_epi64(__m256i a, __m256i b)
__m256i __m256i_sub_epi8(__m256i a, __m256i b)
__m256i __m256i_sub_epi16(__m256i a, __m256i b)
__m256i __m256i_sub_epi64(__m256i a, __m256i b)
__m256i __m256i_mul_epi16(__m256i a, __m256i b)
__m256i __m256i_cmp_eq_epi8(__m256i a, __m256i b)
__m256i __m256i_shuffle_epi8(__m256i src, __m256i control)
int64   __m256i_movemask_epi8(__m256i v)
```

`shuffle_epi8` acts per 16-byte half, as the instruction does — a control byte never reaches across the halves. There is no 256-bit lane read; store to memory and index it.

### `__m512` — 16 × float32

```c
__m512  __m512_set1(float64 value)
__m512  __m512_load(int64 addr)
void    __m512_store(int64 addr, __m512 value)
__m512  __m512_min(__m512 a, __m512 b)
__m512  __m512_max(__m512 a, __m512 b)
__m512  __m512_sqrt(__m512 v)
__m512  __m512_abs(__m512 v)
float64 __m512_hsum(__m512 v)
```

### `__m512d` — 8 × float64

```c
__m512d __m512d_set1(float64 value)
__m512d __m512d_load(int64 addr)
void    __m512d_store(int64 addr, __m512d value)
__m512d __m512d_min(__m512d a, __m512d b)
__m512d __m512d_max(__m512d a, __m512d b)
__m512d __m512d_sqrt(__m512d v)
__m512d __m512d_abs(__m512d v)
float64 __m512d_hsum(__m512d v)
```

### `__m512i` — 64 bytes

```c
__m512i __m512i_set1(int64 value)                 // 16 x int32
__m512i __m512i_set1_epi64(int64 value)           // 8 x int64
__m512i __m512i_load(int64 addr)
void    __m512i_store(int64 addr, __m512i value)
__m512i __m512i_and(__m512i a, __m512i b)
__m512i __m512i_or(__m512i a, __m512i b)
__m512i __m512i_xor(__m512i a, __m512i b)
__m512i __m512i_add_epi8(__m512i a, __m512i b)
__m512i __m512i_add_epi16(__m512i a, __m512i b)
__m512i __m512i_add_epi64(__m512i a, __m512i b)
__m512i __m512i_sub_epi8(__m512i a, __m512i b)
__m512i __m512i_sub_epi16(__m512i a, __m512i b)
__m512i __m512i_sub_epi64(__m512i a, __m512i b)
__m512i __m512i_mul_epi16(__m512i a, __m512i b)
```

The 512-bit families are the narrowest surface: no lane reads, no compares, no movemask, no shuffle. Byte and word arithmetic needs AVX-512BW — gate it on `__simd_has_avx512bw()`.

### A hand-written kernel

```c
float64 dot_kernel(float64* a, float64* b, int64 n) {
    __m256d acc = __m256d_set1(0.0);
    int64 i = 0;
    while (i + 4 <= n) {
        __m256d x = __m256d_load(reinterpret_cast<int64>(&a[i]));
        __m256d y = __m256d_load(reinterpret_cast<int64>(&b[i]));
        acc = acc + x * y;
        i = i + 4;
    }
    float64 total = __m256d_hsum(acc);
    while (i < n) { total = total + a[i] * b[i]; i = i + 1; }   // scalar tail
    return total;
}

std::vector<float64> a = {1.0, 2.0, 3.0, 4.0, 5.0};
std::vector<float64> b = {2.0, 2.0, 2.0, 2.0, 2.0};
dot_kernel(a.data(), b.data(), 5);      // 30.0
```

Gate the wide path on `__simd_has_avx()` if the code has to run on machines without it. For anything that fits the shape, the [SIMD addon](/perception/enma-lang/addons/simd.md) kernels already do this selection for you.

## Reflection builtins

`sizeof` `alignof` `offsetof` `typeid` `decltype` `__is_class` `__manager_of` are covered in [Compile-Time Evaluation](/perception/enma-lang/language-guide/compile-time.md).
