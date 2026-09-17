param([string]$BaseUrl = 'http://localhost:8080', [int]$Users = 20)

$ErrorActionPreference='Stop'
function Assert-Equal($actual,$expected,[string]$message){if($actual -ne $expected){throw "$message. expected=[$expected], actual=[$actual]"}}
function Invoke-Json([string]$method,[string]$path,$body,$headers){$args=@{Method=$method;Uri="$BaseUrl$path";ErrorAction='Stop'};if($headers){$args.Headers=$headers};if($null -ne $body){$args.ContentType='application/json';$args.Body=($body|ConvertTo-Json -Depth 5)};Invoke-RestMethod @args}

$prepared=for($i=1;$i -le $Users;$i++){
  $suffix="${i}_$(Get-Random -Minimum 100000 -Maximum 999999)"
  $register=Invoke-Json 'POST' '/api/auth/register' @{username="conc_$suffix";password='Concurrency_2026'} $null
  $headers=@{Authorization="Bearer $($register.data.token)"}
  $address=Invoke-Json 'POST' '/api/addresses' @{receiverName='Concurrent';phone='13800138000';province='上海市';city='上海市';district='浦东新区';detail='并发秒杀测试地址';defaultAddress=$true} $headers
  [PSCustomObject]@{Index=$i;Token=$register.data.token;AddressId=$address.data.id}
}
$before=(Invoke-Json 'GET' '/api/seckill/activities/1/goods' $null @{Authorization="Bearer $($prepared[0].Token)"}).data[0].stock
$prepared | ForEach-Object -Parallel {
  try { Invoke-RestMethod -Method Post -Uri "$($using:BaseUrl)/api/seckill/goods/1/orders" -Headers @{Authorization="Bearer $($_.Token)"} -ContentType 'application/json' -Body (@{addressId=$_.AddressId}|ConvertTo-Json) | Out-Null; [PSCustomObject]@{Index=$_.Index;Submitted=$true} }
  catch { [PSCustomObject]@{Index=$_.Index;Submitted=$false} }
} -ThrottleLimit $Users | Out-Null
Start-Sleep -Seconds 3
$success=0
foreach($user in $prepared){$result=Invoke-Json 'GET' '/api/seckill/goods/1/result' $null @{Authorization="Bearer $($user.Token)"};if($result.data.status -eq 'SUCCESS'){$success++}}
$after=(Invoke-Json 'GET' '/api/seckill/activities/1/goods' $null @{Authorization="Bearer $($prepared[0].Token)"}).data[0].stock
Assert-Equal $success $before '成功订单数与初始秒杀库存不一致'
Assert-Equal $after 0 '秒杀库存未耗尽或出现异常'
if($success -gt $before){throw '发生超卖'}
Write-Host "PASS: $Users concurrent users produced $success successful seckill orders; no oversell." -ForegroundColor Green
