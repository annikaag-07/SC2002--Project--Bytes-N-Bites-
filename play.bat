@echo off
rem Bytes N Bites - compiles the small Java game server (needs JDK 17 or newer) and opens the game.
cd /d "%~dp0restaurant-rush-app"
if not exist server\out mkdir server\out
dir /s /b server\src\*.java > sources.txt
javac --release 17 -d server\out -sourcepath ..\restaurant-rush\src\main\java @sources.txt
if errorlevel 1 (
  echo Could not compile. Is a JDK 17+ installed and on PATH?
  pause
  exit /b 1
)
echo Bytes N Bites is running at http://localhost:8090  (close this window to stop)
start "" http://localhost:8090
java -cp server\out rush.web.WebServer
pause
