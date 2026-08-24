---
name: vps-ssh-control
description: Connect to and administer the user's VPS at root@153.92.1.5 over SSH. Use for running commands, inspecting services, managing files, or troubleshooting this specific Linux server.
compatibility: Requires OpenSSH access to root@153.92.1.5 from this workstation.
---

# VPS SSH Control

Use this skill only for the user's VPS:

```text
Host: 153.92.1.5
User: root
Port: 22
```

Authentication is already configured on this workstation. Do not request, print, store, or copy passwords, private keys, tokens, or SSH configuration contents.

## Connect and Verify

For non-interactive work, run commands through SSH with a bounded connection timeout and non-interactive authentication:

```bash
ssh -o BatchMode=yes -o ConnectTimeout=10 root@153.92.1.5 '<command>'
```

At the start of a task that needs server access, verify the target and privileges with:

```bash
ssh -o BatchMode=yes -o ConnectTimeout=10 root@153.92.1.5 'hostname; id; uptime -p'
```

Expected identity is `uid=0(root)`. Report connection, authentication, or host-key failures exactly; do not claim remote control without a successful command.

## Host-Key Handling

The host key has been accepted on this workstation. Do not weaken host-key verification with `StrictHostKeyChecking=no` and do not delete or overwrite `known_hosts` entries. If SSH reports a host-key mismatch, stop and report it for deliberate verification or rotation.

## Operating Procedure

1. Inspect the relevant state before changing it: service status, configuration, disk space, logs, or file contents as appropriate.
2. Use a single quoted remote command for focused actions. For multi-step work, send a quoted `bash -lc` command only when shell features are needed.
3. Use absolute paths for system files and quote remote arguments safely. Avoid interpolating untrusted values into remote shell commands.
4. Verify every change with a relevant read-only check (for example `systemctl is-active`, `ss -ltnp`, a file hash, or a targeted command output).
5. State the exact remote command outcome and any remaining issue.

Examples:

```bash
# Inspect a service
ssh -o BatchMode=yes -o ConnectTimeout=10 root@153.92.1.5 'systemctl status nginx --no-pager'

# Check disk usage
ssh -o BatchMode=yes -o ConnectTimeout=10 root@153.92.1.5 'df -hT'

# Upload a local artifact, then verify it remotely
scp ./artifact.tar.gz root@153.92.1.5:/root/artifact.tar.gz
ssh -o BatchMode=yes -o ConnectTimeout=10 root@153.92.1.5 'sha256sum /root/artifact.tar.gz'
```

## File Transfers

Use `scp` for focused copies and `rsync -e ssh` for repeatable directory synchronization. Confirm the intended remote path before uploads or deletions. For important artifacts, compare SHA-256 hashes on both endpoints.

```bash
scp ./local-file root@153.92.1.5:/root/remote-file
scp root@153.92.1.5:/root/remote-file ./local-file
rsync -a --delete -e ssh ./local-dir/ root@153.92.1.5:/root/remote-dir/
```

`rsync --delete`, recursive deletion, partitioning, firewall lockouts, package removals, reboots, shutdowns, and overwriting configuration are impactful. Perform them only when the user's request explicitly calls for that action; otherwise inspect and propose the command first.

## Diagnostics

```bash
# See the effective SSH options without connecting.
ssh -G root@153.92.1.5

# Check SSH reachability.
ssh -o BatchMode=yes -o ConnectTimeout=10 root@153.92.1.5 'true'

# Inspect failed systemd units.
ssh -o BatchMode=yes -o ConnectTimeout=10 root@153.92.1.5 'systemctl --failed --no-pager'
```

Do not change SSH, firewall, or network configuration until the current access path and a recovery method have been confirmed.
