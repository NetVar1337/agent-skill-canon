---
name: eac-ban-stack
description: "EAC/EasyAntiCheat ban-stack research: HWID serials, server-side trust, usermode telemetry, kernel driver signals, FN-class ban discussions."
version: 1.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: game
  triggers:
    - "EAC ban"
    - "EasyAntiCheat"
    - "FN ban"
    - "EAC HWID"
---

# EAC ban-stack research

## Layers (typical)
1. **Usermode service/EOS process** — telemetry, module enumeration, heartbeats
2. **Kernel driver (`EasyAntiCheat.sys` class)** — callbacks, memory integrity, handle protection
3. **Server backend** — aggregates client evidence, HWID features, trust crypto
4. **Game account linkage** — bans stick to account + device features

## Research tasks
- Diff usermode vs kernel collection responsibilities
- Identify which IDs are local-only vs shipped remote
- Trace ban triggers: injection artifact, integrity fail, tamper, report spam
- Separate **detection** (instant kick) vs **silent flag** (delayed ban)

## Practical lab method
1. Baseline clean boot captures (procmon, ETW, driver list)
2. Introduce one variable at a time (mapper, overlay, debugger)
3. Record network destinations + payload sizes (not necessarily decrypt)
4. Correlate local artifacts with ban timing

## Evidence chain (how a kick becomes a ban)
1. Client artifact detected (kernel sweep / module scan / heartbeat failure) → immediate kick, evidence packet uploaded
2. Server aggregates: same HWID-class features across accounts → device-level flag
3. Ban waves: delayed days-weeks to protect detection methods; account ban + optional device ban (tournament/FN-class: device + payment features)
4. Appeal-resistant: telemetry-signed evidence bundles; spoofing detected post-ban escalates device trust scoring

## Kernel signal classes (from `eac-kernel-driver-re`)
- PsSet* / ObRegisterCallbacks registrations: handle-strip on game processes; OpenProcess rights audit
- MmCopyVirtualMemory/page-fault-driven scans; module integrity hashes on game + own modules
- Driver-load observation: image-load callbacks see every driver — manual-mapped drivers invisible to callbacks but visible to pool/PiDDB sweeps
- Heartbeat: usermode service ↔ driver ↔ server triple-path; killing the service = instant fail; the question is what latency/tolerance each path has (lab-measurable via `kevlar-driver-emulation` + live capture)

## HWID stack order (what actually carries a device ban)
| Signal | Collection point | Spoof cost |
|---|---|---|
| TPM EK/AIK | attestation or local TBS | unforgeable (can only fail/block) |
| SMBIOS/UUID | WMI/registry | medium (UEFI var surgery; consistency-checked) |
| Disk serial | ATA pass-through IOCTL vs registry cross-check | medium; inconsistent reads = spoofer flag |
| NIC MAC | registry/NDIS | trivial |
| GPU/board serials | registry/PCI caps | trivial-medium |
| MachineGuid / install IDs | registry | trivial |
- Cross-check consistency is the actual detector — read paths per `hwid-identifier-surfaces` and keep ALL of them coherent when rotating.

## Lab protocol (attribution-grade)
1. Hardware snapshot baseline (all queryable IDs + their query paths)
2. One-variable deltas on canary accounts; log game build, EAC build (`EasyAntiCheat_EOS.exe` version), driver version
3. Post-ban forensics: correlate ban timestamp vs first artifact introduction; a ban that precedes your variable = baseline compromise (HWID carryover), not the variable
4. Device-ban recovery test matrix: change one ID class at a time post-ban to rank what the device ban actually pins on (this is how TPM-vs-SMBIOS-vs-disk ranking was established for FN-class)

## Pair with
`eac-kernel-driver-re`, `eac-usermode-telemetry-re`, `hwid-identifier-surfaces`, `tpm-attestation-research`, `anti-cheat-bypass`.

## Refs
- UC: serials & EAC/FN bans; complete AC bypass sources lists; EAC sys/EOS RE threads
