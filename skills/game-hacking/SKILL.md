---
name: game-hacking
description: Game hacking workflow for exploits, cheats, speedhacks, memory manipulation, protocol RE, and anti-cheat bypass. Covers game memory structures, hooking, injection, and packet editing. Invoke with /game-hacking or when the task involves game exploits or cheats.
---

# Game hacking workflow

## Activation

Use when the task involves game exploits, cheat development, speedhacks,
game memory manipulation, game protocol reverse engineering, or anti-cheat
bypass.

## Workflow

### 1. Game analysis

**Identify the target:**
- Engine: Source 2, Unreal, Unity, custom, id Tech, CryEngine, Godot.
- Anti-cheat: EAC, BattlEye, Vanguard, Ricochet, VAC, custom.
- Architecture: client-server authoritative, P2P, client-predicted.
- Protection: packing, obfuscation, integrity checks, kernel driver.

**Map the game:**
1. Identify main module + engine modules.
2. Locate key classes: player, entity, weapon, vehicle, camera, world.
3. Find the game loop / tick function.
4. Map the rendering pipeline (D3D11/12, Vulkan, OpenGL).
5. Identify network layer (UDP custom, TCP, WebSocket, Steam networking).

### 2. Memory structure discovery

**Finding offsets:**
- Static analysis: IDA/Ghidra on game binary, trace from known strings
  ("health", "position", "ammo") to struct fields.
- Dynamic analysis: Cheat Engine scans (value, delta, pointer scans).
- Symbol dumping: if Unity (dump via Il2CppDumper), Unreal (SDK generator),
  or PDB available.
- Pattern scanning: signature scan for instructions that access target
  fields (survives updates better than hardcoded offsets).

**Common structures:**

```
// Generic game entity
struct Entity {
    char pad_0[0x8];          // vtable
    int32_t health;           // offset varies
    int32_t max_health;
    vec3_t position;
    vec3_t velocity;
    int32_t team;
    bool is_alive;
    // ...
};

// Source 2 (CS2) example offsets (update per patch)
// dwLocalPlayerPawn, dwEntityList, m_iHealth, m_vecOrigin,
// m_iTeamNum, m_lifeState, m_pCameraServices
```

**Pointer chains:**
```
base_module + offset → entity_list
entity_list + (index * stride) → entity_ptr
entity_ptr + health_offset → health_value
```

### 3. Exploit types

**Speedhack:**
1. Locate the game's time source:
   - `QueryPerformanceCounter` / `GetTickCount` / `timeGetTime` hooks.
   - `std::chrono` / engine-specific tick function.
   - Network timestamp fields.
2. Hook the time function to return scaled time:
   ```c
   // Detour QueryPerformanceCounter
   BOOL WINAPI Hooked_QPC(LARGE_INTEGER *count) {
       BOOL ret = Original_QPC(count);
       count->QuadPart = (LONGLONG)(count->QuadPart * g_speed_multiplier);
       return ret;
   }
   ```
3. Or modify the game's internal delta-time / tick-rate variable directly.
4. For server-authoritative: manipulate client-side prediction only
   (server will correct, but gives temporary advantage).

**Out-of-bounds read/write:**
1. Find array/buffer with index from untrusted input (network packet,
   file, user input).
2. Identify bounds check (or lack thereof).
3. Craft input with index beyond allocation.
4. OOB read → info leak (addresses, keys, other players' data).
5. OOB write → corrupt adjacent object (vtable, function pointer, size
   field).

**Buffer overflow:**
1. Find fixed-size buffer filled from network/file/input.
2. Identify the copy operation (`memcpy`, `strcpy`, custom loop).
3. Overflow past buffer into adjacent stack/heap data.
4. Stack: overwrite return address → ROP chain.
5. Heap: overflow into adjacent object → corrupt vtable/size/flags.

**Remote code execution:**
1. Chain info leak (defeat ASLR) + control-flow hijack.
2. For game clients: RCE via malformed packet, map file, model, texture.
3. For game servers: RCE via crafted request, protocol deserialization.
4. Build ROP chain from game binary + loaded modules.
5. Shellcode: download & execute, reverse shell, or in-game action.

**Protocol exploits:**
1. Capture client↔server traffic (Wireshark, mitmproxy, custom proxy).
2. Identify encryption (TLS, custom XOR/AES, protobuf).
3. Decrypt/decode (hook send/recv, extract keys from memory).
4. Map message types: auth, movement, action, chat, item, trade.
5. Replay / modify / inject messages:
   - Duplicate item / currency (race condition on server).
   - Teleport (modify position packet).
   - Item spawn (craft item-create packet).
   - Auth bypass (skip or replay auth sequence).

### 4. Cheat development

**External cheat (read-only memory):**
1. `OpenProcess(PROCESS_VM_READ)` → `ReadProcessMemory`.
2. Read entity list, player positions, health, view angles.
3. World-to-screen transform for ESP overlay.
4. Render via DirectX/GDI+/external overlay window.

**Internal cheat (DLL injection):**
1. Inject DLL into game process (manual map, `SetWindowsHookEx`,
   `CreateRemoteThread`, APC injection).
2. Hook rendering: `IDXGISwapChain::Present` (D3D11/12),
   `wglSwapBuffers` (OpenGL), `vkQueuePresentKHR` (Vulkan).
3. Draw ESP, aimbot, crosshair in the hook.
4. Hook game functions for aimbot:
   - `CreateMove` / `FrameStageNotify` (Source).
   - `ProcessEvent` / `Tick` (Unreal).
   - `OnGUI` / `Update` (Unity via Il2Cpp).

**Aimbot:**
1. Get local player eye position + view angles.
2. Iterate entities, filter by team/visibility/distance.
3. Calculate angle to target: `atan2(delta.y, delta.x)`.
4. Smooth: interpolate current → target angle over N ticks.
5. Apply: write to view angles or call engine's SetViewAngles.
6. RCS (recoil control): subtract recoil punch from aim.

**ESP / wallhack:**
1. World-to-screen: `ViewMatrix * world_pos` → NDC → screen coords.
2. Visibility check: raycast from eye to target, or read occlusion flags.
3. Draw: box, skeleton, health bar, name, distance via render API.
4. Chams: hook material system, override depth test / material.

**Triggerbot:**
1. Read crosshair entity ID (`m_iIDEntIndex` in Source).
2. If valid enemy: simulate fire (set `m_nButtons` |= `IN_ATTACK`).
3. Add reaction delay (randomized) for humanization.

### 5. Anti-cheat bypass

See also: `/kernel-dev` (callback removal, VAD spoofing), `/byovd`
(kernel R/W), `/hypervisor-dev` (EPT hooks for stealth).

**User-mode bypass:**
- Unhook `ntdll` (map fresh copy from disk, overwrite `.text`).
- Hide injected DLL from `InLoadOrderModuleList` /
  `InMemoryOrderModuleList` / `InInitializationOrderModuleList`.
- Patch `NtQueryVirtualMemory` to hide RWX regions.
- Spoof thread call stacks (return address spoofing).

**Kernel-mode bypass:**
- Remove anti-cheat callbacks (process, thread, image, registry).
- Spoof VAD entries for injected regions.
- EPT hooks: execute original code, read/write sees patched code.
- Disable ETW (patch `EtwEventWrite`).
- Handle stripping: remove anti-cheat's handles to game process.

**Detection avoidance:**
- No `ReadProcessMemory` from external process (use kernel R/W).
- No RWX memory (use RW + separate X, or RX only).
- No hardcoded strings (hash or encrypt).
- Randomize timing, humanize aim curves.
- Clean up all traces on unload.

## Tooling reference

| Category | Tools |
|---|---|
| Memory scanning | Cheat Engine, ReClass.NET, custom scanner |
| Disassembly | IDA Pro, Ghidra, Binary Ninja |
| Debugging | x64dbg, WinDbg, GDB |
| Injection | Extreme Injector, manual mapper, custom |
| Hooking | MinHook, Detours, PolyHook2, manual VMT |
| Network | Wireshark, mitmproxy, custom proxy, Scapy |
| Overlay | DirectX hook, GDI+, ImGui, external window |
| Unity RE | Il2CppDumper, dnSpy, AssetStudio |
| Unreal RE | SDK generator, UE4SS, FModel |
| Source RE | Source SDK, netprop dumper |

## Verification checklist

- [ ] Game engine and anti-cheat identified
- [ ] Key offsets found and verified (pattern scan preferred)
- [ ] Memory read/write working (external or internal)
- [ ] Render hook or overlay functional
- [ ] Anti-cheat bypass stable (no detection after extended play)
- [ ] Exploit PoC reliable and minimized
- [ ] Protocol documented with field table (if network exploit)
- [ ] Cleanup on unload (no dangling hooks, freed memory)
