#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PSU 接口集成测试用例逻辑（半自动化）。"""

from __future__ import annotations

import json
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from typing import Any


@dataclass
class TestContext:
    base_url: str
    api_prefix: str
    timeout: float
    suffix: str
    continue_on_failure: bool
    state: dict[str, Any] = field(default_factory=dict)


@dataclass
class CaseResult:
    name: str
    method: str
    path: str
    request_payload: Any
    expected: str
    actual: Any
    passed: bool
    reason: str


def build_url(ctx: TestContext, path: str, query: dict[str, Any] | None = None) -> str:
    q = ""
    if query:
        q = "?" + urllib.parse.urlencode(query)
    return f"{ctx.base_url}{ctx.api_prefix}{path}{q}"


def http_request(
    ctx: TestContext,
    method: str,
    path: str,
    body: Any | None = None,
    query: dict[str, Any] | None = None,
) -> Any:
    data = None
    headers = {"Accept": "application/json"}
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(build_url(ctx, path, query), data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=ctx.timeout) as resp:
        raw = resp.read().decode("utf-8")
        if not raw:
            return None
        if "application/json" in resp.headers.get("Content-Type", ""):
            return json.loads(raw)
        return raw


def run_case(
    ctx: TestContext,
    name: str,
    method: str,
    path: str,
    expected: str,
    body: Any | None = None,
    query: dict[str, Any] | None = None,
    validator=None,
) -> CaseResult:
    req_payload = {
        "query": query,
        "body": body,
    }
    try:
        actual = http_request(ctx, method, path, body=body, query=query)
        if validator:
            ok, reason = validator(actual)
        else:
            ok, reason = True, "未设置校验器，默认通过"
        return CaseResult(
            name=name,
            method=method,
            path=path,
            request_payload=req_payload,
            expected=expected,
            actual=actual,
            passed=ok,
            reason=reason,
        )
    except urllib.error.HTTPError as e:
        payload = e.read().decode("utf-8", errors="replace")
        return CaseResult(
            name=name,
            method=method,
            path=path,
            request_payload=req_payload,
            expected=expected,
            actual={"http_status": e.code, "body": payload},
            passed=False,
            reason=f"HTTP错误: {e.code}",
        )
    except Exception as ex:  # noqa: BLE001
        return CaseResult(
            name=name,
            method=method,
            path=path,
            request_payload=req_payload,
            expected=expected,
            actual=str(ex),
            passed=False,
            reason=f"请求异常: {ex}",
        )


def _is_int(v: Any) -> bool:
    return isinstance(v, int)


def _must_have_id(key: str):
    def _validator(data: Any):
        value = data.get(key) if isinstance(data, dict) else None
        ok = _is_int(value)
        return ok, (f"返回包含整数{key}={value}" if ok else f"返回缺少整数{key}")

    return _validator


def build_cases(ctx: TestContext):
    now = int(time.time())
    psu_code = f"psu_it_{ctx.suffix}_{now}"

    def v_psu_created(data: Any):
        ok = isinstance(data, dict) and _is_int(data.get("id")) and data.get("psuId") == psu_code
        if ok:
            ctx.state["psu_id"] = data["id"]
            return True, f"PSU创建成功，id={data['id']}"
        return False, "PSU创建返回不符合预期"

    def v_schema_updated(data: Any):
        psu_id = ctx.state.get("psu_id")
        ok = isinstance(data, dict) and data.get("psuId") == psu_id
        return ok, ("Schema更新成功" if ok else "Schema更新返回psuId不匹配")

    def v_param_updated(data: Any):
        psu_id = ctx.state.get("psu_id")
        ok = isinstance(data, dict) and data.get("psuId") == psu_id
        return ok, ("参数集更新成功" if ok else "参数集更新返回psuId不匹配")

    def v_comp_saved(data: Any):
        ok = isinstance(data, dict) and _is_int(data.get("id"))
        if ok:
            ctx.state["composition_id"] = data["id"]
            return True, f"编排保存成功，composition_id={data['id']}"
        return False, "编排保存返回缺少id"

    def v_rendered(data: Any):
        mv = data.get("missingVars") if isinstance(data, dict) else None
        ok = mv == [] or mv is None
        return ok, ("渲染无缺失变量" if ok else f"存在缺失变量: {mv}")

    def v_submitted(data: Any):
        ok = isinstance(data, dict) and data.get("status") == "CANDIDATE"
        return ok, ("编排提交成功" if ok else f"提交后状态异常: {data.get('status') if isinstance(data, dict) else data}")

    def v_review_submitted(data: Any):
        ok = isinstance(data, dict) and _is_int(data.get("id"))
        if ok:
            ctx.state["review_id"] = data["id"]
            return True, f"版本提审成功，review_id={data['id']}"
        return False, "版本提审返回缺少id"

    def v_review_approved(data: Any):
        ok = isinstance(data, dict) and data.get("status") == "FORMAL"
        return ok, ("审核通过成功" if ok else f"审核状态异常: {data.get('status') if isinstance(data, dict) else data}")

    def v_latest_revision(data: Any):
        rev = data.get("revisionNo") if isinstance(data, dict) else None
        ok = _is_int(rev)
        if ok:
            ctx.state["revision_no"] = rev
            return True, f"读取快照成功，revisionNo={rev}"
        return False, "快照返回缺少revisionNo"

    def v_dataset_created(data: Any):
        ok = isinstance(data, dict) and _is_int(data.get("id"))
        if ok:
            ctx.state["dataset_id"] = data["id"]
            return True, f"测试集创建成功，dataset_id={data['id']}"
        return False, "测试集创建返回缺少id"

    def v_test_run(data: Any):
        if not isinstance(data, dict):
            return False, "测试运行返回非JSON对象"
        run_id = data.get("runId")
        failed = data.get("failedCases")
        ok = _is_int(run_id) and failed == 0
        if ok:
            ctx.state["run_id"] = run_id
            return True, f"测试运行成功，run_id={run_id}"
        return False, f"测试运行异常，runId={run_id}, failedCases={failed}"

    def v_eval_task(data: Any):
        ok = isinstance(data, dict) and _is_int(data.get("id"))
        if ok:
            ctx.state["task_id"] = data["id"]
            return True, f"评估任务创建成功，task_id={data['id']}"
        return False, "评估任务创建返回缺少id"

    def v_eval_run(data: Any):
        if not isinstance(data, dict):
            return False, "评估执行返回非JSON对象"
        ok = data.get("failedCases") == 0
        if ok:
            ctx.state["report_id"] = data.get("reportId")
            return True, f"评估执行成功，report_id={data.get('reportId')}"
        return False, f"评估执行存在失败，failedCases={data.get('failedCases')}"

    def v_release_created(data: Any):
        ok = isinstance(data, dict) and _is_int(data.get("id"))
        if ok:
            ctx.state["release_id"] = data["id"]
            return True, f"发布单创建成功，release_id={data['id']}"
        return False, "发布单创建返回缺少id"

    def v_simple_ok(data: Any):
        return isinstance(data, dict), "接口调用成功"

    def v_release_executed(data: Any):
        ok = isinstance(data, dict) and data.get("status") == "SUCCESS"
        return ok, ("发布执行成功" if ok else f"发布执行状态异常: {data.get('status') if isinstance(data, dict) else data}")

    def v_resolve(data: Any):
        if not isinstance(data, dict):
            return False, "resolve返回非JSON对象"
        route = data.get("routeType")
        rev = data.get("revisionNo")
        ok = route in {"STABLE", "CANARY"} and _is_int(rev)
        return ok, (f"resolve成功，routeType={route}, revisionNo={rev}" if ok else f"resolve返回异常: {data}")

    def psu_id_query():
        return {"psuId": ctx.state["psu_id"]}

    def comp_id():
        return ctx.state["composition_id"]

    def review_id():
        return ctx.state["review_id"]

    def dataset_id():
        return ctx.state["dataset_id"]

    def task_id():
        return ctx.state["task_id"]

    def release_id():
        return ctx.state["release_id"]

    schema_content = {
        "type": "object",
        "properties": {
            "user": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "intent": {"type": "string"},
                },
            }
        },
    }

    param_content = {"user": {"name": "Alice", "intent": "查询账户余额"}}

    dataset_cases = [
        {"caseId": "case-1", "name": "normal", "input": {"user": {"name": "Bob", "intent": "查账单"}}},
        {"caseId": "case-2", "name": "normal2", "input": {"user": {"name": "Cindy", "intent": "查余额"}}},
    ]

    return [
        {
            "name": "创建PSU",
            "method": "POST",
            "path": "/psus",
            "query": lambda: None,
            "body": lambda: {
                "psuId": psu_code,
                "name": f"PSU 集成测试 {ctx.suffix}",
                "description": "integration test flow",
            },
            "expected": "返回包含id，且psuId与请求一致",
            "validator": v_psu_created,
        },
        {
            "name": "更新Schema",
            "method": "PUT",
            "path": lambda: f"/schemas/{ctx.state['psu_id']}",
            "query": lambda: None,
            "body": lambda: {
                "baseVersionNo": 1,
                "schemaContent": json.dumps(schema_content, ensure_ascii=False),
                "changeLog": "init schema for integration test",
            },
            "expected": "返回psuId与当前PSU一致",
            "validator": v_schema_updated,
        },
        {
            "name": "更新参数集",
            "method": "PUT",
            "path": lambda: f"/param-sets/{ctx.state['psu_id']}",
            "query": lambda: None,
            "body": lambda: {
                "paramSetContent": json.dumps(param_content, ensure_ascii=False),
                "changeLog": "init param set for integration test",
            },
            "expected": "返回psuId与当前PSU一致",
            "validator": v_param_updated,
        },
        {
            "name": "保存编排",
            "method": "PUT",
            "path": "/compositions",
            "query": psu_id_query,
            "body": lambda: {
                "content": "你好 {{user.name}}，你的诉求是：{{user.intent}}",
                "injectionPlan": [{"path": "user.name"}, {"path": "user.intent"}],
                "tokens": [],
                "assembledFragments": [],
            },
            "expected": "返回composition id",
            "validator": v_comp_saved,
        },
        {
            "name": "渲染编排",
            "method": "POST",
            "path": "/compositions/render",
            "query": psu_id_query,
            "body": lambda: {"compositionId": comp_id(), "input": param_content},
            "expected": "missingVars为空",
            "validator": v_rendered,
        },
        {
            "name": "提交编排",
            "method": "POST",
            "path": "/compositions/submit",
            "query": psu_id_query,
            "body": lambda: None,
            "expected": "status=CANDIDATE",
            "validator": v_submitted,
        },
        {
            "name": "提交版本审核",
            "method": "POST",
            "path": lambda: f"/versions/{ctx.state['psu_id']}/submit",
            "query": lambda: None,
            "body": lambda: None,
            "expected": "返回review id",
            "validator": v_review_submitted,
        },
        {
            "name": "审核通过",
            "method": "POST",
            "path": lambda: f"/versions/{review_id()}/review",
            "query": lambda: None,
            "body": lambda: {"approved": True},
            "expected": "status=FORMAL",
            "validator": v_review_approved,
        },
        {
            "name": "读取最新快照",
            "method": "GET",
            "path": lambda: f"/compositions/{comp_id()}/revisions/latest",
            "query": lambda: None,
            "body": lambda: None,
            "expected": "返回revisionNo",
            "validator": v_latest_revision,
        },
        {
            "name": "创建测试集",
            "method": "POST",
            "path": "/test-datasets",
            "query": psu_id_query,
            "body": lambda: {
                "name": f"dataset-it-{ctx.suffix}-{now}",
                "description": "integration test dataset",
                "dataContent": json.dumps(dataset_cases, ensure_ascii=False),
            },
            "expected": "返回dataset id",
            "validator": v_dataset_created,
        },
        {
            "name": "运行测试集",
            "method": "POST",
            "path": "/test-runs",
            "query": lambda: {"psuId": ctx.state["psu_id"], "datasetId": dataset_id()},
            "body": lambda: {"compositionId": comp_id()},
            "expected": "返回runId且failedCases=0",
            "validator": v_test_run,
        },
        {
            "name": "创建评估任务",
            "method": "POST",
            "path": "/evaluations/tasks",
            "query": lambda: None,
            "body": lambda: {
                "psuId": ctx.state["psu_id"],
                "datasetId": dataset_id(),
                "dimensions": ["relevance", "completeness", "format"],
            },
            "expected": "返回task id",
            "validator": v_eval_task,
        },
        {
            "name": "执行评估任务",
            "method": "POST",
            "path": lambda: f"/evaluations/tasks/{task_id()}/run",
            "query": lambda: None,
            "body": lambda: None,
            "expected": "failedCases=0",
            "validator": v_eval_run,
        },
        {
            "name": "创建发布单",
            "method": "POST",
            "path": "/releases",
            "query": lambda: None,
            "body": lambda: {
                "psuId": ctx.state["psu_id"],
                "environment": "DEV",
                "releaseType": "FULL",
                "targetCompositionId": comp_id(),
                "targetRevisionNo": ctx.state["revision_no"],
            },
            "expected": "返回release id",
            "validator": v_release_created,
        },
        {
            "name": "提交发布单",
            "method": "POST",
            "path": lambda: f"/releases/{release_id()}/submit",
            "query": lambda: None,
            "body": lambda: None,
            "expected": "接口成功",
            "validator": v_simple_ok,
        },
        {
            "name": "审批发布单",
            "method": "POST",
            "path": lambda: f"/releases/{release_id()}/approve",
            "query": lambda: None,
            "body": lambda: None,
            "expected": "接口成功",
            "validator": v_simple_ok,
        },
        {
            "name": "执行发布单",
            "method": "POST",
            "path": lambda: f"/releases/{release_id()}/execute",
            "query": lambda: None,
            "body": lambda: None,
            "expected": "status=SUCCESS",
            "validator": v_release_executed,
        },
        {
            "name": "调用resolve",
            "method": "POST",
            "path": "/prompt-service/resolve",
            "query": lambda: None,
            "body": lambda: {
                "psuId": ctx.state["psu_id"],
                "environment": "DEV",
                "context": {"tenantId": "t1", "userId": "u1", "traceId": f"trace-{now}"},
            },
            "expected": "routeType in {STABLE,CANARY} 且 revisionNo 为整数",
            "validator": v_resolve,
        },
    ]


def execute_cases(ctx: TestContext) -> list[CaseResult]:
    results: list[CaseResult] = []
    for spec in build_cases(ctx):
        name = spec["name"]
        method = spec["method"]
        path = spec["path"]() if callable(spec["path"]) else spec["path"]
        query = spec["query"]() if callable(spec["query"]) else spec["query"]
        body = spec["body"]() if callable(spec["body"]) else spec["body"]
        result = run_case(
            ctx=ctx,
            name=name,
            method=method,
            path=path,
            expected=spec["expected"],
            body=body,
            query=query,
            validator=spec["validator"],
        )
        results.append(result)
        if not result.passed and not ctx.continue_on_failure:
            break
    return results
