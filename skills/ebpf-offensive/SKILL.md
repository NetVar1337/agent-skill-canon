---
name: ebpf-offensive
description: "Use when auditing or exploiting Linux eBPF verifier and JIT behavior, building CO-RE/libbpf research probes, exercising maps and ring buffers, attaching kprobe, fentry, LSM, cgroup, tc, or XDP programs, evaluating unprivileged BPF exposure, or enumerating and removing hidden BPF state."
license: MIT
---
# eBPF offensive research

Use this skill to research eBPF as two distinct surfaces: pre-privilege verifier/JIT/kernel attack surface, and controlled post-privilege instrumentation. Keep kernel exploitation claims separate from what a privileged BPF program can already do.

## When to use

Use this skill when the task involves:

- verifier acceptance, abstract-state confusion, helper lifetime, reference tracking, or state explosion;
- JIT translation, constant blinding, native-code differential behavior, or architecture-specific lowering;
- `bpf()` syscall commands, `union bpf_attr`, program/map/link types, helpers, kfuncs, or BTF;
- libbpf, CO-RE relocations, skeleton generation, bpffs pins, or `bpf_link` lifetime;
- kprobe/uprobe, tracepoint, fentry/fexit, LSM, cgroup, tc, XDP, iterator, or socket hooks;
- eBPF persistence/rootkit research in a disposable lab and the corresponding visibility/cleanup tests;
- enumeration of live programs, maps, links, attachments, JIT images, and pinned objects.

Route a resulting generic kernel read/write or control-flow primitive to `linux-kernel-exploitation`. Route broad corpus generation that is not BPF-specific to `offensive-fuzzing`.

## Completion standard

A completed case pins kernel and BPF policy, preserves the exact instruction/object input, captures full verifier and JIT evidence, distinguishes required privilege from impact, reproduces across a controlled kernel matrix, and proves cleanup of every program, map, link, pin, and hook.

## Core workflow

### 1. Freeze kernel, policy, and toolchain identity

Capture facts before loading a program:

```bash
uname -a
cat /etc/os-release
cat /proc/cmdline
bpftool version
clang --version
llc --version 2>/dev/null
cat /proc/sys/kernel/unprivileged_bpf_disabled 2>/dev/null
cat /proc/sys/net/core/bpf_jit_enable 2>/dev/null
cat /proc/sys/net/core/bpf_jit_harden 2>/dev/null
cat /proc/sys/net/core/bpf_jit_kallsyms 2>/dev/null
cat /sys/kernel/security/lockdown 2>/dev/null
```

Preserve the exact kernel image, config, `vmlinux`, modules, BTF blob, symbols, libbpf commit/version, bpftool build, clang/LLVM version, architecture, and boot line. Hash generated BPF ELF objects and raw instruction streams.

Record relevant configuration: `CONFIG_BPF`, `CONFIG_BPF_SYSCALL`, `CONFIG_BPF_JIT`, `CONFIG_BPF_JIT_ALWAYS_ON`, `CONFIG_BPF_UNPRIV_DEFAULT_OFF`, `CONFIG_DEBUG_INFO_BTF`, `CONFIG_DEBUG_INFO_BTF_MODULES`, `CONFIG_CGROUP_BPF`, `CONFIG_BPF_LSM`, `CONFIG_KPROBE_EVENTS`, `CONFIG_UPROBE_EVENTS`, `CONFIG_FTRACE`, `CONFIG_KASAN`, and `CONFIG_KCOV`.

Capability rules vary by kernel and operation. Record effective/permitted/ambient capabilities, user and network namespaces, LSM policy, seccomp, container context, and whether the operation relies on `CAP_BPF`, `CAP_PERFMON`, `CAP_NET_ADMIN`, or legacy `CAP_SYS_ADMIN` fallback.

### 2. Inventory the live BPF surface

Run privileged and unprivileged views separately when the case permits:

```bash
bpftool -j feature probe > feature.json
bpftool -j prog show > programs.before.json
bpftool -j map show > maps.before.json
bpftool -j link show > links.before.json
bpftool -j net show > net.before.json
bpftool -j cgroup tree > cgroups.before.json
find /sys/fs/bpf -xdev -printf '%y %m %u %g %p -> %l\n' > bpffs.before.txt 2>/dev/null
```

An empty unprivileged listing does not prove no BPF objects exist. Preserve permission errors and compare with an appropriately privileged view. Record bpffs mount namespace, tracefs/debugfs mounts, network namespace, cgroup hierarchy, and all pin paths.

For each program retain ID, tag, type, name, load time, UID, map IDs, BTF ID, JIT status/size, memlock, and attachment. For each map retain type, key/value sizes, maximum entries, flags, BTF key/value types, owner program, pins, and freeze state. For each link retain type, target, owning program, pin, and expected detach semantics.

### 3. Choose the research lane

State one lane before changing state:

| Lane | Starting privilege | Question |
| --- | --- | --- |
| Verifier/JIT vulnerability | Unprivileged or constrained caller | Can malformed bytecode violate verifier/JIT/kernel invariants? |
| Helper/kfunc lifetime | Type-specific load privilege | Can references, ownership, or sleepable context become inconsistent? |
| Hook instrumentation | Privileged operator | What can a valid program observe or alter at one hook? |
| Persistence/visibility | Privileged disposable lab | Which objects survive process exit or namespace changes, and who can enumerate them? |
| Defensive reconstruction | Read or dump access | What programs, maps, links, and attachments existed and what did they do? |

Do not report privileged policy manipulation as a privilege escalation. Do not describe an unavailable unprivileged path as exposed merely because the kernel contains the program type.

### 4. Model verifier state precisely

The verifier reasons about instructions, not C source. Preserve the final instruction stream after compiler optimizations and relocations. Track:

- registers `R0` through `R10`, scalar bounds, signed/unsigned ranges, and `tnum` known bits;
- pointer class, fixed/variable offset, object ID, nullability, and bounds;
- stack slot initialization, spilled pointer type, and 512-byte stack constraints;
- reference acquisition/release, dynptr/ring-buffer ownership, and callback boundaries;
- control-flow joins, state pruning, precision marks, loop bounds, subprogram calls, and tail calls;
- 32-bit versus 64-bit ALU semantics and sign/zero extension;
- helper/kfunc prototypes, BTF types, allowed program types, sleepable context, and RCU rules.

Capture the complete verifier log at a level sufficient to show the decisive state transition. Keep accepted and rejected controls that differ by one intentional property. A verifier rejection string alone does not explain root cause.

When reducing a case, preserve:

1. the same kernel outcome;
2. the same decisive verifier state;
3. the same privilege and program type;
4. the same JIT/interpreter setting;
5. one nearby negative control.

### 5. Build a minimal libbpf/CO-RE harness

Compile with debug and BTF information, then inspect before loading:

```bash
mkdir -p build
clang -O2 -g -target bpf -D__TARGET_ARCH_x86 \
  -c src/probe.bpf.c -o build/probe.bpf.o
llvm-objdump -S -r build/probe.bpf.o > build/probe.disasm.txt
bpftool btf dump file build/probe.bpf.o format raw > build/probe.btf.txt
bpftool gen skeleton build/probe.bpf.o > build/probe.skel.h
```

Pin the target architecture macro to the actual build. Preserve `.BTF`, `.BTF.ext`, maps, license, program sections, relocations, and libbpf loader logs.

CO-RE relocations depend on target BTF and field/type existence. Record each relocation outcome and distinguish a compile-time source assumption from a load-time CO-RE adaptation. Treat `bpf_core_field_exists` and flavor matching as explicit compatibility branches, not silent portability.

For verifier research, support a raw `BPF_PROG_LOAD` path that supplies exact `struct bpf_insn` bytes, program type, expected attach type, license, flags, log buffer, BTF IDs, and attach BTF ID. The ELF/libbpf path and raw-instruction path answer different questions.

### 6. Compare verifier, interpreter, and JIT outcomes

Where the kernel supports controlled toggling, preserve the original sysctl values and restore them after testing. Reboot when `BPF_JIT_ALWAYS_ON` or policy makes toggling invalid.

For a loaded program capture translated and native forms:

```bash
bpftool prog dump xlated id "$PROG_ID" opcodes linum > xlated.txt
bpftool prog dump jited id "$PROG_ID" opcodes linum > jited.txt
```

Compare:

- accepted/rejected result and verifier log;
- `BPF_PROG_TEST_RUN` output and context mutation where supported;
- interpreter versus JIT result on identical inputs;
- constant blinding and JIT hardening modes;
- emitted native bounds checks, speculation barriers, calls, tail calls, and exits;
- architecture-specific branch ranges, immediate materialization, and exception behavior.

Do not infer exploitable native code from a textual disassembly mismatch. Prove a semantic divergence, reachable memory-safety effect, or kernel invariant violation with a minimized input and control.

### 7. Investigate helper, map, and lifetime boundaries

For each helper or kfunc record:

- exact prototype and BTF type;
- permitted program/attach types and GPL restriction;
- pointer ownership before and after the call;
- null/error behavior and scalar bounds;
- RCU, lock, preemption, and sleepability context;
- reference release required on every exit path;
- kernel objects reached from map values, dynptrs, sockets, tasks, or local storage.

Map behavior is type-specific. Account for per-CPU values, LRU eviction, preallocation, mmap support, spin locks, timers, bloom filters, queues/stacks, ring buffers, map-in-map lifetime, object pinning, and freeze semantics.

For ring buffers, handle reservation failure, exact-size submission/discard, wakeup policy, consumer position, and lost-data accounting. A userspace consumer that cannot keep up is part of the experiment, not noise.

### 8. Attach one hook with explicit ownership

Select the narrowest hook matching the question:

| Hook | Strength | Constraint |
| --- | --- | --- |
| tracepoint/raw tracepoint | Defined event schema or low-overhead raw context | Schema/build and context differences |
| kprobe/kretprobe | Broad dynamic coverage | Unstable symbols, inlining, recursion, missed returns |
| fentry/fexit/fmod_ret | BTF-typed function boundary | BTF availability and attach permissions |
| uprobe/uretprobe | Userspace function observation | Exact inode/build/offset and process mapping |
| LSM | Security decision visibility/enforcement | LSM ordering, return chaining, sleepable rules |
| cgroup | Per-cgroup socket/syscall policy points | Correct hierarchy, attach flags, multi-attach semantics |
| tc/XDP | Packet path at chosen layer | Namespace/device, headroom, MTU, offload, return action |
| iterator | Structured kernel-object traversal | Supported iterator target and teardown |

Prefer `bpf_link`-based attachment because ownership and teardown are explicit. Document whether closing the link detaches, whether the link is pinned, and whether a legacy attach survives the loader process.

Before attaching, subscribe to the exact expected event or test signal. Trigger one controlled operation, collect one correlated record, then run a negative control. Avoid broad always-on hooks while developing the schema.

### 9. Separate post-root capability from stealth claims

In a disposable post-root lab, evaluate observation or policy behavior as a measured hook experiment:

- process/file/network event source and blind spots;
- map or ring-buffer command/state channel semantics;
- link and pin persistence across loader exit, namespace exit, service restart, and reboot;
- visibility from bpftool, procfs/sysfs, tracefs, audit, EDR, and memory acquisition;
- exact detach and object-release behavior.

Do not assume a renamed program, unpinned object, or absent bpftool output is hidden. IDs, tags, JIT memory, references, attachments, map contents, audit records, and kernel-memory artifacts may remain observable.

### 10. Fuzz and minimize deterministically

A verifier/JIT campaign records generator seed, instruction count, program type, flags, privilege, kernel hash, CPU, JIT mode, and timeout. Keep generation type-aware enough to reach deep verifier states, while retaining raw mutation for parser and boundary cases.

Useful harness paths include:

- syzkaller descriptions for `bpf()` commands and related object lifetimes;
- `BPF_PROG_LOAD` instruction generators with verifier-log capture;
- differential runs across adjacent kernel commits or architectures;
- `BPF_PROG_TEST_RUN` for repeatable context/data inputs;
- KASAN/KMSAN/KCOV kernels for diagnosis, followed by production-like confirmation.

Minimize on the earliest stable signal: verifier divergence, warning, KASAN report, lockdep report, JIT/interpreter mismatch, or deterministic wrong result. Preserve a bounded timeout tied to process completion or a kernel event; do not use arbitrary polling sleeps.

### 11. Verify cleanup and residual state

Inventory after every case with the same privileged namespace view used for the baseline:

```bash
bpftool -j prog show > programs.after.json
bpftool -j map show > maps.after.json
bpftool -j link show > links.after.json
bpftool -j net show > net.after.json
bpftool -j cgroup tree > cgroups.after.json
find /sys/fs/bpf -xdev -printf '%y %m %u %g %p -> %l\n' > bpffs.after.txt 2>/dev/null
```

Close loader FDs, destroy links, detach legacy hooks, remove owned pins, stop consumers, remove tc/XDP attachments, and unmount only mounts created by the case. Compare before/after IDs, tags, targets, and pins. Account for ID reuse by matching more than the numeric ID.

Restore sysctls, resource limits, network namespace/device state, cgroup attachments, qdiscs, and debug mounts. If a verifier/JIT case may have corrupted kernel state, restore the VM snapshot rather than trusting in-place cleanup.

## Key structures & interfaces

- `struct bpf_insn`: opcode, destination/source registers, signed offset, and immediate; wide immediates occupy two instruction slots.
- `union bpf_attr`: command-specific syscall contract for program, map, BTF, link, test-run, and ID operations.
- `bpf_prog`, `bpf_prog_aux`, `bpf_verifier_env`, `bpf_verifier_state`, and register state: verifier and loaded-program ownership.
- `bpf_map` and map-type implementations: key/value geometry, refcounts, memory accounting, callbacks, and pin ownership.
- `bpf_link` and legacy attachment APIs: durable relationship between program and target.
- BTF type graph plus `.BTF.ext`: function/line info and CO-RE relocation records.
- `bpf_object`, `bpf_program`, `bpf_map`, and `bpf_link` in libbpf: userspace ownership and error paths.
- perf event array and BPF ring buffer: output ordering, backpressure, loss, and consumer lifecycle.
- helpers, kfuncs, dynptrs, local storage, timers, and refcounted kernel pointers: type-specific lifetime boundaries.
- bpffs pins, program/map/link IDs, tags, and JIT images: live-state and forensic identities.

## Tooling

| Need | Tools |
| --- | --- |
| Feature and object inventory | `bpftool feature/prog/map/link/net/cgroup`, bpffs, tracefs |
| Build and CO-RE | clang/LLVM, libbpf, `bpftool gen skeleton`, BTFHub when provenance is pinned |
| Bytecode inspection | `llvm-objdump`, `bpftool prog dump xlated`, raw instruction dumper |
| JIT inspection | `bpftool prog dump jited`, GDB/crash on a lab kernel, architecture disassembler |
| Runtime observation | libbpf logs, `bpftool prog tracelog`, perf, trace_pipe, controlled consumers |
| Discovery | syzkaller, KCOV, KASAN, KMSAN, lockdep, targeted instruction generators |
| Kernel diffing | exact source trees, `git bisect`, config/build manifests, semantic diffing |
| Defensive reconstruction | bpftool JSON, audit logs, memory images, `drgn`, crash, `memory-forensics` |

## Evidence outputs

```text
target.md          kernel/config/BTF/toolchain/policy/capabilities and hashes
surface.before/    programs, maps, links, hooks, cgroups, pins, mount namespaces
case/              source, ELF, raw instructions, schemas, seed, verifier log
jit/               translated/native dumps, settings, inputs, differential result
runs.jsonl         kernel, privilege, JIT mode, expected/observed outcome, timing
surface.after/     matching inventory plus cleanup diff and residual explanation
```

Keep verifier logs and object files together; compiler source without final instructions is not a reproducible verifier case.

## Pitfalls & OPSEC

- Do not confuse privileged BPF policy control with a pre-privilege kernel exploit.
- Do not claim exposure without proving program type, attach type, capabilities, namespaces, sysctls, seccomp, lockdown, and LSM permit the path.
- Do not compare C source when verifier behavior depends on optimized instructions and relocations.
- Do not change JIT sysctls on a shared host; preserve prior values and use disposable kernels for unsafe cases.
- Do not assume verifier acceptance means safe runtime behavior or verifier rejection means no JIT/parser exposure.
- Do not trust empty unprivileged bpftool output as a complete inventory.
- Do not leak pointer-bearing verifier logs, map contents, BTF internals, or host identifiers into public artifacts.
- Avoid trace recursion, unbounded event output, and consumers that silently drop ring-buffer records.
- Pins and legacy attachments can outlive the loader; process exit is not cleanup proof.
- Program IDs can be reused; compare tags, type, load time, maps, BTF, and targets.
- Instrumented kernels change timing and layout; reproduce on a production-like target before impact claims.
- Snapshot rollback is mandatory after suspected JIT or kernel-memory corruption.

## Routing

- Route a proven generic kernel corruption, read/write, page-table, credential, or ROP primitive to batch-B sibling `linux-kernel-exploitation`.
- Route post-root Linux host enumeration, credential provenance, persistence assessment, or pivots to batch-B sibling `linux-host-post-exploitation`.
- Route custom agent use of eBPF telemetry or modules to batch-B sibling `c2-implant-engineering`.
- Route COFF/BOF modules to batch-A sibling `bof-coff-development`.
- Route Windows RPC, COM, DCOM, NDR, and ALPC trust boundaries to batch-A sibling `windows-rpc-com-attack`.
- Route Windows provider engineering and ETW/WPP/TraceLogging measurement to batch-A sibling `windows-telemetry-etw`.
- Route Hyper-V partitions, hypercalls, VMBus, VSP/VSC, and worker processes to batch-A sibling `hyper-v-offensive`.
- Route broad fuzzing infrastructure to `offensive-fuzzing`; route detection and artifact searches to `threat-hunting` or `memory-forensics`.

## Final gate

- [ ] Exact kernel, config, BTF, toolchain, BPF policy, capabilities, and namespaces are recorded.
- [ ] Raw instructions, ELF object, verifier log, program type, flags, and privilege reproduce the result.
- [ ] Verifier, interpreter, JIT, helper, and attachment claims are kept distinct.
- [ ] Positive and negative controls differ by one intentional property.
- [ ] Fuzzing and race tests use exact signals and bounded waits, not timing luck.
- [ ] Before/after inventories prove every program, map, link, pin, and hook was removed.
- [ ] Generic kernel impact is handed off only after the BPF-specific root cause is established.
