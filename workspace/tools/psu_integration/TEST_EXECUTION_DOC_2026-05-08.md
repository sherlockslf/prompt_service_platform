# PSU Test Execution Document (Module + Integration)

## 1. Execution Info
- Date: 2026-05-08
- Base URL: `http://127.0.0.1:8084`
- API Prefix: `/api`
- Sequence: module tests first, then integration test

## 2. Script Inventory
### 2.1 Module test scripts
- `workspace/tools/psu_integration/test_module_psu.py`
- `workspace/tools/psu_integration/test_module_schema_param.py`
- `workspace/tools/psu_integration/test_module_composition_review.py`
- `workspace/tools/psu_integration/test_module_dataset_testrun.py`
- `workspace/tools/psu_integration/test_module_evaluation.py`
- `workspace/tools/psu_integration/test_module_release_resolve.py`

### 2.2 Integration test scripts
- `workspace/tools/psu_integration/psu_api_case_logic.py`
- `workspace/tools/psu_integration/psu_api_test_runner.py`

### 2.3 One-click entry scripts
- Module tests: `workspace/tools/psu_integration/run_module_tests.ps1`
- Integration test: `workspace/tools/psu_integration/run_psu_full_flow_test.ps1`
- Full suite: `workspace/tools/psu_integration/run_all_tests.ps1`

## 3. Command
```powershell
powershell -ExecutionPolicy Bypass -File workspace\tools\psu_integration\run_all_tests.ps1 -BaseUrl "http://127.0.0.1:8084" -ApiPrefix "/api"
```

## 4. Results
### 4.1 Module tests
- `test_module_psu.py`: PASS
- `test_module_schema_param.py`: PASS
- `test_module_composition_review.py`: PASS
- `test_module_dataset_testrun.py`: PASS
- `test_module_evaluation.py`: PASS
- `test_module_release_resolve.py`: PASS

Summary: `6/6` passed.

### 4.2 Integration tests
- Total: `18`
- Passed: `18`
- Failed: `0`

Covered flow:
1. Create PSU
2. Update Schema
3. Update Param Set
4. Save Composition
5. Render Composition
6. Submit Composition
7. Submit Version Review
8. Approve Review
9. Fetch Latest Revision
10. Create Test Dataset
11. Run Test Dataset
12. Create Evaluation Task
13. Run Evaluation Task
14. Create Release
15. Submit Release
16. Approve Release
17. Execute Release
18. Resolve Prompt Service

## 5. Artifacts
- JSON report: `workspace/tools/psu_integration/reports/latest-report.json`

## 6. Conclusion
- All module-level and end-to-end test cases passed in this run.
- The semi-automated testing process is repeatable and ready for ongoing regression use.
