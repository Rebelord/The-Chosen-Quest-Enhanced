@echo off
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java is required. Install Java 8 or newer from https://adoptium.net/
  pause
  exit /b 1
)
java -jar TheChosenQuest-Enhanced.jar
if errorlevel 1 pause
