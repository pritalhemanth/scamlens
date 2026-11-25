#!/bin/sh
# Runs all ScamLens tests on macOS or Linux:  sh test.sh
cd "$(dirname "$0")" || exit 1
rm -rf out-test
javac -d out-test -sourcepath src/main/java:src/test/java src/test/java/com/scamlens/TestRunner.java || exit 1
java -cp out-test com.scamlens.TestRunner
