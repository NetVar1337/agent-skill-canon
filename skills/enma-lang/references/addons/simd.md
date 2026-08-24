> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/simd.md).

# SIMD

Registered with `register_addon_simd(engine)`. Auto-imported as `simd`.

Every kernel picks its width at run time — AVX-512, then AVX/AVX2, then SSE, with a scalar tail covering the remainder. You write one call and get the widest path the CPU supports.

Three rules hold across the whole surface:

* **Inputs first, `dst` last.** The exception is `simd_scale_f64`, where the scalar sits between them.
* **Every kernel returns `int64`** — the number of elements it processed, which is the smallest of the operand sizes.
* **`dst` must already be sized.** The natives never grow it; an empty `dst` means zero elements processed.

Element types are fixed per kernel. `simd_add_i32` takes `std::vector<int32>` and nothing else — there is no conversion and no unsigned twin.

For hand-writing your own kernel with raw registers, see [Builtin Instructions](/perception/enma-lang/language-guide/builtin-instructions.md#simd-registers).

## float64

```c
int64 simd_add_f64(const std::vector<float64>& a, const std::vector<float64>& b, std::vector<float64>& dst)
int64 simd_sub_f64(const std::vector<float64>& a, const std::vector<float64>& b, std::vector<float64>& dst)
int64 simd_mul_f64(const std::vector<float64>& a, const std::vector<float64>& b, std::vector<float64>& dst)
int64 simd_div_f64(const std::vector<float64>& a, const std::vector<float64>& b, std::vector<float64>& dst)
int64 simd_min_f64(const std::vector<float64>& a, const std::vector<float64>& b, std::vector<float64>& dst)
int64 simd_max_f64(const std::vector<float64>& a, const std::vector<float64>& b, std::vector<float64>& dst)
int64 simd_abs_f64(const std::vector<float64>& src, std::vector<float64>& dst)
int64 simd_sqrt_f64(const std::vector<float64>& src, std::vector<float64>& dst)
int64 simd_fma_f64(const std::vector<float64>& a, const std::vector<float64>& b,
                   const std::vector<float64>& c, std::vector<float64>& dst)   // a[i] * b[i] + c[i]
int64 simd_scale_f64(const std::vector<float64>& src, float64 k, std::vector<float64>& dst)
```

```c
std::vector<float64> a = {1.0, 2.0, 3.0, 4.0};
std::vector<float64> b = {10.0, 20.0, 30.0, 40.0};
std::vector<float64> dst;
dst.resize(4);

int64 n = simd_add_f64(a, b, dst);   // n = 4, dst = {11, 22, 33, 44}
simd_scale_f64(a, 2.5, dst);         // dst = {2.5, 5, 7.5, 10}
```

### Reductions

```c
float64 simd_dot_f64(const std::vector<float64>& a, const std::vector<float64>& b)
float64 simd_sum_f64(const std::vector<float64>& a)
float64 simd_min_reduce_f64(const std::vector<float64>& a)
float64 simd_max_reduce_f64(const std::vector<float64>& a)
```

```c
simd_dot_f64(a, b);          // 300 — 1*10 + 2*20 + 3*30 + 4*40
simd_sum_f64(a);             // 10
simd_min_reduce_f64(a);      // 1
simd_max_reduce_f64(a);      // 4
```

### Compare

Each lane becomes `1.0` or `0.0`.

```c
int64 simd_cmp_eq_f64(const std::vector<float64>& a, const std::vector<float64>& b, std::vector<float64>& dst)
int64 simd_cmp_lt_f64(const std::vector<float64>& a, const std::vector<float64>& b, std::vector<float64>& dst)
```

```c
std::vector<float64> x = {1.0, 2.0, 3.0, 4.0};
std::vector<float64> y = {1.0, 9.0, 3.0, 9.0};
std::vector<float64> eq;
eq.resize(4);

simd_cmp_eq_f64(x, y, eq);   // {1, 0, 1, 0}
simd_cmp_lt_f64(x, y, eq);   // {0, 1, 0, 1}
```

## float32

Twice the lanes per block. The reductions return `float64` so the accumulation does not lose precision.

```c
int64 simd_add_f32(const std::vector<float32>& a, const std::vector<float32>& b, std::vector<float32>& dst)
int64 simd_sub_f32(const std::vector<float32>& a, const std::vector<float32>& b, std::vector<float32>& dst)
int64 simd_mul_f32(const std::vector<float32>& a, const std::vector<float32>& b, std::vector<float32>& dst)
int64 simd_div_f32(const std::vector<float32>& a, const std::vector<float32>& b, std::vector<float32>& dst)
int64 simd_min_f32(const std::vector<float32>& a, const std::vector<float32>& b, std::vector<float32>& dst)
int64 simd_max_f32(const std::vector<float32>& a, const std::vector<float32>& b, std::vector<float32>& dst)
int64 simd_abs_f32(const std::vector<float32>& src, std::vector<float32>& dst)
int64 simd_sqrt_f32(const std::vector<float32>& src, std::vector<float32>& dst)

float64 simd_dot_f32(const std::vector<float32>& a, const std::vector<float32>& b)
float64 simd_sum_f32(const std::vector<float32>& a)
```

```c
std::vector<float32> p = {1.0, 2.0, 3.0, 4.0};
std::vector<float32> q = {2.0, 2.0, 2.0, 2.0};
std::vector<float32> out;
out.resize(4);

simd_mul_f32(p, q, out);   // {2, 4, 6, 8}
simd_dot_f32(p, q);        // 20.0
simd_sum_f32(p);           // 10.0
```

There are no `f32` compares and no `f32` min/max reductions.

## Integers

Buffers are packed at their element width, so a narrower type runs more lanes per block: 2 for `int64`, 4 for `int32`, 8 for `int16` at 128 bits, and up to four times that under AVX-512.

```c
int64 simd_add_i64(const std::vector<int64>& a, const std::vector<int64>& b, std::vector<int64>& dst)
int64 simd_sub_i64(const std::vector<int64>& a, const std::vector<int64>& b, std::vector<int64>& dst)
int64 simd_mul_i64(const std::vector<int64>& a, const std::vector<int64>& b, std::vector<int64>& dst)
int64 simd_sum_i64(const std::vector<int64>& a)

int64 simd_add_i32(const std::vector<int32>& a, const std::vector<int32>& b, std::vector<int32>& dst)
int64 simd_sub_i32(const std::vector<int32>& a, const std::vector<int32>& b, std::vector<int32>& dst)
int64 simd_mul_i32(const std::vector<int32>& a, const std::vector<int32>& b, std::vector<int32>& dst)

int64 simd_add_i16(const std::vector<int16>& a, const std::vector<int16>& b, std::vector<int16>& dst)
int64 simd_sub_i16(const std::vector<int16>& a, const std::vector<int16>& b, std::vector<int16>& dst)
int64 simd_mul_i16(const std::vector<int16>& a, const std::vector<int16>& b, std::vector<int16>& dst)
```

```c
std::vector<int32> a = {1, 2, 3, 4};
std::vector<int32> b = {10, 20, 30, 40};
std::vector<int32> dst;
dst.resize(4);

simd_mul_i32(a, b, dst);   // {10, 40, 90, 160}
```

There is no integer divide — there is no packed integer divide instruction. `simd_mul_i64` falls back to a scalar loop for the same reason: no packed 64-bit multiply.

## Bytes

The byte kernels take `std::vector<uint8>`, sixteen lanes to a block. `simd_movemask_i8` is the one that reads sign bits, so it takes `std::vector<int8>`.

```c
int64 simd_add_i8(const std::vector<uint8>& a, const std::vector<uint8>& b, std::vector<uint8>& dst)      // wraps
int64 simd_sub_i8(const std::vector<uint8>& a, const std::vector<uint8>& b, std::vector<uint8>& dst)
int64 simd_cmp_eq_i8(const std::vector<uint8>& a, const std::vector<uint8>& b, std::vector<uint8>& dst)   // 0xFF or 0x00
int64 simd_shuffle_i8(const std::vector<uint8>& src, const std::vector<uint8>& mask, std::vector<uint8>& dst)
int64 simd_movemask_i8(const std::vector<int8>& a)   // bit i = sign bit of a[i], first 64 bytes
```

```c
std::vector<uint8> haystack = {65, 66, 67, 66};
std::vector<uint8> needle   = {66, 66, 66, 66};
std::vector<uint8> hits;
hits.resize(4);

simd_cmp_eq_i8(haystack, needle, hits);   // {0, 255, 0, 255}
```

`simd_shuffle_i8` is PSHUFB per 16-byte block: each mask byte picks a source byte within its own block by the low 4 bits, or zeroes the output byte when the high bit is set. It processes whole blocks only — a trailing partial block is left untouched, and the return value tells you how far it got.

```c
std::vector<uint8> src;  src.resize(16);
std::vector<uint8> mask; mask.resize(16);
std::vector<uint8> out;  out.resize(16);

src[0] = 10; src[1] = 20; src[2] = 30; src[3] = 40;
mask[0] = 3; mask[1] = 2; mask[2] = 1; mask[3] = 0;

simd_shuffle_i8(src, mask, out);   // returns 16; out starts {40, 30, 20, 10}
```

## Bitwise

Byte-wise over the whole buffer.

```c
int64 simd_and(const std::vector<uint8>& a, const std::vector<uint8>& b, std::vector<uint8>& dst)
int64 simd_or(const std::vector<uint8>& a, const std::vector<uint8>& b, std::vector<uint8>& dst)
int64 simd_xor(const std::vector<uint8>& a, const std::vector<uint8>& b, std::vector<uint8>& dst)
```

```c
std::vector<uint8> a = {12, 12, 12, 12};
std::vector<uint8> b = {10, 10, 10, 10};
std::vector<uint8> r;
r.resize(4);

simd_and(a, b, r);   // {8, 8, 8, 8}
simd_or(a, b, r);    // {14, 14, 14, 14}
simd_xor(a, b, r);   // {6, 6, 6, 6}
```

## Memory

```c
int64 simd_memset(std::vector<int64>& arr, int64 val)                        // fills every element
int64 simd_memcpy(const std::vector<int64>& src, std::vector<int64>& dst)
```

```c
std::vector<int64> arr;
arr.resize(6);
std::vector<int64> copy;
copy.resize(6);

simd_memset(arr, 9);        // returns 6, arr = {9, 9, 9, 9, 9, 9}
simd_memcpy(arr, copy);     // returns 6
```

## CPU feature gates

The kernels select their own width, so you rarely need these — they matter when you branch between a kernel and your own code. They return `int64`, so compare against zero.

```c
int64 cpu_features()
int64 __simd_has_avx()
int64 __simd_has_avx2()
int64 __simd_has_avx512()
int64 __simd_has_avx512bw()
```

```c
std::vector<float64> dst;
dst.resize(a.size());

if (__simd_has_avx() != 0) {
    simd_mul_f64(a, b, dst);
} else {
    for (int64 i = 0; i < a.size(); i = i + 1) { dst[i] = a[i] * b[i]; }
}
```

The bit layout of `cpu_features()` is in [Builtin Instructions](/perception/enma-lang/language-guide/builtin-instructions.md#cpu-features).
