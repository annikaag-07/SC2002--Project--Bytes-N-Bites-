#!/bin/sh
# Bytes N Bites - compiles the small Java game server (needs JDK 17 or newer) and opens the game.
set -e
cd "$(dirname "$0")/restaurant-rush-app"
PORT="${PORT:-8090}"
mkdir -p server/out
javac --release 17 -d server/out -sourcepath ../restaurant-rush/src/main/java $(find server/src -name '*.java')
URL="http://localhost:$PORT"
echo "Bytes N Bites is running at $URL  (press Ctrl+C to stop)"
( sleep 1; if command -v open >/dev/null 2>&1; then open "$URL"; elif command -v xdg-open >/dev/null 2>&1; then xdg-open "$URL"; fi ) >/dev/null 2>&1 &
export PORT
exec java -cp server/out rush.web.WebServer
