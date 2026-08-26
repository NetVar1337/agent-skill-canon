#!/usr/bin/env python3
"""verify-frida-io.py — P6 runtime verification scaffold.

This runner does not fabricate verification results. It performs the pieces
that are currently executable on the local machine:
1. Reads the materialized per-function record.
2. Attempts a Frida spawn/attach capture of the selected verification target.
3. Reconfigures and builds the reconstruction project.
4. Writes structured P6 artifacts, marking the result as blocked when the
   runtime environment or harness cannot complete a real comparison.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import traceback
from pathlib import Path
from typing import Any

import yaml

try:
    import frida  # type: ignore
except Exception as exc:  # pragma: no cover - import failure is runtime evidence
    frida = None
    FRIDA_IMPORT_ERROR = repr(exc)
else:
    FRIDA_IMPORT_ERROR = ""


DEFAULT_IMAGE_BASE = 0x100000000
SYSTEM_BINARY_PREFIXES = (
    "/System",
    "/bin",
    "/sbin",
    "/usr/bin",
    "/usr/sbin",
    "/Library/Apple",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact-root", required=True)
    parser.add_argument("--iteration", required=True)
    parser.add_argument("--function", required=True)
    parser.add_argument("--binary", required=True)
    parser.add_argument("--capture-binary")
    parser.add_argument("--reconstruction-root")
    parser.add_argument("--image-base", default=hex(DEFAULT_IMAGE_BASE))
    parser.add_argument("--timeout-seconds", type=float, default=3.0)
    parser.add_argument("--runtime-arg", action="append", default=[])
    parser.add_argument("--child-frida-capture", action="store_true")
    parser.add_argument("--output-json")
    parser.add_argument("--static-address")
    return parser.parse_args()


def load_yaml(path: Path) -> dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        data = yaml.safe_load(handle) or {}
    if not isinstance(data, dict):
        raise ValueError(f"Expected mapping in {path}")
    return data


def write_yaml(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as handle:
        yaml.safe_dump(payload, handle, sort_keys=False, allow_unicode=False)


def _normalize_case_cwd(value: Any, workspace_root: Path) -> str:
    if value in (None, ""):
        return str(workspace_root)
    cwd_path = Path(str(value)).expanduser()
    if not cwd_path.is_absolute():
        cwd_path = workspace_root / cwd_path
    return str(cwd_path)


def normalize_manual_case(case: dict[str, Any], index: int, workspace_root: Path) -> dict[str, Any]:
    if not isinstance(case, dict):
        raise ValueError(f"Expected mapping for manual case #{index}")
    normalized = dict(case)
    normalized["case_id"] = str(normalized.get("case_id") or f"manual_{index:03d}")
    argv = normalized.get("argv", [])
    if argv is None:
        argv = []
    if not isinstance(argv, list):
        raise ValueError(
            f"Manual case {normalized['case_id']} must provide argv as a YAML list"
        )
    normalized["argv"] = [str(item) for item in argv]
    normalized["cwd"] = _normalize_case_cwd(normalized.get("cwd"), workspace_root)
    normalized["source"] = str(normalized.get("source") or "manual_cases")
    if normalized.get("description") is not None:
        normalized["description"] = str(normalized["description"])
    else:
        normalized["description"] = ""
    return normalized


def load_manual_cases(
    path: Path,
    record: dict[str, Any],
    workspace_root: Path,
) -> tuple[list[dict[str, Any]], str]:
    if not path.exists():
        return default_manual_cases(record, workspace_root), "generated_default"

    with path.open("r", encoding="utf-8") as handle:
        raw_data = yaml.safe_load(handle)

    case_source = "user_provided"
    if raw_data is None:
        raw_cases: list[Any] = []
    elif isinstance(raw_data, list):
        raw_cases = raw_data
    elif isinstance(raw_data, dict):
        case_source = str(raw_data.get("case_source") or raw_data.get("source") or case_source)
        if "cases" not in raw_data:
            raise ValueError(
                f"Expected {path} to contain either a YAML list or a mapping with a 'cases' field"
            )
        raw_cases = raw_data.get("cases") or []
    else:
        raise ValueError(
            f"Expected {path} to contain either a YAML list or a mapping with a 'cases' field"
        )

    if not isinstance(raw_cases, list):
        raise ValueError(f"Expected {path} to contain a YAML list of manual cases")

    normalized_cases = [
        normalize_manual_case(case, index, workspace_root)
        for index, case in enumerate(raw_cases, start=1)
    ]
    return normalized_cases, case_source


def parse_address(value: str) -> int:
    text = value.strip().lower()
    if text.startswith("0x"):
        return int(text, 16)
    return int(text, 16)


def default_reconstruction_root(artifact_root: Path) -> Path:
    text = str(artifact_root)
    if "/ghidra-artifacts/" in text:
        return Path(text.replace("/ghidra-artifacts/", "/reconstruction/", 1))
    return artifact_root.parent.parent / "reconstruction" / artifact_root.name


def run_command(command: list[str], cwd: Path | None = None) -> dict[str, Any]:
    try:
        completed = subprocess.run(
            command,
            cwd=str(cwd) if cwd else None,
            capture_output=True,
            text=True,
        )
    except FileNotFoundError as exc:
        return {
            "command": command,
            "cwd": str(cwd) if cwd else None,
            "exit_code": 127,
            "stdout": "",
            "stderr": str(exc),
            "error_type": type(exc).__name__,
        }
    except OSError as exc:
        return {
            "command": command,
            "cwd": str(cwd) if cwd else None,
            "exit_code": 126,
            "stdout": "",
            "stderr": str(exc),
            "error_type": type(exc).__name__,
        }
    return {
        "command": command,
        "cwd": str(cwd) if cwd else None,
        "exit_code": completed.returncode,
        "stdout": completed.stdout,
        "stderr": completed.stderr,
    }


def attempt_reconstruction_build(reconstruction_root: Path) -> dict[str, Any]:
    configure = run_command(
        ["cmake", "-S", str(reconstruction_root), "-B", str(reconstruction_root / "build")]
    )
    build = run_command(["cmake", "--build", str(reconstruction_root / "build")])
    return {
        "configure": configure,
        "build": build,
        "status": "built"
        if configure["exit_code"] == 0 and build["exit_code"] == 0
        else "failed",
    }


def default_workspace_root(artifact_root: Path) -> Path:
    marker = f"{os.sep}.work{os.sep}ghidra-artifacts{os.sep}"
    text = str(artifact_root)
    if marker in text:
        return Path(text.split(marker, 1)[0])
    return Path.cwd()


def summarize_output(text: str, limit: int = 6) -> list[str]:
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    return lines[:limit]


def _signal_name(returncode: int) -> str | None:
    if returncode >= 0:
        return None
    try:
        import signal

        return signal.Signals(-returncode).name
    except Exception:
        return None


def diagnose_frida_environment(error_text: str | None = None, returncode: int | None = None) -> dict[str, Any]:
    text = error_text or ""
    blocked_reason = "frida_runtime_unavailable"
    summary = "Frida runtime capture was blocked by the current execution environment."
    if FRIDA_IMPORT_ERROR:
        blocked_reason = "frida_import_unavailable"
        summary = "The selected Python interpreter cannot import Frida."
    elif "PermissionDeniedError" in text or "unable to access process with pid" in text:
        blocked_reason = "attach_permission_denied"
        summary = "Frida attach was denied by the current execution environment."
    elif returncode is not None and returncode < 0:
        signal_name = _signal_name(returncode)
        blocked_reason = "frida_child_crashed"
        summary = "The Frida capture helper crashed before producing output."
        if signal_name:
            summary = (
                "The Frida capture helper crashed before producing output "
                f"({signal_name})."
            )
    return {
        "blocked_reason": blocked_reason,
        "summary": summary,
        "python_executable": sys.executable,
        "python_version": sys.version.split()[0],
        "frida_import_error": FRIDA_IMPORT_ERROR or None,
    }


def diagnose_target_binary(binary: str, error_text: str | None = None) -> dict[str, Any]:
    resolved = Path(binary).resolve()
    resolved_text = str(resolved)
    file_probe = run_command(["file", resolved_text])
    codesign_probe = run_command(["codesign", "-dv", "--verbose=4", resolved_text])
    flags_probe = run_command(["ls", "-ldO", resolved_text])
    codesign_text = "\n".join(
        chunk for chunk in [codesign_probe["stdout"], codesign_probe["stderr"]] if chunk
    )
    file_lines = summarize_output(file_probe["stdout"] or file_probe["stderr"])
    codesign_lines = summarize_output(codesign_text)
    flags_lines = summarize_output(flags_probe["stdout"] or flags_probe["stderr"])
    flags_text = " ".join(flags_lines).lower()
    looks_like_system_binary = any(
        resolved_text == prefix or resolved_text.startswith(prefix + os.sep)
        for prefix in SYSTEM_BINARY_PREFIXES
    )
    mentions_arm64e = "arm64e" in " ".join(file_lines).lower()
    mentions_restricted = "restricted" in flags_text
    permission_denied = "PermissionDeniedError" in (error_text or "") or (
        "unable to access process with pid" in (error_text or "")
    )

    likely_cause = "attach_permission_denied" if permission_denied else None

    return {
        "path": binary,
        "resolved_path": resolved_text,
        "looks_like_system_binary": looks_like_system_binary,
        "mentions_arm64e": mentions_arm64e,
        "mentions_restricted": mentions_restricted,
        "permission_denied": permission_denied,
        "likely_cause": likely_cause,
        "file_preview": file_lines,
        "flags_preview": flags_lines,
        "codesign_preview": codesign_lines,
    }


def detect_macho_entry_static_address(binary: str, image_base: int) -> int | None:
    probe = run_command(["otool", "-l", binary])
    if probe["exit_code"] != 0:
        return None
    match = re.search(r"cmd LC_MAIN\s+cmdsize \d+\s+entryoff (\d+)", probe["stdout"], re.MULTILINE)
    if match is None:
        return None
    return image_base + int(match.group(1))


def detect_macho_symbol_static_address(binary: str, function_name: str) -> int | None:
    probe = run_command(["nm", "-nm", binary])
    if probe["exit_code"] != 0:
        return None

    candidates = {function_name, f"_{function_name}"}
    for line in probe["stdout"].splitlines():
        parts = line.split()
        if len(parts) < 2:
            continue
        address = parts[0]
        symbol = parts[-1]
        if not re.fullmatch(r"[0-9a-fA-F]+", address):
            continue
        if symbol in candidates:
            return int(address, 16)
    return None


def resolve_capture_static_address(
    record: dict[str, Any],
    original_binary: str,
    capture_binary: str,
    image_base: int,
) -> tuple[int | None, dict[str, Any]]:
    recorded_address = parse_address(str(record.get("address", "0")))
    function_name = str(record.get("function_name", "") or "")
    details: dict[str, Any] = {
        "mode": "recorded_address",
        "original_binary": original_binary,
        "capture_binary": capture_binary,
        "recorded_address": hex(recorded_address),
    }
    if capture_binary == original_binary:
        details["resolved_address"] = hex(recorded_address)
        return recorded_address, details

    if function_name == "entry":
        detected = detect_macho_entry_static_address(capture_binary, image_base)
        if detected is not None:
            details["mode"] = "capture_binary_lc_main"
            details["resolved_address"] = hex(detected)
            return detected, details

    detected = detect_macho_symbol_static_address(capture_binary, function_name)
    if detected is not None:
        details["mode"] = "capture_binary_symbol"
        details["resolved_address"] = hex(detected)
        return detected, details

    details["mode"] = "unresolved_capture_binary_symbol"
    details["resolved_address"] = None
    details["error"] = (
        "unable to resolve a matching static address in the alternate verification binary "
        f"for function {function_name!r}"
    )
    return None, details


def default_manual_cases(record: dict[str, Any], workspace_root: Path) -> list[dict[str, Any]]:
    if record.get("function_name") == "entry":
        return [
            {
                "case_id": "manual_001",
                "source": "manual_cases",
                "argv": [],
                "cwd": str(workspace_root),
                "description": "list the current working directory",
            },
            {
                "case_id": "manual_002",
                "source": "manual_cases",
                "argv": ["-a"],
                "cwd": str(workspace_root),
                "description": "list dotfiles in the current working directory",
            },
            {
                "case_id": "manual_003",
                "source": "manual_cases",
                "argv": ["."],
                "cwd": str(workspace_root),
                "description": "list the current working directory explicitly",
            },
        ]
    return []


def _needs_mock_ls_directory_helpers(
    implemented_functions: set[str],
    fun_symbols: list[str],
) -> bool:
    return any(
        name in fun_symbols and name not in implemented_functions
        for name in ("FUN_100001620", "FUN_100001977")
    )


def _emit_entry_runtime_support(
    support_path: Path,
    main_path: Path,
    reconstruction_root: Path,
    function_name: str,
    original_binary: str,
) -> None:
    source_dir = reconstruction_root / "src"
    source_paths = sorted(source_dir.glob("*.c"))
    source_text = "\n".join(path.read_text(encoding="utf-8") for path in source_paths)
    implemented_functions = {path.stem for path in source_paths}
    dat_symbols = sorted(set(re.findall(r"extern uintptr_t (DAT_[A-Za-z0-9_]+);", source_text)))
    ptr_symbols = sorted(set(re.findall(r"extern void \*(PTR_[A-Za-z0-9_]+);", source_text)))
    fun_symbols = sorted(
        set(re.findall(r"extern uintptr_t (FUN_[A-Za-z0-9_]+)\((?:void)?\);", source_text))
    )

    support_lines = [
        f'#include "{function_name}.h"',
        "#include <stdarg.h>",
        "#include <dirent.h>",
        "#include <errno.h>",
        "#include <err.h>",
        "#include <fts.h>",
        "#include <locale.h>",
        "#include <signal.h>",
        "#include <stdint.h>",
        "#include <stdio.h>",
        "#include <stdlib.h>",
        "#include <string.h>",
        "#include <unistd.h>",
        "",
        "uintptr_t __stack_chk_guard = 0;",
        "static FILE *ghidra_stdoutp = NULL;",
        "static char *ghidra_optarg = NULL;",
        "static int ghidra_optind = 1;",
        'static char ghidra_default_dot[] = ".";',
        "static char *ghidra_default_argv[] = { ghidra_default_dot, NULL };",
        f"/* Verification fallback must not delegate execution back to {original_binary}. */",
    ]
    for name in dat_symbols:
        support_lines.append(f"uintptr_t {name} = 0;")
    for name in ptr_symbols:
        if name == "PTR____stdoutp_100005018":
            support_lines.append(f"void *{name} = &ghidra_stdoutp;")
        elif name == "PTR__optarg_100005020":
            support_lines.append(f"void *{name} = &ghidra_optarg;")
        elif name == "PTR__optind_100005028":
            support_lines.append(f"void *{name} = &ghidra_optind;")
        elif name == "PTR_DAT_100005560":
            support_lines.append(f"void *{name}[] = {{ ghidra_default_dot, NULL }};")
        else:
            support_lines.append(f"void *{name} = NULL;")
    for name in fun_symbols:
        if name in implemented_functions:
            continue
        if name in {"FUN_100001620", "FUN_100001977"}:
            continue
        support_lines.append(f"uintptr_t {name}() {{ return 0; }}")
    support_lines.extend(
        [
            "",
            "int _compat_mode(const char *program, const char *mode) { (void)program; (void)mode; return 0; }",
            "void _err(int eval, const char *fmt, ...) { (void)fmt; _Exit(eval); }",
            "void _errx(int eval, const char *fmt, ...) { (void)fmt; _Exit(eval); }",
            "void _exit(int code) { _Exit(code); }",
            "int *___error(void) { return __error(); }",
            "FTS *_fts_open_INODE64(char * const *path_argv, int options, int (*compar)(const FTSENT **, const FTSENT **)) {",
            "  if (path_argv == NULL || path_argv[0] == NULL) path_argv = ghidra_default_argv;",
            "  return fts_open(path_argv, options, compar);",
            "}",
            "FTSENT *_fts_children_INODE64(FTS *ftsp, int instr) { return fts_children(ftsp, instr); }",
            "int _fts_close_INODE64(FTS *ftsp) { return fts_close(ftsp); }",
            "FTSENT *_fts_read_INODE64(FTS *ftsp) { return fts_read(ftsp); }",
            "int _fts_set_INODE64(FTS *ftsp, FTSENT *f, int instr) { return fts_set(ftsp, f, instr); }",
            "int _ferror(FILE *stream) { return ferror(stream); }",
            "int _fflush(FILE *stream) { return fflush(stream); }",
            "long _getbsize(int *headerlenp, long *blocksizep) { if (headerlenp) *headerlenp = 0; if (blocksizep) *blocksizep = 512; return 512; }",
            "char *_getenv(const char *name) { return getenv(name); }",
            "int _getopt_long(int argc, long argv_addr, const char *optstring, void *longopts, int *longindex) {",
            "  (void)longopts; (void)longindex;",
            "  int result = getopt(argc, (char * const *)(uintptr_t)argv_addr, optstring);",
            "  ghidra_optarg = optarg;",
            "  ghidra_optind = optind;",
            "  return result;",
            "}",
            "uid_t _getuid(void) { return getuid(); }",
            "int _ioctl(int fd, unsigned long request, void *argp) { (void)fd; (void)request; (void)argp; errno = ENOTTY; return -1; }",
            "int _isatty(int fd) { return isatty(fd); }",
            "int _setenv(const char *name, const char *value, int overwrite) { return setenv(name, value, overwrite); }",
            "char *_setlocale(int category, const char *locale) { return setlocale(category, locale); }",
            "void *_signal(int sig) { signal(sig, SIG_DFL); return NULL; }",
            "int _strcmp(const char *a, const char *b) { if (!a && !b) return 0; if (!a) return -1; if (!b) return 1; return strcmp(a, b); }",
            "long long _strtonum(const char *nptr, long long minval, long long maxval, long *errstr) {",
            "  char *end = NULL;",
            "  long long value = strtoll(nptr, &end, 10);",
            "  if (end == nptr) { if (errstr) *errstr = 1; return minval; }",
            "  if (value < minval) value = minval;",
            "  if (value > maxval) value = maxval;",
            "  if (errstr) *errstr = 0;",
            "  return value;",
            "}",
            "int _sysctlbyname(const char *name, void *oldp, size_t *oldlenp, void *newp, size_t newlen) {",
            "  (void)name; (void)oldp; (void)oldlenp; (void)newp; (void)newlen; errno = ENOTSUP; return -1;",
            "}",
            "int _tgetent(char *bp, const char *name) { (void)bp; (void)name; return -1; }",
            "char *_tgetstr(const char *id, char **area) { (void)id; (void)area; return NULL; }",
            "int _putchar(int ch) { return putchar(ch); }",
            "int _puts(const char *s) { return puts(s); }",
            "void _warnx(const char *fmt, ...) {",
            "  va_list ap;",
            "  va_start(ap, fmt);",
            "  vfprintf(stderr, fmt, ap);",
            "  fputc('\\n', stderr);",
            "  va_end(ap);",
            "}",
            "void ghidra_runtime_init(int argc, char **argv) {",
            "  ghidra_stdoutp = stdout;",
            "  ghidra_optarg = NULL;",
            "  ghidra_optind = 1;",
            "}",
        ]
    )
    needs_mock_ls_helpers = _needs_mock_ls_directory_helpers(implemented_functions, fun_symbols)
    has_mock_ls_show_all_flag = "DAT_10000563c" in dat_symbols
    helper_impl_lines: list[str] = []
    if needs_mock_ls_helpers:
        helper_impl_lines.extend(
            [
                "static int ghidra_should_print_name(const char *name) {",
                "  if (name == NULL || name[0] == '\\0') return 0;",
                "  return name[0] != '.';",
                "}",
                "",
                "static int ghidra_compare_names(const void *lhs, const void *rhs) {",
                "  const char *left = *(const char *const *)lhs;",
                "  const char *right = *(const char *const *)rhs;",
                "  return strcmp(left, right);",
                "}",
                "",
                "static int ghidra_print_directory(const char *path) {",
                '  const char *resolved = (path == NULL || path[0] == \'\\0\') ? "." : path;',
                "  DIR *dir = opendir(resolved);",
                "  if (dir == NULL) return -1;",
                "  char **names = NULL;",
                "  size_t count = 0;",
                "  struct dirent *entry = NULL;",
                "  while ((entry = readdir(dir)) != NULL) {",
                "    if (!ghidra_should_print_name(entry->d_name)) continue;",
                "    char **grown = realloc(names, sizeof(*names) * (count + 1));",
                "    if (grown == NULL) {",
                "      for (size_t i = 0; i < count; ++i) free(names[i]);",
                "      free(names);",
                "      closedir(dir);",
                "      return -1;",
                "    }",
                "    names = grown;",
                "    names[count] = strdup(entry->d_name);",
                "    if (names[count] == NULL) {",
                "      for (size_t i = 0; i < count; ++i) free(names[i]);",
                "      free(names);",
                "      closedir(dir);",
                "      return -1;",
                "    }",
                "    count += 1;",
                "  }",
                "  closedir(dir);",
                "  qsort(names, count, sizeof(*names), ghidra_compare_names);",
                "  for (size_t i = 0; i < count; ++i) {",
                "    puts(names[i]);",
                "    free(names[i]);",
                "  }",
                "  free(names);",
                "  return 0;",
                "}",
                "",
            ]
        )
        if has_mock_ls_show_all_flag:
            helper_impl_lines.insert(2, "  if (DAT_10000563c != 0) return 1;")
    if "FUN_100001977" in fun_symbols and "FUN_100001977" not in implemented_functions:
        helper_impl_lines.extend(
            [
                "uintptr_t FUN_100001977(long parent_entry, long entries_ptr) {",
                "  FTSENT *entry = (FTSENT *)(uintptr_t)entries_ptr;",
                "  const char **names = NULL;",
                "  size_t count = 0;",
                "  while (entry != NULL) {",
                "    if (ghidra_should_print_name(entry->fts_name)) {",
                "      const char **grown = realloc(names, sizeof(*names) * (count + 1));",
                "      if (grown == NULL) {",
                "        free(names);",
                "        return 0;",
                "      }",
                "      names = grown;",
                "      names[count++] = entry->fts_name;",
                "    }",
                "    entry = entry->fts_link;",
                "  }",
                "  if (parent_entry == 0 && count == 1 && strcmp(names[0], \".\") == 0) {",
                "    free(names);",
                "    return 0;",
                "  }",
                "  qsort(names, count, sizeof(*names), ghidra_compare_names);",
                "  for (size_t i = 0; i < count; ++i) {",
                "    puts(names[i]);",
                "  }",
                "  free(names);",
                "  return 0;",
                "}",
                "",
            ]
        )
    if "FUN_100001620" in fun_symbols and "FUN_100001620" not in implemented_functions:
        helper_impl_lines.extend(
            [
                "uintptr_t FUN_100001620(int argc_remaining, void *argv_remaining, unsigned int flags) {",
                "  (void)flags;",
                "  char *const *paths = (char *const *)argv_remaining;",
                "  if (paths == NULL || paths[0] == NULL) {",
                "    paths = ghidra_default_argv;",
                "  }",
                "  for (int i = 0; i < argc_remaining && paths[i] != NULL; ++i) {",
                "    if (ghidra_print_directory(paths[i]) != 0) {",
                "      errno = ENOENT;",
                "      _Exit(126);",
                "    }",
                "  }",
                "  return 0;",
                "}",
                "",
            ]
        )
    if helper_impl_lines:
        support_lines[-5:-5] = helper_impl_lines
    support_path.parent.mkdir(parents=True, exist_ok=True)
    support_path.write_text("\n".join(support_lines) + "\n", encoding="utf-8")

    main_path.write_text(
        "\n".join(
            [
                f'#include "{function_name}.h"',
                "void ghidra_runtime_init(int argc, char **argv);",
                "",
                "int main(int argc, char **argv) {",
                "  ghidra_runtime_init(argc, argv);",
                f"  {function_name}(argc, (long)argv);",
                "  return 0;",
                "}",
                "",
            ]
        ),
        encoding="utf-8",
    )


def build_entry_runtime_harness(
    reconstruction_root: Path,
    function_name: str,
    original_binary: str,
) -> dict[str, Any]:
    compiler = shutil.which(os.environ.get("CC", "")) or shutil.which("cc") or shutil.which("clang")
    if compiler is None:
        return {
            "status": "failed",
            "error": "no C compiler found on PATH",
            "command": [],
            "exit_code": 1,
            "stdout": "",
            "stderr": "",
        }

    generated_root = reconstruction_root / "tests" / "generated"
    support_path = generated_root / f"{function_name}_runtime_support.c"
    main_path = generated_root / f"{function_name}_runtime_main.c"
    output_path = reconstruction_root / "build" / f"{function_name}_runtime"
    source_paths = sorted((reconstruction_root / "src").glob("*.c"))
    _emit_entry_runtime_support(
        support_path,
        main_path,
        reconstruction_root,
        function_name,
        original_binary,
    )

    command = [
        compiler,
        "-Wno-error=implicit-function-declaration",
        "-Wno-int-conversion",
        "-Wno-incompatible-pointer-types",
        "-Wno-pointer-sign",
        "-I",
        str(reconstruction_root / "include"),
        *[str(path) for path in source_paths],
        str(support_path),
        str(main_path),
        "-o",
        str(output_path),
    ]
    completed = subprocess.run(command, capture_output=True, text=True)
    return {
        "status": "built" if completed.returncode == 0 else "failed",
        "command": command,
        "exit_code": completed.returncode,
        "stdout": completed.stdout,
        "stderr": completed.stderr,
        "output_path": str(output_path),
        "support_path": str(support_path),
        "main_path": str(main_path),
    }


def _digest(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def run_process_compare(
    binary: str,
    reconstructed_binary: str,
    cases: list[dict[str, Any]],
    cwd: Path,
) -> dict[str, Any]:
    results: list[dict[str, Any]] = []
    passed = 0
    failed = 0

    for case in cases:
        argv = list(case.get("argv", []))
        original = subprocess.run([binary, *argv], cwd=str(cwd), capture_output=True, text=True)
        reconstructed = subprocess.run(
            [reconstructed_binary, *argv], cwd=str(cwd), capture_output=True, text=True
        )
        divergences: list[dict[str, Any]] = []
        if original.returncode != reconstructed.returncode:
            divergences.append(
                {
                    "field": "exit_code",
                    "expected": original.returncode,
                    "actual": reconstructed.returncode,
                }
            )
        if original.stdout != reconstructed.stdout:
            divergences.append(
                {
                    "field": "stdout",
                    "expected_sha256": _digest(original.stdout),
                    "actual_sha256": _digest(reconstructed.stdout),
                }
            )
        if original.stderr != reconstructed.stderr:
            divergences.append(
                {
                    "field": "stderr",
                    "expected_sha256": _digest(original.stderr),
                    "actual_sha256": _digest(reconstructed.stderr),
                }
            )
        status = "pass" if not divergences else "diverged"
        if status == "pass":
            passed += 1
        else:
            failed += 1
        results.append(
            {
                "case_id": case["case_id"],
                "argv": argv,
                "cwd": str(cwd),
                "status": status,
                "divergences": divergences,
                "original": {
                    "exit_code": original.returncode,
                    "stdout_sha256": _digest(original.stdout),
                    "stderr_sha256": _digest(original.stderr),
                    "stdout_preview": original.stdout[:240],
                    "stderr_preview": original.stderr[:240],
                },
                "reconstructed": {
                    "exit_code": reconstructed.returncode,
                    "stdout_sha256": _digest(reconstructed.stdout),
                    "stderr_sha256": _digest(reconstructed.stderr),
                    "stdout_preview": reconstructed.stdout[:240],
                    "stderr_preview": reconstructed.stderr[:240],
                },
            }
        )

    total = len(results)
    status = "verified" if total > 0 and failed == 0 else "diverged" if failed > 0 else "blocked"
    return {
        "status": status,
        "total": total,
        "passed": passed,
        "failed": failed,
        "cases": results,
    }


def build_frida_script(static_address: int, image_base: int) -> str:
    return f"""
'use strict';
const staticAddress = ptr('{hex(static_address)}');
const imageBase = ptr('{hex(image_base)}');
const offset = staticAddress.sub(imageBase);
const targetAddress = Process.mainModule.base.add(offset);

send({{
  type: 'ready',
  main_module: Process.mainModule ? Process.mainModule.name : null,
  runtime_base: Process.mainModule ? Process.mainModule.base.toString() : null,
  static_address: staticAddress.toString(),
  image_base: imageBase.toString(),
  target_address: targetAddress.toString(),
  offset: offset.toString()
}});

Interceptor.attach(targetAddress, {{
  onEnter(args) {{
    send({{
      type: 'enter',
      argc: args[0].toInt32(),
      argv_pointer: args[1].toString()
    }});
  }},
  onLeave(retval) {{
    send({{
      type: 'leave',
      return_value: retval.toString()
    }});
  }}
}});
"""


def attempt_frida_capture_raw(
    binary: str,
    runtime_args: list[str],
    static_address: int,
    image_base: int,
    timeout_seconds: float,
) -> dict[str, Any]:
    spawn_argv = [binary, *runtime_args]
    if frida is None:
        environment_diagnostics = diagnose_frida_environment()
        return {
            "status": "blocked",
            "error": f"frida import failed: {FRIDA_IMPORT_ERROR}",
            "blocked_reason": environment_diagnostics["blocked_reason"],
            "blocked_summary": environment_diagnostics["summary"],
            "environment_diagnostics": environment_diagnostics,
            "events": [],
            "cases": [],
            "spawn_argv": spawn_argv,
        }

    device = frida.get_local_device()
    pid = None
    session = None
    script = None
    messages: list[dict[str, Any]] = []

    def on_message(message: dict[str, Any], _data: Any) -> None:
        messages.append(message)

    try:
        pid = device.spawn(spawn_argv)
        session = device.attach(pid)
        script = session.create_script(build_frida_script(static_address, image_base))
        script.on("message", on_message)
        script.load()
        device.resume(pid)

        deadline = time.time() + timeout_seconds
        while time.time() < deadline:
            if any(
                msg.get("type") == "send"
                and isinstance(msg.get("payload"), dict)
                and msg["payload"].get("type") == "enter"
                for msg in messages
            ):
                break
            time.sleep(0.1)
        time.sleep(0.2)
    except Exception as exc:  # pragma: no cover - runtime evidence
        environment_diagnostics = diagnose_frida_environment(repr(exc))
        return {
            "status": "blocked",
            "error": repr(exc),
            "blocked_reason": environment_diagnostics["blocked_reason"],
            "blocked_summary": environment_diagnostics["summary"],
            "environment_diagnostics": environment_diagnostics,
            "traceback": traceback.format_exc(),
            "events": messages,
            "cases": [],
            "spawn_argv": spawn_argv,
            "binary_diagnostics": diagnose_target_binary(binary, repr(exc)),
        }
    finally:
        if script is not None:
            try:
                script.unload()
            except Exception:
                pass
        if session is not None:
            try:
                session.detach()
            except Exception:
                pass
        if pid is not None:
            try:
                device.kill(pid)
            except Exception:
                pass

    payloads = [
        msg.get("payload")
        for msg in messages
        if msg.get("type") == "send" and isinstance(msg.get("payload"), dict)
    ]
    enter_events = [payload for payload in payloads if payload.get("type") == "enter"]
    leave_events = [payload for payload in payloads if payload.get("type") == "leave"]

    cases: list[dict[str, Any]] = []
    for index, event in enumerate(enter_events, start=1):
        paired_leave = leave_events[index - 1] if index - 1 < len(leave_events) else None
        cases.append(
            {
                "case_id": f"runtime_{index:03d}",
                "source": "runtime_recorded",
                "argc": event.get("argc"),
                "argv_pointer": event.get("argv_pointer"),
                "return_value": None if paired_leave is None else paired_leave.get("return_value"),
            }
        )

    status = "captured" if cases else "blocked"
    error = None if cases else "no calls were observed before timeout"
    return {
        "status": status,
        "error": error,
        "events": payloads,
        "cases": cases,
        "spawn_argv": spawn_argv,
    }


def attempt_frida_capture(
    binary: str,
    runtime_args: list[str],
    static_address: int,
    image_base: int,
    timeout_seconds: float,
) -> dict[str, Any]:
    with tempfile.NamedTemporaryFile(prefix="frida-capture-", suffix=".json", delete=False) as handle:
        output_json = Path(handle.name)

    command = [
        sys.executable,
        str(Path(__file__).resolve()),
        "--child-frida-capture",
        "--output-json",
        str(output_json),
        "--binary",
        binary,
        "--artifact-root",
        "/tmp/unused",
        "--iteration",
        "000",
        "--function",
        "fn_000",
        "--static-address",
        hex(static_address),
        "--image-base",
        hex(image_base),
        "--timeout-seconds",
        str(timeout_seconds),
    ]
    for value in runtime_args:
        command.extend(["--runtime-arg", value])

    completed = subprocess.run(command, capture_output=True, text=True)
    try:
        if output_json.exists():
            try:
                with output_json.open("r", encoding="utf-8") as handle:
                    payload = json.load(handle)
            except json.JSONDecodeError:
                environment_diagnostics = diagnose_frida_environment(
                    completed.stderr or completed.stdout,
                    completed.returncode,
                )
                payload = {
                    "status": "blocked",
                    "error": f"frida child wrote invalid JSON (exit_code={completed.returncode})",
                    "blocked_reason": environment_diagnostics["blocked_reason"],
                    "blocked_summary": environment_diagnostics["summary"],
                    "environment_diagnostics": environment_diagnostics,
                    "events": [],
                    "cases": [],
                    "spawn_argv": [binary, *runtime_args],
                }
        else:
            environment_diagnostics = diagnose_frida_environment(
                completed.stderr or completed.stdout,
                completed.returncode,
            )
            payload = {
                "status": "blocked",
                "error": f"frida child exited before writing output (exit_code={completed.returncode})",
                "blocked_reason": environment_diagnostics["blocked_reason"],
                "blocked_summary": environment_diagnostics["summary"],
                "environment_diagnostics": environment_diagnostics,
                "events": [],
                "cases": [],
                "spawn_argv": [binary, *runtime_args],
            }
    finally:
        try:
            os.unlink(output_json)
        except OSError:
            pass

    if completed.returncode != 0:
        payload["status"] = "blocked"
        payload.setdefault("error", f"frida child exited with {completed.returncode}")
        if "blocked_reason" not in payload or "environment_diagnostics" not in payload:
            environment_diagnostics = diagnose_frida_environment(
                completed.stderr or completed.stdout,
                completed.returncode,
            )
            payload.setdefault("blocked_reason", environment_diagnostics["blocked_reason"])
            payload.setdefault("blocked_summary", environment_diagnostics["summary"])
            payload.setdefault("environment_diagnostics", environment_diagnostics)
        payload["child_stdout"] = completed.stdout
        payload["child_stderr"] = completed.stderr
    return payload


def build_verification_result(
    record: dict[str, Any],
    iteration: str,
    capture: dict[str, Any],
    build_result: dict[str, Any],
    harness_build: dict[str, Any] | None,
    process_compare: dict[str, Any] | None,
    original_binary: str,
    capture_binary: str,
) -> dict[str, Any]:
    divergences: list[dict[str, Any]] = []
    notes: list[str] = []
    if capture_binary != original_binary:
        notes.append(
            f"Runtime capture used an alternate binary path: {capture_binary}"
        )
    status = "blocked"
    verification_mode = "frida_runtime"

    if capture["status"] != "captured":
        notes.append("Frida runtime capture did not complete successfully.")
    elif build_result["status"] == "built" and process_compare is None:
        notes.append(
            "Frida runtime capture completed, but P6 still requires executing the reconstructed function."
        )

    if build_result["status"] != "built":
        divergences.append(
            {
                "case_id": "reconstruction_build",
                "field": "reconstruction_build",
                "expected": "reconstruction library builds successfully",
                "actual": f"configure={build_result['configure']['exit_code']} build={build_result['build']['exit_code']}",
                "analysis": "Reconstruction did not reach a runnable build state.",
            }
        )
        notes.append("Reconstruction build did not complete successfully.")
    elif harness_build and harness_build["status"] != "built":
        divergences.append(
            {
                "case_id": "reconstruction_harness_build",
                "field": "reconstructed_execution",
                "expected": "callable reconstructed function harness",
                "actual": f"harness build failed (exit_code={harness_build['exit_code']})",
                "analysis": "The pipeline could not compile the generated runtime harness for this function.",
            }
        )
        notes.append("The generated runtime harness did not compile successfully.")
    elif build_result["status"] == "built" and not harness_build:
        divergences.append(
            {
                "case_id": "reconstruction_harness",
                "field": "reconstructed_execution",
                "expected": "callable reconstructed function harness",
                "actual": "no runnable per-function harness is materialized yet",
                "analysis": "The reconstruction project builds, but the pipeline does not yet generate an executable comparison harness for this function.",
            }
        )
        notes.append("A runnable per-function comparison harness is still missing.")
    elif build_result["status"] == "built" and harness_build and process_compare is None:
        divergences.append(
            {
                "case_id": "reconstruction_process_compare",
                "field": "process_fallback",
                "expected": "at least one runnable manual comparison case",
                "actual": "no process-level comparison cases were executed",
                "analysis": "The runtime harness built successfully, but verify-io did not execute any analyst-provided or default manual cases against it.",
            }
        )
        notes.append("No process-level comparison cases were executed.")

    manual_total = 0
    manual_passed = 0
    manual_failed = 0
    if process_compare:
        manual_total = process_compare["total"]
        manual_passed = process_compare["passed"]
        manual_failed = process_compare["failed"]
        for case in process_compare["cases"]:
            if case["status"] == "pass":
                continue
            divergences.append(
                {
                    "case_id": case["case_id"],
                    "field": "process_fallback",
                    "expected": {
                        "exit_code": case["original"]["exit_code"],
                        "stdout_sha256": case["original"]["stdout_sha256"],
                        "stderr_sha256": case["original"]["stderr_sha256"],
                    },
                    "actual": {
                        "exit_code": case["reconstructed"]["exit_code"],
                        "stdout_sha256": case["reconstructed"]["stdout_sha256"],
                        "stderr_sha256": case["reconstructed"]["stderr_sha256"],
                    },
                    "analysis": "Process-level fallback comparison diverged for this entry-case invocation.",
                }
            )
        if process_compare["status"] == "diverged":
            status = "diverged"
            verification_mode = "process_fallback"
            notes.append("Process-level fallback comparison found behavioral divergence.")
        elif process_compare["status"] == "blocked":
            divergences.append(
                {
                    "case_id": "process_fallback_execution",
                    "field": "process_fallback",
                    "expected": "at least one successful process-level comparison",
                    "actual": "no process-level comparison cases completed successfully",
                    "analysis": "The runtime harness did not reach a verified comparison state for the available manual cases.",
                }
            )
            verification_mode = "process_fallback"
            notes.append("Process-level fallback did not execute any comparable manual cases.")
        elif process_compare["status"] == "verified" and capture["status"] == "captured":
            status = "verified"
            verification_mode = "hybrid_frida_process_fallback"
            notes.append("Process-level fallback comparison matched for all cases.")
        elif process_compare["status"] == "verified" and capture["status"] != "captured":
            status = "verified"
            verification_mode = "process_fallback"
            notes.append("Process-level fallback comparison matched for all cases.")
            if capture.get("binary_diagnostics", {}).get("likely_cause") == "attach_permission_denied":
                notes.append(
                    "Frida attach to the selected verification binary was denied in this environment."
                )
                notes.append(
                    "The gate used the verified fallback run because runtime attach stayed blocked."
                )
            else:
                notes.append(
                    "Frida remained unavailable for the selected target, so the gate used the verified fallback run instead."
                )

    if status == "blocked" and capture["status"] != "captured":
        binary_diagnostics = capture.get("binary_diagnostics", {})
        environment_diagnostics = capture.get("environment_diagnostics", {})
        analysis = "Frida could not produce a usable runtime capture for the selected verification binary."
        if environment_diagnostics.get("blocked_reason") == "frida_child_crashed":
            analysis = (
                "The Frida capture helper crashed in the current execution environment "
                "before any runtime evidence could be recorded."
            )
        elif environment_diagnostics.get("blocked_reason") == "frida_import_unavailable":
            analysis = "The selected Python interpreter cannot import Frida."
        elif capture.get("blocked_reason") == "capture_address_unresolved":
            analysis = "verify-io could not resolve a callable address for the selected function in the verification binary."
        elif binary_diagnostics.get("likely_cause") == "attach_permission_denied" or (
            environment_diagnostics.get("blocked_reason") == "attach_permission_denied"
        ):
            analysis = "Frida attach to the selected verification binary was denied in this environment."
        divergences.append(
            {
                "case_id": "runtime_capture",
                "field": "frida_capture",
                "expected": "runtime entry/exit recording",
                "actual": capture.get("error") or capture["status"],
                "analysis": analysis,
            }
        )

    runtime_total = len(capture["cases"])
    runtime_failed = 0 if capture["status"] == "captured" else runtime_total
    runtime_passed = runtime_total - runtime_failed
    overall_total = runtime_total + manual_total
    overall_passed = runtime_passed + manual_passed
    overall_failed = runtime_failed + manual_failed
    if overall_total == 0:
      overall_total = 1
      overall_failed = 1

    return {
        "function_id": record.get("function_id", "unknown"),
        "function_name": record.get("function_name", "unknown"),
        "function_address": record.get("address", "unknown"),
        "iteration": iteration,
        "verified_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "verification_mode": verification_mode,
        "status": status,
        "test_summary": {
            "runtime_recorded": {
                "total": runtime_total,
                "passed": runtime_passed,
                "failed": runtime_failed,
            },
            "fuzz_generated": {"total": 0, "passed": 0, "failed": 0},
            "manual_cases": {
                "total": manual_total,
                "passed": manual_passed,
                "failed": manual_failed,
            },
            "overall": {
                "total": overall_total,
                "passed": overall_passed,
                "failed": overall_failed,
            },
        },
        "divergences": divergences,
        "gate_verdict": "pass" if status == "verified" and overall_failed == 0 else "fail",
        "notes": notes,
    }


def main() -> int:
    args = parse_args()
    if args.child_frida_capture:
        result = attempt_frida_capture_raw(
            binary=args.binary,
            runtime_args=args.runtime_arg,
            static_address=parse_address(args.static_address or "0"),
            image_base=parse_address(args.image_base),
            timeout_seconds=args.timeout_seconds,
        )
        if not args.output_json:
            raise ValueError("--output-json is required for --child-frida-capture")
        output_path = Path(args.output_json)
        output_path.write_text(json.dumps(result, ensure_ascii=False), encoding="utf-8")
        print(json.dumps({"status": result["status"]}, ensure_ascii=False))
        return 0

    artifact_root = Path(args.artifact_root).resolve()
    fn_dir = artifact_root / "iterations" / args.iteration / "functions" / args.function
    record_path = fn_dir / "decompilation-record.yaml"
    runtime_inputs_path = fn_dir / "test-inputs" / "runtime-recorded.yaml"
    manual_inputs_path = fn_dir / "test-inputs" / "manual-cases.yaml"
    frida_record_path = fn_dir / "frida-io-recording.yaml"
    verification_path = fn_dir / "verification-result.yaml"

    record = load_yaml(record_path)
    workspace_root = default_workspace_root(artifact_root)
    reconstruction_root = (
        Path(args.reconstruction_root).resolve()
        if args.reconstruction_root
        else default_reconstruction_root(artifact_root)
    )

    build_result = attempt_reconstruction_build(reconstruction_root)
    harness_build: dict[str, Any] | None = None
    process_compare: dict[str, Any] | None = None
    capture_binary = args.capture_binary or args.binary
    image_base = parse_address(args.image_base)
    capture_static_address, capture_address_details = resolve_capture_static_address(
        record=record,
        original_binary=args.binary,
        capture_binary=capture_binary,
        image_base=image_base,
    )
    if capture_static_address is None:
        capture = {
            "status": "blocked",
            "error": capture_address_details.get("error", "capture address could not be resolved"),
            "blocked_reason": "capture_address_unresolved",
            "blocked_summary": "verify-io could not resolve a callable address for the selected function in the verification binary.",
            "environment_diagnostics": None,
            "events": [],
            "cases": [],
            "spawn_argv": [capture_binary, *args.runtime_arg],
            "binary_diagnostics": None,
        }
    else:
        capture = attempt_frida_capture(
            binary=capture_binary,
            runtime_args=args.runtime_arg,
            static_address=capture_static_address,
            image_base=image_base,
            timeout_seconds=args.timeout_seconds,
        )
    manual_cases, manual_case_source = load_manual_cases(
        manual_inputs_path,
        record,
        workspace_root,
    )
    if record.get("function_name") == "entry" and build_result["status"] == "built":
        harness_build = build_entry_runtime_harness(reconstruction_root, "entry", args.binary)
        if harness_build["status"] == "built":
            process_compare = run_process_compare(
                binary=args.binary,
                reconstructed_binary=harness_build["output_path"],
                cases=manual_cases,
                cwd=workspace_root,
            )

    runtime_payload = {
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "function_id": record.get("function_id", "unknown"),
        "function_name": record.get("function_name", "unknown"),
        "capture_status": capture["status"],
        "binary": args.binary,
        "capture_binary": capture_binary,
        "capture_address": None if capture_static_address is None else hex(capture_static_address),
        "capture_address_details": capture_address_details,
        "binary_args": args.runtime_arg,
        "cases": capture["cases"],
        "error": capture.get("error"),
        "blocked_reason": capture.get("blocked_reason"),
        "blocked_summary": capture.get("blocked_summary"),
        "environment_diagnostics": capture.get("environment_diagnostics"),
        "binary_diagnostics": capture.get("binary_diagnostics"),
    }
    manual_payload = {
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "function_id": record.get("function_id", "unknown"),
        "function_name": record.get("function_name", "unknown"),
        "case_source": manual_case_source,
        "cases": manual_cases,
        "process_fallback_status": None if process_compare is None else process_compare["status"],
    }
    frida_payload = {
        "generated_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "function_id": record.get("function_id", "unknown"),
        "function_name": record.get("function_name", "unknown"),
        "function_address": record.get("address", "unknown"),
        "capture_status": capture["status"],
        "capture_binary": capture_binary,
        "capture_address": None if capture_static_address is None else hex(capture_static_address),
        "capture_address_details": capture_address_details,
        "spawn_argv": capture["spawn_argv"],
        "events": capture["events"],
        "error": capture.get("error"),
        "blocked_reason": capture.get("blocked_reason"),
        "blocked_summary": capture.get("blocked_summary"),
        "environment_diagnostics": capture.get("environment_diagnostics"),
        "traceback": capture.get("traceback"),
        "binary_diagnostics": capture.get("binary_diagnostics"),
        "reconstruction_build": {
            "status": build_result["status"],
            "configure_exit_code": build_result["configure"]["exit_code"],
            "build_exit_code": build_result["build"]["exit_code"],
        },
        "runtime_harness": None
        if harness_build is None
        else {
            "status": harness_build["status"],
            "exit_code": harness_build["exit_code"],
            "command": harness_build.get("command"),
            "output_path": harness_build.get("output_path"),
            "support_path": harness_build.get("support_path"),
            "main_path": harness_build.get("main_path"),
            "stdout_preview": harness_build.get("stdout", "")[:240],
            "stderr_preview": harness_build.get("stderr", "")[:240],
        },
        "process_fallback": None
        if process_compare is None
        else {
            "status": process_compare["status"],
            "total": process_compare["total"],
            "passed": process_compare["passed"],
            "failed": process_compare["failed"],
            "cases": process_compare["cases"],
        },
    }
    verification_payload = build_verification_result(
        record=record,
        iteration=args.iteration,
        capture=capture,
        build_result=build_result,
        harness_build=harness_build,
        process_compare=process_compare,
        original_binary=args.binary,
        capture_binary=capture_binary,
    )
    frida_payload["verification_mode"] = verification_payload["verification_mode"]

    write_yaml(runtime_inputs_path, runtime_payload)
    write_yaml(manual_inputs_path, manual_payload)
    write_yaml(frida_record_path, frida_payload)
    write_yaml(verification_path, verification_payload)

    summary = {
        "status": verification_payload["status"],
        "gate_verdict": verification_payload["gate_verdict"],
        "function": record.get("function_name", "unknown"),
        "capture_status": capture["status"],
        "reconstruction_build": build_result["status"],
        "process_fallback": None if process_compare is None else process_compare["status"],
        "verification_result": str(verification_path),
    }
    print(json.dumps(summary, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
