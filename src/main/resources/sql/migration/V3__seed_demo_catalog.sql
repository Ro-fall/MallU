-- 为已有本地库补充演示商品、优惠券和秒杀商品；每条均按名称/关联关系防重复插入。

INSERT INTO product (name, description, price, stock, status, main_image)
SELECT 'iPad Air', 'M2 芯片，轻薄便携', 4799.00, 80, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = 'iPad Air');
INSERT INTO product (name, description, price, stock, status, main_image)
SELECT '机械键盘 K87', '热插拔三模机械键盘', 399.00, 300, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '机械键盘 K87');
INSERT INTO product (name, description, price, stock, status, main_image)
SELECT 'MX Master 3S', '静音高精度无线鼠标', 699.00, 150, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = 'MX Master 3S');
INSERT INTO product (name, description, price, stock, status, main_image)
SELECT '小米智能台灯', '支持色温与亮度调节', 199.00, 220, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '小米智能台灯');
INSERT INTO product (name, description, price, stock, status, main_image)
SELECT 'Sony WH-1000XM5', '头戴式降噪耳机', 2499.00, 90, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = 'Sony WH-1000XM5');
INSERT INTO product (name, description, price, stock, status, main_image)
SELECT 'Kindle Paperwhite', '电子书阅读器 16GB', 1199.00, 120, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = 'Kindle Paperwhite');
INSERT INTO product (name, description, price, stock, status, main_image)
SELECT '罗技 C920 摄像头', '1080P 视频通话摄像头', 469.00, 180, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '罗技 C920 摄像头');
INSERT INTO product (name, description, price, stock, status, main_image)
SELECT 'Anker 充电宝', '10000mAh 双向快充', 229.00, 260, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = 'Anker 充电宝');
INSERT INTO product (name, description, price, stock, status, main_image)
SELECT '戴森吹风机 HD15', '高速吹风与智能温控', 3290.00, 60, 1, NULL
WHERE NOT EXISTS (SELECT 1 FROM product WHERE name = '戴森吹风机 HD15');

INSERT INTO coupon (name, type, threshold, discount_value, end_time, total_count, remaining_count)
SELECT '满300减30', 1, 300.00, 30.00, '2030-12-31 23:59:59', 200, 200
WHERE NOT EXISTS (SELECT 1 FROM coupon WHERE name = '满300减30');
INSERT INTO coupon (name, type, threshold, discount_value, end_time, total_count, remaining_count)
SELECT '新用户直减20元', 3, NULL, 20.00, '2030-12-31 23:59:59', 500, 500
WHERE NOT EXISTS (SELECT 1 FROM coupon WHERE name = '新用户直减20元');

INSERT INTO seckill_goods (activity_id, product_id, seckill_price, stock, total_stock)
SELECT a.id, p.id, 3699.00, 20, 20
FROM seckill_activity a JOIN product p ON p.name = 'iPad Air'
WHERE a.name = '618 手机秒杀'
  AND NOT EXISTS (SELECT 1 FROM seckill_goods sg WHERE sg.activity_id = a.id AND sg.product_id = p.id);
INSERT INTO seckill_goods (activity_id, product_id, seckill_price, stock, total_stock)
SELECT a.id, p.id, 1999.00, 30, 30
FROM seckill_activity a JOIN product p ON p.name = 'Sony WH-1000XM5'
WHERE a.name = '618 手机秒杀'
  AND NOT EXISTS (SELECT 1 FROM seckill_goods sg WHERE sg.activity_id = a.id AND sg.product_id = p.id);
