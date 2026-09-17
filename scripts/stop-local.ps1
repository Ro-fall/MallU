$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtime = Join-Path $root '.runtime'

function Stop-OwnedProcess([string]$file, [string]$label) {
    $path = Join-Path $runtime $file
    if (Test-Path $path) {
        $processId = [int](Get-Content $path -Raw)
        & taskkill.exe /PID $processId /T /F 2>$null | Out-Null
        Remove-Item -LiteralPath $path -Force
        Write-Host "$label stopped if it was still running." -ForegroundColor Green
    }
}

Stop-OwnedProcess 'mallu.pid' 'MallU'
Stop-OwnedProcess 'redis-started-by-mallu.pid' 'Redis started by MallU'
