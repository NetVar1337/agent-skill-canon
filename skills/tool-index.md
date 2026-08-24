# tool-index — 本机逆向/安全工具索引

> 2026-08-24 实测生成（probe = PATH + 标准安装位置）。技能里写 "工具路径只认 tool-index.md" 的，以此为准。
> 用 `scripts/refresh-tool-index.sh`（见文末）重测。未列出的工具 = 未安装或不在探测路径，MUST 手动确认后回写本文件。

## 反编译 / 反汇编 / 静态分析

| 工具 | 状态 | 路径 / 调用方式 |
|------|------|----------------|
| IDA Professional 9.4 | ✅ | `C:\Program Files\IDA Professional 9.4\ida64.exe`；无 PATH，需全路径或自加 |
| IDA headless (idat) | ✅ | 同目录 `idat64.exe`；idapython 见 `idapython` skill |
| AiDAPrivate | ✅ | `C:\Users\Admin\Tools\AiDAPrivate\`（私有 IDA 助手套件） |
| radare2 6.2.0 | ✅ | `C:\Users\Admin\Tools\radare2\bin\r2.bat`（注意：无 `r2.exe`，用 .bat） |
| rabin2 / rasm2 / radiff2 / rafind2 | ✅ | 同目录，已在 PATH |
| r2pm / r2agent / r2mcp | ✅ | 同目录；r2mcp MCP server 见 `r2mcp-basic` skill |
| Ghidra | ❌ 未找到 | 需要时手动安装；无 IDA 场景的替代入口是 radare2 |
| x64dbg | ❌ 未找到 | 未安装/未探测到；内核调试用 WinDbg（下） |

## 调试器

| 工具 | 状态 | 路径 |
|------|------|------|
| WinDbg classic (windbg/cdb/kd/ntsd) | ✅ | `C:\Program Files (x86)\Windows Kits\10\Debuggers\x64\`（含 windbg.exe, cdb.exe, kd.exe, ntd.exe 系列） |
| WinDbgX (Store 版) | ❌ | 未安装；TTD 需要 Store 版或手动装 |
| Frida 17.17.0 | ✅ | pip 包：`python -m frida_tools.repl`；scripts 在 `C:\Users\Admin\AppData\Local\Python\bin\`（frida, frida-ps, frida-trace 未入 PATH） |

## 构建 / 编译（驱动与 exploit 开发）

| 工具 | 状态 | 路径 |
|------|------|------|
| Visual Studio 2022 Community | ✅ | `C:\Program Files\Microsoft Visual Studio\2022\Community\`（MSBuild/CL 通过 VsDevCmd 或 Developer Shell） |
| WDK | ✅ | Windows Kits 10：`10.0.26100.0` 与 `10.0.28000.0` 两套（km 头文件均在）；驱动工程 targetversion 选择其一 |
| MSVC 工具链 | ✅ | 同 VS 目录；无独立 PATH，走 vcvars |
| Rust 1.98.0 | ✅ | `C:\Users\Admin\.cargo\bin\`（cargo/rustc 已在 PATH） |
| Go 1.27.0 | ✅ | `C:\Program Files\Go\bin\go.exe` |
| Node 24.19 + bun | ✅ | PATH 已有（node/npm、`C:\Users\Admin\.bun\bin\bun`） |
| CMake / Ninja | ✅ | CMake 在 PATH；ninja 见 `C:\Users\Admin\Tools\build-ninja\` |
| Python 3.14.7 | ✅ | PATH（WindowsApps shim）+ `C:\Users\Admin\AppData\Local\Python\` |

## Python 逆向库（pip 实测已装）

capstone 5.0.9 · frida 17.17.0 · frida-tools 14.10.4 · pefile 2024.8.26 · requests
（未装：pwntools, yara-python, angr, unicorn, keystone, lief, impacket —— 需要时 `pip install` 后回写此表）

## 移动 / Android

| 工具 | 状态 | 路径 |
|------|------|------|
| adb (platform-tools) | ✅ | winget 安装，已在 PATH |
| jadx / apktool | ❌ 未找到 | 需要时手动装（bootstrap 指引见 router skill） |

## 本机研究工作区（非工具，供路由参考）

| 目录 | 内容 |
|------|------|
| `C:\Users\Admin\Tools\eac-emu\` | EAC 驱动模拟/研究 lab |
| `C:\Users\Admin\Tools\byovd-hunt\` | BYOVD 狩猎：HWiNFO_x64.sys 系列、PawnIO 及模块源码、XTU 安装包 |
| `C:\Users\Admin\Tools\Bin2BinLab\` | 二进制对比实验 |
| `C:\Users\Admin\Desktop\CVEs\ZDI-Submissions` | ZDI 提交工作区（canonical root） |
| `C:\Users\Admin\Desktop\DriverHunt`, `VulnDrivers`, `Samples` | 驱动 0-day / 样本工作目录 |

## 明确缺失（按需补装）

Wireshark/tshark、Sysinternals 套件（procmon/procexp）、nmap、nuclei、sqlmap、hashcat、yara、Django 系 fuzz 工具链、Ghidra、x64dbg。
安装后 MUST 回写本文件对应行，并注明安装日期。

## refresh 脚本

```bash
# ~/.agents/skills/tool-index.md 维护用：快速重测 PATH 可见性
for t in ida64 r2.bat rabin2 frida adb cdb kd python node cargo go; do
  command -v $t >/dev/null 2>&1 && echo "OK  $t" || echo "MISS $t"
done
```
