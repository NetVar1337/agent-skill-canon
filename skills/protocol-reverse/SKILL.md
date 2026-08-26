---
name: protocol-reverse
description: Use for authorized reverse engineering of custom binary protocols, Protobuf/gRPC, WebSocket frames, and PCAP-driven protocol recovery.
---

# Protocol Reverse Engineering

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read `../field-journal/precedent-reverse.md` — confirm authorization and routine operational boundaries
2. `NOW`: confirm the task is **protocol/traffic/serialization format** reverse engineering (not pure web parameter signing → go to `js-reverse/`)
3. `NOW`: if there is network interaction with a target → complete scope via `../scripts/case-init.ps1`; ACT against the target is forbidden unless `auth` is granted
4. `NEXT`: read `../tool-index.md`; bootstrap missing tools (tshark/wireshark etc. may need manual install)
5. `ACT`: enter workflow Phase 1 and produce a draft frame layout or message dictionary

## Applicable Scenarios

- Custom TCP/UDP binary protocols
- Protobuf / gRPC / FlatBuffers / MessagePack
- WebSocket / MQTT / proprietary RPC
- PCAP / PCAPNG field and state-machine recovery
- Client-server validation, sequence numbers, encrypted frame headers

## Not This Skill

| Situation | Where to go |
|------|------|
| HTTP parameter signing only / JS encryption | `js-reverse/` |
| TLS certificate issues only | `pentest-tools/` or a browser proxy |
| Deep protocol-stack digging inside firmware + emulation | `firmware-pentest/` then back to this skill |

## Workflow

### Phase 1 — Capture and Triage

```text
□ Obtain samples: PCAP / proxy export / client logs / binary
□ Mark direction: C→S / S→C; any handshake, heartbeat, reconnect
□ Fixed header? Magic number? Length field? TLV? Fixed length?
□ Any compression (zlib/gzip/lz4) or encryption (AES/ChaCha in-frame)
□ tshark -r cap.pcap -T fields -e frame.number -e ip.src -e tcp.payload
```

### Phase 2 — Frame Layout Recovery

```text
□ Align multiple same-type messages; find invariant bytes / incrementing sequence numbers
□ Length field: big/little endian, header-inclusive or not
□ Validation: CRC16/32, checksum, HMAC position
□ Draw the state machine: Connect → Auth → Ready → Request/Response → Close
□ Tools: Wireshark custom dissector draft / ImHex / 010 Editor templates / Kaitai Struct
```

### Phase 3 — Serialization and Encryption

```text
□ Protobuf: .proto recovery (blackboxprotobuf / pbtk / protoc --decode_raw)
□ gRPC: HTTP/2 headers + protobuf body
□ Encryption: find key derivation (client so/dll/JS) → combine with ida-reverse / js-reverse / apk-reverse
□ Replay: only within the authorized scope; harmless fields first, sensitive operations later
```

### Phase 4 — Deliverables

```text
MUST produce:
- A message-type table (name / opcode / fields)
- At least 1 reproducible decode command or script
- Evidence: raw hex excerpt + decode result (sanitized)
```

## Toolchain

| Tool | Required | Purpose | Bootstrap |
|------|------|------|------|
| tshark / Wireshark | Strongly recommended | PCAP parsing | Manual / winget |
| Python3 | Yes | Decode scripts | System |
| blackboxprotobuf | Optional | Unknown protobuf | pip |
| ImHex / 010 | Optional | Structure templates | Manual |
| IDA / r2 / Ghidra | As needed | Client serialization functions | See the corresponding skill |

## References

- `references/protocol-workflow.md` — frame layout and Protobuf quick reference
- Related: `../ida-reverse/` `../js-reverse/` `../firmware-pentest/` `../pentest-tools/`

## Routing Context

**Upstream**: `MASTER-ROUTING` R21 · `routing.md`  
**Downstream**: need client algorithms → `ida-reverse`/`js-reverse`; need exploitation/replay → `pentest-tools`/`api-security`  
**Peers**: `malware-analysis` (C2 protocols), `digital-forensics` (traffic forensics)

## Task Completion Self-Check

- [ ] Recovered a message layout or state machine (not just pasted hex)?
- [ ] A reproducible decode command exists?
- [ ] Scope / data sanitization respected?
- [ ] field-journal updated / report Checklist?
