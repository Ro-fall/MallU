(() => {
  const container = document.querySelector('.container');
  const grid = container?.querySelector('.grid');
  if (!container || !grid) return;
  const hero = document.createElement('header');
  hero.className = 'console-hero';
  hero.innerHTML = '<div><p class="eyebrow">MallU · local api workspace</p><h1>本地 API 测试控制台</h1><p>覆盖认证、交易回归、秒杀压测与安全校验。每个测试块保留原有请求和响应结果，方便手动定位问题。</p></div><div class="console-status"><strong><i class="status-dot"></i>控制台已就绪</strong><small>API 基址：/api · 结果在各测试块内呈现</small></div>';
  const nav = document.createElement('nav'); nav.className = 'quick-nav'; nav.setAttribute('aria-label', '测试分区');
  nav.innerHTML = '<a href="#auth-zone">认证与会话</a><a href="#trade-zone">交易回归</a><a href="#seckill-zone">秒杀压测</a><a href="#security-zone">安全校验</a>';
  const scenarios = document.createElement('section'); scenarios.className = 'scenario-bar'; scenarios.setAttribute('aria-label', '预置测试场景');
  scenarios.innerHTML = '<div class="scenario"><b>01 · 认证闭环</b><span>注册或登录后查看当前用户</span></div><div class="scenario"><b>02 · 下单回归</b><span>地址、购物车、订单与支付回调</span></div><div class="scenario"><b>03 · 秒杀并发</b><span>活动商品、下单与并发结果汇总</span></div><div class="scenario"><b>04 · 安全验证</b><span>幂等 Token 与请求签名正反例</span></div>';
  const tokens = [...container.querySelectorAll(':scope > .token-box')]; const bar = document.createElement('div'); bar.className = 'topbar'; tokens.forEach(token => bar.appendChild(token));
  const specs = [{id:'auth-zone',title:'认证与会话',note:'建立身份并确认会话状态',color:'#2563eb',cards:[0,1,2]},{id:'trade-zone',title:'交易回归',note:'商品、地址、购物车、订单与支付链路',color:'#0f766e',cards:[3,4,5,6,7,8,9,10,11]},{id:'seckill-zone',title:'秒杀压测',note:'活动、库存查询、异步下单与并发压测',color:'#b45309',cards:[12,13,14,15]},{id:'security-zone',title:'安全校验',note:'幂等令牌与请求签名的正反例',color:'#7c3aed',cards:[16]}];
  const cards = [...grid.querySelectorAll(':scope > .card')];
  container.prepend(scenarios); container.prepend(nav); container.prepend(bar); container.prepend(hero);
  specs.forEach(spec => { const section = document.createElement('section'); section.id=spec.id; section.className='section'; section.style.setProperty('--section-color',spec.color); section.innerHTML=`<div class="section-heading"><h2>${spec.title}</h2><span>${spec.note}</span></div>`; const sectionGrid=document.createElement('div'); sectionGrid.className='grid'; spec.cards.forEach(i => cards[i] && sectionGrid.appendChild(cards[i])); section.appendChild(sectionGrid); container.appendChild(section); });
  grid.remove();
})();