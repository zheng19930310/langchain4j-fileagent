@echo off
echo ========================================
echo   Langchain4J FileAgent 启动脚本
echo ========================================
echo.

cd /d "%~dp0"

echo [1/3] 检查Java环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Java环境，请先安装Java 17或更高版本
    pause
    exit /b 1
)
echo [成功] Java环境正常
echo.

echo [2/3] 检查Maven环境...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到Maven环境，请先安装Maven
    pause
    exit /b 1
)
echo [成功] Maven环境正常
echo.

echo [3/3] 启动应用...
echo 提示: 首次运行需要下载依赖，请耐心等待...
echo.

call mvn spring-boot:run

pause
