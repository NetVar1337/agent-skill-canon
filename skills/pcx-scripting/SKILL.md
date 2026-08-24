---
name: pcx-scripting
description: PCX (Perception.cx) scripting with Enma and AngelScript for game interaction, memory manipulation, rendering, GUI, and automation. Full API reference for Enma proc/render/input/GUI/net/unicorn/zydis/win/sound APIs. Invoke with /pcx-scripting or when the task involves Perception.cx scripting.
---

# PCX / Perception.cx scripting

## Activation

Use when the task involves writing PCX scripts, game interaction via
Perception.cx, Enma scripting, or AngelScript scripting.

## Language selection

- **Enma** (primary): AOT/JIT compiled, C++-like syntax. Use for all new
  scripts. Full API below.
- **AngelScript** (deprecated): Being removed after Enma release. Only use
  for maintaining legacy scripts. Similar API surface, AngelScript syntax.

## Enma conventions

- **Colors/positions**: always wrap — `color(255, 255, 255, 255)`,
  `vec2(10.0, 20.0)`.
- **Float32 literals**: `0.2f`, not `static_cast<float32>(0.2)`.
- **Handles**: `create_*`/`load_*` return encrypted `int64`. Pass back
  into draw/bind/destroy. Don't inspect.
- **Casts**: `static_cast<T>(x)` for numeric, `reinterpret_cast<int64>(fn)`
  for function pointers.
- **Strings**: `std::string` for values, `const char*` for read-only params.
  `to_string(n)` for numeric→string. `"a" + "b"` is illegal — use
  `std::string("a") + "b"`.
- **Arrays**: `std::vector<T>` for dynamic, `std::array<T, N>` for fixed.

## Lifecycle

```cpp
int64 main() {
    // setup, register routines
    return 1;  // >0 = stay loaded, <=0 = unload immediately
}
```

### Routines (per-frame callbacks)

```cpp
int64 register_routine(int64 fn_handle, int64 data);
bool unregister_routine(int64 routine_handle);

// Callback shape:
void my_callback(int64 data) { /* data = second arg to register_routine */ }

// Usage:
int64 main() {
    register_routine(reinterpret_cast<int64>(my_callback), 0);
    return 1;
}
```

### Diagnostics

```cpp
void heartbeat();
void dbg_print(const char* s);
void dbg_print(const std::string& s);
void dbg_print_int(int64 x);
void take_int(int64 x);
void take_ptr(int64 p);
```

## Proc API (memory read/write)

`proc_t` is a value-type handle. All addresses are `uint64`.

### Construction

```cpp
proc_t ref_process(uint32 pid);
proc_t ref_process(const char* name);
proc_t ref_process(const std::string& name);
// Verify: proc.alive()
```

### Identity

```cpp
uint64 proc.base_address();
uint64 proc.peb();
uint32 proc.pid();
bool proc.alive();
bool proc.is_valid_address(uint64 addr);
uint64 proc.get_eprocess();  // gated: kernel_rw_access
```

### Read primitives

```cpp
uint8/16/32/64 proc.ru8/ru16/ru32/ru64(uint64 addr);
int8/16/32/64 proc.r8/r16/r32/r64(uint64 addr);
float32 proc.rf32(uint64 addr);
float64 proc.rf64(uint64 addr);
std::string proc.rs(uint64 addr, int32 max_chars);    // UTF-8, cap 8192
std::wstring proc.rws(uint64 addr, int32 max_chars);  // UTF-16, cap 8192
```

### Write primitives (gated: `write_memory`)

```cpp
bool proc.wu8/wu16/wu32/wu64(uint64 addr, uintN v);
bool proc.w8/w16/w32/w64(uint64 addr, intN v);
bool proc.wf32(uint64 addr, float32 v);
bool proc.wf64(uint64 addr, float64 v);
bool proc.ws(uint64 addr, const char* text);
bool proc.ws(uint64 addr, const std::string& text);
bool proc.wws(uint64 addr, const std::wstring& text);
```

### Bulk read/write

```cpp
bool proc.rvm(uint64 addr, pointer out, int64 size);
bool proc.wvm(uint64 addr, pointer buf, int64 size);  // gated
template<typename T> T proc.__rvm(uint64 addr);
template<typename T> bool proc.__wvm(uint64 addr, const T& value);  // gated
bool proc.wvm(uint64 addr, const std::vector<uint8>& bytes);  // gated
```

### SIMD reads/writes

```cpp
std::array<uint8, 16> proc.r128(uint64 addr);
std::array<uint8, 32> proc.r256(uint64 addr);
std::array<uint8, 64> proc.r512(uint64 addr);
bool proc.w128/w256/w512(uint64 addr, const std::vector<uint8>& bytes);  // gated
```

### Modules and exports

```cpp
uint64 proc.get_module_base(const char* name);
uint64 proc.get_module_size(const char* name);
std::vector<module_info_t> proc.get_module_list();
uint64 proc.get_proc_address(uint64 module_base, const char* export_name);
uint64 proc.get_import_rdata_address(uint64 module_base, const char* import_name);
// module_info_t: .name(), .base(), .size()
```

### Pattern scanning

```cpp
uint64 proc.find_code_pattern(uint64 start, uint64 size, const char* sig);
std::vector<uint64> proc.find_all_code_patterns(uint64 start, uint64 size, const char* sig);
// Sig: "48 8B ?? ?? 48 89" — hex bytes, ?? = wildcard
```

### Memory scans (VAD-based, excludes PE images)

```cpp
std::vector<uint64> proc.scan_string(const char* text, bool heap_only);
std::vector<uint64> proc.scan_wstring(const char* text, bool heap_only);
std::vector<uint64> proc.scan_pointer(uint64 target, bool heap_only);
std::vector<uint64> proc.scan_u64(uint64 value, bool heap_only);
std::vector<uint64> proc.scan_u32(uint32 value, bool heap_only);
std::vector<uint64> proc.scan_float(float32 value, bool heap_only);
std::vector<uint64> proc.scan_double(float64 value, bool heap_only);
```

### VAD / virtual query

```cpp
vad_region_t proc.virtual_query(uint64 address);
std::vector<vad_region_t> proc.get_vad_snapshot(bool heap_likely_only);
// vad_region_t: .start(), .size(), .protection(), .heap_likely()
```

### VM alloc/free (gated: `virtual_memory_operations`)

```cpp
uint64 proc.alloc_vm(uint64 size);
bool proc.free_vm(uint64 address);
```

### Threads & pointers

```cpp
std::vector<uint64> proc.get_all_tebs();
std::vector<uint64> proc.read_pointer_array(uint64 base, int64 count, int64 offset_delta);
```

### Permission gates

| Flag | Gates |
|---|---|
| `write_memory` | All write primitives |
| `virtual_memory_operations` | `alloc_vm`, `free_vm` |
| `kernel_rw_access` | `get_eprocess`, kernel address R/W |

## Render API

Namespace: `render::` (or `using namespace render;`).

### 2D primitives

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
void draw_text(const std::string& text, vec2 pos, color c, int64 font, int32 effect, color effect_color, float64 effect_amount);
// effect: 0=none, 1=shadow, 2=outline
```

### Fonts

```cpp
int64 create_font(const char* path, float64 size, bool antialias, bool load_color, const std::vector<uint32>& glyph_ranges);
int64 create_font_mem(const char* label, float64 size, const std::vector<uint8>& buf, bool antialias, bool load_color, const std::vector<uint32>& glyph_ranges);
int64 create_bitmap(const std::vector<uint8>& data);
int64 get_font18(); int64 get_font20(); int64 get_font24(); int64 get_font28();
float64 get_text_width(int64 font, const char* text, int32 maxw, int32 maxh);
float64 get_text_height(int64 font, const char* text, int32 maxw, int32 maxh);
```

### Viewport

```cpp
float64 get_view_width(); float64 get_view_height();
float64 get_view_scale(); float64 get_fps();
```

### Shaders & pipeline

```cpp
int64 create_shader(const char* vs, const char* ps, const char* layout);
int64 create_compute_shader(const char* cs);
int64 create_vertex_buffer(uint32 stride, uint32 max_vertices, bool dynamic);
int64 create_index_buffer(uint32 max_indices, bool use_32bit, bool dynamic);
int64 create_constant_buffer(uint32 size);
int64 create_structured_buffer(uint32 element_size, uint32 element_count, bool cpu_write, bool gpu_write);
int64 create_blend_state(int32 src, int32 dst, int32 op, int32 src_alpha, int32 dst_alpha, int32 op_alpha);
int64 create_sampler(int32 filter, int32 address_u, int32 address_v);
int64 create_depth_stencil_state(bool depth_enable, bool depth_write, int32 compare_func);
int64 create_rasterizer_state(int32 cull_mode, int32 fill_mode, bool scissor_enable);
```

### Textures & meshes

```cpp
int64 create_texture(uint32 w, uint32 h, const std::vector<uint8>& rgba_data);
int64 load_texture(const char* path);
int64 load_texture_mem(const std::vector<uint8>& data);
int64 create_render_target(uint32 w, uint32 h);
int64 create_depth_buffer(uint32 w, uint32 h);
int64 create_mesh_raw(const std::vector<uint8>& vtx, uint32 vtx_count, uint32 stride,
                      const std::vector<uint8>& idx, uint32 idx_count, bool use_32bit);
int64 load_mesh(const char* path);
```

### Custom draw

```cpp
void custom_draw(int64 shader, int64 vb, const std::vector<float32>& vertex_data,
    uint32 vertex_count, int32 topology, int64 blend, int64 sampler, int64 texture,
    int32 tex_slot, int64 cb, const std::vector<float32>& cb_data, int32 cb_slot);
void custom_draw_indexed(/* + index buffer args */);
void draw_mesh(int64 mesh, int64 shader, int32 topology, int64 blend, int64 sampler,
    int64 texture, int32 tex_slot, int64 cb, const std::vector<float32>& cb_data, int32 cb_slot);
int64 dispatch_compute(int64 cs, uint32 x, uint32 y, uint32 z);
// topology: 0=TRI_LIST, 1=TRI_STRIP, 2=LINE_LIST, 3=LINE_STRIP, 4=POINT_LIST
```

### State management

```cpp
int64 custom_set_render_target(int64 rt);
int64 custom_reset_render_target();
int64 custom_bind_rt_as_texture(int64 rt, int32 slot);
int64 custom_restore_state();  // call after custom pipeline before 2D layer
int64 custom_set_viewport(float64 x, float64 y, float64 w, float64 h);
int64 custom_bind_texture(int64 texture, int64 sampler, int32 slot);
int64 custom_bind_constant_buffer(int64 cb, const std::vector<float32>& data, int32 slot, int32 stage);
// stage: 0=VS, 1=PS, 2=CS
void clip_push(vec2 pos, vec2 size);
void clip_pop();
```

## Input API

### Mouse

```cpp
vec2 get_mouse_pos();           // render-window pixels
vec2 get_mouse_pos_desktop();   // desktop pixels
vec2 get_mouse_delta();
bool mouse_movement_received();
bool is_hovered(vec2 pos, vec2 size);
float64 get_scroll_delta();     // positive = up
```

### Keyboard

```cpp
bool key_down(int64 vk);        // currently pressed
bool key_raw_down(int64 vk);    // OS-level
bool key_fired(int64 vk);       // up→down this frame
bool key_toggle(int64 vk);      // caps-lock style
bool key_singlepress(int64 vk); // fired, no modifiers
bool key_prev_down(int64 vk);
key_state_t get_key_state(int64 vk);
std::vector<int32> get_keys_down();
std::string get_recent_key_input();
std::string get_key_name(int64 vk);
```

### VK enum

```cpp
vk::a .. vk::z, vk::k0 .. vk::k9, vk::f1 .. vk::f12
vk::lbutton, vk::rbutton, vk::mbutton, vk::xbutton1, vk::xbutton2
vk::shift, vk::ctrl, vk::alt, vk::escape, vk::space, vk::enter, vk::tab
vk::lshift, vk::rshift, vk::lctrl, vk::rctrl, vk::lalt, vk::ralt
vk::left, vk::up, vk::right, vk::down, vk::insert, vk::delete
vk::home, vk::end, vk::page_up, vk::page_down
vk::numpad0 .. vk::numpad9, vk::multiply, vk::add, vk::subtract, vk::decimal, vk::divide
```

## GUI API

### Sidebar sections & widgets

```cpp
sidebar_section_t create_sidebar_section(const char* name, const char* icon);
void create_sidebar_separator();

// Widgets on section:
label_t section.create_label(const char* text, ui_align align);
button_t section.create_button(const char* label, ui_align align);
checkbox_t section.create_checkbox(const char* label, bool initial);
slider_t section.create_slider(const char* label, float64 initial, float64 minv, float64 maxv, float64 step);
value_input_t section.create_value_input(const char* label, float64 initial, float64 minv, float64 maxv, float64 step);
keybind_t section.create_keybind(const char* label);
progress_bar_t section.create_progress_bar(const char* label, float64 initial, float64 minv, float64 maxv, bool show_pct);
text_input_t section.create_text_input(const char* label, const char* initial, int64 max_lines);
text_editor_t section.create_text_editor(const char* label, const char* initial, int64 visible_lines, const char* lexer);
colorpicker_t section.create_colorpicker(const char* label, color initial);
range_slider_t section.create_range_slider(const char* label, float64 minv, float64 maxv, float64 lo, float64 hi, float64 step);
// + options, multi_options, dropdown, multi_dropdown, list, tabs, table (free functions with vector args)
```

### Widget operations

```cpp
void widget.set_active(bool active);
void widget.set_tooltip(const char* s);
void widget.on_change(int64 fn_handle);  // void cb(int64 widget_handle)
bool checkbox.get(); void checkbox.set(bool v);
float64 slider.get(); void slider.set(float64 v);
int64 dropdown.get(); void dropdown.set(int64 i);
color colorpicker.get(); void colorpicker.set(color c);
```

### Frames & layers

```cpp
frame_t create_frame(const char* name, vec2 pos, vec2 size, layer_t layer);
frame_t create_draggable_frame(const char* name, vec2 pos, vec2 size, layer_t layer);
frame_t create_popup(const char* name, vec2 pos, vec2 size, layer_t layer);
layer_t create_layer(const char* name, bool input_passthrough, bool force_topmost);
layer_t get_default_layer();
```

### Menus, file picker, theme, toasts

```cpp
menu_t create_menu();
void menu.add_item(const char* label, int64 on_click_cb, const char* shortcut, const char* icon);
file_picker_t create_file_picker(const char* title, const char* start_path, const char* filter_ext, bool folder_mode);
bool is_dark_theme(); void set_dark_theme(bool dark);
void show_toast(toast_kind kind, const char* title, const char* msg);
// toast_kind: info, success, warning, error
```

## Win API

```cpp
int64 find_window(const char* title);
int64 find_window(const char* title, const char* class_name);
std::vector<window_info_t> get_all_hwnds();
int64 get_window_width/height(int64 hwnd);
vec2 get_window_pos/size(int64 hwnd);
bool is_foreground_window(int64 hwnd);
bool set_foreground_window(int64 hwnd);
bool post_message(int64 hwnd, int64 msg, int64 wparam, int64 lparam);
// Clipboard
bool copy_to_clipboard(const char* text);
std::string copy_from_clipboard();
// Keyboard send
void win_key_down/up/press(int64 vk, ...);
bool send_char(int64 hwnd, const char* text);
// Mouse send
void mouse_move(int64 x, int64 y);
void mouse_move_relative(int64 dx, int64 dy);
void mouse_left_click(); void mouse_right_click(); void mouse_middle_click();
void mouse_scroll(int64 amount);
```

## Net API (gated: `network_access`)

### HTTP

```cpp
http_response_t http_get(const char* url, int64 timeout_ms);
http_response_t http_post(const char* url, const char* content_type, const char* body, int64 timeout_ms);
// + overloads with headers map
// http_response_t: .status(), .ok(), body(r) free fn
```

### WebSocket

```cpp
ws_t ws_connect(const char* url, int64 timeout_ms);
bool send_text(ws_t ws, const char* msg);
bool send_binary(ws_t ws, const std::vector<uint8>& data);
ws_message_t ws.recv();   // blocking
ws_message_t ws.poll();   // non-blocking
void ws.close(int64 code);
```

### UDP

```cpp
udp_t udp_create();
bool bind(udp_t u, const char* addr, int64 port);
bool send_to(udp_t u, const std::vector<uint8>& data, const char* addr, int64 port);
std::vector<uint8> recv(udp_t u, int64 timeout_ms);
```

## Unicorn API (CPU emulation)

```cpp
cpu_t cpu_create();
cpu_t cpu_create_process(proc_t proc, bool allow_writes);
bool cpu.mem_map(int64 addr, int64 size, uc_prot perms);
bool mem_write(cpu_t cpu, int64 addr, const std::vector<uint8>& bytes);
std::vector<uint8> mem_read(cpu_t cpu, int64 addr, int64 size);
bool cpu.reg_write64(uc_reg reg, int64 value);
int64 cpu.reg_read64(uc_reg reg);
int64 cpu.start(int64 begin, int64 end, int64 timeout, int64 count);
void cpu.emu_stop();
bool cpu.hook_add(uc_hook hook_kind, int64 fn_handle);
// Hook callback: int64 cb(int64 addr) — return 0 to stop, non-zero to continue
// uc_reg: rax..r15, rip, eflags, xmm0..15, ymm0..15, cs/ds/es/fs/gs/ss
// uc_prot: none, read, write, exec, rw, rx, rwx, all
```

## Zydis API (x86/x64 assembler/disassembler)

```cpp
// Encode single instruction
zydis_req_t r;
r.set_mnemonic(zydis_mnemonic_from_string("mov"));
r.set_operand_reg(0, zydis_register_from_string("rax"));
r.set_operand_imm(1, 0x42);
std::vector<uint8> bytes = zydis_encode(r);

// Builder for sequences
zydis_builder_t b;
b.set_machine_mode(zydis_machine_mode::long_64);
b.set_base_address(addr);
b.push(req);
b.push_byte(0xCC);  // int3
b.push_ret();
std::vector<uint8> shellcode = zydis_builder_build(b);

// Disassemble
std::vector<std::string> zydis_disasm(const std::vector<uint8>& bytes, int64 runtime_rip);

// NOP fill
std::vector<uint8> zydis_nop_fill(int64 length);
```

## Sound API

```cpp
sound_t load_sound(const char* relative_path);  // relative to <my_games>/
sound_inst_t sound.play(float64 volume, float64 pan, bool loop);
bool sound_inst.is_playing();
void sound_inst.stop();
void sound_inst.set_volume(float64 v);
void stop_all_sounds();
```

## CPU API

```cpp
std::string cpu_vendor(); std::string cpu_brand();
int64 rdtsc(); int64 perf_time(); int64 perf_frequency();
int64 get_tickcount64();
bool set_thread_priority(thread_priority p);
// thread_priority: lowest, below_normal, normal, above_normal, highest
```

## Common patterns

### ESP overlay (Enma)

```cpp
proc_t g_proc;
int64 g_font;

void on_render(int64 data) {
    uint64 base = g_proc.base_address();
    uint64 entity_list = g_proc.ru64(base + OFFSET_ENTITY_LIST);

    for (int i = 1; i < 64; i++) {
        uint64 entity = g_proc.ru64(entity_list + (i * 0x78));
        if (!entity) continue;

        int32 health = g_proc.r32(entity + OFFSET_HEALTH);
        if (health <= 0) continue;

        float32 pos[3];
        g_proc.rvm(entity + OFFSET_ORIGIN, pos, 12);
        vec3 world = vec3(pos[0], pos[1], pos[2]);

        // world-to-screen transform here
        // draw_text, draw_rect, etc.
    }
}

int64 main() {
    g_proc = ref_process("game.exe");
    if (!g_proc.alive()) return 0;
    g_font = get_font20();
    register_routine(reinterpret_cast<int64>(on_render), 0);
    return 1;
}
```

### Memory patch

```cpp
proc_t p = ref_process("target.exe");
uint64 base = p.get_module_base("game.dll");
uint64 addr = p.find_code_pattern(base, p.get_module_size("game.dll"),
    "48 89 5C 24 ?? 57 48 83 EC 20");
if (addr) {
    std::vector<uint8> patch = {0x90, 0x90, 0x90, 0x90, 0x90};  // NOP
    p.wvm(addr, patch);
}
```

### GUI menu

```cpp
sidebar_section_t g_sec;
checkbox_t g_esp;
slider_t g_fov;

void on_render(int64 data) {
    if (g_esp.get()) { /* draw ESP */ }
    float64 fov = g_fov.get();
}

int64 main() {
    g_sec = create_sidebar_section("Cheats", "crosshair");
    g_esp = g_sec.create_checkbox("Enable ESP", true);
    g_fov = g_sec.create_slider("FOV", 90.0, 1.0, 180.0, 1.0);
    register_routine(reinterpret_cast<int64>(on_render), 0);
    return 1;
}
```

## AngelScript API (deprecated)

Legacy API — use only for maintaining existing scripts. All new work in Enma.

### Key differences from Enma

- Entry: `int main()` (return >0 = persistent, <=0 = unload)
- Callbacks: `register_callback(fn, every_ms, data_index)` — not routines
- GUI: subtab/panel model, not sidebar sections
- Arrays: `array<T>` instead of `std::vector<T>`
- Strings: native `string` type
- Proc: requires `deref()` for cleanup, has `read_struct` with dictionary descriptors
- Render: raw float params, not vec2/color types
- Has engine-specific API (Unreal, Fortnite, Rust helpers)

### Lifecycle

```cpp
int main() {
    register_callback(on_tick, 16, 0);  // every 16ms
    return 1;  // >0 = stay loaded
}
void on_unload() { /* cleanup */ }
void on_tick(int callback_id, int data_index) { /* per-frame */ }
// register_callback(fn, every_ms, data_index, render_on_top=false) → callback ID
// unregister_callback(id)
```

### Logging

```cpp
void log(const string& message);
void log_error(const string& message);
void log_console(const string& message);
void log_console_error(const string& message);
string get_username();
```

### Proc API differences

```cpp
proc_t ref_process(uint pid);
proc_t ref_process(const string& name);
void proc_t::deref();  // MUST call when done (also if alive() == false)

// Struct reads via dictionary descriptor:
dictionary desc = {{"health", "i32"}, {"pos_x", "f32"}, {"pos_y", "f32"}, {"pos_z", "f32"}};
dictionary result;
proc.read_struct(addr, result, desc);
int64 hp = cast<int64>(result["health"]);

// Module lookup (different signature):
uint64 base, size;
proc.get_module("game.dll", base, size);

// VAD snapshot returns array<dictionary@>:
array<dictionary@>@ vad = proc.get_vad_snapshot(true);
```

### Render API differences

```cpp
// Raw float params instead of vec2/color:
void draw_rect(float x, float y, float w, float h,
    uint8 r, uint8 g, uint8 b, uint8 a,
    float thickness, float rounding, uint8 rounding_flags);
void draw_text(const string& text, float x, float y,
    uint8 r, uint8 g, uint8 b, uint8 a,
    uint64 font, int effect,
    uint8 er, uint8 eg, uint8 eb, uint8 ea, float effect_amount);
// effect: TE_NONE=0, TE_OUTLINE=1, TE_SHADOW=2, TE_GLOW=3
// rounding: RR_TOP_LEFT, RR_TOP_RIGHT, RR_BOTTOM_LEFT, RR_BOTTOM_RIGHT
void get_view(float& w, float& h);
void get_text_size(uint64 font, const string& text, int maxw, int maxh, float& w, float& h);
```

### GUI API (subtab/panel model)

```cpp
subtab_t create_subtab(int parent_tab, const string& name);
panel_t panel = subtab.add_panel("Panel Name", false);

// Widgets on panel:
checkbox_t cb = panel.add_checkbox("ESP", true);
keybind_t kb = panel.add_keybind("Toggle", 0x45, "toggle");  // E key
color_picker_t cp = panel.add_color("Color", {255, 0, 0, 255});
slider_double_t sd = panel.add_slider_double("FOV", "", 90.0, 1.0, 180.0, 1.0);
slider_int_t si = panel.add_slider_int("Smooth", "", 5, 1, 20, 1);
input_t inp = panel.add_input("Name", "");
single_select_t ss = panel.add_single_select("Bone", {"Head", "Neck", "Chest"}, 0, false);
multi_select_t ms = panel.add_multi_select("Features", opts, true);
list_t lst = panel.add_list("Players", members);
button_t btn = panel.add_button("Execute", callback_fn);

// Find existing elements:
checkbox_t find_checkbox(int tab, const string& subtab, const string& panel, const string& name);
// + find_slider_double, find_slider_int, find_input, find_keybind, etc.

// Config:
string construct_config();
void apply_config(const string& cfg);
```

### Engine-specific API (AngelScript only)

```cpp
// Unreal Engine:
bool unreal_read_tarray(proc_t& proc, uint64 tarray_addr, array<uint64>& result, uint max_count = 4096);
bool unreal_read_minimal_view_info(proc_t& proc, uint64 pov_addr, vector3& location, vector3& rotation, double& fov);
bool unreal_world_to_screen(const vector3& world_pos, const vector3& cam_location,
    const vector3& cam_rotation, double fov_deg, vector2& screen_pos);

// Generic world-to-screen:
bool world_to_screen_rowmajor(const vector3& world_pos, const matrix4x4& view_matrix,
    vector2& screen_pos, const vector2& viewport = vector2(0, 0));
bool world_to_screen_transposed(const vector3& world_pos, const matrix4x4& view_matrix,
    vector2& screen_pos, const vector2& viewport = vector2(0, 0));

// Game-specific:
string fortnite_get_player_name(proc_t& proc, uint64 addr);
vector3 rust_get_transform_position(proc_t& proc, uint64 addr);
```

### ESP overlay (AngelScript)

```cpp
proc_t g_proc;
uint64 g_font;

void on_tick(int id, int data) {
    float vw, vh; get_view(vw, vh);
    uint64 base = g_proc.base_address();
    uint64 entity_list = g_proc.ru64(base + OFFSET_ENTITY_LIST);

    for (int i = 1; i < 64; i++) {
        uint64 entity = g_proc.ru64(entity_list + (i * 0x78));
        if (entity == 0) continue;
        int32 health = g_proc.r32(entity + OFFSET_HEALTH);
        if (health <= 0) continue;
        // read position, world-to-screen, draw
    }
}

int main() {
    g_proc = ref_process("game.exe");
    if (!g_proc.alive()) return 0;
    g_font = get_font20();
    register_callback(on_tick, 16, 0);
    return 1;
}

void on_unload() {
    g_proc.deref();
}
```
