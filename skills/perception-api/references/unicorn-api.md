> For the complete documentation index, see [llms.txt](https://docs.perception.cx/perception/llms.txt). Markdown versions of documentation pages are available by appending `.md` to page URLs; this page is available as [Markdown](https://docs.perception.cx/perception/unicorn-api.md).

# Unicorn API

All unicorn natives are auto-registered into every loaded script.

`cpu_t` is the emulator handle. RAII destructor closes the engine + frees user hooks. Standalone or process-bound (process-bound: unmapped reads pull from `proc.rvm`, writes go to `proc.wvm`).

## Constants — exposed as enums (no header import needed)

```cpp
uc_reg::rax / rbx / rcx / rdx / rsi / rdi / rbp / rsp
uc_reg::r8 .. r15
uc_reg::rip / eflags
uc_reg::cs / ds / es / fs / gs / ss
uc_reg::fs_base / gs_base / mxcsr
uc_reg::xmm0 .. xmm15
uc_reg::ymm0 .. ymm15

uc_prot::none / read / write / exec / rw / rx / rwx / all

uc_hook::code            // fires per instruction
uc_hook::mem_unmapped    // fires on read/write/fetch to unmapped page
```

## Construction

```cpp
cpu_t cpu_create();
cpu_t cpu_create_process(proc_t proc, bool allow_writes);
```

* `cpu_create` — standalone emulator, no process backing.
* `cpu_create_process` — unmapped reads pull pages from `proc.rvm`; writes go to `proc.wvm` if `allow_writes=true`. Auto-installs read/write/fetch/unmapped hooks plus invalid-instruction + interrupt handlers.

The destructor closes the engine and frees any user hooks.

## Memory

`mem_read` and `mem_write` (plus the SIMD-register variants below) are FREE FUNCTIONS taking the cpu handle. Under the hood the byte-buffer natives are `.__mem_write_native(addr, pointer, int64)` / `.__mem_read_native(addr, size)` on the type; the free-function wrappers accept `std::vector<uint8>` and adopt the return so scripts stay clean.

```cpp
bool                cpu.mem_map(int64 addr, int64 size, uc_prot perms);
bool                mem_write(cpu_t cpu, int64 addr, const std::vector<uint8>& bytes);
std::vector<uint8>  mem_read (cpu_t cpu, int64 addr, int64 size);           // empty vector on read failure
```

`addr` and `size` should be page-aligned. `mem_map` returns true on success or already-mapped. `perms` accepts `uc_prot::*` values OR'd together.

## Registers

```cpp
bool  cpu.reg_write64(uc_reg reg, int64 value);
int64 cpu.reg_read64 (uc_reg reg);

bool                reg_write128(cpu_t cpu, uc_reg reg, const std::vector<uint8>& bytes);    // XMM, must be exactly 16 bytes
std::vector<uint8>  reg_read128 (cpu_t cpu, uc_reg reg);

bool                reg_write256(cpu_t cpu, uc_reg reg, const std::vector<uint8>& bytes);    // YMM, must be exactly 32 bytes
std::vector<uint8>  reg_read256 (cpu_t cpu, uc_reg reg);
```

## Execution

```cpp
int64 cpu.start(int64 begin, int64 end, int64 timeout, int64 count);
```

Emulates from `begin` to `end`. `timeout=0` and `count=0` are unbounded. Returns a `uc_err` code (0 = OK).

```cpp
void cpu.emu_stop();         // halt emulation from inside a hook
bool cpu.flush_code();       // drop translation cache (after self-modifying writes)
bool cpu.setup_stack(int64 base, int64 size, int64 stop_addr);
```

`setup_stack` maps stack pages, plants a NOP page at `stop_addr` (so a `RET` out lands somewhere mapped), and sets `RSP`.

## Hooks (script callbacks)

```cpp
bool cpu.hook_add(uc_hook hook_kind, int64 fn_handle);
```

* `fn_handle` = `reinterpret_cast<int64>(my_callback)` closure handle.
* Callback shape: `int64 cb(int64 addr)`. **Return 0 to stop emulation; non-zero to continue.**

The currently-emulating CPU is available as `cpu_active()` from inside the callback — useful for reading/writing state without capturing the handle.

```cpp
cpu_t cpu_active();    // null outside a hook
```

## Exception inspection

```cpp
int64 cpu.get_last_exception();      // NTSTATUS-shaped: 0 = none, 0xC000001D = invalid insn, 0xC0000005 = AV
int64 cpu.get_exception_address();   // RIP at the faulting instruction
```

Set when emulation stops due to invalid instruction, AV-style unmapped access, or interrupt.

## Example: emulate `mov rax, 0x42; hlt`

```cpp
cpu_t cpu = cpu_create();
cpu.mem_map(0x10000, 0x1000, uc_prot::rwx);
cpu.mem_map(0x20000, 0x1000, uc_prot::rw);

// Build code: mov rax, 0x42 + hlt (0xF4)
zydis_req_t r;
r.set_mnemonic(zydis_mnemonic_from_string("mov"));
r.set_operand_count(2);
r.set_operand_reg(0, zydis_register_from_string("rax"));
r.set_operand_imm(1, 0x42);
std::vector<uint8> code = zydis_encode(r);
code.push_back(static_cast<uint8>(0xF4));

mem_write(cpu, 0x10000, code);
cpu.reg_write64(uc_reg::rsp, 0x21000 - 8);

cpu.start(0x10000, 0x10000 + code.size(), 1000000, 0);
println(to_string(cpu.reg_read64(uc_reg::rax)));    // "66"  (= 0x42)
```

## Lifetime

`cpu_t` releases its host resources via the destructor at scope exit. If a script forgets, the host sweeps remaining cpus at unload — engine closed, hooks freed, no permanent leak.

## Notes

* Hook callbacks fire on the emulating thread (whichever thread called `cpu.start`). Enma's TLS is already set up in that context — heap-alloc, string concat etc. all work.
* `cpu_create_process` automatically maps a fake TEB at `0x101000` and a fake / real PEB so guest code reading FS/GS doesn't fault. `gs_base` and `fs_base` are set to the fake TEB.
