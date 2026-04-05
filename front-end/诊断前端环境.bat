@echo off
chcp 65001 >nul
cd /d "%~dp0"

echo ========== 1. 前端文件目录 ==========
echo %CD%
dir /b index.html 2>nul || echo [异常] 当前目录下没有 index.html，请勿移动此 bat 出 front-end 文件夹
echo.

echo ========== 2. Python ==========
where py 2>nul
where python 2>nul
py -3 --version 2>&1
python --version 2>&1
py -3 -c "print('py-3 OK')" 2>&1
python -c "print('python OK')" 2>&1
echo.
echo 若出现 Unable to create process ... D:\Python\python.exe
echo 说明 py 启动器指向了坏路径，请阅读「环境修复说明.md」重装 Python 或安装 Node。
echo 可检查: %LOCALAPPDATA%\py.ini
echo.

echo ========== 3. PowerShell（当前启动脚本依赖）==========
where powershell 2>nul
powershell -NoProfile -Command "$PSVersionTable.PSVersion"
echo.

echo ========== 4. Node（可选，不必装）==========
where node 2>nul
node --version 2>&1
echo.

echo ========== 5. 常见端口占用（有输出表示被占用）==========
for %%P in (8765 5500 5173 8080 8081 9000) do (
  echo --- :%%P ---
  netstat -ano | findstr ":%%P "
)
echo.

echo ========== 6. 本机访问测试（需先手动启动 启动前端.bat）==========
echo 若服务已在 8765 运行，下面应看到 HTTP/1.0 200 或类似：
curl -s -o nul -w "HTTP状态码: %%{http_code}\n" http://127.0.0.1:8765/ 2>nul
if errorlevel 1 echo （未安装 curl 可忽略此行）
echo.

pause
