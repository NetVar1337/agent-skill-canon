> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/bits.md).

# Bits

Registered with `register_addon_bits(engine)`. Auto-imported.

Every function takes and returns `int64`, and treats the value as an unsigned bit pattern. The plain forms work on all 64 bits; each `_i32` form masks to the low 32 first, so rotates wrap at 32 and the zero-count results top out at 32.

## Population count

```c
int64 popcount(int64 v)
int64 popcount_i32(int64 v)
```

```c
popcount(0xFF);        // 8
popcount(-1);          // 64 — every bit set
popcount_i32(-1);      // 32 — only the low half counted
```

## Leading and trailing zeros

`clz` counts zero bits above the highest set bit, `ctz` zero bits below the lowest. For an input of zero both return the full width instead of being undefined.

```c
int64 clz(int64 v)
int64 clz_i32(int64 v)
int64 ctz(int64 v)
int64 ctz_i32(int64 v)
```

```c
clz(1);            // 63
clz(0);            // 64
clz_i32(1);        // 31
ctz(256);          // 8 — bit 8 is the lowest set bit
ctz_i32(0x1000);   // 12
```

## Rotates

The shift count is reduced modulo the width, so any `n` is valid.

```c
int64 rotl(int64 v, int64 n)
int64 rotl_i32(int64 v, int64 n)
int64 rotr(int64 v, int64 n)
int64 rotr_i32(int64 v, int64 n)
```

```c
int64 a = rotl(0x12345678, 4);      // 0x123456780
rotr(a, 4);                         // 0x12345678 — round-trips
rotl_i32(0x12345678, 8);            // 0x34567812 — wraps within 32 bits
```

## Byte swap

```c
int64 bswap(int64 v)
int64 bswap_i32(int64 v)
```

```c
bswap(0x0102030405060708);   // 0x0807060504030201
bswap_i32(0x12345678);       // 0x78563412
```

## Parity

```c
int64 parity(int64 v)     // 1 if the set-bit count is odd, else 0
```

```c
parity(7);    // 1 — three bits
parity(3);    // 0 — two bits
```

## Bit reverse

Mirrors the whole pattern end to end.

```c
int64 bit_reverse(int64 v)
int64 bit_reverse_i32(int64 v)
```

```c
bit_reverse(1);        // 0x8000000000000000 — bit 0 lands at bit 63
bit_reverse_i32(1);    // 0x80000000
```

## Single-bit operations

```c
int64 set_bit(int64 v, int64 index)
int64 clear_bit(int64 v, int64 index)
int64 toggle_bit(int64 v, int64 index)
int64 test_bit(int64 v, int64 index)    // 1 if set, else 0
```

```c
set_bit(0, 3);        // 8
clear_bit(15, 1);     // 13
toggle_bit(5, 1);     // 7
test_bit(4, 2);       // 1
```

## Bit ranges

`lo` and `hi` are both **inclusive**. `extract_bits` returns the range shifted down to bit 0; `insert_bits` returns `v` with that range replaced by the low bits of `val`.

```c
int64 extract_bits(int64 v, int64 lo, int64 hi)
int64 insert_bits(int64 v, int64 val, int64 lo, int64 hi)
```

```c
extract_bits(0xDEAD, 0, 7);         // 0xAD
extract_bits(0xDEAD, 8, 15);        // 0xDE
insert_bits(0xFF00, 0xAB, 0, 7);    // 0xFFAB
```

## Powers of two

```c
int64 is_pow2(int64 v)      // 1 if v is a power of two, else 0
int64 next_pow2(int64 v)    // smallest power of two >= v; 1 when v <= 1
int64 prev_pow2(int64 v)    // largest power of two <= v; 0 when v == 0
```

```c
is_pow2(16);      // 1
is_pow2(0);       // 0
next_pow2(5);     // 8
prev_pow2(17);    // 16
```

## Alignment

Round to a multiple of `n`, which is normally a power of two.

```c
int64 align_up(int64 v, int64 n)
int64 align_down(int64 v, int64 n)
```

```c
align_up(13, 8);      // 16
align_down(13, 8);    // 8
```
