param([string]$BaseUrl = 'http://localhost:8080', [string]$ReportPath = 'test-results/latest-regression.json')
$ErrorActionPreference = 'Stop'
$root = Resolve-Path "$PSScriptRoot\.."
$results = [System.Collections.Generic.List[object]]::new()
$totalDesigned = 300
$automatedCovered = 109
$suiteCount = 3
function Run([string]$name, [string]$script) {
    $position = $results.Count + 1
    Write-Host "[$position/$suiteCount] START $name" -ForegroundColor Cyan
    $started = Get-Date
    try {
        & (Join-Path $PSScriptRoot $script) -BaseUrl $BaseUrl
        $results.Add(@{ suite=$name; status='PASSED'; durationMs=[int]((Get-Date)-$started).TotalMilliseconds })
        Write-Host "[$position/$suiteCount] PASS  $name" -ForegroundColor Green
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
    [pscustomobject]@{ generatedAt=(Get-Date).ToUniversalTime().ToString('o'); baseUrl=$BaseUrl; coverage=@{ designed=$totalDesigned; automated=$automatedCovered; remaining=$totalDesigned-$automatedCovered; currentRun=$suiteCount }; suites=$results } |
        ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $root $ReportPath)
}
if (@($results | Where-Object status -eq 'FAILED').Count -gt 0) { exit 1 }
Write-Host "PASS: $($results.Count)/$suiteCount suites completed | automation coverage: $automatedCovered/$totalDesigned | Report: $ReportPath" -ForegroundColor Green
