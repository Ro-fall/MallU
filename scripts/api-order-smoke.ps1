param(
    [string]$BaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'

function Assert-Equal($actual, $expected, [string]$message) {
    if ($actual -ne $expected) { throw "$message. expected=[$expected], actual=[$actual]" }
}

function Invoke-Json([string]$method, [string]$path, $body, $headers) {
    $args = @{ Method = $method; Uri = "$BaseUrl$path"; ErrorAction = 'Stop' }
    if ($null -ne $headers) { $args.Headers = $headers }
    if ($null -ne $body) { $args.ContentType = 'application/json'; $args.Body = ($body | ConvertTo-Json -Depth 5) }
    return Invoke-RestMethod @args
}

$suffix = Get-Random -Minimum 100000 -Maximum 999999
$username = "order_smoke_$suffix"
$password = 'OrderSmoke_2026'
$register = Invoke-Json 'POST' '/api/auth/register' @{ username = $username; password = $password } $null
Assert-Equal $register.code 0 '注册失败'
$headers = @{ Authorization = "Bearer $($register.data.token)" }

$address = Invoke-Json 'POST' '/api/addresses' @{
    receiverName = 'Order Smoke'; phone = '13800138000'; province = '上海市'; city = '上海市'
    district = '浦东新区'; detail = '自动化测试地址'; defaultAddress = $true
} $headers
$coupon = Invoke-Json 'POST' '/api/coupons/1/claim' $null $headers
$cartOne = Invoke-Json 'POST' '/api/cart-items' @{ productId = 1; quantity = 1 } $headers
$cartTwo = Invoke-Json 'POST' '/api/cart-items' @{ productId = 3; quantity = 2 } $headers

$firstOrder = Invoke-Json 'POST' '/api/orders' @{
    addressId = $address.data.id; userCouponId = $coupon.data.userCouponId; cartItemIds = @($cartOne.data.id)
} $headers
Assert-Equal $firstOrder.data.status 'PENDING_PAYMENT' '订单状态错误'
Assert-Equal $firstOrder.data.discountAmount 100 '满减金额错误'

$remainingCart = Invoke-Json 'GET' '/api/cart-items' $null $headers
Assert-Equal $remainingCart.data.Count 1 '部分结算后购物车数量错误'
Assert-Equal $remainingCart.data[0].productId 3 '未选中购物车项未保留'

$cancelled = Invoke-Json 'POST' "/api/orders/$($firstOrder.data.id)/cancel" $null $headers
Assert-Equal $cancelled.data.status 'CANCELLED' '取消订单失败'
$coupons = Invoke-Json 'GET' '/api/coupons/mine' $null $headers
Assert-Equal $coupons.data[0].status 'AVAILABLE' '取消订单后优惠券未释放'

$secondOrder = Invoke-Json 'POST' '/api/orders' @{ addressId = $address.data.id; cartItemIds = @($cartTwo.data.id) } $headers
$paid = Invoke-Json 'POST' "/api/orders/$($secondOrder.data.id)/pay" $null $headers
Assert-Equal $paid.data.status 'PAID' '模拟支付失败'
$profile = Invoke-Json 'GET' '/api/auth/me' $null $headers
Assert-Equal $profile.data.points 198 '支付积分错误'

Write-Host "PASS: order smoke test completed for $username" -ForegroundColor Green
