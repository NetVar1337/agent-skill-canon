> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/input-api.md).

# Input API

All input natives are auto-registered into every loaded script.

Read-only complement to [Win API](/perception/win-api.md) — Win API **sends** input, this **reads** state. Pollable per-frame from `my_draw` or routine callbacks.

Virtual-key codes follow Win32 VK\_\* convention. The `vk` enum bundles the common ones so no `#include` is needed.

## Mouse

```cpp
vec2 get_mouse_pos();             // render-window pixels
vec2 get_mouse_pos_desktop();     // desktop pixels (full screen)
vec2 get_mouse_delta();           // raw movement this frame
vec2 get_mouse_delta_desktop();   // desktop-space delta this frame

bool    mouse_movement_received();          // any movement this frame
bool    is_hovered(vec2 pos, vec2 size);    // mouse inside rect at pos with given size
float64 get_scroll_delta();                 // wheel ticks; positive = up
```

## Keyboard — single-flag queries

| Flag          | Meaning                                      |
| ------------- | -------------------------------------------- |
| `down`        | currently pressed (host-debounced)           |
| `raw_down`    | OS-level pressed state                       |
| `fired`       | up→down transition this frame                |
| `toggle`      | caps-lock-style toggle (flips on each press) |
| `singlepress` | fired but suppressed when modifiers are held |
| `prev_down`   | down state from previous frame               |

```cpp
bool key_down       (int64 vk);
bool key_raw_down   (int64 vk);
bool key_fired      (int64 vk);
bool key_toggle     (int64 vk);
bool key_singlepress(int64 vk);
bool key_prev_down  (int64 vk);
```

## Bulk / ergonomic queries

```cpp
key_state_t         get_key_state(int64 vk);    // atomic snapshot of all 6 flags
std::vector<int32>  get_keys_down();            // virtual-key codes currently pressed
std::string         get_recent_key_input();     // buffered text input (UTF-8) since last poll
std::string         get_key_name(int64 vk);     // localized key name (e.g. "F1", "Left Arrow"); empty on invalid
```

### `key_state_t`

```cpp
bool ks.raw_down();      // OS-level pressed state
bool ks.down();          // host-debounced pressed state
bool ks.fired();         // up->down this frame (one-shot)
bool ks.toggle();        // caps-lock-style toggle (flips on each press)
bool ks.singlepress();   // fired but suppressed if modifiers held
bool ks.prev_down();     // down state from previous frame
```

Use `get_key_state(vk)` when you need consistency across multiple flag reads in the same frame — the per-flag fns above each take a separate lock and can race.

## `vk` enum — common Win32 virtual keys

```cpp
vk::backspace  vk::tab       vk::enter     vk::shift     vk::ctrl     vk::alt
vk::pause      vk::caps_lock vk::escape    vk::space
vk::page_up    vk::page_down vk::end       vk::home
vk::left       vk::up        vk::right     vk::down
vk::insert     vk::delete

vk::k0 .. vk::k9          // top-row digits
vk::a  .. vk::z           // letters

vk::lwin       vk::rwin
vk::numpad0 .. vk::numpad9
vk::multiply   vk::add     vk::subtract  vk::decimal  vk::divide

vk::f1 .. vk::f12

vk::num_lock   vk::scroll_lock
vk::lshift     vk::rshift
vk::lctrl      vk::rctrl
vk::lalt       vk::ralt

// Mouse buttons (Win32 puts these in the same VK space):
vk::lbutton  vk::rbutton  vk::mbutton  vk::xbutton1  vk::xbutton2
```

## Example: trigger an action on F1 press

```cpp
void my_tick(int64 data) {
    if (key_fired(vk::f1)) {
        println("F1 pressed");
    }
}

int64 main() {
    register_routine(reinterpret_cast<int64>(my_tick), 0);
    return 1;
}
```
