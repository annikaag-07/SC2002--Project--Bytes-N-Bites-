#!/bin/sh
# Runs the game. Usage: ./run.sh [--seed <long>]
cd "$(dirname "$0")"
exec java -cp out rush.Main "$@"
