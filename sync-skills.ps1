[CmdletBinding(SupportsShouldProcess)]
param(
    [string]$Source = (Join-Path $PSScriptRoot "skills"),
    [string[]]$Targets = @(
        (Join-Path $HOME ".config\opencode\skills"),
        (Join-Path $HOME ".claude\skills"),
        (Join-Path $HOME ".codex\skills"),
        (Join-Path $HOME ".pi\agent\skills"),
        (Join-Path $HOME ".omo\agent\skills")
    ),
    [switch]$Prune
)

$ErrorActionPreference = "Stop"
$skip = @("field-journal", "ops", "references", "scripts")
$rootDocs = @("tool-index.md", "LOCAL-OPERATOR.md", "RULES.md")

function Get-TreeDigest([string]$Directory) {
    $root = [IO.Path]::GetFullPath($Directory).TrimEnd('\', '/')
    $records = Get-ChildItem -LiteralPath $root -Recurse -File -Force -ErrorAction SilentlyContinue |
        Sort-Object FullName |
        ForEach-Object {
            $relative = $_.FullName.Substring($root.Length).TrimStart('\', '/').Replace('\', '/')
            $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
            "${relative}:$hash"
        }
    $bytes = [Text.Encoding]::UTF8.GetBytes(($records -join "`n"))
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return [Convert]::ToHexString($sha.ComputeHash($bytes)) }
    finally { $sha.Dispose() }
}

function Assert-SafeTarget([string]$SourcePath, [string]$TargetPath) {
    $sourceFull = [IO.Path]::GetFullPath($SourcePath).TrimEnd('\', '/')
    $targetFull = [IO.Path]::GetFullPath($TargetPath).TrimEnd('\', '/')
    if ($sourceFull -eq $targetFull) { throw "Target must not equal source: $targetFull" }
    if ([string]::IsNullOrWhiteSpace($targetFull) -or $targetFull -eq [IO.Path]::GetPathRoot($targetFull)) {
        throw "Refusing unsafe target: $targetFull"
    }
}

if (-not (Test-Path -LiteralPath $Source -PathType Container)) {
    throw "Skill source does not exist: $Source"
}
$Source = (Resolve-Path -LiteralPath $Source).Path
$sourceDirs = Get-ChildItem -LiteralPath $Source -Directory -Force |
    Where-Object { $_.Name -notin $skip -and (Get-ChildItem -LiteralPath $_.FullName -Filter SKILL.md -Recurse -File -ErrorAction SilentlyContinue | Select-Object -First 1) }

foreach ($target in $Targets) {
    if ([string]::IsNullOrWhiteSpace($target)) { continue }
    Assert-SafeTarget $Source $target

    $added = 0; $updated = 0; $removed = 0; $docs = 0
    if (-not (Test-Path -LiteralPath $target)) {
        if ($PSCmdlet.ShouldProcess($target, "Create skill root")) {
            New-Item -ItemType Directory -Path $target -Force | Out-Null
        }
    }

    foreach ($directory in $sourceDirs) {
        $destination = Join-Path $target $directory.Name
        $sourceHash = Get-TreeDigest $directory.FullName
        $destinationHash = if (Test-Path -LiteralPath $destination) { Get-TreeDigest $destination } else { $null }
        if ($sourceHash -eq $destinationHash) { continue }

        $action = if ($destinationHash) { "Replace changed skill tree" } else { "Copy skill tree" }
        if ($PSCmdlet.ShouldProcess($destination, $action)) {
            if (Test-Path -LiteralPath $destination) {
                Remove-Item -LiteralPath $destination -Recurse -Force
                $updated++
            } else { $added++ }
            Copy-Item -LiteralPath $directory.FullName -Destination $destination -Recurse -Force
            $verifiedHash = Get-TreeDigest $destination
            if ($verifiedHash -ne $sourceHash) { throw "Post-copy verification failed: $destination" }
        } elseif ($destinationHash) { $updated++ } else { $added++ }
    }

    if ($Prune -and (Test-Path -LiteralPath $target)) {
        foreach ($destinationDirectory in Get-ChildItem -LiteralPath $target -Directory -Force) {
            if ($destinationDirectory.Name -in $skip) { continue }
            if (-not (Test-Path -LiteralPath (Join-Path $Source $destinationDirectory.Name))) {
                if ($PSCmdlet.ShouldProcess($destinationDirectory.FullName, "Remove destination-only skill tree")) {
                    Remove-Item -LiteralPath $destinationDirectory.FullName -Recurse -Force
                }
                $removed++
            }
        }
    }

    foreach ($document in $rootDocs) {
        $sourceDocument = Join-Path $Source $document
        $destinationDocument = Join-Path $target $document
        if (-not (Test-Path -LiteralPath $sourceDocument -PathType Leaf)) { continue }
        $changed = -not (Test-Path -LiteralPath $destinationDocument -PathType Leaf) -or
            (Get-FileHash -LiteralPath $sourceDocument -Algorithm SHA256).Hash -ne
            (Get-FileHash -LiteralPath $destinationDocument -Algorithm SHA256).Hash
        if ($changed -and $PSCmdlet.ShouldProcess($destinationDocument, "Mirror root documentation")) {
            Copy-Item -LiteralPath $sourceDocument -Destination $destinationDocument -Force
        }
        if ($changed) { $docs++ }
    }

    [pscustomobject]@{
        Target = [IO.Path]::GetFullPath($target)
        Added = $added
        Updated = $updated
        Removed = $removed
        Documents = $docs
        Prune = [bool]$Prune
        WhatIf = [bool]$WhatIfPreference
    }
}
