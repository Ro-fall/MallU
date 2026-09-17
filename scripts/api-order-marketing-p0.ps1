param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference = 'Stop'

function Assert-Eq($actual, $expected, [string]$name) { if ($actual -ne $expected) { throw "$name expected=[$expected], actual=[$actual]" } }
function Api([string]$method, [string]$path, $body = $null, $headers = $null) {
    $p = @{ Method=$method; Uri="$BaseUrl$path"; SkipHttpErrorCheck=$true; ErrorAction='Stop' }
    if ($headers) { $p.Headers=$headers }; if ($null -ne $body) { $p.ContentType='application/json'; $p.Body=$body|ConvertTo-Json -Depth 6 -Compress }
    $r=Invoke-WebRequest @p; [PSCustomObject]@{Status=[int]$r.StatusCode;Json=($r.Content|ConvertFrom-Json)}
}
function Check($r,[int]$status,[int]$code,[string]$name) { Assert-Eq $r.Status $status "$name HTTP"; Assert-Eq $r.Json.code $code "$name code" }
function User([string]$prefix) {
    $u="$prefix`_$(Get-Random -Minimum 100000 -Maximum 999999)"; $r=Api POST '/api/auth/register' @{username=$u;password='OrderP0_2026'}; Check $r 200 0 '注册'; return @{username=$u;headers=@{Authorization="Bearer $($r.Json.data.token)"}}
}
function Address($headers,[string]$name) { $r=Api POST '/api/addresses' @{receiverName=$name;phone='13800138000';province='上海市';city='上海市';district='浦东新区';detail='订单营销 P0';defaultAddress=$true} $headers; Check $r 200 0 '创建地址'; return $r.Json.data.id }
function Cart($headers,[long]$product) { $r=Api POST '/api/cart-items' @{productId=$product;quantity=1} $headers; Check $r 200 0 '加购'; return $r.Json.data.id }
function Token($headers,$ids) { $r=Api POST '/api/orders/tokens' @{cartItemIds=@($ids)} $headers; Check $r 200 0 '获取下单 Token'; return $r.Json.data.token }

$a=User 'order_p0'; $b=User 'order_other'; $addressA=Address $a.headers 'Order A'; $addressB=Address $b.headers 'Order B'
$item1=Cart $a.headers 1; $item3=Cart $a.headers 3

# MKT-001~003,008~009: coupon ownership and claim errors.
$coupon=Api POST '/api/coupons/1/claim' $null $a.headers; Check $coupon 200 0 'MKT-001 领取满减券'
$repeatCoupon=Api POST '/api/coupons/1/claim' $null $a.headers; Check $repeatCoupon 409 4091 'MKT-002 重复领券'
$missingCoupon=Api POST '/api/coupons/9999/claim' $null $a.headers; Check $missingCoupon 404 4045 'MKT-003 不存在券'
$mine=Api GET '/api/coupons/mine' $null $a.headers; Check $mine 200 0 'MKT-008 我的券'; Assert-Eq $mine.Json.data.Count 1 'MKT-008 券数量'
$anonymousCoupon=Api GET '/api/coupons/mine'; Check $anonymousCoupon 401 4010 'MKT-009 未登录查券'

# ORDER-003,004,008,013~016,024~028,051: request, ownership and query boundaries.
$emptyToken=Api POST '/api/orders/tokens' @{cartItemIds=@()} $a.headers; Check $emptyToken 400 4000 'ORDER-003 空项集 Token'
$duplicateToken=Api POST '/api/orders/tokens' @{cartItemIds=@($item1,$item1)} $a.headers; Check $duplicateToken 400 4004 'ORDER-004 重复项 Token'
$emptyOrder=Api POST '/api/orders' @{addressId=$addressA;cartItemIds=@();idempotencyToken='ignored'} $a.headers; Check $emptyOrder 400 4000 'ORDER-008 空项集下单'
$duplicateOrderToken=Token $a.headers @($item1)
$duplicateOrder=Api POST '/api/orders' @{addressId=$addressA;cartItemIds=@($item1,$item1);idempotencyToken=$duplicateOrderToken} $a.headers; Check $duplicateOrder 400 4004 'ORDER-013 重复购物车项下单'
$foreignItemToken=Token $b.headers @($item1)
$foreignItem=Api POST '/api/orders' @{addressId=$addressB;cartItemIds=@($item1);idempotencyToken=$foreignItemToken} $b.headers; Check $foreignItem 404 4044 'ORDER-014 他人购物车项'
$foreignAddressToken=Token $a.headers @($item1)
$foreignAddress=Api POST '/api/orders' @{addressId=$addressB;cartItemIds=@($item1);idempotencyToken=$foreignAddressToken} $a.headers; Check $foreignAddress 404 4043 'ORDER-015 他人地址'
$missingAddressToken=Token $a.headers @($item1)
$missingAddress=Api POST '/api/orders' @{addressId=9999;cartItemIds=@($item1);idempotencyToken=$missingAddressToken} $a.headers; Check $missingAddress 404 4043 'ORDER-016 不存在地址'
$anonymousOrder=Api POST '/api/orders'; Check $anonymousOrder 401 4010 'ORDER-051 未登录下单'

# ORDER-005~007,020~039; MKT-010,018~019,024: one complete coupon order state machine.
$orderToken=Token $a.headers @($item1)
$order=Api POST '/api/orders' @{addressId=$addressA;userCouponId=$coupon.Json.data.userCouponId;cartItemIds=@($item1);idempotencyToken=$orderToken} $a.headers
Check $order 200 0 'ORDER-005 部分结算'; Assert-Eq $order.Json.data.status 'PENDING_PAYMENT' 'ORDER-005 状态'; Assert-Eq $order.Json.data.discountAmount 100 'MKT-010 满减金额'
$cartAfter=Api GET '/api/cart-items' $null $a.headers; Assert-Eq $cartAfter.Json.data.Count 1 'ORDER-006 未选中项保留'; Assert-Eq $cartAfter.Json.data[0].id $item3 'ORDER-020 仅删除已结算项'
$list=Api GET '/api/orders' $null $a.headers; Check $list 200 0 'ORDER-024 订单列表'; Assert-Eq $list.Json.data.Count 1 'ORDER-024 本人订单数'
$detail=Api GET "/api/orders/$($order.Json.data.id)" $null $a.headers; Check $detail 200 0 'ORDER-026 订单详情'
$foreignDetail=Api GET "/api/orders/$($order.Json.data.id)" $null $b.headers; Check $foreignDetail 404 4047 'ORDER-027 他人订单详情'
$missingDetail=Api GET '/api/orders/9999' $null $a.headers; Check $missingDetail 404 4047 'ORDER-028 不存在订单'
$foreignCancel=Api POST "/api/orders/$($order.Json.data.id)/cancel" $null $b.headers; Check $foreignCancel 404 4047 'ORDER-034 他人取消'
$paid=Api POST "/api/orders/$($order.Json.data.id)/pay" $null $a.headers; Check $paid 200 0 'ORDER-035 支付'; Assert-Eq $paid.Json.data.status 'PAID' 'ORDER-035 支付状态'
$repeatPay=Api POST "/api/orders/$($order.Json.data.id)/pay" $null $a.headers; Check $repeatPay 409 4095 'ORDER-037 重复支付'
$paidCancel=Api POST "/api/orders/$($order.Json.data.id)/cancel" $null $a.headers; Check $paidCancel 409 4094 'ORDER-032 取消已支付订单'
$profile=Api GET '/api/auth/me' $null $a.headers; Assert-Eq $profile.Json.data.points 3899 'MKT-024/ORDER-036 支付积分'

Write-Host "PASS: order and marketing P0 regression completed for $($a.username)" -ForegroundColor Green
