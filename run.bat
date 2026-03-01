@echo off
chcp 65001 >nul
title ADB Manager - Build Menu

:MENU
cls
echo ========================================
echo   ADB Manager - Android Device Tool
echo ========================================
echo.
echo   [1] Build and Run (First Time)
echo   [2] Build Only
echo   [3] Run EXE (After Build)
echo   [4] Exit
echo.
echo ========================================
echo.

set /p choice=Please select an option (1-4): 

if "%choice%"=="1" goto BUILD_AND_RUN
if "%choice%"=="2" goto BUILD_ONLY
if "%choice%"=="3" goto RUN_EXE
if "%choice%"=="4" goto EXIT

echo.
echo Invalid option, please try again.
timeout /t 2 >nul
goto MENU

:BUILD_AND_RUN
cls
echo ========================================
echo   Build and Run ADB Manager
echo ========================================
echo.

echo [1/2] Compiling and packaging...
call mvn clean package -Pbuild-exe -q
if errorlevel 1 (
    echo.
    echo Build failed!
    pause
    goto MENU
)

echo.
echo Build successful!
echo.
echo [2/2] Starting application...
echo.

REM Run the generated EXE
if exist "target\ADBManager.exe" (
    cd target
    start "" "ADBManager.exe"
    cd ..
    echo Application started!
    echo.
    echo Note: If the window doesn't appear, check that Java 17+ is installed.
) else (
    echo Error: EXE file not found!
    echo.
    pause
    goto MENU
)

goto AFTER_RUN

:BUILD_ONLY
cls
echo ========================================
echo   Build ADB Manager
echo ========================================
echo.

echo Compiling and packaging...
call mvn clean package -Pbuild-exe -q
if errorlevel 1 (
    echo.
    echo Build failed!
    pause
    goto MENU
)

echo.
echo ========================================
echo   Build successful!
echo ========================================
echo.
echo Output files:
echo   - target\adb-manager-1.0.0.jar
echo   - target\ADBManager.exe
echo   - target\lib\ (dependencies)
echo.
pause
goto MENU

:RUN_EXE
cls
echo ========================================
echo   Run ADB Manager
echo ========================================
echo.

if not exist "target\ADBManager.exe" (
    echo Error: ADBManager.exe not found!
    echo.
    echo Please build first using option [1] or [2].
    echo.
    pause
    goto MENU
)

echo Starting ADB Manager...
cd target
start "" "ADBManager.exe"
cd ..
echo.
echo Application started!

goto AFTER_RUN

:AFTER_RUN
echo.
echo ========================================
echo   Application launched
echo ========================================
echo.
echo [1] Return to menu
echo [2] Exit
echo.
set /p after_choice=Please select (1-2): 

if "%after_choice%"=="1" goto MENU
if "%after_choice%"=="2" goto EXIT
goto AFTER_RUN

:EXIT
cls
echo ========================================
echo   Thank you for using ADB Manager!
echo ========================================
echo.
timeout /t 2 >nul
exit
