#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""模块2：Schema + ParamSet 基础接口测试。"""

from __future__ import annotations

import argparse
import json

from psu_test_lib import TestContext, create_psu, ensure, print_ok, print_step, request_json, setup_schema_and_param


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8084")
    parser.add_argument("--api-prefix", default="/api", choices=["/api", "/api/v1"])
    parser.add_argument("--timeout", type=float, default=12.0)
    args = parser.parse_args()

    ctx = TestContext(base_url=args.base_url, api_prefix=args.api_prefix, timeout=args.timeout)
    psu = create_psu(ctx, "m2")
    psu_id = psu["id"]

    print_step("初始化Schema和参数集")
    setup_schema_and_param(ctx, psu_id)

    print_step("读取Schema")
    schema = request_json(ctx, "GET", f"/schemas/{psu_id}")
    ensure(schema.get("psuId") == psu_id, "Schema返回psuId不匹配")
    print_ok("schema get pass")

    print_step("读取ParamSet")
    param = request_json(ctx, "GET", f"/param-sets/{psu_id}")
    ensure(param.get("psuId") == psu_id, "ParamSet返回psuId不匹配")
    print_ok("param get pass")

    print(json.dumps({"module": "schema_param", "passed": True, "psuId": psu_id}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
