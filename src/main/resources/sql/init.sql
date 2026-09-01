CREATE DATABASE IF NOT EXISTS mallu
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE mallu;

DROP TABLE IF EXISTS order_event_log;
DROP TABLE IF EXISTS payment_flow;
DROP TABLE IF EXISTS payment_flow;
DROP TABLE IF EXISTS seckill_order;
DROP TABLE IF EXISTS seckill_goods;
DROP TABLE IF EXISTS seckill_activity;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS user_coupon;
DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS cart;
DROP TABLE IF EXISTS coupon;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS address;
DROP TABLE IF EXISTS user;

CREATE TABLE user
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username    VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(128) NOT NULL COMMENT '加密密码',
    phone       VARCHAR(20)  NULL COMMENT '手机号',
    email       VARCHAR(128) NULL COMMENT '邮箱',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT       NULL COMMENT '创建人',
    update_by   BIGINT       NULL COMMENT '更新人',
    INDEX idx_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

CREATE TABLE address
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '地址ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    receiver_name   VARCHAR(64)  NOT NULL COMMENT '收货人姓名',
    phone           VARCHAR(20)  NOT NULL COMMENT '收货人手机号',
    province        VARCHAR(64)  NOT NULL COMMENT '省份',
    city            VARCHAR(64)  NOT NULL COMMENT '城市',
    district        VARCHAR(64)  NOT NULL COMMENT '区县',
    detail_address  VARCHAR(256) NOT NULL COMMENT '详细地址',
    is_default      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认：1是 0否',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT       NULL COMMENT '创建人',
    update_by       BIGINT       NULL COMMENT '更新人',
    INDEX idx_user_id (user_id),
    FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='收货地址表';

CREATE TABLE product
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
    name        VARCHAR(128)   NOT NULL COMMENT '商品名称',
    description VARCHAR(512)   NULL COMMENT '商品描述',
    price       DECIMAL(10, 2) NOT NULL COMMENT '商品价格',
    stock       INT            NOT NULL DEFAULT 0 COMMENT '库存数量',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
    main_image  VARCHAR(256)   NULL COMMENT '商品主图',
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT         NULL COMMENT '创建人',
    update_by   BIGINT         NULL COMMENT '更新人',
    INDEX idx_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商品表';

CREATE TABLE cart
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '购物车ID',
    user_id    BIGINT   NOT NULL COMMENT '用户ID',
    product_id BIGINT   NOT NULL COMMENT '商品ID',
    quantity   INT      NOT NULL DEFAULT 1 COMMENT '商品数量',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by  BIGINT   NULL COMMENT '创建人',
    update_by  BIGINT   NULL COMMENT '更新人',
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='购物车表';

CREATE TABLE coupon
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '优惠券ID',
    name             VARCHAR(128)   NOT NULL COMMENT '优惠券名称',
    type             TINYINT        NOT NULL COMMENT '类型：1满减 2折扣 3直减',
    threshold        DECIMAL(12, 2) NULL COMMENT '门槛金额（满减券用）',
    discount_value   DECIMAL(10, 2) NOT NULL COMMENT '优惠值（满减=减免金额，折扣=折扣率，直减=直减金额）',
    start_time       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '有效期开始',
    end_time         DATETIME       NOT NULL COMMENT '有效期结束',
    total_count      INT            NOT NULL DEFAULT 0 COMMENT '总发放数量',
    remaining_count  INT            NOT NULL DEFAULT 0 COMMENT '剩余数量',
    status           TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1有效 0无效',
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT         NULL COMMENT '创建人',
    update_by        BIGINT         NULL COMMENT '更新人',
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_time (start_time, end_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='优惠券表';

CREATE TABLE user_coupon
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户优惠券ID',
    user_id    BIGINT   NOT NULL COMMENT '用户ID',
    coupon_id  BIGINT   NOT NULL COMMENT '优惠券ID',
    status     TINYINT  NOT NULL DEFAULT 1 COMMENT '状态：1未使用 2已使用 3已过期',
    order_no   VARCHAR(64) NULL COMMENT '使用时的订单号',
    used_time  DATETIME NULL COMMENT '使用时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by  BIGINT   NULL COMMENT '创建人',
    update_by  BIGINT   NULL COMMENT '更新人',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (coupon_id) REFERENCES coupon (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户优惠券表';

CREATE TABLE `order`
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    order_no        VARCHAR(64)    NOT NULL UNIQUE COMMENT '订单编号',
    user_id         BIGINT         NOT NULL COMMENT '用户ID',
    address_id      BIGINT         NOT NULL COMMENT '收货地址ID',
    coupon_id       BIGINT         NULL COMMENT '使用的优惠券ID',
    total_amount    DECIMAL(12, 2) NOT NULL COMMENT '订单商品总金额',
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '优惠金额',
    pay_amount      DECIMAL(12, 2) NOT NULL COMMENT '实际应付金额',
    status          TINYINT        NOT NULL DEFAULT 0 COMMENT '状态：0待支付 1已支付 2已取消 3已完成',
    pay_time        DATETIME       NULL COMMENT '支付时间',
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT         NULL COMMENT '创建人',
    update_by       BIGINT         NULL COMMENT '更新人',
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    FOREIGN KEY (address_id) REFERENCES address (id) ON DELETE CASCADE,
    FOREIGN KEY (coupon_id) REFERENCES coupon (id) ON DELETE SET NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';

CREATE TABLE order_item
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单项ID',
    order_id      BIGINT         NOT NULL COMMENT '订单ID',
    product_id    BIGINT         NOT NULL COMMENT '商品ID',
    product_name  VARCHAR(128)   NOT NULL COMMENT '商品名称',
    product_image VARCHAR(256)   NULL COMMENT '商品图片',
    product_price DECIMAL(10, 2) NOT NULL COMMENT '下单时商品单价',
    quantity      INT            NOT NULL COMMENT '商品数量',
    total_price   DECIMAL(12, 2) NOT NULL COMMENT '该项总价',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT         NULL COMMENT '创建人',
    update_by     BIGINT         NULL COMMENT '更新人',
    INDEX idx_order_id (order_id),
    FOREIGN KEY (order_id) REFERENCES `order` (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单项表';

CREATE TABLE order_event_log
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '事件日志ID',
    order_id   BIGINT       NOT NULL COMMENT '订单ID',
    event_type VARCHAR(64)  NOT NULL COMMENT '事件类型：NOTIFY=通知 BURY=埋点 STAT=统计',
    content    VARCHAR(512) NULL COMMENT '事件内容',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1成功 0失败',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by  BIGINT       NULL COMMENT '创建人',
    update_by  BIGINT       NULL COMMENT '更新人',
    INDEX idx_order_id (order_id),
    FOREIGN KEY (order_id) REFERENCES `order` (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单事件日志表';

CREATE TABLE payment_flow
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '支付回调流水ID',
    trade_no    VARCHAR(64)    NOT NULL COMMENT '支付平台流水号',
    order_no    VARCHAR(64)    NOT NULL COMMENT '业务订单号',
    order_id    BIGINT         NOT NULL COMMENT '订单ID',
    pay_amount  DECIMAL(12, 2) NOT NULL COMMENT '回调金额',
    result      VARCHAR(16)    NOT NULL COMMENT 'SUCCESS/FAIL',
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_trade_no (trade_no),
    INDEX idx_order_id (order_id),
    FOREIGN KEY (order_id) REFERENCES `order` (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='支付回调幂等流水表';

CREATE TABLE payment_flow
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '支付回调流水ID',
    trade_no    VARCHAR(64)    NOT NULL COMMENT '支付平台流水号',
    order_no    VARCHAR(64)    NOT NULL COMMENT '业务订单号',
    order_id    BIGINT         NOT NULL COMMENT '订单ID',
    pay_amount  DECIMAL(12, 2) NOT NULL COMMENT '回调金额',
    result      VARCHAR(16)    NOT NULL COMMENT 'SUCCESS/FAIL',
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_trade_no (trade_no),
    INDEX idx_order_id (order_id),
    UNIQUE KEY uk_user_seckill_goods (user_id, seckill_goods_id),
    FOREIGN KEY (order_id) REFERENCES `order` (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='支付回调幂等流水表';

CREATE TABLE seckill_activity
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    name        VARCHAR(128)   NOT NULL COMMENT '活动名称',
    start_time  DATETIME       NOT NULL COMMENT '开始时间',
    end_time    DATETIME       NOT NULL COMMENT '结束时间',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1进行中 0未开始 2已结束',
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT         NULL COMMENT '创建人',
    update_by   BIGINT         NULL COMMENT '更新人',
    INDEX idx_time (start_time, end_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀活动表';

CREATE TABLE seckill_goods
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '秒杀商品ID',
    activity_id     BIGINT         NOT NULL COMMENT '活动ID',
    product_id      BIGINT         NOT NULL COMMENT '商品ID',
    seckill_price   DECIMAL(10, 2) NOT NULL COMMENT '秒杀价',
    stock           INT            NOT NULL DEFAULT 0 COMMENT '剩余库存',
    total_stock     INT            NOT NULL DEFAULT 0 COMMENT '初始总库存',
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by       BIGINT         NULL COMMENT '创建人',
    update_by       BIGINT         NULL COMMENT '更新人',
    INDEX idx_activity_id (activity_id),
    INDEX idx_product_id (product_id),
    FOREIGN KEY (activity_id) REFERENCES seckill_activity (id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀商品表';

CREATE TABLE seckill_order
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '秒杀订单ID',
    seckill_goods_id BIGINT      NOT NULL COMMENT '秒杀商品ID',
    order_id         BIGINT      NOT NULL COMMENT '订单ID',
    user_id          BIGINT      NOT NULL COMMENT '用户ID',
    status           TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1待支付 2已支付 3已取消',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by        BIGINT      NULL COMMENT '创建人',
    update_by        BIGINT      NULL COMMENT '更新人',
    INDEX idx_user_id (user_id),
    INDEX idx_seckill_goods_id (seckill_goods_id),
    INDEX idx_order_id (order_id),
    UNIQUE KEY uk_user_seckill_goods (user_id, seckill_goods_id),
    FOREIGN KEY (seckill_goods_id) REFERENCES seckill_goods (id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES `order` (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀订单表';

INSERT INTO product (name, description, price, stock, status, main_image)
VALUES ('iPhone 15 Pro', '苹果最新旗舰手机', 7999.00, 100, 1, 'https://example.com/iphone15.jpg'),
       ('MacBook Air M3', '轻薄高性能笔记本', 8999.00, 50, 1, 'https://example.com/macbook.jpg'),
       ('AirPods Pro 2', '主动降噪耳机', 1899.00, 200, 1, 'https://example.com/airpods.jpg');

INSERT INTO coupon (name, type, threshold, discount_value, end_time, total_count, remaining_count)
VALUES ('满1000减100', 1, 1000.00, 100.00, '2030-12-31 23:59:59', 100, 100),
       ('9折优惠券', 2, NULL, 0.90, '2030-12-31 23:59:59', 100, 100),
       ('直减50元', 3, NULL, 50.00, '2030-12-31 23:59:59', 100, 100);

INSERT INTO seckill_activity (name, start_time, end_time, status)
VALUES ('618 手机秒杀', '2026-07-01 00:00:00', '2030-12-31 23:59:59', 1);

INSERT INTO seckill_goods (activity_id, product_id, seckill_price, stock, total_stock)
VALUES (1, 1, 4999.00, 10, 10),
       (1, 2, 6999.00, 1, 1);
