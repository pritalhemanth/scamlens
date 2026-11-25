@echo off
rem Starts the ScamLens Swing desktop app. Double-click this file, or type  .\run-desktop.bat  in a terminal.
rem It uses the same engine and the same history file (data\scan-history.txt) as the website.
cd /d "%~dp0"

echo Compiling the ScamLens desktop app...
if exist out-desktop rmdir /s /q out-desktop
javac -d out-desktop -sourcepath src\main\java src\main\java\com\scamlens\desktop\DesktopApp.java
if errorlevel 1 (
    echo.
    echo Compiling failed. Check that Java 21 is installed:  java -version
    pause
    exit /b 1
)

echo Starting... (close the ScamLens window to stop)
java -cp out-desktop com.scamlens.desktop.DesktopApp
