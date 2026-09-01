<script setup>
import { computed, onMounted, ref } from 'vue'

const apiBase = '/api'
const activeView = ref('home')
const products = ref([])
const cart = ref([])
const orders = ref([])
const addresses = ref([])
const coupons = ref([])
const activities = ref([])
const seckillGoods = ref([])
const selectedCouponId = ref('')
const selectedAddressId = ref('')
const loading = ref(false)
const notice = ref('')
const noticeType = ref('success')
const authOpen = ref(false)
const authMode = ref('login')
const token = ref(localStorage.getItem('mallu_token') || '')
const user = ref(JSON.parse(localStorage.getItem('mallu_user') || 'null'))
const authForm = ref({ username: '', password: '', phone: '', email: '' })
const addressForm = ref({ receiverName: '', phone: '', province: '浙江省', city: '杭州市', district: '西湖区', detailAddress: '', isDefault: 1 })

const cartCount = computed(() => cart.value.reduce((sum, item) => sum + item.quantity, 0))
const cartTotal = computed(() => cart.value.reduce((sum, item) => sum + Number(item.totalPrice || item.productPrice * item.quantity), 0))
const hasLogin = computed(() => Boolean(token.value))

function show(message, type = 'success') {
  notice.value = message
  noticeType.value = type
  window.setTimeout(() => { if (notice.value === message) notice.value = '' }, 3000)
}

async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }
  if (token.value) headers.Authorization = `Bearer ${token.value}`
  const response = await fetch(`${apiBase}${path}`, { ...options, headers })
  const payload = await response.json().catch(() => ({}))
  if (!response.ok || payload.code !== 200) throw new Error(payload.message || '请求失败，请稍后重试')
  return payload.data
}

async function signedRequest(path, method, body = null, params = {}) {
  const bodyText = body ? JSON.stringify(body) : ''
  const signData = await request('/sign/generate', {
    method: 'POST', body: JSON.stringify({ params, body: bodyText }),
  })
  const query = new URLSearchParams(params).toString()
  const headers = {
    'X-Sign': signData.sign,
    'X-Timestamp': String(signData.timestamp),
    'X-Nonce': signData.nonce,
  }
  if (method === 'POST' || method === 'PUT') {
    try { headers['Idempotency-Key'] = await request('/idempotent/token') } catch (_) { /* 秒杀接口不要求幂等令牌 */ }
  }
  return request(`${path}${query ? `?${query}` : ''}`, {
    method,
    headers,
    body: bodyText || undefined,
  })
}

async function loadProducts() { products.value = (await request('/products?page=1&size=12')).records || (await request('/products?page=1&size=12')).list || [] }
async function loadCart() { if (hasLogin.value) cart.value = await request('/carts') }
async function loadAddresses() {
  if (!hasLogin.value) return
  addresses.value = await request('/addresses')
  if (!selectedAddressId.value && addresses.value[0]) selectedAddressId.value = String(addresses.value.find(a => a.isDefault === 1)?.id || addresses.value[0].id)
}
async function loadCoupons() { if (hasLogin.value) coupons.value = await request('/coupons/my/available') }
async function loadOrders() { if (hasLogin.value) { const data = await request('/orders?page=1&size=20'); orders.value = data.records || data.list || [] } }

async function addCart(product) {
  if (!ensureLogin()) return
  try { await request('/carts', { method: 'POST', body: JSON.stringify({ productId: product.id, quantity: 1 }) }); await loadCart(); show('已加入购物车') } catch (e) { show(e.message, 'error') }
}
async function updateCart(item, quantity) {
  if (quantity < 1) return removeCart(item)
  try { await request(`/carts/${item.id}`, { method: 'PUT', body: JSON.stringify({ quantity }) }); await loadCart() } catch (e) { show(e.message, 'error') }
}
async function removeCart(item) { try { await request(`/carts/${item.id}`, { method: 'DELETE' }); await loadCart() } catch (e) { show(e.message, 'error') } }

function ensureLogin() { if (!hasLogin.value) { authOpen.value = true; return false } return true }
async function submitAuth() {
  try {
    const endpoint = authMode.value === 'login' ? '/users/login' : '/users/register'
    const data = await request(endpoint, { method: 'POST', body: JSON.stringify(authForm.value) })
    token.value = data.token; user.value = data.user || { username: authForm.value.username }
    localStorage.setItem('mallu_token', token.value); localStorage.setItem('mallu_user', JSON.stringify(user.value))
    authOpen.value = false; show('登录成功，开始购物吧'); await refreshPrivateData()
  } catch (e) { show(e.message, 'error') }
}
function logout() { token.value = ''; user.value = null; cart.value = []; orders.value = []; localStorage.removeItem('mallu_token'); localStorage.removeItem('mallu_user'); show('已退出登录') }
async function refreshPrivateData() { await Promise.all([loadCart(), loadAddresses(), loadCoupons(), loadOrders(), loadActivities()]) }

async function saveAddress() {
  try { await request('/addresses', { method: 'POST', body: JSON.stringify(addressForm.value) }); addressForm.value.detailAddress = ''; await loadAddresses(); show('地址已保存') } catch (e) { show(e.message, 'error') }
}
async function createOrder() {
  if (!ensureLogin()) return
  if (!selectedAddressId.value) return show('请先添加并选择收货地址', 'error')
  if (!cart.value.length) return show('购物车为空', 'error')
  try {
    const body = { addressId: Number(selectedAddressId.value), ...(selectedCouponId.value ? { couponId: Number(selectedCouponId.value) } : {}) }
    const order = await signedRequest('/orders', 'POST', body)
    show(`订单 ${order.orderNo} 创建成功`); selectedCouponId.value = ''; await Promise.all([loadCart(), loadOrders(), loadCoupons()]); activeView.value = 'orders'
  } catch (e) { show(e.message, 'error') }
}
async function orderAction(order, action) {
  try {
    if (action === 'pay') await signedRequest(`/orders/${order.id}/pay`, 'PUT')
    else await request(`/orders/${order.id}/${action}`, { method: 'PUT' })
    show(action === 'cancel' ? '订单已取消，库存和优惠券已回滚' : '订单状态已更新'); await Promise.all([loadOrders(), loadCart(), loadCoupons()])
  } catch (e) { show(e.message, 'error') }
}
function statusText(status) { return ({ 0: '待支付', 1: '已支付', 2: '已取消', 3: '已完成' })[status] || '处理中' }

async function loadActivities() {
  if (!hasLogin.value) return
  try {
    activities.value = await request('/seckill/activities')
    if (activities.value[0]) seckillGoods.value = await request(`/seckill/activities/${activities.value[0].id}/goods`)
  } catch (e) { /* 秒杀模块不影响常规业务体验 */ }
}
async function seckill(item) {
  if (!selectedAddressId.value) return show('请先在购物车页添加收货地址', 'error')
  try {
    const result = await signedRequest(`/seckill/goods/${item.id}/seckill`, 'POST', null, { addressId: String(selectedAddressId.value) })
    show(result.status === 0 ? '已进入秒杀队列，正在创建订单' : '秒杀成功'); window.setTimeout(async () => { await loadOrders(); await loadActivities() }, 1200)
  } catch (e) { show(e.message, 'error') }
}

onMounted(async () => { loading.value = true; try { await loadProducts(); if (hasLogin.value) await refreshPrivateData() } catch (e) { show('后端服务未连接：请先启动 MallU', 'error') } finally { loading.value = false } })
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <button class="brand" @click="activeView = 'home'"><span>MU</span>MallU</button>
      <nav>
        <button :class="{ active: activeView === 'home' }" @click="activeView = 'home'">商品</button>
        <button :class="{ active: activeView === 'cart' }" @click="activeView = 'cart'">购物车 <em v-if="cartCount">{{ cartCount }}</em></button>
        <button :class="{ active: activeView === 'orders' }" @click="activeView = 'orders'; loadOrders()">订单</button>
        <button :class="{ active: activeView === 'seckill' }" @click="activeView = 'seckill'; loadActivities()">限时秒杀</button>
      </nav>
      <div class="account">
        <span v-if="user">你好，{{ user.username }}</span>
        <button v-if="user" class="text-button" @click="logout">退出</button>
        <button v-else class="primary small" @click="authOpen = true">登录 / 注册</button>
      </div>
    </header>

    <main>
      <section v-if="activeView === 'home'" class="home">
        <div class="hero"><p>为可靠交易而设计</p><h1>把每一次下单<br>都走得更稳。</h1><span>库存条件更新 · 支付回调幂等 · 秒杀异步落库</span></div>
        <div class="section-title"><div><p class="eyebrow">商品精选</p><h2>今天想买点什么？</h2></div><span>{{ products.length }} 件在售商品</span></div>
        <div v-if="loading" class="empty">正在加载商品…</div>
        <div v-else class="product-grid">
          <article v-for="product in products" :key="product.id" class="product-card">
            <div class="product-visual"><span>{{ product.name?.slice(0, 1) }}</span></div>
            <div class="product-info"><p>{{ product.name }}</p><small>{{ product.description || '品质好物，现货发售' }}</small><div><strong>¥{{ Number(product.price).toFixed(2) }}</strong><button @click="addCart(product)">加入购物车</button></div></div>
          </article>
        </div>
      </section>

      <section v-else-if="activeView === 'cart'" class="workspace two-columns">
        <div><div class="section-title"><div><p class="eyebrow">购物车</p><h2>确认你的订单</h2></div></div>
          <div v-if="!hasLogin" class="empty">登录后可以保存购物车和地址。<button class="link" @click="authOpen = true">去登录</button></div>
          <div v-else-if="!cart.length" class="empty">购物车还是空的。<button class="link" @click="activeView = 'home'">去逛商品</button></div>
          <div v-else class="cart-list"><article v-for="item in cart" :key="item.id" class="cart-item"><div class="mini-visual">{{ item.productName?.slice(0, 1) }}</div><div class="grow"><b>{{ item.productName }}</b><small>¥{{ Number(item.productPrice).toFixed(2) }}</small></div><div class="stepper"><button @click="updateCart(item, item.quantity - 1)">−</button><span>{{ item.quantity }}</span><button @click="updateCart(item, item.quantity + 1)">+</button></div><strong>¥{{ Number(item.totalPrice || item.productPrice * item.quantity).toFixed(2) }}</strong><button class="remove" @click="removeCart(item)">×</button></article></div>
        </div>
        <aside class="checkout-card"><p class="eyebrow">结算</p><h3>订单信息</h3><label>收货地址<select v-model="selectedAddressId"><option value="">请选择地址</option><option v-for="address in addresses" :key="address.id" :value="String(address.id)">{{ address.receiverName }} · {{ address.detailAddress }}</option></select></label><label>优惠券<select v-model="selectedCouponId"><option value="">暂不使用优惠券</option><option v-for="coupon in coupons" :key="coupon.id" :value="String(coupon.id)">{{ coupon.couponName }}</option></select></label><div class="total"><span>商品合计</span><strong>¥{{ cartTotal.toFixed(2) }}</strong></div><button class="primary wide" @click="createOrder">提交订单</button><details><summary>新增收货地址</summary><div class="address-form"><input v-model="addressForm.receiverName" placeholder="收货人姓名"><input v-model="addressForm.phone" placeholder="手机号"><input v-model="addressForm.detailAddress" placeholder="详细地址"><button @click="saveAddress">保存地址</button></div></details></aside>
      </section>

      <section v-else-if="activeView === 'orders'" class="workspace"><div class="section-title"><div><p class="eyebrow">我的订单</p><h2>订单状态清晰可追踪</h2></div><button class="secondary" @click="loadOrders">刷新</button></div><div v-if="!hasLogin" class="empty">请先登录查看订单。</div><div v-else-if="!orders.length" class="empty">还没有订单，去挑选商品吧。</div><div v-else class="order-list"><article v-for="order in orders" :key="order.id" class="order-card"><div><span class="order-no">{{ order.orderNo }}</span><span :class="['status', `s${order.status}`]">{{ statusText(order.status) }}</span></div><div class="order-products"><span v-for="item in order.items" :key="item.id">{{ item.productName }} × {{ item.quantity }}</span></div><div class="order-bottom"><small>{{ order.createdAt?.replace('T', ' ') }}</small><strong>实付 ¥{{ Number(order.payAmount).toFixed(2) }}</strong><div><button v-if="order.status === 0" class="secondary" @click="orderAction(order, 'cancel')">取消订单</button><button v-if="order.status === 0" class="primary small" @click="orderAction(order, 'pay')">模拟支付</button><button v-if="order.status === 1" class="primary small" @click="orderAction(order, 'complete')">确认完成</button></div></div></article></div></section>

      <section v-else class="workspace"><div class="section-title"><div><p class="eyebrow">FLASH SALE</p><h2>限时秒杀</h2></div><span class="pulse">● 正在进行</span></div><div v-if="!hasLogin" class="empty">登录后可参与秒杀。</div><div v-else-if="!seckillGoods.length" class="empty">暂时没有进行中的秒杀活动。</div><div v-else class="seckill-grid"><article v-for="item in seckillGoods" :key="item.id" class="seckill-card"><div class="sale-label">限时特惠</div><h3>{{ item.productName }}</h3><p>活动库存 <b>{{ item.remainingStock }}</b> 件</p><div><strong>¥{{ Number(item.seckillPrice).toFixed(2) }}</strong><del>¥{{ Number(item.price || item.seckillPrice).toFixed(2) }}</del></div><button class="primary wide" @click="seckill(item)">立即抢购</button></article></div></section>
    </main>

    <div v-if="notice" :class="['toast', noticeType]">{{ notice }}</div>
    <div v-if="authOpen" class="modal-mask" @click.self="authOpen = false"><section class="auth-modal"><button class="close" @click="authOpen = false">×</button><p class="eyebrow">MALLU ACCOUNT</p><h2>{{ authMode === 'login' ? '欢迎回来' : '创建账号' }}</h2><input v-model="authForm.username" placeholder="用户名（4-32 位）"><input v-model="authForm.password" type="password" placeholder="密码（6-32 位）"><template v-if="authMode === 'register'"><input v-model="authForm.phone" placeholder="手机号（可选）"><input v-model="authForm.email" placeholder="邮箱（可选）"></template><button class="primary wide" @click="submitAuth">{{ authMode === 'login' ? '登录' : '注册并登录' }}</button><button class="link" @click="authMode = authMode === 'login' ? 'register' : 'login'">{{ authMode === 'login' ? '没有账号？去注册' : '已有账号？去登录' }}</button></section></div>
  </div>
</template>
