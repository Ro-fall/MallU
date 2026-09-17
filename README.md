# MallU

MallU 是一个无前端的最小商城 API 测试靶场。它以单体 Spring Boot 应用承载用户、商品、购物车、订单、营销与秒杀业务，用于接口自动化、并发和中间件可靠性测试。

## 范围

- 用户与 JWT 鉴权、地址管理；
- 分类、推荐、搜索、商品与库存；
- 购物车与选择性结算；
- 订单、优惠券、积分、取消回滚；
- Redis Lua 秒杀预扣、RabbitMQ 异步下单、重试和死信；
- 可重复的测试数据准备与清理。

不包含前端、后台管理端、Elasticsearch、MongoDB、微服务或真实支付。

## 本地依赖

- Java 21
- MySQL 8（数据库名：`mallu`）
- Redis 7+
- RabbitMQ 3.12+

## 数据库脚本与重置策略

脚本分为三个等级，避免把日常测试重置误操作成删库：

| 脚本 | 使用场景 | 行为 |
| --- | --- | --- |
| `src/main/resources/sql/init.sql` | 第一次使用 | 创建 `mallu` 库、建表并写入基础夹具 |
| `src/main/resources/sql/reset-fixtures.sql` | 每次接口回归前 | 清空 MallU 表数据并恢复固定商品、优惠券、秒杀活动；不删库 |
| `src/main/resources/sql/clean-local.sql` | 完全重建前 | 删除 `mallu` 库，危险操作 |

首次初始化：

```powershell
& 'D:\Software\MYSQL\bin\mysql.exe' --default-character-set=utf8mb4 -uroot -p -e "SOURCE D:/Code/javacode/MallU/src/main/resources/sql/init.sql"
```

日常接口自动化执行前重置夹具：

```powershell
& 'D:\Software\MYSQL\bin\mysql.exe' --default-character-set=utf8mb4 -uroot -p -e "SOURCE D:/Code/javacode/MallU/src/main/resources/sql/reset-fixtures.sql"
```

`reset-fixtures.sql` 只影响 `mallu` 库中的表；Redis 的运行时 Key 由测试前置步骤写入并设置过期时间，RabbitMQ 的队列由秒杀模块启动时声明。

再设置本机环境变量：`MALLU_DB_PASSWORD`、`MALLU_JWT_SECRET`。其他连接参数可覆盖 `application.yml` 中的默认本机地址。

Swagger 启动后位于 `/swagger-ui/index.html`。

## 已实现的开发基线

当前已落地：统一响应与异常处理、JWT 鉴权、注册登录、当前用户积分、分类、推荐、分页商品、搜索、商品详情、地址管理和购物车库存校验。

下一阶段会实现选择性购物车结算、订单/优惠券/积分事务，以及 Redis 与 RabbitMQ 秒杀链路；所有业务接口只提供后端 API，不包含前端工程。
