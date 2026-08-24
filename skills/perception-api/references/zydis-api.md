> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/zydis-api.md).

# Zydis API

All Zydis natives are auto-registered into every loaded script.

Two handle types:

* `zydis_req_t` — single-instruction encoder request (mnemonic + operands).
* `zydis_builder_t` — sequence of requests + raw byte chunks; encodes to a flat byte buffer with absolute addressing.

Constants are exposed as enums (`zydis_machine_mode::long_64` etc.) so no header import is needed.

## `zydis_req_t`

```cpp
zydis_req_t r;                                          // factory: defaults to MODE_LONG_64
r.set_mnemonic(int64 mnemonic);                         // ZydisMnemonic value (use zydis_mnemonic_from_string)
r.set_machine_mode(zydis_machine_mode mode);
r.set_operand_count(int64 count);                       // 0..4
r.set_branch_type (zydis_branch_type type);
r.set_branch_width(zydis_branch_width width);

r.set_operand_reg(int64 idx, int64 reg);                                              // ZydisRegister value
r.set_operand_imm(int64 idx, int64 imm);
r.set_operand_mem(int64 idx, int64 base, int64 idx_reg, int64 scale, int64 disp, int64 size);
r.set_operand_ptr(int64 idx, int64 segment, int64 offset);

int64              r.get_mnemonic();
zydis_machine_mode r.get_machine_mode();
int64              r.get_operand_count();
```

## Encoding

```cpp
std::vector<uint8>  zydis_encode         (zydis_req_t req);                                       // empty vector on failure
std::vector<uint8>  zydis_encode_absolute(zydis_req_t req, int64 runtime_rip);                    // bakes RIP-relative immediates
std::vector<uint8>  zydis_nop_fill       (int64 length);                                          // minimal NOP padding
zydis_req_t         zydis_decoded_to_request(const std::vector<uint8>& bytes, int64 runtime_rip);
```

`zydis_decoded_to_request` decodes the bytes and returns a fresh request you can mutate and re-encode (useful for instruction patching).

## Mnemonic / register name lookup

```cpp
int64        zydis_mnemonic_from_string(const char* name);          // case-insensitive; 0 (INVALID) if no match
int64        zydis_mnemonic_from_string(const std::string& name);
std::string  zydis_mnemonic_to_string  (int64 mnemonic);

int64        zydis_register_from_string(const char* name);          // case-insensitive; 0 (NONE) if no match
int64        zydis_register_from_string(const std::string& name);
std::string  zydis_register_to_string  (int64 reg);
```

## Disassembly (textual)

```cpp
std::vector<std::string> zydis_disasm(const std::vector<uint8>& bytes, int64 runtime_rip);
```

One element per decoded instruction, formatted as Zydis's intel syntax (e.g. `"mov rax, 0x1234"`). Decoding stops at the first invalid byte.

For per-operand structure, decode + convert to a `zydis_req_t` via `zydis_decoded_to_request` and read the request fields.

## `zydis_builder_t`

Builds a sequence of instructions (and raw bytes) into one flat output buffer. Tracks a base address so RIP-relative encoding produces correct offsets. Byte-buffer input (`push_bytes`) and the final `build()` use FREE FUNCTIONS taking the handle — same reason as `window_info_t`'s string methods: type-builder methods can't be extended from the `.em` wrapper, so std::vector-shaped overloads live as free functions.

```cpp
zydis_builder_t b;
b.set_machine_mode(zydis_machine_mode mode);
b.set_base_address(int64 addr);
b.clear();

b.push        (zydis_req_t req);
void push_bytes(zydis_builder_t b, const std::vector<uint8>& bytes);   // free fn wrapping method
b.push_byte   (uint8 b);
b.push_u16    (uint16 v);    // little-endian
b.push_u32    (uint32 v);    // little-endian
b.push_u64    (uint64 v);    // little-endian
b.push_nop    (int64 count);
b.push_int3   ();
b.push_ret    ();

std::vector<uint8> zydis_builder_build(zydis_builder_t b);    // free fn wrapping method; encodes every entry in order
int64 b.get_count();                                          // number of entries
```

## Enums (no header needed)

```cpp
zydis_machine_mode::long_64 / long_compat_32 / long_compat_16 / legacy_32 / legacy_16 / real_16
zydis_branch_type::none / short / near / far
zydis_branch_width::none / w8 / w16 / w32 / w64
```

## Example: encode `mov rax, 0x42`, then disasm

```cpp
int64 mov_id = zydis_mnemonic_from_string("mov");
int64 rax_id = zydis_register_from_string("rax");

zydis_req_t r;
r.set_mnemonic(mov_id);
r.set_operand_count(2);
r.set_operand_reg(0, rax_id);
r.set_operand_imm(1, 0x42);

std::vector<uint8>        bytes = zydis_encode(r);
std::vector<std::string>  texts = zydis_disasm(bytes, 0);

println(texts[0]);    // "mov rax, 0x42"
```
