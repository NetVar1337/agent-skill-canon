> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/render-api.md).

# Render API

Everything lives in `namespace render`. Either qualify each call as `render::draw_rect(...)` or drop a `using namespace render;` at the top of the script — same shape as `std::`.

```cpp
using namespace render;

int64 main() {
    // ... draw_rect / draw_text / get_font20 / get_view_width all unqualified below
    return 1;
}
```

Handles (`int64`) are encrypted pointers. Pass them back into other render calls; don't dereference or arithmetic them.

## Buffer input shape

Every native that consumes a byte buffer (vertex data, index data, constant-buffer payloads, texture pixels, font files) is fronted by two overloads:

* `const std::vector<float32>&` — the primary shape for shader data. HLSL vertex layouts and constant buffers are almost always packed float32 arrays (`float2 pos : POSITION`, `float u_time` cbuffer, `float4x4` transforms), so this is what user code writes. The wrapper multiplies `size()` by 4 to pass the byte count the native expects.
* `const std::vector<uint8>&` — for callers already holding a raw byte blob (asset loads, packed vertex formats, texture pixels). `size()` is the byte count.

`custom_draw_indexed` additionally takes `const std::vector<uint32>&` for its `index_data` parameter alongside the uint8 variant.

Font glyph ranges are `const std::vector<uint32>&` — pairs of `[start, end, 0]` codepoints.

`draw_polygon`'s `xy_pairs` is float32 only (interleaved `x, y, x, y, ...`).

Every string-taking native has a `const char*` overload and a `const std::string&` overload; both forward into the same C native.

## `color` type

`color` is an auto-imported script struct — no module import needed. Fields are `uint8 r/g/b/a` with natural byte layout.

```cpp
using namespace render;

int64 main() {
    color red = color(255, 0, 0, 255);
    draw_rect_filled(vec2(10.0, 10.0), vec2(100.0, 50.0), red, 4.0, 15);
    return 0;
}
```

Constructors:

```cpp
color();                                                // opaque black (a=255)
color(uint8 r, uint8 g, uint8 b, uint8 a);
color(int64 r, int64 g, int64 b, int64 a);             // narrows each channel via static_cast<uint8>
```

The `int64` overload lets `color(240, 90, 90, 255)` work with plain int literals.

Read channels directly: `c.r`, `c.g`, `c.b`, `c.a` (each is `uint8`).

Helper: `c.with_alpha(uint8 new_a)` returns a copy with a different alpha.

Under the hood the draw wrappers unpack `color` to 4 rgba scalars when calling the natives — the struct never crosses the FFI as a pointer.

## 2D primitives

```cpp
void draw_rect(vec2 pos, vec2 size, color c, float64 thickness, float64 rounding, uint8 rounding_flags);
void draw_rect_filled(vec2 pos, vec2 size, color c, float64 rounding, uint8 rounding_flags);
void draw_line(vec2 a, vec2 b, color c, float64 thickness);
void draw_circle(vec2 center, float64 radius, color c, float64 thickness, bool filled);
void draw_arc(vec2 center, vec2 radii, float64 start_deg, float64 sweep_deg, color c, float64 thickness, bool filled);
void draw_triangle(vec2 a, vec2 b, vec2 c, color col, float64 thickness, bool filled);
void draw_four_corner_gradient(vec2 pos, vec2 size, color tl, color tr, color bl, color br, float64 rounding);
void draw_polygon(const std::vector<float32>& xy_pairs, uint32 count_pairs, color c, float64 thickness, bool filled);
void draw_bitmap(int64 bmp, vec2 pos, vec2 size, color tint, bool rounded);

void draw_text(const char* text,        vec2 pos, color c, int64 font, int32 effect, color effect_color, float64 effect_amount);
void draw_text(const std::string& text, vec2 pos, color c, int64 font, int32 effect, color effect_color, float64 effect_amount);
```

`effect`: 0=none, 1=shadow, 2=outline. `rounding_flags`: bitmask of which corners to round (ImGui-style, `15` = all corners).

`draw_polygon`'s `xy_pairs` is interleaved float32 `x, y` — `count_pairs` is the number of vertices (half the float count).

## Text and fonts

```cpp
float64 get_text_width (int64 font, const char* text,        int32 maxw, int32 maxh);
float64 get_text_width (int64 font, const std::string& text, int32 maxw, int32 maxh);
float64 get_text_height(int64 font, const char* text,        int32 maxw, int32 maxh);
float64 get_text_height(int64 font, const std::string& text, int32 maxw, int32 maxh);
int32   get_char_advance(int64 font, uint32 wchar32);

int64 create_font(const char* path,        float64 size, bool antialias, bool load_color, const std::vector<uint32>& glyph_ranges);
int64 create_font(const std::string& path, float64 size, bool antialias, bool load_color, const std::vector<uint32>& glyph_ranges);

int64 create_font_mem(const char* label,        float64 size, const std::vector<uint8>& buf, bool antialias, bool load_color, const std::vector<uint32>& glyph_ranges);
int64 create_font_mem(const std::string& label, float64 size, const std::vector<uint8>& buf, bool antialias, bool load_color, const std::vector<uint32>& glyph_ranges);

int64 create_bitmap(const std::vector<uint8>& data);

int64 get_font18();
int64 get_font20();
int64 get_font24();
int64 get_font28();
```

`create_font` first tries the path as-is, then retries under perception's main dir. `glyph_ranges` holds uint32 codepoint pairs `[start, end, 0]`; pass an empty vector for the default range.

## Clipping

```cpp
void clip_push(vec2 pos, vec2 size);
void clip_pop();
```

## Viewport

```cpp
float64 get_view_width();
float64 get_view_height();
float64 get_view_scale();
float64 get_fps();
```

## Shaders

```cpp
int64 create_shader(const char* vs_source,        const char* ps_source,        const char* layout);
int64 create_shader(const std::string& vs_source, const std::string& ps_source, const std::string& layout);
int64 destroy_shader(int64 shader);

int64 create_compute_shader(const char* cs_source);
int64 create_compute_shader(const std::string& cs_source);
int64 destroy_compute_shader(int64 cs);
```

Layout format: `"SEMANTIC:INDEX:TYPE, ..."`. Example: `"POSITION:0:FLOAT2, COLOR:0:FLOAT4"`. Types: `FLOAT1`, `FLOAT2`, `FLOAT3`, `FLOAT4`, `BYTE4` (unorm), `UINT1`.

## Buffers

```cpp
int64 create_vertex_buffer(uint32 stride, uint32 max_vertices, bool dynamic);
int64 destroy_vertex_buffer(int64 vb);
int64 create_index_buffer(uint32 max_indices, bool use_32bit, bool dynamic);
int64 destroy_index_buffer(int64 ib);
int64 create_constant_buffer(uint32 size);
int64 destroy_constant_buffer(int64 cb);
int64 create_structured_buffer(uint32 element_size, uint32 element_count, bool cpu_write, bool gpu_write);
int64 destroy_structured_buffer(int64 sb);
```

## Pipeline state

```cpp
int64 create_blend_state(int32 src, int32 dst, int32 op, int32 src_alpha, int32 dst_alpha, int32 op_alpha);
int64 destroy_blend_state(int64 bs);
int64 create_sampler(int32 filter, int32 address_u, int32 address_v);
int64 destroy_sampler(int64 s);
int64 create_depth_stencil_state(bool depth_enable, bool depth_write, int32 compare_func);
int64 destroy_depth_stencil_state(int64 ds);
int64 create_rasterizer_state(int32 cull_mode, int32 fill_mode, bool scissor_enable);
int64 destroy_rasterizer_state(int64 rs);
```

Enum values (all `int32`):

* `blend_factor`: 0=ZERO, 1=ONE, 2=SRC\_ALPHA, 3=INV\_SRC\_ALPHA, 4=DEST\_ALPHA, 5=INV\_DEST\_ALPHA, 6=SRC\_COLOR, 7=INV\_SRC\_COLOR, 8=DEST\_COLOR, 9=INV\_DEST\_COLOR.
* `blend_op`: 0=ADD, 1=SUBTRACT, 2=REV\_SUBTRACT, 3=MIN, 4=MAX.
* `filter`: 0=POINT, 1=LINEAR, 2=ANISOTROPIC.
* `address`: 0=WRAP, 1=CLAMP, 2=MIRROR, 3=BORDER.
* `compare_func`: 0=NEVER, 1=LESS, 2=EQUAL, 3=LESS\_EQUAL, 4=GREATER, 5=NOT\_EQUAL, 6=GREATER\_EQUAL, 7=ALWAYS.

## Render targets and textures

```cpp
int64 create_render_target(uint32 width, uint32 height);
int64 destroy_render_target(int64 rt);
int64 create_depth_buffer(uint32 width, uint32 height);
int64 destroy_depth_buffer(int64 db);

int64 create_texture(uint32 width, uint32 height, const std::vector<uint8>& rgba_data);
int64 destroy_texture(int64 tex);

int64 load_texture(const char* path);
int64 load_texture(const std::string& path);
int64 load_texture_mem(const std::vector<uint8>& data);

float64 get_texture_width (int64 tex);
float64 get_texture_height(int64 tex);
```

`create_texture` wants `width * height * 4` bytes of RGBA8.

## Meshes

```cpp
int64 create_mesh_raw(const std::vector<uint8>& vertex_data, uint32 vertex_count, uint32 stride,
                      const std::vector<uint8>& index_data,  uint32 index_count,  bool use_32bit);

int64 load_mesh(const char* path);
int64 load_mesh(const std::string& path);
int64 load_mesh_mem(const std::vector<uint8>& data);

int64 destroy_mesh(int64 mesh);
int64   get_mesh_vert_count (int64 mesh);
int64   get_mesh_index_count(int64 mesh);
float64 get_mesh_stride     (int64 mesh);
float64 get_mesh_bounds_min_x(int64 mesh);
float64 get_mesh_bounds_min_y(int64 mesh);
float64 get_mesh_bounds_min_z(int64 mesh);
float64 get_mesh_bounds_max_x(int64 mesh);
float64 get_mesh_bounds_max_y(int64 mesh);
float64 get_mesh_bounds_max_z(int64 mesh);
```

`create_mesh_raw` takes raw byte buffers because a mesh's vertex layout is caller-defined — pack `stride` bytes per vertex into `vertex_data`. `index_data` is `uint16` or `uint32` indices depending on `use_32bit`.

## Custom draw

Each of `custom_draw`, `custom_draw_indexed`, and `draw_mesh` has a float32 overload (the primary shape for HLSL vertex/CB data — see [Buffer input shape](#buffer-input-shape)) and a uint8 overload (raw byte blobs). `custom_draw_indexed` additionally takes `std::vector<uint32>&` indices in its float32 variant.

```cpp
// float32 shape — vertex_data and cb_data are packed float arrays
void custom_draw(int64 shader, int64 vb,
                 const std::vector<float32>& vertex_data, uint32 vertex_count,
                 int32 topology, int64 blend, int64 sampler, int64 texture, int32 tex_slot,
                 int64 cb, const std::vector<float32>& cb_data, int32 cb_slot);

// uint8 shape — raw byte buffers
void custom_draw(int64 shader, int64 vb,
                 const std::vector<uint8>& vertex_data, uint32 vertex_count,
                 int32 topology, int64 blend, int64 sampler, int64 texture, int32 tex_slot,
                 int64 cb, const std::vector<uint8>& cb_data, int32 cb_slot);

void custom_draw_indexed(int64 shader, int64 vb,
                         const std::vector<float32>& vertex_data, uint32 vertex_count,
                         int64 ib, const std::vector<uint32>& index_data, uint32 index_count,
                         int32 topology, int64 blend, int64 sampler, int64 texture, int32 tex_slot,
                         int64 cb, const std::vector<float32>& cb_data, int32 cb_slot);

void custom_draw_indexed(int64 shader, int64 vb,
                         const std::vector<uint8>& vertex_data, uint32 vertex_count,
                         int64 ib, const std::vector<uint8>& index_data, uint32 index_count,
                         int32 topology, int64 blend, int64 sampler, int64 texture, int32 tex_slot,
                         int64 cb, const std::vector<uint8>& cb_data, int32 cb_slot);

void draw_mesh(int64 mesh, int64 shader, int32 topology,
               int64 blend, int64 sampler, int64 texture, int32 tex_slot,
               int64 cb, const std::vector<float32>& cb_data, int32 cb_slot);

void draw_mesh(int64 mesh, int64 shader, int32 topology,
               int64 blend, int64 sampler, int64 texture, int32 tex_slot,
               int64 cb, const std::vector<uint8>& cb_data, int32 cb_slot);

int64 dispatch_compute(int64 cs, uint32 x, uint32 y, uint32 z);
```

`topology`: 0=TRIANGLE\_LIST, 1=TRIANGLE\_STRIP, 2=LINE\_LIST, 3=LINE\_STRIP, 4=POINT\_LIST.

Any of `blend` / `sampler` / `texture` / `cb` can be `0` to skip binding. `cb_data` may be an empty vector.

## Binding and state

```cpp
int64 custom_set_render_target(int64 rt);
int64 custom_set_render_target_ext(int64 rt, int64 depth_buffer);
int64 custom_reset_render_target();
int64 custom_bind_rt_as_texture(int64 rt, int32 slot);
int64 custom_restore_state();
int64 custom_set_depth_stencil_state(int64 ds);
int64 custom_set_rasterizer_state(int64 rs);
int64 custom_set_viewport(float64 x, float64 y, float64 w, float64 h);
int64 custom_reset_viewport();
int64 custom_bind_texture(int64 texture, int64 sampler, int32 slot);

int64 custom_bind_constant_buffer(int64 cb, const std::vector<float32>& data, int32 slot, int32 stage);
int64 custom_bind_constant_buffer(int64 cb, const std::vector<uint8>&   data, int32 slot, int32 stage);

int64 custom_update_texture(int64 tex, uint32 x, uint32 y, uint32 w, uint32 h,
                            const std::vector<uint8>& rgba_data);

int64 custom_clear_render_target(int64 rt, float64 r, float64 g, float64 b, float64 a);
int64 custom_clear_depth_buffer(int64 db);

int64 bind_structured_buffer(int64 sb, int32 slot, int32 stage);
int64 update_structured_buffer(int64 sb, const std::vector<float32>& data);
int64 update_structured_buffer(int64 sb, const std::vector<uint8>&   data);

int64 capture_backbuffer(int32 slot);
```

`stage`: 0=VS, 1=PS, 2=CS (matches D3D11 shader stages).

Call `custom_restore_state()` after any custom-pipeline sequence before returning control to the 2D layer.

## Minimal triangle

```cpp
using namespace render;

int64 g_shader;
int64 g_vb;

int64 main() {
    std::string vs = "struct VSIn { float2 pos : POSITION; float4 color : COLOR; };\nstruct VSOut { float4 pos : SV_Position; float4 color : COLOR; };\nVSOut main(VSIn i) { VSOut o; o.pos = float4(i.pos, 0.0, 1.0); o.color = i.color; return o; }\n";
    std::string ps = "struct VSOut { float4 pos : SV_Position; float4 color : COLOR; };\nfloat4 main(VSOut i) : SV_Target { return i.color; }\n";

    g_shader = create_shader(vs, ps, "POSITION:0:FLOAT2, COLOR:0:FLOAT4");
    g_vb     = create_vertex_buffer(24, 3, true);  // 2*4 + 4*4 = 24 bytes per vertex
    register_routine(reinterpret_cast<int64>(my_draw), 0);
    return 1;
}

void my_draw(int64 data) {
    std::vector<float32> verts;
    // vertex 0: pos(-0.5, -0.5) color(1, 0, 0, 1)
    verts.push_back(-0.5f); verts.push_back(-0.5f);
    verts.push_back( 1.0f); verts.push_back( 0.0f); verts.push_back(0.0f); verts.push_back(1.0f);
    // vertex 1: pos(0.5, -0.5) color(0, 1, 0, 1)
    verts.push_back( 0.5f); verts.push_back(-0.5f);
    verts.push_back( 0.0f); verts.push_back( 1.0f); verts.push_back(0.0f); verts.push_back(1.0f);
    // vertex 2: pos(0, 0.5) color(0, 0, 1, 1)
    verts.push_back( 0.0f); verts.push_back( 0.5f);
    verts.push_back( 0.0f); verts.push_back( 0.0f); verts.push_back(1.0f); verts.push_back(1.0f);

    std::vector<float32> no_cb;
    custom_draw(g_shader, g_vb, verts, 3, 0, 0, 0, 0, 0, 0, no_cb, 0);
}
```

## Cleanup

On script unload, every handle returned by `create_*` / `load_*` is destroyed automatically. Explicit `destroy_*` is optional and only needed if you want to free a resource mid-script.
