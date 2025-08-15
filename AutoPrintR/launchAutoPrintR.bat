@echo off
REM Change directory to the app folder inside the current user's Local AppData
cd /d "%LOCALAPPDATA%\AutoPrintR\app"

REM Run the Java app
java -jar AutoPrintR.jar

REM Pause so you can see any messages/errors before the window closes
pause
