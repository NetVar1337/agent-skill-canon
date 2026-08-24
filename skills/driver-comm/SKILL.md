---
name: driver-comm
description: "Usermode↔kernel driver communication engineering: IOCTL design and hardening, METHOD_NEITHER double-fetch traps, inverted-call queues, shared-section rings + events, device ACL/SDDL, stealth device naming, client authentication and version handshake. Use when designing or reversing the control channel of any custom or third-party driver."
version: 2.0.0
license: GPL-3.0-or-later
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: stealth
---

# Driver communication

Design goals, in priority order: (1) kernel never trusts user input, (2) channel is
hard to discover and attribute, (3) protocol survives driver/client version skew.

## Channel selection

| Channel | Best for | Cost |
|---|---|---|
| IOCTL on named device | request/response control | visible `\DosDevices` link |
| IOCTL on unnamed device / private interface | stealth control | client needs a way to find the device |
| Inverted call (pending IRP queue) | kernel→user async events (e.g. process-create alerts) | IRP lifetime complexity |
| Shared section + events | high-rate data (frames, telemetry rings) | user can tamper with shared memory |
| ALPC port (rare) | RPC-style, inherits ALPC tooling | more exposed surface |

Combine: IOCTL for control, inverted-call for events, section for bulk.

## IOCTL protocol design

- Define codes with `CTL_CODE(FileDevice, Function, Method, Access)`; reserve
  `Function 0x800` range (user) and keep a `IOCTL_GET_VERSION` first.
- Every request struct starts with `{ UINT16 size; UINT16 version; }` and the
  dispatch checks `Parameters.DeviceIoControl.InputBufferLength` against it
  before touching anything. One length check per field group, no exceptions.
- Sequence-sensitive ops carry a 64-bit monotonic cookie; driver rejects replay.

### Buffer method tradeoffs

- `METHOD_BUFFERED`: driver gets `Irp->AssociatedIrp.SystemBuffer`, copied by
  the I/O manager. Safest default; costs a copy. Input and output share one
  buffer — set `OutputBufferLength` expectations explicitly.
- `METHOD_IN_DIRECT`/`METHOD_OUT_DIRECT`: user pages locked via MDL
  (`Irp->MdlAddress`). Good for large buffers; avoid on hot paths that can be
  paged out.
- `METHOD_NEITHER`: raw user pointers (`Type3InputBuffer`, `UserBuffer`).
  Fast but the classic vuln source:
  1. Capture pointer and length on first touch, `ProbeForRead`/`ProbeForWrite`
     in a `try/except`, then copy to kernel memory **before** validation logic.
  2. Never dereference user memory twice — second read may hit different data
     (double fetch → TOCTOU). One probe, one capture, one use.
  3. Reject buffers in kernel VA range; watch for 0-length + wraparound
     (`ptr + len` overflow).

### Dispatch skeleton (WDM)

```c
switch (ioctl) {
case IOCTL_CMD_X: {
    auto in  = (CMD_X*)(UCHAR*)Irp->AssociatedIrp.SystemBuffer;
    if (stk->Parameters.DeviceIoControl.InputBufferLength < sizeof(CMD_X))
        { status = STATUS_INFO_LENGTH_MISMATCH; break; }
    // capture everything needed from `in` into locals NOW (single fetch)
    status = HandleCmdX(Irp, in->arg1, in->arg2, &reply_len);
    Irp->IoStatus.Information = reply_len;
    break;
}
default:
    status = STATUS_INVALID_DEVICE_REQUEST; // do not log unknown codes
}
```

- Complete the IRP at the IRQL you received it (PASSIVE for DeviceIoControl);
  never queue-pending without `IoMarkIrpPending` + returning STATUS_PENDING.
- Zero `Information` on every error path.

## Inverted call model (kernel→user events)

1. User issues a blocking `ReadFile`/IOCTL; driver enqueues the IRP on a
   dedicated `KQUEUE` or list, returns `STATUS_PENDING`.
2. Kernel-side producer (callback, DPC→workitem) takes an IRP, writes the
   event payload into its buffer, `IoCompleteRequest`.
3. Rules: cancel routine must be set (`IoSetCancelRoutine`) so process death
   doesn't orphan IRPs; inspect `Irp->Cancel` under the cancel lock before
   completing; cap queue depth and drop (with counter) rather than grow.

## Shared section + events

- `ZwCreateSection(NULL, SECTION_ALL_ACCESS, NULL, &maxSize, PAGE_READWRITE,
  SEC_COMMIT, NULL)`; map twice: `ZwMapViewOfSection(..., BaseAddress=NULL, ...)`
  in user process context at client startup, and a system-space mapping for the
  driver (`ZwMapViewOfSection` with `ViewShare` into system space or build an
  MDL from the user pages — prefer the former).
- Layout: header `{ magic, version, head, tail, overflow }` + power-of-2 ring of
  fixed-size frames. Single-producer/single-consumer per direction; the header
  fields written by only one side each, ordinary release/acquire ordering —
  avoids needing a shared mutex.
- Notify: user→kernel via IOCTL kick (or polling); kernel→user via
  `KeSetEvent` on an event object the client passed
  (`OBJECT_ATTRIBUTES` on a named event, or duplicate from a handle in the
  IRP using `ObReferenceObjectByHandle`).
- Trust model: every field read from shared memory is attacker-controlled.
  Ring indices validated with mask before any kernel-side use; never store
  pointers in shared memory, only indices/offsets.

## Security descriptor and discoverability

- Default device ACL already restricts to Admin/SYSTEM; tighten explicitly with
  `IoCreateDeviceSecure(..., &SDDL_DEVOBJ_SYS_ADMIN_ACE, ...)` (wdmsec) or the
  WDF equivalent `WdfDeviceInitAssignSDDL`. Deny-by-default beats allow-lists
  of well-known SIDs that change per SKU.
- Stealth naming: avoid `\Device\GameHints` + `\DosDevices\GameHints`. Prefer
  an opaque GUID-ish name, **no** DOS symlink, and hand the client the internal
  path (`\\Device\\...`) or use a private `IoRegisterDeviceInterface` GUID
  enabled only when needed. Device objects are still enumerable via
  `NtQueryDirectoryObject` on `\Device` — treat discovery resistance as
  speed-bump, not secrecy.
- No `DbgPrint` on any steady-state path; a debug channel behind a compile-time
  flag only.

## Client authentication

1. `IoGetRequestorProcess(Irp)` (or `PsGetThreadProcess(Irp->Tail.Overlay.Thread)`)
   → EPROCESS. Compare full image path (via `SeLocateProcessImageName`) against
   an allow-list; treat name checks as weak (hollowing beats them).
2. Stronger: challenge–response. `IOCTL_CHALLENGE` returns a nonce; client
   answers `HMAC-SHA256(k, nonce)` with `k` derived at build time; constant-
   time compare. Rotating `k` per build is enough for lab/red-team tooling.
3. Rate-limit failed auth per EPROCESS; wipe state on process exit
   ( PsSetCreateProcessNotifyRoutine to catch dies).

## Version handshake

- On connect: client calls `IOCTL_GET_VERSION`; mismatch → driver returns
   supported range and both sides refuse. Structs embed `version`; dispatch
   switches on it. Never `sizeof(struct)` as a version identity — fields move.

## Reversing an existing channel (analysis mode)

1. Find the device: strings/`IoCreateDevice`/`IoCreateDeviceSecure` xrefs,
   symlink creation, or walk `\Device` + `\GLOBAL??` for candidates.
2. Recover the IOCTL table: locate `MajorFunction[IRP_MJ_DEVICE_CONTROL]`,
   decompile the switch — note CTL_CODE decode (device type/function/method)
   to reconstruct the protocol header comments.
3. Look for the classic bugs: unchecked `InputBufferLength`, `METHOD_NEITHER`
   without probe, double fetches, user pointers cached across waits, missing
   cancel routines on pending IRPs.
4. Fuzz surface: subclass the client, replay with mutated
   lengths/buffers; monitor with Driver Verifier + KDNET (see `windbg-ttd`).

## Pair with

`kernel-dev` (driver skeleton), `kernel-callbacks` (event sources feeding the
inverted-call channel), `byovd` (when the channel belongs to a vulnerable
third-party driver), `windows-driver-0day` (bug classes), `windbg-ttd`
(runtime verification).
