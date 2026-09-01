# 优选交易平台（MallU）项目记录

## 项目介绍

MallU 是一个**轻量级电商后端服务**，定位为 **TestPilot 智能测试 Agent 的被测对象**，同时作为简历项目展示 Java 21 + Spring Boot 4 后端开发能力。

项目覆盖了一套完整电商系统的基础链路：
- 用户注册登录、地址管理
- 商品浏览、购物车、订单创建与支付
- 优惠券领取与结算
- 秒杀活动（高并发场景）

同时融入了多个可作为测试点的技术亮点：
- 接口幂等防重
- 接口签名防篡改
- 基于 Redis + Lua 的秒杀库存扣减
- 限流控制
- Java 21 虚拟线程异步编排

---

## 项目信息

| 项目 | 内容 |
|------|------|
| 项目名 | MallU（优选交易平台） |
| 定位 | 轻量级电商后端服务，为 TestPilot 智能测试 Agent 提供被测接口 |
| 技术栈 | Spring Boot 4.0.7 + Java 21 + MyBatis 3.0.4 + MySQL 8.0.46 + Redis 7.2.5 + JJWT + Lombok |
| 接口文档 | SpringDoc（OpenAPI 3.0），访问 `/swagger-ui/index.html` |
| 测试页面 | `src/main/resources/static/index.html` |

---

## 核心功能模块

### 1. 基础设施
- 统一响应封装 `Result<T>` + 业务错误码 `ResultCode`
- 业务异常 `BusinessException` + 全局异常处理 `GlobalExceptionHandler`
- JWT 认证链路：登录颁发 Token、接口拦截校验、ThreadLocal 用户上下文
- 手动配置 `SqlSessionFactory`（Spring Boot 4 下 MyBatis 自动配置不兼容）
- 静态资源：`favicon.ico`、`banner.txt` 已替换
- 接口文档：SpringDoc（OpenAPI 3.0），支持 JWT 认证，Markdown 文档 `API.md` + 自动生成脚本 `scripts/generate_api_doc.py`

### 2. 用户模块
- 注册 / 登录 / JWT Token 颁发
- 收货地址管理（增删改查、设默认、数量上限）
- 密码使用 BCrypt 加密

### 3. 商品模块
- 商品列表（分页）
- 商品详情
- 库存查询
- 库存扣减 / 回滚

### 4. 购物车模块
- 加入购物车（检查商品、库存；已存在则合并数量）
- 查看购物车
- 修改商品数量
- 删除购物车商品

### 5. 订单模块
- 创建订单（从购物车生成、扣减库存、生成订单项、清空购物车）
- 订单列表（分页）
- 订单详情
- 订单状态流转：待支付 → 已支付 → 已完成，待支付 → 已取消
- 取消订单时回滚库存
- 创建订单时可选择优惠券，自动计算优惠金额和实付金额

### 6. 优惠券策略模式
- 优惠券类型：满减、折扣、直减
- 使用策略模式隔离结算逻辑（`CouponStrategy` + `CouponStrategyFactory`）
- 优惠券领取 / 列表 / 查询接口
- 用户优惠券表，支持已使用状态流转

### 7. 接口幂等防重复提交
- 基于 Redis + Token 的幂等控制
- 注解 `@Idempotent` + 拦截器 `IdempotentInterceptor`
- 已应用于：创建订单、支付订单
- 前端支持并发幂等测试（同一个 Token 同时发两次请求）

### 8. MyBatis 拦截器自动填充
- 定义 `BaseEntity` 通用字段接口
- `AutoFillInterceptor` 拦截 insert/update 操作
- 自动填充 `created_at` / `updated_at` / `create_by` / `update_by`
- 支持单个对象和集合（如 `batchInsert`）的批量填充
- 所有业务实体实现 `BaseEntity` 接口

### 9. 秒杀模块
- 秒杀活动 / 秒杀商品 / 秒杀订单三张表
- 同步秒杀链路：校验活动 → 限流 → Redis + Lua 原子扣减库存 → 防重复购买 → 创建订单
- 秒杀库存预热到 Redis，`SeckillRedisService` 用 Lua 脚本保证原子性
- 限流注解 `@RateLimit` + 拦截器 `RateLimitInterceptor`，支持用户级/全局限流
- 预留异步查询结果接口 `/api/seckill/orders/{seckillOrderId}`
- 前端增加秒杀活动/商品/下单/并发测试页面
- 新增库存为 1 的秒杀商品（MacBook Air M3），用于测试售罄场景
- 秒杀商品表增加 `total_stock` 字段，区分“剩余库存”和“初始总库存”
- 取消/支付订单时同步更新 `seckill_order` 状态和库存回滚
- 秒杀订单超时未支付自动释放库存：定时任务每 5 分钟扫描，恢复 `seckill_goods` 和 Redis 库存，取消订单

### 10. 接口防刷与防篡改
- `@ApiSign` 注解标记需要签名校验的接口
- `SignFilter` 拦截 POST `/api/orders`、PUT `/api/orders/*/pay`、POST `/api/seckill/goods/*/seckill`
- 签名规则：HMAC-SHA256（排序后的参数 + body + timestamp + nonce）
- 校验 timestamp 5 分钟内有效，nonce Redis 5 分钟去重
- 提供 `/api/sign/generate` 测试接口生成签名
- 前端增加签名测试区域，支持生成签名、发送正常请求、发送错误签名请求
- 已应用于：创建订单、支付订单、秒杀下单

### 11. 虚拟线程异步编排
- 使用 Java 21 `Executors.newVirtualThreadPerTaskExecutor()` 创建虚拟线程池
- 下单成功后异步并行执行：通知发送、埋点记录、销量统计
- 新增 `order_event_log` 表记录异步任务执行结果
- 使用 `TransactionSynchronizationManager` 确保事务提交后再触发异步任务
- 异步任务失败不影响主订单创建
- 前端增加订单事件日志查询页面

---

## 技术亮点

| 亮点 | 说明 |
|------|------|
| Java 21 虚拟线程 | 下单后异步通知/埋点/统计，提升响应速度 |
| Redis + Lua 秒杀 | 原子扣减库存，防止超卖，支持重复购买校验 |
| 接口签名防篡改 | HMAC-SHA256 参数签名，timestamp + nonce 防重放 |
| 接口幂等 | Redis Token 防止重复提交，已用于订单创建/支付 |
| 策略模式 | 优惠券满减/折扣/直减结算逻辑解耦 |
| MyBatis 自动填充 | 拦截器统一填充时间戳/操作人字段 |
| 限流控制 | 注解 + Redis 计数器，支持用户级/全局限流 |

---

## 开发调试过程记录

### 1. MyBatis 自动配置不兼容
**问题**：Spring Boot 4.0.7 下 MyBatis 自动配置失效。
**解决**：手动创建 `MyBatisConfig`，配置 `SqlSessionFactory` 和 `SqlSessionTemplate`。

### 2. 数据库表字段缺失
**问题**：Mapper 查询 `create_by`/`update_by` 时提示 `Unknown column`。
**原因**：表结构未更新，旧表缺少新增字段。
**解决**：重新执行 `init.sql`，必要时 `DROP DATABASE` 后重建。

### 3. 秒杀商品表创建失败
**问题**：`seckill_goods` 表创建/插入失败。
**原因**：`init.sql` 中秒杀商品插入语句在 `product` 表创建之前，外键约束导致失败。
**解决**：重排 `init.sql` 顺序：先创建所有基础表，再统一插入测试数据。

### 4. 秒杀订单取消后库存未恢复
**问题**：取消秒杀订单后，数据库秒杀库存和 Redis 库存未回滚。
**原因**：`OrderService.cancelOrder` 只回滚了普通商品库存。
**解决**：判断订单是否为秒杀订单，额外回滚 `seckill_goods.stock`、恢复 Redis 库存、更新 `seckill_order` 状态。

### 5. 秒杀库存显示不正确
**问题**：下单后显示“剩余库存 8 / 9”，总库存数字也下降了。
**原因**：前端用 `seckill_goods.stock` 作为总库存，但 `stock` 字段会随扣减变化。
**解决**：新增 `total_stock` 字段表示初始总库存，前端显示 `剩余库存 / 总库存`。

### 6. 签名验证失败（创建订单）
**问题**：创建订单提示“签名验证失败”。
**原因**：`/api/sign/generate` 生成签名时未包含 body，但后端 `SignFilter` 验证时把 body 参与签名。
**解决**：修改 `SignController.generate()`，将 body 加入签名参数。

### 7. Jackson 包不存在
**问题**：编译报错 `com.fasterxml.jackson.databind` 不存在。
**原因**：项目中未显式引入 Jackson，直接注入 `ObjectMapper` 失败。
**解决**：`SignFilter` 中移除 `ObjectMapper`，错误响应改为手写 JSON 字符串。

### 8. 虚拟线程 BURY 任务未记录日志
**问题**：订单事件日志中只看到 NOTI
**原因**：虚拟线程在事务提交前执行，BURY 任务查询订单时数据还不可见。
**解决**：使用 `TransactionSynchronizationManager.registerSynchronization()` 在事务提交后触发异步任务。

### 9. 浏览器请求 `/.well-known/appspecific/com.chrome.devtools.json` 404
**问题**：Chrome 自动发送该请求，后端报 `NoResourceFoundException`。
**原因**：Chrome/DevTools 后台探测，项目无此静态资源。
**解决**：可忽略，或在 `static/.well-known/appspecific/` 下放置空 JSON 文件消除日志。

---

## 本地启动

1. 启动 MySQL 和 Redis
2. 执行 `init.sql` 初始化数据库
3. 修改 `application.yaml` 中的数据库密码
4. 运行 `mvnw.cmd spring-boot:run`
5. 访问前端测试页面：`http://localhost:8080/index.html`
6. 访问接口文档：`http://localhost:8080/swagger-ui/index.html`

---

## 已知问题 / 注意事项

- 当前 MyBatis 使用 3.0.4 + 手动 `MyBatisConfig`，后续 Spring Boot 4 对 MyBatis 支持更新后可恢复自动配置
- 数据库脚本：`src/main/resources/sql/init.sql`，新增表后需重新执行
- 签名接口在 Swagger UI 中直接调试会失败（不会自动算签名），建议通过前端页面或测试脚本调用
- 浏览器刷新时可能会触发 Chrome DevTools 的 `.well-known` 探测请求，产生 404 日志，不影响功能

---

## 已完成总结

MallU 项目功能开发已全部完成，覆盖核心电商链路 + 多个技术亮点：

- **基础电商功能**：用户、商品、购物车、订单、地址、优惠券
- **高并发/安全**：秒杀（Redis + Lua + 限流）、接口签名防篡改、接口幂等防重、限流
- **Java 21 / 工程化**：虚拟线程异步编排、MyBatis 自动填充、策略模式结算优惠券
- **接口文档**：SpringDoc 自动生成，支持 JWT 认证，同时提供 `API.md` 核心接口文档和自动生成脚本

后续测试脚本、单元测试、中间件扩展在另一个项目中推进。

---

## 未来可扩展方向（暂不实现）

以下为可进一步提升项目技术深度的中间件/架构方向，当前版本不做实现，后续有空再补：

### 1. SkyWalking 全链路追踪
- 接入 SkyWalking Java Agent，实现请求全链路可视化
- 不需要 Docker，可直接本地运行 OAP + UI 二进制
- 能查看 Controller → Service → Mapper → Redis → MySQL 完整调用链

### 2. 消息队列（Kafka / RabbitMQ）
- 把秒杀下单改成 MQ 异步处理，削峰填谷
- 用户抢中后先发消息，消费者异步创建订单

### 3. Elasticsearch
- 商品数据同步到 ES，实现关键词搜索、分页、高亮

### 4. Redisson 分布式锁
- 替换现有 Redis Lua 自研锁
- 可重入锁、看门狗自动续期

### 5. Sentinel 限流熔断
- 替换现有 `@RateLimit` 自实现
- 流量控制、熔断降级、系统保护、可视化控制台

### 6. XXL-Job 分布式任务调度
- 替换现有 `@Scheduled`
- 支持任务分片、失败重试、可视化调度

### 7. Micrometer + Prometheus + Grafana
- 指标监控体系，替代 SkyWalking 做轻量级监控

### 8. AI 大模型接入
- 商品智能推荐
- 智能客服/导购
