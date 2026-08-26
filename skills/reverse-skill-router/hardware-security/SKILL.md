---
name: router-reverse-skill-router-hardware-security
description: Use for authorized hardware and embedded interface security research including UART/JTAG discovery, debug pad triage, secure boot overview, and offline firmware extraction support.
---

# Hardware / Embedded Interface Security

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: confirm **physical access authorization** and device ownership
2. `NOW`: ESD/power safety; read-only probing by default
3. `NEXT`: combine with firmware-pentest for image analysis
4. `ACT`: enclosure and debug interface identification → consoles → extraction

## Applicable Scenarios

- UART / JTAG / SWD debug port discovery
- Boot logs, root shell, boot interruption
- Flash extraction alongside teardown
- Secure boot/encrypted Flash feasibility assessment (non-destructive first)

## Workflow

```text
□ Disassemble the authorized device; photograph and label test points
□ Find GND/VCC/TX/RX with a multimeter; logic levels 1.8/3.3/5V
□ USB-TTL read-only logging; record the baud rate
□ JTAG: enumerate IDCODE; assess whether it is locked
□ Extract the image → hand off to firmware-pentest / ghidra
```

## Toolchain

| Tool | Purpose |
|------|------|
| USB-TTL / logic analyzer | UART |
| J-Link / CMSIS-DAP | Debugging |
| bus pirate / flipper (lab) | Multi-protocol |
| binwalk / flashrom | Extraction |

## References

- `references/debug-interface-triage.md`
- `../firmware-pentest/` `../ot-ics/`

## Routing Context

**Upstream**: MASTER R34  
**MUST NOT**: unauthorized teardown/damaging others' equipment

## Task Completion Self-Check

- [ ] Interface levels and pinout recorded?
- [ ] Image preserved with hashes?
- [ ] Checklist?
