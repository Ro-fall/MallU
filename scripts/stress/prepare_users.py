#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JMeter 压测数据准备脚本：
1. 批量注册压测用户
2. 登录获取 JWT token
3. 创建收货地址
4. 输出 users.csv（username,token,addressId）供 JMeter CSV 驱动
"""
import csv
import json
import sys
import time
import urllib.request
import urllib.error

BASE_URL = "http://localhost:8080"
COUNT = int(sys.argv[1]) if len(sys.argv) > 1 else 50
PREFIX = "jmeter"
OUTPUT = "users.csv"


def http(method, path, body=None, token=None, timeout=10):
    url = BASE_URL + path
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))


def main():
    rows = []
    for i in range(1, COUNT + 1):
        username = f"{PREFIX}{i}"
        password = "pass123456"
        # 注册（已存在则忽略错误）
        reg = http("POST", "/api/users/register",
                   {"username": username, "password": password,
                    "phone": f"139{i:08d}", "email": f"{username}@mallu.com"})
        if reg.get("code") != 200 and reg.get("code") != 1001:
            print(f"[{i}] 注册失败: {reg}")
            continue
        # 登录
        login = http("POST", "/api/users/login", {"username": username, "password": password})
        if login.get("code") != 200:
            print(f"[{i}] 登录失败: {login}")
            continue
        token = login["data"]["token"]
        # 创建地址
        addr = http("POST", "/api/addresses",
                    {"receiverName": f"压测{i}", "phone": "13800138000",
                     "province": "浙江省", "city": "杭州市", "district": "西湖区",
                     "detailAddress": f"压测地址{i}号", "isDefault": 1}, token=token)
        if addr.get("code") != 200:
            print(f"[{i}] 创建地址失败: {addr}")
            continue
        address_id = addr["data"]["id"]
        rows.append([username, token, address_id])
        if i % 10 == 0:
            print(f"已准备 {i} 个用户")
        time.sleep(0.05)

    with open(OUTPUT, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["username", "token", "addressId"])
        writer.writerows(rows)
    print(f"完成，共 {len(rows)} 个用户，输出到 {OUTPUT}")


if __name__ == "__main__":
    main()
