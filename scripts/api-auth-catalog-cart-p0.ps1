param(
    [string]$BaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'

# P0 mappings: AUTH-002~016,025,027; CAT-001~012,014,018,019; CART-001~031,039,040.
# Every run creates isolated users. Run reset-fixtures.sql before a clean, repeatable full regression.

function Assert-Equal($actual, $expected, [string]$message) {
    if ($actual -ne $expected) { throw "$message. expected=[$expected], actual=[$actual]" }
}

function Assert-True($condition, [string]$message) {
    if (-not $condition) { throw $message }
}

function Invoke-Api([string]$method, [string]$path, $body = $null, $headers = $null) {
    $request = @{ Method = $method; Uri = "$BaseUrl$path"; SkipHttpErrorCheck = $true; ErrorAction = 'Stop' }
    if ($null -ne $headers) { $request.Headers = $headers }
    if ($null -ne $body) {
        $request.ContentType = 'application/json'
        $request.Body = $body | ConvertTo-Json -Depth 6 -Compress
    }
    $response = Invoke-WebRequest @request
    $json = if ([string]::IsNullOrWhiteSpace($response.Content)) { $null } else { $response.Content | ConvertFrom-Json }
    return [PSCustomObject]@{ Status = [int]$response.StatusCode; Json = $json }
}

function Assert-Api([object]$response, [int]$status, [int]$code, [string]$message) {
    Assert-Equal $response.Status $status "$message HTTP 状态"
    Assert-Equal $response.Json.code $code "$message 业务码"
}

function Register-User([string]$prefix) {
    $suffix = Get-Random -Minimum 100000 -Maximum 999999
    $username = "${prefix}_$suffix"
    $password = 'P0Cart_2026'
    $response = Invoke-Api 'POST' '/api/auth/register' @{ username = $username; password = $password }
    Assert-Api $response 200 0 "注册 $username"
    Assert-True (-not [string]::IsNullOrWhiteSpace($response.Json.data.token)) '注册响应未返回 Token'
    Assert-True (-not ($response.Json.data.PSObject.Properties.Name -contains 'password')) '注册响应泄露 password'
    return [PSCustomObject]@{ Username = $username; Password = $password; Headers = @{ Authorization = "Bearer $($response.Json.data.token)" } }
}

# AUTH: validation, login, token authentication and response privacy.
$primary = Register-User 'p0_auth'
$duplicate = Invoke-Api 'POST' '/api/auth/register' @{ username = $primary.Username; password = $primary.Password }
Assert-Api $duplicate 409 4001 'AUTH-002 重复用户名注册'

foreach ($invalid in @(
    @{ username = 'abc'; password = 'P0Cart_2026'; id = 'AUTH-003' },
    @{ username = ('a' * 33); password = 'P0Cart_2026'; id = 'AUTH-004' },
    @{ username = 'bad-name'; password = 'P0Cart_2026'; id = 'AUTH-005' },
    @{ username = ''; password = 'P0Cart_2026'; id = 'AUTH-006' },
    @{ username = 'valid_user'; password = 'short'; id = 'AUTH-007' },
    @{ username = 'valid_user'; password = ('x' * 73); id = 'AUTH-008' }
)) {
    $response = Invoke-Api 'POST' '/api/auth/register' @{ username = $invalid.username; password = $invalid.password }
    Assert-Api $response 400 4000 "$($invalid.id) 注册参数校验"
}

$wrongPassword = Invoke-Api 'POST' '/api/auth/login' @{ username = $primary.Username; password = 'WrongPassword_2026' }
Assert-Api $wrongPassword 401 4011 'AUTH-010 错误密码登录'
Assert-True (-not ($wrongPassword.Json.data -and $wrongPassword.Json.data.token)) '错误登录不应返回 Token'
$unknownUser = Invoke-Api 'POST' '/api/auth/login' @{ username = 'missing_user_2026'; password = 'P0Cart_2026' }
Assert-Api $unknownUser 401 4011 'AUTH-011 不存在用户登录'
$emptyLogin = Invoke-Api 'POST' '/api/auth/login' @{}
Assert-Api $emptyLogin 400 4000 'AUTH-012 空登录请求'
$login = Invoke-Api 'POST' '/api/auth/login' @{ username = $primary.Username; password = $primary.Password }
Assert-Api $login 200 0 'AUTH-009 正确密码登录'
$primary.Headers = @{ Authorization = "Bearer $($login.Json.data.token)" }
$profile = Invoke-Api 'GET' '/api/auth/me' $null $primary.Headers
Assert-Api $profile 200 0 'AUTH-013 当前用户'
Assert-Equal $profile.Json.data.username $primary.Username 'AUTH-013 用户名不一致'
$missingAuth = Invoke-Api 'GET' '/api/auth/me'
Assert-Api $missingAuth 401 4010 'AUTH-014 缺失 Authorization'
$badPrefix = Invoke-Api 'GET' '/api/auth/me' $null @{ Authorization = $login.Json.data.token }
Assert-Api $badPrefix 401 4010 'AUTH-015 缺失 Bearer 前缀'
$badJwt = Invoke-Api 'GET' '/api/auth/me' $null @{ Authorization = 'Bearer malformed.token.value' }
Assert-Api $badJwt 401 4010 'AUTH-016 非法 JWT'

# CAT: public catalogue, pagination, searching and not-found behavior.
$categories = Invoke-Api 'GET' '/api/catalog/categories'
Assert-Api $categories 200 0 'CAT-001 分类列表'
Assert-Equal $categories.Json.data.Count 3 'CAT-001 固定分类数量'
$products = Invoke-Api 'GET' '/api/catalog/products'
Assert-Api $products 200 0 'CAT-002 默认商品分页'
Assert-Equal $products.Json.data.number 0 'CAT-002 Spring 分页索引应为 0'
Assert-True ($products.Json.data.content.Count -ge 3) 'CAT-002 缺少固定商品夹具'
$categoryProducts = Invoke-Api 'GET' '/api/catalog/products?categoryId=1&size=50'
Assert-Api $categoryProducts 200 0 'CAT-003 指定分类商品'
Assert-True (@($categoryProducts.Json.data.content | Where-Object { $_.categoryId -ne 1 }).Count -eq 0) 'CAT-003 返回了其他分类商品'
$emptyCategory = Invoke-Api 'GET' '/api/catalog/products?categoryId=9999'
Assert-Api $emptyCategory 200 0 'CAT-004 空分类商品'
Assert-Equal $emptyCategory.Json.data.content.Count 0 'CAT-004 空分类不应有商品'
foreach ($path in @('/api/catalog/products?page=0', '/api/catalog/products?size=51', '/api/catalog/products?size=-1')) {
    $response = Invoke-Api 'GET' $path
    Assert-Api $response 400 4000 "CAT 分页边界 $path"
}
$oneProduct = Invoke-Api 'GET' '/api/catalog/products?size=1'
Assert-Api $oneProduct 200 0 'CAT-007 size=1'
Assert-Equal $oneProduct.Json.data.content.Count 1 'CAT-007 size=1 返回数量错误'
$recommendations = Invoke-Api 'GET' '/api/catalog/products/recommendations'
Assert-Api $recommendations 200 0 'CAT-011 推荐商品'
Assert-True (@($recommendations.Json.data | Where-Object { -not $_.recommended }).Count -eq 0) 'CAT-011 推荐列表包含非推荐商品'
$search = Invoke-Api 'GET' '/api/catalog/products/search?keyword=Pro'
Assert-Api $search 200 0 'CAT-012 名称搜索'
Assert-True (@($search.Json.data.content | Where-Object { $_.name -notlike '*Pro*' }).Count -eq 0) 'CAT-012 搜索结果未命中名称'
$noResult = Invoke-Api 'GET' '/api/catalog/products/search?keyword=does-not-exist'
Assert-Api $noResult 200 0 'CAT-014 空搜索'
Assert-Equal $noResult.Json.data.content.Count 0 'CAT-014 无结果搜索应为空'
$detail = Invoke-Api 'GET' '/api/catalog/products/1'
Assert-Api $detail 200 0 'CAT-018 商品详情'
Assert-Equal $detail.Json.data.stock 100 'CAT-018 固定商品库存不符'
$notFound = Invoke-Api 'GET' '/api/catalog/products/9999'
Assert-Api $notFound 404 4042 'CAT-019 不存在商品详情'

# CART: address defaults, ownership, cart merge/update/delete, stock validation and anonymous access.
$addressOne = Invoke-Api 'POST' '/api/addresses' @{ receiverName = 'P0 One'; phone = '13800138000'; province = '上海市'; city = '上海市'; district = '浦东新区'; detail = '测试路 1 号'; defaultAddress = $true } $primary.Headers
Assert-Api $addressOne 200 0 'CART-001/CART-002 新增默认地址'
Assert-Equal $addressOne.Json.data.defaultAddress $true 'CART-002 默认地址标识错误'
$addressTwo = Invoke-Api 'POST' '/api/addresses' @{ receiverName = 'P0 Two'; phone = '13900139000'; province = '北京市'; city = '北京市'; district = '朝阳区'; detail = '测试路 2 号'; defaultAddress = $true } $primary.Headers
Assert-Api $addressTwo 200 0 'CART-003 第二默认地址'
$addresses = Invoke-Api 'GET' '/api/addresses' $null $primary.Headers
Assert-Api $addresses 200 0 'CART-004 地址列表'
Assert-Equal @($addresses.Json.data | Where-Object { $_.defaultAddress }).Count 1 'CART-003 只能存在一个默认地址'
Assert-Equal $addresses.Json.data[0].id $addressTwo.Json.data.id 'CART-004 默认地址未优先返回'
$other = Register-User 'p0_other'
$otherAddressRead = Invoke-Api 'GET' '/api/addresses' $null $other.Headers
Assert-Equal $otherAddressRead.Json.data.Count 0 'CART-008 地址数据越权泄露'

$cart = Invoke-Api 'POST' '/api/cart-items' @{ productId = 1; quantity = 1 } $primary.Headers
Assert-Api $cart 200 0 'CART-014 有效加购'
Assert-Equal $cart.Json.data.quantity 1 'CART-014 加购数量错误'
$merged = Invoke-Api 'POST' '/api/cart-items' @{ productId = 1; quantity = 2 } $primary.Headers
Assert-Api $merged 200 0 'CART-015 重复加购'
Assert-Equal $merged.Json.data.id $cart.Json.data.id 'CART-015 重复加购不应创建新项'
Assert-Equal $merged.Json.data.quantity 3 'CART-015 重复加购未合并数量'
$missingProduct = Invoke-Api 'POST' '/api/cart-items' @{ productId = 9999; quantity = 1 } $primary.Headers
Assert-Api $missingProduct 404 4042 'CART-016 不存在商品加购'
foreach ($quantity in @(0, -1)) {
    $response = Invoke-Api 'POST' '/api/cart-items' @{ productId = 1; quantity = $quantity } $primary.Headers
    Assert-Api $response 400 4000 "CART 加购非法数量 $quantity"
}
$overStock = Invoke-Api 'POST' '/api/cart-items' @{ productId = 1; quantity = 100 } $primary.Headers
Assert-Api $overStock 409 4003 'CART-020 加购超过库存'
$updated = Invoke-Api 'PUT' "/api/cart-items/$($cart.Json.data.id)" @{ quantity = 4 } $primary.Headers
Assert-Api $updated 200 0 'CART-021 修改购物车数量'
Assert-Equal $updated.Json.data.quantity 4 'CART-021 修改后数量错误'
$invalidUpdate = Invoke-Api 'PUT' "/api/cart-items/$($cart.Json.data.id)" @{ quantity = 0 } $primary.Headers
Assert-Api $invalidUpdate 400 4000 'CART-022 修改数量为 0'
$otherUpdate = Invoke-Api 'PUT' "/api/cart-items/$($cart.Json.data.id)" @{ quantity = 1 } $other.Headers
Assert-Api $otherUpdate 404 4044 'CART-025 修改他人购物车项'
$cartList = Invoke-Api 'GET' '/api/cart-items' $null $primary.Headers
Assert-Api $cartList 200 0 'CART-029 购物车列表'
Assert-Equal $cartList.Json.data.Count 1 'CART-029 购物车项数量错误'
$anonymousCart = Invoke-Api 'GET' '/api/cart-items'
Assert-Api $anonymousCart 401 4010 'CART-031 未登录购物车'
$deleted = Invoke-Api 'DELETE' "/api/cart-items/$($cart.Json.data.id)" $null $primary.Headers
Assert-Api $deleted 200 0 'CART-026 删除购物车项'
$afterDelete = Invoke-Api 'GET' '/api/cart-items' $null $primary.Headers
Assert-Equal $afterDelete.Json.data.Count 0 'CART-026 删除后购物车仍有数据'
$missingCart = Invoke-Api 'DELETE' "/api/cart-items/$($cart.Json.data.id)" $null $primary.Headers
Assert-Api $missingCart 404 4044 'CART-027 删除不存在购物车项'

Write-Host "PASS: P0 auth/catalog/cart regression completed for $($primary.Username)" -ForegroundColor Green
