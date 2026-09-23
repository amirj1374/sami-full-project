[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][ValidateSet('TASK','FEATURE','WAVE','PHASE')]
    [string]$ScopeType,
    [Parameter(Mandatory=$true)][string[]]$Steps,
    [Parameter(Mandatory=$true)][int]$CompletedIndex,
    [switch]$OwnerBlocked,
    [switch]$Interrupted
)
$Steps = @($Steps | ForEach-Object { $_ -split ',' }) | Where-Object { $_ }

# Executable continuation guard for Lead runs. A PHASE/WAVE child completion
# is never returned as a terminal result while the parent remains active.
if ($OwnerBlocked -or $Interrupted) { Write-Output 'TERMINAL'; exit 0 }
if ($ScopeType -in @('PHASE','WAVE') -and $CompletedIndex -lt ($Steps.Count - 1)) {
    $next = $CompletedIndex + 1
    Write-Output "CONTINUE:$($Steps[$next])"
    exit 0
}
Write-Output 'TERMINAL'
