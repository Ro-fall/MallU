-- 首次初始化专用：仅用于新建 MallU 测试数据库。
-- 已存在业务数据时请使用 reset-fixtures.sql，不要重复执行本文件。
CREATE DATABASE IF NOT EXISTS mallu DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mallu;

CREATE TABLE mall_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    points INT NOT NULL DEFAULT 0,
    status BIT NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(64) NOT NULL,
    city VARCHAR(64) NOT NULL,
    district VARCHAR(64) NOT NULL,
    detail VARCHAR(255) NOT NULL,
    is_default BIT NOT NULL DEFAULT b'0',
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES mall_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL UNIQUE,
    sort_order INT NOT NULL DEFAULT 0,
    status BIT NOT NULL DEFAULT b'1'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    recommended BIT NOT NULL DEFAULT b'0',
    status BIT NOT NULL DEFAULT b'1',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_category(category_id),
    INDEX idx_product_recommended(recommended, status),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cart_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cart_user_product(user_id, product_id),
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES mall_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(16) NOT NULL COMMENT 'FULL_REDUCTION,DISCOUNT,DIRECT',
    threshold_amount DECIMAL(10,2),
    value DECIMAL(10,2) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    remaining_count INT NOT NULL,
    status BIT NOT NULL DEFAULT b'1'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    used_order_no VARCHAR(64),
    UNIQUE KEY uk_user_coupon_once(user_id, coupon_id),
    CONSTRAINT fk_user_coupon_user FOREIGN KEY (user_id) REFERENCES mall_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupon(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE mall_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    address_id BIGINT NOT NULL,
    user_coupon_id BIGINT,
    total_amount DECIMAL(10,2) NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    pay_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING_PAYMENT',
    expires_at DATETIME NOT NULL,
    paid_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_user_created(user_id, created_at),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES mall_user(id),
    CONSTRAINT fk_order_address FOREIGN KEY (address_id) REFERENCES address(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    product_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES mall_order(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE seckill_activity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status BIT NOT NULL DEFAULT b'1'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE seckill_goods (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    activity_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    seckill_price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    total_stock INT NOT NULL,
    per_user_limit INT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_activity_product(activity_id, product_id),
    CONSTRAINT fk_seckill_goods_activity FOREIGN KEY (activity_id) REFERENCES seckill_activity(id),
    CONSTRAINT fk_seckill_goods_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE seckill_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    seckill_goods_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING_PAYMENT',
    UNIQUE KEY uk_seckill_user_goods(user_id, seckill_goods_id),
    CONSTRAINT fk_seckill_order_goods FOREIGN KEY (seckill_goods_id) REFERENCES seckill_goods(id),
    CONSTRAINT fk_seckill_order_order FOREIGN KEY (order_id) REFERENCES mall_order(id),
    CONSTRAINT fk_seckill_order_user FOREIGN KEY (user_id) REFERENCES mall_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO category(name, sort_order) VALUES ('手机数码', 10), ('电脑办公', 20), ('生活用品', 30);
INSERT INTO product(category_id, name, description, price, stock, recommended) VALUES
  (1, '测试手机 Pro', '用于商品、购物车和订单接口测试', 3999.00, 100, 1),
  (2, '测试笔记本 Air', '用于库存与优惠券测试', 5999.00, 50, 1),
  (3, '测试保温杯', '用于低价商品测试', 99.00, 200, 0);
INSERT INTO coupon(name, type, threshold_amount, value, start_time, end_time, remaining_count) VALUES
  ('满千减百券', 'FULL_REDUCTION', 1000.00, 100.00, '2025-01-01 00:00:00', '2035-01-01 00:00:00', 100),
  ('九折券', 'DISCOUNT', NULL, 0.90, '2025-01-01 00:00:00', '2035-01-01 00:00:00', 100);
INSERT INTO seckill_activity(name, start_time, end_time, status) VALUES
  ('接口测试秒杀活动', '2025-01-01 00:00:00', '2035-01-01 00:00:00', 1);
INSERT INTO seckill_goods(activity_id, product_id, seckill_price, stock, total_stock) VALUES (1, 1, 2999.00, 10, 10);
