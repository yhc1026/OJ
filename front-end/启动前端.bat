@echo off
setlocal EnableExtensions
chcp 65001 >nul
cd /d "%~dp0"

set "PORT=8765"
set "SVC_URL=http://127.0.0.1:%PORT%/"

echo ========================================
echo OJ front-end static server
echo PowerShell - no Python / Node
echo DIR: %CD%
echo PORT: %PORT%
echo URL:  %SVC_URL%
echo ========================================
echo.

where powershell >nul 2>&1 || (
  echo [ERROR] PowerShell not found.
  pause
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0serve-static.ps1" -Port %PORT%

echo.
echo Server stopped.
pause
