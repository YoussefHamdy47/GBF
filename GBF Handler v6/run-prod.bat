@echo off
title BunnyNexus - Production
color 0A

echo Starting BunnyNexus in production mode...
echo.

rem Run the bot and wait for it to finish
mvn -q exec:java -Dexec.mainClass=org.bunnys.Main

echo.
echo  BunnyNexus has stopped.
echo  Press any key to exit...
pause >nul