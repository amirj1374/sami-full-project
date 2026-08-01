<#
.SYNOPSIS
Builds, exports, uploads, deploys, rolls back, or cleans SAMI release artifacts.

.DESCRIPTION
The default Build mode is local-only. Full mode builds reproducible linux/amd64
images from a clean development branch, exports and verifies them, uploads them
under temporary names, deploys backend then frontend, and automatically rolls
back application images if health verification fails. PostgreSQL and named
volumes are never removed or recreated by this script.

SSH is non-interactive and key-only by default. OpenSSH uses ssh-agent and the
standard ~/.ssh key locations automatically. Use -IdentityFile to select a key.
Use -AllowInteractiveAuth only for an explicitly supervised setup/emergency run;
the script never accepts or passes a password.

.PARAMETER Mode
Build, Export, Upload, Deploy, Full, Rollback, or Cleanup. Default: Build.

.PARAMETER ConfigFile
Optional local PowerShell file returning a hashtable of non-secret settings.

.PARAMETER IdentityFile
Optional OpenSSH private-key path. The key is referenced, never read or logged.

.PARAMETER AllowInteractiveAuth
Allows OpenSSH to prompt interactively. Normal deployment omits this switch.

.PARAMETER DryRun
Shows planned work without building, exporting, uploading, loading, recreating,
cleaning, or connecting to the VPS.

.EXAMPLE
.\scripts\deploy.ps1 -Mode Build

.EXAMPLE
.\scripts\deploy.ps1 -Mode Full -IdentityFile "$HOME\.ssh\sami_vps_ed25519"

.EXAMPLE
.\scripts\deploy.ps1 -Mode Full -DryRun -IdentityFile "$HOME\.ssh\sami_vps_ed25519"
#>

#requires -Version 5.1
[CmdletBinding()]
param(
    [ValidateSet('Build', 'Export', 'Upload', 'Deploy', 'Full', 'Rollback', 'Cleanup')]
    [string]$Mode = 'Build',
    [string]$ConfigFile,
    [string]$SshHost = '87.248.131.157',
    [ValidateRange(1, 65535)][int]$SshPort = 9011,
    [string]$SshUser = 'root',
    [string]$IdentityFile,
    [switch]$AllowInteractiveAuth,
    [string]$RepositoryRoot,
    [string]$ArtifactDirectory,
    [string]$RemoteArtifactDirectory = '/root',
    [string]$RemoteComposeDirectory = '/root/sami-full-project/sami-backend',
    [string]$RemoteHistoryDirectory = '/root/sami-deployment-history',
    [string]$ComposeFile = 'docker-compose.prod.yml',
    [string]$EnvironmentFile = '.env',
    [string]$BackendImageTag = 'sami-backend:test',
    [string]$FrontendImageTag = 'sami-frontend:test',
    [string]$FrontendApiBaseUrl = '/api',
    [string]$ApplicationVersion = '0.1.0',
    [string]$ApplicationUrl,
    [switch]$AllowDirtyWorkingTree,
    [switch]$NoCache,
    [switch]$SkipUpload,
    [switch]$DryRun,
    [switch]$ConfirmCleanup,
    [ValidateRange(1, 3650)][int]$RetentionDays = 30,
    [ValidateRange(30, 1800)][int]$HealthTimeoutSeconds = 300,
    [string]$DockerCommand = 'docker',
    [string]$GitCommand = 'git',
    [string]$SshCommand = 'ssh',
    [string]$ScpCommand = 'scp'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$script:ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$script:InitialBoundParameterNames = @($PSBoundParameters.Keys)
$script:StartedAt = Get-Date
$script:LogFile = $null
$script:Branch = $null
$script:CommitSha = $null
$script:ShortCommitSha = $null
$script:BuildTimestamp = $null
$script:BackendImage = $null
$script:FrontendImage = $null
$script:Manifest = $null
$script:DeploymentTimestamp = $null

function Protect-LogText {
    param([AllowNull()][string]$Text)
    if ($null -eq $Text) { return '' }
    $protected = $Text
    $protected = $protected -replace '(?i)((?:password|passwd|secret|token|authorization|cookie)\s*[:=]\s*)[^\s,;]+', '$1[REDACTED]'
    $protected = $protected -replace '(?i)(Bearer\s+)[A-Za-z0-9._~+/-]+=*', '$1[REDACTED]'
    return $protected
}

function Write-RunLog {
    param(
        [ValidateSet('INFO', 'SUCCESS', 'WARN', 'ERROR', 'PLAN')][string]$Level,
        [string]$Message
    )
    $safeMessage = Protect-LogText $Message
    $line = '[{0}] [{1}] {2}' -f (Get-Date).ToString('s'), $Level, $safeMessage
    $color = switch ($Level) {
        'SUCCESS' { 'Green' }
        'WARN' { 'Yellow' }
        'ERROR' { 'Red' }
        'PLAN' { 'Cyan' }
        default { 'Gray' }
    }
    Write-Host $line -ForegroundColor $color
    if ($script:LogFile) {
        Add-Content -LiteralPath $script:LogFile -Value $line -Encoding UTF8
    }
}

function Resolve-Executable {
    param([string]$Name, [string]$Purpose)
    try {
        $command = Get-Command -Name $Name -CommandType Application -ErrorAction Stop | Select-Object -First 1
        return $command.Source
    }
    catch {
        throw "$Purpose executable '$Name' is unavailable. Install it or provide its command path."
    }
}

function Invoke-CapturedNative {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$Description
    )
    $output = @(& $FilePath @Arguments 2>&1)
    $exitCode = $LASTEXITCODE
    if ($exitCode -ne 0) {
        $safeOutput = Protect-LogText (($output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine)
        if ($safeOutput.Length -gt 1200) { $safeOutput = $safeOutput.Substring(0, 1200) + '...' }
        throw "$Description failed with exit code $exitCode. $safeOutput"
    }
    return (($output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine).Trim()
}

function Invoke-Native {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$Description
    )
    Write-RunLog INFO $Description
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Description failed with exit code $LASTEXITCODE."
    }
}

function Assert-SafeRemotePath {
    param([string]$Value, [string]$Name)
    if ($Value -notmatch '^/[A-Za-z0-9._/-]+$' -or $Value.Contains('..')) {
        throw "$Name must be an absolute Linux path containing only letters, digits, '.', '_', '-', and '/'."
    }
}

function Assert-SafeRelativePath {
    param([string]$Value, [string]$Name)
    if ($Value -notmatch '^[A-Za-z0-9._/-]+$' -or $Value.StartsWith('/') -or $Value.Contains('..')) {
        throw "$Name must be a safe relative Linux path without '..'."
    }
}

function Assert-ImageTag {
    param([string]$Value, [string]$Name)
    if ($Value -notmatch '^[a-z0-9][a-z0-9._/-]*:[A-Za-z0-9][A-Za-z0-9._-]*$') {
        throw "$Name '$Value' is not a supported explicit Docker image tag."
    }
}

function Get-ObjectPropertyValue {
    param([object]$Object, [string]$Name)
    if ($null -eq $Object) { return $null }
    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) { return $null }
    return $property.Value
}

function Initialize-Configuration {
    $boundNames = $script:InitialBoundParameterNames
    if ($ConfigFile) {
        $resolvedConfig = (Resolve-Path -LiteralPath $ConfigFile -ErrorAction Stop).Path
        $config = & $resolvedConfig
        if ($config -isnot [hashtable]) {
            throw "ConfigFile must return one hashtable."
        }
        $allowedKeys = @(
            'SshHost', 'SshPort', 'SshUser', 'IdentityFile',
            'RemoteArtifactDirectory', 'RemoteComposeDirectory', 'RemoteHistoryDirectory',
            'ComposeFile', 'EnvironmentFile', 'BackendImageTag', 'FrontendImageTag',
            'FrontendApiBaseUrl', 'ApplicationVersion', 'ApplicationUrl'
        )
        foreach ($key in $config.Keys) {
            if ($allowedKeys -notcontains $key) {
                throw "Unsupported configuration key '$key'."
            }
            if ($boundNames -notcontains $key) {
                Set-Variable -Name $key -Value $config[$key] -Scope Script
            }
        }
    }

    if (-not $RepositoryRoot) {
        $script:RepositoryRoot = (Resolve-Path -LiteralPath (Join-Path $script:ScriptRoot '..') -ErrorAction Stop).Path
    }
    else {
        $script:RepositoryRoot = (Resolve-Path -LiteralPath $RepositoryRoot -ErrorAction Stop).Path
    }
    if (-not $ArtifactDirectory) {
        $script:ArtifactDirectory = Join-Path $script:RepositoryRoot 'deployment-artifacts'
    }
    else {
        $script:ArtifactDirectory = [IO.Path]::GetFullPath($ArtifactDirectory)
    }
    if (-not $ApplicationUrl) {
        $script:ApplicationUrl = "http://$SshHost"
    }

    if ($SshHost -notmatch '^[A-Za-z0-9.-]+$') { throw 'SshHost contains unsupported characters.' }
    if ($SshUser -notmatch '^[A-Za-z_][A-Za-z0-9_-]*$') { throw 'SshUser contains unsupported characters.' }
    Assert-SafeRemotePath $RemoteArtifactDirectory 'RemoteArtifactDirectory'
    Assert-SafeRemotePath $RemoteComposeDirectory 'RemoteComposeDirectory'
    Assert-SafeRemotePath $RemoteHistoryDirectory 'RemoteHistoryDirectory'
    Assert-SafeRelativePath $ComposeFile 'ComposeFile'
    Assert-SafeRelativePath $EnvironmentFile 'EnvironmentFile'
    Assert-ImageTag $BackendImageTag 'BackendImageTag'
    Assert-ImageTag $FrontendImageTag 'FrontendImageTag'
    if ($FrontendApiBaseUrl -notmatch '^/[A-Za-z0-9._/-]*$') { throw 'FrontendApiBaseUrl must be a same-origin absolute path.' }
    if ($ApplicationVersion -notmatch '^[A-Za-z0-9._+-]+$') { throw 'ApplicationVersion contains unsupported characters.' }
    if ($script:ApplicationUrl -notmatch '^https?://') { throw 'ApplicationUrl must use http or https.' }

    if ($IdentityFile) {
        $script:IdentityFile = (Resolve-Path -LiteralPath $IdentityFile -ErrorAction Stop).Path
        if ((Get-Item -LiteralPath $script:IdentityFile).PSIsContainer) {
            throw 'IdentityFile must identify an OpenSSH private-key file, not a directory.'
        }
    }

    New-Item -ItemType Directory -Path $script:ArtifactDirectory -Force | Out-Null
    $logDirectory = Join-Path $script:ArtifactDirectory 'logs'
    New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
    $script:LogFile = Join-Path $logDirectory ('deployment-{0}-{1}.log' -f (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ'), $Mode.ToLowerInvariant())
}

function Initialize-RepositoryState {
    $script:GitExecutable = Resolve-Executable $GitCommand 'Git'
    $topLevel = Invoke-CapturedNative $script:GitExecutable @('-C', $script:RepositoryRoot, 'rev-parse', '--show-toplevel') 'Git repository check'
    $expected = [IO.Path]::GetFullPath($script:RepositoryRoot).TrimEnd('\', '/')
    $actual = [IO.Path]::GetFullPath($topLevel).TrimEnd('\', '/')
    if (-not $expected.Equals($actual, [StringComparison]::OrdinalIgnoreCase)) {
        throw "RepositoryRoot '$expected' is not the Git worktree root '$actual'."
    }

    $script:Branch = Invoke-CapturedNative $script:GitExecutable @('-C', $script:RepositoryRoot, 'rev-parse', '--abbrev-ref', 'HEAD') 'Git branch check'
    if ($script:Branch -ne 'development') {
        throw "Deployment automation requires the exact 'development' branch; current branch is '$($script:Branch)'."
    }
    $script:CommitSha = Invoke-CapturedNative $script:GitExecutable @('-C', $script:RepositoryRoot, 'rev-parse', 'HEAD') 'Git revision check'
    $script:ShortCommitSha = Invoke-CapturedNative $script:GitExecutable @('-C', $script:RepositoryRoot, 'rev-parse', '--short=12', 'HEAD') 'Git short revision check'
    $workingTree = Invoke-CapturedNative $script:GitExecutable @('-C', $script:RepositoryRoot, 'status', '--porcelain', '--untracked-files=all') 'Git working-tree check'
    if ($workingTree -and -not $AllowDirtyWorkingTree) {
        throw 'The Git working tree is dirty. Commit/stash the changes or explicitly use -AllowDirtyWorkingTree for a reviewed exception.'
    }
    if ($workingTree) {
        Write-RunLog WARN 'Proceeding with a dirty working tree because -AllowDirtyWorkingTree was supplied.'
    }
    Write-RunLog SUCCESS "Repository gate passed: branch=$($script:Branch), commit=$($script:CommitSha)."
}

function Initialize-Docker {
    $script:DockerExecutable = Resolve-Executable $DockerCommand 'Docker'
    $serverPlatform = Invoke-CapturedNative $script:DockerExecutable @('version', '--format', '{{.Server.Os}}/{{.Server.Arch}}') 'Docker daemon check'
    if ($serverPlatform -ne 'linux/amd64') {
        throw "Docker daemon platform must be linux/amd64; detected '$serverPlatform'."
    }
    $null = Invoke-CapturedNative $script:DockerExecutable @('buildx', 'version') 'Docker buildx check'
    Write-RunLog SUCCESS "Docker/buildx gate passed: $serverPlatform."
}

function Get-SshArguments {
    param([switch]$ForScp)
    $arguments = New-Object System.Collections.Generic.List[string]
    if ($ForScp) { $arguments.Add('-P') } else { $arguments.Add('-p') }
    $arguments.Add($SshPort.ToString())
    $arguments.Add('-o'); $arguments.Add('ConnectTimeout=15')
    $arguments.Add('-o'); $arguments.Add('ServerAliveInterval=15')
    $arguments.Add('-o'); $arguments.Add('StrictHostKeyChecking=accept-new')
    if (-not $AllowInteractiveAuth) {
        $arguments.Add('-o'); $arguments.Add('BatchMode=yes')
        $arguments.Add('-o'); $arguments.Add('PasswordAuthentication=no')
        $arguments.Add('-o'); $arguments.Add('KbdInteractiveAuthentication=no')
        $arguments.Add('-o'); $arguments.Add('PreferredAuthentications=publickey')
    }
    else {
        $arguments.Add('-o'); $arguments.Add('BatchMode=no')
    }
    if ($IdentityFile) {
        $arguments.Add('-i'); $arguments.Add($script:IdentityFile)
    }
    return $arguments.ToArray()
}

function Initialize-SshTools {
    $script:SshExecutable = Resolve-Executable $SshCommand 'OpenSSH ssh'
    if ($Mode -eq 'Upload' -or ($Mode -eq 'Full' -and -not $SkipUpload)) {
        $script:ScpExecutable = Resolve-Executable $ScpCommand 'OpenSSH scp'
    }
    Write-RunLog SUCCESS 'OpenSSH client tools are available. Authentication material was not inspected.'
}

function Test-SshConnectivity {
    if ($DryRun) {
        Write-RunLog PLAN "Would test SSH connectivity to $SshUser@$SshHost on port $SshPort without modifying the VPS."
        return
    }
    $arguments = @(Get-SshArguments) + @('-T', "$SshUser@$SshHost", 'true')
    try {
        $null = Invoke-CapturedNative $script:SshExecutable $arguments 'SSH connectivity check'
    }
    catch {
        if (-not $AllowInteractiveAuth) {
            throw "Non-interactive SSH key authentication is unavailable for $SshUser@$SshHost`:$SshPort. Load a key into ssh-agent, use a standard ~/.ssh key, or pass -IdentityFile. Details: $($_.Exception.Message)"
        }
        throw
    }
    Write-RunLog SUCCESS 'SSH connectivity passed.'
}

function Invoke-SshScript {
    param([string]$RemoteScript, [string]$Description)
    if ($DryRun) {
        Write-RunLog PLAN "Would run remote phase: $Description."
        return ''
    }
    $arguments = @(Get-SshArguments) + @('-T', "$SshUser@$SshHost", 'bash -s')
    $normalized = $RemoteScript -replace "`r`n", "`n"
    $output = @($normalized | & $script:SshExecutable @arguments 2>&1)
    $exitCode = $LASTEXITCODE
    foreach ($line in $output) {
        $safeLine = Protect-LogText $line.ToString()
        if ($safeLine) { Write-Host $safeLine }
    }
    if ($exitCode -ne 0) {
        throw "$Description failed with exit code $exitCode."
    }
    return (($output | ForEach-Object { Protect-LogText $_.ToString() }) -join [Environment]::NewLine).Trim()
}

function Get-ImageMetadata {
    param([string]$Tag, [string]$Component)
    $indexJson = Invoke-CapturedNative $script:DockerExecutable @('image', 'inspect', $Tag) "$Component image-index inspection"
    $indexResult = $indexJson | ConvertFrom-Json
    $indexImage = if ($indexResult -is [array]) { $indexResult[0] } else { $indexResult }
    if ($null -eq $indexImage) { throw "$Component image '$Tag' was not found." }

    # Buildx with provenance may load an OCI index containing the application
    # manifest plus an attestation manifest. Inspect the release platform
    # explicitly instead of treating empty index-level platform fields as data.
    $platformJson = Invoke-CapturedNative $script:DockerExecutable @(
        'image', 'inspect', '--platform', 'linux/amd64', $Tag
    ) "$Component linux/amd64 image inspection"
    $platformResult = $platformJson | ConvertFrom-Json
    $image = if ($platformResult -is [array]) { $platformResult[0] } else { $platformResult }
    $architecture = Get-ObjectPropertyValue $image 'Architecture'
    $operatingSystem = Get-ObjectPropertyValue $image 'Os'
    if ($operatingSystem -ne 'linux' -or $architecture -ne 'amd64') {
        throw "$Component image '$Tag' must be linux/amd64; detected $operatingSystem/$architecture."
    }
    $config = Get-ObjectPropertyValue $image 'Config'
    $labels = Get-ObjectPropertyValue $config 'Labels'
    $revision = Get-ObjectPropertyValue $labels 'org.opencontainers.image.revision'
    $version = Get-ObjectPropertyValue $labels 'org.opencontainers.image.version'
    if ($revision -ne $script:CommitSha) {
        throw "$Component image '$Tag' revision '$revision' does not match current HEAD '$($script:CommitSha)'."
    }
    if ($version -ne $ApplicationVersion) {
        throw "$Component image '$Tag' version '$version' does not match '$ApplicationVersion'."
    }
    if ($Component -eq 'Frontend') {
        $null = Invoke-CapturedNative $script:DockerExecutable @(
            'run', '--rm', '--entrypoint', '/bin/sh', $Tag,
            '-c', "grep -R -F -q '$($script:CommitSha)' /usr/share/nginx/html"
        ) 'Frontend embedded build-revision check'
    }
    $repoDigests = @(Get-ObjectPropertyValue $indexImage 'RepoDigests')
    return [pscustomobject]@{
        Component    = $Component
        Tag          = $Tag
        Id           = (Get-ObjectPropertyValue $indexImage 'Id')
        PlatformId   = (Get-ObjectPropertyValue $image 'Id')
        Architecture = $architecture
        Os           = $operatingSystem
        Revision     = $revision
        Version      = $version
        RepoDigests  = @($repoDigests | Where-Object { $_ })
    }
}

function Invoke-BuildPhase {
    Write-RunLog INFO 'PHASE: Build linux/amd64 release images.'
    $script:BuildTimestamp = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
    $backendContext = Join-Path $script:RepositoryRoot 'sami-backend'
    $frontendContext = Join-Path $script:RepositoryRoot 'sami-frontend'
    foreach ($required in @(
        (Join-Path $backendContext 'Dockerfile'),
        (Join-Path $frontendContext 'Dockerfile')
    )) {
        if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { throw "Required Dockerfile is missing: $required" }
    }

    $backendArgs = @(
        'buildx', 'build', '--platform', 'linux/amd64', '--load',
        '--build-arg', "BUILD_BRANCH=$($script:Branch)",
        '--build-arg', "BUILD_COMMIT=$($script:CommitSha)",
        '--build-arg', "APP_VERSION=$ApplicationVersion",
        '--tag', $BackendImageTag
    )
    $frontendArgs = @(
        'buildx', 'build', '--platform', 'linux/amd64', '--load',
        '--build-arg', "VITE_API_BASE_URL=$FrontendApiBaseUrl",
        '--build-arg', "VITE_BUILD_BRANCH=$($script:Branch)",
        '--build-arg', "VITE_BUILD_COMMIT=$($script:CommitSha)",
        '--build-arg', "VITE_BUILD_TIMESTAMP=$($script:BuildTimestamp)",
        '--build-arg', "VITE_APP_VERSION=$ApplicationVersion",
        '--tag', $FrontendImageTag
    )
    if ($NoCache) {
        $backendArgs += '--no-cache'
        $frontendArgs += '--no-cache'
    }
    $backendArgs += $backendContext
    $frontendArgs += $frontendContext

    if ($DryRun) {
        Write-RunLog PLAN "Would build $BackendImageTag from sami-backend for linux/amd64 with branch/commit/version metadata."
        Write-RunLog PLAN "Would build $FrontendImageTag from sami-frontend for linux/amd64 with /api and branch/commit/timestamp/version metadata."
        return
    }
    Invoke-Native $script:DockerExecutable $backendArgs "Building backend image '$BackendImageTag'."
    Invoke-Native $script:DockerExecutable $frontendArgs "Building frontend image '$FrontendImageTag'."
    $script:BackendImage = Get-ImageMetadata $BackendImageTag 'Backend'
    $script:FrontendImage = Get-ImageMetadata $FrontendImageTag 'Frontend'
    Write-RunLog SUCCESS "Backend image verified: $($script:BackendImage.Id), linux/amd64."
    Write-RunLog SUCCESS "Frontend image verified: $($script:FrontendImage.Id), linux/amd64."
}

function Publish-AtomicFile {
    param([string]$TemporaryPath, [string]$FinalPath)
    if (Test-Path -LiteralPath $FinalPath) {
        $backupPath = "$FinalPath.replace-backup-$([Guid]::NewGuid().ToString('N'))"
        try {
            [IO.File]::Replace($TemporaryPath, $FinalPath, $backupPath)
        }
        finally {
            if (Test-Path -LiteralPath $backupPath) {
                Remove-Item -LiteralPath $backupPath -Force -ErrorAction SilentlyContinue
            }
        }
    }
    else {
        [IO.File]::Move($TemporaryPath, $FinalPath)
    }
}

function Invoke-ExportPhase {
    Write-RunLog INFO 'PHASE: Export verified images and release manifest.'
    if ($DryRun) {
        Write-RunLog PLAN "Would verify current-HEAD images and atomically export two TARs plus release-manifest.json under $($script:ArtifactDirectory)."
        return
    }
    if (-not $script:BackendImage) { $script:BackendImage = Get-ImageMetadata $BackendImageTag 'Backend' }
    if (-not $script:FrontendImage) { $script:FrontendImage = Get-ImageMetadata $FrontendImageTag 'Frontend' }
    if (-not $script:BuildTimestamp) { $script:BuildTimestamp = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ') }

    $backendFinal = Join-Path $script:ArtifactDirectory 'sami-backend-test.tar'
    $frontendFinal = Join-Path $script:ArtifactDirectory 'sami-frontend-test.tar'
    $manifestFinal = Join-Path $script:ArtifactDirectory 'release-manifest.json'
    $nonce = [Guid]::NewGuid().ToString('N')
    $backendTemp = "$backendFinal.$nonce.tmp"
    $frontendTemp = "$frontendFinal.$nonce.tmp"
    $manifestTemp = "$manifestFinal.$nonce.tmp"

    try {
        Invoke-Native $script:DockerExecutable @('save', '--output', $backendTemp, $BackendImageTag) 'Exporting backend image to a temporary TAR.'
        Invoke-Native $script:DockerExecutable @('save', '--output', $frontendTemp, $FrontendImageTag) 'Exporting frontend image to a temporary TAR.'
        $backendHash = (Get-FileHash -LiteralPath $backendTemp -Algorithm SHA256).Hash.ToLowerInvariant()
        $frontendHash = (Get-FileHash -LiteralPath $frontendTemp -Algorithm SHA256).Hash.ToLowerInvariant()
        $manifestObject = [ordered]@{
            branch              = $script:Branch
            commitSha           = $script:CommitSha
            shortCommitSha      = $script:ShortCommitSha
            buildTimestamp      = $script:BuildTimestamp
            backendImageTag     = $BackendImageTag
            frontendImageTag    = $FrontendImageTag
            backendImageId      = $script:BackendImage.Id
            frontendImageId     = $script:FrontendImage.Id
            backendPlatformImageId = $script:BackendImage.PlatformId
            frontendPlatformImageId = $script:FrontendImage.PlatformId
            backendImageDigests = @($script:BackendImage.RepoDigests)
            frontendImageDigests = @($script:FrontendImage.RepoDigests)
            backendTarFilename  = 'sami-backend-test.tar'
            frontendTarFilename = 'sami-frontend-test.tar'
            backendSha256       = $backendHash
            frontendSha256      = $frontendHash
        }
        $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
        [IO.File]::WriteAllText($manifestTemp, ($manifestObject | ConvertTo-Json -Depth 6), $utf8NoBom)
        Publish-AtomicFile $backendTemp $backendFinal
        Publish-AtomicFile $frontendTemp $frontendFinal
        Publish-AtomicFile $manifestTemp $manifestFinal
        $script:Manifest = [pscustomobject]$manifestObject
        Write-RunLog SUCCESS "Backend SHA256: $backendHash"
        Write-RunLog SUCCESS "Frontend SHA256: $frontendHash"
        Write-RunLog SUCCESS "Artifacts published under $($script:ArtifactDirectory)."
    }
    finally {
        foreach ($temporary in @($backendTemp, $frontendTemp, $manifestTemp)) {
            if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force }
        }
    }
}

function Read-VerifiedManifest {
    $manifestPath = Join-Path $script:ArtifactDirectory 'release-manifest.json'
    if (-not (Test-Path -LiteralPath $manifestPath -PathType Leaf)) {
        throw "Release manifest is missing: $manifestPath"
    }
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    foreach ($property in @('branch', 'commitSha', 'backendImageTag', 'frontendImageTag', 'backendImageId', 'frontendImageId', 'backendTarFilename', 'frontendTarFilename', 'backendSha256', 'frontendSha256')) {
        if (-not (Get-ObjectPropertyValue $manifest $property)) { throw "Release manifest property '$property' is missing." }
    }
    if ($manifest.branch -ne 'development' -or $manifest.commitSha -ne $script:CommitSha) {
        throw 'Release manifest branch/commit does not match the current development HEAD.'
    }
    if ($manifest.backendImageTag -ne $BackendImageTag -or $manifest.frontendImageTag -ne $FrontendImageTag) {
        throw 'Release manifest image tags do not match configured deployment tags.'
    }
    foreach ($pair in @(
        [pscustomobject]@{ Name = 'backend'; File = $manifest.backendTarFilename; Expected = $manifest.backendSha256 },
        [pscustomobject]@{ Name = 'frontend'; File = $manifest.frontendTarFilename; Expected = $manifest.frontendSha256 }
    )) {
        if ($pair.File -notmatch '^[A-Za-z0-9._-]+$') { throw "Unsafe $($pair.Name) artifact filename in manifest." }
        if ($pair.Expected -notmatch '^[a-fA-F0-9]{64}$') { throw "Invalid $($pair.Name) SHA256 in manifest." }
        $path = Join-Path $script:ArtifactDirectory $pair.File
        if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Required $($pair.Name) artifact is missing: $path" }
        $actual = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($actual -ne $pair.Expected.ToLowerInvariant()) { throw "$($pair.Name) artifact checksum mismatch." }
    }
    $script:Manifest = $manifest
    return $manifest
}

function Invoke-UploadPhase {
    Write-RunLog INFO 'PHASE: Verify and atomically upload release artifacts.'
    if ($DryRun) {
        Write-RunLog PLAN "Would verify local artifacts, test key-based SSH, upload three .uploading files, and atomically rename them in $RemoteArtifactDirectory."
        return
    }
    $manifest = Read-VerifiedManifest
    Test-SshConnectivity
    $files = @($manifest.backendTarFilename, $manifest.frontendTarFilename, 'release-manifest.json')
    $mkdirScript = "set -euo pipefail`nmkdir -p '$RemoteArtifactDirectory'"
    $null = Invoke-SshScript $mkdirScript 'Remote artifact directory preparation'
    foreach ($file in $files) {
        $localPath = Join-Path $script:ArtifactDirectory $file
        $remoteTemporary = "$RemoteArtifactDirectory/$file.uploading"
        $arguments = @(Get-SshArguments -ForScp) + @($localPath, "${SshUser}@${SshHost}:$remoteTemporary")
        Invoke-Native $script:ScpExecutable $arguments "Uploading $file under a temporary remote name."
    }
    $renameScript = @'
set -euo pipefail
artifact_dir='__ARTIFACT_DIR__'
for file in sami-backend-test.tar sami-frontend-test.tar release-manifest.json; do
  test -f "$artifact_dir/$file.uploading"
done
for file in sami-backend-test.tar sami-frontend-test.tar release-manifest.json; do
  mv -f "$artifact_dir/$file.uploading" "$artifact_dir/$file"
done
'@.Replace('__ARTIFACT_DIR__', $RemoteArtifactDirectory)
    $null = Invoke-SshScript $renameScript 'Remote artifact publication'
    Write-RunLog SUCCESS 'Upload and remote atomic publication completed.'
}

function New-RemoteChecksumScript {
    param([object]$Manifest)
    return @'
set -euo pipefail
artifact_dir='__ARTIFACT_DIR__'
backend_expected='__BACKEND_HASH__'
frontend_expected='__FRONTEND_HASH__'
backend_actual="$(sha256sum "$artifact_dir/sami-backend-test.tar" | awk '{print $1}')"
frontend_actual="$(sha256sum "$artifact_dir/sami-frontend-test.tar" | awk '{print $1}')"
echo "Backend local/remote SHA256: $backend_expected / $backend_actual"
echo "Frontend local/remote SHA256: $frontend_expected / $frontend_actual"
test "$backend_actual" = "$backend_expected"
test "$frontend_actual" = "$frontend_expected"
'@.Replace('__ARTIFACT_DIR__', $RemoteArtifactDirectory).Replace('__BACKEND_HASH__', $Manifest.backendSha256.ToLowerInvariant()).Replace('__FRONTEND_HASH__', $Manifest.frontendSha256.ToLowerInvariant())
}

function New-RemoteDeployScript {
    param([object]$Manifest)
    $template = @'
set -Eeuo pipefail
artifact_dir='__ARTIFACT_DIR__'
compose_dir='__COMPOSE_DIR__'
history_root='__HISTORY_DIR__'
compose_file='__COMPOSE_FILE__'
environment_file='__ENV_FILE__'
backend_tag='__BACKEND_TAG__'
frontend_tag='__FRONTEND_TAG__'
backend_expected_id='__BACKEND_ID__'
frontend_expected_id='__FRONTEND_ID__'
deployment_timestamp='__DEPLOYMENT_TIMESTAMP__'
health_timeout='__HEALTH_TIMEOUT__'
manifest_path="$artifact_dir/release-manifest.json"
history_dir="$history_root/$deployment_timestamp"

cd "$compose_dir"
test -f "$compose_file"
test -f "$environment_file"
test -f "$manifest_path"
mkdir -p "$history_dir"

compose() {
  BACKEND_IMAGE="$backend_tag" FRONTEND_IMAGE="$frontend_tag" \
    docker compose --env-file "$environment_file" -f "$compose_file" "$@"
}

container_image_id() {
  local service="$1"
  local cid
  cid="$(compose ps -q "$service" 2>/dev/null || true)"
  if [ -n "$cid" ]; then docker inspect --format '{{.Image}}' "$cid"; else echo NONE; fi
}

wait_healthy() {
  local service="$1"
  local deadline=$(( $(date +%s) + health_timeout ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    local cid status
    cid="$(compose ps -q "$service" 2>/dev/null || true)"
    if [ -n "$cid" ]; then
      status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || true)"
      if [ "$status" = healthy ] || [ "$status" = running ]; then return 0; fi
      if [ "$status" = unhealthy ] || [ "$status" = exited ] || [ "$status" = dead ]; then return 1; fi
    fi
    sleep 5
  done
  return 1
}

previous_backend_id="$(container_image_id backend)"
previous_frontend_id="$(container_image_id frontend)"
compose ps --all > "$history_dir/pre-deployment-compose-ps.txt" || true
compose config --no-interpolate > "$history_dir/effective-compose-no-secrets.yml"
compose config --services > "$history_dir/compose-services.txt"
compose config --images > "$history_dir/compose-images.txt"
cp "$compose_file" "$history_dir/compose-source.yml"
cp "$manifest_path" "$history_dir/release-manifest.json"
printf 'PREVIOUS_BACKEND_IMAGE_ID=%s\nPREVIOUS_FRONTEND_IMAGE_ID=%s\nBACKEND_TAG=%s\nFRONTEND_TAG=%s\n' \
  "$previous_backend_id" "$previous_frontend_id" "$backend_tag" "$frontend_tag" \
  > "$history_dir/previous-images.env"

rollback_images() {
  echo 'Attempting application-image rollback; database and volumes are untouched.'
  if [ "$previous_backend_id" = NONE ] || [ "$previous_frontend_id" = NONE ]; then
    echo 'Rollback unavailable: a previous backend or frontend image ID was not recorded.' >&2
    return 1
  fi
  docker image inspect "$previous_backend_id" >/dev/null
  docker image inspect "$previous_frontend_id" >/dev/null
  docker tag "$previous_backend_id" "$backend_tag"
  docker tag "$previous_frontend_id" "$frontend_tag"
  compose up -d --no-deps --no-build --force-recreate backend
  wait_healthy backend
  compose up -d --no-deps --no-build --force-recreate frontend
  wait_healthy frontend
  curl --fail --silent --show-error http://127.0.0.1/health >/dev/null
  echo 'Application-image rollback succeeded.'
}

diagnose() {
  compose ps --all || true
  compose logs --tail=80 backend frontend 2>&1 | \
    sed -E 's/((password|passwd|secret|token|authorization|cookie)[[:space:]]*[:=][[:space:]]*)[^ ,;]+/\1[REDACTED]/Ig' || true
}

deploy_release() {
  docker load --input "$artifact_dir/sami-backend-test.tar"
  docker load --input "$artifact_dir/sami-frontend-test.tar"
  backend_actual_id="$(docker image inspect --format '{{.Id}}' "$backend_tag")"
  frontend_actual_id="$(docker image inspect --format '{{.Id}}' "$frontend_tag")"
  test "$backend_actual_id" = "$backend_expected_id"
  test "$frontend_actual_id" = "$frontend_expected_id"
  compose config --quiet

  db_cid="$(compose ps -q db 2>/dev/null || true)"
  if [ -z "$db_cid" ]; then compose up -d db; fi
  wait_healthy db
  compose up -d --no-deps --no-build --force-recreate backend
  wait_healthy backend
  compose up -d --no-deps --no-build --force-recreate frontend
  wait_healthy frontend
  curl --fail --silent --show-error http://127.0.0.1/ >/dev/null
  curl --fail --silent --show-error http://127.0.0.1/health >/dev/null
  for route in /auth/login /sales /automations /licensing; do
    curl --fail --silent --show-error "http://127.0.0.1$route" >/dev/null
  done
  api_status="$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' http://127.0.0.1/api/v1/menu || true)"
  case "$api_status" in 200|401|403) ;; *) echo "Unexpected API proxy status: $api_status" >&2; return 1 ;; esac
}

if ! deploy_release; then
  echo 'Deployment verification failed.' >&2
  diagnose
  rollback_images
  exit 1
fi

compose ps --all
echo "DEPLOYMENT_HISTORY=$history_dir"
echo 'Remote deployment and smoke checks passed.'
'@
    $result = $template.Replace('__ARTIFACT_DIR__', $RemoteArtifactDirectory)
    $result = $result.Replace('__COMPOSE_DIR__', $RemoteComposeDirectory)
    $result = $result.Replace('__HISTORY_DIR__', $RemoteHistoryDirectory)
    $result = $result.Replace('__COMPOSE_FILE__', $ComposeFile)
    $result = $result.Replace('__ENV_FILE__', $EnvironmentFile)
    $result = $result.Replace('__BACKEND_TAG__', $BackendImageTag)
    $result = $result.Replace('__FRONTEND_TAG__', $FrontendImageTag)
    $result = $result.Replace('__BACKEND_ID__', $Manifest.backendImageId)
    $result = $result.Replace('__FRONTEND_ID__', $Manifest.frontendImageId)
    $result = $result.Replace('__DEPLOYMENT_TIMESTAMP__', $script:DeploymentTimestamp)
    return $result.Replace('__HEALTH_TIMEOUT__', $HealthTimeoutSeconds.ToString())
}

function Invoke-DeployPhase {
    Write-RunLog INFO 'PHASE: Remote checksum verification and guarded deployment.'
    if ($DryRun) {
        Write-RunLog PLAN 'Would test key-based SSH, compare remote SHA256 values, back up non-secret deployment state, load exact images, recreate backend/frontend only, and run smoke checks with automatic application-image rollback.'
        return
    }
    $manifest = Read-VerifiedManifest
    Test-SshConnectivity
    $null = Invoke-SshScript (New-RemoteChecksumScript $manifest) 'Remote checksum verification'
    Write-RunLog SUCCESS 'Remote checksums match the local manifest.'
    $script:DeploymentTimestamp = (Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')
    $null = Invoke-SshScript (New-RemoteDeployScript $manifest) 'Remote deployment'
    Write-RunLog SUCCESS 'Remote container and route verification passed.'
    $verifyScript = Join-Path $script:ScriptRoot 'verify-deployment.ps1'
    & $verifyScript -BaseUrl $script:ApplicationUrl -RequestTimeoutSeconds 10 -RetryCount 6 -RetryDelaySeconds 5
    Write-RunLog SUCCESS 'External HTTP verification passed.'
}

function New-RemoteRollbackScript {
    $template = @'
set -Eeuo pipefail
compose_dir='__COMPOSE_DIR__'
history_root='__HISTORY_DIR__'
compose_file='__COMPOSE_FILE__'
environment_file='__ENV_FILE__'
backend_tag='__BACKEND_TAG__'
frontend_tag='__FRONTEND_TAG__'
health_timeout='__HEALTH_TIMEOUT__'

cd "$compose_dir"
test -f "$compose_file"
test -f "$environment_file"
history_dir=''
while IFS= read -r candidate; do
  if [ -f "$candidate/previous-images.env" ]; then history_dir="$candidate"; break; fi
done < <(find "$history_root" -mindepth 1 -maxdepth 1 -type d -print | sort -r)
test -n "$history_dir" || { echo 'No valid deployment-history entry exists.' >&2; exit 1; }

previous_backend_id="$(sed -n 's/^PREVIOUS_BACKEND_IMAGE_ID=//p' "$history_dir/previous-images.env" | head -n1)"
previous_frontend_id="$(sed -n 's/^PREVIOUS_FRONTEND_IMAGE_ID=//p' "$history_dir/previous-images.env" | head -n1)"
case "$previous_backend_id" in sha256:[a-f0-9]*) ;; *) echo 'Invalid previous backend image ID.' >&2; exit 1 ;; esac
case "$previous_frontend_id" in sha256:[a-f0-9]*) ;; *) echo 'Invalid previous frontend image ID.' >&2; exit 1 ;; esac
docker image inspect "$previous_backend_id" >/dev/null
docker image inspect "$previous_frontend_id" >/dev/null
docker tag "$previous_backend_id" "$backend_tag"
docker tag "$previous_frontend_id" "$frontend_tag"

compose() {
  BACKEND_IMAGE="$backend_tag" FRONTEND_IMAGE="$frontend_tag" \
    docker compose --env-file "$environment_file" -f "$compose_file" "$@"
}
wait_healthy() {
  local service="$1" deadline=$(( $(date +%s) + health_timeout ))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    local cid status
    cid="$(compose ps -q "$service" 2>/dev/null || true)"
    if [ -n "$cid" ]; then
      status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$cid" 2>/dev/null || true)"
      if [ "$status" = healthy ] || [ "$status" = running ]; then return 0; fi
      if [ "$status" = unhealthy ] || [ "$status" = exited ] || [ "$status" = dead ]; then return 1; fi
    fi
    sleep 5
  done
  return 1
}

compose up -d --no-deps --no-build --force-recreate backend
wait_healthy backend
compose up -d --no-deps --no-build --force-recreate frontend
wait_healthy frontend
curl --fail --silent --show-error http://127.0.0.1/health >/dev/null
compose ps --all
echo "Rolled back application images from $history_dir; database and volumes were untouched."
'@
    $result = $template.Replace('__COMPOSE_DIR__', $RemoteComposeDirectory)
    $result = $result.Replace('__HISTORY_DIR__', $RemoteHistoryDirectory)
    $result = $result.Replace('__COMPOSE_FILE__', $ComposeFile)
    $result = $result.Replace('__ENV_FILE__', $EnvironmentFile)
    $result = $result.Replace('__BACKEND_TAG__', $BackendImageTag)
    $result = $result.Replace('__FRONTEND_TAG__', $FrontendImageTag)
    return $result.Replace('__HEALTH_TIMEOUT__', $HealthTimeoutSeconds.ToString())
}

function Invoke-RollbackPhase {
    Write-RunLog INFO 'PHASE: Roll back application images from the latest valid history entry.'
    if ($DryRun) {
        Write-RunLog PLAN 'Would locate the latest valid remote history entry, verify both previous images still exist, retag them, recreate backend/frontend only, and verify health.'
        return
    }
    Test-SshConnectivity
    $null = Invoke-SshScript (New-RemoteRollbackScript) 'Remote rollback'
    Write-RunLog SUCCESS 'Remote application-image rollback passed.'
}

function Invoke-CleanupPhase {
    Write-RunLog INFO 'PHASE: Preview guarded local cleanup.'
    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    $preservedNames = @('sami-backend-test.tar', 'sami-frontend-test.tar', 'release-manifest.json')
    $artifactCandidates = @(
        Get-ChildItem -LiteralPath $script:ArtifactDirectory -File -ErrorAction SilentlyContinue |
            Where-Object { $_.LastWriteTime -lt $cutoff -and $preservedNames -notcontains $_.Name }
    )
    $logDirectory = Join-Path $script:ArtifactDirectory 'logs'
    $logCandidates = @(
        Get-ChildItem -LiteralPath $logDirectory -File -ErrorAction SilentlyContinue |
            Where-Object { $_.LastWriteTime -lt $cutoff -and $_.FullName -ne $script:LogFile }
    )
    $runningRefs = @()
    try {
        $runningOutput = Invoke-CapturedNative $script:DockerExecutable @('ps', '--format', '{{.Image}}') 'Running image reference check'
        $runningRefs = @($runningOutput -split "`r?`n" | Where-Object { $_ })
    }
    catch {
        $runningRefs = @()
    }
    $imageCandidates = New-Object System.Collections.Generic.List[string]
    $imageLines = Invoke-CapturedNative $script:DockerExecutable @('image', 'ls', '--format', '{{.Repository}}|{{.Tag}}') 'Local SAMI image cleanup inventory'
    foreach ($line in @($imageLines -split "`r?`n")) {
        $parts = $line -split '\|', 2
        if ($parts.Count -eq 2 -and @('sami-backend', 'sami-frontend') -contains $parts[0] -and $parts[1] -like 'build-check-*') {
            $reference = "$($parts[0]):$($parts[1])"
            if ($reference -notin @($BackendImageTag, $FrontendImageTag) -and $runningRefs -notcontains $reference) {
                $imageCandidates.Add($reference)
            }
        }
    }

    Write-RunLog PLAN ("Cleanup candidates: {0} old artifact(s), {1} old log(s), {2} non-running build-check image tag(s)." -f $artifactCandidates.Count, $logCandidates.Count, $imageCandidates.Count)
    foreach ($candidate in $artifactCandidates + $logCandidates) { Write-RunLog PLAN $candidate.FullName }
    foreach ($candidate in $imageCandidates) { Write-RunLog PLAN $candidate }
    Write-RunLog INFO 'Preserved: current release TARs/manifest, configured release tags, running images, all volumes, rollback history, source directories, and worktrees.'

    if ($DryRun -or -not $ConfirmCleanup) {
        if (-not $ConfirmCleanup) { Write-RunLog WARN 'No cleanup performed. Re-run with -ConfirmCleanup after reviewing this summary.' }
        return
    }
    foreach ($candidate in $artifactCandidates + $logCandidates) { Remove-Item -LiteralPath $candidate.FullName -Force }
    foreach ($candidate in $imageCandidates) { Invoke-Native $script:DockerExecutable @('image', 'rm', $candidate) "Removing local build-check image tag '$candidate'." }
    Write-RunLog SUCCESS 'Guarded local cleanup completed.'
}

function Write-FinalSummary {
    param([bool]$Succeeded)
    $elapsed = (Get-Date) - $script:StartedAt
    Write-Host ''
    Write-Host '=== SAMI deployment automation summary ===' -ForegroundColor Cyan
    Write-Host ("Result: {0}" -f $(if ($Succeeded) { 'SUCCESS' } else { 'FAILED' }))
    Write-Host "Mode: $Mode"
    Write-Host "Branch: $($script:Branch)"
    Write-Host "Commit: $($script:CommitSha)"
    if ($script:BackendImage) { Write-Host "Backend image ID: $($script:BackendImage.Id)" }
    if ($script:FrontendImage) { Write-Host "Frontend image ID: $($script:FrontendImage.Id)" }
    if ($script:Manifest) {
        Write-Host "Backend SHA256: $($script:Manifest.backendSha256)"
        Write-Host "Frontend SHA256: $($script:Manifest.frontendSha256)"
    }
    Write-Host "Remote: $SshUser@$SshHost`:$SshPort"
    Write-Host "Deployment timestamp: $($script:DeploymentTimestamp)"
    Write-Host "Application URL: $($script:ApplicationUrl)"
    Write-Host ('Elapsed: {0:hh\:mm\:ss}' -f $elapsed)
    if ($script:LogFile) { Write-Host "Log: $($script:LogFile)" }
}

$succeeded = $false
try {
    Initialize-Configuration
    Write-RunLog INFO "Starting SAMI deployment automation in $Mode mode$(if ($DryRun) { ' (dry run)' } else { '' })."
    Initialize-RepositoryState

    if (@('Build', 'Export', 'Full', 'Cleanup') -contains $Mode) { Initialize-Docker }
    if (@('Upload', 'Deploy', 'Full', 'Rollback') -contains $Mode) { Initialize-SshTools }

    switch ($Mode) {
        'Build' { Invoke-BuildPhase }
        'Export' { Invoke-ExportPhase }
        'Upload' { Invoke-UploadPhase }
        'Deploy' { Invoke-DeployPhase }
        'Full' {
            Invoke-BuildPhase
            Invoke-ExportPhase
            if (-not $SkipUpload) { Invoke-UploadPhase } else { Write-RunLog WARN 'Upload skipped by explicit request; remote artifacts must already match the local manifest.' }
            Invoke-DeployPhase
        }
        'Rollback' { Invoke-RollbackPhase }
        'Cleanup' { Invoke-CleanupPhase }
    }
    $succeeded = $true
    Write-RunLog SUCCESS "$Mode mode completed."
}
catch {
    if (-not $script:LogFile) { Write-Host (Protect-LogText $_.Exception.Message) -ForegroundColor Red }
    else { Write-RunLog ERROR $_.Exception.Message }
}
finally {
    Write-FinalSummary $succeeded
}

if (-not $succeeded) { exit 1 }
