#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""模块4：测试集 + 测试运行基础接口测试。"""

from __future__ import annotations

import argparse
import json

from psu_test_lib import (
    TestContext,
    create_dataset,
    create_psu,
    ensure,
    print_ok,
    print_step,
    request_json,
    setup_composition,
    setup_schema_and_param,
)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8084")
    parser.add_argument("--api-prefix", default="/api", choices=["/api", "/api/v1"])
    parser.add_argument("--timeout", type=float, default=12.0)
    args = parser.parse_args()

    ctx = TestContext(base_url=args.base_url, api_prefix=args.api_prefix, timeout=args.timeout)
    psu = create_psu(ctx, "m4")
    psu_id = psu["id"]

    setup_schema_and_param(ctx, psu_id)
    comp = setup_composition(ctx, psu_id)
    ds = create_dataset(ctx, psu_id, "m4")

    print_step("执行测试运行")
    run = request_json(
        ctx,
        "POST",
        "/test-runs",
        {"compositionId": comp["id"]},
        query={"psuId": psu_id, "datasetId": ds["id"]},
    )
    ensure(isinstance(run.get("runId"), int), "runId不存在")
    ensure(run.get("failedCases") == 0, f"测试运行失败数不为0: {run.get('failedCases')}")
    print_ok("test run pass")

    detail = request_json(ctx, "GET", f"/test-runs/{run['runId']}")
    ensure(detail.get("runId") == run["runId"], "运行详情不匹配")

    print(json.dumps({"module": "dataset_testrun", "passed": True, "psuId": psu_id}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
