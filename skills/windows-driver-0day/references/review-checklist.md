# Windows Driver Review Checklist

Use this checklist after mapping dispatch and request schemas. It is a review aid, not evidence by itself.

## Reachability and authorization

- Device or interface security descriptor grants more access than intended.
- `FILE_ANY_ACCESS` is used for a privileged operation.
- Handle desired-access checks differ from IOCTL access bits.
- Admin-only companion service exposes the same primitive to standard users.
- `RequestorMode`, `ExGetPreviousMode`, impersonation, or caller process is assumed rather than checked.
- Kernel-mode requests and user-mode requests share a handler with weaker validation.
- Internal IOCTLs are reachable through an exposed forwarding path.
- Object/handle lookup uses `KernelMode` for a user-supplied handle.
- Cross-handle object IDs lack ownership or session binding.

## Buffer provenance

- `METHOD_NEITHER` pointer is dereferenced without probe and structured exception handling.
- User memory is probed and then read again instead of captured once.
- Embedded pointer is trusted because the outer structure was captured.
- Pointer/length pair can be changed concurrently.
- MDL byte count, mapped length, and logical request length disagree.
- `MmGetSystemAddressForMdlSafe` failure is ignored.
- Direct-I/O direction is treated as an authorization guarantee.
- KMDF unsafe user-buffer APIs are used outside the required request context.
- Buffer remains referenced after IRP/request completion, cancellation, cleanup, or process exit.
- Input and output aliasing violates an unstated assumption.

## Arithmetic and bounds

- Addition is checked only after overflow: `offset + length <= total`.
- Multiplication wraps before allocation or copy.
- 64-bit length narrows to 32 or 16 bits.
- Signed negative value passes a maximum-only comparison.
- Element count is validated but byte count is not.
- Header size is subtracted before checking the buffer contains the header.
- Flexible-array or variable-tail size omits alignment/padding.
- Loop bound and allocation use different units.
- Output `Information` exceeds initialized or available output bytes.
- Integer division/rounding causes under-allocation.
- WOW64 structure conversion changes pointer or size semantics.

## Memory corruption and disclosure

- User-controlled length reaches copy, set, compare, compression, crypto, or DMA operations.
- Index selects function pointer, vtable, register table, or fixed array without a complete bound.
- Allocation type/size differs from subsequent cast or initializer.
- Structure version controls a larger field access without matching length validation.
- Error path frees an object also owned by a completion or cleanup path.
- Partial initialization is returned to user mode.
- Pool or stack padding is included in output.
- Failed operation completes with a stale success length.
- Output buffer contains kernel pointers, handles, physical addresses, cookies, or uninitialized flags.

## Lifetime, cancellation, and concurrency

- Request can be completed twice.
- Cancel routine and normal completion race over the same ownership flag.
- Cancel-safe queue protocol is incomplete.
- `CLEANUP`, `CLOSE`, unload, surprise removal, and work-item completion share an object without a stable reference.
- Timer/DPC/work item outlives its parent device or file context.
- Refcount increment occurs after publication or asynchronous enqueue.
- Refcount decrement/free is outside the lock that protects lookup.
- Lock is dropped between validation and use of mutable shared state.
- Parallel KMDF queue reaches code designed for sequential dispatch.
- Framework synchronization scope excludes a raw WDM callback touching the same state.
- Lock order changes between normal and error paths.
- User-triggered wait occurs while holding a lock needed by completion.
- Per-file context is looked up after cleanup can free it.

## Kernel object and process operations

- PID is used after process exit without an object reference.
- `PEPROCESS`, `PETHREAD`, token, section, event, or file object references are imbalanced.
- User handle is accepted with wrong type or desired access.
- Kernel pointer is accepted directly or derived from a weak cookie.
- Process attach/detach is unbalanced on exception or error paths.
- User virtual address is interpreted in the wrong process context.
- Callback registration/unregistration races with unload.

## Hardware and privileged primitives

- User controls physical address or mapping size.
- MMIO/physical range allowlist can overflow or has inclusive-end mistakes.
- Mapping crosses out of an approved BAR/range.
- Arbitrary MSR, control-register, I/O-port, PCI configuration, or firmware access is exposed.
- DMA target, descriptor count, or scatter/gather list is user-controlled without IOMMU-safe validation.
- Cache type and mapping lifetime are inconsistent.
- Kernel virtual memory, process memory, CR3/page tables, or page-frame numbers are exposed.
- User controls a callback, function pointer, shellcode address, or indirect call target.
- A nominally restricted primitive can target security-critical registers or memory through truncation/aliasing.

## PnP, power, and stack behavior

- IRP forwarded after completion or completed after forwarding without ownership.
- Completion routine returns the wrong continuation status.
- Pending status or pending bit is mishandled.
- Remove lock is missing, released early, or leaked.
- Device extension is used after surprise removal.
- Start/stop state is checked without synchronization.
- Lower-device result length/status is trusted when copying to user output.
- Filter assumes a specific lower-stack structure or IOCTL schema.
- Power transition races with mapped memory, DMA, queues, or worker threads.

## Differential and variant review

- Patched version adds a check in only one dispatch path.
- Equivalent 32-bit, internal, fast-I/O, or compatibility path is unchanged.
- Shared helper has callers that do not satisfy its new precondition.
- Added lock omits error, cancel, or teardown paths.
- Added length check uses a different unit than the sink.
- Fix prevents the crash but preserves information disclosure or privileged operation.
- Related driver packages contain copied vulnerable code.
- Older and newer branches received materially different fixes.

## Evidence to capture for a finding

- Driver SHA-256, signer, version, architecture, and installation source.
- OS build, VBS/HVCI state, VM configuration, and symbols.
- Device path, ACL, caller token, handle access, IOCTL, and transfer method.
- Exact request bytes, pointer layout, sequence, timing, and returned status.
- Static path from dispatch to root cause with addresses and renamed functions.
- Crash dump, debugger transcript, verifier configuration, and allocation/free stacks.
- Reproduction rate from a clean snapshot.
- Minimal trigger and negative controls.
- Results on adjacent versions and known-vulnerability search terms.
