param([string]$BaseUrl = 'http://localhost:8080')
$ErrorActionPreference='Stop'
function Eq($a,$b,[string]$n){if($a -ne $b){throw "$n expected=[$b], actual=[$a]"}}
function Api([string]$m,[string]$p,$b=$null,$h=$null){$x=@{Method=$m;Uri="$BaseUrl$p";SkipHttpErrorCheck=$true};if($h){$x.Headers=$h};if($null -ne $b){$x.ContentType='application/json';$x.Body=$b|ConvertTo-Json -Compress};$r=Invoke-WebRequest @x;[pscustomobject]@{s=[int]$r.StatusCode;j=($r.Content|ConvertFrom-Json)}}
function Check($r,$s,$c,$n){Eq $r.s $s "$n HTTP";Eq $r.j.code $c "$n code"}

if(-not $env:MALLU_DB_PASSWORD){throw '请设置 MALLU_DB_PASSWORD。'}
& "$PSScriptRoot\reset-test-environment.ps1" -SkipRedisRebuildWait
$mysql=Get-Command mysql.exe -ErrorAction Stop;$old=$env:MYSQL_PWD
try{$env:MYSQL_PWD=$env:MALLU_DB_PASSWORD;$sql=(Resolve-Path "$PSScriptRoot\..\src\main\resources\sql\scenarios\catalog-edge.sql").Path.Replace('\','/');& $mysql.Source --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --default-character-set=utf8mb4 -e "SOURCE $sql";if($LASTEXITCODE -ne 0){throw '导入 catalog edge 场景失败。'}}finally{$env:MYSQL_PWD=$old}

$list=Api GET '/api/catalog/products?size=50';Check $list 200 0 'CAT-021 商品列表';Eq @($list.j.data.content|Where-Object{$_.name -eq '下架测试商品'}).Count 0 'CAT-021 下架商品不可见'
$search=Api GET '/api/catalog/products/search?keyword=下架测试';Check $search 200 0 'CAT-022 搜索';Eq $search.j.data.content.Count 0 'CAT-022 搜索不应返回下架商品'
$recommend=Api GET '/api/catalog/products/recommendations';Check $recommend 200 0 'CAT-023 推荐';Eq @($recommend.j.data|Where-Object{$_.name -eq '下架测试商品'}).Count 0 'CAT-023 推荐不应返回下架商品'
$offDetail=Api GET '/api/catalog/products/4';Check $offDetail 404 4042 'CAT-020 下架详情'
$zeroDetail=Api GET '/api/catalog/products/5';Check $zeroDetail 200 0 'CAT-026 零库存详情';Eq $zeroDetail.j.data.stock 0 'CAT-026 零库存值'
$u="catalog_edge_$(Get-Random -Minimum 100000 -Maximum 999999)";$reg=Api POST '/api/auth/register' @{username=$u;password='CatalogEdge_2026'};Check $reg 200 0 '注册';$h=@{Authorization="Bearer $($reg.j.data.token)"}
$offCart=Api POST '/api/cart-items' @{productId=4;quantity=1} $h;Check $offCart 404 4042 'CART-017 下架加购'
$zeroCart=Api POST '/api/cart-items' @{productId=5;quantity=1} $h;Check $zeroCart 409 4003 'CART-020 零库存加购'
Write-Host 'PASS: catalog edge regression completed.' -ForegroundColor Green
