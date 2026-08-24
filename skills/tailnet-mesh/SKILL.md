---
name: tailnet-mesh
description: Use when connecting over SSH or RDP to any node on the NetVar1337 tailnet (laptop, desktop, work, vps, macbook, phone), or when diagnosing that mesh.
version: 1.0.0
author: NetVar1337
license: Private
metadata:
  hermes:
    tags: [tailscale, ssh, rdp, scp, mesh]
    related_skills: [desktop-ei069kk-access]
---

# Tailnet mesh

Bi-directional SSH/RDP across `tailc76cfe.ts.net`. Kit lives at `C:\Users\Admin\tailnet-mesh`. Shared identity is `~/.ssh/tailnet_ed25519`. Do not print private keys or passwords.

## Nodes

| Alias   | Tailscale IP    | User  | Inbound                         |
|---------|-----------------|-------|---------------------------------|
| laptop  | 100.90.69.122   | Admin | SSH + RDP (this machine is laptop / DESKTOP-EI069KK) |
| desktop | 100.126.147.3   | Admin | SSH + RDP (C3PO)                |
| work    | 100.89.155.58   | Admin | SSH + RDP after bootstrap (TFK-CZC448377C, DERP-only) |
| vps     | 100.118.168.81  | root  | SSH only                        |
| macbook | 100.110.152.82  | set   | SSH + VNC after bootstrap       |
| phone   | 100.113.231.120 | —     | client only                     |

`hostname` on this Windows box is `DESKTOP-EI069KK`; Tailscale name is `laptop`.

## Client commands

```powershell
ssh laptop
ssh desktop
ssh work
ssh vps
ssh macbook
rdp laptop
rdp desktop
rdp work
mesh-put desktop .\artifact.zip
mesh-get desktop Downloads/artifact.zip .
mesh-status
```

Default remote Windows shell is `cmd.exe`. Use `dir`/`cls`, or enter `powershell`.

## Install / repair

Windows node (elevated, idempotent):

```powershell
powershell -ExecutionPolicy Bypass -File C:\Users\Admin\tailnet-mesh\install-windows.ps1
```

Client aliases only:

```powershell
powershell -ExecutionPolicy Bypass -File C:\Users\Admin\tailnet-mesh\install-windows.ps1 -ClientOnly
```

VPS (from laptop, once `ssh vps` works):

```powershell
powershell -ExecutionPolicy Bypass -File C:\Users\Admin\tailnet-mesh\deploy-vps.ps1
```

## Diagnostics

```powershell
tailscale status
ssh -G desktop | Select-String '^(user|hostname|port|identityfile) '
ssh -o BatchMode=yes -o ConnectTimeout=8 desktop "whoami && hostname"
ssh -o BatchMode=yes -o ConnectTimeout=8 vps "whoami && hostname"
C:\Users\Admin\tailnet-mesh\mesh-status.ps1
```

On this machine, inbound services:

```powershell
Get-Service sshd, TermService
```

Both must be `Running` before claiming laptop is reachable.

## Constraints

1. Tailscale SSH is not supported on Windows. Use native OpenSSH (`sshd`) and RDP.
2. `work` may have no inbound ports until `install-windows.ps1` is run there. GPO can block RDP/SSH.
3. `macbook` has no native RDP; use SSH or VNC :5900.
4. `phone` cannot host SSH/RDP in this mesh.
5. Never set `StrictHostKeyChecking=no`. Host keys use `accept-new`.
6. The older `work` alias in `desktop-ei069kk-access` targeted this laptop from C3PO. The mesh alias `work` is the TFK machine at `100.89.155.58`. Use `ssh laptop` / `rdp laptop` for DESKTOP-EI069KK.
