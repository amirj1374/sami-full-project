<#
.SYNOPSIS
Runs non-deploying contract tests for scripts/deploy.ps1.

.DESCRIPTION
This lightweight harness uses only Windows PowerShell and Git. It verifies
syntax, help, safety gates, error handling, and DryRun behavior. Real Docker
Build/Export verification remains a separate release-gate command because it is
intentionally slower and produces images/TAR artifacts.
#>

#requires -Version 5.1
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$deployScript = Join-Path $repositoryRoot 'scripts\deploy.ps1'
$verifyScript = Join-Path $repositoryRoot 'scripts\verify-deployment.ps1'
$shell = (Get-Process -Id $PID).Path
$failures = New-Object System.Collections.Generic.List[string]
$passed = 0

function Assert-Test {
    param([string]$Name, [scriptblock]$Test)
    try {
        & $Test
        $script:passed++
        Write-Host "[PASS] $Name" -ForegroundColor Green
    }
    catch {
        $script:failures.Add("$Name`: $($_.Exception.Message)")
        Write-Host "[FAIL] $Name`: $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Invoke-DeployProcess {
    param([string[]]$Arguments)
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = @(& $shell -NoProfile -ExecutionPolicy Bypass -File $deployScript @Arguments 2>&1)
        $exitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousPreference
    }
    return [pscustomobject]@{
        ExitCode = $exitCode
        Output   = (($output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine)
    }
}

function New-TestDirectory {
    $path = Join-Path ([IO.Path]::GetTempPath()) ('sami-deploy-tests-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $path | Out-Null
    return $path
}

Assert-Test 'PowerShell syntax parses without errors' {
    foreach ($path in @($deployScript, $verifyScript)) {
        $tokens = $null
        $errors = $null
        [System.Management.Automation.Language.Parser]::ParseFile($path, [ref]$tokens, [ref]$errors) | Out-Null
        if ($errors.Count -gt 0) { throw ($errors.Message -join '; ') }
    }
}

Assert-Test 'Comment-based help is available' {
    $help = Get-Help $deployScript -Full | Out-String
    if ($help -notmatch 'Build, Validate, Export, Upload, Deploy') { throw 'Expected mode help was not found.' }
    if ($help -notmatch 'IdentityFile') { throw 'IdentityFile help was not found.' }
}

Assert-Test 'Development preflight succeeds in DryRun mode' {
    $artifactDir = New-TestDirectory
    try {
        $result = Invoke-DeployProcess @('-Mode', 'Build', '-DryRun', '-AllowDirtyWorkingTree', '-ArtifactDirectory', $artifactDir)
        if ($result.ExitCode -ne 0) { throw $result.Output }
        if ($result.Output -notmatch 'Repository gate passed') { throw 'Repository gate output is missing.' }
    }
    finally { Remove-Item -LiteralPath $artifactDir -Recurse -Force -ErrorAction SilentlyContinue }
}

Assert-Test 'Wrong branch is rejected before any build' {
    $tempRepo = New-TestDirectory
    $artifactDir = New-TestDirectory
    try {
        & git -C $tempRepo init -q
        & git -C $tempRepo config user.email 'deployment-tests@sami.local'
        & git -C $tempRepo config user.name 'SAMI Deployment Tests'
        [IO.File]::WriteAllText((Join-Path $tempRepo 'probe.txt'), 'probe')
        & git -C $tempRepo add probe.txt
        & git -C $tempRepo commit -q -m 'test fixture'
        & git -C $tempRepo branch -M not-development
        $result = Invoke-DeployProcess @('-Mode', 'Build', '-DryRun', '-RepositoryRoot', $tempRepo, '-ArtifactDirectory', $artifactDir)
        if ($result.ExitCode -eq 0) { throw 'Wrong branch unexpectedly succeeded.' }
        if ($result.Output -notmatch "requires the exact 'development' branch") { throw $result.Output }
    }
    finally {
        Remove-Item -LiteralPath $tempRepo -Recurse -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $artifactDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Assert-Test 'Dirty worktree is rejected by default' {
    $probe = Join-Path $repositoryRoot '.deployment-automation-dirty-probe'
    $artifactDir = New-TestDirectory
    try {
        [IO.File]::WriteAllText($probe, 'temporary test probe')
        $result = Invoke-DeployProcess @('-Mode', 'Build', '-DryRun', '-ArtifactDirectory', $artifactDir)
        if ($result.ExitCode -eq 0) { throw 'Dirty worktree unexpectedly succeeded.' }
        if ($result.Output -notmatch 'working tree is dirty') { throw $result.Output }
    }
    finally {
        Remove-Item -LiteralPath $probe -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $artifactDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Assert-Test 'Unavailable Docker command fails clearly' {
    $artifactDir = New-TestDirectory
    try {
        $result = Invoke-DeployProcess @('-Mode', 'Build', '-AllowDirtyWorkingTree', '-DockerCommand', 'definitely-missing-docker-command', '-ArtifactDirectory', $artifactDir)
        if ($result.ExitCode -eq 0) { throw 'Missing Docker command unexpectedly succeeded.' }
        if ($result.Output -notmatch 'Docker executable') { throw $result.Output }
    }
    finally { Remove-Item -LiteralPath $artifactDir -Recurse -Force -ErrorAction SilentlyContinue }
}

Assert-Test 'Missing export artifacts block Upload before SSH' {
    $artifactDir = New-TestDirectory
    try {
        $result = Invoke-DeployProcess @('-Mode', 'Upload', '-AllowDirtyWorkingTree', '-ArtifactDirectory', $artifactDir)
        if ($result.ExitCode -eq 0) { throw 'Upload without artifacts unexpectedly succeeded.' }
        if ($result.Output -notmatch 'Release manifest is missing') { throw $result.Output }
    }
    finally { Remove-Item -LiteralPath $artifactDir -Recurse -Force -ErrorAction SilentlyContinue }
}

Assert-Test 'Invalid Mode is rejected by parameter validation' {
    $result = Invoke-DeployProcess @('-Mode', 'InvalidMode')
    if ($result.ExitCode -eq 0) { throw 'Invalid mode unexpectedly succeeded.' }
}

Assert-Test 'Validate DryRun plans the disposable local release gate without SSH' {
    $artifactDir = New-TestDirectory
    try {
        $result = Invoke-DeployProcess @('-Mode', 'Validate', '-DryRun', '-AllowDirtyWorkingTree', '-ArtifactDirectory', $artifactDir)
        if ($result.ExitCode -ne 0) { throw $result.Output }
        foreach ($expected in @('backend clean verify', 'disposable PostgreSQL/Compose stack', 'temporary containers, network, and volumes')) {
            if ($result.Output -notmatch [regex]::Escape($expected)) { throw "Missing Validate DryRun plan text: $expected" }
        }
        if ($result.Output -match 'SSH connectivity passed') { throw 'Validate DryRun connected to SSH unexpectedly.' }
    }
    finally { Remove-Item -LiteralPath $artifactDir -Recurse -Force -ErrorAction SilentlyContinue }
}

Assert-Test 'Full DryRun plans every phase without remote connectivity' {
    $artifactDir = New-TestDirectory
    try {
        $result = Invoke-DeployProcess @('-Mode', 'Full', '-DryRun', '-AllowDirtyWorkingTree', '-ArtifactDirectory', $artifactDir)
        if ($result.ExitCode -ne 0) { throw $result.Output }
        foreach ($expected in @('backend clean verify', 'Would build', 'atomically export', 'atomically rename', 'guarded deployment')) {
            if ($result.Output -notmatch [regex]::Escape($expected)) { throw "Missing DryRun plan text: $expected" }
        }
        if ($result.Output -match 'SSH connectivity passed') { throw 'DryRun connected to SSH unexpectedly.' }
    }
    finally { Remove-Item -LiteralPath $artifactDir -Recurse -Force -ErrorAction SilentlyContinue }
}

Assert-Test 'Cleanup is preview-only without ConfirmCleanup' {
    $artifactDir = New-TestDirectory
    try {
        $result = Invoke-DeployProcess @('-Mode', 'Cleanup', '-DryRun', '-AllowDirtyWorkingTree', '-ArtifactDirectory', $artifactDir)
        if ($result.ExitCode -ne 0) { throw $result.Output }
        if ($result.Output -notmatch 'Cleanup candidates') { throw 'Cleanup preview was not shown.' }
    }
    finally { Remove-Item -LiteralPath $artifactDir -Recurse -Force -ErrorAction SilentlyContinue }
}

Write-Host ''
Write-Host ("Deployment automation tests: {0} passed, {1} failed." -f $passed, $failures.Count)
if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    exit 1
}
