@echo off
title BunnyNexus - DEV (nodemon)
color 0E

set NODEMON_CMD=nodemon
set NODEMON_RUNNING=0

:menu
cls
echo BunnyNexus Dev Mode - Nodemon
echo.
if %NODEMON_RUNNING%==1 (
    echo [P] Pause nodemon
) else (
    echo [S] Start nodemon
)
echo [Q] Quit
echo.

choice /c SPQ /n /m "Select option: "
if errorlevel 3 goto quit
if errorlevel 2 goto pause
if errorlevel 1 goto start

:start
if %NODEMON_RUNNING%==1 (
    echo Nodemon is already running.
    timeout /t 2 >nul
    goto menu
)
start "Nodemon" cmd /c %NODEMON_CMD%
set NODEMON_RUNNING=1
goto menu

:pause
if %NODEMON_RUNNING%==0 (
    echo Nodemon is not running.
    timeout /t 2 >nul
    goto menu
)
taskkill /im node.exe /f >nul 2>&1
set NODEMON_RUNNING=0
echo Nodemon paused.
timeout /t 2 >nul
goto menu

:quit
if %NODEMON_RUNNING%==1 (
    taskkill /im node.exe /f >nul 2>&1
)
exit