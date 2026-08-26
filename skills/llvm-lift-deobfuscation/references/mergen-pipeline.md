# Mergen pipeline

Upstream: `https://github.com/NaC-L/Mergen` (`ARCHITECTURE.md`, `docs/BUILDING.md`).

## Build (Windows, preferred)

Needs CMake, Ninja, VS C++ headers, LLVM 18 (`LLVM_DIR`), Rust/Cargo for iced.

```bat
cmd /c scripts\dev\configure_iced.cmd
cmd /c scripts\dev\build_iced.cmd
python test.py quick
```

Outputs: `build_iced/lifter.exe`, `build_iced/rewrite_microtests.exe`.

Zydis-only backend is the fallback (`configure_zydis.cmd`). Docker image exports `/output/lifter`.

```bash
docker run --rm -v "$PWD":/work mergen /root/Mergen/build/lifter /work/target.exe 0x140001000
```

## Lift order

1. `createRuntimeImageContext` — PE validate
2. `createConfiguredLifterForRuntime` — load, parse exports, auto-outline export VAs
3. `configureDefaultMemoryPolicy` — SYMBOLIC default; CONCRETE for PE sections + stack; `stackReserve` in `[0x1000, 0x100000]`
4. `prepareRuntimePagedMemory`
5. `runSignatureStage`
6. Lift loop (`LiftDriver`)
7. `run_opts` fixpoint

`STACKP_VALUE = 0x14FEA0`. A `ret` whose RSP folds to that constant is the real function return (`ret rax`). Anything else is a ROP/continuation ret.

## run_opts

```
loop {
    O1
    GEPLoadPass              # fold concrete PE loads through memory-base GEPs
    ReplaceTruncWithLoadPass # trunc(load wide) -> load narrow (LE)
    PromotePseudoStackPass   # stack window -> alloca
    PromotePseudoMemory      # leftover memory GEPs -> inttoptr
} until instcount delta == 0
O2 once
```

`GEPLoadPass` must run before `PromotePseudoMemory` or image constants become opaque pointers.

## Control-flow shapes

- Direct jmp: iced Immediate* → RIP + simm → `solvePath`
- Indirect jmp: everything else; failure → `UnresolvedIndirectJump` in `output_diagnostics.json`
- Themida-virt `push IAT; ret`: if popped target is in `importMap`, collapse two stack slots, emit `call @import`, `br contVA`
- `--outline 0x...,0x...` plus PE exports: do not inline those VAs

## ABI reminder

Emitted:

```llvm
define i64 @main(i64 %rax, i64 %rcx, i64 %rdx, i64 %rbx, i64 %rsp, ...)
```

Microsoft x64 would be `rcx, rdx, r8, r9`. Hex-Rays of `opt.obj` looks “wrong” until you rewrite the signature.

## Tests

```bat
set CLANG_CL_EXE=C:\Program Files\LLVM\bin\clang-cl.exe
python test.py quick
python test.py vmp
```

VMP 3.8.x fixtures must finish with `blocks_completed > 0`. 3.6 is best-effort.

## GAMBA / Simplifier cheatsheet

```
python src/simplify_general.py "x+x" "a&a" -b 64 -z
python src/simplify.py "linear-mba"          # linear only (SiMBA)
Simplifier.exe "7471873370*~(y&c)+..." -b 64 -z
Simplifier.exe "x+y" -e                      # equality saturation; not the default
```
