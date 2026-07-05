@echo off
setlocal
cd /d "%~dp0"

nginx.exe -p "%CD%\\" -s stop
exit /b %ERRORLEVEL%
