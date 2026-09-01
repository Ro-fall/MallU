-- 供已有本地库增量执行；init.sql 仅用于从零初始化，包含 DROP TABLE。
CREATE TABLE IF NOT EXISTS payment_flow
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_no    VARCHAR(64)    NOT NULL,
    order_no    VARCHAR(64)    NOT NULL,
    order_id    BIGINT         NOT NULL,
    pay_amount  DECIMAL(12, 2) NOT NULL,
    result      VARCHAR(16)    NOT NULL,
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_trade_no (trade_no),
    INDEX idx_order_id (order_id),
    CONSTRAINT fk_payment_flow_order FOREIGN KEY (order_id) REFERENCES `order` (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='支付回调幂等流水表';

-- 运行前若历史库已有同一用户同一秒杀商品的重复记录，请先人工去重。
SET @index_exists := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'seckill_order'
      AND index_name = 'uk_user_seckill_goods'
);
SET @sql := IF(@index_exists = 0,
    'ALTER TABLE seckill_order ADD UNIQUE KEY uk_user_seckill_goods (user_id, seckill_goods_id)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
