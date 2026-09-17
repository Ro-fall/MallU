# MallU 测试用例库

## 使用规则

- 总量：300 条设计用例；`A` 表示当前已有自动化脚本覆盖，`D` 表示已设计、待实现自动化。
- 类型：`API` 接口；`INT` 跨模块集成；`REL` 可靠性；`CON` 并发；`SCH` 定时任务。
- 每条执行前使用 `src/main/resources/sql/reset-fixtures.sql` 恢复夹具；涉及 Redis/MQ 的用例使用 `scripts/start-local.ps1` 启动依赖。
- 断言至少包含 HTTP/业务码，并在涉及状态变化时检查 MySQL、Redis 或 RabbitMQ 状态。

## 统计

| 模块 | 数量 | 编号 |
|---|---:|---|
| 用户与鉴权 | 35 | AUTH-001~035 |
| 商品与目录 | 35 | CAT-001~035 |
| 地址与购物车 | 45 | CART-001~045 |
| 订单 | 55 | ORDER-001~055 |
| 营销 | 30 | MKT-001~030 |
| Redis 可靠性 | 40 | REDIS-001~040 |
| 秒杀与 MQ | 60 | SEC-001~060 |
| **合计** | **300** | |

## 用户与鉴权（35）

| ID | 用例 | 关键断言 | 类型/状态 |
|---|---|---|---|
| AUTH-001 | 合法注册 | `200/0`、用户入库、密码 BCrypt | API/A |
| AUTH-002 | 重复用户名注册 | `409`、不新增用户 | API/D |
| AUTH-003 | 用户名少于 4 位 | `400/4000` | API/D |
| AUTH-004 | 用户名超过 32 位 | `400/4000` | API/D |
| AUTH-005 | 用户名含非法字符 | `400/4000` | API/D |
| AUTH-006 | 空用户名 | `400/4000` | API/D |
| AUTH-007 | 密码少于 8 位 | `400/4000` | API/D |
| AUTH-008 | 密码超过 72 位 | `400/4000` | API/D |
| AUTH-009 | 正确密码登录 | `200/0`、返回 JWT | API/A |
| AUTH-010 | 错误密码登录 | `401`、不返回 Token | API/D |
| AUTH-011 | 不存在用户登录 | `401` | API/D |
| AUTH-012 | 空登录请求 | `400/4000` | API/D |
| AUTH-013 | 获取当前用户 | `200/0`、用户名与积分正确 | API/A |
| AUTH-014 | 缺失 Authorization | `401/4010` | API/A |
| AUTH-015 | Bearer 前缀缺失 | `401/4010` | API/D |
| AUTH-016 | 非法 JWT | `401/4010` | API/A |
| AUTH-017 | 过期 JWT | `401/4010` | API/D |
| AUTH-018 | 篡改 JWT 签名 | `401/4010` | API/D |
| AUTH-019 | JWT 访问他人地址 | `404` 或拒绝、无泄露 | API/D |
| AUTH-020 | JWT 访问他人订单 | `404` 或拒绝、无泄露 | API/D |
| AUTH-021 | JWT 访问他人购物车项 | `404` 或拒绝、无修改 | API/D |
| AUTH-022 | 注册初始积分 | 初始值为 0 | API/D |
| AUTH-023 | 支付后积分查询 | 积分增量等于实付整数部分 | INT/A |
| AUTH-024 | 同用户重复支付 | 第二次支付被拒绝、积分不重复 | INT/D |
| AUTH-025 | Token 在多个请求中可复用 | 同一 Token 可读写本人资源 | API/D |
| AUTH-026 | Token 不写入响应日志 | 日志不含明文 Token | REL/D |
| AUTH-027 | 密码不写入响应 | 注册/登录响应无 password 字段 | API/D |
| AUTH-028 | 密码散列非明文 | 数据库 hash 不等于原密码 | API/D |
| AUTH-029 | 停用用户登录 | 被拒绝（预留状态用例） | API/D |
| AUTH-030 | 停用用户访问受保护接口 | 被拒绝（预留状态用例） | API/D |
| AUTH-031 | 并发重复注册 | 仅一条用户记录 | CON/D |
| AUTH-032 | 大小写用户名策略 | 按当前唯一约束验证 | API/D |
| AUTH-033 | 中文用户名 | 按正则拒绝 | API/D |
| AUTH-034 | SQL 注入式用户名 | 参数校验拒绝、库无异常 | REL/D |
| AUTH-035 | XSS 式用户名 | 参数校验拒绝、响应安全 | REL/D |

## 商品与目录（35）

| ID | 用例 | 关键断言 | 类型/状态 |
|---|---|---|---|
| CAT-001 | 分类列表 | `200/0`、固定 3 类 | API/A |
| CAT-002 | 商品默认分页 | `200/0`、页码正确 | API/A |
| CAT-003 | 指定分类商品 | 仅返回目标分类 | API/D |
| CAT-004 | 空分类商品 | 空列表、非 500 | API/D |
| CAT-005 | page=1 边界 | 首页正确 | API/D |
| CAT-006 | page=0 | `400/4000` | API/D |
| CAT-007 | size=1 | 仅一条 | API/D |
| CAT-008 | size=50 | 最大值允许 | API/D |
| CAT-009 | size=51 | `400/4000` | API/D |
| CAT-010 | 负数 size | `400/4000` | API/D |
| CAT-011 | 推荐商品列表 | 仅 recommended=true | API/A |
| CAT-012 | 关键字命中名称 | 返回目标商品 | API/A |
| CAT-013 | 关键字命中描述 | 返回目标商品 | API/D |
| CAT-014 | 关键字无结果 | 空页、非异常 | API/D |
| CAT-015 | 空关键字 | `400/4000` | API/D |
| CAT-016 | 中文关键字 | 正确匹配 | API/D |
| CAT-017 | 大小写英文关键字 | 忽略大小写 | API/D |
| CAT-018 | 商品详情 | 名称、价格、库存正确 | API/A |
| CAT-019 | 不存在商品详情 | `404/4042` | API/D |
| CAT-020 | 下架商品详情 | `404/4042` | API/D |
| CAT-021 | 下架商品不在列表 | 不返回 | API/D |
| CAT-022 | 下架商品不在搜索 | 不返回 | API/D |
| CAT-023 | 下架商品不在推荐 | 不返回 | API/D |
| CAT-024 | 分类排序 | 按 sortOrder 升序 | API/D |
| CAT-025 | 商品价格精度 | 两位小数不漂移 | API/D |
| CAT-026 | 商品库存为 0 的展示 | 详情可读、库存为 0 | API/D |
| CAT-027 | 高页码 | 空页、非 500 | API/D |
| CAT-028 | categoryId 非数字 | `400/4000` | API/D |
| CAT-029 | categoryId 不存在 | 空页或约定结果 | API/D |
| CAT-030 | 商品详情公共访问 | 无 Token 可访问 | API/D |
| CAT-031 | 目录接口 Redis 宕机 | MySQL 读取仍可用 | REL/D |
| CAT-032 | 并发详情查询 | 响应稳定 | CON/D |
| CAT-033 | 搜索特殊字符 | 无 SQL 错误 | REL/D |
| CAT-034 | 长关键字 | 受控响应、非 500 | API/D |
| CAT-035 | 商品响应无内部字段 | 不泄露建表字段 | API/D |

## 地址与购物车（45）

| ID | 用例 | 关键断言 | 类型/状态 |
|---|---|---|---|
| CART-001 | 新增地址 | `200/0`、地址归属当前用户 | API/A |
| CART-002 | 默认地址创建 | 默认地址为 true | API/A |
| CART-003 | 第二默认地址 | 原默认地址自动取消 | API/D |
| CART-004 | 地址列表排序 | 默认地址优先 | API/D |
| CART-005 | 修改地址 | 字段持久化正确 | API/D |
| CART-006 | 修改为默认地址 | 其他默认地址取消 | API/D |
| CART-007 | 删除地址 | 列表无该项 | API/D |
| CART-008 | 删除他人地址 | `404`、数据不变 | API/D |
| CART-009 | 修改他人地址 | `404`、数据不变 | API/D |
| CART-010 | 空收件人 | `400/4000` | API/D |
| CART-011 | 非法手机号 | `400/4000` | API/D |
| CART-012 | 空省市区详情 | `400/4000` | API/D |
| CART-013 | 未登录地址列表 | `401/4010` | API/D |
| CART-014 | 加购有效商品 | `200/0`、数量正确 | API/A |
| CART-015 | 同商品重复加购 | 合并数量 | API/D |
| CART-016 | 加购不存在商品 | `404/4042` | API/D |
| CART-017 | 加购下架商品 | `404/4042` | API/D |
| CART-018 | 加购数量 0 | `400/4000` | API/D |
| CART-019 | 加购负数数量 | `400/4000` | API/D |
| CART-020 | 加购超过库存 | `409/4003` | API/A |
| CART-021 | 修改购物车数量 | 数量更新 | API/A |
| CART-022 | 修改数量为 0 | `400/4000` | API/D |
| CART-023 | 修改数量超过库存 | `409/4003` | API/D |
| CART-024 | 修改不存在购物车项 | `404/4044` | API/D |
| CART-025 | 修改他人购物车项 | `404`、数据不变 | API/D |
| CART-026 | 删除购物车项 | `200/0`、列表消失 | API/A |
| CART-027 | 删除不存在购物车项 | `404/4044` | API/D |
| CART-028 | 删除他人购物车项 | `404`、数据不变 | API/D |
| CART-029 | 购物车列表 | 仅本人数据 | API/A |
| CART-030 | 空购物车列表 | 空数组 | API/D |
| CART-031 | 未登录购物车 | `401/4010` | API/A |
| CART-032 | 多商品购物车 | 商品及价格独立正确 | API/D |
| CART-033 | 下架后查看购物车 | 约定拒绝或提示 | API/D |
| CART-034 | 库存下降后改数量 | 再次校验库存 | INT/D |
| CART-035 | 并发同商品加购 | 不产生重复 cart_item | CON/D |
| CART-036 | 并发修改数量 | 最终数量合法 | CON/D |
| CART-037 | 加购与下单并发 | 不出现负库存 | CON/D |
| CART-038 | 地址删除后下单 | 地址校验失败 | INT/D |
| CART-039 | 购物车项 ID 非数字 | `400/4000` | API/D |
| CART-040 | 商品 ID 非数字 | `400/4000` | API/D |
| CART-041 | 大数量整数溢出 | 受控失败 | API/D |
| CART-042 | 地址详情超长 | `400` 或受控失败 | API/D |
| CART-043 | 地址默认状态重置 | 重置后无脏数据 | REL/D |
| CART-044 | 购物车重置 | reset 后无记录 | REL/D |
| CART-045 | 购物车响应无用户敏感字段 | 无越权信息 | API/D |

## 订单（55）

| ID | 用例 | 关键断言 | 类型/状态 |
|---|---|---|---|
| ORDER-001 | 获取下单 Token | `200/0`、5 分钟 TTL | API/A |
| ORDER-002 | Token 绑定购物车项 | Redis 值为排序项集 | REL/A |
| ORDER-003 | 空项集取 Token | `400/4000` | API/D |
| ORDER-004 | 重复 cartItemIds 取 Token | `400/4004` | API/D |
| ORDER-005 | 部分结算创建订单 | 仅选中项生成订单 | INT/A |
| ORDER-006 | 未选购物车项保留 | 剩余项仍在购物车 | INT/A |
| ORDER-007 | 全部购物车项结算 | 购物车清空 | API/D |
| ORDER-008 | 空购物车项下单 | `400/4000` | API/D |
| ORDER-009 | 下单缺失 Token | `400/4000` | API/D |
| ORDER-010 | Token 过期下单 | `409/4096` | REL/D |
| ORDER-011 | Token 重复下单 | `409/4096`、仅一单 | REL/A |
| ORDER-012 | Token 与项不匹配 | `409/4097` | REL/A |
| ORDER-013 | 购物车项重复下单 | `400/4004` | API/D |
| ORDER-014 | 他人购物车项下单 | `404/4044` | API/D |
| ORDER-015 | 他人地址下单 | `404/4043` | API/D |
| ORDER-016 | 不存在地址下单 | `404/4043` | API/D |
| ORDER-017 | 商品下架后下单 | `404/4042` | INT/D |
| ORDER-018 | 库存不足下单 | `409/4003`、无订单 | INT/D |
| ORDER-019 | 下单扣普通库存 | 库存减指定数量 | INT/D |
| ORDER-020 | 下单删选中购物车项 | 仅选中项删除 | INT/A |
| ORDER-021 | 订单总金额 | 商品单价×数量正确 | API/D |
| ORDER-022 | 金额两位精度 | 无浮点误差 | API/D |
| ORDER-023 | 订单号唯一 | 并发下单不冲突 | CON/D |
| ORDER-024 | 订单列表 | 仅本人、按时间倒序 | API/D |
| ORDER-025 | 空订单列表 | 空数组 | API/D |
| ORDER-026 | 订单详情 | 项目、金额、状态正确 | API/D |
| ORDER-027 | 他人订单详情 | `404/4047` | API/D |
| ORDER-028 | 不存在订单详情 | `404/4047` | API/D |
| ORDER-029 | 取消待支付订单 | 状态变 CANCELLED | API/A |
| ORDER-030 | 取消订单回补库存 | 商品库存恢复 | INT/A |
| ORDER-031 | 取消订单释放优惠券 | user_coupon AVAILABLE | INT/A |
| ORDER-032 | 取消已支付订单 | `409/4094` | API/D |
| ORDER-033 | 重复取消订单 | `409/4094` | API/D |
| ORDER-034 | 取消他人订单 | `404/4047` | API/D |
| ORDER-035 | 模拟支付待支付订单 | 状态 PAID、paidAt 有值 | API/A |
| ORDER-036 | 支付后积分 | 增量等于实付整数部分 | INT/A |
| ORDER-037 | 重复支付 | `409/4095`、积分不重复 | API/D |
| ORDER-038 | 支付已取消订单 | `409/4095` | API/D |
| ORDER-039 | 支付他人订单 | `404/4047` | API/D |
| ORDER-040 | 自动超时关闭 | 状态 CANCELLED | SCH/A |
| ORDER-041 | 超时订单回补库存 | 库存恢复 | SCH/A |
| ORDER-042 | 超时订单释放优惠券 | 优惠券恢复可用 | SCH/D |
| ORDER-043 | 超时已支付订单 | 不关闭 | SCH/D |
| ORDER-044 | 并发同 Token 下单 | 仅一笔订单 | CON/D |
| ORDER-045 | 并发不同 Token 同库存 | 不出现负库存 | CON/D |
| ORDER-046 | 下单事务失败回滚购物车 | 购物车仍在 | REL/D |
| ORDER-047 | 下单事务失败回滚库存 | 库存不变 | REL/D |
| ORDER-048 | 下单事务失败优惠券不核销 | 券仍可用 | REL/D |
| ORDER-049 | 正常订单 Redis 宕机 | `503/8001`、无订单 | REL/D |
| ORDER-050 | Redis 恢复后重新取 Token | 可成功下单 | REL/D |
| ORDER-051 | 未登录创建订单 | `401/4010` | API/D |
| ORDER-052 | 非法 orderId | `400/4000` | API/D |
| ORDER-053 | 订单响应项目快照 | 改商品后历史订单不变 | INT/D |
| ORDER-054 | 订单取消与购物车 | 已结算项不自动回购物车 | API/D |
| ORDER-055 | reset 后订单清空 | 固定夹具无订单 | REL/D |

## 营销（30）

| ID | 用例 | 关键断言 | 类型/状态 |
|---|---|---|---|
| MKT-001 | 领取满减券 | `200/0`、剩余数减一 | API/A |
| MKT-002 | 重复领取同券 | `409/4091` | API/D |
| MKT-003 | 领取不存在券 | `404/4045` | API/D |
| MKT-004 | 领取已结束券 | `409/4092` | API/D |
| MKT-005 | 领取未开始券 | `409/4092` | API/D |
| MKT-006 | 领取已禁用券 | `409/4092` | API/D |
| MKT-007 | 领取余量为 0 券 | `409/4092` | API/D |
| MKT-008 | 我的优惠券列表 | 仅本人券 | API/D |
| MKT-009 | 未登录查我的券 | `401/4010` | API/D |
| MKT-010 | 满减达门槛 | 折扣 100 | INT/A |
| MKT-011 | 满减未达门槛 | 折扣 0 | INT/D |
| MKT-012 | 满减不超过订单额 | 应付不为负 | API/D |
| MKT-013 | 九折券计算 | 折扣精确到两位 | INT/A |
| MKT-014 | 直接减免券计算 | 不超过订单额 | API/D |
| MKT-015 | 未领取券下单 | `404/4046` | API/D |
| MKT-016 | 他人 userCoupon 下单 | `404/4046` | API/D |
| MKT-017 | 已使用券再次下单 | `409/4093` | API/D |
| MKT-018 | 取消订单释放券 | AVAILABLE | INT/A |
| MKT-019 | 已支付订单不释放券 | 仍 USED | INT/D |
| MKT-020 | 超时订单释放券 | AVAILABLE | SCH/D |
| MKT-021 | 并发领取最后一张券 | 仅一人成功 | CON/D |
| MKT-022 | 并发使用同一券 | 仅一单核销 | CON/D |
| MKT-023 | 领取券与 reset | 余量恢复夹具 | REL/D |
| MKT-024 | 支付积分快照 | 支付后积分正确 | INT/A |
| MKT-025 | 小数实付积分取整 | 向下取整 | API/D |
| MKT-026 | 0 元订单积分 | 不增加负积分 | API/D |
| MKT-027 | 取消订单不扣已得积分 | 仅待支付允许取消 | API/D |
| MKT-028 | 优惠券响应字段 | 不泄露其他用户 | API/D |
| MKT-029 | 优惠券 Redis 宕机 | 普通 MySQL 券查询约定 | REL/D |
| MKT-030 | 非法券 ID | `400/4000` | API/D |

## Redis 可靠性（40）

| ID | 用例 | 关键断言 | 类型/状态 |
|---|---|---|---|
| REDIS-001 | Redis 健康可用 | `available=true/PONG` | REL/A |
| REDIS-002 | Redis 健康不可用 | `available=false` | REL/A |
| REDIS-003 | Redis 恢复 | 健康重新 PONG | REL/A |
| REDIS-004 | 创建订单 Token | Redis 写入、TTL 5 分钟 | REL/A |
| REDIS-005 | Token 格式随机 | 无重复、非空 | REL/D |
| REDIS-006 | Token 绑定单项购物车 | 绑定值正确 | REL/A |
| REDIS-007 | Token 绑定多项排序无关 | 同集合一致 | REL/D |
| REDIS-008 | Token 绑定重复项 | `400/4004` | REL/D |
| REDIS-009 | Token 过期 | `409/4096` | REL/D |
| REDIS-010 | Token 首次消费 | 原子删除 | REL/A |
| REDIS-011 | Token 重复消费 | `409/4096` | REL/A |
| REDIS-012 | Token 绑定不匹配 | `409/4097` | REL/A |
| REDIS-013 | 并发消费同 Token | 仅一请求成功 | CON/D |
| REDIS-014 | Redis 宕机取 Token | `503/8001` | REL/A |
| REDIS-015 | Redis 宕机创建订单 | `503/8001`、无订单 | REL/D |
| REDIS-016 | Redis 宕机普通商品读取 | MySQL 仍可读 | REL/D |
| REDIS-017 | Redis 重启后取 Token | 正常可用 | REL/D |
| REDIS-018 | Token key TTL | 到期自动删除 | REL/D |
| REDIS-019 | Token 不写入 MySQL | 无冗余持久化 | REL/D |
| REDIS-020 | Token 不在响应外泄 | 仅创建接口返回 | REL/D |
| REDIS-021 | 下单固定窗口首次计数 | 计数为 1 | REL/D |
| REDIS-022 | 10 次下单允许 | 未触发限流 | REL/D |
| REDIS-023 | 第 11 次下单 | `429/4291` | REL/A |
| REDIS-024 | 窗口过期后恢复 | 可再次下单 | REL/D |
| REDIS-025 | 多用户限流隔离 | 彼此不影响 | CON/D |
| REDIS-026 | 限流 Redis 宕机 | `503/8001` | REL/D |
| REDIS-027 | 限流 key TTL | 不永久增长 | REL/D |
| REDIS-028 | Redis 超时约 1 秒 | 不长时间阻塞 | REL/A |
| REDIS-029 | 秒杀库存 key 初始化 | 值等于 MySQL 秒杀库存 | REL/D |
| REDIS-030 | 秒杀用户资格 key | 首次预扣写入 | REL/D |
| REDIS-031 | 秒杀结果 PENDING | 有 TTL | REL/D |
| REDIS-032 | 秒杀成功结果 | `SUCCESS:orderId` | REL/D |
| REDIS-033 | 秒杀失败结果 | `FAILED` | REL/A |
| REDIS-034 | 秒杀补偿删除资格 | 用户可再次提交 | REL/D |
| REDIS-035 | 秒杀 Redis 宕机 | `503/8001`、不投 MQ | REL/D |
| REDIS-036 | Redis 重启后秒杀初始化 | 库存重建 | REL/D |
| REDIS-037 | Redis key 命名隔离 | 使用 mallu 前缀 | REL/D |
| REDIS-038 | 非 MallU key 不清理 | reset 不影响其他库 | REL/D |
| REDIS-039 | Redis 脚本原子性 | 高并发不负库存 | CON/D |
| REDIS-040 | Redis 连接日志安全 | 不记录凭据 | REL/D |

## 秒杀与 RabbitMQ（60）

| ID | 用例 | 关键断言 | 类型/状态 |
|---|---|---|---|
| SEC-001 | 秒杀商品列表 | `200/0`、活动商品正确 | API/A |
| SEC-002 | 不存在活动 | `404/4049` | API/D |
| SEC-003 | 空活动商品列表 | 空数组 | API/D |
| SEC-004 | 活动进行中 | active=true | API/D |
| SEC-005 | 活动未开始 | 提交 `409/4099` | API/D |
| SEC-006 | 活动已结束 | 提交 `409/4099` | API/D |
| SEC-007 | 活动禁用 | 提交 `409/4099` | API/D |
| SEC-008 | 不存在秒杀商品 | `404/4048` | API/D |
| SEC-009 | 未登录秒杀 | `401/4010` | API/D |
| SEC-010 | 秒杀缺地址 | `400/4000` | API/D |
| SEC-011 | 他人地址秒杀 | 消费失败并补偿 | REL/D |
| SEC-012 | 秒杀请求排队 | 返回 PENDING | API/A |
| SEC-013 | Lua 预扣库存 | Redis stock 减一 | REL/D |
| SEC-014 | 消费成功 | 结果 SUCCESS | INT/A |
| SEC-015 | 返回秒杀订单 ID | 可查订单详情 | INT/D |
| SEC-016 | 秒杀价格下单 | order_item 使用秒杀价 | INT/D |
| SEC-017 | 秒杀扣普通库存 | product stock 减一 | INT/D |
| SEC-018 | 秒杀扣秒杀库存 | seckill_goods stock 减一 | INT/D |
| SEC-019 | 一人重复抢购 | `409/4098` | REL/A |
| SEC-020 | 一人并发抢购 | 最多一单 | CON/D |
| SEC-021 | 秒杀库存为 0 | `409/4003` | API/D |
| SEC-022 | 10 件顺序抢购 | 前 10 成功 | INT/D |
| SEC-023 | 第 11 人抢购 | 库存不足 | INT/D |
| SEC-024 | 20 人并发抢 10 件 | 恰好 10 成功 | CON/A |
| SEC-025 | 50 人并发抢 10 件 | 不超卖 | CON/D |
| SEC-026 | 100 人并发抢 10 件 | 不超卖 | CON/D |
| SEC-027 | 并发库存不为负 | Redis/MySQL 均非负 | CON/D |
| SEC-028 | 主队列声明 | durable 且绑定正确 | REL/D |
| SEC-029 | 重试队列声明 | TTL 3 秒 | REL/D |
| SEC-030 | 死信队列声明 | durable 且绑定正确 | REL/D |
| SEC-031 | 消费者手动 ACK | 成功后消息移除 | REL/D |
| SEC-032 | 消费首次故障 | 进入重试队列 | REL/D |
| SEC-033 | 重试后成功 | 最终 SUCCESS | REL/D |
| SEC-034 | 3 次重试计数 | header 递增 | REL/D |
| SEC-035 | 最终失败 | 结果 FAILED | REL/A |
| SEC-036 | 最终失败死信 | 消息进入 dead queue | REL/D |
| SEC-037 | 最终失败补 Redis 库存 | Redis stock 恢复 | REL/A |
| SEC-038 | 最终失败删除资格 | 可再次秒杀 | REL/D |
| SEC-039 | 最终失败不创建订单 | MySQL 无订单 | REL/D |
| SEC-040 | 消费重复消息 | 幂等返回已有 orderId | REL/D |
| SEC-041 | MySQL 唯一索引兜底 | 同用户同商品仅一行 | CON/D |
| SEC-042 | MQ 不可用投递 | `503/8002`、Redis 补偿 | REL/D |
| SEC-043 | MQ 恢复后投递 | 正常 PENDING | REL/D |
| SEC-044 | 消费者停止 | 结果保持 PENDING | REL/D |
| SEC-045 | 消费者恢复 | 积压消息变 SUCCESS | REL/D |
| SEC-046 | 秒杀结果 NONE | 未提交时返回 NONE | API/D |
| SEC-047 | 秒杀结果 PENDING | 刚投递可轮询 | API/D |
| SEC-048 | 秒杀结果 SUCCESS | 包含 orderId | API/D |
| SEC-049 | 秒杀结果 FAILED | 无 orderId | API/D |
| SEC-050 | 他人查询秒杀结果 | 仅查本人结果 | API/D |
| SEC-051 | 取消秒杀订单 | 状态 CANCELLED | INT/A |
| SEC-052 | 取消回补普通库存 | product stock 恢复 | INT/D |
| SEC-053 | 取消回补秒杀库存 | seckill stock 恢复 | INT/A |
| SEC-054 | 取消删除 Redis 资格 | 同用户可再秒杀 | REL/D |
| SEC-055 | 秒杀订单超时关闭 | 状态 CANCELLED | SCH/D |
| SEC-056 | 秒杀超时回补双库存 | 两类库存恢复 | SCH/D |
| SEC-057 | 秒杀超时删除资格 | 用户可再秒杀 | SCH/D |
| SEC-058 | Redis 重建秒杀库存 | 与 MySQL 一致 | REL/D |
| SEC-059 | 秒杀消息序列化 | JSON 可消费 | REL/D |
| SEC-060 | reset 后 MQ/Redis 清理策略 | 无旧 MallU 秒杀残留 | REL/D |
