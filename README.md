# MallU：电商自动化与可靠性测试靶场

MallU（E-commerce Automation & Reliability Testing Sandbox）是一个无前端的最小商城 API 测试靶场。它以单体 Spring Boot 应用承载电商核心链路，服务于接口自动化、并发、幂等和中间件故障测试。

## 项目目标与边界

目标范围：

- 用户与 JWT 鉴权、地址管理；
- 分类、推荐、搜索、商品与库存；
- 购物车与选择性结算；
- 订单、优惠券、积分、取消回滚；
- Redis Lua 秒杀预扣、RabbitMQ 异步下单、重试和死信；
- 可重复的测试数据准备、重置与清理。

不包含前端、后台管理端、Elasticsearch、MongoDB、微服务或真实支付。

## 本地依赖

- Java 21
- MySQL 8（数据库名：`mallu`）
- Redis 7+（后续秒杀模块启用）
- RabbitMQ 3.12+（后续秒杀模块启用）

## 安全约定

- 仓库不保存数据库、JWT 或 RabbitMQ 的真实密码；`.env`、`application-local.yml`、Maven 本机配置已被 Git 忽略。
- 启动前必须在本机设置 `MALLU_DB_PASSWORD` 与一个不少于 32 字符的 `MALLU_JWT_SECRET`；不要把它们写进 `application.yml`、README、提交信息或截图。
- RabbitMQ 用户名与密码必须通过 `MALLU_RABBITMQ_USERNAME` 和 `MALLU_RABBITMQ_PASSWORD` 提供；即便是本机开发，也不将 `guest/guest` 等凭据提交到配置文件。不要将 5672、15672 暴露到公网。
- `clean-local.sql` 会删除整个 `mallu` 数据库，仅限确认无误的本地环境使用；日常回归请使用 `reset-fixtures.sql`。

## 数据库脚本与重置策略

脚本分为三个等级，避免把日常测试重置误操作成删库：

| 脚本 | 使用场景 | 行为 |
| --- | --- | --- |
| `src/main/resources/sql/init.sql` | 第一次使用 | 创建 `mallu` 库、建表并写入基础夹具 |
| `src/main/resources/sql/reset-fixtures.sql` | 每次接口回归前 | 清空 MallU 表数据并恢复固定商品、优惠券、秒杀活动；不删库 |
| `src/main/resources/sql/clean-local.sql` | 完全重建前 | 删除 `mallu` 库，危险操作 |

在仓库根目录执行。以下示例假定 `mysql` 已加入系统 `PATH`；若未加入，请将 `mysql` 替换为本机 MySQL 客户端的绝对路径。

首次初始化：

```powershell
$script = (Resolve-Path 'src/main/resources/sql/init.sql').Path -replace '\\', '/'
mysql --default-character-set=utf8mb4 -uroot -p -e "SOURCE $script"
```

日常接口自动化执行前重置夹具：

```powershell
$script = (Resolve-Path 'src/main/resources/sql/reset-fixtures.sql').Path -replace '\\', '/'
mysql --default-character-set=utf8mb4 -uroot -p -e "SOURCE $script"
```

`reset-fixtures.sql` 只影响 `mallu` 库中的表；Redis 的运行时 Key 由测试前置步骤写入并设置过期时间，RabbitMQ 的队列由秒杀模块启动时声明。

设置本机环境变量（PowerShell 示例，不要把真实值提交到仓库）：

```powershell
$env:MALLU_DB_PASSWORD = '<your-local-password>'
$env:MALLU_JWT_SECRET = '<at-least-32-character-local-secret>'
$env:MALLU_RABBITMQ_USERNAME = '<local-rabbitmq-user>'
$env:MALLU_RABBITMQ_PASSWORD = '<local-rabbitmq-password>'
```

其他连接参数可通过 `MALLU_DB_URL`、`MALLU_DB_USERNAME`、`MALLU_REDIS_HOST`、`MALLU_RABBITMQ_HOST` 等环境变量覆盖 `application.yml` 中的本机默认地址。

Swagger 启动后位于 `/swagger-ui/index.html`。

## 当前已实现

当前已落地：统一响应与异常处理、JWT 鉴权、注册登录、当前用户积分、分类、推荐、分页商品、搜索、商品详情、地址管理、购物车库存校验、优惠券领取、部分购物车结算、订单列表与详情、订单取消/超时库存回滚、模拟支付积分、Redis 健康检查、幂等 Token、下单限流，以及 RabbitMQ 秒杀异步订单。

## 开发路线图

- 扩展并发压测、消息失败注入和死信消费观测脚本。

## 秒杀与 RabbitMQ

秒杀请求使用 Redis Lua 原子完成库存预扣和一人一单资格，然后投递 RabbitMQ；调用方通过结果接口轮询订单状态。

```text
POST /api/seckill/goods/{goodsId}/orders
  → Redis Lua 预扣
  → mallu.seckill.order.queue
  → 消费者事务创建订单
  → GET /api/seckill/goods/{goodsId}/result
```

RabbitMQ 声明主队列、3 秒延迟重试队列和死信队列。消费者手动 ACK；处理失败会最多重试 3 次，最终进入死信队列并补偿 Redis 资格/库存、标记秒杀失败。MySQL 的 `(user_id, seckill_goods_id)` 唯一索引是最终幂等兜底。订单取消或超时关闭时同时回补普通库存、秒杀库存与 Redis 资格。

所有业务接口只提供后端 API，不包含前端工程。

## 当前自动化测试

- `mvn test`：金额规则单元测试，覆盖满减门槛和折扣券计算；
- `scripts/api-order-smoke.ps1`：真实 HTTP 冒烟，覆盖注册、地址、领券、部分结算、取消库存回补、优惠券释放、模拟支付与积分。
- `scripts/api-redis-reliability-smoke.ps1`：Redis 健康、Token 原子消费、重复提交与购物车绑定；
- `scripts/api-order-rate-limit-smoke.ps1`：第 11 次下单请求触发 `4291` 限流；
- `scripts/api-seckill-smoke.ps1`：Redis Lua 秒杀预扣、RabbitMQ 消费、结果轮询与一人一单。
- `scripts/api-seckill-failure-smoke.ps1`：消费者重试、死信、Redis 补偿与失败结果（需测试故障开关）；
- `scripts/api-seckill-concurrency-smoke.ps1`：20 用户并发抢 10 件，验证无超卖；
- `scripts/api-order-timeout-smoke.ps1`：订单超时关闭与库存回滚（需测试超时配置）。

运行接口冒烟前先执行 `reset-fixtures.sql`，并以已设置必要环境变量的方式启动应用；随后在仓库根目录运行：

```powershell
.\scripts\api-order-smoke.ps1
```

脚本会创建一名随机测试用户。完成一次演示后可再次运行 `reset-fixtures.sql` 清理测试数据。

## Redis 可靠性与本机启停

- `GET /api/health/redis`：返回 Redis 可用状态；
- `POST /api/orders/tokens`：为指定 `cartItemIds` 签发 5 分钟有效的下单 Token；创建订单必须携带该 Token；
- 同一 Token 通过 Redis Lua 原子校验并删除，重复提交返回 `4096`；Token 与购物车项不匹配返回 `4097`；
- 创建订单按用户做 Redis 固定窗口限流（每分钟 10 次）；Redis 不可用时，Token 和限流等强依赖接口在约 1 秒内返回 `503 / 8001`，不降级执行写操作。

先设置本机环境变量、执行 `mvn package`，然后可用以下脚本管理本地进程：

```powershell
.\scripts\start-local.ps1
# 运行接口测试后
.\scripts\stop-local.ps1
```

`start-local.ps1` 会检查 6379：若没有 Redis，它仅绑定 `127.0.0.1` 启动一个本地 Redis 并记录 PID；若已有 Redis，则只复用且不会在停止时关闭它。`stop-local.ps1` 只终止脚本自身记录的 MallU/Redis 进程树。

Redis 可靠性冒烟脚本：

```powershell
.\scripts\api-redis-reliability-smoke.ps1
.\scripts\api-order-rate-limit-smoke.ps1
.\scripts\api-seckill-smoke.ps1
```

故障、并发与超时演练都应先运行 `reset-fixtures.sql`。消费者最终失败测试需要在启动前仅为当前 PowerShell 会话设置 `MALLU_SECKILL_TEST_FAIL_ATTEMPTS=4`；超时测试需要设置 `MALLU_ORDER_TIMEOUT_MINUTES=0`、`MALLU_ORDER_TIMEOUT_SCAN_MS=1000`。它们默认值分别为 `0`、`30`、`60000`，不会在正常运行时触发故障或立即关闭订单。

```powershell
# 消费者重试 3 次后进入死信并补偿
$env:MALLU_SECKILL_TEST_FAIL_ATTEMPTS = '4'
.\scripts\start-local.ps1
.\scripts\api-seckill-failure-smoke.ps1
.\scripts\stop-local.ps1

# 立即超时测试
$env:MALLU_ORDER_TIMEOUT_MINUTES = '0'
$env:MALLU_ORDER_TIMEOUT_SCAN_MS = '1000'
.\scripts\start-local.ps1
.\scripts\api-order-timeout-smoke.ps1
.\scripts\stop-local.ps1
```
