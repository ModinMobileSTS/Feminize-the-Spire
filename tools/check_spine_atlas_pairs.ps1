$ErrorActionPreference = 'Stop'

$roots = @(
    'src/main/resources/feminizethespire/monster_overrides',
    'src/main/resources/feminizethespire/npc_overrides',
    'src/main/resources/feminizethespire/downfall_overrides',
    'src/main/resources/feminizationResources/images/characters',
    'src/main/resources/extraFeminizationResources/images/characters'
)

$issues = New-Object System.Collections.Generic.List[object]

foreach ($root in $roots) {
    if (!(Test-Path -LiteralPath $root)) {
        continue
    }

    Get-ChildItem -LiteralPath $root -Recurse -File -Filter '*.json' | ForEach-Object {
        $jsonPath = $_.FullName
        $atlasPath = [System.IO.Path]::ChangeExtension($jsonPath, '.atlas')
        if (!(Test-Path -LiteralPath $atlasPath)) {
            return
        }

        $regions = New-Object 'System.Collections.Generic.HashSet[string]'
        foreach ($line in [System.IO.File]::ReadAllLines($atlasPath)) {
            if ([string]::IsNullOrWhiteSpace($line)) {
                continue
            }
            if ($line.StartsWith(' ') -or $line.Contains(':') -or $line.Trim().EndsWith('.png')) {
                continue
            }
            [void]$regions.Add($line.Trim())
        }

        try {
            $data = [System.IO.File]::ReadAllText($jsonPath) | ConvertFrom-Json -Depth 100 -AsHashtable
        } catch {
            $issues.Add([pscustomobject]@{
                Json = $jsonPath
                Atlas = $atlasPath
                Issue = 'invalid json'
                Detail = $_.Exception.Message
            }) | Out-Null
            return
        }

        $attachments = New-Object 'System.Collections.Generic.HashSet[string]'
        if ($data.ContainsKey('skins')) {
            foreach ($skinName in $data['skins'].Keys) {
                $skin = $data['skins'][$skinName]
                if ($skin -isnot [System.Collections.IDictionary]) {
                    continue
                }
                foreach ($slotName in $skin.Keys) {
                    $slot = $skin[$slotName]
                    if ($slot -isnot [System.Collections.IDictionary]) {
                        continue
                    }
                    foreach ($attachmentName in $slot.Keys) {
                        $attachment = $slot[$attachmentName]
                        $regionName = [string]$attachmentName
                        if ($attachment -is [System.Collections.IDictionary] -and $attachment.ContainsKey('path') -and $attachment['path']) {
                            $regionName = [string]$attachment['path']
                        }
                        if ($regionName -and $regionName -ne '<none>') {
                            [void]$attachments.Add($regionName)
                        }
                    }
                }
            }
        }

        foreach ($name in $attachments) {
            if (!$regions.Contains($name)) {
                $issues.Add([pscustomobject]@{
                    Json = $jsonPath
                    Atlas = $atlasPath
                    Issue = 'missing atlas region'
                    Detail = $name
                }) | Out-Null
            }
        }
    }
}

if ($issues.Count -gt 0) {
    $issues | Sort-Object Json, Detail | Format-List
    exit 1
}

Write-Output "All Spine JSON attachments have matching atlas regions."
