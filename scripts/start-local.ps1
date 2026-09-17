param(
    [string]$JarPath = (Join-Path $PSScriptRoot '..\target\mallu-1.0.0-SNAPSHOT.jar')
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtime = Join-Path $root '.runtime'
New-Item -ItemType Directory -Force -Path $runtime | Out-Null

if (-not $env:MALLU_DB_PASSWORD -or -not $env:MALLU_JWT_SECRET -or -not $env:MALLU_RABBITMQ_USERNAME -or -not $env:MALLU_RABBITMQ_PASSWORD) {
    throw '请先设置 MALLU_DB_PASSWORD、MALLU_JWT_SECRET、MALLU_RABBITMQ_USERNAME、MALLU_RABBITMQ_PASSWORD。'
}

$redisCli = Get-Command redis-cli.exe -ErrorAction Stop
function Test-RedisReady {
    $ping = & $redisCli.Source -h 127.0.0.1 -p 6379 ping 2>$null
    return $LASTEXITCODE -eq 0 -and $ping -eq 'PONG'
}

if (-not (Test-RedisReady)) {
    $redis = Get-Command redis-server.exe -ErrorAction Stop
    $redisLog = Join-Path $runtime 'redis.out'
    $redisErrorLog = Join-Path $runtime 'redis.err'
    $redisProcess = Start-Process -FilePath $redis.Source -ArgumentList @('--bind', '127.0.0.1', '--protected-mode', 'yes', '--port', '6379', '--appendonly', 'no') -RedirectStandardOutput $redisLog -RedirectStandardError $redisErrorLog -WindowStyle Hidden -PassThru
    $redisProcess.Id | Set-Content (Join-Path $runtime 'redis-started-by-mallu.pid')
    for ($attempt = 0; $attempt -lt 15; $attempt++) {
        if (Test-RedisReady) { break }
        Start-Sleep -Milliseconds 200
    }
    if (-not (Test-RedisReady)) { throw 'Redis 未能在 6379 启动，请检查 .runtime/redis.out 和 .runtime/redis.err。' }
    Write-Host "Redis started by MallU (PID $($redisProcess.Id))." -ForegroundColor Green
} else { Write-Host 'Redis already listens on 6379; MallU will reuse it and will not stop it.' -ForegroundColor Yellow }

$jar = (Resolve-Path $JarPath).Path
$appLog = Join-Path $runtime 'mallu.out'
$appErrorLog = Join-Path $runtime 'mallu.err'
$app = Start-Process -FilePath 'java.exe' -ArgumentList @('-jar', $jar) -WorkingDirectory $root -RedirectStandardOutput $appLog -RedirectStandardError $appErrorLog -WindowStyle Hidden -PassThru
$app.Id | Set-Content (Join-Path $runtime 'mallu.pid')
Write-Host "MallU started (PID $($app.Id)). Logs: .runtime/mallu.out" -ForegroundColor Green
