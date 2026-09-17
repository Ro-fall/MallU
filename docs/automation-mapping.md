# 用例与自动化映射

## 当前已自动化的套件

| 脚本/测试 | 覆盖用例编号 | 当前状态 |
|---|---|---|
| `OrderAmountCalculatorTest` | MKT-010、MKT-013 | 单元测试通过 |
| `api-order-smoke.ps1` | AUTH-001、AUTH-009、AUTH-013、CART-001、CART-014、ORDER-005~006、ORDER-029~031、ORDER-035~036、MKT-001、MKT-010、MKT-018、MKT-024 | 已实跑 |
| `api-redis-reliability-smoke.ps1` | REDIS-001、004、006、010~012 | 已实跑 |
| `api-order-rate-limit-smoke.ps1` | REDIS-023 | 已实跑 |
| `api-seckill-smoke.ps1` | SEC-001、012、014、019 | 已实跑 |
| `api-seckill-failure-smoke.ps1` | SEC-035、037、039 | 已实跑 |
| `api-seckill-concurrency-smoke.ps1` | SEC-024、027 | 已实跑 |
| `api-order-timeout-smoke.ps1` | ORDER-040~041 | 已实跑 |
| `api-auth-catalog-cart-p0.ps1` | AUTH-002~016、025、027；CAT-001~004、006~007、009~012、014、018~019；CART-001~004、008、014~022、025~027、029、031 | 已实跑通过 |
| `api-order-marketing-p0.ps1` | ORDER-003~008、013~016、020、024~028、032、034~037、051；MKT-001~003、008~010、024 | 已实跑通过 |
| `api-catalog-edge.ps1` | CAT-020~023、026；CART-017、020 | 已实跑通过 |
| `api-seckill-edge.ps1` | SEC-002、005~010、046、050 | 已实跑通过 |
| `api-seckill-stock-limit.ps1` | SEC-012、021~023 | 已实跑通过 |
| `api-seckill-lifecycle.ps1` | SEC-014~018、051~054 | 已实跑通过 |
| `api-seckill-timeout-smoke.ps1` | SEC-055~057 | 已实跑通过 |
| `api-redis-outage-smoke.ps1` | REDIS-002~003、014、028 | 已实跑通过 |
| `api-redis-token-edge.ps1` | REDIS-005、007~008 | 已实跑通过 |
| `api-redis-token-concurrency.ps1` | REDIS-013 | 已实跑通过 |

## 落地批次

| 批次 | 目标 | 用例范围 | 交付 |
|---|---|---|---|
| 1 | P0 核心回归 | AUTH/CAT/CART/ORDER/MKT 正向与关键失败 | 约 50 条 API 脚本 |
| 2 | 权限与边界 | 所有模块 ID 越权、参数边界、状态机 | 约 100 条接口断言 |
| 3 | Redis/MQ 可靠性 | 宕机、恢复、重试、死信、超时 | 约 80 条可靠性检查 |
| 4 | 并发与压测 | 并发注册、下单、领券、秒杀 | 约 70 条/场景检查 |

`docs/test-cases.md` 是 300 条用例的唯一编号来源；脚本中新增断言时必须回填对应 ID，避免“脚本有了但没有测试设计”的情况。
