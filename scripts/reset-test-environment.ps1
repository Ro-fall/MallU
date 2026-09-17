param(
    [switch]$SkipRabbitMq,
    [switch]$SkipRedisRebuildWait,
    [string]$MysqlHost = '127.0.0.1',
    [int]$MysqlPort = 3306,
    [string]$RabbitMqManagementUrl = 'http://127.0.0.1:15672'
)

$ErrorActionPreference = 'Stop'

if (-not $env:MALLU_DB_PASSWORD) {
    throw '请为当前终端设置 MALLU_DB_PASSWORD；脚本不会保存数据库密码。'
}

function Invoke-MalluSql([string]$file) {
    $mysql = Get-Command mysql.exe -ErrorAction Stop
    $previousPassword = $env:MYSQL_PWD
    try {
        # MYSQL_PWD only exists in this PowerShell process while mysql imports the fixture.
        $env:MYSQL_PWD = $env:MALLU_DB_PASSWORD
        $source = (Resolve-Path $file).Path.Replace('\\', '/')
        & $mysql.Source --protocol=TCP --host=$MysqlHost --port=$MysqlPort --user=root --default-character-set=utf8mb4 -e "SOURCE $source"
        if ($LASTEXITCODE -ne 0) { throw "导入 SQL 失败：$file" }
    } finally {
        $env:MYSQL_PWD = $previousPassword
    }
}

function Clear-MalluRedisKeys {
    $redisCli = Get-Command redis-cli.exe -ErrorAction Stop
    $keys = @(& $redisCli.Source -h 127.0.0.1 -p 6379 --scan --pattern 'mallu:*')
    if ($LASTEXITCODE -ne 0) { throw '无法连接 Redis，拒绝执行不完整的测试环境重置。' }
    if ($keys.Count -gt 0) {
        & $redisCli.Source -h 127.0.0.1 -p 6379 del @keys | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Redis MallU 键清理失败。' }
    }
    return $keys.Count
}

function Clear-MalluRabbitMqQueues {
    if (-not $env:MALLU_RABBITMQ_USERNAME -or -not $env:MALLU_RABBITMQ_PASSWORD) {
        throw '请设置 MALLU_RABBITMQ_USERNAME 和 MALLU_RABBITMQ_PASSWORD，或显式传入 -SkipRabbitMq。'
    }
    $pair = "$($env:MALLU_RABBITMQ_USERNAME):$($env:MALLU_RABBITMQ_PASSWORD)"
    $token = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair))
    $headers = @{ Authorization = "Basic $token" }
    $queues = @('mallu.seckill.order.queue', 'mallu.seckill.retry.queue', 'mallu.seckill.dead.queue')
    foreach ($queue in $queues) {
        $escapedQueue = [uri]::EscapeDataString($queue)
        $uri = "$RabbitMqManagementUrl/api/queues/%2F/$escapedQueue/contents"
        $response = Invoke-WebRequest -Method Delete -Uri $uri -Headers $headers -SkipHttpErrorCheck
        if ($response.StatusCode -notin @(204, 404)) { throw "清空 RabbitMQ 队列失败：$queue，HTTP $($response.StatusCode)" }
    }
}

Invoke-MalluSql 'src/main/resources/sql/reset-fixtures.sql'
$redisCount = Clear-MalluRedisKeys
if (-not $SkipRabbitMq) { Clear-MalluRabbitMqQueues }
if (-not $SkipRedisRebuildWait) {
    # The application restores only missing seckill stock keys, so this never overwrites a live Lua reservation.
    Start-Sleep -Seconds 31
}

Write-Host "PASS: test environment reset (MySQL fixtures restored; Redis MallU keys removed: $redisCount; RabbitMQ cleared: $(-not $SkipRabbitMq))." -ForegroundColor Green
