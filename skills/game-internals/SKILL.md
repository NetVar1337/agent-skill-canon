---
name: game-internals
description: "Game internals RE: engines (UE/Unity/Source), entity systems, networking ticks, prediction, rendering data, asset formats."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: game
  triggers:
    - "UWorld"
    - "il2cpp"
    - "entity list"
    - "view matrix"
    - "game netcode"
---

# Game internals

## Engine fingerprints
- **Unreal**: GWorld, UObject arrays, GNames/FName, ProcessEvent, replication graph
- **Unity**: il2cpp vs mono, domain/assemblies, native→managed bridges
- **Source/Source2**: entity list, interfaces, schema systems
- **Custom**: start from input→simulation→render dataflow

## Systems to map
1. Entity/actor/component registries + handle schemes
2. Transforms (local/world), bones, bounds
3. Simulation tick vs render frame; client prediction & reconciliation
4. Netcode: snapshots, delta compress, lag comp, interest management
5. Physics world pointers
6. Camera/view matrix origins (for world-to-screen)
7. Inventory/ability/state machines

## Method
- Static: strings, RTTI, PDB leftovers, IL/Bytecode dumps
- Dynamic: ReClass/Offests, hooks on tick/Packet write
- Validate offsets across patches; generate offset DBs

## Unreal Engine 4/5 deep map

### Core globals (find via Dumper-7 or hand-rolled sigs)
| Symbol | Purpose | Notes |
|---|---|---|
| `GNames` / FNamePool | name table | 4.23+ uses FNamePool (blocks of 8KB); older = TNameEntryArray. Sig: FName::ToString references |
| `GObjects` | UObject array | FUObjectArray; 4.22+ pre-GC layout switch (chunked 8192/64). Sig: `StaticConstructObject` internals |
| `GWorld` → `ULevel*` → `AActor*` | actor iteration | OwningGameInstance → LocalPlayers |
| `GEngine` → `GameViewport` → `GamePlayers[0]` | player chain | → `APlayerController` → `PlayerCameraManager` (view) + `AcknowledgedPawn` |
| `ProcessEvent` (UObject vtable slot or sig) | hookable event dispatch | vtable index 0x41-ish on 4.x (verify per build) |
| `GEngine->NetDrivers` | replication channels | snapshot + delta dumps |

### Per-patch survival
- UE ships with incremental version (`++UE+Release-42.00`); tie offset DB to exact build.
- Mobile UE (e.g. libUnreal.so, stripped, LLD): only ~50 dynsym exports survive — GNames/GObjects via pattern scans; re-dump per patch (see `offset-dumper`).
- FName index → string: walk FNamePool block by index >> 16, entry header has bIsWide + len.

### UE5 deltas vs UE4
- `UWorld` → `PersistentLevel` unchanged in practice; `FNamePool` layout stable.
- Chaos physics replaces PhysX: `FPhysicsActor` handles differ; mesh → transform indirection via `Chaos::FSingleParticlePhysics`.
- World Partition changes level streaming: iterate `ULevel::Actors` per cell; streaming sources gated by `WorldPartition` subsystem.

## Unity deep map
- il2cpp: `global-metadata.dat` header (sanity: magic 0xFAB11BAF, version field drives Il2CppDumper behavior); `CodeRegistration` + `MetadataRegistration` via `il2cpp_init` xrefs.
- Key natives: `il2cpp_domain_get_assemblies`, `il2cpp_class_from_type`, `il2cpp_runtime_invoke` — hook these for logic-level instrumentation without touching game code.
- Mono (legacy): root domain → `mono_assembly_load` chains; use `mono_get_root_domain` and walk `MonoImage` tables.
- Il2CppInterop / MelonLoader style: inject a mono runtime host to call back into managed — detection cost: assembly scan.

## Source / Source2 deep map
- Source1: `IBaseClientDLL` → `CreateMove` (input → cmd), `IVEngineClient` (view), netvars via `GetClientClass` chain + `RecvTable` recursion (flat offsets = netvar manager).
- CS2/Source2: schema system (`resource/…/schema.bin`, `.vcss`-adjacent) exposes typed field paths; community exports as `schema.json`. `CEntityIdentity` list head via `Source2Client` → entity handles are `CEntityHandle` (index + serial). Panorama UI and entity sim decoupled — read entity list each sim tick, not render frame.

## World-to-screen (the render bridge)
```
// column-major view-projection; v = bones[j] * 4x4 vp
x = v.x / v.w, y = v.y / v.w           // NDC
if v.w < 0 -> behind camera, cull
screen.x = (x * 0.5 + 0.5) * width
screen.y = (1 - (y * 0.5 + 0.5)) * height   // Y flip
```
- Camera origin: `PlayerCameraManager->GetCameraLocation` or `ViewTarget->POV`; confirm FOV scaling (UE: horizontal FOV; multiply Y by aspect correction).
- Bone matrix: `USkeletalMeshComponent->GetBoneMatrix(i)` — or cached `ComponentToWorld` + `BoneSpaceTransforms`.

## Netcode classes worth memorizing
- UE: `UNetConnection`, `UChannel`, `UActorChannel` (property replication per-actor); client prediction via `SavedMoves` on `UCharacterMovementComponent` — replay window abuses live here (`game-hacking-exploits`).
- Source1: `CUserCmd` (tick, viewangles, buttons), sequence-nr rollback = lag-comp window; anti-aim/silent-aim derive from same structures.
- Interest management tells you which entities the server sends — spoof nothing outside it, detect desyncs by comparing local sim vs last snapshot.

## Pair with
`game-hacking`, `game-hacking-exploits`, `aimbot-humanization`.
