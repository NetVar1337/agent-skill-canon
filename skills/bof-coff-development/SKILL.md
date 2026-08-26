---
name: bof-coff-development
description: Use when building, inspecting, debugging, or reviewing Cobalt Strike Beacon Object Files, COFF object loaders, inline-execute modules, relocations, Beacon API shims, argument packing, or position-independent Windows post-exploitation code that runs without the normal PE loader.
license: MIT
---
# BOF and in-memory COFF development

## When to use

Use this skill for a Windows COFF object that is loaded from bytes, relocated by a small runtime, and entered through `go(char *args, int len)` rather than the PE loader.
Use it when the task names a Beacon Object File, BOF, COFF loader, inline execute, object relocation, Beacon API, `__imp_` symbol, or loader compatibility failure.
Use it to separate three contracts that are often confused: compiler-emitted COFF, the loader's supported COFF subset, and the operator framework's argument/output ABI.
Do not treat a `.o` file as a DLL without headers; it has no normal image layout, import directory, TLS startup, CRT initialization, or automatic unwind registration.
A production result includes the object, an inspection transcript, a loader-capability manifest, deterministic argument fixtures, and repeat-invocation evidence.

## Completion standard

Pin the target architecture, compiler version, command line, object SHA-256, loader build, supported relocation set, supported Beacon APIs, argument grammar, and ownership of every allocation.
Reject unsupported object features explicitly instead of attempting best-effort execution.
For every accepted object, prove all file spans are bounds-checked before mapping and all relocations are checked before writing.
For every execution, capture return status, Beacon output records, exception or child-process status, allocation cleanup, and handle-count delta.
Treat a loader crash as a loader defect until a child-process harness proves the object itself violated the declared contract.

## Core workflow

### 1. Freeze the ABI and toolchain

Create an evidence directory and record exact tools before compiling:

```powershell
$Case = 'C:\lab\bof\case-001'
New-Item -ItemType Directory -Force "$Case\build", "$Case\evidence" | Out-Null
clang --version | Out-File "$Case\evidence\clang-version.txt"
llvm-readobj --version | Out-File "$Case\evidence\llvm-version.txt"
(Get-Command x86_64-w64-mingw32-gcc -ErrorAction SilentlyContinue).Source | Out-File "$Case\evidence\mingw-path.txt"
```

Declare one object/loader tuple, for example `AMD64 + clang 18 windows-gnu + loader-v3`.
Build x86 and x64 separately; never infer compatibility from the host process being WOW64-capable.
Keep the public entry contract minimal:

```c
#include <windows.h>
#define BOF_IMPORT __declspec(dllimport)
BOF_IMPORT void BeaconPrintf(int type, char *format, ...);
BOF_IMPORT void BeaconDataParse(void *parser, char *buffer, int size);
BOF_IMPORT int  BeaconDataInt(void *parser);
void go(char *args, int len) {
    BeaconPrintf(0, "argument bytes: %d", len);
}
```

Match declarations to the loader's header, including calling convention and parser layout; do not copy a header from a different framework fork without diffing it.
Avoid constructors, C++ exceptions, thread-local storage, locale, environment-dependent CRT calls, and writable global state unless the loader manifest explicitly supports them.

Compile reproducibly with one of these pinned flows:

```powershell
x86_64-w64-mingw32-gcc -c "$Case\src\module.c" -o "$Case\build\module.x64.o" -Os -Wall -Wextra -fno-asynchronous-unwind-tables -fno-ident -fno-stack-protector -ffunction-sections -fdata-sections
clang --target=x86_64-w64-windows-gnu -c "$Case\src\module.c" -o "$Case\build\module.clang.x64.o" -Os -Wall -Wextra -fno-asynchronous-unwind-tables -fno-stack-protector -ffunction-sections -fdata-sections
clang --target=i686-w64-windows-gnu -c "$Case\src\module.c" -o "$Case\build\module.clang.x86.o" -Os -Wall -Wextra -fno-asynchronous-unwind-tables -fno-stack-protector -ffunction-sections -fdata-sections
Get-FileHash "$Case\build\*.o" -Algorithm SHA256 | Format-Table -AutoSize | Out-File "$Case\evidence\object-hashes.txt"
```

Do not strip the object before compatibility inspection; the symbol and relocation tables are the loader's input.

### 2. Inspect before execution

Generate independent views of headers, sections, symbols, relocations, and disassembly:

```powershell
llvm-readobj --file-headers --sections --symbols --relocations "$Case\build\module.x64.o" | Out-File "$Case\evidence\readobj.txt"
llvm-objdump -dr --section-headers --syms "$Case\build\module.x64.o" | Out-File "$Case\evidence\objdump.txt"
dumpbin /headers /symbols /relocations /disasm "$Case\build\module.x64.o" | Out-File "$Case\evidence\dumpbin.txt"
```

Confirm `Machine` equals `IMAGE_FILE_MACHINE_AMD64` or `IMAGE_FILE_MACHINE_I386` as declared.
Confirm `SizeOfOptionalHeader` is zero for an ordinary object and reject files that attempt to smuggle an image-style optional header into an unsupported path.
Inventory every section characteristic and classify it as code, read-only data, writable data, uninitialized data, directive metadata, unwind data, or unknown.
Inventory every undefined external and map it to exactly one resolver namespace: Beacon shim, `DLL$Function`, loader extension, weak external, or rejected symbol.
Flag `.drectve`, `.tls$*`, `.CRT$*`, `.pdata`, `.xdata`, COMDAT, weak externals, common symbols, and associative sections; support must be explicit, not accidental.
Search for unwanted compiler helpers such as `__chkstk`, security-cookie routines, exception helpers, `memcpy`, or floating-point helpers and either provide a tested shim or change the source/compiler flags.

A static admission check should fail closed:

```powershell
$Report = Get-Content "$Case\evidence\readobj.txt" -Raw
$Forbidden = @('__CxxFrameHandler', '__security_check_cookie', '_tls_used', '.CRT$', '.drectve')
$Hits = $Forbidden | Where-Object { $Report -match [regex]::Escape($_) }
if ($Hits) { throw "Unsupported COFF features: $($Hits -join ', ')" }
```

The admission list belongs to the loader release, not to a single BOF repository.

### 3. Parse COFF with checked arithmetic

Read the file into an immutable byte span and use `offset <= size` plus `length <= size - offset` for every range check.
Never validate `offset + length <= size` with unchecked integer addition.
Parse the 20-byte `IMAGE_FILE_HEADER`, then locate the section table at `20 + SizeOfOptionalHeader`.
Check `NumberOfSections * sizeof(IMAGE_SECTION_HEADER)` for multiplication overflow before taking the section-table span.
For each 40-byte `IMAGE_SECTION_HEADER`, validate `PointerToRawData/SizeOfRawData` and `PointerToRelocations/NumberOfRelocations * 10` independently.
Treat a zero raw-data pointer as valid only for an uninitialized section with an understood allocation size.
Locate the symbol table using `PointerToSymbolTable` and `NumberOfSymbols * 18`; auxiliary records count toward `NumberOfSymbols`.
Locate the string table immediately after the last 18-byte symbol record; its first four bytes are a little-endian size that includes the size field itself.
Reject string offsets below four, offsets outside the string table, and names lacking a terminating NUL before the table end.
Reject section indexes outside `1..NumberOfSections` except the documented undefined, absolute, and debug values.
Advance over `NumberOfAuxSymbols` atomically; never reinterpret an auxiliary record as a new primary symbol.
Apply a configured ceiling to sections, symbols, relocations, and total mapped bytes before allocating.

### 4. Build a deterministic mapped layout

Assign each admitted section an aligned runtime base in one private allocation or in separately tracked allocations.
Use the greater of initialized bytes and the loader-declared zero-fill requirement, then zero the full mapped span before copying raw bytes.
Keep a table containing section index, name, file span, mapped base, mapped size, alignment, characteristics, and final protection.
Do not trust object `VirtualAddress` as a PE RVA; derive the loader layout and record it.
Resolve a defined symbol as `mapped_section[symbol.SectionNumber - 1] + symbol.Value` after checking the value lies in the admitted section extent.
Handle undefined symbols only through the resolver; a nonzero-value common symbol requires explicit common-storage support or rejection.
Honor COMDAT selection and associative relationships only if implemented and tested; otherwise reject all COMDAT sections before mapping.

Use writable, non-executable memory while copying and relocating:

```text
VirtualAlloc(NULL, total_size, MEM_RESERVE | MEM_COMMIT, PAGE_READWRITE)
copy and zero sections
resolve symbols into a separate immutable resolution table
apply checked relocations
VirtualProtect each page range to its final W^X protection
FlushInstructionCache(GetCurrentProcess(), code_base, code_size)
```

If page-sharing forces code and writable data onto one page, change the layout or keep the object rejected; do not silently leave RWX pages.

### 5. Resolve imports without name ambiguity

Normalize only decorations the loader contract explicitly permits.
On AMD64, a compiler import reference commonly names `__imp_BeaconPrintf` or `__imp_KERNEL32$GetLastError`.
The relocation usually targets an import slot containing the function address, so the symbol value is the address of that slot, not the function address itself.
Store resolver slots in stable memory that outlives object execution.
Split `DLL$Function` at the first dollar sign only after validating both components against the loader grammar.
Use a fixed allowlist of DLL basenames and exported functions for test hosts; do not accept paths, ordinals, forwarded-name recursion, or API-set guesses accidentally.
Load system libraries with a constrained search policy such as `LoadLibraryExW(name, NULL, LOAD_LIBRARY_SEARCH_SYSTEM32)`.
Resolve forwarded exports through the Windows loader and record the final module/version during compatibility tests.
For x86, account for leading underscores and stdcall suffixes only through a declared compiler profile; do not strip arbitrary punctuation until a lookup succeeds.
Unknown Beacon APIs and loader extensions are hard errors with the unresolved symbol included in the result.

### 6. Apply relocations exactly

Each `IMAGE_RELOCATION` is 10 bytes: section-relative `VirtualAddress`, `SymbolTableIndex`, and relocation `Type`.
Validate that the patch width fits the mapped section and that the symbol index names a primary record rather than an auxiliary record.
Read and write unaligned little-endian values with `memcpy`-style helpers rather than undefined C pointer casts.
Let `S` be the resolved symbol address, `A` the encoded addend, and `P` the runtime address of the relocation field.
For `IMAGE_REL_AMD64_ADDR64`, write `S + A` after unsigned overflow validation.
For `IMAGE_REL_AMD64_ADDR32` and `ADDR32NB`, prove the result fits 32 bits; define the synthetic image-base convention for `ADDR32NB` or reject it.
For `IMAGE_REL_AMD64_REL32`, write signed 32-bit `S + A - (P + 4)` and reject displacement overflow.
For `REL32_1` through `REL32_5`, subtract the additional encoded bias: `S + A - (P + 4 + N)`.
For `IMAGE_REL_AMD64_SECTION` and `SECREL`, emit the mapped section ordinal or section-relative offset according to the COFF specification and loader layout.
For i386 `DIR32`, write `S + A`; for i386 `REL32`, write `S + A - (P + 4)`, both with width checks.
Reject every relocation type not named by the loader-capability manifest.
After relocation, disassemble the mapped code in the harness and compare each branch/call target with the symbol-resolution ledger.

Record relocation evidence as:

```text
object_sha256, section, section_offset, relocation_type, symbol_name,
encoded_addend, resolved_symbol, computed_value, patch_width, validation_result
```

This ledger is the offset methodology: file offsets come from checked COFF spans, runtime offsets come from the loader's section map, and no address is reused across object builds without re-derivation.

### 7. Implement the Beacon compatibility surface

Implement only APIs required by admitted objects and version the shim table.
At minimum, common compatibility sets include `BeaconDataParse`, `BeaconDataInt`, `BeaconDataShort`, `BeaconDataLength`, `BeaconDataExtract`, `BeaconPrintf`, and `BeaconOutput`.
Mirror the framework's packed-argument byte order exactly; common BOF packers encode integer and length fields in network byte order and include precise string-length conventions.
Keep golden fixtures generated by the actual operator-side packer and parse them in the native harness.
Reject negative lengths, truncated fields, integer wrap, missing string terminators where required, and reads beyond the original argument buffer.
Return extracted pointers into the immutable argument buffer only for the declared invocation lifetime.
Represent output as typed records containing callback type, byte length, raw bytes, decoded view, timestamp, and invocation ID.
Preserve embedded NUL bytes for `BeaconOutput`; do not route binary output through `printf`.
Bound total output and individual records so a module cannot exhaust the host.
Document unsupported asynchronous, token, format, and loader-specific APIs rather than returning fake success.

### 8. Invoke and contain

Resolve `go` as an external defined symbol with executable-section provenance; reject duplicate or undefined entries.
Switch final page protections before invocation and call `FlushInstructionCache`.
Run untrusted development objects in a short-lived child process so access violations, stack corruption, and deadlocks do not take down the test coordinator.
Pass the exact packed byte length; never derive it with `strlen`.
Use a job object with process-time and memory limits, redirected output transport, and kill-on-job-close behavior.
Inside a same-process compatibility test, use a native exception boundary appropriate to the compiler; C++ `catch` alone does not contain Windows access violations.
Capture process exit code, exception code/address, module-relative fault offset, thread stacks, output records, and timeout reason.
A timeout ends by terminating the isolated child, not by abandoning a loader thread in a long-lived process.

### 9. Release in reverse ownership order

Wait for loader-owned callbacks and output drains to finish before releasing object memory.
Zero sensitive argument and output buffers when their classification requires it.
Release resolver slots, mapped sections, library references owned by the invocation, parser state, output state, and file bytes exactly once.
Compare process handle count, private bytes, and loaded-module set before and after repeated calls.
Run at least 100 sequential invocations of the same object and 100 alternating successful/failing fixtures.
A successful execution with monotonically growing handles or memory is a failed loader test.

## Key structures & interfaces

`IMAGE_FILE_HEADER` selects machine, section count, symbol-table location, symbol count, optional-header size, and characteristics.
`IMAGE_SECTION_HEADER` supplies the object file spans, relocation spans, counts, and section characteristics; image RVA assumptions do not apply.
`IMAGE_SYMBOL` is an 18-byte packed record with short/long name union, value, signed section number, type, storage class, and auxiliary count.
`IMAGE_AUX_SYMBOL` has context-dependent layouts for function definitions, weak externals, files, and section definitions; interpret it only from the parent symbol class/type.
`IMAGE_RELOCATION` binds a section-relative patch location to a symbol-table index and machine-specific relocation type.
`IMAGE_SYM_CLASS_EXTERNAL` usually covers `go` and imports; `STATIC` commonly covers section-local labels; unsupported storage classes must be rejected deliberately.
The loader's central interfaces should remain narrow:

```c
bool parse_coff(byte_span file, coff_view *out, load_error *err);
bool plan_layout(const coff_view *coff, loader_policy policy, layout *out, load_error *err);
bool resolve_symbol(const coff_view *coff, uint32_t index, resolver *r, uintptr_t *value, load_error *err);
bool apply_relocations(const coff_view *coff, layout *map, resolver *r, relocation_log *log, load_error *err);
run_result invoke_bof(layout *map, byte_span packed_args, output_sink *sink, run_policy policy);
void release_layout(layout *map);
```

Keep parser state immutable after admission; mapping and execution state should reference it without rewriting file bytes.
Keep loader policy machine-consumed: accepted machines, section ceiling, byte ceiling, relocations, storage classes, section flags, import namespaces, and shim version.

## Test harness and corpus

Maintain positive fixtures for x64/x86 calls, internal branches, data references, BSS, each supported relocation, every shim, binary output, empty arguments, and repeat invocation.
Maintain malformed fixtures for truncated headers, section-count overflow, raw-span overflow, relocation-span overflow, symbol-count overflow, bad auxiliary counts, bad string sizes, unterminated names, invalid section indexes, relocation into the final byte, auxiliary-symbol references, and signed displacement overflow.
Maintain policy fixtures for wrong architecture, duplicate `go`, missing `go`, unresolved import, forbidden DLL, COMDAT, TLS, exception tables, common storage, and unknown relocation types.
Mutate one field at a time from a known object and retain the original SHA-256, mutation offset, old/new bytes, expected rejection stage, and actual result.
Run parser fuzzing without execution first; only admitted objects enter the isolated execution harness.
Use Application Verifier or page heap on the native test host and collect dumps on first-chance corruption indicators.
Compare loader output across compiler versions only when source, arguments, and shim version are held fixed.

## Tooling

- `llvm-readobj`: authoritative scriptable COFF headers, symbols, auxiliary records, and relocations.
- `llvm-objdump`: relocation-annotated disassembly and section/symbol cross-check.
- `dumpbin`: independent Microsoft parser and MSVC object view.
- MinGW-w64 GCC and LLVM/clang: produce controlled i386/AMD64 COFF variants.
- WinDbg: child-process exceptions, mapped instruction targets, heap/handle leaks, and call ABI verification.
- GFlags/Application Verifier: heap misuse and handle diagnostics in the loader host.
- PE/COFF specification: relocation, symbol, auxiliary, COMDAT, and section semantics; pin the revision in evidence.
- A native loader host plus a separate coordinator: never use the operational agent process as the first test surface.

## Pitfalls & OPSEC

- A BOF runs inside its host's identity and lifetime; one crash, deadlock, `ExitProcess`, or global-state corruption can destroy the enclosing agent.
- Compiler upgrades can add helper symbols, unwind sections, or relocation types even when source is unchanged; inspect every build artifact.
- Name-stripping heuristics can resolve the wrong function and hide ABI mismatches; resolver behavior must be deterministic.
- Leaving all sections RWX is not compatibility; it hides layout defects and creates avoidable telemetry.
- Calling loader APIs while holding global loader locks or executing from callbacks can deadlock; document invocation context.
- Threads, timers, callbacks, APCs, and overlapped I/O that outlive object memory are use-after-free conditions; forbid them or add explicit lifetime joins.
- Variadic formatting is an ABI boundary; malformed format strings and width mismatches can corrupt the host even when relocations are correct.
- Packed arguments may contain credentials or binary material; do not log raw buffers by default, and classify evidence before retention.
- Dynamic library loading and function resolution are observable; record telemetry in the lab rather than claiming memory-only execution is invisible.
- Do not test malformed objects in a production C2 process; use an isolated child and a disposable lab endpoint.
- Treat object and loader hashes as a pair in reports; results from one loader fork do not establish compatibility with another.

## Routing

- Use `windows-rpc-com-attack` when a BOF calls or audits RPC, COM/DCOM, or ALPC rather than when the issue is COFF loading.
- Use `windows-telemetry-etw` to measure provider, event, stack, loss, and consumer behavior caused by the loader or module.
- Use `hyper-v-offensive` for Hyper-V hypercalls, VMBus, worker processes, or HCS boundaries.
- Use `linux-kernel-exploitation` for a Linux kernel primitive; BOF/COFF mechanics are Windows-specific.
- Use `c2-implant-engineering` for the task protocol, job lifecycle, module ABI integration, cancellation, transport, update, and long-lived implant behavior.
- Use `ebpf-offensive` for verifier/JIT or eBPF hook research, not Windows COFF loading.
- Use `linux-host-post-exploitation` for evidence-driven operation on an authorized Linux shell.
- Use `pe-tools` for PE reconstruction, import directories, manual mapping, and normal image-loader semantics.
- Use `offensive-shellcode` for raw position-independent shellcode and custom reflective payloads.
- Use `exploit-dev` after a memory-corruption primitive is confirmed; this skill only establishes a safe object/module runtime.
- Use `edr-bypass-re` for a product/build-pinned visibility hypothesis after `windows-telemetry-etw` establishes sensor health.

## Final gate

- [ ] Architecture, compiler, object hash, loader hash, policy version, and shim version are recorded.
- [ ] Every file span and count uses checked arithmetic before dereference or allocation.
- [ ] Sections, symbols, auxiliary records, imports, and relocations pass an explicit admission policy.
- [ ] Relocation targets are logged and width/range checked against the mapped layout.
- [ ] Final protections are W^X-compatible and the instruction cache is flushed.
- [ ] Golden argument fixtures and typed output capture match the actual operator-side ABI.
- [ ] Malformed corpus, isolated crash handling, timeout handling, and 100-run cleanup tests pass.
- [ ] Unsupported features fail closed with a stable machine-consumed error.
