param(
    [string]$Source = "C:\Users\Admin\.agents\skills",
    [string[]]$Targets = @(
        "C:\Users\Admin\.config\opencode\skills",
        "C:\Users\Admin\.claude\skills",
        "C:\Users\Admin\.codex\skills",
        "C:\Users\Admin\.pi\agent\skills",
        "C:\Users\Admin\.omo\agent\skills"
    ),
    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"
# Non-skill assets in the canon root that must not be mirrored as skills
$skip = @("field-journal", "ops", "references", "scripts", "LOCAL-OPERATOR.md")

function Get-TreeDigest($dir) {
    $files = Get-ChildItem $dir -Recurse -File -ErrorAction SilentlyContinue |
        Sort-Object FullName |
        ForEach-Object { (Get-FileHash $_.FullName -Algorithm SHA256).Hash }
    return ($files -join "")
}

foreach ($t in $Targets) {
    if (-not $t) { continue }
    if (-not (Test-Path $t)) { New-Item -ItemType Directory -Path $t -Force | Out-Null }

    $sourceDirs = Get-ChildItem $Source -Directory | Where-Object { $_.Name -notin $skip }
    $added = 0; $updated = 0; $removed = 0

    foreach ($d in $sourceDirs) {
        $dest = Join-Path $t $d.Name
        $srcHash = Get-TreeDigest $d.FullName

        if (-not (Test-Path $dest)) {
            if (-not $WhatIf) { Copy-Item $d.FullName $dest -Recurse -Force }
            $added++
        } else {
            $dstHash = Get-TreeDigest $dest
            if ($srcHash -ne $dstHash) {
                if (-not $WhatIf) {
                    Remove-Item $dest -Recurse -Force -ErrorAction SilentlyContinue
                    Copy-Item $d.FullName $dest -Recurse -Force
                }
                $updated++
            }
        }
    }

    foreach ($td in Get-ChildItem $t -Directory) {
        if ($td.Name -in $skip) { continue }
        if (-not (Test-Path (Join-Path $Source $td.Name))) {
            if (-not $WhatIf) { Remove-Item $td.FullName -Recurse -Force }
            $removed++
        }
    }

    Write-Output ("{0}: added={1} updated={2} removed={3}" -f $t, $added, $updated, $removed)
}
