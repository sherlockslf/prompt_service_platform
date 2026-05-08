#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""模块1：PSU 基础接口测试。"""

from __future__ import annotations

import argparse
import json
import sys

from psu_test_lib import TestContext, create_psu, ensure, print_ok, print_step, request_json


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8084")
    parser.add_argument("--api-prefix", default="/api", choices=["/api", "/api/v1"])
    parser.add_argument("--timeout", type=float, default=12.0)
    args = parser.parse_args()

    ctx = TestContext(base_url=args.base_url, api_prefix=args.api_prefix, timeout=args.timeout)

    print_step("创建PSU")
    psu = create_psu(ctx, "m1")
    psu_id = psu["id"]
    print_ok(f"created psu_id={psu_id}")

    print_step("查询PSU详情")
    detail = request_json(ctx, "GET", f"/psus/{psu_id}")
    ensure(detail.get("id") == psu_id, "PSU详情ID不匹配")
    print_ok("detail check pass")

    print_step("分页查询PSU")
    page = request_json(ctx, "GET", "/psus", query={"page": 1, "size": 10})
    ensure("content" in page, "分页返回缺少content")
    print_ok("page query pass")

    print(json.dumps({"module": "psu", "passed": True, "psuId": psu_id}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as ex:
        print(json.dumps({"module": "psu", "passed": False, "reason": str(ex)}, ensure_ascii=False))
        raise
