---
name: perception-api
description: Perception.cx's Enma host API for script lifecycle, process memory, rendering, GUI, input, networking, sound, Windows automation, Unicorn, Zydis, CPU helpers, and MCP. Use when writing, reviewing, or debugging scripts that call APIs registered by Perception; not for Enma language syntax itself.
---

# Perception API

Authoritative local snapshot of `https://docs.perception.cx/perception`.

## Workflow

1. Read [references/INDEX.md](references/INDEX.md), then open only the API pages relevant to the task.
2. Read [references/readme.md](references/readme.md) for current conventions before producing code.
3. For executable scripts, confirm lifecycle semantics in `references/lifecycle-and-routines.md`.
4. Search exact names and signatures locally; do not invent overloads, enums, permission gates, or handle semantics.
5. Return complete Enma code when implementation is requested. Keep permission-dependent behavior explicit.

## API routing

- Process attachment, memory, modules, VADs, patterns: `references/proc-api.md`
- Drawing, fonts, textures, shaders, GPU resources: `references/render-api.md`
- Controls, frames, layers, menus, file pickers: `references/gui-api.md`
- Keyboard and mouse state: `references/input-api.md`
- HTTP, WebSocket, UDP: `references/net-api.md`
- Window queries and input sending: `references/win-api.md`
- CPU/timing: `references/cpu-api.md`
- Emulation/disassembly: `references/unicorn-api.md`, `references/zydis-api.md`
- Audio: `references/sound-api.md`
- Agent JSON-RPC surface: `references/mcp-api.md`

## Boundaries

Perception registers this host API on top of Enma. For language syntax, standard-library behavior, compiler semantics, or embedding SDK details, load the `enma-lang` skill. Treat legacy Lua and AngelScript APIs as different surfaces; do not translate their signatures into Enma unless asked.
