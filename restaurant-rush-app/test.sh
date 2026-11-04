#!/bin/sh
# Adapter self-tests: parity with the Java CLI's Stage 1 logs, save/replay and the decision questions
# (AdapterSelfTest), then the mode, the arrivals and payment timing (ModeSelfTest).
set -e
cd "$(dirname "$0")"
BACKEND="${RUSH_BACKEND:-../restaurant-rush}"
rm -rf server/out-test && mkdir -p server/out-test
javac --release 17 -Xlint:all -d server/out-test -sourcepath "$BACKEND/src/main/java" $(find server/src server/test -name '*.java')
java -cp server/out-test rush.web.AdapterSelfTest "$BACKEND"
java -cp server/out-test rush.web.ModeSelfTest
