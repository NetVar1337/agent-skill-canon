---
name: state-snapshot
description: Capture a full debuggee state snapshot (all committed memory regions + processor state) to disk for offline analysis
allowed-tools: mcp__x64dbg__get_debugger_status, mcp__x64dbg__pause, mcp__x64dbg__disconnect, mcp__x64dbg__connect_to_session, mcp__x64dbg__go, Bash
---

# state-snapshot

Capture a full debuggee state snapshot — all committed memory regions as raw binary files plus the complete processor state as JSON.

## Instructions

Follow these steps exactly:

### 1. Verify debugger connection

Call `mcp__x64dbg__get_debugger_status` to confirm the debugger is connected and a debuggee is loaded. Note the **session PID** and **x64dbg path** from the current MCP connection — you will need these to reconnect later.

If no debuggee is loaded, tell the user and stop.

### 2. Pause the debuggee if running

If the debugger status shows the debuggee is running (not paused), call `mcp__x64dbg__pause` to pause it. Remember that you auto-paused so you can resume later.

### 3. Disconnect the MCP client

Call `mcp__x64dbg__disconnect` to release the ZMQ connection. This is **required** because only one client can be connected to an x64dbg session at a time, and the Python script needs its own connection.

### 4. Run the snapshot script

Execute the snapshot script:

```
python "${CLAUDE_PLUGIN_ROOT}\skills\state-snapshot\state_snapshot.py" --x64dbg-path "<x64dbg_path>" --pid <session_pid>
```

Where:
- `<x64dbg_path>` is the path to the x64dbg executable noted in step 1
- `<session_pid>` is the debugger process PID noted in step 1

The script defaults output to `./snapshots/<timestamp>/`. If the user specified a custom output directory, pass `--output-dir <path>`.

### 5. Reconnect the MCP client

Call `mcp__x64dbg__connect_to_session` with the **x64dbg path** and **session PID** saved from step 1 to restore the MCP connection.

### 6. Report results

Summarize what was captured:
- Output directory path
- Number of memory region files saved and total size
- Whether registers were captured successfully
- Any regions that failed to read
