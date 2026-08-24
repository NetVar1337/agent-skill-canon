> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/gui-api.md).

# GUI API

All GUI natives are auto-registered into every loaded script.

The API is in two parts:

* **Part 1 — sidebar sections + widgets.** A `sidebar_section_t` is a select-button in the host sidebar plus a content panel auto-attached to the main frame. The panel renders only when the section's button is selected. Widgets are created on the section directly.
* **Part 2 — frames, layers, custom widgets, menus, file pickers.** Lower-level primitives for floating windows and custom drawing.

All GUI handles are `int64`-backed. The script doesn't own the underlying resources — calling a destructor on a handle is a noop. At script unload, every handle the script created gets cleaned up automatically.

## Method vs free function

Handles registered via `type_builder` (`sidebar_section_t`, `text_input_t`, `list_t`, `file_picker_t`, `frame_t`, etc.) expose most operations as methods (`w.get_value()`, `frame.set_pos(...)`). Two shapes are exposed as **free functions taking the handle as the first argument** instead:

* Methods that would **return `std::string`** — `get(text_input_t)`, `get(text_editor_t)`, `get(inline_text_input_t)`, `get_selected(file_picker_t)`.
* Methods / factories that would **take a `std::vector`** — `set_items(list_t, ...)`, `add_row(table_t, ...)`, plus every `sidebar_section_t` builder whose items argument is a vector: `create_options`, `create_multi_options`, `create_dropdown`, `create_multi_dropdown`, `create_list`, `create_tabs`, `create_table`.

The reason is mechanical: methods on `type_builder`-registered handles are declared C++-side and can't be extended from the `.em` prelude, so the adapter that owns the `std::string` or `std::vector` shape lives as a free function alongside them.

String-taking global factories (`create_sidebar_section`, `create_frame`, `create_layer`, `show_toast`, `create_widget`, `create_file_picker`, etc.) each ship both `const char*` and `const std::string&` overloads.

## Sidebar sections

```cpp
sidebar_section_t create_sidebar_section(const char* name, const char* icon);
sidebar_section_t create_sidebar_section(const std::string& name, const std::string& icon);
void              create_sidebar_separator();
```

`name` renders as the sidebar label. `icon` accepts a codicon string (e.g. `"\xEE\xAC\xA3"` for file-code U+EB23) or `""` for no icon.

Each section is a radio-style sidebar entry: clicking one auto-deselects siblings and shows that section's panel.

```cpp
void section.set_active(bool active);   // toggle selection programmatically
```

### Widget builders on `sidebar_section_t`

Every widget builder returns a typed handle. Each is also `int64`-backed; pass to other natives via the typed name.

Builders whose argument list is `std::string` label + primitive scalars stay as methods:

```cpp
label_t              section.create_label(const char* text, ui_align align);
void                 section.create_separator();
button_t             section.create_button(const char* label, ui_align align);
checkbox_t           section.create_checkbox(const char* label, bool initial);
slider_t             section.create_slider(const char* label, float64 initial, float64 minv, float64 maxv, float64 step);
slider_icon_t        section.create_slider_icon(const char* icon, float64 initial, float64 minv, float64 maxv, float64 step);
value_input_t        section.create_value_input(const char* label, float64 initial, float64 minv, float64 maxv, float64 step);
inline_button_t      section.create_inline_button(const char* label, float64 width, const char* icon);
inline_text_input_t  section.create_inline_text_input(const char* initial, float64 width, const char* placeholder);
keybind_t            section.create_keybind(const char* label);
progress_bar_t       section.create_progress_bar(const char* label, float64 initial, float64 minv, float64 maxv, bool show_pct);
spinner_t            section.create_spinner(const char* label);
range_slider_t       section.create_range_slider(const char* label, float64 minv, float64 maxv,
                                                  float64 lo, float64 hi, float64 step);
text_input_t         section.create_text_input(const char* label, const char* initial, int64 max_lines);
text_editor_t        section.create_text_editor(const char* label, const char* initial, int64 visible_lines, const char* lexer);
colorpicker_t        section.create_colorpicker(const char* label, color initial);
```

Builders whose argument list carries a `std::vector<std::string>` are free functions taking the section as first arg (both `const char*` and `const std::string&` label overloads exposed):

```cpp
options_t         create_options       (sidebar_section_t s, const char* label, const std::vector<std::string>& items, int64 selected);
multi_options_t   create_multi_options (sidebar_section_t s, const char* label, const std::vector<std::string>& items, int64 selected_mask);
dropdown_t        create_dropdown      (sidebar_section_t s, const char* label, const std::vector<std::string>& items, int64 selected);
multi_dropdown_t  create_multi_dropdown(sidebar_section_t s, const char* label, const std::vector<std::string>& items, int64 selected_mask);
list_t            create_list          (sidebar_section_t s, const char* label,
                                         const std::vector<std::string>& info1, const std::vector<std::string>& info2,
                                         bool selectable, int64 selected,
                                         int64 visible_rows, bool filterable);
tabs_t            create_tabs          (sidebar_section_t s, const std::vector<std::string>& items, int64 selected);
table_t           create_table         (sidebar_section_t s, const char* label,
                                         const std::vector<std::string>& col_names,
                                         const std::vector<float64>& col_widths,
                                         int64 visible_rows);
```

## Common widget operations

Every widget type (except `text_editor_t`, which has no `set_active`) supports:

```cpp
void widget.set_active(bool active);
void widget.set_tooltip(const char* s);
void widget.on_change(int64 fn_handle);   // closure: void cb(int64 widget_handle)
```

`on_change` fires on `CALLBACK_VALUE_CHANGED` — which means click for buttons, value mutation for sliders / checkboxes / dropdowns / etc. Pass the closure via `reinterpret_cast<int64>(my_callback)`.

## Per-widget typed get / set

```cpp
// label
void label.set_text(const char* s);

// button
void button.attach_to(button_t other);   // group buttons into one row

// checkbox
bool checkbox.get();
void checkbox.set(bool v);

// slider, slider_icon, value_input
float64 X.get();
void    X.set(float64 v);

// options, dropdown, tabs
int64 X.get();
void  X.set(int64 i);

// multi_options, multi_dropdown
int64 X.get_mask();
void  X.set_mask(int64 m);

// list
int64 list.get_selected();
void  list.set_selected(int64 i);
void  set_items(list_t w, const std::vector<std::string>& info1,
                          const std::vector<std::string>& info2);  // free function
int64 list.size();

// inline_text_input, text_input, text_editor — string get/set are free functions
std::string get(inline_text_input_t w);
std::string get(text_input_t w);
std::string get(text_editor_t w);
void set(inline_text_input_t w, const char* s);
void set(inline_text_input_t w, const std::string& s);
void set(text_input_t w, const char* s);
void set(text_input_t w, const std::string& s);
void set(text_editor_t w, const char* s);
void set(text_editor_t w, const std::string& s);

// keybind
void  keybind.bind(int64 vk, bool ctrl, bool shift, bool alt, keybind_mode mode);
bool  keybind.is_active();      // true when any binding is currently active per its mode; poll to react to activation
int64 keybind.binding_count();  // number of bindings on this row

// progress_bar
void progress_bar.set(float64 v);

// range_slider — split lo/hi getters since there's no natural pair type
float64 range_slider.get_lo();
float64 range_slider.get_hi();
void    range_slider.set(float64 lo, float64 hi);

// table
void  add_row(table_t w, const std::vector<std::string>& cells);   // free function
void  table.clear();
int64 table.size();

// colorpicker — uses the registered `color` type
void  colorpicker.attach_to(colorpicker_t other);
color colorpicker.get();
void  colorpicker.set(color c);
```

## Frames (Part 2)

`frame_t` wraps any of four host frame kinds — distinguished by which factory you call. Each factory ships `const char*` and `const std::string&` overloads for the `name` argument:

```cpp
frame_t create_frame(const char* name, vec2 pos, vec2 size, layer_t layer);
frame_t create_frame(const std::string& name, vec2 pos, vec2 size, layer_t layer);
//   raw frame, no chrome. Pass 0 for layer to use the default layer.
frame_t create_default_frame(const char* name, vec2 pos, vec2 size, layer_t layer);
frame_t create_default_frame(const std::string& name, vec2 pos, vec2 size, layer_t layer);
//   frame with title bar / logo / drag chrome.
frame_t create_draggable_frame(const char* name, vec2 pos, vec2 size, layer_t layer);
frame_t create_draggable_frame(const std::string& name, vec2 pos, vec2 size, layer_t layer);
frame_t create_popup(const char* name, vec2 pos, vec2 size, layer_t layer);
frame_t create_popup(const std::string& name, vec2 pos, vec2 size, layer_t layer);
```

```cpp
void    frame.set_pos(vec2 pos);
void    frame.set_size(vec2 size);
vec2    frame.get_pos();
vec2    frame.get_size();
void    frame.set_visible(bool v);
bool    frame.is_visible();
void    frame.set_anchors(int64 mask);   // ui_anchor::* OR'd
void    frame.attach(frame_t parent);
void    frame.set_float(int64 hash, float64 v);   // widget_attr::* keys
void    frame.install_hook(int64 hook_id, int64 fn_handle);
void    frame.remove_hook(int64 hook_id);
void    frame.set_focused();
frame_t get_focused_frame();
bool    ui_is_focused();
```

## Layers

A layer is a z-stacked frame group; frames in higher layers paint over lower ones.

```cpp
layer_t create_layer(const char* name, bool input_passthrough, bool force_topmost);
layer_t create_layer(const std::string& name, bool input_passthrough, bool force_topmost);
layer_t get_default_layer();
int64   layer_count();

void  layer.promote_to_top();
void  layer.set_visible(bool v);
int64 layer.frame_count();
bool  layer.register_in_taskbar(const char* label);
void  layer.unregister_from_taskbar();
```

`register_in_taskbar` exposes the layer as a top-of-viewport taskbar entry that toggles every frame in the layer on click. Refused for the built-in default layer and for labels that collide with existing entries (Main / IDE / Chat / Logger / etc.) — first-come wins, and host registrations always run before script load.

## Custom widgets on a script-owned frame

Drop a `widget_t` into one of your `frame_t`s for a custom render callback that fires every tick during the frame's render pass:

```cpp
widget_t create_widget(frame_t parent, const char* name, int64 execute_cb_handle, bool consume_input);
widget_t create_widget(frame_t parent, const std::string& name, int64 execute_cb_handle, bool consume_input);
//   execute_cb shape: void cb(int64 widget_handle) — called every tick.

void widget.set_pos(vec2 pos);
void widget.set_size(vec2 size);
void widget.set_active(bool v);
void widget.set_tooltip(const char* s);
void widget.set_float(int64 hash, float64 v);
void widget.set_anchors(int64 mask);
void widget.install_hook(int64 hook_id, int64 fn_handle);
void widget.remove_hook(int64 hook_id);
```

## Menus

A `menu_t` is a context menu — a popup list of items. Attach it to any widget to make right-click on that widget open it.

```cpp
menu_t create_menu();
void   menu.add_item(const char* label, int64 on_click_cb, const char* shortcut, const char* icon);
//   shortcut: visible label only (e.g. "Ctrl+C"); not bound by add_item itself.
//   icon: codicon string or "" for none.
//   on_click_cb shape: void cb(int64 menu_user_data).
void   menu.add_separator();
```

`menu_t.attach_to_widget` is split per widget type because enma's overloading is by arity, not by parameter type. Use the variant matching the widget you're attaching to:

```cpp
void menu.attach_to_widget(widget_t target);
void menu.attach_to_button(button_t target);
void menu.attach_to_label(label_t target);
void menu.attach_to_checkbox(checkbox_t target);
void menu.attach_to_slider(slider_t target);
void menu.attach_to_slider_icon(slider_icon_t target);
void menu.attach_to_value_input(value_input_t target);
void menu.attach_to_options(options_t target);
void menu.attach_to_multi_options(multi_options_t target);
void menu.attach_to_dropdown(dropdown_t target);
void menu.attach_to_multi_dropdown(multi_dropdown_t target);
void menu.attach_to_list(list_t target);
void menu.attach_to_inline_button(inline_button_t target);
void menu.attach_to_inline_text_input(inline_text_input_t target);
void menu.attach_to_tabs(tabs_t target);
void menu.attach_to_keybind(keybind_t target);
void menu.attach_to_progress_bar(progress_bar_t target);
void menu.attach_to_spinner(spinner_t target);
void menu.attach_to_range_slider(range_slider_t target);
void menu.attach_to_table(table_t target);
void menu.attach_to_text_input(text_input_t target);
void menu.attach_to_text_editor(text_editor_t target);
void menu.attach_to_colorpicker(colorpicker_t target);
```

## File picker

```cpp
file_picker_t create_file_picker(const char* title, const char* start_path,
                                  const char* filter_extension, bool folder_mode);
file_picker_t create_file_picker(const std::string& title, const std::string& start_path,
                                  const std::string& filter_extension, bool folder_mode);
void         picker.open();
void         picker.close();
std::string  get_selected(file_picker_t p);   // free function; returns current navigation path
```

## Theme

```cpp
bool  is_dark_theme();
void  set_dark_theme(bool dark);
color get_theme_color(int64 color_hash);
void  set_theme_color(int64 color_hash, color c);
```

`color_hash` is a value from the `ui_color` enum.

## Toasts and queries

```cpp
void show_toast(toast_kind kind, const char* title, const char* msg);
void show_toast(toast_kind kind, const std::string& title, const std::string& msg);
bool gui_active();
```

## Enums

All exposed without needing a header import:

| Enum           | Values                                                                                                                     |
| -------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `ui_anchor`    | `none`, `left`, `right`, `top`, `bottom`, `all`                                                                            |
| `ui_edge`      | `left`, `top`, `right`, `bottom`                                                                                           |
| `ui_align`     | `left`, `center`, `right`                                                                                                  |
| `ui_layout`    | `none`, `vertical`, `horizontal`                                                                                           |
| `ui_hook`      | 33 hook IDs incl. `pre_execute`, `post_execute`, `clicked`, `right_clicked`, `should_render`, `editor_*`, `widget_execute` |
| `ui_callback`  | `value_changed`, `item_activated`                                                                                          |
| `widget_attr`  | well-known position / size / scroll / rounding hashes                                                                      |
| `ui_color`     | 35 color hashes (`bg`, `text`, `accent`, `frame_bg`, `sidebar_bg`, `element_button_bg`, etc.)                              |
| `keybind_mode` | `off`, `on`, `single`, `toggle`, `always_on`                                                                               |
| `toast_kind`   | `info`, `success`, `warning`, `error`                                                                                      |

## Lifecycle and cleanup

GUI resources you create (sections, frames, layers, custom widgets, menus, file pickers) are tracked per-script and torn down automatically at script unload. You don't need to destroy them manually — the destructor on each handle is a noop.

Caveats:

* **Sidebar slots persist.** Sections you create occupy a sidebar slot for the lifetime of the host. Hot-reloading scripts that create many sections will leave stale slots in the sidebar.
* **Separators** stay visible after unload (no remove path).

Hook callbacks fire on the UI thread. Heavy work inside a `pre_execute` or `on_change` (running every tick on every widget) shows up in profile — keep them lightweight.

## Example

A comprehensive script exercising most of the widget builders, `on_change` plumbing through typed handles, an attached-button row, an attached colorpicker chain, a context menu, a tabs widget with per-tab content, and a routine polling `keybind.is_active()`.

```cpp
sidebar_section_t g_sec;
menu_t            g_menu;

keybind_t g_kb_aim;
keybind_t g_kb_esp;
bool g_aim_was_active = false;
bool g_esp_was_active = false;
int64 g_kb_routine = 0;

tabs_t       g_tabs;
label_t      g_t0_label; slider_t  g_t0_slider;
label_t      g_t1_label; checkbox_t g_t1_check;

void on_apply(int64 _)  { print_console("[demo] Apply clicked"); }
void on_cancel(int64 _) { print_console("[demo] Cancel clicked"); }

void on_volume(int64 self) {
    slider_t s = static_cast<slider_t>(self);
    print_console("Volume -> " + to_string(s.get()));
}

void on_features(int64 self) {
    multi_options_t mo = static_cast<multi_options_t>(self);
    print_console("features mask -> " + to_string(mo.get_mask()));
}

void on_accent(int64 self) {
    colorpicker_t cp = static_cast<colorpicker_t>(self);
    color c = cp.get();
    print_console("accent -> " +
        to_string(c.r()) + "," + to_string(c.g()) + "," +
        to_string(c.b()) + "," + to_string(c.a()));
}

void on_view_tabs(int64 self) {
    tabs_t t = static_cast<tabs_t>(self);
    int64 sel = t.get();
    // selected tab's widgets active, others inactive
    g_t0_label.set_active(sel == 0);
    g_t0_slider.set_active(sel == 0);
    g_t1_label.set_active(sel == 1);
    g_t1_check.set_active(sel == 1);
}

void on_kb_aim_changed(int64 self) {
    keybind_t kb = static_cast<keybind_t>(self);
    print_console("aim bindings -> " + to_string(kb.binding_count()));
}

// keybinds don't fire a callback on hardware-key activation —
// poll keybind.is_active() to react.
void kb_poll_routine(int64 _data) {
    bool now = g_kb_aim.is_active();
    if (now != g_aim_was_active) {
        print_console(now ? "Aim ACTIVE" : "Aim inactive");
        g_aim_was_active = now;
    }
    bool esp = g_kb_esp.is_active();
    if (esp != g_esp_was_active) {
        print_console(esp ? "ESP ACTIVE" : "ESP inactive");
        g_esp_was_active = esp;
    }
}

int32 main() {
    g_sec = create_sidebar_section("demo", "");

    g_sec.create_label("Settings panel demo.", ui_align::left);
    g_sec.create_separator();

    // Attached button row — children share the primary's row.
    button_t apply  = g_sec.create_button("Apply",  ui_align::right);
    button_t cancel = g_sec.create_button("Cancel", ui_align::right);
    cancel.attach_to(apply);
    apply.on_change(reinterpret_cast<int64>(on_apply));
    cancel.on_change(reinterpret_cast<int64>(on_cancel));

    g_sec.create_separator();
    g_sec.create_checkbox("Notifications", true);

    slider_t vol = g_sec.create_slider("Volume", 0.6, 0.0, 1.0, 0.0);
    vol.on_change(reinterpret_cast<int64>(on_volume));
    g_sec.create_value_input("Port", 8080.0, 1.0, 65535.0, 1.0);

    // Codicon UTF-8 byte sequence — `\xHH` lexer escape required.
    g_sec.create_slider_icon("\xEE\xA9\xB0", 0.75, 0.0, 1.0, 0.0);

    g_sec.create_separator();
    std::vector<std::string> features;
    features.push_back("Autosave"); features.push_back("Spell check");
    features.push_back("Auto-complete"); features.push_back("Line numbers");
    multi_options_t mo = create_multi_options(g_sec, "Editor features", features, 13);
    mo.on_change(reinterpret_cast<int64>(on_features));

    g_sec.create_separator();
    std::vector<std::string> tab_items;
    tab_items.push_back("Overview"); tab_items.push_back("Logs");
    g_tabs = create_tabs(g_sec, tab_items, 0);
    g_tabs.on_change(reinterpret_cast<int64>(on_view_tabs));

    g_t0_label  = g_sec.create_label("Overview content.", ui_align::left);
    g_t0_slider = g_sec.create_slider("FOV", 90.0, 60.0, 120.0, 1.0);
    g_t1_label  = g_sec.create_label("Logs content.", ui_align::left);
    g_t1_check  = g_sec.create_checkbox("Verbose logging", false);
    g_t1_label.set_active(false);
    g_t1_check.set_active(false);

    g_sec.create_separator();
    g_kb_aim = g_sec.create_keybind("Aimbot");
    g_kb_aim.bind(0x01, false, false, false, keybind_mode::on);    // VK_LBUTTON
    g_kb_aim.on_change(reinterpret_cast<int64>(on_kb_aim_changed));

    g_kb_esp = g_sec.create_keybind("ESP toggle");
    g_kb_esp.bind(0x45, false, false, false, keybind_mode::toggle); // 'E'

    g_sec.create_separator();
    colorpicker_t accent = g_sec.create_colorpicker("Accent", color(180, 180, 180, 255));
    accent.on_change(reinterpret_cast<int64>(on_accent));

    // Attached colorpicker chain — children render as swatches in the parent's popup.
    colorpicker_t theme_cp  = g_sec.create_colorpicker("Theme",     color(120, 120, 120, 255));
    colorpicker_t primary   = g_sec.create_colorpicker("Primary",   color( 80,  80,  80, 255));
    colorpicker_t secondary = g_sec.create_colorpicker("Secondary", color(200, 200, 200, 255));
    primary.attach_to(theme_cp);
    secondary.attach_to(theme_cp);

    // Context menu attached to a button — opens on the host's right-click path.
    button_t actions = g_sec.create_button("Actions", ui_align::center);
    g_menu = create_menu();
    g_menu.add_item("Reset",     reinterpret_cast<int64>(on_apply),  "Ctrl+R", "");
    g_menu.add_separator();
    g_menu.add_item("About...",  reinterpret_cast<int64>(on_cancel), "",       "");
    g_menu.attach_to_button(actions);

    g_kb_routine = register_routine(reinterpret_cast<int64>(kb_poll_routine), 0);
    return 1;   // keep loaded so the section stays interactive
}
```
