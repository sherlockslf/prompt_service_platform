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

$moduleScript = Join-Path $scriptRoot "run_module_tests.ps1"
$integrationScript = Join-Path $scriptRoot "run_psu_full_flow_test.ps1"

Write-Host "[PHASE-1] 运行模块基础测试"
& powershell -ExecutionPolicy Bypass -File $moduleScript -BaseUrl $BaseUrl -ApiPrefix $ApiPrefix -Timeout $Timeout
if ($LASTEXITCODE -ne 0) {
  Write-Host "[STOP] 模块基础测试未通过，终止集成测试"
  exit $LASTEXITCODE
}

Write-Host "[PHASE-2] 运行全流程集成测试"
$cmd = @("powershell", "-ExecutionPolicy", "Bypass", "-File", $integrationScript,
  "-BaseUrl", $BaseUrl, "-ApiPrefix", $ApiPrefix, "-Suffix", $Suffix, "-Timeout", $Timeout)
if ($ContinueOnFailure) { $cmd += "-ContinueOnFailure" }

& $cmd[0] $cmd[1..($cmd.Count - 1)]
exit $LASTEXITCODE
