$script = Join-Path $PSScriptRoot '..\agent\Invoke-PhaseContinuation.ps1'
$result = & pwsh -NoProfile -File $script -ScopeType PHASE -Steps 'P4-S3,P4-S4' -CompletedIndex 0
if ($result -ne 'CONTINUE:P4-S4') { throw "Phase continuation regression failed: $result" }
$result = & pwsh -NoProfile -File $script -ScopeType PHASE -Steps 'P4-S3,P4-S4' -CompletedIndex 0 -OwnerBlocked
if ($result -ne 'TERMINAL') { throw "Owner-blocked terminal regression failed: $result" }
Write-Output 'PASS: phase continuation remains executable after child completion'
