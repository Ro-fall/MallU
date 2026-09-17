-- Execute only after reset-fixtures.sql. Used by CAT-020~023 and CAT-026.
USE mallu;

INSERT INTO product(category_id, name, description, price, stock, status, recommended)
VALUES
  (1, '下架测试商品', '不应出现在商品列表、搜索或推荐结果', 1.00, 10, 0, 1),
  (3, '零库存测试商品', '详情仍可读取，库存为零', 9.90, 0, 1, 0);
