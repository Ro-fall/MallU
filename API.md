# MallU 优选交易平台 API 文档

**版本**: 1.0.0

本文档包含 MallU 项目核心接口说明。完整接口文档可通过 Swagger UI 查看：`http://localhost:8080/swagger-ui/index.html`

也可运行项目后执行 `python scripts/generate_api_doc.py` 自动生成完整 Markdown 文档。

---

## 认证方式

大部分接口需要 JWT Token。登录后获取 token，在请求头中携带：

```http
Authorization: Bearer {token}
```

---

## 用户模块

### 用户注册

- **请求方式**: `POST`
- **请求路径**: `/api/users/register`
- **说明**: 新用户注册，注册成功后返回 JWT token

**请求参数**:

```json
{
  "username": "test",
  "password": "123456",
  "phone": "13800138000",
  "email": "test@example.com"
}
```

**响应示例**:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGci..."
  }
}
```

### 用户登录

- **请求方式**: `POST`
- **请求路径**: `/api/users/login`

**请求参数**:

```json
{
  "username": "test",
  "password": "123456"
}
```

### 获取当前用户

- **请求方式**: `GET`
- **请求路径**: `/api/users/me`
- **说明**: 需要 JWT

---

## 地址模块

### 添加地址

- **请求方式**: `POST`
- **请求路径**: `/api/addresses`
- **说明**: 需要 JWT

```json
{
  "receiverName": "张三",
  "phone": "13800138000",
  "province": "广东",
  "city": "深圳",
  "district": "南山",
  "detailAddress": "科技园",
  "isDefault": 1
}
```

### 地址列表

- **请求方式**: `GET`
- **请求路径**: `/api/addresses`
- **说明**: 需要 JWT

### 删除地址

- **请求方式**: `DELETE`
- **请求路径**: `/api/addresses/{id}`
- **说明**: 需要 JWT

### 设置默认地址

- **请求方式**: `PUT`
- **请求路径**: `/api/addresses/{id}/default`
- **说明**: 需要 JWT

---

## 商品模块

### 商品列表

- **请求方式**: `GET`
- **请求路径**: `/api/products`

### 商品详情

- **请求方式**: `GET`
- **请求路径**: `/api/products/{id}`

---

## 购物车模块

### 加入购物车

- **请求方式**: `POST`
- **请求路径**: `/api/carts`
- **说明**: 需要 JWT

```json
{
  "productId": 1,
  "quantity": 2
}
```

### 查看购物车

- **请求方式**: `GET`
- **请求路径**: `/api/carts`
- **说明**: 需要 JWT

### 更新购物车数量

- **请求方式**: `PUT`
- **请求路径**: `/api/carts/{id}`
- **说明**: 需要 JWT

```json
{
  "quantity": 3
}
```

### 删除购物车商品

- **请求方式**: `DELETE`
- **请求路径**: `/api/carts/{id}`
- **说明**: 需要 JWT

---

## 优惠券模块

### 优惠券列表

- **请求方式**: `GET`
- **请求路径**: `/api/coupons`
- **说明**: 需要 JWT

### 领取优惠券

- **请求方式**: `POST`
- **请求路径**: `/api/coupons/{id}/claim`
- **说明**: 需要 JWT

### 我的优惠券

- **请求方式**: `GET`
- **请求路径**: `/api/coupons/my/available`
- **说明**: 需要 JWT

---

## 订单模块

### 创建订单

- **请求方式**: `POST`
- **请求路径**: `/api/orders`
- **说明**: 需要 JWT + 签名 + 幂等 Token

**请求头**:

```http
Authorization: Bearer {token}
Idempotency-Key: {idemToken}
X-Timestamp: {timestamp}
X-Nonce: {nonce}
X-Sign: {sign}
```

**请求参数**:

```json
{
  "addressId": 1,
  "couponId": 1
}
```

### 订单列表

- **请求方式**: `GET`
- **请求路径**: `/api/orders`
- **说明**: 需要 JWT

### 订单详情

- **请求方式**: `GET`
- **请求路径**: `/api/orders/{id}`
- **说明**: 需要 JWT

### 支付订单

- **请求方式**: `PUT`
- **请求路径**: `/api/orders/{id}/pay`
- **说明**: 需要 JWT + 签名 + 幂等 Token

### 取消订单

- **请求方式**: `PUT`
- **请求路径**: `/api/orders/{id}/cancel`
- **说明**: 需要 JWT

### 完成订单

- **请求方式**: `PUT`
- **请求路径**: `/api/orders/{id}/complete`
- **说明**: 需要 JWT

### 订单事件日志

- **请求方式**: `GET`
- **请求路径**: `/api/orders/{orderId}/events`
- **说明**: 需要 JWT，查看虚拟线程异步任务记录

---

## 秒杀模块

### 秒杀活动列表

- **请求方式**: `GET`
- **请求路径**: `/api/seckill/activities`
- **说明**: 需要 JWT

### 秒杀商品列表

- **请求方式**: `GET`
- **请求路径**: `/api/seckill/activities/{activityId}/goods`
- **说明**: 需要 JWT

### 秒杀商品详情

- **请求方式**: `GET`
- **请求路径**: `/api/seckill/goods/{seckillGoodsId}`
- **说明**: 需要 JWT

### 秒杀下单

- **请求方式**: `POST`
- **请求路径**: `/api/seckill/goods/{seckillGoodsId}/seckill?addressId={addressId}`
- **说明**: 需要 JWT + 签名 + 限流

**请求头**:

```http
Authorization: Bearer {token}
X-Timestamp: {timestamp}
X-Nonce: {nonce}
X-Sign: {sign}
```

### 秒杀订单结果查询

- **请求方式**: `GET`
- **请求路径**: `/api/seckill/orders/{seckillOrderId}`
- **说明**: 需要 JWT

---

## 签名测试接口

### 生成签名

- **请求方式**: `POST`
- **请求路径**: `/api/sign/generate`
- **说明**: 需要 JWT，用于前端获取签名参数

**请求参数**:

```json
{
  "method": "POST",
  "path": "/api/orders",
  "params": {},
  "body": "{\"addressId\":1}"
}
```

**响应**:

```json
{
  "code": 200,
  "data": {
    "timestamp": 1234567890123,
    "nonce": "abc123",
    "sign": "..."
  }
}
```

---

## 幂等 Token 接口

### 获取幂等 Token

- **请求方式**: `GET`
- **请求路径**: `/api/idempotent/token`
- **说明**: 需要 JWT

---

## 签名规则说明

签名为 HMAC-SHA256，参与签名字段包括：

1. 排序后的 query 参数（key=value）
2. 请求 body 字符串（非 JSON 请求可忽略）
3. `timestamp`
4. `nonce`

签名字符串格式：

```text
{param1}={value1}&{param2}={value2}&body={body}&timestamp={timestamp}&nonce={nonce}
```

请求头：

```http
X-Timestamp: {timestamp}
X-Nonce: {nonce}
X-Sign: {signature}
```

---

## 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录或 token 失效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

业务错误码详见 `ResultCode`。
