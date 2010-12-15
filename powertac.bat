@ECHO OFF

SET SCRIPT_DIR=%~dp0%

rem TODO: (optional?) maven build task

rem Start server
call "%SCRIPT_DIR%\virgo-kernel\bin\startup.bat"
