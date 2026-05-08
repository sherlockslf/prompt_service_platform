#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PSU 测试公共库。"""

from __future__ import annotations

import json
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any


@dataclass
class TestContext:
    base_url: str = "http://127.0.0.1:8084"
    api_prefix: str = "/api"
    timeout: float = 12.0
    suffix: str = "local"


class TestFailure(RuntimeError):
    pass


def url(ctx: TestContext, path: str, query: dict[str, Any] | None = None) -> str:
    q = ""
    if query:
        q = "?" + urllib.parse.urlencode(query)
    return f"{ctx.base_url.rstrip('/')}{ctx.api_prefix}{path}{q}"


def request_json(
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

    req = urllib.request.Request(url(ctx, path, query), data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=ctx.timeout) as resp:
            raw = resp.read().decode("utf-8")
            if not raw:
                return None
            if "application/json" in resp.headers.get("Content-Type", ""):
                return json.loads(raw)
            return raw
    except urllib.error.HTTPError as e:
        payload = e.read().decode("utf-8", errors="replace")
        raise TestFailure(f"HTTP {e.code} {method} {path}: {payload}") from e
    except urllib.error.URLError as e:
        raise TestFailure(f"URL错误 {method} {path}: {e}") from e


def ensure(cond: bool, message: str) -> None:
    if not cond:
        raise TestFailure(message)


def now_suffix(seed: str) -> str:
    return f"{seed}_{int(time.time())}"


def create_psu(ctx: TestContext, seed: str = "mod") -> dict[str, Any]:
    code = f"psu_{now_suffix(seed)}"
    data = request_json(
        ctx,
        "POST",
        "/psus",
        {"psuId": code, "name": f"PSU-{seed}", "description": "module test"},
    )
    ensure(isinstance(data, dict) and isinstance(data.get("id"), int), "创建PSU失败")
    return data


def setup_schema_and_param(ctx: TestContext, psu_id: int) -> None:
    schema_content = {
        "type": "object",
        "properties": {
            "user": {
                "type": "object",
                "properties": {"name": {"type": "string"}, "intent": {"type": "string"}},
            }
        },
    }
    request_json(
        ctx,
        "PUT",
        f"/schemas/{psu_id}",
        {
            "baseVersionNo": 1,
            "schemaContent": json.dumps(schema_content, ensure_ascii=False),
            "changeLog": "module setup",
        },
    )
    request_json(
        ctx,
        "PUT",
        f"/param-sets/{psu_id}",
        {
            "paramSetContent": json.dumps({"user": {"name": "Alice", "intent": "查余额"}}, ensure_ascii=False),
            "changeLog": "module setup",
        },
    )


def setup_composition(ctx: TestContext, psu_id: int) -> dict[str, Any]:
    comp = request_json(
        ctx,
        "PUT",
        "/compositions",
        {
            "content": "你好 {{user.name}}，诉求：{{user.intent}}",
            "injectionPlan": [{"path": "user.name"}, {"path": "user.intent"}],
            "tokens": [],
            "assembledFragments": [],
        },
        query={"psuId": psu_id},
    )
    ensure(isinstance(comp.get("id"), int), "保存编排失败")
    return comp


def submit_and_approve_review(ctx: TestContext, psu_id: int) -> dict[str, Any]:
    request_json(ctx, "POST", "/compositions/submit", query={"psuId": psu_id})
    review = request_json(ctx, "POST", f"/versions/{psu_id}/submit")
    ensure(isinstance(review.get("id"), int), "提交审核失败")
    approved = request_json(ctx, "POST", f"/versions/{review['id']}/review", {"approved": True})
    ensure(approved.get("status") == "FORMAL", "审核通过失败")
    return review


def create_dataset(ctx: TestContext, psu_id: int, seed: str = "ds") -> dict[str, Any]:
    cases = [
        {"caseId": "case-1", "name": "n1", "input": {"user": {"name": "Bob", "intent": "查账单"}}},
        {"caseId": "case-2", "name": "n2", "input": {"user": {"name": "Cindy", "intent": "查余额"}}},
    ]
    ds = request_json(
        ctx,
        "POST",
        "/test-datasets",
        {
            "name": f"dataset_{now_suffix(seed)}",
            "description": "module test dataset",
            "dataContent": json.dumps(cases, ensure_ascii=False),
        },
        query={"psuId": psu_id},
    )
    ensure(isinstance(ds.get("id"), int), "创建测试集失败")
    return ds


def print_ok(msg: str) -> None:
    print(f"[OK] {msg}")


def print_step(msg: str) -> None:
    print(f"[STEP] {msg}")
