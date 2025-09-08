@echo off
title BunnyNexus - DEV (nodemon)
color 0E

echo Starting BunnyNexus in dev mode...
echo Nodemon is watching your sources
echo Type "rs" and press Enter to restart
echo Press Ctrl+C to stop
echo.

nodemon

echo.
echo BunnyNexus stopped. Press any key to exit...
pause >nul
