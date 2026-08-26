# XHCI endpoint discovery for Plouton

Intel XHCI only. Identify the device’s Root Port first (System Information Viewer USB tree).

## Walk

1. PCI `00:14.0` config +0x10 → MBAR (clear low byte).
2. MBAR + CapLength (byte at MBAR+0) + 0x30 → DCAB.
3. DCAB is an array of 64-bit device-context pointers.
4. Each context +0x6 = Root Port. Match the port you named.
5. Endpoints start at context +0x20, 0x20 bytes each.
6. Endpoint +0x8 = transfer ring. Touch the device and watch the ring to pick IN vs OUT.

Values to record (Intel xHCI spec tables 6-9..6-11):

| Field | Where |
|---|---|
| Type | +0x4, bits 5:3 (`>> 3`) |
| Max packet size | +0x6, 16-bit |
| Average TRB length | +0x10, 16-bit |
| Packet magic | first bytes on the ring (Logitech often `0x409301` at +0x8) |

Same walk is implemented in `hardware/xhci.c` `getEndpointRing`. Drop log level to print it from SMM instead of RWEverything.

## New mouse

Two functions:

```
InitMouseDriver(MouseDriverTdCheck) -> mouseProfile_t
MouseDriverTdCheck(EFI_PHYSICAL_ADDRESS) -> BOOLEAN
```

Register both in `InitMouseDriversFuns` (`hardware/mouse/mouseDriver.h`). Add the `.c` to `Plouton.inf`. Guard with `#ifdef` in `general/config.h`.

## New audio

One function:

```
InitAudioDriverFun() -> audioProfile_t  // ok, channels, ring PA
```

Register in `InitAudioDriverFuns` (`hardware/audio/audioDriver.h`). Same INF / `#ifdef` rules.

Stock: Logitech G Pro / Superlight mice; Corsair Wireless, Creative Pebble V3, HyperX Cloud 2, Logitech G Pro X / X 2 audio.
