#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
MallU 秒杀压测脚本（Python 版，Windows/Linux 均可运行）
使用线程池模拟并发请求，统计 TPS / 响应时间 / 超卖。

用法:
    python stress_seckill.py [并发数] [商品ID]
"""
import csv
import hashlib
import hmac
import json
import statistics
import sys
import time
import urllib.request
import urllib.error
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed

BASE_URL = "http://localhost:8080"
SECRET = "MallU-Secret-Key-2026"
THREADS = int(sys.argv[1]) if len(sys.argv) > 1 else 100
GOODS_ID = int(sys.argv[2]) if len(sys.argv) > 2 else 1
CSV_FILE = "users.csv"


def make_sign(address_id, timestamp, nonce):
    to_sign = f"addressId={address_id}&timestamp={timestamp}&nonce={nonce}"
    return hmac.new(SECRET.encode(), to_sign.encode(), hashlib.sha256).hexdigest()


def seckill(row):
    username, token, address_id = row
    timestamp = int(time.time() * 1000)
    nonce = uuid.uuid4().hex
    sign = make_sign(address_id, timestamp, nonce)

    url = f"{BASE_URL}/api/seckill/goods/{GOODS_ID}/seckill?addressId={address_id}"
    req = urllib.request.Request(url, method="POST")
    req.add_header("Authorization", "Bearer " + token)
    req.add_header("Content-Type", "application/json")
    req.add_header("X-Timestamp", str(timestamp))
    req.add_header("X-Nonce", nonce)
    req.add_header("X-Sign", sign)

    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            body = json.loads(resp.read().decode("utf-8"))
            elapsed = (time.perf_counter() - start) * 1000
            return {"code": body.get("code"), "message": body.get("message"), "elapsed": elapsed}
    except urllib.error.HTTPError as e:
        elapsed = (time.perf_counter() - start) * 1000
        try:
            body = json.loads(e.read().decode("utf-8"))
            return {"code": body.get("code"), "message": body.get("message"), "elapsed": elapsed}
        except Exception:
            return {"code": e.code, "message": "HTTP " + str(e.code), "elapsed": elapsed}
    except Exception as e:
        elapsed = (time.perf_counter() - start) * 1000
        return {"code": -1, "message": str(e), "elapsed": elapsed}


def main():
    with open(CSV_FILE, encoding="utf-8") as f:
        reader = csv.reader(f)
        next(reader)
        rows = [r for r in reader if len(r) == 3]
    rows = rows[:THREADS]
    print(f"压测配置: 并发 {len(rows)}, 商品ID {GOODS_ID}, 请求数 {len(rows)}")

    results = []
    start = time.perf_counter()
    with ThreadPoolExecutor(max_workers=THREADS) as pool:
        futures = [pool.submit(seckill, row) for row in rows]
        for i, fut in enumerate(as_completed(futures)):
            results.append(fut.result())
            if (i + 1) % 50 == 0:
                print(f"  完成 {i + 1}/{len(rows)}")
    total_time = time.perf_counter() - start

    # 统计
    codes = {}
    for r in results:
        codes[r["code"]] = codes.get(r["code"], 0) + 1
    elapsed = [r["elapsed"] for r in results]

    print("\n========== 压测结果 ==========")
    print(f"总请求: {len(results)}")
    print(f"总耗时: {total_time:.2f}s")
    print(f"TPS: {len(results) / total_time:.1f}")
    if elapsed:
        elapsed.sort()
        p95 = elapsed[int(len(elapsed) * 0.95) - 1]
        print(f"平均响应: {statistics.mean(elapsed):.1f}ms")
        print(f"P50: {statistics.median(elapsed):.1f}ms")
        print(f"P95: {p95:.1f}ms")
        print(f"最大: {max(elapsed):.1f}ms")
    print("业务码分布:")
    for code, cnt in sorted(codes.items()):
        label = {200: "抢购成功(排队)", -1: "网络错误", 6001: "限流", 7003: "已购买", 7004: "售罄", 7001: "活动不存在", 401: "签名/鉴权失败"}.get(code, "其他")
        print(f"  {code} ({label}): {cnt}")

    success = codes.get(200, 0)
    print(f"\n抢购成功: {success}，成功率 {success / len(results) * 100:.1f}%")
    print("提示: 订单由异步线程落库，等待 3 秒后可查 seckill_order 表验证无超卖")
    time.sleep(3)


if __name__ == "__main__":
    main()
