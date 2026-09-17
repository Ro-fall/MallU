param([string]$BaseUrl='http://localhost:8080')
$ErrorActionPreference='Stop'
function Eq($a,$b,$n){if($a -ne $b){throw "$n expected=[$b], actual=[$a]"}}
function Api($m,$p,$b=$null,$h=$null){$x=@{Method=$m;Uri="$BaseUrl$p";SkipHttpErrorCheck=$true};if($h){$x.Headers=$h};if($null -ne $b){$x.ContentType='application/json';$x.Body=$b|ConvertTo-Json -Compress};$r=Invoke-WebRequest @x;[pscustomobject]@{s=[int]$r.StatusCode;j=($r.Content|ConvertFrom-Json)}}
function Check($r,$s,$c,$n){Eq $r.s $s "$n HTTP";Eq $r.j.code $c "$n code"}
if(-not $env:MALLU_DB_PASSWORD){throw '请设置 MALLU_DB_PASSWORD。'}
& "$PSScriptRoot\reset-test-environment.ps1"
$success=0
for($i=1;$i -le 10;$i++){
  $u="sec_stock_$i`_$(Get-Random -Minimum 100000 -Maximum 999999)";$reg=Api POST '/api/auth/register' @{username=$u;password='SecStock_2026'};Check $reg 200 0 "用户 $i 注册";$h=@{Authorization="Bearer $($reg.j.data.token)"}
  $addr=Api POST '/api/addresses' @{receiverName="Stock $i";phone='13800138000';province='上海市';city='上海市';district='浦东新区';detail='秒杀库存边界';defaultAddress=$true} $h;Check $addr 200 0 "用户 $i 地址"
  $submit=Api POST '/api/seckill/goods/1/orders' @{addressId=$addr.j.data.id} $h;Check $submit 200 0 "SEC-022 用户 $i 提交";Eq $submit.j.data.status 'PENDING' "SEC-012 用户 $i 排队"
  for($try=0;$try -lt 30;$try++){Start-Sleep -Milliseconds 200;$result=Api GET '/api/seckill/goods/1/result' $null $h;if($result.j.data.status -ne 'PENDING'){break}}
  Eq $result.j.data.status 'SUCCESS' "SEC-022 用户 $i 消费成功";$success++
}
Eq $success 10 'SEC-022 十件顺序抢购'
$u="sec_stock_11_$(Get-Random -Minimum 100000 -Maximum 999999)";$reg=Api POST '/api/auth/register' @{username=$u;password='SecStock_2026'};Check $reg 200 0 '第十一用户注册';$h=@{Authorization="Bearer $($reg.j.data.token)"};$addr=Api POST '/api/addresses' @{receiverName='Stock 11';phone='13800138000';province='上海市';city='上海市';district='浦东新区';detail='秒杀库存边界';defaultAddress=$true} $h
$soldOut=Api POST '/api/seckill/goods/1/orders' @{addressId=$addr.j.data.id} $h;Check $soldOut 409 4003 'SEC-021/023 库存耗尽'
Write-Host 'PASS: ten sequential seckill orders succeeded; the eleventh was rejected.' -ForegroundColor Green
