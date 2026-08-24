---
name: pe-tools
description: "PE/COFF engineering: full header/dir parsing, rebuild (checksum, relocs, imports), manual-mapping helpers in the correct order, TLS/CFG/LoadConfig/exception dirs, memory-dump reconstruction, PDB reference extraction, Authenticode/catalog awareness. Use for any structural work on Windows binaries."
version: 2.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: re
---

# PE tools

Local tooling reality: `pefile` 2024.8.26 (Python) is installed; IDA 9.4 and
radare2 cover interactive work. Prefer pefile snippets for scripted analysis,
r2 (`rabin2 -I/-s/-i`) for quick recon — no need to invent parsers.

## Parse (what actually matters, by directory)

- **Headers**: validate `MZ`→`e_lfanew`→`PE\0\0`, machine, `NumberOfSections`,
  `SizeOfImage`, characteristics (DLL/EXE), `SizeOfOptionalHeader`. Record
  timestamp but never trust it (reproducible builds, delphi-style fakes).
- **Imports**: `OriginalFirstThunk` (ILT) vs `FirstThunk` (IAT). Bound imports
  (`TimeDateStamp != 0` in import descriptor) can make the ILT useless — fall
  back to hint/name recovery via the IAT and export tables. Delay imports:
  `__delayLoadHelper2` machinery, resolved lazily; a module with empty IAT but
  rich delay-load section is hiding nothing — it is just lazy.
- **Relocations**: `IMAGE_DIRECTORY_ENTRY_BASERELOC` blocks; types
  `IMAGE_REL_BASED_DIR64` (x64), `HIGHLOW` (x86), `ABSOLUTE` (padding).
  `RelocDir->Size == 0` + DLL → ASLR-less or already-rebased snapshot.
- **TLS**: callbacks run before entry — favorite anti-debug/early-inject spot.
  Dump `AddressOfCallBacks` array (null-terminated) always, even if
  `AddressOfIndex/StartAddress` look stubby.
- **Exception (.pdata/.xdata)**: per-function unwind info; the function table
  gives you clean function extents for x64 (better than heuristic chunking),
  and `UNW_FLAG_EHANDLER/UHANDLER` points at C-specific handlers — language
  fingerprinting for free.
- **Load config**: `GuardFlags` (CFG), `GuardCFFunctionTable` (valid call
  targets — a map of indirect-call surface), `SecurityCookie`, `DependentLoadFlags`,
  dynamic-relocation table (ARM64X/CHPE metadata on cross-arch binaries).
- **Debug**: `IMAGE_DEBUG_TYPE_CODEVIEW` RSD →
  `{PDB GUID, age}` → symbol-server path
  `srv*C:\symbols*https://msdl.microsoft.com/download/symbols` — recovering
  the exact PDB for the binary is a first-class recon step, not an afterthought.
- **Resources / version info**: build provenance; also a common overlay/
  signature carrier.

## Rebuild

- Recompute `OptionalHeader.CheckSum` (`CheckSumMappedFile` semantics) after
  any edit — some loaders/PPL paths verify.
- Section edits: update `SizeOfRawData` (file-aligned), `VirtualSize`,
  recompute `SizeOfImage` (section-alignment over last section),
  `NumberOfSections`; keep pointer math in raw-vs-virtual explicit.
- Append a section (when space in headers allows) rather than growing an
  existing one when injecting — fewer reloc/alignment surprises.
- After edits, re-run `Dumpbin /headers`-equivalent (rabin2 -S) and diff.

## Manual mapping order (the checklist that actually BSODs/works)

1. Map sections at `preferredBase + VirtualAddress` (allocate
   `MEM_COMMIT|MEM_RESERVE`, RW first).
2. Apply **relocations** for delta `actualBase − preferredBase`.
3. Resolve **imports**: walk ILT, write IAT. For APIs resolved dynamically
   by payload design, skip — but never leave a stale IAT entry the payload
   will call.
4. Protect sections per headers (`PAGE_EXECUTE_READ` etc.) last, after all
   writes (or you are patching RX pages).
5. **TLS callbacks** (if the payload legitimately uses TLS): execute before
   entry.
6. Call `DllMain(hinst, DLL_PROCESS_ATTACH, ...)`.
7. x64 exceptions: if the payload raises, RTL needs its function table —
   `RtlAddFunctionTable(.pdata entries mapped to actualBase)`. Missing this
   = crash inside any SEH/C++ throw, not at map time (nasty to debug).
8. Loader-lock hygiene: don't runDllMain of a mapped DLL inside
   `LdrLockLoaderLock` held contexts (inside DllMain of the host) unless
   you accept deadlocks.

## Memory-dump reconstruction

Dumping a module from a process (VAD/diskless mapping, unpacked image):

1. Read PE headers from the base; trust in-memory `SizeOfImage`.
2. `SizeOfHeaders` region then sections by VA from the VAD view.
3. Fix ups for a *re-loadable* file: rebuild import directory to names (from
   memory IAT, reverse via export tables), zero out bound-import timestamps,
   raw-offsets re-layout, recompute checksum. Perfect reconstruction is not
   always possible (IAT overwritten with resolved pointers without export
   recovery); document what was recovered vs inferred.
4. Tools: pefile round-trip for headers; `procdump -ma` / manual
   `MiniDumpWriteDump` for whole-process evidence (pair with
   `malware-analysis` for the analysis flow).

## Authenticode / catalog awareness

- Signature lives in `IMAGE_DIRECTORY_ENTRY_SECURITY` (blob after last
  section, `PointerToRawData` = file offset). Any byte edit invalidates it —
  re-sign or strip, don't ship broken signatures (broken ≠ unsigned and is
  itself a signal defenders key on).
- Driver signing on Win10/11: WHQL attestation vs test-signing; catalog-signed
  files have no embedded SIG dir — check via
  `WinVerifyTrust` with catalog policy, not by looking for the directory.
- Research framing for signature abuse (stomping transplants) belongs to
  `malware-analysis`/`edr-bypass-re`; here we only care about *validity state*
  and *signer identity* as recon facts.

## Quick recon snippets

```python
import pefile
pe = pefile.PE("target.sys", fast_load=True)
pe.parse_data_directories(directories=[
    pefile.DIRECTORY_ENTRY['IMAGE_DIRECTORY_ENTRY_IMPORT'],
    pefile.DIRECTORY_ENTRY['IMAGE_DIRECTORY_ENTRY_BASERELOC'],
    pefile.DIRECTORY_ENTRY['IMAGE_DIRECTORY_ENTRY_TLS'],
    pefile.DIRECTORY_ENTRY['IMAGE_DIRECTORY_ENTRY_DEBUG']])
print(pe.OPTIONAL_HEADER.CheckSum, hex(pe.OPTIONAL_HEADER.AddressOfEntryPoint))
for d in pe.DIRECTORY_ENTRY_DEBUG:
    if d.struct.Type == 2:  # CODEVIEW
        print(d.entry.PdbFileName.rstrip(b"\0"), d.entry.Signature_Guid, d.entry.Age)
```

```bash
~/Tools/radare2/bin/rabin2 -I -s -i -S target.exe   # info, symbols, imports, sections
```

## Pair with

`pattern-scanner` (post-parse locating), `offset-dumper` (schema layer),
`manual-map-injector-engineering` (mapping as an injection technique),
`malware-analysis` (packed samples), `binary-diff` (cross-build structure
comparison).
