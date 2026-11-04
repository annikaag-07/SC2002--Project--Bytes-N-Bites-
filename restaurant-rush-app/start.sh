#!/bin/sh
# Starts the game at http://localhost:8090 (build first with ./build.sh).
cd "$(dirname "$0")"
exec java -cp server/out rush.web.WebServer
