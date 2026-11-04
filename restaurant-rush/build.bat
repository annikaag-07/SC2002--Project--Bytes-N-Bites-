@echo off
rem Compiles the game into out\ (Java 17 bytecode).
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac --release 17 -Xlint:all -d out @sources.txt
