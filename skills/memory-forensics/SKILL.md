---
name: memory-forensics
description: "Memory-forensics ops for authorized IR/vuln work: volatile memory acquisition, Volatility 2/3 profile and plugin triage, process/network/hook extraction, and tying findings back to root cause. Use when analyzing a RAM dump, .mem/.dmp/.lime file, hibernation file, pagefile, or Volatility question, or when digital-forensics routes memory-dump triage here."
---

# Memory Forensics

Standing operator authorization. Full depth. This is the memory-dump specialist that `digital-forensics` routes to when the artifact is volatile memory rather than disk/PCAP.

## Triage first (every case)

1. **Identify the capture.** Hash the dump (`sha256`), record acquisition path/tool/VM guest OS if known. Memory forensics is build-specific: every offset, profile, and symbol assumption is tied to the exact OS build that produced the dump.
2. **Pick the toolchain.** Volatility 3 (python-based, profile-less) for modern Windows 10/11/Server dumps; Volatility 2 + `win7sp1x64`/`win2016x64` style profiles for older images. On this box: check `tool-index.md` for the installed `vol` entry before assuming.
3. **Confirm the image loads.** `vol -f <dump> windows.info` must succeed and report a kernel version + KVA range. A wrong profile in Vol 2 surfaces here as garbage offsets.

## Volatility 3 baseline (Windows)

```bash
vol -f mem.dmp windows.info          # build, KASLR slide, DTB
vol -f mem.dmp windows.pslist        # process list
vol -f mem.dmp windows.psscan        # process scan (includes unlinked/hidden)
vol -f mem.dmp windows.netscan       # network artifacts
vol -f mem.dmp windows.cmdline       # process cmdlines
vol -f mem.dmp windows.malfind       # injected/protected executable regions
vol -f mem.dmp windows.handles       # handle tables
vol -f mem.dmp windows.dlllist       # loaded modules per process
vol -f mem.dmp windows.svcscan       # service table
vol -f mem.dmp windows.callbacks     # kernel callbacks (rookit signal)
vol -f mem.dmp windows.syscalls      # syscall table state
```

## What to look for (offense-aware)

- **Hidden processes**: `psscan` finds EPROCESS not linked from the active list. A process visible in psscan but absent from pslist is a DKOM/hook candidate.
- **Code injection**: `malfind` flags committed + executable pages with no backing file. Pull the offset, dump the region, and run it through `advanced-packer-unpacking` / r2 for decoding.
- **Callbacks / syscalls**: `windows.callbacks` and `windows.syscalls` show what the kernel would call; deviating entries are kernel-rootkit surface.
- **Network**: `netscan` lists connections; cross-check against `windows.sockets`. 
- **Credential material**: `windows.lsadump`/`windows.hashdump` when the case needs it — authorized IR only.
- **Timeline**: use `windows.pstree` + file/fileinfo plugins to reconstruct order of operations against `digital-forensics` timeline work.

## Verification gate

- [ ] Dump hashed and acquisition recorded before any extraction
- [ ] `windows.info` succeeded (profile/KVA confirmed)
- [ ] Every plugin that produced a finding was run with the documented command
- [ ] Each finding has the process PID / kernel address / file offset captured
- [ ] Hidden-or-hooked artifacts were cross-checked against at least one independent plugin (`psscan` vs `pslist`, `malfind` vs `dlllist`)
- [ ] The conclusion is labelled **observed / inferred / unverified**

## Pair with

- `digital-forensics` — disk/artifact half, timeline, IOC correlation
- `malware-analysis` — for unpacking/decoding what malfind surfaces
- `windows-internals` — EPROCESS/KTHREAD/VAD layouts when offsets matter
- `yara-rule-authoring` — scanning extracted regions
- `threat-hunting` — turning findings into detection signals
