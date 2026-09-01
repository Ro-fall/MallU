# MallU

MallU 是一个用于练习交易系统开发与测试的电商后端项目，覆盖注册登录、商品与购物车、下单、优惠券、支付回调和秒杀下单等核心链路。

## 技术栈

- Java 21、Spring Boot、MyBatis、MySQL 8、Redis 6+
- JUnit 5：以 HTTP API 为入口的端到端自动化测试
- Java 虚拟线程：处理下单后的通知、埋点和事件日志等异步任务

## 项目亮点

- **交易链路**：下单过程使用事务管理；库存通过 MySQL 条件更新扣减，避免并发超卖；优惠券根据策略计算优惠金额。
- **支付幂等**：支付平台流水号在 `payment_flow` 表中唯一约束。重复回调会返回首次处理结果，服务重启后仍然有效；金额校验失败的回调不会占用流水号。
- **状态一致性**：支付、取消、完成订单均采用“期望状态 + 条件更新”，避免支付与取消并发时相互覆盖。取消未支付订单会回滚库存并恢复关联的用户优惠券。
- **秒杀链路**：Redis Lua 脚本原子预扣库存并限制一人一单；异步落库在独立事务 Bean 内执行，失败后补偿 Redis 库存；数据库对 `(user_id, seckill_goods_id)` 设唯一约束兜底。
- **接口防护**：JWT 鉴权、请求 HMAC 签名、nonce 防重放、Redis 限流和幂等 Token。数据库、JWT 与签名密钥均由本地配置或环境变量提供，不提交真实凭据。

## 架构概览

```text
客户端 / Swagger / 自动化测试
              |
        Controller
              |
            Service
         /          \
   MyBatis / MySQL   Redis
         |
   虚拟线程异步任务
```

业务按领域划分为：`user`、`product`、`cart`、`coupon`、`order`、`payment`、`seckill`；鉴权、签名、限流、幂等和 Redis 能力放在 `common`。

## 本地启动

环境要求：Java 21、MySQL 8、Redis 6+。

1. 在本地 MySQL 执行 `src/main/resources/sql/init.sql`。注意：该脚本会重建 `mallu` 数据库，仅适用于首次初始化。
2. 将 `src/main/resources/application-example.yaml` 复制为 `application-local.yaml`，填写本地数据库、JWT 和请求签名密钥。该文件已被 Git 忽略。
3. 启动服务：

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. 打开 Swagger：`http://localhost:8080/swagger-ui/index.html`。

## 数据库增量迁移

已有本地数据时，不要再次执行 `init.sql`。请一次性执行：

```text
src/main/resources/sql/migration/V2__transaction_hardening.sql
```

该脚本会创建支付回调流水表，并补充秒杀一人一单唯一索引，不会删除已有数据。若索引创建提示存在历史重复秒杀记录，需要先人工去重。

## 自动化测试

先启动服务，再在另一个终端运行：

```powershell
.\mvnw.cmd clean test
```

签名测试需要测试进程与服务使用同一个本地密钥，例如：

```powershell
$env:MALLU_SIGNATURE_SECRET = '替换为本地随机密钥'
.\mvnw.cmd spring-boot:run

# 另一个同样配置了该环境变量的终端
.\mvnw.cmd clean test
```

当前测试覆盖注册数据驱动测试、下单—支付—回调完整链路、支付重复回调幂等，以及签名、空购物车、错误金额等负向场景。

## 安全说明

请勿提交 `application-local.yaml`、`.env`、数据库密码、JWT 密钥、请求签名密钥或构建产物。部署环境通过 `MALLU_DB_PASSWORD`、`MALLU_JWT_SECRET` 和 `MALLU_SIGNATURE_SECRET` 等环境变量注入配置。
