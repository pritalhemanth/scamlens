@echo off
rem Starts the ScamLens website. Double-click this file, or type  .\run.bat  in a terminal.
cd /d "%~dp0"

echo Compiling ScamLens...
if exist out rmdir /s /q out
javac -d out -sourcepath src\main\java src\main\java\com\scamlens\Main.java
if errorlevel 1 (
    echo.
    echo Compiling failed. Check that Java 21 is installed:  java -version
    pause
    exit /b 1
)

echo.
java -cp out com.scamlens.Main
pause
