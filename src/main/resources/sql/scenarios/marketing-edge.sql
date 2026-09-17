-- Execute only after reset-fixtures.sql. Used by MKT-004~007 and MKT-014.
USE mallu;

INSERT INTO coupon(name, type, threshold_amount, value, start_time, end_time, remaining_count, status) VALUES
  ('未开始测试券', 'FULL_REDUCTION', 1.00, 1.00, '2035-01-01 00:00:00', '2036-01-01 00:00:00', 1, 1),
  ('已结束测试券', 'FULL_REDUCTION', 1.00, 1.00, '2020-01-01 00:00:00', '2021-01-01 00:00:00', 1, 1),
  ('已禁用测试券', 'FULL_REDUCTION', 1.00, 1.00, '2025-01-01 00:00:00', '2035-01-01 00:00:00', 1, 0),
  ('已领完测试券', 'FULL_REDUCTION', 1.00, 1.00, '2025-01-01 00:00:00', '2035-01-01 00:00:00', 0, 1),
  ('直接减免测试券', 'DIRECT', NULL, 9999.00, '2025-01-01 00:00:00', '2035-01-01 00:00:00', 1, 1);
