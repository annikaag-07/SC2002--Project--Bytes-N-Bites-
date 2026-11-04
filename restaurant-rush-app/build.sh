#!/bin/sh
# Builds the Java API server (against the unchanged back end) and the React client.
# RUSH_BACKEND points at the Java project (default ../restaurant-rush).
set -e
cd "$(dirname "$0")"
BACKEND="${RUSH_BACKEND:-../restaurant-rush}"
rm -rf server/out && mkdir -p server/out
javac --release 17 -Xlint:all -d server/out -sourcepath "$BACKEND/src/main/java" $(find server/src -name '*.java')
(cd client && npm install --no-audit --no-fund && npm run build)
