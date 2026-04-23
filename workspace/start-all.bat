@echo off
chcp 65001 >nul
echo ========================================
echo   一键启动前后端服务
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] 检查环境依赖...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Maven，请先安装 Maven 并配置环境变量
    pause
    exit /b 1
)

node -v >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Node.js，请先安装 Node.js
    pause
    exit /b 1
)
echo [OK] 环境检查通过
echo.

echo [2/3] 启动后端服务 (Spring Boot)...
start "后端服务" cmd /k "cd /d "%~dp0backend" && mvn spring-boot:run"
echo 后端服务已在新窗口启动，等待 10 秒...
timeout /t 10 /nobreak >nul
echo.

echo [3/3] 启动前端服务 (Vue + Vite)...
start "前端服务" cmd /k "cd /d "%~dp0frontend" && npm run dev"
echo.

echo ========================================
echo   所有服务已启动!
echo   后端: http://localhost:8080
echo   前端: 请查看前端窗口中的地址
echo ========================================
echo.
echo 提示: 关闭对应窗口即可停止服务
pause
