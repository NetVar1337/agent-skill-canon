---
name: rust-driver-reconstruction
description: Rust reconstruction of closed-source Windows kernel drivers after reverse engineering, preserving WDM/KMDF architecture, ABI layouts, IOCTL contracts, IRQL, concurrency, PnP/power, and observable behavior. Use for reimplementing or porting a reversed .sys driver with windows-drivers-rs, wdk-build, wdk-sys, wdk, or cargo-wdk. Do NOT use for ordinary Rust applications or source-available driver refactors.
compatibility: Windows analysis host, isolated target VM or test machine, matching WDK/eWDK, WinDbg, and a PE decompiler.
---

# Rust Driver Reconstruction

## Objective

Produce an evidence-backed Rust implementation of a Windows driver that
matches the original driver's required external behavior. This is semantic
reconstruction, not decompiler-to-Rust translation.

Load `reverse-engineering` first when the binary has not yet been mapped. Load
`windows-driver-0day` when the goal is vulnerability discovery rather than
behavioral reconstruction. Use the repository's normal driver-development
workflow for new designs that do not require compatibility with a reversed
binary.

## Non-Negotiable Gates

- **Evidence gate:** No behavior is implemented solely from decompiler
  pseudocode.
- **Architecture gate:** Identify the driver model and stack role before
  selecting Rust project structure or APIs.
- **ABI gate:** Every shared, IOCTL, DMA, MMIO, persisted, and callback layout
  has verified size, alignment, offsets, and architecture variants.
- **IRQL gate:** No allocation, blocking call, pageable access, or lock is used
  at an IRQL where it is invalid.
- **Ownership gate:** Request, object, allocation, handle, and teardown
  ownership are explicit.
- **Unsafe gate:** Each `unsafe` block states the local invariant that makes it
  valid; the invariant must come from WDK semantics and recovered evidence.
- **Parity gate:** Required behavior is compared against the original under the
  same workload and environment.
- **Uncertainty gate:** An `unverified` fact cannot control an unsafe access,
  public ABI, IRQL decision, lock protocol, or completion path.

## Required Artifacts

```text
case/
|-- provenance.md          # Original hash, signer, INF/CAT, versions, systems
|-- evidence.csv           # Observed/inferred/unverified claims and sources
|-- architecture.md        # Driver model, stack role, object graph, entry paths
|-- callbacks.csv          # ABI, IRQL, ownership, synchronization, teardown
|-- ioctls.csv             # Codes, methods, access, schemas, status semantics
|-- state-machines.md      # Handles, requests, cancellation, PnP, power
|-- abi/                   # Rust assertions and same-WDK C layout oracle
|-- traces/original/       # Baseline workloads and debugger/ETW traces
|-- traces/rust/           # Matching traces from the reconstruction
|-- harness/               # Deterministic differential exerciser
|-- rust/                  # Rust driver workspace
`-- parity.md              # Differences, rationale, and completion results
```

Scale this layout down for small targets, but do not omit the evidence,
contract, and parity artifacts.

## Phase 1: Establish the Baseline

Before coding:

1. Preserve the `.sys`, INF, CAT, installer, symbols, companion services/DLLs,
   firmware, and representative hardware or VM state.
2. Record hashes, signer, PE architecture, OS builds, WDK/WDF versions,
   service configuration, VBS/HVCI state, test-signing state, and hardware IDs.
3. Capture repeatable workloads for install, load, open, normal I/O, invalid
   I/O, cancel, close, stop, remove, sleep/resume, unload, and reload.
4. Record exact statuses, output bytes and lengths, side effects, completion
   order, timing requirements, and externally visible names/security settings.

The original binary is the behavioral oracle, not a source-code oracle.

## Phase 2: Classify the Driver

Identify both model and role:

- Legacy NT control driver, WDM, KMDF, UMDF, minifilter, NDIS, Storport,
  AVStream, USB/class extension, or another framework.
- Bus, function, upper filter, lower filter, control device, software-only
  driver, or mixed role.
- PnP-aware versus legacy loading, hardware resources, DMA, interrupts, MMIO,
  firmware interaction, and lower-stack forwarding.

Do not automatically modernize WDM into KMDF. KMDF changes defaults for
serialization, forwarding, cancellation, object lifetime, PnP/power, and
callback IRQL. A framework migration requires a separately documented
compatibility argument.

If classification identifies UMDF or another user-mode component rather than a
kernel `.sys` implementation, stop applying the kernel-only Rust baseline in
this skill and switch to a user-mode reconstruction workflow.

### WDM Recovery Focus

- `DriverEntry`, `DriverUnload`, `DriverExtension->AddDevice`.
- `MajorFunction[]`, minor functions, completion and cancel routines.
- Pending propagation, remove locks, stack forwarding, detach/delete order.
- Device/file extensions, spin locks, events, DPCs, timers, work items,
  interrupts, and reference counts.

### KMDF Recovery Focus

- `WdfDriverCreate`, device creation, queue topology, file-object setup, and
  callback registrations.
- Queue dispatch mode, synchronization scope, execution level, automatic
  serialization, parent-child ownership, and cleanup/destroy callbacks.
- Request retrieval, completion, cancellation, forwarding, reuse, and object
  context types.

Never identify a KMDF API from a guessed `WdfFunctions` index. Generate or use
bindings for the exact WDF configuration and resolve the call through those
bindings.

## Phase 3: Specify Observable Contracts

For every ingress path, document:

- Preconditions, required handle access, caller mode, security context, and
  device state.
- Input/output buffer locations, minimum and maximum lengths, validation order,
  and aliasing.
- Exact success and failure statuses, status precedence, returned byte count,
  partial-output behavior, and initialization guarantees.
- Synchronous versus pending behavior, cancellation, timeout, completion
  context, and callback ordering.
- State changes, hardware effects, lower-stack requests, persistent effects,
  and cleanup behavior.
- x64, ARM64, and WOW64 variants where pointer width or alignment changes the
  contract.

### IOCTL Transfer Methods

| Method | Contract to Recover |
|---|---|
| `METHOD_BUFFERED` | Shared `SystemBuffer`, input/output aliasing, initialization, and `Information` length |
| `METHOD_IN_DIRECT` | Header in `SystemBuffer`, MDL-backed secondary input, mapping and length behavior |
| `METHOD_OUT_DIRECT` | Header in `SystemBuffer`, MDL-backed output, writable length, and partial completion |
| `METHOD_NEITHER` | Raw user pointers, requestor mode, process context, capture timing, SEH, and double-fetch behavior |

Also preserve IOCTL access bits, device ACLs, open requirements, internal versus
external controls, and request sequencing.

For `METHOD_NEITHER`, probe user addresses in the originating caller context,
guard each later user-memory access with kernel structured exception handling,
and capture data before asynchronous processing. A successful probe does not
make a later dereference safe and does not prevent a double fetch.

## Phase 4: Recover Concurrency, IRQL, and Lifetime

Create an IRQL call graph and a lock-order table. For each callback record:

- Possible IRQL, thread/process context, reentrancy, and serialization source.
- Pageable versus nonpageable code/data.
- Allocation type, ownership transfer, reference rules, and failure cleanup.
- Locks held, lock ordering, atomic fields, waiting behavior, and callbacks made
  while locked.
- Cancellation race, cleanup/close interaction, surprise removal, power
  transition, and unload constraints.

Model cancellation and PnP removal as state machines. A happy-path call graph
is not sufficient for a correct driver reconstruction.

## Phase 5: Prove the ABI

Use generated `wdk-sys` types for Windows structures. For proprietary boundary
types:

- Use `#[repr(C)]`, fixed-width integers, explicit unions/newtypes, and only
  evidenced packing.
- Assert `size_of`, `align_of`, and every recovered field offset.
- Compile a small C layout oracle with the same WDK, target architecture, and
  packing settings, then compare its output with Rust assertions.
- Treat C enums, flags, and bitfields as integer newtypes/constants when invalid
  values are possible; an invalid Rust enum discriminant is undefined behavior.
- Use `repr(packed)` only when packing is proven. Never create references to
  unaligned packed fields; use unaligned reads/writes.
- Parse flexible arrays and variable tails as checked byte ranges rather than
  casting an entire user buffer to a struct.
- Keep architecture-specific schemas separate where layouts differ.

Raw pointers are the default at FFI boundaries unless non-nullness, alignment,
aliasing, and lifetime are all proven.

## Phase 6: Choose and Pin the Rust Toolchain

Consult the current official documentation before scaffolding because the Rust
WDK ecosystem changes quickly. Prefer the Microsoft `windows-drivers-rs`
project and pin the known-good Rust toolchain, WDK/eWDK, LLVM/libclang, crate
versions, target, generated bindings, and Cargo lockfile.

Treat `windows-drivers-rs` and its tooling as early-stage until current official
documentation says otherwise. Confirm supported targets, WDF versions, DDIs,
packaging, signing, HLK, and production-support requirements before adopting it
for a shipping driver.

Typical crate roles:

| Crate/tool | Role |
|---|---|
| `wdk-build` | `build.rs`, WDK discovery, bindings/link configuration, driver metadata |
| `wdk-sys` | Raw generated WDM/WDF FFI, structures, constants, callbacks, WDF dispatch |
| `wdk` | Select safe conveniences only where their guarantees match the recovered contract |
| `wdk-alloc` | Optional kernel allocator after IRQL and alignment constraints are verified |
| `wdk-panic` | Kernel panic handler; understand and test its failure behavior |
| `cargo-wdk` | Build/package integration after pinning and validating the version used |

Expected baseline:

- `#![no_std]` kernel crate.
- Driver-compatible crate type and linker configuration from current official
  samples.
- `panic = "abort"`; no unwinding across FFI.
- No `unwrap`, `expect`, panic-prone indexing, unchecked size arithmetic, or
  infallible allocation assumptions on kernel request paths.

Do not assume every WDK DDI has a complete safe wrapper. Use `wdk-sys` where
needed and keep missing C-only facilities behind minimal, reviewed WDK shims.
Native kernel structured exception handling may require such a shim for
specific user-buffer paths.

Before using `wdk-alloc`, verify its current minimum Windows version, permitted
IRQL, and alignment behavior. Do not use it for over-aligned allocations unless
the selected version explicitly guarantees the required alignment.

## Phase 7: Design the Safety Boundary

Keep entry points and callbacks as thin `unsafe extern "system"` adapters:

```text
Windows/WDF callback
    -> validate raw handles, pointers, lengths, mode, and state
    -> capture or map data according to the exact transfer contract
    -> convert to owned/validated domain values
    -> call safe core state machine
    -> serialize output and complete/forward exactly once
```

Organize by responsibility, not by guessed original source files:

```text
rust/
|-- build.rs
|-- Cargo.toml
`-- src/
    |-- lib.rs             # Driver entry and exports
    |-- ffi.rs             # Thin WDK/WDF adapters and local invariants
    |-- abi.rs             # Boundary layouts and assertions
    |-- device.rs          # Per-device state and lifecycle
    |-- file.rs            # Per-handle state
    |-- ioctl.rs           # Parsing, validation, serialization
    |-- pnp_power.rs       # Explicit lifecycle state machines
    `-- core.rs            # Safe semantic behavior where practical
```

Keep the structure smaller when the target is small. Add a module only when it
creates a meaningful safety or ownership boundary.

## Phase 8: Implement Vertical Slices

Implement one complete path at a time:

1. Ingress and prerequisites.
2. Boundary parsing and validation.
3. State transition or hardware operation.
4. Output/status serialization.
5. Completion, cancellation, and cleanup.
6. Unit, integration, and differential tests.

Do not scaffold every decompiled function with guessed bodies. Do not preserve
compiler thunks, inlining boundaries, stack temporaries, tail-call artifacts,
or security-cookie paths as application architecture.

Security defects and undefined behavior are not copied by default. If exact
bug compatibility is required, isolate it, document it, and test it as an
explicit compatibility decision.

## Phase 9: Differential Verification

Run identical scripted workloads against the original and Rust drivers on
equivalent snapshots or hardware. Compare:

- Installation, device/interface names, ACLs, open/share/access behavior.
- Status values, status precedence, output length and bytes.
- Side effects, state transitions, hardware/firmware interactions.
- Pending/completion behavior, callback order, cancellation, and timing bounds.
- Multiple handles, malformed requests, low resources, repeated load/unload.
- PnP start/stop/remove/surprise-remove and sleep/resume where applicable.

Record intentional differences and their rationale in `parity.md`. A single
successful IOCTL is not parity.

## Test Ladder

| Level | Required Evidence |
|---|---|
| Host unit tests | Pure parsers, checked arithmetic, serialization, state machines, status mapping |
| ABI tests | Compile-time Rust assertions plus same-WDK C layout oracle |
| Property/fuzz tests | Variable tails, offsets/counts, malformed records, parser/serializer round trips |
| Differential tests | Original versus Rust outputs, statuses, side effects, and ordering |
| VM integration | Install/load/open/I/O/cancel/unload and interface/security behavior |
| Stress tests | Parallel handles, cancellation/close races, low resources, repeated lifecycle transitions |
| Verifier tests | Pool, IRQL, I/O, lock, DMA, WDF, and teardown checks as applicable |
| Compatibility matrix | Required OS builds, architectures, VBS/HVCI states, hardware/firmware versions |

Use WinDbg and targeted Driver Verifier settings. For KMDF, include WDF
Verifier and `!wdfkd` evidence. Broad verifier settings can distort timing, so
record exact settings and isolate the class being tested.

## Completion Gate

- [ ] Original driver behavior is captured by repeatable workloads.
- [ ] Driver model, stack role, WDF version, and lifecycle are established.
- [ ] External contracts include exact layouts, statuses, lengths, and timing.
- [ ] ABI checks pass against the same-WDK C oracle.
- [ ] IRQL, lock order, ownership, cancellation, and teardown are explicit.
- [ ] `unsafe` is localized and each operation has a documented invariant.
- [ ] Unit, ABI, property, differential, integration, stress, and applicable
  Verifier tests pass.
- [ ] Required OS/architecture/hardware matrix is tested.
- [ ] Intentional differences and unresolved uncertainty are documented.
- [ ] Packaging, signing, deployment, and production certification requirements
  are verified for the intended use.

## Primary References

- https://github.com/microsoft/windows-drivers-rs
- https://github.com/microsoft/Windows-rust-driver-samples
- https://learn.microsoft.com/windows-hardware/drivers/kernel/defining-i-o-control-codes
- https://learn.microsoft.com/windows-hardware/drivers/kernel/managing-hardware-priorities
- https://learn.microsoft.com/windows-hardware/drivers/kernel/handling-exceptions
- https://learn.microsoft.com/windows-hardware/drivers/wdf/using-automatic-synchronization
- https://learn.microsoft.com/windows-hardware/drivers/wdf/framework-object-life-cycle
- https://doc.rust-lang.org/reference/type-layout.html
