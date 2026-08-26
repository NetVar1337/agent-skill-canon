"""
Compare two debuggee state snapshots and produce a focused change report.

Takes two snapshot directories (produced by state_snapshot.py) and diffs:
  - registers.json: Emit only registers whose values changed
  - memory_map.json + *.bin: Detect added/removed/resized/modified regions with byte-level change blocks
"""

import argparse
import json
import sys
from pathlib import Path


def load_json(path: Path) -> dict | list:
    return json.loads(path.read_text())


def hex_dump(data: bytes) -> str:
    return " ".join(f"{b:02X}" for b in data)


def ascii_dump(data: bytes) -> str:
    return "".join(chr(b) if 0x20 <= b < 0x7F else "." for b in data)


def diff_registers(before_dir: Path, after_dir: Path) -> list[dict]:
    before_data = load_json(before_dir / "registers.json")
    after_data = load_json(after_dir / "registers.json")

    changes = []

    def walk(before_obj, after_obj, prefix=""):
        if isinstance(before_obj, dict) and isinstance(after_obj, dict):
            for key in sorted(set(before_obj.keys()) | set(after_obj.keys())):
                b_val = before_obj.get(key)
                a_val = after_obj.get(key)
                name = f"{prefix}.{key}" if prefix else key
                walk(b_val, a_val, name)
        elif before_obj != after_obj:
            def fmt(v):
                if isinstance(v, int):
                    return hex(v)
                return str(v) if v is not None else "<missing>"
            changes.append({
                "register": prefix,
                "before": fmt(before_obj),
                "after": fmt(after_obj),
            })

    walk(before_data.get("registers", {}), after_data.get("registers", {}))
    return changes


def build_region_index(manifest: list[dict]) -> dict[str, dict]:
    index = {}
    for entry in manifest:
        index[entry["base"]] = entry
    return index


MERGE_GAP = 16
MAX_CHANGE_BLOCKS = 512
MAX_BLOCK_BYTES = 2048


def diff_memory_region(before_dir: Path, after_dir: Path,
                       before_entry: dict, after_entry: dict) -> dict:
    base = before_entry["base"]
    size = before_entry["size"]
    info = after_entry.get("info", "")

    result = {
        "base": base,
        "size": size,
        "info": info,
        "total_changed_bytes": 0,
        "change_pct": 0.0,
        "changes": [],
    }

    before_file = before_dir / before_entry["file"] if before_entry.get("file") else None
    after_file = after_dir / after_entry["file"] if after_entry.get("file") else None

    if not before_file or not after_file or not before_file.exists() or not after_file.exists():
        if not before_entry.get("read_ok") or not after_entry.get("read_ok"):
            result["error"] = "one or both regions could not be read"
        return result

    before_bytes = before_file.read_bytes()
    after_bytes = after_file.read_bytes()

    length = min(len(before_bytes), len(after_bytes))
    if length == 0:
        return result

    # Scan for changed byte ranges
    raw_blocks = []
    i = 0
    while i < length:
        if before_bytes[i] != after_bytes[i]:
            start = i
            while i < length and before_bytes[i] != after_bytes[i]:
                i += 1
            raw_blocks.append((start, i))
        else:
            i += 1

    # Merge nearby blocks
    merged = []
    for block in raw_blocks:
        if merged and block[0] - merged[-1][1] <= MERGE_GAP:
            merged[-1] = (merged[-1][0], block[1])
        else:
            merged.append(block)

    total_changed = sum(end - start for start, end in merged)
    result["total_changed_bytes"] = total_changed
    result["change_pct"] = round(total_changed / length * 100, 2)

    if len(merged) > MAX_CHANGE_BLOCKS:
        result["changes"] = []
        result["summarized"] = True
        result["block_count"] = len(merged)
        return result

    for start, end in merged:
        block_size = end - start
        b_slice = before_bytes[start:end]
        a_slice = after_bytes[start:end]

        truncated = block_size > MAX_BLOCK_BYTES
        if truncated:
            b_slice = b_slice[:MAX_BLOCK_BYTES]
            a_slice = a_slice[:MAX_BLOCK_BYTES]

        change = {
            "offset": hex(start),
            "size": block_size,
            "before_hex": hex_dump(b_slice),
            "after_hex": hex_dump(a_slice),
            "before_ascii": ascii_dump(b_slice),
            "after_ascii": ascii_dump(a_slice),
        }
        if truncated:
            change["truncated"] = True
            change["shown_bytes"] = MAX_BLOCK_BYTES

        result["changes"].append(change)

    return result


def diff_memory(before_dir: Path, after_dir: Path) -> dict:
    before_manifest = load_json(before_dir / "memory_map.json")
    after_manifest = load_json(after_dir / "memory_map.json")

    before_index = build_region_index(before_manifest)
    after_index = build_region_index(after_manifest)

    before_bases = set(before_index.keys())
    after_bases = set(after_index.keys())

    added = []
    for base in sorted(after_bases - before_bases):
        e = after_index[base]
        added.append({"base": e["base"], "size": e["size"], "info": e.get("info", "")})

    removed = []
    for base in sorted(before_bases - after_bases):
        e = before_index[base]
        removed.append({"base": e["base"], "size": e["size"], "info": e.get("info", "")})

    common_bases = sorted(before_bases & after_bases)
    resized = []
    modified = []
    unchanged_count = 0

    for base in common_bases:
        b_entry = before_index[base]
        a_entry = after_index[base]

        if b_entry["size"] != a_entry["size"]:
            resized.append({
                "base": base,
                "before_size": b_entry["size"],
                "after_size": a_entry["size"],
                "info": a_entry.get("info", ""),
            })
            continue

        # Same base + size — check bytes
        region_diff = diff_memory_region(before_dir, after_dir, b_entry, a_entry)
        if region_diff["total_changed_bytes"] > 0 or region_diff.get("error"):
            modified.append(region_diff)
        else:
            unchanged_count += 1

    return {
        "added_regions": added,
        "removed_regions": removed,
        "resized_regions": resized,
        "modified_regions": modified,
        "unchanged_region_count": unchanged_count,
        "summary": {
            "total_regions_before": len(before_manifest),
            "total_regions_after": len(after_manifest),
            "modified": len(modified),
            "added": len(added),
            "removed": len(removed),
            "resized": len(resized),
        },
    }


def print_summary(report: dict):
    reg_changes = report["register_changes"]
    mem = report["memory"]
    s = mem["summary"]

    print(f"\n{'='*60}")
    print(f"  State Diff Report")
    print(f"{'='*60}")
    print(f"  Before: {report['before_dir']}")
    print(f"  After:  {report['after_dir']}")
    print()

    print(f"  Registers changed: {len(reg_changes)}")
    for rc in reg_changes[:20]:
        print(f"    {rc['register']:20s}  {rc['before']} -> {rc['after']}")
    if len(reg_changes) > 20:
        print(f"    ... and {len(reg_changes) - 20} more")
    print()

    print(f"  Memory regions: {s['total_regions_before']} before, {s['total_regions_after']} after")
    print(f"    Added:     {s['added']}")
    print(f"    Removed:   {s['removed']}")
    print(f"    Resized:   {s['resized']}")
    print(f"    Modified:  {s['modified']}")
    print(f"    Unchanged: {mem['unchanged_region_count']}")

    for m in mem["modified_regions"][:10]:
        print(f"\n    Region {m['base']} ({m['info']}): {m['total_changed_bytes']} bytes changed ({m['change_pct']}%)")
        if m.get("summarized"):
            print(f"      ({m['block_count']} change blocks — summarized)")
        else:
            for c in m["changes"][:5]:
                trunc = " [truncated]" if c.get("truncated") else ""
                print(f"      @ {c['offset']} ({c['size']} bytes){trunc}")

    print(f"\n{'='*60}\n")


def main():
    parser = argparse.ArgumentParser(description="Diff two x64dbg state snapshots")
    parser.add_argument("--before", required=True, help="Path to the 'before' snapshot directory")
    parser.add_argument("--after", required=True, help="Path to the 'after' snapshot directory")
    parser.add_argument("--output", default=None, help="Output report path (default: <after_dir>/diff_report.json)")
    args = parser.parse_args()

    before_dir = Path(args.before)
    after_dir = Path(args.after)

    for d in (before_dir, after_dir):
        if not d.is_dir():
            print(f"Error: {d} is not a directory", file=sys.stderr)
            sys.exit(1)
        if not (d / "registers.json").exists():
            print(f"Error: {d / 'registers.json'} not found", file=sys.stderr)
            sys.exit(1)
        if not (d / "memory_map.json").exists():
            print(f"Error: {d / 'memory_map.json'} not found", file=sys.stderr)
            sys.exit(1)

    print(f"[*] Diffing snapshots:")
    print(f"    Before: {before_dir.resolve()}")
    print(f"    After:  {after_dir.resolve()}")

    register_changes = diff_registers(before_dir, after_dir)
    print(f"[+] Register diff: {len(register_changes)} changes")

    memory_diff = diff_memory(before_dir, after_dir)
    print(f"[+] Memory diff: {memory_diff['summary']}")

    report = {
        "before_dir": str(before_dir.resolve()),
        "after_dir": str(after_dir.resolve()),
        "register_changes": register_changes,
        "memory": memory_diff,
    }

    output_path = Path(args.output) if args.output else after_dir / "diff_report.json"
    output_path.write_text(json.dumps(report, indent=2))
    print(f"[+] Report saved to {output_path.resolve()}")

    print_summary(report)


if __name__ == "__main__":
    main()
