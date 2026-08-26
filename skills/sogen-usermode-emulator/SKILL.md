---
name: sogen-usermode-emulator
description: "Use when running or extending momo5502 Sogen: syscall-level Windows/Linux userspace emulation with real ntdll/kernel32, Unicorn/icicle/WHP/KVM/FEX backends, GDB-invisible debug, snapshots, or DRM/malware detonation without a real OS. Distinct from Kevlar (kernel DriverEntry) and Qiling (API reimplementation)."
version: 1.0.0
license: MIT
metadata:
  package: local-operator-skills
  category: re
  author: Admin
  source: https://github.com/momo5502/sogen
  triggers:
    - sogen
    - momo5502 emulator
    - windows userspace emulator
    - syscall emulator
    - sogen.dev
---

# Sogen userspace emulator

momo5502 Sogen emulates at CPU + syscall, then runs the **real** system DLLs. It does not reimplement Win32. Kevlar is a kernel `DriverEntry` Unicorn harness (`kevlar-driver-emulation`). Qiling reimplements APIs. `vmtrace` is the WHP trap library Sogen can sit on.

Upstream: `https://github.com/momo5502/sogen` · demo `https://sogen.dev` · companion `momo5502/sogen-linux-files` · lecture `momo5502/drm-analysis`

## When this is the PRIMARY

- Detonate a PE/ELF that probes ntdll / PEB / SEH / registry and must see real Microsoft/glibc bytes
- Invisible debug (GDB/IDA RSP) from *outside* the guest
- Deterministic replay / snapshot around a DRM or packer check
- Swap CPU backends: Unicorn, icicle-emu, WHP, KVM, FEX

## Workflow

1. **Pick backend vs host.** See [references/backends.md](references/backends.md). WHP needs `Microsoft-Hyper-V-Hypervisor` + `HypervisorPlatform`. KVM needs `/dev/kvm`. Unicorn/icicle run anywhere, including the browser build.
   - Done when the chosen backend is installed and a `hello` PE/ELF exits 0.

2. **Give it a real sysroot.** Windows guests need a matching `ntdll.dll` / `kernel32.dll` / `user32.dll` tree (and wow64 if 32-bit). Linux guests need the `sogen-linux-files` rootfs. Do not stub those DLLs — the product is “real system DLLs.”
   - Done when the loader maps `ntdll` from the sysroot (log the path + file hash).

3. **Load like the OS.** Confirm PE relocs, TLS callbacks, section protect, reserved vs committed, SEH registration, and the initial thread. A crash in TLS before `main` is a loader bug, not a sample bug.
   - Done when RIP is in the image entry and PEB/TEB look like a real process.

4. **Hook the interesting surface.** Intercept syscalls, specific ntdll exports, instruction ranges, and memory R/W. Rewrite return values instead of patching guest `.text` when the sample hashes itself.
   - Done when a named syscall/API hook fired with logged args.

5. **Debug from the host.** Attach IDA/GDB to the GDB stub. Guest anti-debug (PEB.BeingDebugged, `NtQueryInformationProcess`, timing of `int 3`) must stay clean because the debugger is not in-process.
   - Done when a breakpoint hits without the sample’s anti-debug path firing.

6. **Snapshot around the check.** Full serialize + in-memory snapshot + minidump load. Restore, mutate one input, restore again. DRM lecture path: snapshot before the license VM, step with HyperDbg/Sogen, dump the slice into `llvm-lift-deobfuscation` / `virtualization-deobfuscation`.
   - Done when two restores produce identical RIP/regs before the mutation.

7. **GUI / GPU only if needed.** Native windows work. D3D8–11 goes through DXVK over the GPU paravirtual bridge; WHP is the fast CPU path. Do not turn this on for a headless malware run.
   - Done when the requested window presents, or the task is marked headless.

## Pair with

- `kevlar-driver-emulation` — `.sys` / `DriverEntry`, not usermode
- `vm-and-bytecode-reverse` / `virtualization-deobfuscation` / `llvm-lift-deobfuscation` — after you have a slice
- `hypervisor-dev` + `vmtrace` WHP API — write a custom trap guest
- `malware-analysis` — detonation policy
- `imgui-overlay` — not Sogen’s CEF `gameoverlay`

## Verification

- [ ] Backend named; hello guest exits 0
- [ ] Sysroot ntdll/kernel32 (or linux rootfs) hashed in the log
- [ ] TLS/entry reached with a sane PEB
- [ ] At least one syscall or export hook observed
- [ ] GDB/IDA attach does not set guest BeingDebugged
- [ ] Snapshot restore is bit-identical at the stated RIP
