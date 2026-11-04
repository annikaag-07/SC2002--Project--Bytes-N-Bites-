#!/bin/sh
# Compiles the game into out/ (Java 17 bytecode).
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir -p out
javac --release 17 -Xlint:all -d out $(find src/main/java -name '*.java')
