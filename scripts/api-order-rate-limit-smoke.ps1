param([string]$BaseUrl = 'http://localhost:8080')

$ErrorActionPreference = 'Stop'
function Assert-Equal($actual, $expected, [string]$message) { if ($actual -ne $expected) { throw "$message. expected=[$expected], actual=[$actual]" } }
function Invoke-Json([string]$method, [string]$path, $body, $headers) {
    $args=@{Method=$method;Uri="$BaseUrl$path";ErrorAction='Stop'}; if($headers){$args.Headers=$headers}; if($null -ne $body){$args.ContentType='application/json';$args.Body=($body|ConvertTo-Json -Depth 5)}; Invoke-RestMethod @args
}
$suffix=Get-Random -Minimum 100000 -Maximum 999999
$register=Invoke-Json 'POST' '/api/auth/register' @{username="rate_smoke_$suffix";password='RateSmoke_2026'} $null
$headers=@{Authorization="Bearer $($register.data.token)"}
$address=Invoke-Json 'POST' '/api/addresses' @{receiverName='Rate Smoke';phone='13800138000';province='上海市';city='上海市';district='浦东新区';detail='限流测试地址';defaultAddress=$true} $headers
$cart=Invoke-Json 'POST' '/api/cart-items' @{productId=3;quantity=1} $headers
$last=$null
for($i=1;$i -le 11;$i++) {
    $token=Invoke-Json 'POST' '/api/orders/tokens' @{cartItemIds=@($cart.data.id)} $headers
    $last=Invoke-WebRequest -SkipHttpErrorCheck -Method Post -Uri "$BaseUrl/api/orders" -Headers $headers -ContentType 'application/json' -Body (@{addressId=$address.data.id;cartItemIds=@($cart.data.id);idempotencyToken=$token.data.token}|ConvertTo-Json)
}
Assert-Equal $last.StatusCode 429 '第 11 次下单未触发限流'
Assert-Equal (($last.Content|ConvertFrom-Json).code) 4291 '限流业务码错误'
Write-Host 'PASS: fixed-window order rate limit rejects the 11th request.' -ForegroundColor Green
