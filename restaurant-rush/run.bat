@echo off
rem Runs the game. Usage: run.bat [--seed N]
cd /d "%~dp0"
java -cp out rush.Main %*
