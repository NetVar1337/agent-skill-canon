---
name: cheat-longevity-engineering
description: "Engineering cheats for longevity under AC: minimize ban surfaces, feature risk tiers, OPSEC build/deploy, silent flags, update cadence."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: game
  triggers:
    - "cheat longevity"
    - "silent ban"
    - "ban evasion engineering"
    - "undetected design"
---

# Cheat longevity engineering ("write cheat unless bans")

Goal: design features and delivery so research builds last longer under modern AC — not magic immunity.

## Risk tier features
| Tier | Examples | Notes |
|---|---|---|
| Lower | external read radar, web radar | still HWID/telemetry risk |
| Medium | internal ESP with careful stacks | stack walks, overlays |
| High | aim write, input synthesis | behavioral + integrity |
| Extreme | kernel RW without HV hygiene | callback hell |

## Longevity principles
1. **Read > write** when possible
2. Prefer **signed execution contexts** / legitimate modules for call stacks (`anti-cheat-stack-walk-stealth`)
3. Separate **loader** trust from **payload** trust
4. Assume **silent bans** — canary accounts, staged rollout
5. Strip debug strings; deterministic builds; no public GH paste signatures
6. Rotate primitives after AC patches; keep offset DB versioned
7. HWID/TPM: understand what you cannot cheaply forge (`tpm-attestation-research`)

## Delivery pipelines
- Manual map vs LoadLibrary risk profile
- HV read vs driver IOCTL vs usermode RPM
- UEFI/boot loaders change measured boot story

## Feature risk matrix (observable → verdict mapping)
| Feature surface | AC sees | Verdict class |
|---|---|---|
| External radar (2nd PC / phone) | nothing on game PC | survives all but network-usage heuristics |
| External read overlay (same PC, no inject) | overlay window enum, driver presence | lives until driver-sweep |
| Internal ESP | stack walks, module scans, page scans | medium; dies on signature or sweep |
| Aim-assist write | server-side aim statistics (spectral, hit ratios) | time-delayed ban wave bait |
| Input synthesis (driver-level mouse) | input-source consistency (HID stack) | medium-high; match real-HID timing quirks |
| Kernel RW, no callbacks | pool scans, PiDDB, module lists | dies on artifact sweep if unhygienic |
| Kernel RW + callbacks removed | PatchGuard eventually | extreme |

## Silent-flag canary program
1. N canary accounts, one variable each, controlled burn schedule
2. Log: build hash, offsets version, feature set on/off timestamps, network capture sizes
3. Ban-timing regression: overlay ban latency vs feature; a flag-detected build dies in waves (days) not instantly — distinguish server-statistics bans (aim features) from artifact bans (injectors) by wave shape
4. Rotate one primitive per cycle (never all) to attribute detections

## Build/deploy OPSEC
- Deterministic builds (reproducible timestamps/paths); strip PDB paths; no unique build IDs per customer (one seizure = all customers if per-build).
- Never ship the loader with the payload; loader fetches keyed, versioned payload.
- No public-paste code in the trust boundary: public mappers/injectors are signatured; if used, mutate structure not just strings.
- Update cadence: after every game patch, verify offset DB (automated via `offset-dumper` drift alerts); a stale read = garbage writes = instant statistical flag.
- Kill switch: remote config can force-disable features faster than full update distribution.

## HWID reality
- Disk serial (ATA identify), SMBIOS (board/product/UUID), NIC MAC, GPU serial (EDID-adjacent), TPM EK/AIK, Windows machine GUID, volume IDs — see `hwid-identifier-surfaces` for the collection map.
- Cheap rotations: MAC, volume GUID, machine GUID, registry DevicePath entries. Expensive: SMBIOS (UEFI variable surgery), disk serial (firmware or filter-driver interception — inconsistent reads across query paths = spoofer detector), TPM (EK is hardware — can only block/attestation-fail, not forge; `tpm-attestation-research`).
- Spoofer detection: ACs cross-check serial consistency across query methods (IOCTL vs WMI vs registry); a disk that reports different serials on different paths = flagged spoofer, worse than static serial.

## Pair with
`game-hacking`, `eac-ban-stack`, `stealth-injectors`, `valthrun-style-stack`, `aimbot-humanization`.

## Refs
- UC: write cheat unless bans discussions
