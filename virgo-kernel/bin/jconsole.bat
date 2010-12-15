@echo off
rem Script for starting jconsole

if "%OS%" == "Windows_NT" setlocal

rem Find root drive and path for current bat file directory (includes trailing backslash)
  set SCRIPT_DIR=%~dp0

if exist "%SCRIPT_DIR%setupVars.bat" (
  call "%SCRIPT_DIR%setupVars.bat"
  if not "%ERRORLEVEL%"=="0" (
    if "%OS%" == "Windows_NT" endlocal
    exit /B %ERRORLEVEL%
  )
) else (
  echo Cannot set up environment. "setupVars.bat" file missing.
  if "%OS%" == "Windows_NT" endlocal
  exit /B 1
)

rem Set defaults
  set TRUSTSTORE_PATH=%KERNEL_HOME%\config\keystore
  set TRUSTSTORE_PASSWORD=changeit
  set OTHER_ARGS=
  
:Loop
  if "%~1"=="" goto EndLoop

  if "%~1"=="-truststore" (
    set TRUSTSTORE_PATH=%~2
    shift
    shift
    goto Loop
  ) 
  if "%~1"=="-truststorePassword" (
    set TRUSTSTORE_PASSWORD=%~2
    shift
    shift
    goto Loop
  )
  rem Accumulate extra parameters.
    set OTHER_ARGS=%OTHER_ARGS% "%~1"
    shift
  goto Loop
:EndLoop

set JMX_OPTS=%JMX_OPTS% -J-Dcom.sun.tools.jconsole.mbeans.keyPropertyList=category,type 
set JMX_OPTS=%JMX_OPTS% -J-Djavax.net.ssl.trustStore="%TRUSTSTORE_PATH%" 
set JMX_OPTS=%JMX_OPTS% -J-Djavax.net.ssl.trustStorePassword=%TRUSTSTORE_PASSWORD%

"%JAVA_HOME%\bin\jconsole" %JMX_OPTS% %OTHER_ARGS%

if "%OS%" == "Windows_NT" endlocal
goto :eof
