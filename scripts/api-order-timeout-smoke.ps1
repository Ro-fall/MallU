param([string]$BaseUrl = 'http://localhost:8080')

$ErrorActionPreference='Stop'
function Assert-Equal($actual,$expected,[string]$message){if($actual -ne $expected){throw "$message. expected=[$expected], actual=[$actual]"}}
function Invoke-Json([string]$method,[string]$path,$body,$headers){$args=@{Method=$method;Uri="$BaseUrl$path";ErrorAction='Stop'};if($headers){$args.Headers=$headers};if($null -ne $body){$args.ContentType='application/json';$args.Body=($body|ConvertTo-Json -Depth 5)};Invoke-RestMethod @args}

$suffix=Get-Random -Minimum 100000 -Maximum 999999
$register=Invoke-Json 'POST' '/api/auth/register' @{username="timeout_smoke_$suffix";password='TimeoutSmoke_2026'} $null
$headers=@{Authorization="Bearer $($register.data.token)"}
$address=Invoke-Json 'POST' '/api/addresses' @{receiverName='Timeout Smoke';phone='13800138000';province='上海市';city='上海市';district='浦东新区';detail='超时测试地址';defaultAddress=$true} $headers
$before=(Invoke-Json 'GET' '/api/catalog/products/3' $null $null).data.stock
$cart=Invoke-Json 'POST' '/api/cart-items' @{productId=3;quantity=1} $headers
$token=Invoke-Json 'POST' '/api/orders/tokens' @{cartItemIds=@($cart.data.id)} $headers
$order=Invoke-Json 'POST' '/api/orders' @{addressId=$address.data.id;cartItemIds=@($cart.data.id);idempotencyToken=$token.data.token} $headers
for($i=0;$i -lt 10;$i++){Start-Sleep -Seconds 1;$detail=Invoke-Json 'GET' "/api/orders/$($order.data.id)" $null $headers;if($detail.data.status -eq 'CANCELLED'){break}}
$after=(Invoke-Json 'GET' '/api/catalog/products/3' $null $null).data.stock
Assert-Equal $detail.data.status 'CANCELLED' '超时订单未关闭'
Assert-Equal $after $before '超时订单未回补库存'
Write-Host 'PASS: timeout scheduler closed the order and restored stock.' -ForegroundColor Green
