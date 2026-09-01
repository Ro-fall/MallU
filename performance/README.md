# MallU 性能测试

## 商品列表读压测

前置条件：MallU 后端已在 `127.0.0.1:8080` 运行。

默认场景：20 并发用户，5 秒爬坡，每用户请求 10 次，共 200 次 `GET /api/products?page=1&size=12`。

```powershell
& 'D:\Software\apache-jmeter-5.6.3\bin\jmeter.bat' -n `
  -t performance\mallu-product-list.jmx `
  -l performance\results\product-list.jtl `
  -e -o performance\results\html
```

可调整负载：`-Jthreads=50 -Jramp=10 -Jloops=20`。

`results/` 中的 JTL 与 HTML 是机器运行结果，不提交 Git；每次压测前请清空其中的旧报告。

## 交易接口说明

下单和秒杀接口包含 JWT、HMAC 签名、nonce 和幂等保护。它们需要先通过登录接口生成用户状态，并为每次请求动态生成签名，不能直接使用静态 JMeter 请求伪造压测。当前项目优先以 API 回归测试覆盖交易正确性；后续可用 JMeter 的 JSR223/Groovy 预处理器接入动态签名，再做交易吞吐压测。
