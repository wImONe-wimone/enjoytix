@echo off
setlocal
cd /d "%~dp0"

nginx.exe -p "%CD%\\" -s reload
exit /b %ERRORLEVEL%
