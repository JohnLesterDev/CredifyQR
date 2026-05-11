@echo off
:start
echo [SYSTEM] Starting CredifyQR...
call gradlew.bat run
set EXIT_CODE=%errorlevel%

:: Exit codes 130 or 3221225786 usually indicate Ctrl+C/Termination
if %EXIT_CODE% equ 130 exit /b 0

echo [SYSTEM] App exited with code %EXIT_CODE%. 
echo [SYSTEM] Press Ctrl+C NOW to stop the loop, or wait for restart...
timeout /t 2
goto start