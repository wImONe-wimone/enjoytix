@echo off
setlocal
cd /d "%~dp0"

if not exist logs mkdir logs
if not exist temp mkdir temp
if not exist temp\client_body_temp mkdir temp\client_body_temp
if not exist temp\proxy_temp mkdir temp\proxy_temp
if not exist temp\fastcgi_temp mkdir temp\fastcgi_temp
if not exist temp\uwsgi_temp mkdir temp\uwsgi_temp
if not exist temp\scgi_temp mkdir temp\scgi_temp

nginx.exe -p "%CD%\\" -c conf\nginx.conf -t
if errorlevel 1 exit /b %ERRORLEVEL%

start "enjoytix-nginx" /B nginx.exe -p "%CD%\\" -c conf\nginx.conf
exit /b %ERRORLEVEL%
