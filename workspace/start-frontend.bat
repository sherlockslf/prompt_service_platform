@echo off
chcp 65001 >nul
echo ========================================
echo   启动前端服务 (Vue + Vite)
echo ========================================

cd /d "%~dp0frontend"

echo [1/2] 检查 Node.js 环境...
node -v >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到 Node.js，请先安装 Node.js
    pause
    exit /b 1
)

echo [2/2] 启动 Vite 开发服务器...
echo 提示: 首次启动会自动安装依赖，请耐心等待
echo ========================================
npm run dev

pause
