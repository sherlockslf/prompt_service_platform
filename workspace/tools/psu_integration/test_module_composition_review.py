#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""模块3：编排 + 审核基础接口测试。"""

from __future__ import annotations

import argparse
import json

from psu_test_lib import (
    TestContext,
    create_psu,
    ensure,
    print_ok,
    print_step,
    request_json,
    setup_composition,
    setup_schema_and_param,
    submit_and_approve_review,
)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", default="http://127.0.0.1:8084")
    parser.add_argument("--api-prefix", default="/api", choices=["/api", "/api/v1"])
    parser.add_argument("--timeout", type=float, default=12.0)
    args = parser.parse_args()

    ctx = TestContext(base_url=args.base_url, api_prefix=args.api_prefix, timeout=args.timeout)
    psu = create_psu(ctx, "m3")
    psu_id = psu["id"]

    setup_schema_and_param(ctx, psu_id)
    comp = setup_composition(ctx, psu_id)

    print_step("渲染编排")
    render = request_json(
        ctx,
        "POST",
        "/compositions/render",
        {"compositionId": comp["id"], "input": {"user": {"name": "A", "intent": "B"}}},
        query={"psuId": psu_id},
    )
    ensure(render.get("missingVars") in ([], None), "渲染出现缺失变量")
    print_ok("render pass")

    print_step("提审并审核")
    review = submit_and_approve_review(ctx, psu_id)
    ensure(isinstance(review.get("id"), int), "审核链路失败")
    print_ok("review pass")

    latest = request_json(ctx, "GET", f"/compositions/{comp['id']}/revisions/latest")
    ensure(isinstance(latest.get("revisionNo"), int), "最新revision不存在")

    print(json.dumps({"module": "composition_review", "passed": True, "psuId": psu_id}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
