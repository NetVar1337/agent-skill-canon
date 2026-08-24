> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/math.md).

# Math

Registered with `register_addon_math(engine)`. The same registration also defines the [vec2 / vec3 / vec4, quat and mat4](/perception/enma-lang/addons/math-3d.md) types.

Every scalar function takes and returns `float64` unless stated otherwise. The names mandated by the standard also resolve under their `std::` spelling — `std::sqrt`, `std::pow`, `std::floor` and so on — so either form works.

## Trigonometry

```c
float64 sin(float64 x)
float64 cos(float64 x)
float64 tan(float64 x)
float64 asin(float64 x)
float64 acos(float64 x)
float64 atan(float64 x)
float64 atan2(float64 y, float64 x)     // note the order: y first
```

```c
float64 sinh(float64 x)
float64 cosh(float64 x)
float64 tanh(float64 x)
float64 asinh(float64 x)
float64 acosh(float64 x)
float64 atanh(float64 x)
```

Angles are radians.

```c
float64 deg_to_rad(float64 degrees)
float64 rad_to_deg(float64 radians)
```

```c
deg_to_rad(180.0);              // 3.141593
rad_to_deg(std::numbers::pi);   // 180
```

## Powers and logarithms

```c
float64 sqrt(float64 x)
float64 cbrt(float64 x)
float64 pow(float64 base, float64 exponent)
float64 hypot(float64 a, float64 b)         // sqrt(a*a + b*b) without overflow
float64 exp(float64 x)
float64 log(float64 x)                      // natural log
float64 log2(float64 x)
float64 log10(float64 x)
float64 log_base(float64 x, float64 base)
```

```c
sqrt(9.0);              // 3
pow(2.0, 10.0);         // 1024
hypot(3.0, 4.0);        // 5
log_base(8.0, 2.0);     // 3
```

## Rounding and remainder

```c
float64 floor(float64 x)                     // toward negative infinity
float64 ceil(float64 x)                      // toward positive infinity
float64 round(float64 x)                     // half away from zero
float64 fmod(float64 x, float64 y)           // remainder, sign of x
float64 fract(float64 x)                     // x - floor(x); always in [0, 1)
```

```c
floor(-1.5);      // -2
ceil(-1.5);       // -1
round(2.5);       // 3
fmod(7.0, 3.0);   // 1
fract(-1.25);     // 0.75
```

## Comparison

`abs`, `min`, `max` and `clamp` are overloaded on `int32`, `int64` and `float64` and return the type they were given. `fabs`, `fmin` and `fmax` are the float-only spellings.

```c
int32   abs(int32 v)        int64   abs(int64 v)        float64 abs(float64 v)
int32   min(int32 a, int32 b)   int64 min(int64 a, int64 b)   float64 min(float64 a, float64 b)
int32   max(int32 a, int32 b)   int64 max(int64 a, int64 b)   float64 max(float64 a, float64 b)
int32   clamp(int32 v, int32 lo, int32 hi)
int64   clamp(int64 v, int64 lo, int64 hi)
float64 clamp(float64 v, float64 lo, float64 hi)

float64 fabs(float64 v)
float64 fmin(float64 a, float64 b)
float64 fmax(float64 a, float64 b)
float64 sign(float64 v)         // -1, 0 or +1
float64 saturate(float64 v)     // clamp(v, 0, 1)
```

```c
abs(-7);                    // 7    — int64 in, int64 out
abs(-2.5);                  // 2.5  — float64 in, float64 out
clamp(15, 0, 10);           // 10
clamp(9.0, 0.0, 1.0);       // 1
sign(-3.0);                 // -1
saturate(2.0);              // 1
```

## Interpolation

```c
float64 lerp(float64 a, float64 b, float64 t)              // a + (b - a) * t
float64 inverse_lerp(float64 a, float64 b, float64 v)      // where v sits in [a, b]; 0 if a == b
float64 remap(float64 v, float64 from_lo, float64 from_hi,
              float64 to_lo, float64 to_hi)
float64 smoothstep(float64 edge0, float64 edge1, float64 x)
float64 step(float64 edge, float64 x)                      // 0 below the edge, 1 at or above
float64 lerp_angle(float64 a, float64 b, float64 t)        // takes the short way around
float64 move_toward(float64 current, float64 target, float64 max_step)
float64 wrap(float64 v, float64 lo, float64 hi)            // wrap into [lo, hi)
```

```c
lerp(0.0, 10.0, 0.25);                   // 2.5
inverse_lerp(0.0, 10.0, 2.5);            // 0.25
remap(5.0, 0.0, 10.0, 0.0, 100.0);       // 50
smoothstep(0.0, 1.0, 0.5);               // 0.5
step(1.0, 0.5);                          // 0
move_toward(0.0, 10.0, 3.0);             // 3 — never overshoots the target
wrap(11.0, 0.0, 10.0);                   // 1
```

## Easing

Each takes a `t` in `[0, 1]` and returns the eased position.

```c
float64 ease_in(float64 t)        // slow start
float64 ease_out(float64 t)       // slow finish
float64 ease_in_out(float64 t)    // slow at both ends
```

```c
ease_in(0.5);       // 0.25
ease_out(0.5);      // 0.75
ease_in_out(0.5);   // 0.5
```

## Classification and float bits

```c
bool is_nan(float64 v)          bool std::isnan(float64 v)
bool is_inf(float64 v)          bool std::isinf(float64 v)
bool is_finite(float64 v)       bool std::isfinite(float64 v)
bool approx_eq(float64 a, float64 b, float64 epsilon)

float64 copysign(float64 magnitude, float64 sign)
float64 nextafter(float64 from, float64 toward)
```

```c
is_finite(0.0);                        // true
approx_eq(1.0, 1.0000001, 0.001);      // true
copysign(3.0, -1.0);                   // -3
```

For reinterpreting a float's bits as an integer, use `std::bit_cast` — a language builtin, not a math function. See [Builtin Instructions](/perception/enma-lang/language-guide/builtin-instructions.md#bit-reinterpretation).

## Constants

Constants live in `namespace std::numbers` under their standard names, so nothing is taken from the global scope.

```c
std::numbers::pi        std::numbers::e         std::numbers::inv_pi
std::numbers::sqrt2     std::numbers::ln2       std::numbers::log2e
std::numbers::egamma    std::numbers::phi
```

These are constants, not calls — no parentheses.

## Random

```c
float64 random_double()                                  // in [0, 1)
int64   rand_int(int64 lo, int64 hi)                     // in [lo, hi)
bool    random_bool()                                    // even coin flip
float64 random_gaussian(float64 mean, float64 deviation)
void    random_seed(int64 seed)                          // fix the sequence
```

```c
random_seed(42);

float64 f = random_double();               // 0 <= f < 1
int64   n = rand_int(1, 7);                // a die roll: 1..6
bool    b = random_bool();
float64 g = random_gaussian(0.0, 1.0);     // standard normal
```

Seeding with a fixed value makes a run reproducible, which is what you want for a test or a deterministic simulation.
