#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""PSU 接口集成测试执行器。"""

from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime
from pathlib import Path

from psu_api_case_logic import CaseResult, TestContext, execute_cases


def print_result(index: int, item: CaseResult) -> None:
    flag = "PASS" if item.passed else "FAIL"
    print(f"[{index:02d}] [{flag}] {item.name}")
    print(f"     接口: {item.method} {item.path}")
    print(f"     预期: {item.expected}")
    print(f"     结论: {item.reason}")


def save_report(results: list[CaseResult], output_file: Path) -> None:
    payload = {
        "generatedAt": datetime.now().isoformat(),
        "total": len(results),
        "passed": sum(1 for x in results if x.passed),
        "failed": sum(1 for x in results if not x.passed),
        "items": [
            {
                "name": x.name,
                "method": x.method,
                "path": x.path,
                "request": x.request_payload,
                "expected": x.expected,
                "actual": x.actual,
                "passed": x.passed,
                "reason": x.reason,
            }
            for x in results
        ],
    }
    output_file.parent.mkdir(parents=True, exist_ok=True)
    output_file.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description="PSU 接口全流程半自动化测试")
    parser.add_argument("--base-url", default="http://127.0.0.1:8084")
    parser.add_argument("--api-prefix", default="/api", choices=["/api", "/api/v1"])
    parser.add_argument("--timeout", type=float, default=12.0)
    parser.add_argument("--suffix", default="local")
    parser.add_argument("--continue-on-failure", action="store_true")
    parser.add_argument("--report", default="workspace/tools/psu_integration/reports/latest-report.json")
    args = parser.parse_args()

    ctx = TestContext(
        base_url=args.base_url.rstrip("/"),
        api_prefix=args.api_prefix,
        timeout=args.timeout,
        suffix=args.suffix,
        continue_on_failure=args.continue_on_failure,
    )

    print("[INFO] 开始执行 PSU 接口全流程测试")
    print(f"[INFO] baseUrl={ctx.base_url}, apiPrefix={ctx.api_prefix}")

    results = execute_cases(ctx)
    for i, item in enumerate(results, 1):
        print_result(i, item)

    report_path = Path(args.report)
    save_report(results, report_path)

    passed = sum(1 for x in results if x.passed)
    failed = len(results) - passed
    print("\n[SUMMARY]")
    print(f"total={len(results)}, passed={passed}, failed={failed}")
    print(f"report={report_path.resolve()}")

    return 0 if failed == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
