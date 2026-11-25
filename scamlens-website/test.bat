@echo off
rem Runs all ScamLens tests. Double-click this file, or type  .\test.bat  in a terminal.
cd /d "%~dp0"

echo Compiling tests...
if exist out-test rmdir /s /q out-test
javac -d out-test -sourcepath "src\main\java;src\test\java" src\test\java\com\scamlens\TestRunner.java
if errorlevel 1 (
    echo.
    echo Compiling failed. Check that Java 21 is installed:  java -version
    pause
    exit /b 1
)

java -cp out-test com.scamlens.TestRunner
pause
