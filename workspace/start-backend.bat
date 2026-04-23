@echo off
chcp 65001 >nul
echo ========================================
echo   启动后端服务 (Spring Boot)
echo ========================================

cd /d "%~dp0backend"

echo [1/2] 检查 Maven 环境...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Maven，请先安装 Maven 并配置环境变量
    pause
    exit /b 1
)

echo [2/2] 启动 Spring Boot 应用...
echo 提示: 首次启动会自动下载依赖，请耐心等待
echo ========================================
mvn spring-boot:run

pause
