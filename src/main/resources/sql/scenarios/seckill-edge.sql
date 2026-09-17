-- Execute only after reset-fixtures.sql. Used by SEC-005~008 and SEC-021~023.
USE mallu;

INSERT INTO seckill_activity(name, start_time, end_time, status) VALUES
  ('未开始秒杀活动', '2035-01-01 00:00:00', '2036-01-01 00:00:00', 1),
  ('已结束秒杀活动', '2020-01-01 00:00:00', '2021-01-01 00:00:00', 1),
  ('禁用秒杀活动', '2025-01-01 00:00:00', '2035-01-01 00:00:00', 0);

INSERT INTO seckill_goods(activity_id, product_id, seckill_price, stock, total_stock) VALUES
  (2, 1, 2999.00, 1, 1),
  (3, 1, 2999.00, 1, 1),
  (4, 1, 2999.00, 1, 1);
