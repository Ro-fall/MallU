param([string]$BaseUrl = 'http://localhost:8080')

$ErrorActionPreference = 'Stop'
function Assert-Equal($actual, $expected, [string]$message) { if ($actual -ne $expected) { throw "$message. expected=[$expected], actual=[$actual]" } }
function Invoke-Json([string]$method, [string]$path, $body, $headers) {
    $args = @{ Method=$method; Uri="$BaseUrl$path"; ErrorAction='Stop' }
    if ($headers) { $args.Headers=$headers }
    if ($null -ne $body) { $args.ContentType='application/json'; $args.Body=($body | ConvertTo-Json -Depth 5) }
    Invoke-RestMethod @args
}

$suffix=Get-Random -Minimum 100000 -Maximum 999999
$register=Invoke-Json 'POST' '/api/auth/register' @{username="seckill_smoke_$suffix";password='SeckillSmoke_2026'} $null
$headers=@{Authorization="Bearer $($register.data.token)"}
$address=Invoke-Json 'POST' '/api/addresses' @{receiverName='Seckill Smoke';phone='13800138000';province='上海市';city='上海市';district='浦东新区';detail='秒杀自动化测试地址';defaultAddress=$true} $headers
$goods=Invoke-Json 'GET' '/api/seckill/activities/1/goods' $null $headers
Assert-Equal $goods.code 0 '秒杀商品查询失败'
$before=$goods.data[0].stock
$submitted=Invoke-Json 'POST' '/api/seckill/goods/1/orders' @{addressId=$address.data.id} $headers
Assert-Equal $submitted.data.status 'PENDING' '秒杀未进入排队状态'

for($i=0;$i -lt 30;$i++) {
    Start-Sleep -Milliseconds 250
    $result=Invoke-Json 'GET' '/api/seckill/goods/1/result' $null $headers
    if($result.data.status -eq 'SUCCESS') { break }
}
Assert-Equal $result.data.status 'SUCCESS' '秒杀消息未被消费成功'
$repeat=Invoke-WebRequest -SkipHttpErrorCheck -Method Post -Uri "$BaseUrl/api/seckill/goods/1/orders" -Headers $headers -ContentType 'application/json' -Body (@{addressId=$address.data.id}|ConvertTo-Json)
Assert-Equal $repeat.StatusCode 409 '重复秒杀 HTTP 状态错误'
Assert-Equal (($repeat.Content|ConvertFrom-Json).code) 4098 '一人一单未拦截'
Write-Host "PASS: seckill queue, RabbitMQ consumer, result polling, and one-user-one-order. stock_before=$before order_id=$($result.data.orderId)" -ForegroundColor Green
