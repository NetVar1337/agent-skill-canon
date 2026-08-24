> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/net-api.md).

# Net API

All net natives are auto-registered into every loaded script.

All network calls are gated by the `network_access` permission. Without it, calls return a transport-failure value (`status=0` / null handle / empty vector).

## Method vs free function

Many operations on `http_response_t`, `ws_t`, `ws_message_t`, and `udp_t` return `std::string` / `std::vector<uint8>`, or take `const std::string&` / `const std::vector<uint8>&` parameters. The `type_builder` API that registers those types' methods can't be extended from the `.em` prelude, so those calls appear in the docs as FREE FUNCTIONS taking the handle as their first arg — `body(r)` instead of `r.body()`, `send_text(ws, msg)` instead of `ws.send_text(msg)`, `recv(u, timeout)` instead of `u.recv(timeout)`. Operations that only take and return primitives (`r.status()`, `r.ok()`, `ws.is_open()`, `ws.recv()` returning `ws_message_t`, `ws.poll()`, `ws.close(code)`, `m.ok()`, `m.is_text()`, `m.is_closed()`, `u.last_sender_port()`, `u.close()`) stay as methods. Same pattern as `proc` / `gui` / `win` / `unicorn`.

## HTTP — sync, with timeout

```cpp
http_response_t http_get(const char* url, int64 timeout_ms);
http_response_t http_get(const std::string& url, int64 timeout_ms);
http_response_t http_get(const char* url,
                         std::unordered_map<std::string, std::string>& headers,
                         int64 timeout_ms);
http_response_t http_get(const std::string& url,
                         std::unordered_map<std::string, std::string>& headers,
                         int64 timeout_ms);

http_response_t http_post(const char* url, const char* content_type,
                          const char* body, int64 timeout_ms);
http_response_t http_post(const std::string& url, const std::string& content_type,
                          const std::string& body, int64 timeout_ms);
http_response_t http_post(const char* url, const char* content_type,
                          const char* body,
                          std::unordered_map<std::string, std::string>& headers,
                          int64 timeout_ms);
http_response_t http_post(const std::string& url, const std::string& content_type,
                          const std::string& body,
                          std::unordered_map<std::string, std::string>& headers,
                          int64 timeout_ms);
```

Both always return a non-null `http_response_t`. Read via:

```cpp
int64        r.status();       // 0 on transport failure / permission denied
std::string  body(r);          // free fn — response body
bool         r.ok();           // true if status is 200..299
```

`content_type` may be empty for `http_post`. The 3-arg `http_get` / 5-arg `http_post` overloads take a `std::unordered_map<std::string, std::string>` of extra request headers — useful for `Authorization: Bearer ...`, `X-API-Key`, `Accept`, custom protocol headers, etc. Pass an empty map to skip.

### Headers example

```cpp
std::unordered_map<std::string, std::string> headers;
headers["Authorization"] = "Bearer " + g_token;
headers["Accept"] = "application/json";

http_response_t r = http_get("https://api.example.com/me", headers, 5000);
if (r.ok()) println(body(r));
```

```cpp
std::unordered_map<std::string, std::string> headers;
headers["X-API-Key"] = "abc123";

http_response_t r = http_post(
    "https://api.example.com/events",
    "application/json",
    "{\"event\":\"login\"}",
    headers,
    5000);
```

## WebSocket

```cpp
ws_t ws_connect(const char* url, int64 timeout_ms);
ws_t ws_connect(const std::string& url, int64 timeout_ms);
```

Connects to `ws://`, `wss://` (also `http://` / `https://` accepted). Spawns a background recv thread. Returns a null handle on failure or permission denied.

### `ws_t` operations

```cpp
bool          ws.is_open();
bool          send_text  (ws_t ws, const char* msg);                       // free fn
bool          send_text  (ws_t ws, const std::string& msg);                // free fn
bool          send_binary(ws_t ws, const std::vector<uint8>& data);        // free fn

ws_message_t  ws.recv();      // blocks until a message arrives or the connection closes
ws_message_t  ws.poll();      // non-blocking

void          ws.close(int64 code);    // standard WS close codes (1000 = normal)
```

### `ws_message_t` operations

```cpp
bool         m.ok();          // true if a message was returned
bool         m.is_text();     // payload framing
bool         m.is_closed();   // peer / local close has fired
std::string  payload(m);      // free fn — message payload
```

## UDP — raw datagrams

```cpp
udp_t udp_create();
```

Creates a fresh UDP socket. Returns a null handle on failure / permission denied. Send-only sockets can skip `bind()`; sockets that receive must `bind()` to a local port first.

### `udp_t` operations

```cpp
bool                 bind   (udp_t u, const char* addr, int64 port);          // free fn — "0.0.0.0" / port; port 0 = OS-picked
bool                 bind   (udp_t u, const std::string& addr, int64 port);   // free fn
bool                 send_to(udp_t u, const std::vector<uint8>& data,
                             const char* addr, int64 port);                   // free fn
bool                 send_to(udp_t u, const std::vector<uint8>& data,
                             const std::string& addr, int64 port);            // free fn
std::vector<uint8>   recv   (udp_t u, int64 timeout_ms);                      // free fn — blocking with timeout; empty on timeout/error

std::string  last_sender_addr(udp_t u);   // free fn — IP of the most recent successful recv
int64        u.last_sender_port();        // port of the most recent successful recv

void         u.close();
```

`recv` returns up to one full UDP datagram (max 65535 bytes). Timeout is in milliseconds — `timeout_ms = 0` means block indefinitely. After a successful `recv`, `last_sender_addr(u)` / `u.last_sender_port()` give you the peer to reply to.

### UDP example — Source Query Protocol (A2S\_INFO)

```cpp
udp_t s = udp_create();
if (static_cast<int64>(s) == 0) return 0;

// A2S_INFO request: FF FF FF FF 54 "Source Engine Query" 00
std::vector<uint8> q;
q.push_back(0xFF); q.push_back(0xFF); q.push_back(0xFF); q.push_back(0xFF);
q.push_back(0x54);
std::string banner = "Source Engine Query";
for (int64 i = 0; i < banner.size(); i = i + 1) {
    q.push_back(static_cast<uint8>(banner[i]));
}
q.push_back(0x00);

if (!send_to(s, q, "1.2.3.4", 27015)) {
    println("send failed");
    return 0;
}

std::vector<uint8> reply = recv(s, 2000);  // 2-second timeout
if (reply.size() == 0) {
    println("no reply (timeout)");
} else {
    println(format("got {d} bytes from {s}:{d}",
        reply.size(), last_sender_addr(s), s.last_sender_port()));
}
```

### UDP example — listener

```cpp
udp_t s = udp_create();
bind(s, "0.0.0.0", 9999);

for (int32 i = 0; i < 10; i = i + 1) {
    std::vector<uint8> pkt = recv(s, 1000);
    if (pkt.size() == 0) continue;
    println(format("from {s}:{d} ({d} bytes)",
        last_sender_addr(s), s.last_sender_port(), pkt.size()));
}
```

## HTTP example

```cpp
http_response_t r = http_get("https://api.example.com/status", 5000);
if (r.ok()) {
    println("got: " + body(r));
} else if (r.status() == 0) {
    println("transport failed or permission denied");
} else {
    println("server returned " + to_string(r.status()));
}
```

## WebSocket example

```cpp
ws_t ws = ws_connect("wss://echo.example.com/", 5000);
if (static_cast<int64>(ws) == 0) return 0;

send_text(ws, "hello");
ws_message_t m = ws.recv();
if (m.ok()) {
    println("got: " + payload(m));
}
ws.close(1000);
```

## Permission

`network_access` gates every native in this file (HTTP, WebSocket, UDP). When off, every call returns a transport-failure value.

## Lifetime

`ws_t` and `udp_t` close + free via the destructor at scope exit. If the script forgets, the host sweeps remaining sockets at unload — connections closed, threads joined, no permanent leak. UDP packets in flight are not buffered host-side; once you close, anything still on the wire is dropped by the OS.
