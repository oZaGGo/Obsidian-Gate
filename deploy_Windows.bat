@echo off
setlocal enabledelayedexpansion

:loop
echo [SYSTEM] Starting Obsidian panel...

call mvn clean package -DskipTests

set JAR_PATH=
for %%f in (target\*.jar) do set JAR_PATH=%%f

if "%JAR_PATH%"=="" (
    echo [ERROR] No JAR file found in target directory.
    echo [SYSTEM] Retrying in 5 seconds...
    timeout /t 5 >nul
    goto loop
)

java -jar %JAR_PATH%

set EXIT_CODE=%errorlevel%

if %EXIT_CODE% equ 10 (
    echo [UPDATE] Updating app from git...
    git restore .idea/* 2>nul
    git pull origin main
    echo [UPDATE] Done applying changes. Deploying again...
    goto loop
)

if %EXIT_CODE% equ 0 (
    echo [SYSTEM] Closing app normally...
    pause
    exit /b 0
) else (
    echo [SYSTEM] App crashed or stopped with Error Code: %EXIT_CODE%
    echo [SYSTEM] Restarting deploy in 5 seconds...
    timeout /t 5 >nul
    goto loop
)