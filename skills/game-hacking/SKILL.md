---
name: game-hacking
description: Use for authorized game security research involving Unreal, Unity, Source, or custom-engine reverse engineering, build-pinned offsets, external/internal instrumentation, overlays, speed/time virtualization, client-server protocol and logic exploits, reconciliation or rollback trust, cheat telemetry, and anti-cheat experiments. Routes offset dumping, protocol RE, exploit development, and detection validation to the correct specialist.
---

# Game hacking workflow

## Activation

Use when the task involves game exploits, cheat development, speedhacks,
game memory manipulation, game protocol reverse engineering, or anti-cheat
bypass.

## Route first

| Actual objective | PRIMARY / handoff |
|---|---|
| Engine objects, reflection, schemas, or patch offsets | Start here; hand extraction/regression tooling to `offset-dumper` |
| External reader, internal hook, or render surface | Start here; route rendering to `imgui-overlay` |
| Message grammar, replay, parser, or server logic | `network-protocol-re`, with this skill retaining game trust semantics |
| Memory-corruption primitive | `exploit-dev`; do not mix it with cheat-feature validation |
| Anti-cheat sensor/heartbeat/integrity behavior | `anti-cheat-bypass`, then `windows-telemetry-etw` for ETW evidence |
| Hyper-V/VMBus/root-partition surface | `hyper-v-offensive`, not a generic game offset workflow |

One case may cross rows, but each experiment gets one objective, one build, and one measurable outcome.

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

## Build provenance and offset regression contract

Before accepting any address or schema, record executable/module SHA-256, PE timestamp or ELF Build ID, file/product version, engine version/commit, distribution channel, architecture, anti-cheat service/driver versions, symbol/dump tool versions, and server protocol/schema version.

```powershell
Get-FileHash .\game.exe,.\GameAssembly.dll -Algorithm SHA256
(Get-Item .\game.exe).VersionInfo | Format-List FileVersion,ProductVersion
sigcheck64.exe -nobanner -h -i .\game.exe
```

Store each recovered value as `module hash + RVA/field + extraction source + signature + semantic assertions`, never as a bare offset. A regression run must:

1. require one executable-section signature match and resolve every relative displacement inside the expected module;
2. validate pointer canonicality, object/class identity, field alignment/range, and at least one live state transition;
3. compare reflection/schema output with a separate runtime or disassembly observation;
4. fail closed on zero/multiple matches or a changed enclosing function; and
5. emit an old/new manifest diff before generated bindings are rebuilt.

Route manifest generation, signature uniqueness, and CI drift alarms to `offset-dumper`.

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

### Engine extraction and validation recipes

- **Unreal:** pin the cooked build, then recover `FNamePool`, `GUObjectArray`, and `GWorld`. Validate `FUObjectArray` bounds and `UObject::{ClassPrivate,NamePrivate,OuterPrivate}` before walking `UStruct::{SuperStruct,ChildProperties}`. A generated SDK passes only when sampled names/classes match runtime objects and a known actor transform changes coherently in two observations.
- **Unity IL2CPP:** run `Il2CppDumper.exe GameAssembly.dll global-metadata.dat out`, retain the metadata header/version and tool log, and map generated `MethodInfo`/field offsets back to `GameAssembly.dll` RVAs. Validate a type through both metadata and a runtime IL2CPP API such as `il2cpp_class_from_name`; separate Mono builds and use Mono reflection APIs instead of forcing an IL2CPP layout.
- **Source:** acquire `SchemaSystem_001`, enumerate the correct module type scope, and retain `CSchemaClassInfo` field names/types/offsets. For Source 1, recover `RecvTable`/`RecvProp` instead. Validate entity-system bounds, class identity, and one netvar against a live update before emitting bindings.

Every recipe keeps the raw dump, tool version, module hashes, generated schema, validation log, and rejected ambiguous signatures.

### 3. Exploit types

**Speedhack:**
1. Locate the game's time source:
   - `QueryPerformanceCounter` / `GetTickCount` / `timeGetTime` hooks.
   - `std::chrono` / engine-specific tick function.
   - Network timestamp fields.
2. Hook the time function to return scaled time:
   ```c
   // Capture real_base and virtual_base atomically when the hook is enabled.
   BOOL WINAPI Hooked_QPC(LARGE_INTEGER *count) {
       LARGE_INTEGER now;
       if (!Original_QPC(&now)) return FALSE;
       count->QuadPart = virtual_base.QuadPart + (LONGLONG)
           ((now.QuadPart - real_base.QuadPart) * g_speed_multiplier);
       return TRUE;
   }
   ```
   Keep `QueryPerformanceFrequency` unchanged. When the multiplier changes, atomically rebase `virtual_base` to the current virtual value and `real_base` to the current raw counter so time stays continuous and monotonic; test concurrent callers and server correction separately.
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

## Server authority, tick, reconciliation, and rollback

Map ownership per field: client input, client prediction, server simulation, replicated snapshot, reconciliation rule, rollback window, and final persistence transaction. Join packet captures/runtime hooks by client command number, client tick, server tick, acknowledgement, snapshot ID, correction delta, and backend transaction ID.

A valid logic test changes one client-controlled field and records whether the server rejects, clamps, rewinds/resimulates, accepts transiently, or commits persistently. Replaying movement without inventory persistence does not prove a durable server exploit. Negative controls use a valid sequence and one deliberately stale/duplicate/out-of-window sequence; reset through a server-confirmed state event rather than a delay.

Route message framing/encryption/replay harnesses to `network-protocol-re`; route a parser memory-safety primitive to `exploit-dev`.

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

### 5. Anti-cheat research hypotheses

See also: `anti-cheat-bypass` for product-specific implementation, `kernel-dev` for supported driver mechanics, `byovd` for a third-party vulnerable-driver boundary, and `hyper-v-offensive`/`hypervisor-dev` for virtualization research. The items below are candidate surfaces, not portable bypass claims; test one against a pinned product/build and healthy telemetry.

**User-mode candidate surfaces:** compare same-hash `ntdll` remapping/unhooking, loader-list visibility, `NtQueryVirtualMemory` observations, and captured stacks as separate variables; retain module/VAD/stack evidence before and after.

**Kernel/hypervisor candidate surfaces:** callback ownership, VAD observations, EPT split views, ETW provider behavior, and handle filtering belong to their specialist skills. Never directly remove another component's callbacks as a generic driver workflow.

**Detection hypotheses:** measure `ReadProcessMemory` versus the selected access path, RW→RX versus mapped-image lifecycle, string/config handling, input timing/curves, and unload cleanup independently. Kernel access, encrypted strings, or randomized timing do not imply low visibility.

## Baseline-versus-modified detection validation

Pin game/anti-cheat/service/driver hashes, policy, account/test environment, backend connectivity, and telemetry source health. For each technique run:

| Run | Purpose | Required evidence |
|---|---|---|
| Clean baseline | establish normal events and server corrections | ETL/service logs/network timeline with known action markers |
| Positive control | prove the expected sensor/report path is alive | vendor-supported test event and correlated backend result |
| One modified variable | test the hypothesis | same action markers, local events, heartbeat/report, server outcome |
| Rollback | prove cleanup and recovery | module/handle/hook state restored and positive control works again |

Classify local block, kick, delayed report, telemetry-only, ban, server correction, crash, and no observed delta separately. Absence is inconclusive if event loss, heartbeat health, cloud ingestion, or policy differs. Route ETW session/provider/loss work to `windows-telemetry-etw` and anti-cheat implementation to `anti-cheat-bypass`.

## Routing

- Batch A siblings: `bof-coff-development`, `windows-rpc-com-attack`, `windows-telemetry-etw`, and `hyper-v-offensive`.
- Batch B siblings: `linux-kernel-exploitation`, `c2-implant-engineering`, `ebpf-offensive`, and `linux-host-post-exploitation`.
- Game-specific implementation routes: `offset-dumper`, `imgui-overlay`, `network-protocol-re`, `anti-cheat-bypass`, and `exploit-dev`.

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

- [ ] Route-first objective, game/server build, engine, anti-cheat, module hashes, and schema versions are pinned
- [ ] Every offset has provenance, a unique signature, semantic assertions, and an old/new regression result
- [ ] UE/Unity/Source extraction is confirmed by an independent runtime or disassembly observation
- [ ] Time virtualization is baseline-relative, monotonic across multiplier changes, and separated from server correction
- [ ] Server trust tests distinguish rejection, transient acceptance, rollback/reconciliation, and durable commit
- [ ] Clean baseline and positive control prove telemetry/report health before a modified run is interpreted
- [ ] Detection outcomes distinguish block, kick, delayed report, telemetry, ban, correction, crash, and inconclusive
- [ ] Hooks, allocations, handles, sessions, and server test state are restored
