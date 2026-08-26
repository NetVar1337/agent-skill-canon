---
name: yara-sigs
description: Scan a state snapshot's memory dumps with YARA signatures to detect packers, crypto constants, malware, and more
allowed-tools: mcp__x64dbg__get_debugger_status, mcp__x64dbg__pause, mcp__x64dbg__disconnect, mcp__x64dbg__connect_to_session, Bash, Read, AskUserQuestion, Skill
---

# yara-sigs

Scan debuggee memory (via a state snapshot) against a large YARA signature database to identify packers, crypto constants, anti-debug tricks, malware families, and more.

## Instructions

Follow these steps exactly:

### 1. Check prerequisites

Run `pip show yara-python` via Bash. If not installed, tell the user to run `pip install yara-python` and stop.
Run `git --version` via Bash. If not installed, tell the user to install Git and stop.

### 2. Ensure the YARA signature database is available

Check if the directory `${CLAUDE_PLUGIN_ROOT}\yarasigs` exists (use `dir`). If it does **not** exist, clone it:

```
git clone --recurse-submodules https://github.com/x64dbg/yarasigs "${CLAUDE_PLUGIN_ROOT}\yarasigs"
```

If the directory exists but looks incomplete (missing `Yara-Rules` or `citizenlab` subdirectories), update submodules:

```
git -C "${CLAUDE_PLUGIN_ROOT}\yarasigs" submodule update --init --recursive
```

### 3. Determine what to scan for

The YARA database contains many rule categories. If the user specified what they want to scan for in their invocation, use that. Otherwise, ask the user what they want to scan for using `AskUserQuestion` with these options:

- **Packers & compilers** — Detect packers (UPX, Themida, etc.) and compiler signatures
- **Crypto constants** — Find cryptographic algorithm constants (AES S-boxes, RSA, MD5, etc.)
- **Anti-debug / anti-VM** — Detect anti-debugging and anti-virtualization techniques
- **All signatures** — Scan with every available rule (slower, more noise)

Map the selection to rule category paths:

| Selection | Rule paths (relative to `yarasigs/`) |
|-----------|---------------------------------------|
| Packers & compilers | `packer.yara`, `packer_compiler_signatures.yara`, `Yara-Rules/packers/` |
| Crypto constants | `crypto_signatures.yara`, `Yara-Rules/crypto/` |
| Anti-debug / anti-VM | `Yara-Rules/antidebug_antivm/` |
| All signatures | All `.yar` and `.yara` files recursively |

### 4. Obtain a snapshot to scan

Check if a recent snapshot exists in `${CLAUDE_PLUGIN_ROOT}\snapshots` (use `dir`).

- If snapshots exist, ask the user whether to use an existing snapshot or take a fresh one.
- If no snapshots exist, tell the user you need to take a snapshot first.

To take a fresh snapshot, invoke the `state-snapshot` skill via `Skill("state-snapshot")`. After it completes, note the snapshot directory path.

### 5. Run the YARA scan

Execute the scan script:

```
python "${CLAUDE_PLUGIN_ROOT}\skills\yara-sigs\yara_scan.py" --snapshot-dir <snapshot_path> --yarasigs-dir "${CLAUDE_PLUGIN_ROOT}\yarasigs" --categories <category> [--module-filter <module_name>]
```

Where `<category>` is one of: `packers`, `crypto`, `antidebug`, or `all`.

**Module filtering:** If the user asks to focus on a specific module (e.g. the main executable), pass `--module-filter <name>` where `<name>` is a substring of the module name as shown in the memory map (e.g. `secret_encryptor`). This merges all of the module's sections into a single buffer before scanning, which is critical for YARA rules whose patterns span multiple PE sections (e.g. MD5 init constants in `.text` + T-table in `.rdata`). **Always prefer using `--module-filter` when scanning a specific module** rather than relying on per-region scanning.

The script writes results to `<snapshot_path>/yara_results.json` and prints a summary to stdout.

### 6. Report results

Read `<snapshot_path>/yara_results.json` if it exists and the stdout summary is not sufficient.

Present findings organized by:
- **Match summary** — How many rules matched across how many memory regions
- **Matches by rule** — Each matched rule name, its description/metadata, and which memory regions it hit (with base addresses and region info from `memory_map.json`)
- **Notable findings** — Call out anything especially interesting (known packers, specific crypto algorithms, anti-debug patterns)

If no matches were found, tell the user and suggest trying a broader category (e.g., "all").
