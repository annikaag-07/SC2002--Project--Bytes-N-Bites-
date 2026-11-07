#!/bin/sh
# Adapter self-tests: classic-mode parity with the Java CLI's Stage 3 logs, Custom-mode extras and save/replay
# (AdapterSelfTest), then the four modes, the arrival schedule, payment timing and Stage 1/2 parity (ModeSelfTest).
set -e
cd "$(dirname "$0")"
BACKEND="${RUSH_BACKEND:-../restaurant-rush}"
rm -rf server/out-test && mkdir -p server/out-test
javac --release 17 -Xlint:all -d server/out-test -sourcepath "$BACKEND/src/main/java" $(find server/src server/test -name '*.java')
java -cp server/out-test rush.web.AdapterSelfTest "$BACKEND"
java -cp server/out-test rush.web.ModeSelfTest "$BACKEND"
