@echo off
rem Set up env vars needed for dmk.bat and jconsole.bat (with user-pluggable mods if present)

if "%SCRIPT_DIR%"=="" (
  echo Called setupVars.bat out of context.
  exit /B 1
)

rem Derive KERNEL_HOME full path from script's parent (no backslash)
  for %%I in ("%SCRIPT_DIR%..") do set KERNEL_HOME=%%~dpfsI

rem Check files exist (exit if not)
  set ChkLst="%KERNEL_HOME%\bin\setupClasspath.bat","%KERNEL_HOME%\bin\checkJava.bat"

  for %%I in (%ChkLst%) do if not exist "%%~I" (
    echo File "%%~I" does not exist but is required.
    exit /B 1
  )

rem set up the classpath (check result)
  call "%KERNEL_HOME%\bin\setupClasspath.bat"
  if not "%ERRORLEVEL%" == "0" exit /B %ERRORLEVEL%

rem Run Java Version check (uses JAVA_HOME) (check result)
  call "%KERNEL_HOME%\bin\checkJava.bat"
  if not "%ERRORLEVEL%" == "0" exit /B %ERRORLEVEL%
	
rem Execute user setenv script if needed (ignore result)
  if exist "%KERNEL_HOME%\bin\setenv.bat" call "%KERNEL_HOME%\bin\setenv.bat"

goto :eof
