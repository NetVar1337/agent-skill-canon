> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/enma-lang/addons/math-3d.md).

# Math:3D

Value structs in the math addon. Registered with `register_addon_math(engine)`, the same call that provides the [scalar math](/perception/enma-lang/addons/math.md) functions.

| Type   | Size      | Components                             |
| ------ | --------- | -------------------------------------- |
| `vec2` | 16 bytes  | `.x .y`                                |
| `vec3` | 24 bytes  | `.x .y .z`                             |
| `vec4` | 32 bytes  | `.x .y .z .w`                          |
| `quat` | 32 bytes  | `.x .y .z .w` — `w` is the scalar part |
| `mat4` | 128 bytes | `.m00` … `.m33`, row-major             |

Components are public fields, read and written directly — there is no getter form, and field access compiles to a plain memory read with no call and no allocation. Every type default-constructs to all zeros, so use `quat_identity()` and `mat4_identity()` when you want the do-nothing value.

Values are stored inline: a `std::vector<vec3>` is N × 24 bytes back to back, and `push_back` copies the value in.

All angles are radians.

## Construction

```c
vec2(float64 x, float64 y)
vec3(float64 x, float64 y, float64 z)
vec4(float64 x, float64 y, float64 z, float64 w)
quat(float64 x, float64 y, float64 z, float64 w)
mat4()                     // all zeros
```

```c
vec3 a = vec3(1.0, 2.0, 3.0);
vec3 zero;                       // (0, 0, 0)

a.x = 5.0;
float64 y = a.y;
```

## Vector operators

Available on `vec2`, `vec3` and `vec4`:

```c
vecN operator+(vecN b)      vecN operator-(vecN b)      vecN operator-()
vecN operator*(float64 s)   bool operator==(vecN b)     bool operator!()
void operator+=(vecN b)     void operator-=(vecN b)     operator bool()
```

`operator bool` is false when every component is zero, so a vector can be tested directly.

```c
vec3 a = vec3(1.0, 2.0, 3.0);
vec3 b = vec3(4.0, 5.0, 6.0);

vec3 s = a + b;      // (5, 7, 9)
vec3 d = b - a;      // (3, 3, 3)
vec3 k = a * 2.0;    // (2, 4, 6)
vec3 n = -a;         // (-1, -2, -3)
a += b;
```

## Vector methods

Shared by `vec2`, `vec3` and `vec4` — `vecN` below stands for the receiver's own type:

```c
vecN    v.add(vecN b)              // same as v + b
vecN    v.sub(vecN b)
vecN    v.scale(float64 s)         // same as v * s
vecN    v.neg()                    // same as -v
float64 v.dot(vecN b)
float64 v.length()
float64 v.length_sq()              // no square root
float64 v.distance(vecN b)
vecN    v.normalize()              // unit length; a zero vector stays zero
vecN    v.lerp(vecN b, float64 t)  // component-wise
```

```c
vec3 v = vec3(3.0, 4.0, 0.0);

v.length();                              // 5
v.length_sq();                           // 25
v.normalize().x;                         // 0.6
vec3(1.0,2.0,3.0).dot(vec3(4.0,5.0,6.0));    // 32
vec3(1.0,2.0,3.0).lerp(vec3(4.0,5.0,6.0), 0.5).x;   // 2.5
```

`vec2` additionally has:

```c
vec2 v.rotate(float64 angle_rad)   // counter-clockwise
```

`vec3` additionally has:

```c
vec3    v.cross(vec3 b)
vec3    v.reflect(vec3 normal)                       // mirror across the normal
vec3    v.project(vec3 onto)                         // component of v along onto
float64 v.angle(vec3 b)                              // radians between the two
vec3    v.rotate_around(vec3 axis, float64 angle_rad)  // axis normalized internally
```

```c
vec3(1.0,0.0,0.0).cross(vec3(0.0,1.0,0.0)).z;                    // 1
vec3(1.0,0.0,0.0).angle(vec3(0.0,1.0,0.0));                      // 1.570796
vec3(2.0,2.0,0.0).project(vec3(1.0,0.0,0.0)).x;                  // 2
vec3(1.0,0.0,0.0).rotate_around(vec3(0.0,0.0,1.0), deg_to_rad(90.0)).y;   // 1
```

## quat

Writing a component directly does not re-normalize — call `normalize()` afterwards if you need a unit quaternion.

```c
quat quat_identity()                                     // (0, 0, 0, 1)
quat quat_from_euler(float64 yaw, float64 pitch, float64 roll)   // Tait-Bryan ZYX
quat quat_from_axis_angle(vec3 axis, float64 angle_rad)
```

```c
quat    q.add(quat b)             quat    q.sub(quat b)
quat    q.scale(float64 s)        quat    q.neg()
float64 q.dot(quat b)             float64 q.length()
float64 q.length_sq()
quat    q.mul(quat b)             // Hamilton product; same as q * b
quat    q.normalize()
quat    q.conjugate()             // negates x, y, z
quat    q.inverse()               // equals conjugate for a unit quaternion
vec3    q.rotate(vec3 v)          // rotate a vector; assumes unit length
vec3    q.to_euler()              // yaw, pitch, roll packed into x, y, z
quat    q.slerp(quat b, float64 t)  // along the shorter arc
```

Operators: `+ - -() *` (Hamilton product), `==`, `!`, `+=`, `-=`, `operator bool`.

```c
quat id = quat_identity();                                  // w = 1
quat r  = quat_from_axis_angle(vec3(0.0,0.0,1.0), deg_to_rad(90.0));

r.rotate(vec3(1.0, 0.0, 0.0)).y;      // 1 — x turned into y
id.slerp(r, 0.0).w;                   // 1 — t = 0 gives the first rotation back
```

Multiplication is non-commutative: `a * b` means rotate by `b` first, then by `a`.

## mat4

Row-major, with `m00` … `m33` public. `get` / `set` take a row and a column in `0..3`.

```c
mat4 mat4_identity()
mat4 mat4_translation(vec3 t)
mat4 mat4_scale(vec3 s)
mat4 mat4_rotation_x(float64 angle_rad)
mat4 mat4_rotation_y(float64 angle_rad)
mat4 mat4_rotation_z(float64 angle_rad)
mat4 mat4_rotation_axis(vec3 axis, float64 angle_rad)    // axis normalized internally
mat4 mat4_from_quat(quat q)
mat4 mat4_perspective(float64 fov_rad, float64 aspect, float64 near_z, float64 far_z)
mat4 mat4_orthographic(float64 left, float64 right, float64 bottom,
                       float64 top, float64 near_z, float64 far_z)
mat4 mat4_look_at(vec3 eye, vec3 target, vec3 up)
```

```c
float64 m.get(int64 row, int64 col)
void    m.set(int64 row, int64 col, float64 value)
mat4    m.transpose()
mat4    m.inverse()                  // identity if the matrix is singular
float64 m.determinant()
mat4    m.mul(mat4 b)                // same as m * b
mat4    m.add(mat4 b)                // elementwise
mat4    m.sub(mat4 b)
mat4    m.scale(float64 s)           // elementwise
mat4    m.neg()
vec3    m.transform_point(vec3 v)    // applies translation, divides by w
vec3    m.transform_vec3(vec3 v)     // ignores translation — for directions
vec4    m.transform_vec4(vec4 v)     // full 4-component transform
```

Operators: `+ - -() *`, `==`, `+=`, `-=`, `*=`.

```c
mat4 t = mat4_translation(vec3(5.0, 0.0, 0.0));

t.transform_point(vec3(1.0, 0.0, 0.0)).x;   // 6 — translated
t.transform_vec3(vec3(1.0, 0.0, 0.0)).x;    // 1 — direction, untranslated
mat4_identity().determinant();               // 1
```

`mat4` is a value type, so `set` on a copy leaves the original alone.

## Patterns

```c
// A world matrix, then a point through it.
mat4 world = mat4_translation(pos) * mat4_from_quat(rot) * mat4_scale(scl);
vec3 world_pos = world.transform_point(local_pos);

// View-projection.
mat4 view = mat4_look_at(camera_pos, target, vec3(0.0, 1.0, 0.0));
mat4 proj = mat4_perspective(deg_to_rad(60.0), 16.0 / 9.0, 0.1, 1000.0);
vec3 ndc  = (proj * view).transform_point(world_pos);

// Blend two orientations and get the facing direction.
quat from = quat_identity();
quat to   = quat_from_axis_angle(vec3(0.0, 1.0, 0.0), deg_to_rad(90.0));
vec3 facing = from.slerp(to, t).rotate(vec3(0.0, 0.0, 1.0));
```

## Conventions

* `mat4_perspective` is right-handed with OpenGL-style depth — `z` clips to `[-1, 1]` after the perspective divide.
* `inverse()` hands back the identity for a singular matrix, so check `determinant() != 0.0` when that is possible.
* `quat_from_euler` and `to_euler` both use Tait-Bryan ZYX. Round-tripping is inexact at gimbal lock (`pitch = ±π/2`).
