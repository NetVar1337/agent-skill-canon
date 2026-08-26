# tool-index — workstation RE/security tool inventory

> Generated 2026-08-24 from live probing (PATH + standard install locations). Any skill that says "tool paths are governed by tool-index.md" resolves against this file.
> Re-probe with the refresh snippet at the bottom. Unlisted tools = not installed or outside probe paths; MUST confirm manually and write the result back here.

## Decompilers / disassemblers / static analysis

| Tool | Status | Path / invocation |
|------|------|----------------|
| IDA Professional 9.4 | ✅ | `C:\Program Files\IDA Professional 9.4\ida64.exe`; not on PATH — use full path or add it |
| IDA headless (idat) | ✅ | Same dir, `idat64.exe`; idapython flows in the `idapython` skill |
| AiDAPrivate | ✅ | `C:\Users\Admin\Tools\AiDAPrivate\` (private IDA assistant toolkit) |
| radare2 6.2.0 | ✅ | `C:\Users\Admin\Tools\radare2\bin\r2.bat` (note: no `r2.exe` — use the .bat) |
| rabin2 / rasm2 / radiff2 / rafind2 | ✅ | Same dir, already on PATH |
| r2pm / r2agent / r2mcp | ✅ | Same dir; r2mcp MCP server covered in the `r2mcp-basic` skill |
| Ghidra 12.1.3 (×2) | ✅ | Canonical: `E:\Tools\ghidra\ghidra_12.1.3_PUBLIC\` (`ghidraRun`/`analyzeHeadless` shims on PATH via E:\Tools\bin; JAVA_HOME=E:\Tools\jdk-21\jdk-21.0.12.1+1). Legacy: `C:\Users\Admin\Tools\Ghidra\` (keeps ReVa extension) |
| ReVa MCP 7.3.0 | ✅ | Extension: `C:\Users\Admin\Tools\Ghidra\Ghidra\Extensions\reverse-engineering-assistant\`; stdio: `C:\Users\Admin\.local\bin\mcp-reva.exe` |
| ghidra-cli 0.2.2 | ✅ | `C:\Users\Admin\Tools\ghidra-cli\ghidra.exe`; user PATH entry configured |
| x64dbg/x32dbg 2026.05.27 | ✅ | `E:\Tools\x64dbg\release\x{32,64}\` — `x64dbg`/`x32dbg` shims |
| dnSpyEx 6.6.0 | ✅ | `E:\Tools\dnSpyEx\dnSpy.exe` — `dnspy` shim |
| DIE 3.21 | ✅ | `E:\Tools\DIE\die.exe` — `die` shim |
| PE-bear 0.7.2 | ✅ | `E:\Tools\PE-bear\` — `pebear` shim |
| pcileech v4.19.8 | ✅ | `E:\Tools\pcileech\pcileech.exe` — `pcileech` shim |
| Sysinternals suite (164) | ✅ | `E:\Tools\Sysinternals\` — procmon64/procexp64/autoruns64/tcpview shims |

## Debuggers

| Tool | Status | Path |
|------|------|------|
| WinDbg classic (windbg/cdb/kd/ntsd) | ✅ | `C:\Program Files (x86)\Windows Kits\10\Debuggers\x64\` (windbg.exe, cdb.exe, kd.exe, ntd.exe family) |
| WinDbgX (Store) | ❌ | Not installed; TTD requires the Store version or a manual install (`winget install Microsoft.WinDbg`) |
| Frida 17.17.0 | ✅ | pip package: `python -m frida_tools.repl`; scripts in `C:\Users\Admin\AppData\Local\Python\bin\` (frida, frida-ps, frida-trace not on PATH) |

## Build / compilers (driver and exploit development)

| Tool | Status | Path |
|------|------|------|
| Visual Studio 2022 Community | ✅ | `C:\Program Files\Microsoft Visual Studio\2022\Community\` (MSBuild/CL via VsDevCmd or a Developer Shell) |
| WDK | ✅ | Windows Kits 10: two sets, `10.0.26100.0` and `10.0.28000.0` (km headers present in both); driver projects target one of them |
| MSVC toolchain | ✅ | Same VS tree; no standalone PATH — go through vcvars |
| Rust 1.98.0 | ✅ | `C:\Users\Admin\.cargo\bin\` (cargo/rustc on PATH) |
| Go 1.27.0 | ✅ | `C:\Program Files\Go\bin\go.exe` |
| Node 24.19 + bun | ✅ | On PATH (node/npm, `C:\Users\Admin\.bun\bin\bun`) |
| CMake / Ninja | ✅ | CMake on PATH; ninja under `C:\Users\Admin\Tools\build-ninja\` |
| Python 3.14.7 | ✅ | On PATH (WindowsApps shim) + `C:\Users\Admin\AppData\Local\Python\` |

## Python RE libraries

**Primary env: `E:\Tools\pyenv` (Python 3.12.14, uv-managed) — `pre` shim = E:\Tools\pyenv\Scripts\python.exe**
pwntools · angr · capstone · unicorn · keystone-engine · yara-python · yara-x · pefile · lief · impacket · volatility3 · z3-solver · qiling · frida 17.17 · frida-tools · ropper · ROPgadget · dnfile (all import-verified 2026-08-24)
Legacy system pip: capstone 5.0.9 · frida 17.17.0 · frida-tools 14.10.4 · pefile 2024.8.26 · requests

## Mobile / Android

| Tool | Status | Path |
|------|------|------|
| adb (platform-tools) | ✅ | Installed via winget, on PATH |
| jadx 1.5.6 / apktool 3.0.3 | ✅ | `jadx`/`jadx-gui` shims (E:\Tools\jadx); apktool at `E:\Tools\apktool\apktool.cmd` |
| Il2CppDumper 6.7.46 | ✅ | `il2cppdumper` shim (E:\Tools\Il2CppDumper) |
| Dumper-7 v7.0.1 | ✅ | `E:\Tools\Dumper-7\Dumper-7.dll` (UE mod) |

## Local research workspaces (not tools — routing context)

| Dir | Contents |
|------|------|
| `C:\Users\Admin\Tools\eac-emu\` | EAC driver emulation/research lab |
| `C:\Users\Admin\Tools\byovd-hunt\` | BYOVD hunting: HWiNFO_x64.sys family, PawnIO + module sources, XTU installers |
| `C:\Users\Admin\Tools\Bin2BinLab\` | Binary diffing experiments |
| `C:\Users\Admin\Desktop\CVEs\ZDI-Submissions` | ZDI submission workspace (canonical root) |
| `C:\Users\Admin\Desktop\DriverHunt`, `VulnDrivers`, `Samples` | Driver 0-day / sample working dirs |
| `E:\Tools\git\momo5502` | Sogen / vmtrace / EPT-detect / momo HV (15 trees) |
| `E:\Tools\git\aftermathlabs` | ring-1.io / Voyager / VDM / vmp2 (21 trees) |
| `E:\Tools\git\xeroxz` | Bluepill / plouton / Mergen / qemu-anti-detect (17 trees) |
| `E:\Tools\git\hv-baselines` | SimpleVisor / HyperDbg / hvpp / VisualUefi (7 trees) |

## Explicitly missing (install on demand)

Cheat Engine (no silent installer — manual: https://github.com/cheatengine/cheat_engine/releases), yara CLI (yara-python covers), WinDbgX/TTD, fuzzing toolchain (AFL++/honggfuzz need WSL).
Wireshark + Nmap: winget-installed 2026-08-24 (own PATH entries). hashcat 6.2.6 `hashcat` shim. nuclei/subfinder/httpx/ffuf/sqlmap/Responder/Sliver/donut: E:\Tools shims (see E:\Tools\README.md).
After installing, MUST write the row back here with the install date.

## Refresh snippet

```bash
# tool-index.md maintenance: quick PATH re-probe
for t in ida64 r2.bat rabin2 frida adb cdb kd python node cargo go; do
  command -v $t >/dev/null 2>&1 && echo "OK  $t" || echo "MISS $t"
done
```
