param([string]$BaseUrl = 'http://localhost:8080', [string]$RabbitMqManagementUrl = 'http://127.0.0.1:15672', [switch]$SkipReset)

$ErrorActionPreference='Stop'
function Assert-Equal($actual,$expected,[string]$message){if($actual -ne $expected){throw "$message. expected=[$expected], actual=[$actual]"}}
function Invoke-Json([string]$method,[string]$path,$body,$headers){$args=@{Method=$method;Uri="$BaseUrl$path";ErrorAction='Stop'};if($headers){$args.Headers=$headers};if($null -ne $body){$args.ContentType='application/json';$args.Body=($body|ConvertTo-Json -Depth 5)};Invoke-RestMethod @args}
if (-not $SkipReset) {
    if (-not $env:MALLU_DB_PASSWORD) { throw '请设置 MALLU_DB_PASSWORD。' }
    & "$PSScriptRoot\reset-test-environment.ps1"
}

$suffix=Get-Random -Minimum 100000 -Maximum 999999
$register=Invoke-Json 'POST' '/api/auth/register' @{username="dead_smoke_$suffix";password='DeadSmoke_2026'} $null
$headers=@{Authorization="Bearer $($register.data.token)"}
$address=Invoke-Json 'POST' '/api/addresses' @{receiverName='Dead Smoke';phone='13800138000';province='上海市';city='上海市';district='浦东新区';detail='死信测试地址';defaultAddress=$true} $headers
$before=(Invoke-Json 'GET' '/api/seckill/activities/1/goods' $null $headers).data[0].stock
Invoke-Json 'POST' '/api/seckill/goods/1/orders' @{addressId=$address.data.id} $headers | Out-Null
for($i=0;$i -lt 40;$i++){Start-Sleep -Milliseconds 300;$result=Invoke-Json 'GET' '/api/seckill/goods/1/result' $null $headers;if($result.data.status -eq 'FAILED'){break}}
$after=(Invoke-Json 'GET' '/api/seckill/activities/1/goods' $null $headers).data[0].stock
Assert-Equal $result.data.status 'FAILED' '消费者未在重试后进入失败状态'
Assert-Equal $after $before '最终失败后秒杀库存未补偿'
$credential = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("$($env:MALLU_RABBITMQ_USERNAME):$($env:MALLU_RABBITMQ_PASSWORD)"))
$dead = $null
for ($i = 0; $i -lt 10; $i++) {
    $dead = Invoke-RestMethod -Uri "$RabbitMqManagementUrl/api/queues/%2F/mallu.seckill.dead.queue" -Headers @{Authorization="Basic $credential"}
    if ($dead.messages -ge 1) { break }
    Start-Sleep -Milliseconds 300
}
if ($dead.messages -lt 1) { throw '最终失败消息未进入死信队列' }
Write-Host 'PASS: consumer retries, dead-letter path, Redis compensation, and FAILED result.' -ForegroundColor Green
