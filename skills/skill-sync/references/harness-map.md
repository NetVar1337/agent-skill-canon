# Local Harness Skill Roots

Source of truth: `C:\Users\Admin\.agents\skills`.

| Harness | Scanned skill root | Sync by default | Notes |
| --- | --- | --- | --- |
| Shared / Unleash | `C:\Users\Admin\.agents\skills` | source | Author here; git repo is the source of truth |
| OpenCode | `C:\Users\Admin\.config\opencode\skills` | yes | Mirrored by `sync-skills.ps1` |
| Claude Code | `C:\Users\Admin\.claude\skills` | yes | Separate copy, not a symlink |
| Pi | `C:\Users\Admin\.pi\agent\skills` | yes | Mirrored; Pi also reads `~/.agents/skills` |
| Codex | `C:\Users\Admin\.codex\skills` | yes | Confirmed Codex scans this path |
| Hermes | `C:\Users\Admin\.hermes\skills` | on request | Partial tree; do not overwrite Hermes-native skills blindly |
| OMO | `C:\Users\Admin\.omo\agent\skills` | yes | Mirrored |
| OMP | `C:\Users\Admin\.omp\agent\skills` | on request | Large partial tree |

## Full mirror

`C:\Users\Admin\.agents\sync-skills.ps1` mirrors the whole canon (minus
non-skill assets: `field-journal`, `ops`, `references`, `scripts`,
`LOCAL-OPERATOR.md`) to all harness roots with byte-level verification.

```powershell
& C:\Users\Admin\.agents\sync-skills.ps1          # sync everything
& C:\Users\Admin\.agents\sync-skills.ps1 -WhatIf  # dry run
```

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
