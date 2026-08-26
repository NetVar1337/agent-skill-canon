---
name: router-reverse-skill-router-browser-extension-reverse
description: Use for authorized reverse engineering of browser extensions (Chrome/Firefox) including manifest analysis, background workers, and extension-based credential or traffic logic recovery.
---

# Browser Extension Reverse Engineering

## ACTION REQUIRED (execute immediately after reading)

1. `NOW`: read `../field-journal/precedent-reverse.md`
2. `NOW`: confirm the target is a **browser extension** (crx/xpi/unpacked directory), not ordinary webpage JS (for that → `js-reverse/`)
3. `NEXT`: unpack the extension; read the manifest
4. `ACT`: permission surface → background scripts → network/storage hooks

## Applicable Scenarios

- Chrome/Edge MV2/MV3 extension analysis
- Firefox extensions
- Malicious extension IOCs, supply-chain extension poisoning investigations
- Recovery of signing/encryption/proxy logic implemented by an extension

## Workflow

### 1. Bundle

```text
□ Unpack the crx / take the extension directory from the profile
□ manifest.json: permissions, host_permissions, background, content_scripts
□ Assess excessive permissions (<all_urls>, webRequest, debugger)
```

### 2. Logic

```text
□ service_worker / background entry points
□ content_script injection points and worlds (isolated)
□ chrome.storage / IndexedDB keys
□ Same as `js-reverse`: Observe network and message passing (runtime.sendMessage)
```

### 3. Dynamic

```text
□ Load the unpacked directory in developer mode
□ Check errors in chrome://extensions
□ Attach DevTools to the service worker
□ Frida/browser CDP when necessary (jshookmcp)
```

## Toolchain

| Tool | Purpose |
|------|------|
| unzip/jq | manifest |
| Chrome DevTools | worker debugging |
| js-reverse toolchain | Deep JS |
| YARA | Malicious extension rules |

## References

- `references/extension-analysis.md`
- field-journal extension-recovery related entries
- `../js-reverse/` `../malware-analysis/`

## Routing Context

**Upstream**: MASTER R30  
**Downstream**: heavily obfuscated JS → `js-reverse`; poisoning investigation → supply-chain / malware

## Task Completion Self-Check

- [ ] Permission surface and entry scripts listed?
- [ ] Key data flows recovered?
- [ ] Checklist?
