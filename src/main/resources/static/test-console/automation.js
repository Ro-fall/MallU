(() => {
  let testSession = JSON.parse(localStorage.getItem('testConsoleSession') || 'null');
  const host = document.querySelector('.container');
  if (!host) return;
  const panel = document.createElement('section');
  panel.className = 'automation-panel';
  panel.innerHTML = `
    <div class="automation-copy"><p class="eyebrow">自动化工作区</p><h2>一键准备测试数据</h2><p>自动创建带 <code>tc_</code> 前缀的账号和默认收货地址，并填入当前页面。清理只会删除本控制台创建的测试账号及其级联数据。</p></div>
    <div class="automation-actions"><button id="prepareTestSession">准备测试会话</button><button id="cleanupTestSession" class="danger">清理测试数据</button></div>
    <pre id="automationResult" class="automation-result">尚未准备测试会话</pre>`;
  host.prepend(panel);
  const result = panel.querySelector('#automationResult');
  const show = (data, error = false) => { result.textContent = typeof data === 'string' ? data : JSON.stringify(data, null, 2); result.classList.toggle('error', error); };
  const field = (id, value) => { const el = document.getElementById(id); if (el && value != null) el.value = value; };
  const fillSession = async (session) => {
    token = session.token; localStorage.setItem('token', token); localStorage.setItem('testConsoleSession', JSON.stringify(session));
    document.getElementById('currentToken').textContent = token;
    field('regUsername', session.username); field('regPassword', session.password); field('regPhone', session.phone); field('regEmail', session.email);
    field('loginUsername', session.username); field('loginPassword', session.password);
    ['addrPhone'].forEach(id => field(id, session.phone)); field('addrName', '自动化测试用户'); field('addrProvince', '上海市'); field('addrCity', '上海市'); field('addrDistrict', '浦东新区'); field('addrDetail', '测试大道 100 号');
    ['orderAddressId', 'seckillAddressId', 'stressAddressId'].forEach(id => field(id, session.addressId));
    try {
      const products = (await api.get('/products')).data.data || []; const product = products.list ? products.list[0] : products[0];
      if (product) field('cartProductId', product.id);
      const activities = (await api.get('/seckill/activities')).data.data || []; const activity = activities[0];
      if (activity) { field('seckillActivityId', activity.id); const goods = (await api.get('/seckill/activities/' + activity.id + '/goods')).data.data || []; const first = goods[0]; if (first) ['seckillGoodsId', 'stressSeckillGoodsId'].forEach(id => field(id, first.id)); }
    } catch (error) { console.warn('预置商品或秒杀数据失败', error); }
  };
  document.getElementById('prepareTestSession').addEventListener('click', async () => {
    try { show('正在创建测试账号、地址并预置表单…'); const res = await fetch('/api/test-support/sessions', {method:'POST'}); const body = await res.json(); if (!res.ok || body.code !== 200) throw new Error(body.message || '创建失败'); testSession = body.data; await fillSession(testSession); show({message:'测试会话已准备完成，表单中的账号、地址和可用商品已自动填入。', session:{username:testSession.username, password:testSession.password, addressId:testSession.addressId}}); } catch (error) { show(error.message || '创建测试会话失败', true); }
  });
  document.getElementById('cleanupTestSession').addEventListener('click', async () => {
    if (!testSession) { show('没有可清理的控制台测试会话。', true); return; }
    try { show('正在删除本控制台创建的测试数据…'); const url = '/api/test-support/sessions/' + encodeURIComponent(testSession.userId) + '?username=' + encodeURIComponent(testSession.username); const res = await fetch(url, {method:'DELETE'}); const body = await res.json(); if (!res.ok || body.code !== 200) throw new Error(body.message || '清理失败'); localStorage.removeItem('testConsoleSession'); localStorage.removeItem('token'); token = ''; document.getElementById('currentToken').textContent = '未登录'; testSession = null; show('测试账号及其关联的地址、购物车、订单和支付记录已清理。'); } catch (error) { show(error.message || '清理测试数据失败', true); }
  });
  if (testSession) { fillSession(testSession).then(() => show('已恢复本浏览器保存的测试会话。')); }
})();