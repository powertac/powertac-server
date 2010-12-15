@echo off
rem Construct the CLASSPATH list from the Kernel lib directory.

if "%JAVA_HOME%" == "" (
  echo The JAVA_HOME environment variable is not defined.
  exit /B 1
)
if "%KERNEL_HOME%" == "" (
  echo The KERNEL_HOME environment variable is not defined.
  exit /B 1
)

for %%G in ("%KERNEL_HOME%\lib\*.jar") do call :AppendToClasspath "%%G"
rem Remove leading semi-colon if present
if "%CLASSPATH:~0,1%"==";" set CLASSPATH=%CLASSPATH:~1%
exit /B 0

:AppendToClasspath
  set CLASSPATH=%CLASSPATH%;%~1
  goto :eof
