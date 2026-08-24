#!/usr/bin/env python3
"""Validate a portal-ready ZDI case package without modifying it."""

from __future__ import annotations

import hashlib
import re
import sys
from pathlib import Path

MAX_PORTAL_ATTACHMENT = 50 * 1024 * 1024
REQUIRED_SECTIONS = (
    "# ZDI Submission Draft",
    "## Name of Vulnerability",
    "## Detailed Description",
    "### 1. Vulnerability Title",
    "### 2. High-Level Overview and Impact",
    "### 3. Affected Product and Complete Version",
    "### 4. Root Cause Analysis",
    "### 5. Proof of Concept",
    "### 6. Software Download Link",
    "## Payment Method",
    "## Credit Discovery To",
)
PLACEHOLDER_RE = re.compile(r"\[[^\]\n]+\]")


def section_value(text: str, heading: str) -> str:
    start = text.find(heading)
    if start < 0:
        return ""
    start += len(heading)
    match = re.search(r"^#{1,3}\s", text[start:], re.MULTILINE)
    end = start + match.start() if match else len(text)
    return text[start:end].strip()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: validate_case.py <zdi-case-directory>", file=sys.stderr)
        return 2

    root = Path(sys.argv[1]).resolve()
    report = root / "submission.md"
    attachments = root / "attachments"
    manifest = root / "hashes.sha256"
    errors: list[str] = []
    warnings: list[str] = []

    if not report.is_file():
        errors.append("missing submission.md")
        text = ""
    else:
        text = report.read_text(encoding="utf-8")

    for heading in REQUIRED_SECTIONS:
        if heading not in text:
            errors.append(f"missing section: {heading}")
        elif heading != "# ZDI Submission Draft" and not section_value(text, heading):
            errors.append(f"empty section: {heading}")

    name = section_value(text, "## Name of Vulnerability")
    if len(name) > 255:
        errors.append("vulnerability name exceeds 255 characters")
    if name and not re.fullmatch(r"[A-Za-z0-9 ]+", name):
        errors.append("vulnerability name must contain only letters, numbers, and spaces")

    placeholders = sorted(set(PLACEHOLDER_RE.findall(text)))
    if placeholders:
        errors.append("unresolved template placeholders: " + ", ".join(placeholders[:10]))

    if not attachments.is_dir():
        errors.append("missing attachments directory")
        files: list[Path] = []
    else:
        files = sorted(path for path in attachments.rglob("*") if path.is_file())
        if not files:
            errors.append("attachments directory contains no PoC files")

    expected_hashes = {sha256(path): path.relative_to(root).as_posix() for path in files}
    for path in files:
        if path.stat().st_size > MAX_PORTAL_ATTACHMENT:
            warnings.append(
                f"over 50 MB; do not portal-upload without ZDI transfer routing: "
                f"{path.relative_to(root)}"
            )

    if not manifest.is_file():
        errors.append("missing hashes.sha256")
    else:
        manifest_text = manifest.read_text(encoding="utf-8", errors="replace")
        for digest, relative in expected_hashes.items():
            if digest not in manifest_text or relative not in manifest_text.replace("\\", "/"):
                errors.append(f"hashes.sha256 missing current digest/path for {relative}")

    for warning in warnings:
        print(f"WARNING: {warning}")
    for error in errors:
        print(f"ERROR: {error}")

    if errors:
        print(f"FAILED: {len(errors)} error(s), {len(warnings)} warning(s)")
        return 1

    print(f"OK: {len(files)} attachment(s), {len(warnings)} warning(s)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
