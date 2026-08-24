> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/win-api.md).

# Win API

All win natives are auto-registered into every loaded script.

This API **sends** input and reads window state. For state polling (mouse position, key down/up etc.), see [Input API](/perception/input-api.md).

`HWND` is exposed as raw `int64`. OS-owned; if the window disappears, subsequent calls reject via `IsWindow()`.

## `window_info_t`

Snapshot of a window at enumeration time. Heap-allocated; the numeric fields are methods on the handle, and the three string fields are FREE FUNCTIONS taking the handle as their first argument (the shipped-convention pattern — see the note at the top of [Proc API](/perception/proc-api.md#method-vs-free-function) for why).

```cpp
int64        info.hwnd();
int64        info.pid();
int64        info.tid();
std::string  process_name(window_info_t info);    // exe basename
std::string  title(window_info_t info);           // window title at snapshot time
std::string  class_name(window_info_t info);
```

## Enumerate / find

```cpp
std::vector<window_info_t> get_all_hwnds();

int64 find_window(const char* title);
int64 find_window(const std::string& title);
int64 find_window(const char* title, const char* class_name);
int64 find_window(const std::string& title, const std::string& class_name);
```

`find_window` returns 0 when no match.

## Window queries

Geometry is split per axis (no array tuples). Combine pos + size for a rect.

```cpp
int64        get_window_width(int64 hwnd);                    // 0 on invalid hwnd
int64        get_window_height(int64 hwnd);                   // 0 on invalid hwnd
vec2         get_window_pos(int64 hwnd);                      // screen coords; (0,0) on invalid
vec2         get_window_size(int64 hwnd);                     // (width, height) as vec2
bool         is_foreground_window(int64 hwnd);
bool         is_window_active(int64 hwnd);                    // visible AND not minimized
std::string  get_window_title(int64 hwnd);
std::string  get_window_class(int64 hwnd);
bool         set_foreground_window(int64 hwnd);
int64        get_window_thread_id(int64 hwnd);                // 0 on invalid hwnd
int64        get_window_process_id(int64 hwnd);               // 0 on invalid hwnd
bool         post_message(int64 hwnd, int64 msg, int64 wparam, int64 lparam);
```

## Clipboard

```cpp
bool         copy_to_clipboard(const char* text);
bool         copy_to_clipboard(const std::string& text);
std::string  copy_from_clipboard();    // empty string when nothing or wrong format
```

`copy_to_clipboard` is gated by perception's restricted-string filter (returns false + logs when blocked).

## Keyboard SEND

Synthesized via `SendInput`. Restricted virtual keys (set host-side) are blocked + logged.

```cpp
void win_key_down (int64 vk);
void win_key_up   (int64 vk);
void win_key_press(int64 vk, int64 delay_ms);    // down + sleep + up; delay capped at 1000ms

bool send_char(int64 hwnd, const char* text);          // PostMessageW(WM_CHAR), first wide char only
bool send_char(int64 hwnd, const std::string& text);
bool send_key (int64 hwnd, int64 vk);                  // PostMessageW(WM_KEYDOWN+WM_KEYUP) targeted at hwnd
```

## Mouse SEND

```cpp
void mouse_move         (int64 x, int64 y);          // absolute screen coords
void mouse_move_relative(int64 dx, int64 dy);
void mouse_left_click   ();                          // down + 10ms + up
void mouse_right_click  ();
void mouse_middle_click ();
void mouse_scroll       (int64 amount);              // multiples of WHEEL_DELTA
void send_mouse_input   (int64 dx, int64 dy, int64 flags, int64 mouse_data);   // raw SendInput
```

## Example: focus a window and click in it

```cpp
int64 hwnd = find_window("Notepad");
if (hwnd == 0) return 0;

set_foreground_window(hwnd);
sleep_ms(50);

vec2 pos = get_window_pos(hwnd);
vec2 sz  = get_window_size(hwnd);
mouse_move(pos.x() + sz.x() / 2.0, pos.y() + sz.y() / 2.0);
mouse_left_click();
```
