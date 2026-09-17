param([string]$BaseUrl='http://localhost:8080')
$ErrorActionPreference='Stop'
function Eq($a,$b,$n){if($a -ne $b){throw "$n expected=[$b], actual=[$a]"}}
function Api($m,$p,$b=$null,$h=$null){$x=@{Method=$m;Uri="$BaseUrl$p";SkipHttpErrorCheck=$true};if($h){$x.Headers=$h};if($null -ne $b){$x.ContentType='application/json';$x.Body=$b|ConvertTo-Json -Compress};$r=Invoke-WebRequest @x;[pscustomobject]@{s=[int]$r.StatusCode;j=($r.Content|ConvertFrom-Json)}}
function Check($r,$s,$c,$n){Eq $r.s $s "$n HTTP";Eq $r.j.code $c "$n code"}
function Wait-Result($h){for($i=0;$i -lt 30;$i++){Start-Sleep -Milliseconds 200;$r=Api GET '/api/seckill/goods/1/result' $null $h;if($r.j.data.status -ne 'PENDING'){return $r}};throw '秒杀结果轮询超时'}
$u="sec_life_$(Get-Random -Minimum 100000 -Maximum 999999)";$r=Api POST '/api/auth/register' @{username=$u;password='SecLife_2026'};Check $r 200 0 '注册';$h=@{Authorization="Bearer $($r.j.data.token)"}
$a=Api POST '/api/addresses' @{receiverName='Sec Life';phone='13800138000';province='上海市';city='上海市';district='浦东新区';detail='生命周期测试';defaultAddress=$true} $h;Check $a 200 0 '地址'
$productBefore=(Api GET '/api/catalog/products/1').j.data.stock;$secBefore=(Api GET '/api/seckill/activities/1/goods' $null $h).j.data[0].stock
$submit=Api POST '/api/seckill/goods/1/orders' @{addressId=$a.j.data.id} $h;Check $submit 200 0 'SEC-012 提交';$done=Wait-Result $h;Eq $done.j.data.status 'SUCCESS' 'SEC-014 消费成功'
$detail=Api GET "/api/orders/$($done.j.data.orderId)" $null $h;Check $detail 200 0 'SEC-015 订单详情';Eq $detail.j.data.items[0].productPrice 2999 'SEC-016 秒杀价快照'
Eq (Api GET '/api/catalog/products/1').j.data.stock ($productBefore-1) 'SEC-017 普通库存扣减';Eq (Api GET '/api/seckill/activities/1/goods' $null $h).j.data[0].stock ($secBefore-1) 'SEC-018 秒杀库存扣减'
$cancel=Api POST "/api/orders/$($done.j.data.orderId)/cancel" $null $h;Check $cancel 200 0 'SEC-051 取消秒杀订单';Eq $cancel.j.data.status 'CANCELLED' 'SEC-051 状态';Eq (Api GET '/api/catalog/products/1').j.data.stock $productBefore 'SEC-052 普通库存回补';Eq (Api GET '/api/seckill/activities/1/goods' $null $h).j.data[0].stock $secBefore 'SEC-053 秒杀库存回补'
$again=Api POST '/api/seckill/goods/1/orders' @{addressId=$a.j.data.id} $h;Check $again 200 0 'SEC-054 取消后可再次秒杀';$againDone=Wait-Result $h;Eq $againDone.j.data.status 'SUCCESS' 'SEC-054 再次消费';Api POST "/api/orders/$($againDone.j.data.orderId)/cancel" $null $h | Out-Null
Write-Host 'PASS: seckill order lifecycle, inventory compensation, and eligibility release.' -ForegroundColor Green
