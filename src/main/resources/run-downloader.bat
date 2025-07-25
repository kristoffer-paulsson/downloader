@echo off
setlocal EnableDelayedExpansion

set APP_NAME=downloader
set JAR_NAME=%APP_NAME%-1.0-SNAPSHOT.jar
set JAVA_MIN_VERSION=11

:: Function to check if Java is installed and meets the minimum version
:check_java
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Error: Java is not installed or not in PATH
    exit /b 1
)

for /f "tokens=2 delims=^" %%i in ('java -version 2^>^&1 ^| findstr /r "version"') do (
    set JAVA_VERSION=%%i
    set JAVA_VERSION=!JAVA_VERSION:~0,2!
    if !JAVA_VERSION! lss %JAVA_MIN_VERSION% (
        echo Error: Java %JAVA_MIN_VERSION% or higher is required, found Java !JAVA_VERSION!
        exit /b 1
    )
)
goto :eof

:: Function to run the JAR
:run_jar
:: Assume JAR is in the same directory as the script
set SCRIPT_DIR=%~dp0
set JAR_PATH=%SCRIPT_DIR%\%JAR_NAME%

if not exist "%JAR_PATH%" (
    echo Error: Could not find %JAR_NAME% in %SCRIPT_DIR%
    exit /b 1
)

:: Build classpath including dependencies in the same directory
set CLASSPATH=%JAR_PATH%
for %%f in ("%SCRIPT_DIR%\*.jar") do (
    if not "%%f"=="%JAR_PATH%" (
        set CLASSPATH=!CLASSPATH!;%%f
    )
)

:: Run the JAR with specified memory settings and pass arguments
java -Xmx512m -cp "%CLASSPATH%" org.example.downloader.Main %*
goto :eof

:: Main execution
call :check_java
if %ERRORLEVEL% neq 0 exit /b %ERRORLEVEL%
call :run_jar %*
exit /b %ERRORLEVEL%