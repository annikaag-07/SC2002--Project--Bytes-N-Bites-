#!/bin/sh
# Development mode: Java API on :8090 plus Vite with hot reload on :5173 (open http://localhost:5173).
set -e
cd "$(dirname "$0")"
BACKEND="${RUSH_BACKEND:-../restaurant-rush}"
mkdir -p server/out
javac --release 17 -d server/out -sourcepath "$BACKEND/src/main/java" $(find server/src -name '*.java')
java -cp server/out rush.web.WebServer &
SERVER=$!
trap 'kill $SERVER' EXIT INT TERM
cd client && npm install --no-audit --no-fund && npm run dev
