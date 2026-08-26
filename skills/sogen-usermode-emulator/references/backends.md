# Sogen CPU backends and vmtrace

| Backend | Host | Speed | Notes |
|---|---|---|---|
| Unicorn | Win/Linux/macOS/browser | slow | default portable; good for hooks |
| icicle-emu | same | medium | Rust emulator; `momo5502` forks it |
| WHP | Windows with Hypervisor Platform | native-ish | enable `Microsoft-Hyper-V-Hypervisor` + `HypervisorPlatform`; reboot |
| KVM | Linux `/dev/kvm` | native-ish | |
| FEX | Arm64 Linux | x86-on-arm | `momo5502/FEX` fork |

## vmtrace (WHP library)

`https://github.com/momo5502/vmtrace` — static C++20 library. Map host pages into a guest, start from a supplied CPU state, handle page R/W/X traps, CPUID, and syscalls in usermode.

```
cmake -S . -B build -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build build
.\build\vmtrace_demo.exe
```

Demo must print intercepted CPUID leaf 0 and syscall `0x1234`. Consume via `find_package(vmtrace)`. Use vmtrace when you want a *tiny* WHP guest (shellcode, syscall ABI experiment). Use Sogen when you want a real PE with ntdll.

## Levo (AOT cousin)

`momo5502/levo`: Ghidra CFG → Remill lift → LLVM recompile. Experimental. If the job is AOT rewrite rather than emulate, start at `llvm-lift-deobfuscation` and treat Levo as a pipeline sketch, not a product.
