param(
  [string]$BaseUrl = "http://127.0.0.1:8084",
  [ValidateSet('/api','/api/v1')]
  [string]$ApiPrefix = "/api",
  [double]$Timeout = 12
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..\..")
$venvPython = Join-Path $repoRoot ".venv\Scripts\python.exe"
if (-not (Test-Path $venvPython)) { $venvPython = "python" }

$modules = @(
  "test_module_psu.py",
  "test_module_schema_param.py",
  "test_module_composition_review.py",
  "test_module_dataset_testrun.py",
  "test_module_evaluation.py",
  "test_module_release_resolve.py"
)

foreach ($m in $modules) {
  $path = Join-Path $scriptRoot $m
  Write-Host "[RUN] $m"
  & $venvPython $path --base-url $BaseUrl --api-prefix $ApiPrefix --timeout $Timeout
  if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAILED] 模块测试失败: $m"
    exit $LASTEXITCODE
  }
  Write-Host "[PASS] $m"
}

Write-Host "[DONE] 全部模块基础测试通过"
exit 0
