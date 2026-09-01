# MallU 秒杀压测说明

## 环境

- MallU 运行在 **Windows**（localhost:8080），MySQL + Redis 已启动，已执行 `init.sql`
- JMeter 位于 `D:\Software\apache-jmeter-5.6.3`
- 脚本目录：`scripts/stress/`

## 步骤

### 1. 启动 MallU

```cmd
cd D:\Code\javacode\MallU
set JAVA_HOME=D:\Code\java
mvnw.cmd spring-boot:run
```

### 2. 准备压测用户（在 WSL 或 Windows Python 均可）

```bash
cd /mnt/d/code/javacode/mallu/scripts/stress
python3 prepare_users.py 500   # 生成 500 个压测用户 -> users.csv
```

> 注意：`prepare_users.py` 里的 `BASE_URL=http://localhost:8080`。如果在 WSL 里跑但 MallU 在 Windows 上，需要把 `localhost` 改为 Windows 宿主 IP（WSL2 中通常是网关 IP，或直接用 Windows 侧 Python 跑）。

### 3. 重置秒杀库存（保证每轮压测初始库存一致）

```sql
-- 在 MySQL 中执行
UPDATE seckill_goods SET stock = total_stock WHERE id = 1;
```

并清空 Redis 秒杀库存 key：

```bash
redis-cli del "seckill:stock:1" "seckill:users:1"
```

### 4. 运行 JMeter

```cmd
D:\Software\apache-jmeter-5.6.3\bin\jmeter.bat -n -t seckill_stress.jmx -l result.jtl -e -o report
```

- `result.jtl`：原始结果
- `report/`：HTML 聚合报告

### 5. 解读结果

- **聚合报告**：TPS（Throughput）、平均响应时间、90%/95%/99% 响应时间
- **错误率**：非 200 的比例（限流 6001 / 售罄 7004 / 已购买 7003 属业务正常返回，断言 `"code":200` 会将其记为失败，需人工区分）

### 6. 验证超卖

压测后执行（库存应为 0，不可能为负数，否则超卖）：

```sql
SELECT id, stock, total_stock FROM seckill_goods WHERE id = 1;
SELECT COUNT(*) FROM seckill_order WHERE seckill_goods_id = 1;
```

`seckill_order` 数量不应超过 `total_stock`。

## 常见问题

- **签名失败 401**：确认 `addressId` 在签名与请求 query 中一致；确认时间戳未超 5 分钟（JMeter 运行时若长期挂起会过期，建议脚本时间短）
- **限流 6001**：`@RateLimit(limit=1, windowSeconds=3)` 单个用户 3 秒只能秒 1 次。压测用 CSV 每用户 1 次请求可避开；如需并发测试同一商品，需多用户
- **秒杀异步落库**：压测返回 200 只是"抢购成功排队"，订单由异步线程落库，压测后需等待数秒再统计订单数
