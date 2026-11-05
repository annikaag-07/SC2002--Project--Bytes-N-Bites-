# Restaurant Rush (SC2002)

Restaurant Rush is a turn-based command-line restaurant game in Java. You play the manager. Each turn you decide what the Waiter and the Chef do, while customers arrive, lose patience, order (they choose their own food), eat and pay. Staff time is limited, so the order you serve people in decides whether you meet the targets after 18 turns.

## Repository

- https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-
- Git tags `stage-1` and `stage-2` mark the runnable stages. Each tag builds and passes `./test.sh` on its own, and each stage builds on the previous one.
- The repository root holds the web game (`restaurant-rush-app/`) and this Java game (`restaurant-rush/`).

Run a stage from a fresh clone:

```bash
git clone https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-.git
```

```bash
cd SC2002--Project--Bytes-N-Bites- && git checkout stage-2 && cd restaurant-rush && ./build.sh && ./run.sh
```

(Use `stage-1` the same way to run Stage 1, and `git checkout main` to return.)

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
./run.sh --seed 2002 < runs/stage2-victory.txt
```

```bash
./run.sh --seed 2002 < runs/stage2-defeat.txt
```

```bash
./run.sh --seed 2002 < runs/stage2-five-payers.txt
```

The Stage 1 scripts (`runs/stage1-*.txt`) belong to the all-Regular Stage 1 schedule; replay them from the `stage-1` tag.

When input is piped, the game echoes each input line so the transcript reads like an interactive session. Lines starting with `#` are comments. If the input ends early, every remaining prompt defaults to Wait. See [docs/TEST_NOTES.md](docs/TEST_NOTES.md) for turn-by-turn walkthroughs and [docs/RULES_AND_ASSUMPTIONS.md](docs/RULES_AND_ASSUMPTIONS.md) for the exact rules.

## Customer types (Stage 2)

| Type | Loss per waiting turn | Bill | Special |
|---|---|---|---|
| Regular | 8 | Full price | None |
| VIP | 5 | 10% off | None |
| Critic | 12 | Full price | Judges service speed: served within 2 turns of ordering gives +10 (impressed), otherwise -10 (unimpressed) |
