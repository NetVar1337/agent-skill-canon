---
name: offset-dumper
description: "Game/engine offset & schema dumper engineering: build identification, UE (GNames/GObjects/SDK), Unity (il2cpp + global-metadata), Source (Netvars/schema) pipelines, pattern-DB design with per-build pinning and drift alerts, live validation, C++/Rust binding emission, patch-day automation. Use when maintaining offsets for versioned game builds."
version: 2.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: game
---

# Offset dumper

The dumper's job is to turn "game updated" from a reverse-engineering session
into a mechanical re-run. Success metric: time from patch-live to validated
offsets < 30 minutes, with zero manual IDA time for routine updates.

## Build identification (the key that everything hangs off)

Pick ONE stable buildid and use it in filenames, DB keys, and validation logs:

| Engine | Best buildid source |
|---|---|
| UE | `GEngineVersion` / `-Build=CL=*****` in logs or `GameVersion` UObject prop |
| Unity | `globalgamemanagers`/player version string + il2cpp SHA |
| Source | `steam_api` app build / `client.dll` product version |
| generic | (module PE timestamp, SizeOfImage, product version) tuple |

Never key on install path or window title. Ship the buildid inside the
generated header so runtime code can self-check.

## Pipeline

1. **Identify build** (above) → if known-good and module hashes match the DB,
   emit cached bindings and stop. Hash match is the fast path that makes
   patch-day cheap.
2. **Dump schemas** (engine-specific, below).
3. **Pattern-scan** what schemas can't provide (functions: aim, W2S, present).
   Sigs come from the DB (`pattern-scanner` format: `{pattern, mask, offset,
   resolve, builds{}}`), each with a uniqueness assertion.
4. **Validate live**: attach read-only to a running instance, dereference
   pointer chains, sanity-check field values (team ∈ [0,3], health ≤ 10000,
   world→actor count < 10⁵…). A bad offset that validates cleanly is worse
   than a crash — assert semantics, not just readability.
5. **Emit bindings** + record `builds[buildid] = {rva…}` back into the DB.

## Engine specifics

### Unreal (UE4/UE5)

- Anchor recovery order: `GNames` (FName pool) → `GObjects` (FUObjectArray)
  → `GWorld`/`GEngine`. Classic sigs target `FName::ToString`/`FNamePool`
  internals and `UObjectArray` access patterns in `CoreUObject` exports.
- With names+objects: walk `ChunkedObjects`, build the class hierarchy dump
  (a Dumper-7-style SDK generator shape), emit per-class offsets.
- Validate: name sanity (`Engine.Actor` exists, property counts within
  historical variance ±15%).

### Unity (il2cpp)

- `GameAssembly.dll` + `global-metadata.dat` (in `Data/il2cpp_data/Metadata`).
  Metadata header gives string/US string literals; the code-registration
  structures (found via sig or the standard `il2cpp_init` xref walk) give the
  full method table → full class/method offset map.
- Metadata version matters (v24…v31 headers differ); pin the parser to the
  exact version, refuse unknowns.
- Mono builds: dump via runtime assemblies instead (no il2cpp step).

### Source/Source 2

- Source1: `Netvars` via `GetAllClasses` walk (dwGetAllClasses sig), entity
  schema from classprop chains. Source2: `schema system` binary has typed
  declarations — parse `CSchemaSystem` type data.

## DB layout (repo artifact)

```
offsets/<game>/
  builds/<buildid>.json      # frozen, never edited after validation
  sigs.json                  # patterns + per-build hit rva history
  structs/<Class>.json       # field name/type/offset/size
  reports/<date>-<buildid>.md
```

Rules: builds/*.json are append-only; a changed offset for the same buildid =
new file + incident note; sigs.json changes require re-validation against
every build in `builds[]`.

## Drift alerts & patch-day automation

- Scheduled task / cron on the research box: poll the game's manifest
  (Steam depot/CDN API or launcher log), detect buildid change, kick the
  dumper headlessly, run validation, write report, notify.
- Embed *recovery* patterns in the emitted header (sig + mask + resolve) so a
  shipped binary can self-repair on unknown builds — with a hard
  uniqueness-assert fail-closed if the sig is ambiguous.

## Anti-tamper realities

- Games with pointer encryption / chunked props (EAC-protected titles etc.):
  some "offsets" are runtime-computed. Model them as pointer-chain *recipes*
  (module → sig → deref → transform), not constants — the DB stores recipes.
- Obfuscated metadata (modified engines, custom il2cpp headers): fall back to
  dynamic dumping via `frida-dbi` (hook the accessors, observe real accesses)
  before fighting static parsers.

## Emission

- C++: `namespace offsets::buildid { inline constexpr uint64_t GWorld = 0x...; }`
  + struct mirrors with `static_assert(sizeof)` where the engine type is known.
- Rust: typed `pub const G_WORLD: u64 = ...;` + `#[repr(C)]` mirrors, same
  static asserts via `const` asserts.
- Always emit `BUILD_ID` and a runtime `validate_build()` that compares the
  live module hash to the pinned one.

## Pair with

`pattern-scanner` (the scan layer), `pe-tools` (module identity), `frida-dbi`
(dynamic validation + encrypted-pointer recovery), `game-internals`
(engine structures), `imgui-overlay` (consumer).
