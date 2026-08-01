<#
.SYNOPSIS
Runs unauthenticated deployment smoke checks against a SAMI web origin.

.DESCRIPTION
Checks the frontend root, nginx-backed health endpoint, login route, critical
module routes, and the nginx /api proxy. It does not submit credentials or
mutate application state. A thrown error produces a non-zero process exit when
the script is run directly.

.PARAMETER BaseUrl
The deployed application origin, for example http://87.248.131.157.

.EXAMPLE
.\scripts\verify-deployment.ps1 -BaseUrl http://87.248.131.157
#>

#requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^https?://')]
    [string]$BaseUrl,

    [ValidateRange(1, 120)]
    [int]$RequestTimeoutSeconds = 10,

    [ValidateRange(1, 30)]
    [int]$RetryCount = 6,

    [ValidateRange(0, 30)]
    [int]$RetryDelaySeconds = 5,

    [switch]$PassThru
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http

$origin = $BaseUrl.TrimEnd('/')
$checks = @(
    [pscustomobject]@{ Name = 'Frontend root'; Path = '/'; Allowed = @(200) },
    [pscustomobject]@{ Name = 'Backend health through nginx'; Path = '/health'; Allowed = @(200) },
    [pscustomobject]@{ Name = 'Login route'; Path = '/auth/login'; Allowed = @(200) },
    [pscustomobject]@{ Name = 'Sales route'; Path = '/sales'; Allowed = @(200) },
    [pscustomobject]@{ Name = 'Automation route'; Path = '/automations'; Allowed = @(200) },
    [pscustomobject]@{ Name = 'Licensing route'; Path = '/licensing'; Allowed = @(200) },
    # GET is intentionally not a login attempt. 400/401/403/405 proves nginx
    # reached the API instead of serving the SPA or returning a proxy failure.
    [pscustomobject]@{ Name = 'nginx API proxy'; Path = '/api/v1/auth/login'; Allowed = @(200, 400, 401, 403, 405) }
)

$handler = New-Object System.Net.Http.HttpClientHandler
$client = New-Object System.Net.Http.HttpClient($handler)
$client.Timeout = [TimeSpan]::FromSeconds($RequestTimeoutSeconds)
$results = New-Object System.Collections.Generic.List[object]

try {
    foreach ($check in $checks) {
        $uri = $origin + $check.Path
        $lastStatus = $null
        $lastError = $null
        $passed = $false

        for ($attempt = 1; $attempt -le $RetryCount; $attempt++) {
            try {
                $response = $client.GetAsync($uri).GetAwaiter().GetResult()
                $lastStatus = [int]$response.StatusCode
                $response.Dispose()
                if ($check.Allowed -contains $lastStatus) {
                    $passed = $true
                    break
                }
                $lastError = "Unexpected HTTP status $lastStatus"
            }
            catch {
                $lastError = $_.Exception.Message
            }

            if ($attempt -lt $RetryCount -and $RetryDelaySeconds -gt 0) {
                Start-Sleep -Seconds $RetryDelaySeconds
            }
        }

        $result = [pscustomobject]@{
            Name       = $check.Name
            Uri        = $uri
            Passed     = $passed
            StatusCode = $lastStatus
            Error      = $lastError
        }
        $results.Add($result)

        if ($passed) {
            Write-Host ("[PASS] {0} ({1})" -f $check.Name, $lastStatus) -ForegroundColor Green
        }
        else {
            Write-Host ("[FAIL] {0}: {1}" -f $check.Name, $lastError) -ForegroundColor Red
        }
    }
}
finally {
    $client.Dispose()
    $handler.Dispose()
}

$failures = @($results | Where-Object { -not $_.Passed })
if ($failures.Count -gt 0) {
    throw ("Deployment verification failed: {0} of {1} checks failed." -f $failures.Count, $results.Count)
}

Write-Host ("Deployment verification passed: {0} checks." -f $results.Count) -ForegroundColor Green
if ($PassThru) {
    $results
}
