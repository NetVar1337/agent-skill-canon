# Local Harness Skill Roots

Source of truth: `C:\Users\Admin\.agents\skills`.

| Harness | Scanned skill root | Sync by default | Notes |
| --- | --- | --- | --- |
| Shared / Unleash | `C:\Users\Admin\.agents\skills` | source | Author here |
| Claude Code | `C:\Users\Admin\.claude\skills` | yes | Separate copy, not a symlink |
| Pi | `C:\Users\Admin\.pi\agent\skills` | only if used | Currently empty; Pi may also read `~/.agents/skills` |
| Codex | `C:\Users\Admin\.codex\skills` | only if used | Confirm Codex still scans this path before mirroring |
| Hermes | `C:\Users\Admin\.hermes\skills` | on request | Partial tree; do not overwrite Hermes-native skills blindly |
| OMO | `C:\Users\Admin\.omo\agent\skills` | on request | Partial tree |
| OMP | `C:\Users\Admin\.omp\agent\skills` | on request | Large partial tree |

## Copy Recipe

PowerShell, one skill:

```powershell
$Name = 'prompt-forge'
$Src  = Join-Path $env:USERPROFILE ".agents\skills\$Name"
$Dst  = Join-Path $env:USERPROFILE ".claude\skills\$Name"
New-Item -ItemType Directory -Force -Path $Dst | Out-Null
Copy-Item -Path (Join-Path $Src '*') -Destination $Dst -Recurse -Force
```

Verify:

```powershell
Get-FileHash (Join-Path $Src 'SKILL.md'), (Join-Path $Dst 'SKILL.md')
```

## Do Not Sync Into

- `~\.agents\skills-archive\`
- Vendor checkouts under `~\.agents\vendor\`
- Project-local `skills/` unless the operator named that repo
