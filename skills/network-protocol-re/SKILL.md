---
name: network-protocol-re
description: "Protocol reverse engineering for games/apps: capture layers (pcap, proxy, ETW, API hooks), framing/opcode discovery, crypto detection via constants and entropy, TLS interception at pre-encryption hooks, protobuf inference, parser reimplementation, replay and mutation fuzzing. Use when reversing any custom wire or socket protocol."
version: 2.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: re
---

# Network protocol RE

Goal: a reimplementation that speaks the protocol well enough to (a) decode
all traffic, (b) craft valid messages, (c) fuzz the parsers — in that order.

## 1. Capture (pick layers by what you need)

| Layer | What you get | Tooling on this box |
|---|---|---|
| Wire pcap | everything post-NIC | Wireshark **not installed** — install first or decode offline (tshark/scapy in WSL) |
| Local proxy | per-app view, easy mutate | mitmproxy (pip) for HTTP(S)-shaped traffic |
| ETW | per-process socket events, no hooks | `logman` + Microsoft-Windows-Winsock-AFD provider |
| API hooks | cleartext **before** encryption + call stacks | `frida-dbi`: hook `ws2_32!send/recv/WSASend`, `schannel!EncryptMessage/DecryptMessage`, openssl `SSL_write/SSL_read` if bundled |

Frida is the workhorse: it sees pre-encryption plaintext for TLS (hook
`EncryptMessage` input `SecBufferDesc` for schannel apps) and exposes the
calling module via backtrace — tying every packet to code is what makes step 3
fast.

## 2. Framing discovery

Read a session as a byte stream and answer, per direction:

- Delimiting: fixed size? length-prefix (u8/16/32, LE/BE, at fixed offset)?
  delimiter byte/sequence? length includes header?
- Header shape: typical `{len, opcode/cmd, seq/nonce, flags}`. Find the
  opcode by differential capture: do the *same* action twice, diff the stable
  bytes; do *different* actions, the bytes that track action identity are your
  opcode candidates.
- Sequence/ack fields: monotonic counters = replay protection (step 5's
  problem).
- Entropy profile per segment: header low-entropy/structured, body high =
  encrypted payload behind a structured header (very common in games:
  plaintext header + encrypted body, sometimes per-packet keys derived from a
  handshake).

## 3. Crypto & compression detection

- Constant scan (on the client binary, `pattern-scanner`/IDA): AES S-box /
  Rcon (even AES-NI builds ship tables for key expansion), TEA/XTEA delta
  `0x9E3779B9`, ChaCha `expand 32-byte k`, SM4 S-box, CRC32/Adler tables,
  zlib `78 9C` magic in captures.
- Behavior: does byte 0 of plaintext predictably map (XOR/stream)? Reconnect
  and compare — same plaintext, different ciphertext ⇒ per-session keys
  (handshake analysis needed); identical ciphertext ⇒ static key (extract it).
- Compression: zlib/lz4/zstd magic or decompress-API imports in the client;
  compression usually wraps the *whole* body pre-encryption.
- Key schedule RE: the handshake messages + the client's key-derivation
  function (find via crypto-constant xrefs) — this is where most custom
  protocols actually break.

## 4. Structure inference (protobuf & friends)

- Protobuf without .proto: field-tag heuristics — varint keys with
  `(field# << 3) | wire_type` structure, repeated patterns; feed candidate
  blobs to `protoc --decode_raw` and iterate on a recovered .proto.
- gRPC: HTTP/2 framing is self-describing; focus on message internals.
- Custom binary: build the field table empirically — capture N sessions,
  align by opcode, classify each offset as const/counter/enum/float/len/
  string; automate with a diff script, don't eyeball 200 packets.

## 5. Reimplementation

- Parser skeleton in Rust/C++: decode into typed structs with unknown-bytes
  preservation (never drop unknowns — they're future fields and fuzz material).
- Session state: replay counters, keepalive timing, ordering constraints.
- Replay discipline in live labs: honor nonce/seq rules or the server drops
  you before you learn anything; build a *responder* (fake server) for the
  client first when possible — it makes fuzzing the client side trivial.

## 6. Fuzzing

- Mutate at the semantic layer (fields, not bytes) once the schema is known;
  byte-flips before that.
- Targets in order of value: server parsers (via proxy, watch for
  disconnects/crashes/error-class oracles), the client's decompress/decode
  path (malformed body), handshake state machine.
- Feed crashes/flags back into the field table (`aisolve`/`exploit-dev` for
  the follow-on).

## MMO/AC specifics

- Opcode tables are often encrypted/obfuscated in the client — dump them at
  runtime (frida hook on the dispatch function) rather than statically.
- Heartbeat/pacing: replicating timing matters for not getting kicked during
  research; respect the game's ToS-boundary rules of your engagement.
- Packet-level AC telemetry (movement validation) — decode before you craft,
  or you're just writing a ban generator.

## Pair with

`frida-dbi` (capture + plaintext hooks), `pattern-scanner` (crypto constant
location), `protocol-reverse` (schema/formal side), `exploit-dev` (parser
bugs → primitives), `attack-chain` (where protocol fits the full path).
