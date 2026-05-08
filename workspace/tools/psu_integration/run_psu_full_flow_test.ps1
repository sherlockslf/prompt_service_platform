param(
  [string]$BaseUrl = "http://127.0.0.1:8084",
  [ValidateSet('/api','/api/v1')]
  [string]$ApiPrefix = "/api",
  [string]$Suffix = "local",
  [double]$Timeout = 12,
  [switch]$ContinueOnFailure
)

$ErrorActionPreference = "Stop"

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$runner = Join-Path $scriptRoot "psu_api_test_runner.py"
$repoRoot = Resolve-Path (Join-Path $scriptRoot "..\..\..")
$venvPython = Join-Path $repoRoot ".venv\Scripts\python.exe"

if (-not (Test-Path $runner)) {
  throw "未找到测试执行器: $runner"
}

if (-not (Test-Path $venvPython)) {
  $venvPython = "python"
}

$reportPath = Join-Path $scriptRoot "reports\latest-report.json"

$cmd = @(
  "$venvPython",
  "$runner",
  "--base-url", "$BaseUrl",
  "--api-prefix", "$ApiPrefix",
  "--suffix", "$Suffix",
  "--timeout", "$Timeout",
  "--report", "$reportPath"
)

if ($ContinueOnFailure) {
  $cmd += "--continue-on-failure"
}

Write-Host "[INFO] 一键执行 PSU 全流程接口测试..."
Write-Host "[INFO] BaseUrl=$BaseUrl ApiPrefix=$ApiPrefix"

& $cmd[0] $cmd[1..($cmd.Count - 1)]
$exitCode = $LASTEXITCODE

if ($exitCode -eq 0) {
  Write-Host "[DONE] 测试通过，报告: $reportPath"
} else {
  Write-Host "[FAILED] 存在失败用例，报告: $reportPath"
}

exit $exitCode
