---
name: manual-map-injector-engineering
description: "Stealth/manual-map injector engineering from real injector lineages: LoadLibrary vs NtCreateThreadEx vs APC vs hijack vs kernel map; W^X; SEC_IMAGE; WOW64."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: stealth
  triggers:
    - "manual map injector"
    - "Sastasha"
    - "Xenos"
    - "kdmapper"
    - "stealth injector"
---

# Manual-map / stealth injector engineering

Synthesizes patterns from injector trees (Sastasha-class, Xenos/Blackbone-class, Xenox options, kdmapper-style driver delivery).

## Technique matrix
| Method | Pros | Cons |
|---|---|---|
| LoadLibrary | simple | module list artifact |
| NtCreateThreadEx | flexible | start-address heuristics |
| Thread hijack | no new thread object | race/suspend artifacts |
| APC | stealthy if alertable | delivery constraints |
| Manual map | no module list | private RX / stack walks |
| Kernel map / APC | powerful | driver trust + PG |

## Hardening checklist (payload delivery)
- Architecture detect x64/WOW64
- Relocs, imports, delayed imports, TLS, exceptions/unwind
- Section protect final W^X (no long RWX)
- Optional header wipe / name unlink
- Least-privilege handles; transient opens
- File-backed `SEC_IMAGE` dual views when useful
- Call stack spoof on sensitive APIs (`anti-cheat-stack-walk-stealth`)

## Local corpora
- `Desktop/Injectors/Sastasha Injector v1.7*`
- `Desktop/Injectors/Xenos-master`, `Xenox v2.3.2`
- `Desktop/Injectors/kdmapper v3.0.1`
- Hypervisor-SVM / VEN / Milkyway trees as available

## Full manual-map sequence (correct order, or crashes)
```
1.  Parse PE (pefile on the tool side; payload-side: minimal parser)
2.  Allocate: SizeOfImage at preferred base (or ASLR-random anywhere)
    - allocation type: MEM_COMMIT|MEM_RESERVE; protect RW first
3.  Copy headers, then each section (SizeOfHeaders, section by section,
    honoring PointerToRawData vs VirtualAddress and VirtualSize > SizeOfRawData tail zeroing)
4.  Base relocations: apply delta to every BASE_RELOCATION block entry
    (type DIR64 for x64; skip ABSOLUTE)
5.  Import resolution: IAT walk — GetProcAddress via parent, or load-free:
    hash-resolve from ntdll/kernel32 export tables directly (API-set resolution:
    apiset host maps via `ApiSetSchema` — easier: link against ntdll only)
6.  Delayed imports (optional): resolve lazily or upfront
7.  TLS: TlsAlloc index, replicate ThreadLocalStoragePointer for calling thread,
    run TLS callbacks (DLL_PROCESS_ATTACH)
8.  Exception support: RtlAddFunctionTable(image, RUNTIME_FUNCTION count, base)
9.  Final protections: per-section R/RX/RW via NtProtectVirtualMemory (W^X)
10. Invoke entry (DllMain ATTACH or explicit init export) from:
    hijacked thread / APC / NtCreateThreadEx (spoofed start address)
11. Post-run hygiene: wipe headers (optional), wipe loader stub, close handles
```

## Import strategies ranked by stealth
| Strategy | Artifact | Use |
|---|---|---|
| Static link /CRT-less, ntdll-only | none extra | default for implants |
| Parent-process GetProcAddress into target IAT | IAT in private RX page | fine |
| Hash-resolved inside payload | no import table at all | shellcode-first design |
| LoadLibrary in-target for deps | module-list growth | avoid; if forced, load cold legit deps and free |

## Entry-invoke details that bite
- DllMain in loader lock: no LoadLibrary/sync/COM inside; keep init thin, spawn worker from init.
- WOW64: 32-bit target from 64-bit injector — either run a 32-bit helper process (heavily easiest) or use NtQueryInformationProcess ProcessWow64Information to walk 32-bit PEB; heaven's-gate both directions for syscalls (wow64 transitions via 0x33/0x23 code segments).
- CFG: `/guard:cf-` payload OR SetProcessValidCallTargets on every indirect target range (see stealth-injectors deep section).

## kdmapper-class driver delivery
- Vulnerable driver (iqvw64e/RTCore/etc., see `byovd`) exposes arbitrary physical write primitive → map driver:
  1. ZwCreateSection on driver file → physical addresses of image pages
  2. Allocate kernel pool (driver's own alloc primitive) → copy sections with relocs to kernel VA
  3. Resolve kernel imports via MmGetSystemRoutineAddress-equivalent through primitive
  4. Call DriverEntry via kernel thread spawn primitive or hijack
- Post-map: free/integrity-restore the vulnerable driver; scrub its service/prefetch traces.
- Ban-stack cost: BYOVD detection (Microsoft blocklist, EAC lists) — `eac-ban-stack`, `cheat-longevity-engineering`.

## SEC_IMAGE dual-mapping trick (file-backed stealth)
- `NtCreateSection(SEC_IMAGE)` on a payload file, map RX view in target; second RW mapping of same section object (different VAs) lets you mutate bytes without RWX — walk IAT through RW view, execute through RX view. Unlink from module list happens naturally if loaded via NtCreateSection from parent without LdrLoadDll.
- Detection: image-load callbacks only fire on LdrLoadDll path — this path is quiet; ETW-DLL-loads misses it too.

## Pair with
`stealth-injectors`, `pe-tools`, `driver-comm`, `anti-cheat-stack-walk-stealth`.
