---
name: pwn-chain
description: |
  Full-chain engineering methodology for going from reverse engineering to a working exploit.
  Applicable scenarios: you have the binary + the vulnerability + the target environment, and need to write an exploit that reliably gets through (not a script that only reproduces locally once and crashes the moment it goes remote).
  Covers three major directions: stack overflow / heap exploitation / kernel pwn. Emphasizes the engineering gap between "CTF passes locally → real remote stable exploitation": libc version mismatch, heap spray timing, SMEP/SMAP/KASLR, stack alignment, remote buffering.
  Core toolchain: pwntools + GEF/pwndbg + ROPgadget/Ropper + one_gadget + libc-database + qemu-system kernel debugging.
  Trigger keywords: pwn, stack overflow, heap overflow, ROP, ret2libc, ret2csu, one_gadget, libc-database, heap exploitation, tcache, fastbin, unsorted bin, kernel pwn, kROP, SMEP, SMAP, KASLR, modprobe_path, pwntools, GEF, pwndbg.
---

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: Read `../field-journal/precedent-reverse.md` — confirm this skill's operations are pre-authorized routine operations
2. `NOW`: Confirm whether the current task falls within this skill's scope
3. `NEXT`: Read `../tool-index.md`, validate tool availability and actual paths
4. `NEXT`: If tools are missing, invoke bootstrap — do not guess paths
5. `ACT`: Enter step one of the "Workflow" and execute; do not stop at a confirmation state

# From Vulnerability Point to Working Exploit (Pwn Chain)

## Scope

Use this skill when the task falls under these scenarios:

1. **Have binary + known vulnerability point** — static analysis/audit/fuzzing has already found an overflow/UAF/double free, and you need to go from trigger to shell
2. **CTF challenge already passes locally but fails remotely** — remote environment differences break the script, and stabilization is needed
3. **Binary exploitation of a real target** — in SRC / red team scenarios, a memory corruption vulnerability has been identified and an RCE needs to be built
4. **Linux kernel driver ioctl bug** — triggered from user mode, goal is privilege escalation to root

**Precondition**: You already know "where it blows up". This skill is not responsible for finding vulnerabilities (that is fuzzing / auditing); it only handles "turning a known vulnerability point into an exploit".

### Division of labor with other skills

| Scenario | Use what |
|------|--------|
| Identifying custom VM / anti-debug / complex obfuscation | `reverse-engineering/` |
| Opening a binary from scratch for static analysis | `ida-reverse/` or `radare2/` |
| **Have a vulnerability point, write an exploit to get through remotely** | **this skill** |
| Integrating the shell obtained via pwn into a complete attack chain | `attack-chain/` (downstream) |

`reverse-engineering/` focuses on "understanding what the program does" (pattern recognition, protocol recovery, solving the weird mechanisms in CTF challenges); this skill focuses on "turning an already-understood vulnerability into an executable attack". The two are often used together, but the division of labor is clear.

## Core Workflow

```text
Step 1: Confirm vulnerability type + mitigation mechanisms
   ├─ checksec ./vuln (NX / Canary / PIE / RELRO / Fortify)
   ├─ file ./vuln  + readelf -d ./vuln
   ├─ Vulnerability classification: stack overflow / format string / heap (UAF/DF/OF) / integer / race / kernel
   └─ → decide which references/ path to take

Step 2: Choose exploitation strategy
   ├─ NX off + no ASLR → direct shellcode
   ├─ NX on + libc provided → ret2libc / one_gadget
   ├─ NX on + no libc provided → leak then identify via libc-database
   ├─ Heap → technique per glibc version (tcache/fastbin/unsorted/large)
   └─ Kernel → commit_creds / modprobe_path / core_pattern

Step 3: Prepare libc + gadgets
   ├─ libc-database: ./find puts 0x6f0
   ├─ ROPgadget --binary ./libc.so.6 --only "pop|ret"
   ├─ one_gadget ./libc.so.6
   └─ compute base: leak_addr - libc.sym['puts']

Step 4: Write pwntools template (local process)
   ├─ context.binary = ELF('./vuln')
   ├─ p = process('./vuln')  /  p = gdb.debug('./vuln','b *main+xx')
   ├─ payload = cyclic(N) + p64(ret) + ...
   └─ p.interactive()

Step 5: Pass locally
   ├─ Repeatedly attach + inspect registers + tune offsets
   ├─ Use pwndbg/GEF's vmmap / heap / bins / telescope
   └─ Once it works, switch to remote()

Step 6: Remote stabilization
   ├─ libc offsets: identify via leak + libc-database, do not guess
   ├─ Stack alignment: 16-byte misaligned → movaps crash → add a ret gadget
   ├─ Remote network latency → recvuntil with precise anchor strings, ban fuzzy sleeps
   ├─ Remote buffering: sendlineafter is more stable than sendline
   ├─ Heap spray success rate: scale up spray count + keep padding chunks to prevent coalescing
   └─ Multiple runs: write while True to verify a success rate ≥ 95%
```

## Typical Scenarios

### Scenario 1: Remote 64-bit binary (NX+PIE+canary, libc provided)

```text
Have: ./vuln (64-bit ELF, NX, PIE, canary) + ./libc.so.6 + nc host port
Vulnerability: read(buf, 0x200) but buf is only 0x40 bytes → stack overflow
Mitigations: canary blocks it, PIE randomizes .text

Strategy:
1. First leak the canary (stack/format string/partial read)
2. Then leak a libc function address (puts@got)
3. Compute libc base with libc.address = leaked - libc.sym['puts']
4. one_gadget ./libc.so.6, pick a magic gadget whose constraints can be satisfied
5. payload = padding + canary + saved_rbp + (pop_rdi + bin_sh + system), or one_gadget directly
6. Add a ret gadget to fix stack alignment (critical!)
```

See `references/stack-pwn.md` for the full template.

### Scenario 2: Linux kernel driver ioctl out-of-bounds write → get root

```text
Have: vmlinux + bzImage + initramfs.cpio.gz + custom vuln.ko
Vulnerability: ioctl(0x1337, ptr) with controllable copy_from_user length → kernel heap overflow (kmalloc-64 slab)
Mitigations: SMEP, SMAP, KASLR, KPTI

Strategy:
1. Modify the init script to get a root shell (CTF), or first leak the KASLR base then continue (real)
2. Leak the kernel base via /proc/kallsyms (may be permission-restricted) or an uninitialized heap spray
3. Spray tty_struct / msg_msg / pipe_buffer in the kmalloc-64 slab
4. Overwrite the vtable pointer to point at user mode → won't work (SMEP), go with stack pivot + kernel ROP instead
5. ROP chain: prepare_kernel_cred(0) → commit_creds → swapgs+iretq → user-mode execve("/bin/sh")
6. Or more economical: overwrite modprobe_path with "/tmp/x", write a /tmp/x, then trigger modprobe
```

See `references/kernel-pwn.md` for the full template.

## On-Demand Bootstrap

### Tool dependencies

| Tool | Purpose | Installation |
|------|------|---------|
| pwntools | exploit writing framework | `pip install pwntools` |
| GEF | gdb enhancement (recommended for kernel + user mode) | `git clone https://github.com/bata24/gef` (actively maintained fork) |
| pwndbg | gdb enhancement (best heap debugging experience) | `git clone https://github.com/pwndbg/pwndbg && ./setup.sh` |
| ROPgadget | gadget search | `pip install ropgadget` |
| Ropper | gadget search (alternative, supports more architectures) | `pip install ropper` |
| one_gadget | libc magic gadget finder | `gem install one_gadget` (requires ruby) |
| libc-database | libc fingerprint lookup | `git clone https://github.com/niklasb/libc-database && ./get` |
| qemu-system-x86_64 | kernel challenge debugging | `apt install qemu-system-x86` |
| binwalk / cpio | initramfs unpacking | `apt install binwalk cpio` |
| patchelf | switching libc versions | `apt install patchelf` |

### Bootstrap check script

```bash
# One-shot check + install of core tools
for t in pwntools ropgadget ropper; do
  pip show $t >/dev/null 2>&1 || pip install $t
done

command -v one_gadget >/dev/null || gem install one_gadget

[ -d ~/tools/libc-database ] || git clone https://github.com/niklasb/libc-database ~/tools/libc-database
[ -d ~/tools/libc-database/db ] || (cd ~/tools/libc-database && ./get ubuntu debian)

[ -d ~/tools/pwndbg ] || (git clone https://github.com/pwndbg/pwndbg ~/tools/pwndbg && cd ~/tools/pwndbg && ./setup.sh)
```

### After the same tool fails to auto-install 2 times

Stop retrying; output structured manual installation steps (pip mirror / gem mirror / domestic git mirror / apt mirror) and ask the user to confirm.

## Routing Context

**Upstream entry points**: `skills/SKILL.md` (master control), `routing.md`
**Trigger condition**: have a binary + identified vulnerability point, need to write an exploit

**Upstream skills (use them first, then return to this skill)**:
- Haven't yet understood what the binary does → `reverse-engineering/`
- Need detailed static analysis → `ida-reverse/`
- Quick reconnaissance to confirm architecture/mitigations → `radare2/`

**Downstream skills (after getting a shell)**:
- Integrate into a complete attack chain (lateral movement, privilege escalation, persistence) → `attack-chain/`

**Submodule navigation**:
- Stack exploitation (ret2libc / ret2csu / one_gadget / stack alignment) → `references/stack-pwn.md`
- Heap exploitation (tcache / fastbin / unsorted / large bin / FILE struct) → `references/heap-pwn.md`
- Kernel pwn (kROP / SMEP-SMAP bypass / KASLR leak / modprobe_path) → `references/kernel-pwn.md`

## Notes

- **Do not call it done just because it passes locally** — the local libc / ASLR / network environment all differ from remote; you must run 20+ consecutive times in remote mode to verify stability
- **The libc version must be confirmed** — identify it via leak + libc-database, do not assume it is the Ubuntu 22.04 default libc
- **Stack alignment is a common 64-bit pitfall** — `movaps xmm0, [rsp]` segfaults when rsp is not 16-byte aligned; add an empty `ret` gadget to fix it
- **Heap exploitation is extremely sensitive to the glibc version** — tcache was introduced in 2.27, safe-linking in 2.32, hooks were removed in 2.34; each version has a different exploitation path
- **Kernel pwn must first confirm cpu flags** — whether the qemu launch arguments include +smep +smap +pku directly determines how to write the ROP chain
- **One KASLR leak is enough** — once you have one kernel address, all other addresses are computed as offsets; do not leak repeatedly

## Task Completion Self-Check (MUST pass before claiming completion)

- [ ] Did I execute every step of the workflow (rather than just reading it)?
- [ ] Did I use real tool paths based on `tool-index`?
- [ ] Did I produce reproducible evidence (commands/scripts/screenshots/reports)?
- [ ] Did I complete and write back the Checklist items required by RULES?
