param([string]$BaseUrl = 'http://localhost:8080', [string]$ReportPath = 'test-results/latest-regression.json')
$ErrorActionPreference = 'Stop'
$root = Resolve-Path "$PSScriptRoot\.."
$results = [System.Collections.Generic.List[object]]::new()
function Run([string]$name, [string]$script) {
    $started = Get-Date
    try {
        & (Join-Path $PSScriptRoot $script) -BaseUrl $BaseUrl
        $results.Add(@{ suite=$name; status='PASSED'; durationMs=[int]((Get-Date)-$started).TotalMilliseconds })
    } catch {
        $results.Add(@{ suite=$name; status='FAILED'; durationMs=[int]((Get-Date)-$started).TotalMilliseconds; error=$_.Exception.Message })
        throw
    }
}
try {
    Run 'auth-catalog-cart-p0' 'api-auth-catalog-cart-p0.ps1'
    Run 'order-marketing-p0' 'api-order-marketing-p0.ps1'
    Run 'catalog-edge' 'api-catalog-edge.ps1'
} finally {
    $directory = Split-Path -Parent (Join-Path $root $ReportPath)
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    [pscustomobject]@{ generatedAt=(Get-Date).ToUniversalTime().ToString('o'); baseUrl=$BaseUrl; suites=$results } |
        ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $root $ReportPath)
}
if (@($results | Where-Object status -eq 'FAILED').Count -gt 0) { exit 1 }
Write-Host "PASS: $($results.Count) regression suites completed. Report: $ReportPath" -ForegroundColor Green
