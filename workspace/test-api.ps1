# PSU API测试脚本
$uri = "http://localhost:8084/api/auth/login"
$body = '{"username":"dev_user","password":"Dev@123"}'

try {
    $response = Invoke-RestMethod -Uri $uri -Method POST -ContentType "application/json" -Body $body
    Write-Host "API响应:"
    $response | ConvertTo-Json -Depth 5
    Write-Host "`n测试成功！后端API正常运行。"
} catch {
    Write-Host "API调用失败: $_"
    Write-Host "请确认后端服务是否在 http://localhost:8084 运行"
}
