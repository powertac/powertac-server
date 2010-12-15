@echo off
rem Script for checking we have the right version of Java.

if "%JAVA_HOME%" == "" (
  echo The JAVA_HOME environment variable is not defined.
  exit /B 1
)
if "%CLASSPATH%" == "" (
  echo The CLASSPATH environment variable is not defined.
  exit /B 1
)

rem Run java version check with the discovered java jvm.
"%JAVA_HOME%\bin\java" -classpath "%CLASSPATH%" org.eclipse.virgo.osgi.launcher.JavaVersionChecker

rem If non-zero exit then either we cannot find the checker or the Java version is incorrect.
if not "%ERRORLEVEL%"=="0" exit /B %ERRORLEVEL%
