param(
    [string]$OutputPath = 'src/main/resources/static/test-report/index.html'
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$caseFile = Join-Path $root 'docs/test-cases.md'
$mappingFile = Join-Path $root 'docs/automation-mapping.md'

function Escape-Text([string]$value) {
    [System.Net.WebUtility]::HtmlEncode($value)
}

function Expand-Coverage([string]$cell) {
    $items = [System.Collections.Generic.List[string]]::new()
    $prefix = $null
    foreach ($part in ($cell -split '[、；,;\s]+')) {
        if ($part -match '^(AUTH|CAT|CART|ORDER|MKT|REDIS|SEC)-(\d{3})(?:~(\d{3}))?$') {
            $prefix = $Matches[1]
            $start = [int]$Matches[2]
            $end = if ($Matches[3]) { [int]$Matches[3] } else { $start }
        } elseif ($prefix -and $part -match '^(\d{3})(?:~(\d{3}))?$') {
            $start = [int]$Matches[1]
            $end = if ($Matches[2]) { [int]$Matches[2] } else { $start }
        } else {
            continue
        }
        foreach ($number in $start..$end) {
            $items.Add(('{0}-{1:D3}' -f $prefix, $number))
        }
    }
    return $items
}

$cases = [System.Collections.Generic.List[object]]::new()
foreach ($line in (Get-Content -Encoding utf8 $caseFile)) {
    if ($line -match '^\|\s*((?:AUTH|CAT|CART|ORDER|MKT|REDIS|SEC)-\d{3})\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|') {
        $cases.Add([ordered]@{ id=$Matches[1]; name=$Matches[2]; assertion=$Matches[3]; type=$Matches[4] })
    }
}

$sources = @{}
foreach ($line in (Get-Content -Encoding utf8 $mappingFile)) {
    if ($line -notmatch '^\|\s*`([^`]+)`\s*\|\s*(.*?)\s*\|\s*([^|]+)\|') { continue }
    $script = $Matches[1]
    $covered = Expand-Coverage $Matches[2]
    foreach ($id in $covered) {
        if (-not $sources.ContainsKey($id)) { $sources[$id] = [System.Collections.Generic.List[string]]::new() }
        $sources[$id].Add($script)
    }
}

foreach ($case in $cases) {
    $case.module = $case.id.Split('-')[0]
    $case.status = if ($sources.ContainsKey($case.id)) { 'passed' } else { 'pending' }
    $case.source = if ($sources.ContainsKey($case.id)) { $sources[$case.id] -join '、' } else { '' }
}

$moduleNames = [ordered]@{
    AUTH='用户与鉴权'; CAT='商品与目录'; CART='地址与购物车'; ORDER='订单';
    MKT='营销'; REDIS='Redis 可靠性'; SEC='秒杀与 RabbitMQ'
}
$summary = foreach ($key in $moduleNames.Keys) {
    $group = @($cases | Where-Object module -eq $key)
    [ordered]@{ key=$key; name=$moduleNames[$key]; total=$group.Count; passed=@($group | Where-Object status -eq 'passed').Count }
}
$payload = [ordered]@{
    generatedAt=(Get-Date).ToString('yyyy-MM-dd HH:mm:ss K')
    total=$cases.Count
    passed=@($cases | Where-Object status -eq 'passed').Count
    cases=$cases
    modules=$summary
} | ConvertTo-Json -Depth 6 -Compress

$template = @'
<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>MallU 自动化测试台账</title>
<style>
:root{--ink:#102a43;--muted:#627d98;--line:#d9e2ec;--paper:#f5f8fb;--card:#fff;--blue:#1d4ed8;--green:#047857;--green-bg:#d1fae5;--amber:#a16207;--amber-bg:#fef3c7;--navy:#0b1f33}*{box-sizing:border-box}body{margin:0;background:var(--paper);color:var(--ink);font:14px/1.5 "Microsoft YaHei","Segoe UI",sans-serif}.shell{max-width:1280px;margin:auto;padding:28px 24px 56px}.mast{background:var(--navy);color:#fff;padding:30px 34px;border-radius:16px;display:flex;gap:24px;justify-content:space-between;align-items:end}.mast h1{margin:0;font-size:28px;letter-spacing:.02em}.mast p{margin:7px 0 0;color:#b8c8d8}.stamp{text-align:right;color:#cbd5e1;font-size:12px}.stamp strong{display:block;color:#fff;font-size:13px;font-weight:600}.metrics{display:grid;grid-template-columns:1.3fr repeat(3,1fr);gap:12px;margin:18px 0}.metric{padding:18px 20px;background:var(--card);border:1px solid var(--line);border-radius:12px}.metric b{display:block;font-size:27px;line-height:1.1;color:var(--blue)}.metric span{display:block;margin-top:6px;color:var(--muted)}.metric.primary{background:#eff6ff;border-color:#bfdbfe}.coverage{font-size:13px;color:#2563eb;margin-top:9px}.module-grid{display:grid;grid-template-columns:repeat(7,1fr);gap:8px;margin:18px 0}.module{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px}.module b{font-size:13px}.module span{display:block;margin-top:8px;font-variant-numeric:tabular-nums}.bar{height:5px;background:#e5edf5;border-radius:10px;overflow:hidden;margin-top:10px}.bar i{display:block;height:100%;background:var(--green)}.panel{background:var(--card);border:1px solid var(--line);border-radius:14px;overflow:hidden}.tools{padding:16px;display:flex;gap:12px;align-items:center;border-bottom:1px solid var(--line);flex-wrap:wrap}.tools input{min-width:240px;flex:1;border:1px solid #b8c7d6;border-radius:8px;padding:9px 11px;font:inherit}.chips{display:flex;gap:6px;flex-wrap:wrap}.chip{border:1px solid #b8c7d6;background:#fff;padding:7px 10px;border-radius:99px;color:var(--ink);cursor:pointer}.chip.active{background:var(--blue);border-color:var(--blue);color:#fff}.table-wrap{overflow:auto;max-height:640px}table{border-collapse:collapse;width:100%;min-width:840px}th{position:sticky;top:0;background:#edf4fa;color:#334e68;font-size:12px;text-align:left;padding:11px 14px;z-index:1}td{padding:11px 14px;border-top:1px solid #edf1f5;vertical-align:top}td.id{font-family:Consolas,monospace;font-size:12px;font-weight:700;color:#244568}.status{display:inline-block;border-radius:99px;padding:3px 8px;font-size:12px;font-weight:700}.passed{background:var(--green-bg);color:var(--green)}.pending{background:var(--amber-bg);color:var(--amber)}.source{font:12px Consolas,monospace;color:var(--muted)}.foot{color:var(--muted);font-size:12px;margin:14px 2px 0}@media(max-width:900px){.mast{align-items:start;flex-direction:column}.stamp{text-align:left}.metrics{grid-template-columns:repeat(2,1fr)}.metrics .primary{grid-column:span 2}.module-grid{grid-template-columns:repeat(2,1fr)}}
</style></head><body><main class="shell"><header class="mast"><div><h1>MallU 自动化测试台账</h1><p>用例编号、脚本来源与最近记录结果的静态快照</p></div><div class="stamp"><strong id="timestamp"></strong>结果来自已记录的实跑通过脚本；未覆盖不代表执行失败。</div></header><section class="metrics"><article class="metric primary"><b id="coverage"></b><span>设计用例自动化覆盖</span><div class="coverage" id="coverageText"></div></article><article class="metric"><b id="passed"></b><span>最近记录为通过</span></article><article class="metric"><b id="pending"></b><span>待自动化</span></article><article class="metric"><b id="modules"></b><span>业务模块</span></article></section><section class="module-grid" id="moduleGrid"></section><section class="panel"><div class="tools"><input id="search" placeholder="搜索编号、用例名称、脚本…" aria-label="搜索用例"><div class="chips" id="chips"></div></div><div class="table-wrap"><table><thead><tr><th>编号</th><th>用例</th><th>关键断言</th><th>类型</th><th>自动化结果</th><th>脚本/测试来源</th></tr></thead><tbody id="rows"></tbody></table></div></section><p class="foot">数据来源：<code>docs/test-cases.md</code> 与 <code>docs/automation-mapping.md</code>。页面由 <code>scripts/export-test-report.ps1</code> 生成。</p></main><script>const report=__REPORT_DATA__;let status='all',module='all';const esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));const label={AUTH:'用户与鉴权',CAT:'商品与目录',CART:'地址与购物车',ORDER:'订单',MKT:'营销',REDIS:'Redis 可靠性',SEC:'秒杀与 RabbitMQ'};document.querySelector('#timestamp').textContent='快照生成：'+report.generatedAt;document.querySelector('#coverage').textContent=report.passed+' / '+report.total;document.querySelector('#coverageText').textContent='覆盖率 '+(report.passed/report.total*100).toFixed(1)+'%';document.querySelector('#passed').textContent=report.passed;document.querySelector('#pending').textContent=report.total-report.passed;document.querySelector('#modules').textContent=report.modules.length;document.querySelector('#moduleGrid').innerHTML=report.modules.map(m=>`<article class="module"><b>${m.key}</b><span>${m.passed}/${m.total}</span><div class="bar"><i style="width:${m.passed/m.total*100}%"></i></div></article>`).join('');const chips=[['all','全部'],['passed','已通过'],['pending','待自动化'],...report.modules.map(m=>[m.key,label[m.key]])];document.querySelector('#chips').innerHTML=chips.map(([k,n])=>`<button class="chip ${k==='all'?'active':''}" data-k="${k}">${n}</button>`).join('');function render(){const q=document.querySelector('#search').value.trim().toLowerCase();const visible=report.cases.filter(c=>(status==='all'||c.status===status)&&(module==='all'||c.module===module)&&(!q||[c.id,c.name,c.assertion,c.source].join(' ').toLowerCase().includes(q)));document.querySelector('#rows').innerHTML=visible.map(c=>`<tr><td class="id">${c.id}</td><td>${esc(c.name)}</td><td>${esc(c.assertion)}</td><td>${esc(c.type)}</td><td><span class="status ${c.status}">${c.status==='passed'?'已通过':'待自动化'}</span></td><td class="source">${esc(c.source||'—')}</td></tr>`).join('')||'<tr><td colspan="6">没有匹配的用例。</td></tr>'}document.querySelector('#search').addEventListener('input',render);document.querySelector('#chips').addEventListener('click',e=>{if(!e.target.dataset.k)return;const k=e.target.dataset.k;if(k==='all'||k==='passed'||k==='pending'){status=k;module='all'}else{status='all';module=k}document.querySelectorAll('.chip').forEach(x=>x.classList.toggle('active',x===e.target));render()});render();</script></body></html>
'@

$output = Join-Path $root $OutputPath
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $output) | Out-Null
$template.Replace('__REPORT_DATA__', $payload) | Set-Content -Encoding utf8 $output
Write-Host "Report generated: $output" -ForegroundColor Green
Write-Host "Coverage: $(@($cases | Where-Object status -eq 'passed').Count)/$($cases.Count)" -ForegroundColor Green
