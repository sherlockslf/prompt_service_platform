#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""模块5：评估任务基础接口测试。"""

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
    psu = create_psu(ctx, "m5")
    psu_id = psu["id"]

    setup_schema_and_param(ctx, psu_id)
    setup_composition(ctx, psu_id)
    ds = create_dataset(ctx, psu_id, "m5")

    print_step("创建评估任务")
    task = request_json(
        ctx,
        "POST",
        "/evaluations/tasks",
        {"psuId": psu_id, "datasetId": ds["id"], "dimensions": ["relevance", "completeness", "format"]},
    )
    ensure(isinstance(task.get("id"), int), "创建评估任务失败")
    print_ok(f"task_id={task['id']}")

    print_step("执行评估任务")
    done = request_json(ctx, "POST", f"/evaluations/tasks/{task['id']}/run")
    ensure(done.get("failedCases") == 0, f"评估失败数不为0: {done.get('failedCases')}")
    print_ok("evaluation run pass")

    print(json.dumps({"module": "evaluation", "passed": True, "psuId": psu_id}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
