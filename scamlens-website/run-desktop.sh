#!/bin/sh
# Starts the ScamLens Swing desktop app on macOS or Linux:  sh run-desktop.sh
cd "$(dirname "$0")" || exit 1
rm -rf out-desktop
javac -d out-desktop -sourcepath src/main/java src/main/java/com/scamlens/desktop/DesktopApp.java || exit 1
java -cp out-desktop com.scamlens.desktop.DesktopApp
