# 自动化测试前置与隔离

## 重置边界

`scripts/reset-test-environment.ps1` 是所有接口、可靠性和并发脚本的统一前置。它只做三件明确、可恢复的事：

1. 导入 `reset-fixtures.sql`，恢复 MallU 固定商品、优惠券和秒杀夹具；
2. 只删除 Redis 中键名以 `mallu:` 开头的数据；
3. 只清空 MallU 声明的三个 RabbitMQ 队列，不删除交换机、vhost、用户或其他项目队列。

应用每 30 秒只补建**缺失的**秒杀 Redis 库存键；重置脚本默认等待一个周期。它不会覆盖已有库存键，因此不会破坏正在执行的 Lua 库存预扣。仅测试不涉及秒杀时可传 `-SkipRedisRebuildWait`。

它不会删除 `mallu` 数据库，也不会触碰 MySQL 其他库、Redis 非 `mallu:` 键或 RabbitMQ 其他队列。

## 当前终端需要的变量

```powershell
$env:MALLU_DB_PASSWORD = '本机 MySQL root 密码'
$env:MALLU_RABBITMQ_USERNAME = 'RabbitMQ 用户名'
$env:MALLU_RABBITMQ_PASSWORD = 'RabbitMQ 密码'
.\scripts\reset-test-environment.ps1
```

变量只保留在当前终端；不写入 README、脚本或 Git。若当前仅运行无 MQ 的目录测试，可显式使用 `-SkipRabbitMq`。

## 场景夹具

每个异常状态都由独立 SQL 建立，且必须先执行基础重置，避免测试彼此依赖：

| 文件 | 用途 |
|---|---|
| `sql/scenarios/catalog-edge.sql` | 下架商品、零库存商品 |
| `sql/scenarios/marketing-edge.sql` | 未开始、已结束、禁用、领完、直接减免券 |
| `sql/scenarios/seckill-edge.sql` | 未开始、已结束、禁用的秒杀活动 |

执行规则：`reset-test-environment.ps1` → 导入一个场景 SQL → 执行对应套件 → 再次重置。
