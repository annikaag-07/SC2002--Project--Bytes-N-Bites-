#!/bin/sh
# Builds, runs the rule self-tests, then replays every scripted run in runs/expected.txt
# and checks its final RESULT line. Prints ALL CHECKS PASSED or exits non-zero.
set -e
cd "$(dirname "$0")"

echo "== build"
./build.sh

echo "== rule self-tests"
rm -rf out-test && mkdir -p out-test
javac --release 17 -Xlint:all -d out-test $(find src/main/java src/test/java -name '*.java')
java -ea -cp out-test rush.RulesSelfTest

echo "== scripted runs"
mkdir -p runs/logs
failures=0
while read -r script seed expected; do
    case "$script" in ''|'#'*) continue ;; esac
    log="runs/logs/${script%.txt}.log"
    ./run.sh --seed "$seed" < "runs/$script" > "$log" 2>&1 || true
    result=$(grep '^RESULT:' "$log" | tail -n 1)
    if echo "$result" | grep -q "$expected"; then
        echo "  ok   $script (seed $seed): $result"
    else
        echo "  FAIL $script (seed $seed): expected $expected, got '${result:-no RESULT line}'"
        failures=$((failures + 1))
    fi
done < runs/expected.txt

if [ "$failures" -ne 0 ]; then
    echo "$failures scripted run(s) failed"
    exit 1
fi
echo "ALL CHECKS PASSED"
