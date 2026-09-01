# MallU

MallU is a lightweight e-commerce backend used to practice transaction-system development and test automation. It covers the customer journey from registration and cart management to order creation, payment callbacks, coupons, and flash-sale ordering.

## Highlights

- JWT authentication, request signing, nonce replay protection, Redis-backed rate limiting, and idempotency-token checks.
- Transactional order creation with MySQL conditional stock updates and coupon settlement strategies.
- Flash-sale stock pre-deduction using Redis Lua, duplicate-purchase prevention, asynchronous order creation, and timeout stock recovery.
- Java 21 virtual threads for post-order notification, analytics, and event logging.
- JUnit 5 API tests covering registration data, order flow, negative cases, and payment callback handling.

## Architecture

```text
HTTP API / Swagger / test page
          |
Controller -> Service -> MyBatis Mapper -> MySQL
     |          |
     |          +-> virtual-thread post-order tasks
     +-> JWT / signing / idempotency / rate limiting
                         |
                       Redis
```

Business modules are organized by domain: `user`, `product`, `cart`, `coupon`, `order`, `payment`, and `seckill`. Cross-cutting infrastructure is in `common`.

## Local setup

Requirements: Java 21, MySQL 8, Redis 6+.

1. Run `src/main/resources/sql/init.sql` against a local MySQL instance. It recreates the `mallu` database and test data.
2. Copy `src/main/resources/application-example.yaml` to `application-local.yaml` and set a local database password and JWT secret. The local file is ignored by Git.
3. Start the service:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Open Swagger at `http://localhost:8080/swagger-ui/index.html`.

## Tests

Keep the service running, then execute the HTTP API tests in a second terminal:

```powershell
.\mvnw.cmd clean test
```

The tests create users, orders, and payment callback records in the local test database.

## Configuration security

Do not commit `application-local.yaml`, `.env`, database credentials, JWT secrets, or build output. Use environment variables such as `MALLU_DB_PASSWORD` and `MALLU_JWT_SECRET` when running outside local development.
