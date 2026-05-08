#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""模块6：发布 + resolve 基础接口测试。"""

from __future__ import annotations

import argparse
import json
import time

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
    psu = create_psu(ctx, "m6")
    psu_id = psu["id"]

    setup_schema_and_param(ctx, psu_id)
    comp = setup_composition(ctx, psu_id)
    submit_and_approve_review(ctx, psu_id)

    latest = request_json(ctx, "GET", f"/compositions/{comp['id']}/revisions/latest")
    rev_no = latest.get("revisionNo")
    ensure(isinstance(rev_no, int), "未拿到revisionNo")

    print_step("创建发布单")
    release = request_json(
        ctx,
        "POST",
        "/releases",
        {
            "psuId": psu_id,
            "environment": "DEV",
            "releaseType": "FULL",
            "targetCompositionId": comp["id"],
            "targetRevisionNo": rev_no,
        },
    )
    ensure(isinstance(release.get("id"), int), "发布单创建失败")
    rid = release["id"]

    request_json(ctx, "POST", f"/releases/{rid}/submit")
    request_json(ctx, "POST", f"/releases/{rid}/approve")
    done = request_json(ctx, "POST", f"/releases/{rid}/execute")
    ensure(done.get("status") == "SUCCESS", f"发布执行状态异常: {done.get('status')}")
    print_ok("release execute pass")

    print_step("调用resolve")
    resolve = request_json(
        ctx,
        "POST",
        "/prompt-service/resolve",
        {
            "psuId": psu_id,
            "environment": "DEV",
            "context": {"tenantId": "t1", "userId": "u1", "traceId": f"tr-{int(time.time())}"},
        },
    )
    ensure(resolve.get("routeType") in {"STABLE", "CANARY"}, "resolve routeType 非法")
    ensure(isinstance(resolve.get("revisionNo"), int), "resolve revisionNo 非法")
    print_ok("resolve pass")

    print(json.dumps({"module": "release_resolve", "passed": True, "psuId": psu_id}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
