# Restaurant Rush (SC2002)

Restaurant Rush is a turn-based command-line game in Java in which **the player is the manager** of a small restaurant. Over 18 turns, six customers arrive (Regular, VIP and Critic). They wait for a table, choose their own food, wait for the kitchen, eat and pay. Every turn you decide what each staff member does: the Host, the Waiter and the Chef. You can also switch on a once-per-game Happy Hour. Staff time is short and customers lose patience, so your priorities decide whether you reach the targets: at least 4 paid customers, $45.00 revenue and an average satisfaction of 60.

## Repository

- https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-
- Git tags `stage-1` to `stage-4` mark the runnable stages. The terminal game's rules are complete at `stage-3`; `stage-4` only adds the opt-in hooks the web game's Custom mode uses (see `docs/TRANSITIONS.md`). Each tag builds and passes `./test.sh` on its own, and each stage builds on the previous one.
- The repository root holds the web game (`restaurant-rush-app/`) and this Java game (`restaurant-rush/`).

Run a stage from a fresh clone:

```bash
git clone https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-.git
```

```bash
cd SC2002--Project--Bytes-N-Bites- && git checkout stage-4 && cd restaurant-rush && ./build.sh && ./run.sh
```

(Use `stage-1`, `stage-2` or `stage-3` the same way, and `git checkout main` to return.)

## Requirements

- JDK 17 or newer (compiled with `javac --release 17`; tested with JDK 26).
- No external libraries, no build tool, fully offline.

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

Windows (Command Prompt):

```bat
build.bat
run.bat --seed 2002
```

- `--seed <long>` sets the seed for the customers' food choices (default 2002). The same seed and the same input always give the same game.
- `./test.sh` builds, runs the rule self-tests (`src/test/java/rush/RulesSelfTest.java`, plain Java with no JUnit), replays every run in `runs/expected.txt` into `runs/logs/`, and checks each final `RESULT:` line. It prints `ALL CHECKS PASSED` on success.

## How to play

Each turn:

1. **Arrivals.** A scheduled customer joins the waiting queue with satisfaction 100.
2. **Status.** Revenue, average satisfaction of paid customers, Happy Hour status, queue, tables, every customer (type, satisfaction, state), every order (status, `units x/y`, locked price), and each staff member's valid targets.
3. **Your decisions**, asked in execution order. Each valid choice happens immediately:
   - `Activate Happy Hour now? (y/n)`: only while it is unused (once per game, lasts 2 turns).
   - **Host:** `1` seat a queued customer at a free table, `0` wait.
   - **Waiter:** `1` seat, `2` take a seated customer's order (the customer chooses the dish and the price is locked), `3` serve a READY order, `0` wait.
   - **Chef:** `1` advance an order by one preparation unit, `0` wait.

   Ids can be typed as `C1` / `T1` / `O1` or just `1`. An invalid choice is explained and the same staff member is asked again; no task is used up.
4. **Eating and payment** (automatic). The Cashier charges one READY_TO_PAY customer, then customers served on an earlier turn finish eating.
5. **Waiting.** Customers still waiting for a table or food lose satisfaction (Regular 8, VIP 5, Critic 12). At 0 they leave, and their order is cancelled.
6. **End-of-turn summary.** After turn 18, the result: each target shown as met or not met, then `RESULT: VICTORY` or `RESULT: DEFEAT`.

Timing rules: food that becomes READY this turn can be served next turn at the earliest. A customer served on turn t is READY_TO_PAY on t+1 and pays on t+2 at the earliest.

## Reproducible runs

All runs use seed 2002. Scripts contain `#` comments explaining each turn; when input is piped the game echoes every line, so the logs read like a live session.

| Stage | Script | Seed | Command | Expected result | Log file |
|---|---|---|---|---|---|
| 1 | `runs/stage1-victory.txt` | 2002 | `./run.sh --seed 2002 < runs/stage1-victory.txt` (at tag `stage-1`) | VICTORY (4 paid, $51.00, avg 78.0) | `runs/logs/stage1-victory.log` |
| 1 | `runs/stage1-defeat.txt` | 2002 | `./run.sh --seed 2002 < runs/stage1-defeat.txt` (at tag `stage-1`) | DEFEAT (1 paid, $9.00) | `runs/logs/stage1-defeat.log` |
| 2 | `runs/stage2-victory.txt` | 2002 | `./run.sh --seed 2002 < runs/stage2-victory.txt` (at tag `stage-2`) | VICTORY (4 paid, $49.20, avg 79.8) | `runs/logs/stage2-victory.log` |
| 2 | `runs/stage2-five-payers.txt` | 2002 | `./run.sh --seed 2002 < runs/stage2-five-payers.txt` (at tag `stage-2`) | VICTORY (5 paid, $67.20, avg 70.2) | `runs/logs/stage2-five-payers.log` |
| 2 | `runs/stage2-defeat.txt` | 2002 | `./run.sh --seed 2002 < runs/stage2-defeat.txt` (at tag `stage-2`) | DEFEAT (avg 57.3) | `runs/logs/stage2-defeat.log` |
| 3 | `runs/stage3-victory.txt` | 2002 | `./run.sh --seed 2002 < runs/stage3-victory.txt` | VICTORY (5 paid, $98.34, avg 86.4) | `runs/logs/stage3-victory.log` |
| 3 | `runs/stage3-defeat.txt` | 2002 | `./run.sh --seed 2002 < runs/stage3-defeat.txt` | DEFEAT (avg 59.5) | `runs/logs/stage3-defeat.log` |
| 3 | `runs/stage3-features-demo.txt` | 2002 | `./run.sh --seed 2002 < runs/stage3-features-demo.txt` | VICTORY (4 paid, $72.85, avg 82.8) | `runs/logs/stage3-features-demo.log` |

Earlier stages' scripts answer earlier prompts and menus, so replay them from their own tag (`./test.sh` there replays them automatically). Turn-by-turn walkthroughs and the reason each run wins or loses: [docs/TEST_NOTES.md](docs/TEST_NOTES.md).

## Game rules and design decisions

The full rules and every assumption, each with a rationale, are in [docs/RULES_AND_ASSUMPTIONS.md](docs/RULES_AND_ASSUMPTIONS.md). Summary:

| Topic | Decision |
|---|---|
| Base setting | 18 turns (always all played), 2 tables, arrivals on turns 1, 4, 7, 10, 13, 16: Regular, **VIP**, Regular, **Critic**, Regular, Regular |
| Menu | Salad $9.00 (1 unit), Burger $15.00 (1), Pasta $18.00 (2), Salad + Burger Combo $21.60 (2), Salad + Pasta Combo $24.30 (3) |
| Food choice | Each customer chooses with its own `Random(seed * 1_000_003 + arrivalIndex)`, uniformly over the menu, when its order is taken. **Combo rule:** a combo is ordered exactly when that draw lands on one of the two combos. |
| Critic behaviour | Served within **2 turns** of the order being taken: impressed, +10 (max 100). Otherwise unimpressed, -10 (min 0; a served Critic never leaves). |
| VIP | Loses 5 per waiting turn; 10% off. |
| Happy Hour | Once per game, asked at the start of a turn before any staff act. Active that turn and the next (one turn if started on turn 18). Orders **taken** while active get 20% off. At the start of each active turn, customers still waiting for a table or food recover +15 (max 100). |
| Pricing order | Menu price (combo already 90%) -> VIP 90% -> Happy Hour 80%. Rounded **half up to the cent after each step**; locked when the order is taken (VIP Burger in Happy Hour = $10.80). |
| Staff order | Happy Hour decision -> **Host -> Waiter -> Chef**, each acting as soon as chosen. The Host seats first, so the Waiter can take that customer's order the same turn; the Waiter acts before the Chef. |
| Cashier | Automatic, one per turn, oldest first = earliest READY_TO_PAY turn, then earliest arrival. |
| Abandonment | At 0 satisfaction a waiting customer leaves; the order is cancelled at any progress and the table freed; no revenue. |
| Money | `long` cents everywhere, shown as `$x.yy`. |

## Architecture (BCE)

| Package | Class | Responsibility |
|---|---|---|
| boundary | `ConsoleUI` | Shows the state, asks the manager for each decision, forwards commands to the controller, prints events and the result. No rules. |
| boundary | `InputReader` | Line input: echo when piped, `#` comments, end of input defaults to Wait / No. |
| control | `GameController` | Turn engine: step order, one task per staff member, staff ordering, end-of-turn steps, read-only queries and valid targets. |
| control | `ArrivalSchedule` | Who arrives when; the only place that names concrete customer types. |
| control | `BillCalculator` | The single pricing pipeline, locking each order's price. |
| control | `HappyHourPolicy` | Once-per-game activation, active window, discount step, recovery. |
| control | `GameConfig` | Turns, tables, seed, menu, victory thresholds. |
| control | `GameResult` | Victory check (exact integer comparison) and average formatting. |
| control | `GameEvent` | A reported event (turn + text). |
| entity | `Customer` (abstract), `RegularCustomer`, `VIPCustomer`, `CriticCustomer` | Satisfaction, lifecycle state, own seeded food choice; type-specific hooks. |
| entity | `Staff` (abstract), `Host`, `Waiter`, `Chef`, `Cashier` | One task per turn; each validates and performs its own actions and returns an `ActionResult`. |
| entity | `Restaurant` | Owns tables, queue, customers, orders and revenue; seating, payment and departure rules. |
| entity | `Order`, `Table` | Order progress, status, key turns and locked price; table occupancy. |
| entity | `MenuItem`, `ComboMeal`, `Menu` | Dishes, combos composed of two dishes, the ordered menu. |
| entity | `Money`, `ActionResult`, `CustomerState`, `OrderStatus` | Cents formatting and half-up percentages; command outcome; state enums. |

- **Encapsulation.** All fields are private. State-changing methods on `Customer`, `Order` and `Table` (`seatAt`, `attachOrder`, `advanceUnit`, `markServed`, `markPaid`, `occupy`, ...) are package-private, so only the entity rules (`Restaurant`, the staff classes) can call them. The public rule methods (`applyWaitingPenalty`, `recoverWhileWaiting`, `finishEatingIfDone`, `Order.lockPrice` (once only)) check state themselves. Collections are returned as unmodifiable views.
- **Polymorphism.** `Customer` declares `satisfactionLossPerTurn()`, `applyCustomerPricing(long)` and `onServed(int)`. The `final` templates `applyWaitingPenalty()` and `receiveServedOrder(int)` call them. `BillCalculator`, `GameController` and `ConsoleUI` never test a customer's type. `ComboMeal` overrides `MenuItem.describe()`. `Host`, `Waiter`, `Chef` and `Cashier` share `Staff`'s one-task-per-turn logic.
- **Composition.** `Restaurant` creates and owns its `Table`s. A `ComboMeal` is composed of two `MenuItem`s. `GameController` owns the `Restaurant`, the staff and the policies.

## Stage transitions

Each table below was generated from Git (`git diff --name-status <previous-tag> <tag> -- restaurant-rush`), so every changed path is listed; paths are relative to `restaurant-rush/`. The web game's changed files are listed in `docs/TRANSITIONS.md`.

### Stage 1 → Stage 2: Customer behaviour

Compare: https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-/compare/stage-1...stage-2

#### New files

| File | Purpose |
|---|---|
| `src/main/java/rush/entity/VIPCustomer.java` | VIP customer: overrides `satisfactionLossPerTurn()` (5) and `applyCustomerPricing()` (90%, rounded half up). |
| `src/main/java/rush/entity/CriticCustomer.java` | Critic customer: overrides `satisfactionLossPerTurn()` (12) and `onServed()` (service-speed reaction: +10 if served within 2 turns of ordering, otherwise -10). |
| `runs/stage2-victory.txt` | Scripted base-setting victory with a VIP and a Critic (VIP discount and impressed Critic visible). |
| `runs/stage2-five-payers.txt` | Scripted alternative victory showing the constrained-staff trade-off (5 payers, unimpressed Critic). |
| `runs/stage2-defeat.txt` | Scripted base-setting defeat (average satisfaction below 60). |
| `runs/logs/stage2-victory.log`, `runs/logs/stage2-five-payers.log`, `runs/logs/stage2-defeat.log` | Saved transcripts of the three runs, produced by `./test.sh`. |
| `docs/TRANSITIONS.md` | This document. |

#### Modified files

| File | What changed | Why (design reason) |
|---|---|---|
| `src/main/java/rush/entity/Customer.java` | Added the hooks `public long applyCustomerPricing(long)` (default: unchanged) and `protected String onServed(int)` (default: no reaction), plus `protected final changeSatisfaction(int)` (clamped 0 to 100). `receiveServedOrder()` became the `final` template `receiveServedOrder(int turn)`, which sets EATING and then calls `onServed`. | Type-specific behaviour has to be dispatched through the customer object. These are the extension points the new subclasses override. Callers keep using `Customer` only. |
| `src/main/java/rush/entity/Waiter.java` | `serve()` passes the turn to `receiveServedOrder(turn)` and appends any reaction text to the serve message. | The Critic needs the serve turn to judge service speed, and its reaction must reach the player as part of the serve event. |
| `src/main/java/rush/control/BillCalculator.java` | The locked price is now `customer.applyCustomerPricing(item price)` instead of the item price. | The single pricing place now includes customer pricing (the VIP discount) without any type check. |
| `src/main/java/rush/control/ArrivalSchedule.java` | Replaced the all-Regular list with a small builder of `(turn, CustomerFactory)` slots. The factory is a functional interface satisfied by constructor references (`VIPCustomer::new`). `base()` is now Regular, VIP, Regular, Critic, Regular, Regular. `regulars()` is kept with the same signature. | A small registration change. This is the only class that names concrete customer types. The builder lets tests create mixed schedules. |
| `src/test/java/rush/RulesSelfTest.java` | Added 7 Stage 2 test groups (VIP loss and pricing, Critic loss and price, impressed/unimpressed with cap and floor, base schedule types, and a static no-type-check scan of `control` and `boundary`). The Stage 1 test groups are unchanged. | Evidence for the new rules and that the old ones still hold. |
| `src/test/java/rush/entity/EntityTestSupport.java` | Calls `receiveServedOrder(turn)` (new signature). | Follows the `Customer` template change. |
| `runs/expected.txt` | Replaced the Stage 1 script lines with the three Stage 2 runs. | The Stage 1 scripts narrate the all-Regular schedule. They and their logs are kept, and they are replayed from the `stage-1` tag. |
| `docs/RULES_AND_ASSUMPTIONS.md` | Added the Stage 2 section (type table, Critic rule, decisions D12 to D16, the trade-off). | Documents the new rules. |
| `docs/TEST_NOTES.md` | Added the Stage 2 section (new tests, Runs 3 to 5). | Reproducible evidence. |
| `README.md` | Replay commands for the Stage 2 runs and a customer-type table. | User-facing documentation. |

#### Deleted files

None.

#### Evidence earlier behaviour is preserved

- All 10 Stage 1 test groups in `RulesSelfTest` pass unchanged under Stage 2: the spec section 3.3 walkthrough value by value, Waiter/Chef one task per turn, invalid choices keeping the task, READY-this-turn not servable, Cashier order, abandonment, money, seeded choices, victory boundaries and full game length. They build their own all-Regular schedules through `ArrivalSchedule.regulars(...)`, whose signature did not change.
- `./test.sh` at the `stage-2` commit prints `ALL CHECKS PASSED` (17 test groups, 0 failures, plus the three scripted runs).
- `git worktree add ../rr-verify stage-1 && (cd ../rr-verify/restaurant-rush && ./test.sh)` still passes, so Stage 1 stays runnable on its own.
- Seed 2002 food choices are identical in both stages (C1 Salad, C2 Pasta, C3 Salad, C4 Burger, C5 Pasta), because a customer's choice depends only on the seed and its arrival index, not on its type.

#### Design note

Polymorphism removed the need for type checks. The rules that differ by customer type are each one overridable method on `Customer`:

- patience: `satisfactionLossPerTurn()`, used by the `final` template `applyWaitingPenalty()`
- pricing: `applyCustomerPricing()`, called by `BillCalculator`
- reaction to service: `onServed()`, called by the `final` template `receiveServedOrder()`

The turn engine (`GameController`), the staff classes other than the one-line `Waiter.serve` change, `Restaurant`, `Order`, `Table`, `Menu`, `Money` and the whole boundary (`ConsoleUI`, `InputReader`) were **not modified**. They work with `Customer` and so are closed to modification for new customer types. A new type needs one new subclass and one line in `ArrivalSchedule`. The Gate 2 grep `grep -rnE "instanceof|getClass\(\)" src/main/java/rush/control src/main/java/rush/boundary` returns nothing, and `RulesSelfTest` checks the same thing.

### Stage 2 → Stage 3: New role and menu policy

Compare: https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-/compare/stage-2...stage-3

#### New files

| File | Purpose |
|---|---|
| `src/main/java/rush/entity/Host.java` | New staff role (`extends Staff`): seats at most one queued customer per turn, reusing the seating rule in `Restaurant.seatCustomer`. |
| `src/main/java/rush/entity/ComboMeal.java` | `extends MenuItem`, **composed of** two base items: summed preparation units, 90% of the combined price (half up). Rejects null and nested combos; `describe()` shows the components. |
| `src/main/java/rush/control/HappyHourPolicy.java` | The Happy Hour policy: once-per-game activation, the 2-turn active window and remaining count, the 20% discount step and the +15 start-of-turn recovery. |
| `runs/stage3-victory.txt`, `runs/stage3-defeat.txt`, `runs/stage3-features-demo.txt` | Scripted Stage 3 runs (the demo shows Host seating, combos, Happy Hour recovery and discount, VIP and Critic behaviour). |
| `runs/logs/stage3-victory.log`, `runs/logs/stage3-defeat.log`, `runs/logs/stage3-features-demo.log` | Saved transcripts of the three runs, produced by `./test.sh`. |

#### Modified files

| File | What changed | Why (design reason) |
|---|---|---|
| `src/main/java/rush/control/GameController.java` | Registers the Host (`hostSeat`, `hostWait`, reset each turn) and the Happy Hour phase (`activateHappyHour`, recovery on the second active turn in `beginTurn`). Adds read-only queries (`isHappyHourAvailable`, `getHappyHourRemainingTurns`, `hasHostActed`) and the ordering rule that closes the Host step once the Waiter acts and the Happy Hour offer once any staff act. | The turn engine owns the step order, so the new step and the new phase must be registered here. The existing Waiter/Chef/Cashier logic and the end-of-turn steps are unchanged. |
| `src/main/java/rush/control/BillCalculator.java` | Takes the `HappyHourPolicy` in its constructor and adds the Happy Hour step after customer pricing: menu (combo already 90%) -> customer (VIP 90%) -> Happy Hour (80%) -> lock. | This is the single pricing place, and the brief fixes the stacking order (VIP first, then Happy Hour, locked at order time). |
| `src/main/java/rush/entity/Menu.java` | `standard()` now also registers the two combos (Salad + Burger, Salad + Pasta) after the three base items. | A small registration change: combos are built from the same base item objects. |
| `src/main/java/rush/entity/Customer.java` | Added `public final boolean recoverWhileWaiting(int points)`: +points (max 100), only while WAITING / SEATED / ORDERED. | Recovery is a satisfaction rule on the customer's own state, so it belongs with `applyWaitingPenalty()`. It is `final` so subclasses cannot change who is eligible. |
| `src/main/java/rush/boundary/ConsoleUI.java` | Asks the Happy Hour question (only while it is available), adds the Host prompt before the Waiter, and shows the Happy Hour status and the Host's valid targets. | The boundary must collect the new decisions and show the new state. It still only forwards commands to the controller. |
| `src/main/java/rush/boundary/InputReader.java` | Javadoc only: end of input answers "No" to yes/no questions. | Documents behaviour that the Happy Hour prompt now uses. No code change. |
| `src/test/java/rush/RulesSelfTest.java` | Added 5 Stage 3 test groups (combos, Happy Hour pricing, timing and recovery, Host seating and ordering). Stage 1 and 2 test groups are unchanged. | Evidence for the new rules and that the old ones still hold. |
| `runs/expected.txt` | Replaced the Stage 2 lines with the three Stage 3 runs. | Earlier scripts do not answer the new Host and Happy Hour prompts; they and their logs are kept and replayed from their tags. |
| `docs/RULES_AND_ASSUMPTIONS.md`, `docs/TEST_NOTES.md` | Stage 3 sections (rules, decisions D17 to D21, new tests, Runs 6 to 8). | Documentation. |
| `docs/TRANSITIONS.md` | This section. | Change analysis. |
| `README.md` | Final README (overview, build/run, how to play, runs, rules, architecture, transitions, video timestamps). | Final documentation. |

`MenuItem` was **not** changed: its public constructor already allowed `ComboMeal` to subclass it. `Waiter`, `Chef`, `Cashier`, `Restaurant`, `Order`, `Table`, the customer subclasses, `ArrivalSchedule` and `GameConfig` were not changed either.

#### Deleted files

None.

#### Evidence earlier behaviour is preserved

- All 10 Stage 1 and all 7 Stage 2 test groups in `RulesSelfTest` pass unchanged under Stage 3 (22 groups, 468 checks, 0 failures). They drive only the Waiter and Chef, which still works, because the Host step and the Happy Hour offer are optional and only *closed* by later steps (rule D18).
- The spec section 3.3 walkthrough still gives 92 / 84 / 76 / 76 / READY_TO_PAY / PAID 1800 with 76 recorded.
- `./test.sh` at `stage-3` prints `ALL CHECKS PASSED`, and the worktree checks of `stage-1` and `stage-2` still pass on their own.

#### Design note

New behaviour came in as new classes plugged into existing extension points:

- **Host** reuses the `Staff` task-per-turn base class and the existing `Restaurant.seatCustomer` rule. No seating logic was copied.
- **ComboMeal** is a `MenuItem`, so `Order`, `Chef`, `Menu`, the customers' choice rule and `BillCalculator` handle it without changes (Liskov substitution). It overrides only `describe()`.
- **Happy Hour** is a separate policy object in `control`, so pricing (`BillCalculator`) and the turn engine call it instead of growing conditionals. The VIP step is still dispatched through `Customer.applyCustomerPricing`.

The only edits to existing classes are registrations (controller step and phase, menu entries, pricing step) and the matching prompts in the boundary. The polymorphism grep (`instanceof` / `getClass()` in `control` and `boundary`) and the BCE grep (`System.out` / `Scanner` in `control` and `entity`) both still return nothing.

### Stage 3 → Stage 4: Web edition hooks

Compare: https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-/compare/stage-3...stage-4

The terminal game's rules do not change: Stage 4 adds an opt-in `GameConfig` setting and three optional `Customer` hooks, used only by the web game's Custom mode. The table is in `docs/TRANSITIONS.md`.

Commits: `git rev-parse --short stage-1` (and the same for each later tag) prints a stage's commit, or see the compare links above.

## Video timestamps

| # | Segment | Timestamp | Presenter |
|---|---|---|---|
| 1 | Game launching from the submitted project | [mm:ss] | [Member name] |
| 2 | Meaningful manager choices, incl. a constrained-staff decision | [mm:ss] | [Member name] |
| 3 | A dish going READY → SERVED → payment | [mm:ss] | [Member name] |
| 4 | A VIP or Critic behaviour | [mm:ss] | [Member name] |
| 5 | A Stage 3 feature (Host / ComboMeal / Happy Hour) | [mm:ss] | [Member name] |
| 6 | One victory and one defeat | [mm:ss] | [Member name] |
| 7 | Stage tag comparisons and the diagrams | [mm:ss] | [Member name] |

Video link: `[link - must stay accessible to graders]`

## Suggested video script pointers

Line numbers refer to the saved logs (`runs/logs/*.log`), which `./test.sh` regenerates identically.

| # | What to show | Script / seed / turn |
|---|---|---|
| 1 | `./build.sh` then `./run.sh --seed 2002` from a fresh clone. Banner, seed, menu with combos, turn 1 display. | Live, or `runs/logs/stage3-victory.log` from line 1 (turn 1 at line 14) |
| 2 | Constrained Waiter: on turn 13 the single Waiter either serves the Critic (impressed, 4 payers) or seats C5 (5 payers, Critic unimpressed). Also the Stage 3 Chef switching to the Critic's order. | At tag `stage-2`: `stage2-victory.txt` vs `stage2-five-payers.txt`, seed 2002, turn 13 (line 396 of the five-payers log). Stage 3: `stage3-features-demo.txt` turns 10 to 12 (line 357) |
| 3 | O1 READY on turn 1, served turn 2, READY_TO_PAY turn 3, paid $15.00 on turn 4 | `stage3-victory.txt`, seed 2002, turns 1 to 4 (lines 14 to 125) |
| 4 | VIP combo locked at $19.44 (turn 4); Critic IMPRESSED (turn 13); Critic UNIMPRESSED (defeat, turn 15) | `stage3-victory.txt` turn 4 (line 125) and turn 13 (line 477); `stage3-defeat.txt` turn 15 (line 560) |
| 5 | Happy Hour on turn 4: recovery, Host seats VIP C2, Waiter takes the order the same turn, $21.60 -> $19.44 -> $15.55, O1 keeps $15.00; turn 7 invalid Host choice | `stage3-features-demo.txt`, seed 2002, turns 4 and 5 (line 123), turn 7 (line 238) |
| 6 | Final result screens | `stage3-victory.log` (RESULT at line 739) and `stage3-defeat.log` (RESULT at line 734) |
| 7 | `git tag -n`, `git diff --name-status stage-1 stage-2`, `git diff --name-status stage-2 stage-3`, the compare links, then the UML diagrams | Terminal + report |

## Submission checklist (not generated here)

- [ ] PDF report (final diagrams, design discussion, functional evidence, reflections)
- [ ] UML class diagram (must match the Stage 3 code) and UML sequence diagram
- [ ] Design discussion (abstractions table, SOLID where real, trade-offs, one alternative design, how choices change results)
- [ ] Signed Declaration of Original Work
- [ ] Each member's Declaration on Use of GAI Assistance
- [ ] Repository URL filled in above, tags pushed (`git push origin main --tags`)
- [ ] Video timestamps, presenters and link filled in above
- [ ] Zip named `LABGroup_AssignmentGrp.zip` (for example `FCS1_Grp1.zip`)
