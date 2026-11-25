#!/bin/sh
# Starts the ScamLens website on macOS or Linux:  sh run.sh
cd "$(dirname "$0")" || exit 1
rm -rf out
javac -d out -sourcepath src/main/java src/main/java/com/scamlens/Main.java || exit 1
java -cp out com.scamlens.Main
