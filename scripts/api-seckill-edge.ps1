param([string]$BaseUrl='http://localhost:8080')
$ErrorActionPreference='Stop'
function Eq($a,$b,$n){if($a -ne $b){throw "$n expected=[$b], actual=[$a]"}}
function Api($m,$p,$b=$null,$h=$null){$x=@{Method=$m;Uri="$BaseUrl$p";SkipHttpErrorCheck=$true};if($h){$x.Headers=$h};if($null -ne $b){$x.ContentType='application/json';$x.Body=$b|ConvertTo-Json -Compress};$r=Invoke-WebRequest @x;[pscustomobject]@{s=[int]$r.StatusCode;j=($r.Content|ConvertFrom-Json)}}
function Check($r,$s,$c,$n){Eq $r.s $s "$n HTTP";Eq $r.j.code $c "$n code"}
function User($p){$r=Api POST '/api/auth/register' @{username="$p`_$(Get-Random -Minimum 100000 -Maximum 999999)";password='SeckillEdge_2026'};Check $r 200 0 '注册';return @{Authorization="Bearer $($r.j.data.token)"}}

if(-not $env:MALLU_DB_PASSWORD){throw '请设置 MALLU_DB_PASSWORD。'}
& "$PSScriptRoot\reset-test-environment.ps1" -SkipRedisRebuildWait
$mysql=Get-Command mysql.exe -ErrorAction Stop;$old=$env:MYSQL_PWD
try{$env:MYSQL_PWD=$env:MALLU_DB_PASSWORD;$sql=(Resolve-Path "$PSScriptRoot\..\src\main\resources\sql\scenarios\seckill-edge.sql").Path.Replace('\','/');& $mysql.Source --protocol=TCP --host=127.0.0.1 --port=3306 --user=root --default-character-set=utf8mb4 -e "SOURCE $sql";if($LASTEXITCODE -ne 0){throw '导入秒杀场景失败。'}}finally{$env:MYSQL_PWD=$old}

$anonymous=Api GET '/api/seckill/activities/1/goods';Check $anonymous 401 4010 'SEC-009 未登录秒杀列表'
$a=User 'sec_edge';$b=User 'sec_other'
$none=Api GET '/api/seckill/goods/1/result' $null $a;Check $none 200 0 'SEC-046 未提交结果';Eq $none.j.data.status 'NONE' 'SEC-046 状态'
$otherNone=Api GET '/api/seckill/goods/1/result' $null $b;Check $otherNone 200 0 'SEC-050 他人结果';Eq $otherNone.j.data.status 'NONE' 'SEC-050 不泄露他人结果'
$missingActivity=Api GET '/api/seckill/activities/9999/goods' $null $a;Check $missingActivity 404 4049 'SEC-002 不存在活动'
$missingGoods=Api POST '/api/seckill/goods/9999/orders' @{addressId=1} $a;Check $missingGoods 404 4048 'SEC-008 不存在秒杀商品'
$missingAddress=Api POST '/api/seckill/goods/1/orders' @{} $a;Check $missingAddress 400 4000 'SEC-010 缺地址'
foreach($goodsId in @(2,3,4)){$inactive=Api POST "/api/seckill/goods/$goodsId/orders" @{addressId=1} $a;Check $inactive 409 4099 "SEC-005~007 活动状态商品 $goodsId"}
Write-Host 'PASS: seckill edge regression completed.' -ForegroundColor Green
