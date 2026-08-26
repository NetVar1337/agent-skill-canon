---
name: thick-client
description: "Use for authorized security testing of desktop thick clients including local storage, update channels, IPC, traffic, and client-side trust boundaries."
version: 1.0.0
license: MIT
metadata:
  package: unleash-skills
  author: NetVar1337/unleash
  category: reverse-skill
  upstream: https://github.com/zhaoxuya520/reverse-skill
---

> Bundled with Unleash skills pack. Upstream: https://github.com/zhaoxuya520/reverse-skill

# Thick Client Security Testing

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read `../field-journal/precedent-pentest.md`
2. `NOW`: confirm the target is a **desktop thick client** (Win/macOS/Linux GUI or service companion), not pure web
3. `NOW`: case-init; write the installer source and test accounts into scope
4. `NEXT`: tools (Burp upstream proxy, process monitoring, reverse engineering tools)
5. `ACT`: trust-boundary map → local surface → network surface → update/supply chain

## Applicable Scenarios

- C/S architecture clients, Electron/Qt/.NET WinForms/WPF
- Local config/credential storage, IPC, named pipes
- Client-side enforced-validation bypass research (authorized)
- Auto-update channels and code-signature verification

## Workflow

### 1. Build Boundaries

```text
□ Process tree, child processes, drivers/services
□ Listening ports and outbound domains
□ Local sensitive paths: %APPDATA%, Keychain, registry
```

### 2. Local Attack Surface

```text
□ Cleartext config, hardcoded keys, debug switches
□ DLL hijacking/search order (Windows)
□ Database file (SQLite) permissions and encryption
□ IPC: who can connect? Is there authentication?
```

### 3. Network Surface

```text
□ System proxy / app-custom TLS
□ Certificate pinning → combine with mobile/js methodology or Frida
□ API privilege escalation: admin endpoints hidden in the client
```

### 4. Reverse-Engineering Verification

```text
□ .NET → dotnet-reverse; native → ida/ghidra; Electron → asar + js-reverse
```

## Toolchain

| Tool | Purpose |
|------|------|
| Process Monitor / API Monitor | Behavior |
| Burp / mitmproxy | Traffic |
| dnSpy / IDA / Ghidra | Reverse engineering |
| Sysinternals | Windows surface |
| asar / nexe detection | Electron |

## References

- `references/thick-client-checklist.md`
- `../dotnet-reverse/` `../ida-reverse/` `../js-reverse/` `../api-security/`

## Routing Context

**Upstream**: MASTER R32  
**Downstream**: pure protocols `protocol-reverse`; supply-chain updates `supply-chain-security`

## Task Completion Self-Check

- [ ] Trust boundaries drawn?
- [ ] Both local + network surfaces covered?
- [ ] Checklist?
