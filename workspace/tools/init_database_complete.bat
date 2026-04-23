@echo off
chcp 65001 >nul
echo ========================================
echo PSU数据库完整初始化工具
echo ========================================
echo.
echo 功能：一次性完成所有表创建、版本字段更新、默认数据插入
echo 适用于新库初始化，确保只需执行一次即可完成全部初始化工作
echo.

REM 尝试使用常见的Python路径
where python >nul 2>&1
if %errorlevel% equ 0 (
    python "%~dp0init_database_complete.py"
    goto :end
)

where python3 >nul 2>&1
if %errorlevel% equ 0 (
    python3 "%~dp0init_database_complete.py"
    goto :end
)

where py >nul 2>&1
if %errorlevel% equ 0 (
    py "%~dp0init_database_complete.py"
    goto :end
)

REM 尝试常见的Python安装路径
if exist "C:\Python39\python.exe" (
    "C:\Python39\python.exe" "%~dp0init_database_complete.py"
    goto :end
)

if exist "C:\Python310\python.exe" (
    "C:\Python310\python.exe" "%~dp0init_database_complete.py"
    goto :end
)

if exist "C:\Python311\python.exe" (
    "C:\Python311\python.exe" "%~dp0init_database_complete.py"
    goto :end
)

if exist "C:\Python312\python.exe" (
    "C:\Python312\python.exe" "%~dp0init_database_complete.py"
    goto :end
)

if exist "%LOCALAPPDATA%\Programs\Python\Python39\python.exe" (
    "%LOCALAPPDATA%\Programs\Python\Python39\python.exe" "%~dp0init_database_complete.py"
    goto :end
)

if exist "%LOCALAPPDATA%\Programs\Python\Python310\python.exe" (
    "%LOCALAPPDATA%\Programs\Python\Python310\python.exe" "%~dp0init_database_complete.py"
    goto :end
)

if exist "%LOCALAPPDATA%\Programs\Python\Python311\python.exe" (
    "%LOCALAPPDATA%\Programs\Python\Python311\python.exe" "%~dp0init_database_complete.py"
    goto :end
)

if exist "%LOCALAPPDATA%\Programs\Python\Python312\python.exe" (
    "%LOCALAPPDATA%\Programs\Python\Python312\python.exe" "%~dp0init_database_complete.py"
    goto :end
)

echo 错误: 未找到Python解释器
echo 请确保已安装Python并将其添加到系统PATH中
echo.
echo 或者手动执行: python "%~dp0init_database_complete.py"
echo.
pause
exit /b 1

:end
echo.
pause