# Restaurant Rush (SC2002)

Restaurant Rush is a turn-based command-line restaurant game in Java. You play the manager. Each turn you decide what the Waiter and the Chef do, while customers arrive, lose patience, order (they choose their own food), eat and pay. Staff time is limited, so the order you serve people in decides whether you meet the targets after 18 turns.

## Repository

- https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-
- Git tag `stage-1` marks this stage.
- The repository root holds the web game (`restaurant-rush-app/`) and this Java game (`restaurant-rush/`).

Run this stage from a fresh clone:

```bash
git clone https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-.git
```

```bash
cd SC2002--Project--Bytes-N-Bites- && git checkout stage-1 && cd restaurant-rush && ./build.sh && ./run.sh
```

## Requirements

- JDK 17 or newer (the code is compiled with `--release 17`).
- No external libraries, no build tool, no network access.

## Build, run, test

Linux / macOS:

```bash
./build.sh
```

```bash
./run.sh
```

```bash
./run.sh --seed 2002
```

```bash
./test.sh
```

Windows:

```bat
build.bat
run.bat --seed 2002
```

`--seed <long>` sets the seed of the customers' food choices (default 2002). The same seed and the same input always give the same game.

`./test.sh` builds the game, runs the rule self-tests (`src/test/java/rush/RulesSelfTest.java`), replays every scripted run in `runs/expected.txt`, saves the transcripts to `runs/logs/`, and checks each final `RESULT:` line. It prints `ALL CHECKS PASSED` on success.

## Replaying the scripted runs

```bash
./run.sh --seed 2002 < runs/stage1-victory.txt
```

```bash
./run.sh --seed 2002 < runs/stage1-defeat.txt
```

When input is piped, the game echoes each input line so the transcript reads like an interactive session. Lines starting with `#` are comments. If the input ends early, every remaining prompt defaults to Wait. See [docs/TEST_NOTES.md](docs/TEST_NOTES.md) for turn-by-turn walkthroughs and [docs/RULES_AND_ASSUMPTIONS.md](docs/RULES_AND_ASSUMPTIONS.md) for the exact rules.
