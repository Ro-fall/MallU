-- 日常接口自动化重置脚本。
-- 仅操作当前已 USE 的 mallu 数据库；不会 DROP DATABASE。
USE mallu;

DELETE FROM seckill_order;
DELETE FROM order_item;
DELETE FROM mall_order;
DELETE FROM user_coupon;
DELETE FROM cart_item;
DELETE FROM address;
DELETE FROM seckill_goods;
DELETE FROM seckill_activity;
DELETE FROM coupon;
DELETE FROM product;
DELETE FROM category;
DELETE FROM mall_user;

ALTER TABLE seckill_order AUTO_INCREMENT = 1;
ALTER TABLE order_item AUTO_INCREMENT = 1;
ALTER TABLE mall_order AUTO_INCREMENT = 1;
ALTER TABLE user_coupon AUTO_INCREMENT = 1;
ALTER TABLE cart_item AUTO_INCREMENT = 1;
ALTER TABLE address AUTO_INCREMENT = 1;
ALTER TABLE seckill_goods AUTO_INCREMENT = 1;
ALTER TABLE seckill_activity AUTO_INCREMENT = 1;
ALTER TABLE coupon AUTO_INCREMENT = 1;
ALTER TABLE product AUTO_INCREMENT = 1;
ALTER TABLE category AUTO_INCREMENT = 1;
ALTER TABLE mall_user AUTO_INCREMENT = 1;

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
