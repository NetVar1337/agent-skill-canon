> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/mcp-api.md).

# MCP API

Perception's MCP server exposes the proc-API surface as JSON-RPC tools that any [Model Context Protocol](https://modelcontextprotocol.io/) client (Claude Code, Cline, Continue, ...) can call.

Writing an Enma script? Use [Proc](/perception/proc-api.md) / [CPU](/perception/cpu-api.md) / [Zydis](/perception/zydis-api.md) directly. Driving perception from an AI agent? Enable MCP.

## Enable

In perception, **Settings → Perception MCP**:

1. Type a **Bind port** (1024..65535), or leave blank for OS-pick.
2. Toggle **Enable MCP server** on.
3. Copy the **Bound URL** that appears.

The server is loopback-only.

### Other toggles in the same panel

* **Auto-start on perception load** — persisted; on next launch the server starts automatically using the saved port (config loads before the autostart fires, so the saved port is reused rather than regenerated).
* **Heap-only scans by default** — controls the default of the `heap_only` flag on `scan_value` / `scan_string` / `scan_pointer_to` / `find_string_refs` when an MCP caller omits it. **On by default.** Flipping it off makes those tools walk the entire user-space when callers don't supply `heap_only`, which can OOM or hang on targets with multi-GiB heaps (Forza-class).

## Connect

**Claude Code:**

```
claude mcp add --transport http perception http://127.0.0.1:<port>/mcp
```

Add `--scope user` for global registration. Other clients (Cline, Continue, ...) accept the same URL via their Streamable HTTP transport.

## Transport

The server auto-detects two framings on the same port:

| First bytes                           | Framing                 | Used by                              |
| ------------------------------------- | ----------------------- | ------------------------------------ |
| `POST` / `GET` / `OPTIONS` / `DELETE` | HTTP/1.1 streamable     | MCP clients                          |
| anything else                         | Line-delimited JSON-RPC | The cpp example below, raw debugging |

Both carry JSON-RPC 2.0. Real MCP clients use the 5 protocol methods (`initialize` / `notifications/initialized` / `tools/list` / `tools/call` / `ping`); raw clients can call tool methods directly (`"method": "process/list"`).

## Handles

Most tools need a `handle` from `process/reference_by_pid` / `_by_name`. Handles are **per-connection**:

* Other connections can't use yours.
* Disconnecting releases everything automatically.
* Manual release: `process/dereference` (one) or `process/cleanup_references` (all).

## Permissions

Shared with [enma](/perception/proc-api.md#permissions). Toggle in **Scripting → API permissions**:

| Flag                        | Gates                                                                                                                                                                                                                |
| --------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `kernel_rw_access`          | Kernel-mode addresses in any read / write / disasm / `query_memory_region` / `find_pattern*` call; the `eprocess` field in `process/list` + `info_by_*`; the `ethread` field in `get_threads`; `system/list_drivers` |
| `write_memory`              | Every tool that writes target memory: `write_virtual_memory`, `write_typed_value`, `write_string`, `copy_memory`, `fill_memory`                                                                                      |
| `virtual_memory_operations` | `allocate_memory`, `free_memory`                                                                                                                                                                                     |

Blocked calls return `-32001` with the missing permission named.

## Error codes

| Code     | Meaning                         |
| -------- | ------------------------------- |
| `-32700` | Parse error                     |
| `-32600` | Invalid request                 |
| `-32601` | Method not found                |
| `-32602` | Invalid params                  |
| `-32603` | Internal                        |
| `-32001` | Permission denied               |
| `-32002` | Stale / cross-connection handle |
| `-32003` | Target not found                |
| `-32004` | Operation failed                |

## Tools

59 tools. Addresses + handles are **hex strings** (`"0x7ff7..."`) — JSON numbers lose precision past 2^53. Required params are listed plain, optional params get `?`.

Every "take a handle" tool below has `handle` as its first param — omitted from the params column to keep things readable. The tools that don't take a handle are called out per-section.

### Discovery + reference lifecycle

Params shown literally (no implicit `handle` — `process/dereference` explicitly takes the handle it's about to release).

| Tool                         | Params   |                                                   |
| ---------------------------- | -------- | ------------------------------------------------- |
| `process/list`               | —        | Snapshot of every active process.                 |
| `process/info_by_pid`        | `pid`    | One process by PID.                               |
| `process/info_by_name`       | `name`   | One process by image name.                        |
| `process/reference_by_pid`   | `pid`    | Take a per-connection handle. Returns hex string. |
| `process/reference_by_name`  | `name`   | Same, by image name.                              |
| `process/dereference`        | `handle` | Release one handle.                               |
| `process/cleanup_references` | —        | Release every handle this connection holds.       |
| `process/list_references`    | —        | What this connection currently holds.             |

### Memory I/O

| Tool                           | Params                                            |                                                                                   |
| ------------------------------ | ------------------------------------------------- | --------------------------------------------------------------------------------- |
| `process/read_virtual_memory`  | `address`, `size`                                 | Raw bytes as hex. Max 16 MiB.                                                     |
| `process/write_virtual_memory` | `address`, `data`                                 | Gated `write_memory`. `data` is hex.                                              |
| `process/is_valid_address`     | `address`                                         | Does the address resolve?                                                         |
| `process/read_typed_value`     | `address`, `type`                                 | `type` ∈ `u8..u64 / i8..i64 / f32 / f64 / ptr / bool`.                            |
| `process/write_typed_value`    | `address`, `type`, `value`                        | Gated `write_memory`. Use hex string for `value` when type is u64/i64/ptr.        |
| `process/read_string`          | `address`, `max_len?`, `encoding?`                | `max_len` 1024 default. `encoding` ∈ `auto / ascii / utf16` (default auto-sniff). |
| `process/write_string`         | `address`, `text`, `encoding?`, `null_terminate?` | Gated `write_memory`. `encoding` ∈ `ascii / utf16`.                               |
| `process/copy_memory`          | `src_address`, `dst_address`, `size`              | In-target memcpy. Gated `write_memory`. Max 64 MiB, 1 MiB chunks.                 |
| `process/fill_memory`          | `address`, `size`, `byte`                         | Memset. `byte` 0..255 (0x90 = NOP, 0xCC = int3). Gated `write_memory`.            |
| `process/read_pointer_chain`   | `base_address`, `offsets`                         | `offsets` is an int array, max 64.                                                |
| `process/disassemble`          | `address`, `max_bytes?`, `max_instructions?`      | Zydis. Defaults 256 / 32.                                                         |

### Modules / threads / PE

| Tool                          | Params                                    |                                                                                                                                                                                                                            |
| ----------------------------- | ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `process/get_modules`         | —                                         | All loaded modules.                                                                                                                                                                                                        |
| `process/get_threads`         | —                                         | All threads.                                                                                                                                                                                                               |
| `process/get_module_by_name`  | `name`                                    | One module by name.                                                                                                                                                                                                        |
| `process/get_export_address`  | `module_base`, `export_name`              | Single resolve.                                                                                                                                                                                                            |
| `process/get_import_address`  | `module_base`, `import_name`              | Resolve IAT slot VA.                                                                                                                                                                                                       |
| `process/get_module_imports`  | `module_base`                             | Full IAT walk.                                                                                                                                                                                                             |
| `process/list_module_exports` | `module_base`                             | Full EAT walk.                                                                                                                                                                                                             |
| `process/get_module_sections` | `module_base`                             | PE sections.                                                                                                                                                                                                               |
| `process/get_pe_header`       | `module_base`                             | NT/optional header summary.                                                                                                                                                                                                |
| `process/get_module_strings`  | `module_base`, `min_length?`, `encoding?` | `min_length` default 4. `encoding` ∈ `ascii / utf16 / both` (default both).                                                                                                                                                |
| `process/get_exception_table` | `module_base`, `max_entries?`             | x64 RUNTIME\_FUNCTION entries from `.pdata`. Precise function bounds.                                                                                                                                                      |
| `process/get_data_directory`  | `module_base`, `directory`                | One PE data-dir entry. `directory` ∈ `export / import / resource / exception / security / basereloc / debug / architecture / globalptr / tls / load_config / bound_import / iat / delay_import / com_descriptor` or 0..15. |

### Memory regions + allocation

| Tool                               | Params       |                                                                                                                                                                                                   |
| ---------------------------------- | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `process/query_memory_region`      | `address`    | VirtualQuery-style.                                                                                                                                                                               |
| `process/enumerate_memory_regions` | `heap_only?` | All committed regions. `heap_only` default false.                                                                                                                                                 |
| `process/allocate_memory`          | `size`       | Gated `virtual_memory_operations`. Max 256 MiB. Allocation itself is safe. To execute code from the returned VA the target must have Control Flow Guard (CFG) off; reads + writes are unaffected. |
| `process/free_memory`              | `address`    | Same gate.                                                                                                                                                                                        |

### Pattern + scanner + xrefs + signature

| Tool                         | Params                                                             |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| ---------------------------- | ------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `process/find_pattern`       | `start`, `size`, `signature`                                       | IDA-style `"AB CD ?? EF"`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `process/find_all_patterns`  | `start`, `size`, `signature`                                       | Same, all hits (cap 1024).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `process/scan_value`         | `type`, `value`, `aligned?`, `heap_only?`                          | `type` ∈ `u8..u64 / i8..i64 / f32 / f64`. Use hex string for `value` when u64/i64. Defaults: aligned true. `heap_only` defaults to the MCP UI's "Heap-only by default" toggle (on by default — skips code/module regions); pass `heap_only=false` to walk full user-space.                                                                                                                                                                                                                                                                                                             |
| `process/scan_next`          | `compare`, `value?`, `min?`, `max?`                                | `compare` ∈ `exact / range / unchanged / changed / increased / decreased`. `value` for `exact`, `min`+`max` for `range`.                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `process/scan_string`        | `text`, `encoding?`, `heap_only?`                                  | `encoding` ∈ `ascii / utf16`, default ascii. `heap_only` default = UI toggle (see `scan_value`).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `process/scan_pointer_to`    | `target_address`, `heap_only?`                                     | Aligned QWORDs pointing at `target_address`. `heap_only` default = UI toggle.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `process/find_xrefs`         | `module_base`, `target_address`                                    | Decode `.text`, return refs.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `process/find_string_refs`   | `module_base`, `text`, `encoding?`, `heap_only?`, `string_module?` | Combo: scan for the string, then decode `module_base`'s `.text` for code refs to each hit. Phase 1 (string search) defaults to a heap-only VAD walk (`heap_only` follows the UI toggle) and is **pre-capped at 1 GiB** so the listener never crashes on huge targets — if the cap fires the call returns an error asking you to pass `heap_only=true` or set `string_module` (hex VA of the module that owns the string, usually the same as `module_base`) for a fast bounded scan of just that module's image. Phase 2 caps code hits at 4096; response includes a `truncated` flag. |
| `process/generate_signature` | `address`, `max_length?`                                           | Default 32. `is_unique=false` if length exhausted.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `process/diff_memory`        | `addr_a`, `addr_b`, `size`                                         | Cap 1 MiB.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |

### Code analysis

| Tool                                 | Params                                   |                                                                             |
| ------------------------------------ | ---------------------------------------- | --------------------------------------------------------------------------- |
| `process/find_function_bounds`       | `address`, `scan_back?`, `scan_forward?` | Defaults 4096 / 65536. Heuristic — use `get_exception_table` for precision. |
| `process/find_function_by_signature` | `module_base`, `signature`               | AOB-scan a module's `.text` + run bounds walk on each hit.                  |
| `process/analyze_vtable`             | `vtable_address`, `max_entries?`         | Default 64. Classifies entries as code/data per loaded modules.             |
| `process/read_rtti`                  | `vtable_address`                         | Win64 RTTI: class name + base classes.                                      |

### Symbol / function lookup

| Tool                            | Params                                       |                                                                                          |
| ------------------------------- | -------------------------------------------- | ---------------------------------------------------------------------------------------- |
| `process/lookup_symbol`         | `address`                                    | VA → `{module_base, module_name, module_offset, section, nearest_export}`.               |
| `process/find_function_by_name` | `pattern`, `case_sensitive?`, `max_results?` | Substring match across all modules' export tables. Default case-insensitive, 64 results. |

### Handles

| Tool                   | Params         |                                                                            |
| ---------------------- | -------------- | -------------------------------------------------------------------------- |
| `process/enum_handles` | `max_entries?` | Default 8192. `NtQuerySystemInformation(SystemExtendedHandleInformation)`. |

### System / environment

| Tool                       | Params         |                                                                                                              |
| -------------------------- | -------------- | ------------------------------------------------------------------------------------------------------------ |
| `system/info`              | —              | Build number, page size, processor count + arch. `is_24h2_or_later` flag for build-keyed offsets. No handle. |
| `system/list_drivers`      | `max_entries?` | Kernel modules via `NtQuerySystemInformation(SystemModuleInformation)`. Gated `kernel_rw_access`. No handle. |
| `process/get_command_line` | —              | Reads `PEB.ProcessParameters.CommandLine`. x64 only.                                                         |
| `process/list_environment` | `max_bytes?`   | Reads `PEB.ProcessParameters.Environment`. Returns `[{key, value}]`.                                         |

### Enma scripting bridge

None of these takes a `handle` — they run a script (or return reference text) with its own permissions, independent of any referenced process.

| Tool                 | Params   |                                                                                                                                                                                                                                                                                                                                    |
| -------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `script/get_context` | —        | Returns the full enma language + Perception API reference as a single `context` string. **Call this once per session before generating any script** — enma is proprietary and its addon surface can't be inferred from training data. Covers language grammar, all 17 pre-shipped enma addons, and all 12 Perception API surfaces. |
| `script/validate`    | `source` | Compile-only. **All** addons registered (render / proc / cpu / zydis / sound / win / unicorn / net / input / **gui** / **thread** / filesystem). Returns `{ ok, errors:[] }`.                                                                                                                                                      |
| `script/execute`     | `source` | Compile + run `main()` once. **GUI and thread addons are NOT registered** — those resources would outlive a one-shot script and leak. For long-lived scripts use the in-app script editor. Returns `{ ok, logs:[] }`.                                                                                                              |

## Example — minimal C++ client

Build with the VS Developer Command Prompt:

```
cl /EHsc /std:c++17 minimal_mcp.cpp /link Ws2_32.lib
```

```cpp
#define WIN32_LEAN_AND_MEAN
#include <winsock2.h>
#include <ws2tcpip.h>
#include <stdio.h>
#pragma comment(lib, "Ws2_32.lib")

int main(int argc, char** argv) {
    if (argc < 2) { printf("usage: %s <port>\n", argv[0]); return 1; }
    WSADATA wd; WSAStartup(MAKEWORD(2, 2), &wd);

    SOCKET s = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    sockaddr_in a{};
    a.sin_family = AF_INET;
    a.sin_port   = htons((u_short)atoi(argv[1]));
    inet_pton(AF_INET, "127.0.0.1", &a.sin_addr);
    if (connect(s, (sockaddr*)&a, sizeof(a)) != 0) { printf("connect failed\n"); return 1; }

    auto call = [&](const char* line) {
        send(s, line, (int)strlen(line), 0);
        char buf[8192];
        int n = recv(s, buf, sizeof(buf) - 1, 0);
        if (n > 0) { buf[n] = 0; printf("%s", buf); }
    };

    // 1. List processes.
    call("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"process/list\",\"params\":{}}\n");

    // 2. Reference notepad.exe.
    call("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"process/reference_by_name\","
         "\"params\":{\"name\":\"notepad.exe\"}}\n");

    // 3. Run a one-shot enma script.
    call("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"script/execute\","
         "\"params\":{\"source\":\"fn main() { println(\\\"hello from mcp\\\"); }\"}}\n");

    closesocket(s);
    WSACleanup();
    return 0;
}
```

Plain line-delimited JSON-RPC — the server's auto-detect routes us to that framing because we don't open with `POST` / `GET`. For the HTTP path, wrap each request in `POST /mcp HTTP/1.1\r\nContent-Length: N\r\n\r\n<body>` — that's what Claude Code does for you.
