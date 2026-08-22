<#
.SYNOPSIS
Runs the SAMI Asan migration customer-validation candidate locally with Docker.

.DESCRIPTION
Creates local-only secrets, builds the current source with provenance labels,
starts PostgreSQL/backend/frontend through the existing production Compose,
and can collect infrastructure evidence without copying source rows or logs.
Stop preserves the named database and upload volumes so a customer can resume.

.EXAMPLE
.\scripts\asan-customer-validation.ps1 -Mode Start
.\scripts\asan-customer-validation.ps1 -Mode Evidence
.\scripts\asan-customer-validation.ps1 -Mode Stop
#>

#requires -Version 5.1
[CmdletBinding()]
param(
    [ValidateSet('Start', 'Status', 'Evidence', 'Stop')]
    [string]$Mode = 'Start',
    [ValidateRange(1024, 65535)]
    [int]$Port = 7475,
    [string]$ProjectName = 'sami-asan-customer-validation'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $scriptRoot '..')).Path
$composeFile = Join-Path $repositoryRoot 'sami-backend\docker-compose.prod.yml'
$artifactDirectory = Join-Path $repositoryRoot 'customer-validation-artifacts'
$environmentFile = Join-Path $artifactDirectory '.env.customer-validation'
$credentialFile = Join-Path $artifactDirectory 'customer-validation-credentials.txt'
$packageManifest = Join-Path $repositoryRoot 'customer-validation-manifest.json'

if ($ProjectName -notmatch '^[a-z0-9][a-z0-9_-]+$') {
    throw 'ProjectName may contain only lowercase letters, digits, underscores, and hyphens.'
}

function Invoke-Native {
    param([string]$Executable, [string[]]$Arguments, [string]$Description, [switch]$Capture)
    if ($Capture) {
        $output = @(& $Executable @Arguments 2>&1)
        if ($LASTEXITCODE -ne 0) { throw "$Description failed with exit code $LASTEXITCODE." }
        return ($output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine
    }
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Description failed with exit code $LASTEXITCODE." }
}

function New-HexSecret {
    param([ValidateRange(16, 128)][int]$Bytes = 32)
    $buffer = New-Object byte[] $Bytes
    [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($buffer)
    return ([BitConverter]::ToString($buffer)).Replace('-', '').ToLowerInvariant()
}

function Get-SourceIdentity {
    $gitDirectory = Join-Path $repositoryRoot '.git'
    if (Test-Path -LiteralPath $gitDirectory) {
        $sha = (Invoke-Native 'git' @('-C', $repositoryRoot, 'rev-parse', 'HEAD') 'Resolve source SHA' -Capture).Trim()
        $branch = (Invoke-Native 'git' @('-C', $repositoryRoot, 'branch', '--show-current') 'Resolve source branch' -Capture).Trim()
        return [pscustomobject]@{ CommitSha = $sha; Branch = $branch }
    }
    if (Test-Path -LiteralPath $packageManifest) {
        $manifest = Get-Content -Raw -LiteralPath $packageManifest | ConvertFrom-Json
        return [pscustomobject]@{ CommitSha = [string]$manifest.commitSha; Branch = [string]$manifest.branch }
    }
    throw 'Source provenance is unavailable. Use the original customer-validation ZIP.'
}

function Initialize-Environment {
    New-Item -ItemType Directory -Path $artifactDirectory -Force | Out-Null
    if (Test-Path -LiteralPath $environmentFile) { return }

    $source = Get-SourceIdentity
    if ($source.CommitSha -notmatch '^[a-f0-9]{40}$') { throw 'Source SHA is invalid.' }
    $shortSha = $source.CommitSha.Substring(0, 12)
    $adminPassword = 'Cv!' + (New-HexSecret 16)
    $databasePassword = New-HexSecret 32
    $jwtSecret = New-HexSecret 48
    $portalJwtSecret = New-HexSecret 48
    $timestamp = (Get-Date).ToUniversalTime().ToString('o')

    $environmentLines = @(
        'POSTGRES_DB=sami_customer_validation'
        'POSTGRES_USER=sami_validation'
        "POSTGRES_PASSWORD=$databasePassword"
        'SPRING_PROFILES_ACTIVE=prod'
        "JWT_SECRET=$jwtSecret"
        "PORTAL_JWT_SECRET=$portalJwtSecret"
        'JWT_ISSUER=sami-customer-validation'
        "CORS_ALLOWED_ORIGINS=http://localhost:$Port"
        'BOOTSTRAP_ADMIN_ENABLED=true'
        'BOOTSTRAP_ADMIN_EMAIL=validation@sami.local'
        "BOOTSTRAP_ADMIN_PASSWORD=$adminPassword"
        'BOOTSTRAP_ADMIN_NAME=Customer Validation'
        'DEMO_HOURLY_NOTIFICATION_ENABLED=false'
        "FRONTEND_PORT=$Port"
        'VITE_API_BASE_URL=/api'
        "BUILD_BRANCH=$($source.Branch)"
        "BUILD_COMMIT=$($source.CommitSha)"
        "BUILD_TIMESTAMP=$timestamp"
        "APP_VERSION=asan-customer-validation-$shortSha"
        "BACKEND_IMAGE=sami-backend:asan-customer-$shortSha"
        "FRONTEND_IMAGE=sami-frontend:asan-customer-$shortSha"
    )
    [IO.File]::WriteAllLines($environmentFile, $environmentLines, (New-Object Text.UTF8Encoding($false)))

    @(
        'SAMI Asan customer validation - local credentials'
        "Application: http://localhost:$Port"
        'Email: validation@sami.local'
        "Password: $adminPassword"
        ''
        'Keep this file private. Do not send it with validation evidence.'
    ) | Set-Content -LiteralPath $credentialFile -Encoding UTF8
}

function Get-ComposeArguments {
    param([string[]]$Tail)
    return @('compose', '--project-name', $ProjectName, '--env-file', $environmentFile, '-f', $composeFile) + $Tail
}

function Invoke-Compose {
    param([string[]]$Tail, [string]$Description, [switch]$Capture)
    return Invoke-Native 'docker' (Get-ComposeArguments $Tail) $Description -Capture:$Capture
}

function Assert-Docker {
    $null = Get-Command docker -CommandType Application -ErrorAction Stop
    Invoke-Native 'docker' @('info', '--format', '{{.ServerVersion}}') 'Docker Desktop engine check' | Out-Null
    Invoke-Native 'docker' @('compose', 'version') 'Docker Compose check' | Out-Null
    Invoke-Native 'docker' @('buildx', 'version') 'Docker Buildx check' | Out-Null
}

function Get-ServiceState {
    param([string]$Service)
    $containerId = (Invoke-Compose @('ps', '-q', $Service) "Resolve $Service container" -Capture).Trim()
    if (-not $containerId) { return 'missing' }
    return (Invoke-Native 'docker' @('inspect', '--format', '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}', $containerId) "Inspect $Service state" -Capture).Trim()
}

function Wait-HealthyStack {
    $deadline = (Get-Date).AddMinutes(8)
    do {
        $states = @('db', 'backend', 'frontend') | ForEach-Object { Get-ServiceState $_ }
        if (($states | Where-Object { $_ -ne 'healthy' }).Count -eq 0) { return }
        if (($states | Where-Object { $_ -in @('unhealthy', 'exited', 'dead') }).Count -gt 0) {
            throw "A validation service failed: $($states -join ', ')"
        }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for validation services: $($states -join ', ')"
}

function Get-HttpStatus {
    param([string]$Uri)
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri -Method Get -TimeoutSec 15
        return [int]$response.StatusCode
    } catch {
        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            return [int]$_.Exception.Response.StatusCode
        }
        return 0
    }
}

function Write-Status {
    Invoke-Compose @('ps', '--all') 'Read validation stack status'
    Write-Host "Application: http://localhost:$Port"
    Write-Host "Credentials: $credentialFile"
}

function Write-Evidence {
    $source = Get-SourceIdentity
    $services = foreach ($service in @('db', 'backend', 'frontend')) {
        $containerId = (Invoke-Compose @('ps', '-q', $service) "Resolve $service container" -Capture).Trim()
        $imageRevision = $null
        if ($containerId) {
            $imageRevision = (Invoke-Native 'docker' @('inspect', '--format', '{{index .Config.Labels "org.opencontainers.image.revision"}}', $containerId) "Read $service provenance" -Capture).Trim()
        }
        [ordered]@{ name = $service; state = Get-ServiceState $service; imageRevision = $imageRevision }
    }
    $evidence = [ordered]@{
        schemaVersion = '1.0'
        collectedAt = (Get-Date).ToUniversalTime().ToString('o')
        source = [ordered]@{ branch = $source.Branch; commitSha = $source.CommitSha }
        safety = [ordered]@{ rawRecordsIncluded = $false; logsIncluded = $false; secretsIncluded = $false; finalImportAvailable = $false }
        services = $services
        http = [ordered]@{
            frontend = Get-HttpStatus "http://localhost:$Port/"
            health = Get-HttpStatus "http://localhost:$Port/health"
            apiProxy = Get-HttpStatus "http://localhost:$Port/api/v1/menu"
        }
    }
    $evidencePath = Join-Path $artifactDirectory 'customer-validation-runtime-evidence.json'
    $evidence | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $evidencePath -Encoding UTF8
    Write-Host "Runtime evidence: $evidencePath"
}

Assert-Docker

switch ($Mode) {
    'Start' {
        Initialize-Environment
        Invoke-Compose @('config', '--quiet') 'Validate production Compose'
        Invoke-Compose @('up', '--build', '--detach') 'Build and start customer-validation stack'
        Wait-HealthyStack
        Write-Status
        Write-Host 'The validation stack is healthy. Final Import remains unavailable.' -ForegroundColor Green
    }
    'Status' {
        if (-not (Test-Path -LiteralPath $environmentFile)) { throw 'Validation environment has not been started.' }
        Write-Status
    }
    'Evidence' {
        if (-not (Test-Path -LiteralPath $environmentFile)) { throw 'Validation environment has not been started.' }
        Write-Evidence
    }
    'Stop' {
        if (-not (Test-Path -LiteralPath $environmentFile)) { throw 'Validation environment has not been started.' }
        Invoke-Compose @('down', '--remove-orphans') 'Stop customer-validation stack while preserving named volumes'
        Write-Host 'Containers and the project network were removed. Database and upload volumes were preserved.'
    }
}
