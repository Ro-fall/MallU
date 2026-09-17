param([string]$BaseUrl = 'http://localhost:8080')

$ErrorActionPreference = 'Stop'
function Assert-Equal($actual, $expected, [string]$message) { if ($actual -ne $expected) { throw "$message. expected=[$expected], actual=[$actual]" } }
function Invoke-Json([string]$method, [string]$path, $body, $headers) {
    $args = @{ Method = $method; Uri = "$BaseUrl$path"; ErrorAction = 'Stop' }
    if ($headers) { $args.Headers = $headers }
    if ($null -ne $body) { $args.ContentType = 'application/json'; $args.Body = ($body | ConvertTo-Json -Depth 5) }
    Invoke-RestMethod @args
}

$health = Invoke-Json 'GET' '/api/health/redis' $null $null
Assert-Equal $health.code 0 'Redis health endpoint failed'
Assert-Equal $health.data.available $true 'Redis is not available'

$suffix = Get-Random -Minimum 100000 -Maximum 999999
$register = Invoke-Json 'POST' '/api/auth/register' @{ username = "redis_smoke_$suffix"; password = 'RedisSmoke_2026' } $null
$headers = @{ Authorization = "Bearer $($register.data.token)" }
$address = Invoke-Json 'POST' '/api/addresses' @{ receiverName='Redis Smoke';phone='13800138000';province='上海市';city='上海市';district='浦东新区';detail='Redis 自动化测试地址';defaultAddress=$true } $headers
$cart = Invoke-Json 'POST' '/api/cart-items' @{ productId=3;quantity=1 } $headers
$token = Invoke-Json 'POST' '/api/orders/tokens' @{ cartItemIds=@($cart.data.id) } $headers
$orderPayload = @{ addressId=$address.data.id;cartItemIds=@($cart.data.id);idempotencyToken=$token.data.token }
$order = Invoke-Json 'POST' '/api/orders' $orderPayload $headers
Assert-Equal $order.code 0 '首次下单失败'

$repeat = Invoke-WebRequest -SkipHttpErrorCheck -Method Post -Uri "$BaseUrl/api/orders" -Headers $headers -ContentType 'application/json' -Body ($orderPayload | ConvertTo-Json)
Assert-Equal $repeat.StatusCode 409 '重复下单 HTTP 状态错误'
Assert-Equal (($repeat.Content | ConvertFrom-Json).code) 4096 '重复 Token 未被拒绝'

$boundCart = Invoke-Json 'POST' '/api/cart-items' @{ productId=1;quantity=1 } $headers
$boundToken = Invoke-Json 'POST' '/api/orders/tokens' @{ cartItemIds=@($boundCart.data.id) } $headers
$mismatchPayload = @{ addressId=$address.data.id;cartItemIds=@($boundCart.data.id, $cart.data.id);idempotencyToken=$boundToken.data.token }
$mismatch = Invoke-WebRequest -SkipHttpErrorCheck -Method Post -Uri "$BaseUrl/api/orders" -Headers $headers -ContentType 'application/json' -Body ($mismatchPayload | ConvertTo-Json)
Assert-Equal $mismatch.StatusCode 409 'Token 绑定不匹配 HTTP 状态错误'
Assert-Equal (($mismatch.Content | ConvertFrom-Json).code) 4097 'Token 未绑定购物车项'
Write-Host 'PASS: Redis health, bound idempotency token, duplicate-submit protection, and cart binding.' -ForegroundColor Green
