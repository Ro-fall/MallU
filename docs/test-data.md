# MallU 测试数据与重置规则

## 固定夹具

执行 `src/main/resources/sql/reset-fixtures.sql` 后：

| 对象 | 固定数据 |
|---|---|
| 分类 | 3 条：手机数码、电脑办公、生活用品 |
| 商品 1 | 测试手机 Pro，普通库存 100，秒杀库存 10，秒杀价 2999 |
| 商品 2 | 测试笔记本 Air，普通库存 50 |
| 商品 3 | 测试保温杯，普通库存 200 |
| 优惠券 1 | 满 1000 减 100，剩余 100 |
| 优惠券 2 | 九折券，剩余 100 |
| 秒杀活动 | 活动 ID 1，测试期间为进行中 |

## 测试用户规则

- 自动化脚本必须使用随机用户名：`<suite>_<随机数>`；不复用真实用户。
- 密码只作为测试请求内容使用，不记录进 Git、README、日志或报告。
- 每个用户自行创建地址；不得依赖其他测试用户的地址、购物车、订单、优惠券。

## 重置顺序

1. 关闭 MallU 与脚本托管 Redis：`scripts/stop-local.ps1`。
2. 执行 `reset-fixtures.sql`，只影响 `mallu` 数据库。
3. 使用 `start-local.ps1` 启动本机 Redis 和 MallU；RabbitMQ 为已有本机服务时仅复用。
4. 执行目标脚本；结束后再次 reset。

## 故障测试隔离

| 场景 | 仅当前 PowerShell 会话变量 |
|---|---|
| MQ 最终失败 | `MALLU_SECKILL_TEST_FAIL_ATTEMPTS=4` |
| 立即超时 | `MALLU_ORDER_TIMEOUT_MINUTES=0`、`MALLU_ORDER_TIMEOUT_SCAN_MS=1000` |
| 正常运行 | 不设置上述变量，默认故障次数 0、超时 30 分钟、扫描 60 秒 |

这些变量必须在 `start-local.ps1` 前设置，且测试结束后关闭进程并打开新 PowerShell 会话或移除变量。
