@ECHO OFF
IF "%OS%" == "Windows_NT" SETLOCAL

SET SCRIPT_DIR=%~dp0%
SET EXECUTABLE=dmk.bat

call "%SCRIPT_DIR%%EXECUTABLE%" start %*
if not "%ERRORLEVEL%"=="0" exit /B %ERRORLEVEL%
