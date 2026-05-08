#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PSU 后端接口全流程集成测试脚本。
覆盖链路：PSU -> Schema -> ParamSet -> Composition -> VersionReview -> TestRun -> Evaluation -> Release -> Resolve。
"""

from __future__ import annotations

import argparse
import json
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any


@dataclass
class Ctx:
    base_url: str
    api_prefix: str
    timeout: float


def _url(ctx: Ctx, path: str, query: dict[str, Any] | None = None) -> str:
    q = ""
    if query:
        q = "?" + urllib.parse.urlencode(query)
    return f"{ctx.base_url}{ctx.api_prefix}{path}{q}"


def _request(ctx: Ctx, method: str, path: str, body: Any | None = None, query: dict[str, Any] | None = None) -> Any:
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(_url(ctx, path, query), data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=ctx.timeout) as resp:
            raw = resp.read().decode("utf-8")
            if not raw:
                return None
            ctype = resp.headers.get("Content-Type", "")
            if "application/json" in ctype:
                return json.loads(raw)
            return raw
    except urllib.error.HTTPError as e:
        payload = e.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {e.code} {method} {path} 失败: {payload}") from e
    except urllib.error.URLError as e:
        raise RuntimeError(f"请求失败 {method} {path}: {e}") from e


def _assert(cond: bool, msg: str) -> None:
    if not cond:
        raise AssertionError(msg)


def _step(name: str) -> None:
    print(f"\n[STEP] {name}")


def _ok(msg: str) -> None:
    print(f"[OK]   {msg}")


def run_flow(ctx: Ctx, suffix: str) -> None:
    now = int(time.time())
    psu_code = f"psu_it_{suffix}_{now}"

    _step("创建 PSU")
    psu = _request(
        ctx,
        "POST",
        "/psus",
        {
            "psuId": psu_code,
            "name": f"PSU 集成测试 {suffix}",
            "description": "integration test flow",
        },
    )
    psu_id = psu.get("id")
    _assert(isinstance(psu_id, int), "创建PSU后未返回 id")
    _ok(f"psu_id={psu_id}, psuId={psu.get('psuId')}")

    _step("维护 Schema")
    schema_content = {
        "type": "object",
        "properties": {
            "user": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "intent": {"type": "string"}
                }
            }
        }
    }
    schema = _request(
        ctx,
        "PUT",
        f"/schemas/{psu_id}",
        {
            "baseVersionNo": 1,
            "schemaContent": json.dumps(schema_content, ensure_ascii=False),
            "changeLog": "init schema for integration test",
        },
    )
    _assert(schema.get("psuId") == psu_id, "Schema 更新返回 psuId 不匹配")
    _ok("Schema 更新成功")

    _step("维护参数集")
    param_content = {"user": {"name": "Alice", "intent": "查询账户余额"}}
    param_set = _request(
        ctx,
        "PUT",
        f"/param-sets/{psu_id}",
        {
            "paramSetContent": json.dumps(param_content, ensure_ascii=False),
            "changeLog": "init param set for integration test",
        },
    )
    _assert(param_set.get("psuId") == psu_id, "参数集更新返回 psuId 不匹配")
    _ok("参数集更新成功")

    _step("保存编排草稿")
    content = "你好 {{user.name}}，你的诉求是：{{user.intent}}"
    comp = _request(
        ctx,
        "PUT",
        "/compositions",
        {
            "content": content,
            "injectionPlan": [{"path": "user.name"}, {"path": "user.intent"}],
            "tokens": [],
            "assembledFragments": [],
        },
        query={"psuId": psu_id},
    )
    comp_id = comp.get("id")
    _assert(isinstance(comp_id, int), "保存编排后未返回 composition id")
    _ok(f"composition_id={comp_id}")

    _step("渲染编排")
    render = _request(
        ctx,
        "POST",
        "/compositions/render",
        {"compositionId": comp_id, "input": param_content},
        query={"psuId": psu_id},
    )
    _assert(render.get("missingVars") == [] or render.get("missingVars") is None, "渲染存在缺失变量")
    _ok(f"rendered={render.get('renderedPrompt')}")

    _step("提交编排")
    submitted = _request(ctx, "POST", "/compositions/submit", query={"psuId": psu_id})
    _assert(submitted.get("status") == "CANDIDATE", "编排提交后状态不是 CANDIDATE")
    _ok("编排提交成功")

    _step("提交版本审核")
    review = _request(ctx, "POST", f"/versions/{psu_id}/submit")
    review_id = review.get("id")
    _assert(isinstance(review_id, int), "提交审核未返回 review id")
    _ok(f"review_id={review_id}")

    _step("审核通过")
    reviewed = _request(
        ctx,
        "POST",
        f"/versions/{review_id}/review",
        {"approved": True},
    )
    _assert(reviewed.get("status") == "FORMAL", "审核通过后状态不是 FORMAL")
    _ok("审核通过成功")

    _step("读取编排最新快照")
    latest_rev = _request(ctx, "GET", f"/compositions/{comp_id}/revisions/latest")
    rev_no = latest_rev.get("revisionNo")
    _assert(isinstance(rev_no, int), "未获取到 revisionNo")
    _ok(f"revisionNo={rev_no}")

    _step("创建测试集")
    dataset_cases = [
        {"caseId": "case-1", "name": "normal", "input": {"user": {"name": "Bob", "intent": "查账单"}}},
        {"caseId": "case-2", "name": "normal2", "input": {"user": {"name": "Cindy", "intent": "查余额"}}},
    ]
    dataset = _request(
        ctx,
        "POST",
        "/test-datasets",
        {
            "name": f"dataset-it-{suffix}-{now}",
            "description": "integration test dataset",
            "dataContent": json.dumps(dataset_cases, ensure_ascii=False),
        },
        query={"psuId": psu_id},
    )
    dataset_id = dataset.get("id")
    _assert(isinstance(dataset_id, int), "创建测试集未返回 id")
    _ok(f"dataset_id={dataset_id}")

    _step("运行测试集")
    run = _request(
        ctx,
        "POST",
        "/test-runs",
        {"compositionId": comp_id},
        query={"psuId": psu_id, "datasetId": dataset_id},
    )
    run_id = run.get("runId")
    _assert(isinstance(run_id, int), "测试运行未返回 runId")
    _assert(run.get("failedCases", 0) == 0, f"测试运行存在失败: {run.get('failedCases')}")
    _ok(f"run_id={run_id}, status={run.get('status')}")

    _step("创建并执行评估任务")
    task = _request(
        ctx,
        "POST",
        "/evaluations/tasks",
        {"psuId": psu_id, "datasetId": dataset_id, "dimensions": ["relevance", "completeness", "format"]},
    )
    task_id = task.get("id")
    _assert(isinstance(task_id, int), "评估任务未返回 task id")
    task_run = _request(ctx, "POST", f"/evaluations/tasks/{task_id}/run")
    _assert(task_run.get("failedCases", 0) == 0, f"评估任务存在失败: {task_run.get('failedCases')}")
    _ok(f"task_id={task_id}, report_id={task_run.get('reportId')}")

    _step("创建发布单并执行发布")
    release = _request(
        ctx,
        "POST",
        "/releases",
        {
            "psuId": psu_id,
            "environment": "DEV",
            "releaseType": "FULL",
            "targetCompositionId": comp_id,
            "targetRevisionNo": rev_no,
        },
    )
    release_id = release.get("id")
    _assert(isinstance(release_id, int), "创建发布单未返回 id")
    _request(ctx, "POST", f"/releases/{release_id}/submit")
    _request(ctx, "POST", f"/releases/{release_id}/approve")
    release_done = _request(ctx, "POST", f"/releases/{release_id}/execute")
    _assert(release_done.get("status") == "SUCCESS", "发布执行后状态不是 SUCCESS")
    _ok(f"release_id={release_id}")

    _step("调用对外 resolve")
    resolved = _request(
        ctx,
        "POST",
        "/prompt-service/resolve",
        {
            "psuId": psu_id,
            "environment": "DEV",
            "context": {"tenantId": "t1", "userId": "u1", "traceId": "tr-it"},
        },
    )
    _assert(resolved.get("routeType") in {"STABLE", "CANARY"}, "resolve 返回 routeType 非预期")
    _assert(isinstance(resolved.get("revisionNo"), int), "resolve 未返回 revisionNo")
    _ok(f"resolve routeType={resolved.get('routeType')}, revisionNo={resolved.get('revisionNo')}")

    print("\n[DONE] PSU 全流程集成测试通过")


def main() -> None:
    parser = argparse.ArgumentParser(description="PSU 后端全流程集成测试")
    parser.add_argument("--base-url", default="http://127.0.0.1:8084", help="后端地址，默认 http://127.0.0.1:8084")
    parser.add_argument("--api-prefix", default="/api", choices=["/api", "/api/v1"], help="接口前缀")
    parser.add_argument("--timeout", type=float, default=10.0, help="单请求超时秒数")
    parser.add_argument("--suffix", default="local", help="测试数据后缀")
    args = parser.parse_args()

    ctx = Ctx(base_url=args.base_url.rstrip("/"), api_prefix=args.api_prefix, timeout=args.timeout)
    try:
        run_flow(ctx, args.suffix)
    except Exception as ex:
        print(f"\n[FAILED] {ex}")
        sys.exit(1)


if __name__ == "__main__":
    main()
