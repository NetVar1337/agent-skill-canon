---
name: c2-implant-engineering
description: "Use when designing, implementing, reviewing, or debugging a custom C2 implant, beacon, or agent runtime: versioned task protocols, cancellable jobs, encrypted framing, transport failover, module ABIs, sleep/wake behavior, self-update, and operator-to-agent conformance tests."
license: MIT
---
# C2 implant engineering

Use this skill to engineer the endpoint runtime behind a command-and-control system. Its unit of quality is a long-lived agent that remains protocol-correct, bounded, recoverable, and testable through reconnects, malformed input, module faults, and mixed-version operation.

## When to use

Use this skill for:

- custom implant, beacon, agent, or endpoint-runtime architecture;
- task/result framing, session establishment, replay protection, and schema evolution;
- job scheduling, streaming output, cancellation, deadlines, and reconnect persistence;
- transport adapters for HTTPS, WebSocket, DNS, named pipes, or other approved channels;
- BOF/COFF, shellcode, native module, script, or built-in command execution contracts;
- sleep, jitter, working hours, proxy handling, failover, kill date, or self-update behavior;
- mock team-server, protocol conformance, fault injection, compatibility, and lifecycle testing.

Do not use it to design redirectors, public listeners, DNS zones, certificates, or operator infrastructure; `c2-tradecraft` owns that control-plane surface. Do not let transport concerns leak into module execution or task semantics.

## Completion standard

A completed runtime has documented trust boundaries, a versioned machine-readable protocol, authenticated session establishment, cancellable task state machines, bounded resources, explicit module ownership, deterministic lifecycle tests, and a compatibility matrix. A successful one-shot callback is not an implant release gate.

## Core workflow

### 1. Define components and trust boundaries

Keep these components separable even if the first implementation is one binary:

```text
bootstrap/config -> identity -> session/crypto -> transport
                                  |
                                  v
persistent inbox -> dispatcher -> jobs -> services/modules -> result spool
                                  |
                                  v
                         telemetry, update, teardown
```

For each edge state:

- input schema and maximum size;
- caller and callee ownership;
- timeout and cancellation behavior;
- whether data survives reconnect or restart;
- sensitive fields and log policy;
- failure result visible to the operator;
- cleanup obligations.

Treat the network and task source as untrusted until server authentication and frame validation complete. Treat native modules as fault-prone extensions even when they are operator-authored. Keep server policy decisions out of endpoint parsing code.

### 2. Pin identity and configuration

Define immutable build identity separately from mutable campaign configuration:

| Field class | Examples | Rule |
| --- | --- | --- |
| Build | agent semantic version, protocol range, commit, target OS/arch, feature set | Embedded and reportable |
| Instance | installation/boot ID, generated agent ID, key slot | Unique and rotation-aware |
| Profile | endpoints, transport order, timing policy, proxy mode, feature gates | Signed and schema-validated |
| Runtime | current session, connection epoch, negotiated capabilities, clock estimate | Never serialized as static config |

Use a canonical config schema. Reject unknown security-critical fields, duplicate keys, invalid durations, impossible jitter ranges, oversized endpoint lists, unsupported proxy modes, and protocol ranges with no intersection. Validate before any network or module initialization.

Never use hostname, MAC address, or a stable hardware identifier as the sole identity. They collide, leak, and change. Generate a random instance identifier and carry host attributes as separately scoped observations.

### 3. Specify the wire protocol before transport code

Use a machine-consumed schema such as Protobuf, FlatBuffers, CBOR with a canonical profile, or a tightly specified binary format. Define byte order and integer widths. A useful envelope includes:

```text
magic/version | flags | header_length | connection_epoch
message_id | correlation_id | stream_id | sequence
message_type | payload_length | authenticated payload
```

Set explicit upper bounds before allocation. Include protocol major/minor, agent capability set, task schema version, and module ABI version independently; they evolve at different rates.

Core message families should include:

- client hello, authenticated server hello, and negotiated capabilities;
- heartbeat and server-time estimate;
- task offer, acceptance/rejection, progress, result, and acknowledgment;
- cancel request and terminal cancel result;
- stream open, ordered chunk, resumable offset, digest, and close;
- profile update, agent update, rollback status, and terminal shutdown reason;
- typed protocol error that never embeds raw secret material.

Do not infer task completion from transport acknowledgment. The protocol distinguishes receipt, acceptance, execution, result delivery, and operator acknowledgment.

### 4. Establish sessions with proven cryptography

Prefer TLS 1.3 with strict server authentication, or a reviewed Noise construction when an application-layer secure channel is required. Pin the intended trust root or server static public key in the signed profile. Do not invent a key exchange or stream cipher.

If application frames use AEAD:

- derive separate client-to-server and server-to-client keys;
- bind protocol version, direction, connection epoch, sequence, and header as associated data;
- guarantee nonce uniqueness for every key, including after crash and reconnect;
- rotate keys on a bounded byte/message/time policy and on reauthentication;
- reject replayed or out-of-window sequences before dispatch;
- erase superseded session keys as far as the runtime and language allow;
- never retry an AEAD encryption with the same key/nonce after partial failure.

Connection resumption must preserve replay guarantees. A durable message ID provides idempotency; it does not make nonce reuse safe. Keep encryption tests based on published vectors and cross-implementation fixtures.

### 5. Model tasks and jobs as state machines

Use an explicit state model:

```text
RECEIVED -> VALIDATED -> ACCEPTED -> QUEUED -> RUNNING
RUNNING -> SUCCEEDED | FAILED | TIMED_OUT | CANCELLED | CRASHED
QUEUED/RUNNING -> CANCEL_REQUESTED -> CANCELLED or terminal race result
```

Every transition emits one durable event with task ID, attempt ID, monotonic timestamp, state, and bounded detail. Invalid transitions fail closed and become protocol errors.

A task definition carries:

- globally unique task ID and optional idempotency key;
- operation type and schema version;
- creation, not-before, deadline, and maximum runtime policy;
- priority and resource class;
- input digest and bounded payload or stream reference;
- required capability and operator-side provenance;
- retry policy that names retryable failure classes.

A job owns its cancellation token, output streams, child processes/threads, temporary files, handles/descriptors, module instance, and deadline. Cancellation is cooperative first and forceful only through a component-specific containment boundary. Report partial side effects explicitly.

Use bounded worker pools and queues. Backpressure must reject or defer tasks rather than consume unbounded memory. Reserve capacity for cancellation, shutdown, and health messages so task saturation cannot deadlock control flow.

### 6. Make reconnect and delivery semantics explicit

Choose and document delivery semantics per message family:

- task offer: at-least-once delivery with agent-side task-ID deduplication;
- side-effecting task: idempotency contract or explicit duplicate rejection;
- stream chunk: offset plus digest, acknowledged cumulatively or selectively;
- result: durable until server acknowledgment, with a bounded retention policy;
- heartbeat: lossy and never persisted.

Persist only the minimum state required to recover. Authenticate and version the spool. Enforce total bytes, record count, age, and per-task quotas. A corrupted spool must quarantine a record and continue bounded recovery rather than crash-loop the agent.

Reconnect uses exponential backoff with a cap and randomized jitter, but tests use an injected clock and deterministic random source. Reset backoff only after a meaningful healthy interval, not one TCP connect. Use circuit-breaker state so a dead endpoint does not starve alternatives.

### 7. Isolate transport adapters

A transport implements a narrow contract:

```text
connect(context) -> authenticated byte channel
send(frame) -> accepted-for-delivery or typed error
receive() -> complete bounded frame or typed error
close(reason) -> terminal completion
peer() -> authenticated endpoint metadata
```

The session layer owns framing, cryptography, replay, and message semantics. The transport owns proxy negotiation, DNS resolution, connection establishment, TLS carrier behavior, read/write deadlines, and carrier-specific size limits.

For HTTPS or WebSocket, pin method/path/header/body rules and test HTTP/2 versus HTTP/1.1 behavior. For proxies, cover explicit proxy, environment policy, authenticated proxy, PAC output, DNS-at-proxy versus DNS-local, and connection reuse. For named pipes, define server identity, ACL expectation, impersonation behavior, message/byte mode, and reconnect ownership.

Do not claim two carriers have the same observable profile. Capture each carrier's DNS, TLS, HTTP, timing, connection reuse, and payload-size distribution in the test lab.

### 8. Define the module ABI and memory lifecycle

Every executable extension has a manifest:

```text
module ID/version | format | target OS/arch | ABI version
entry point | required host capabilities | input/output schemas
maximum runtime/output/memory | concurrency rule | cleanup contract
```

The host owns allocations crossing the ABI unless the ABI says otherwise. Pair every allocate/free function in the same runtime. Never return pointers to module stack or transient loader memory. Version host service tables by size and capability bits so older modules do not call beyond the table.

For in-memory native execution:

1. parse and validate the complete object before mapping;
2. reject architecture, relocation, import, ABI, and size mismatches;
3. map writable memory only while relocating;
4. transition final pages to the minimum W^X-compatible protection;
5. register any required unwind metadata before execution;
6. contain exceptions at the host boundary where the platform permits;
7. bound output, runtime, threads, and child activity;
8. unregister metadata and release all mappings on every terminal path.

BOF-specific relocation and Beacon API behavior belongs to `bof-coff-development`. Generic position-independent payload construction belongs to `offensive-shellcode`. The implant owns admission, containment, I/O, cancellation, and cleanup.

Out-of-process workers provide a stronger crash boundary than in-process exception handling. Use them for unstable or third-party modules when process creation and telemetry constraints permit.

### 9. Engineer sleep, wake, and working-time behavior

Represent timing as policy, not scattered calls:

- base interval, jitter distribution, minimum and maximum;
- allowed working windows and timezone source;
- maximum offline interval and forced health-check rules;
- server-directed wake with authentication and replay protection;
- monotonic deadlines versus wall-clock calendar decisions;
- suspend/resume and large clock-jump behavior.

Calculate jitter with a cryptographically strong generator in production and an injected seeded generator in tests. Clamp before conversion to platform duration types. Never allow overflow, negative duration, or a zero-delay reconnect storm.

A kill date is signed policy evaluated against a defensible time source. Define behavior for clock rollback, unavailable server time, expired profile, and teardown failure. It is not a substitute for server-side revocation.

### 10. Make update and teardown transactional

An update package has a signed manifest with agent version, protocol range, target OS/arch, payload hash, size, minimum rollback version, and signer key ID. Verify signature and hash before staging.

Use an A/B or side-by-side update flow:

```text
download -> verify -> stage -> preflight -> handoff -> health proof -> commit
                                             |
                                             +-> timeout/failure -> rollback
```

Prevent downgrade unless policy explicitly allows the signed target. Preserve enough prior state for rollback, but never copy live session nonces or keys into the new process.

Teardown stops intake, cancels or drains jobs according to policy, flushes terminal results within a bound, closes transports, unloads modules, deletes owned temporary state, releases secrets, and reports what could not be removed. Make teardown idempotent.

### 11. Build the conformance and fault harness

Create a mock team server that can script exact protocol events and expose barriers. Tests subscribe to the expected state transition before triggering it; they do not wait by sleep.

Required suites include:

- published crypto vectors and cross-language frame fixtures;
- every supported major/minor and capability negotiation outcome;
- unknown fields, truncation, overlong lengths, decompression bombs, bad tags, replay, and reordering;
- duplicate task offers and side-effect idempotency;
- cancellation before queue, during execution, during output, and racing completion;
- connection loss at every frame and stream boundary;
- partial upload/download resume and digest mismatch;
- queue saturation, spool quotas, low disk, and clock jumps;
- module load failure, timeout, exception/crash, output overflow, and leaked-worker detection;
- update success, failed health proof, rollback, downgrade rejection, and kill-date expiry;
- proxy failures, endpoint failover, suspend/resume, and clean shutdown.

Run language and platform diagnostics appropriate to the implementation:

```bash
buf lint
buf breaking --against '.git#branch=main'
go test -race ./...
cargo test --all-targets
```

Use fuzzers on frame decode, decompression, config parse, task decode, spool recovery, and module metadata before those inputs reach allocation or execution. Keep a minimized regression corpus.

For network impairment in a disposable Linux lab, capture and restore the prior qdisc exactly; do not leave global test state behind. Assert normalized carrier properties such as request sequence and size classes, not equality of randomized ciphertext.

## Key structures & interfaces

- `BuildIdentity`, `InstanceIdentity`, `SignedProfile`, and `NegotiatedCapabilities` must remain distinct types.
- `Envelope` owns framing metadata; `Message` owns one validated typed payload.
- `Session` owns authenticated peer state, directional keys, connection epoch, sequence windows, and clock estimate.
- `Transport` moves complete encrypted frames and exposes typed carrier errors.
- `Dispatcher` validates capability and schema before creating a `Job`.
- `Job` owns cancellation, deadline, resources, progress, output, and one terminal state.
- `ResultSpool` provides bounded authenticated persistence and acknowledgment-aware deletion.
- `ModuleManifest`, `HostServices`, and `ModuleInstance` define ABI, ownership, and teardown.
- `Updater` owns signed package verification, staging, handoff, health proof, commit, and rollback.
- `Clock`, `RandomSource`, and `NetworkFaults` are injectable test interfaces; production implementations remain OS-backed.

## Tooling

| Need | Tools |
| --- | --- |
| Schema lifecycle | Protobuf/`buf`, FlatBuffers, CDDL/CBOR validators, JSON Schema for profiles |
| Crypto validation | TLS test servers, Noise test vectors, Wycheproof, platform crypto APIs |
| Protocol inspection | Wireshark dissector, `tshark`, mitmproxy in a lab, structured frame dumper |
| Fault injection | Toxiproxy, Linux netem, proxy fixtures, injected clocks and barriers |
| Native diagnostics | ASan, UBSan, TSan, Valgrind, Application Verifier, WinDbg, GDB |
| Fuzzing | libFuzzer, AFL++, cargo-fuzz, go fuzzing, malformed spool corpus |
| Compatibility | Matrix CI across agent/server versions, OS, architecture, proxy, and transport |
| Release evidence | SBOM, reproducible build metadata, code signing, manifest and binary hashes |

## Evidence outputs

```text
architecture.md      components, trust boundaries, ownership, failure domains
protocol/            schemas, version rules, vectors, message/state diagrams
compatibility.csv    agent/server/protocol/module combinations and outcomes
lifecycle.md         startup, reconnect, sleep, cancellation, update, teardown
fault-runs.jsonl     injected fault, expected transition, observed terminal state
captures/            sanitized PCAPs, normalized wire-profile assertions
release/             hashes, signatures, SBOM, build identity, conformance receipt
```

Keep secrets, live endpoint values, tokens, and private keys out of logs and fixtures. Use deterministic synthetic values for shipped test vectors.

## Pitfalls & OPSEC

- Never reuse an AEAD nonce/key pair, including across crash recovery or resumed sessions.
- Never deserialize, decompress, allocate, or dispatch before enforcing authenticated size and schema bounds.
- Never let transport retries duplicate a side effect without a task-level idempotency decision.
- Never use fixed sleeps in lifecycle tests; await the precise transition with a bounded timeout.
- Do not treat in-process exception handling as complete isolation for native modules.
- Do not let one blocked job consume the dispatcher, transport receive loop, or shutdown path.
- Avoid plaintext task data, credentials, host inventory, module output, or endpoint lists in diagnostic logs.
- Treat proxy credentials, PAC results, DNS mode, TLS fingerprints, and connection reuse as carrier-specific evidence.
- Bound offline queues and spool lifetime; encrypted disk artifacts still expose size, timing, and existence.
- Zeroization is best-effort in managed runtimes; minimize copies and lifetime instead of promising perfect erasure.
- Make profile changes, update, rollback, and teardown produce receipts; silent partial state is an operational failure.
- Test sensor visibility and system stability with `windows-telemetry-etw` or the platform's telemetry owner rather than assuming quiet traffic means quiet execution.

## Routing

- Route listener, redirector, domain, certificate, and egress infrastructure to `c2-tradecraft`.
- Route Linux kernel privilege-escalation primitives to batch-B sibling `linux-kernel-exploitation`.
- Route eBPF verifier/JIT research or controlled BPF instrumentation to batch-B sibling `ebpf-offensive`.
- Route Linux host enumeration, credential provenance, persistence assessment, and lateral pivots to batch-B sibling `linux-host-post-exploitation`.
- Route BOF/COFF object format, relocation, import, and Beacon API work to batch-A sibling `bof-coff-development`.
- Route Windows RPC, COM, DCOM, NDR, or ALPC transport boundaries to batch-A sibling `windows-rpc-com-attack`.
- Route Windows ETW, WPP, TraceLogging, event-loss, and sensor measurement to batch-A sibling `windows-telemetry-etw`.
- Route Hyper-V guest/root, VMBus, hypercall, or worker-process boundaries to batch-A sibling `hyper-v-offensive`.
- Route generic payload encoding and position-independent code to `offensive-shellcode`; route endpoint detection hypotheses to `edr-bypass-re`.

## Final gate

- [ ] Component boundaries, ownership, limits, and failure behavior are documented.
- [ ] Protocol schemas, crypto vectors, replay rules, and version negotiation pass conformance.
- [ ] Jobs are cancellable, bounded, idempotency-aware, and terminally observable.
- [ ] Reconnect, spool, stream resume, proxy, failover, and clock-jump cases are tested.
- [ ] Module ABI enforces architecture, ownership, W^X, timeout, output, and cleanup rules.
- [ ] Update rollback, kill date, shutdown, and residual-artifact checks pass.
- [ ] Compatibility and fault matrices cover every supported release combination.
