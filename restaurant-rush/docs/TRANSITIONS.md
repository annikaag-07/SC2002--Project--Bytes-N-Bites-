# Stage Transitions

Each section links to the comparison on GitHub. Its tables were generated from Git (`git diff --name-status <previous-tag> <tag>`), so every changed path is listed. The first tables give paths relative to `restaurant-rush/` (this Java game); the **Web game** table at the end of each section lists the top-level `README.md` and the `restaurant-rush-app/` files.

## Stage 1 → Stage 2: Customer behaviour

Compare: https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-/compare/stage-1...stage-2

### New files

| File | Purpose |
|---|---|
| `src/main/java/rush/entity/VIPCustomer.java` | VIP customer: overrides `satisfactionLossPerTurn()` (5) and `applyCustomerPricing()` (90%, rounded half up). |
| `src/main/java/rush/entity/CriticCustomer.java` | Critic customer: overrides `satisfactionLossPerTurn()` (12) and `onServed()` (service-speed reaction: +10 if served within 2 turns of ordering, otherwise -10). |
| `runs/stage2-victory.txt` | Scripted base-setting victory with a VIP and a Critic (VIP discount and impressed Critic visible). |
| `runs/stage2-five-payers.txt` | Scripted alternative victory showing the constrained-staff trade-off (5 payers, unimpressed Critic). |
| `runs/stage2-defeat.txt` | Scripted base-setting defeat (average satisfaction below 60). |
| `runs/logs/stage2-victory.log`, `runs/logs/stage2-five-payers.log`, `runs/logs/stage2-defeat.log` | Saved transcripts of the three runs, produced by `./test.sh`. |
| `docs/TRANSITIONS.md` | This document. |

### Modified files

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

### Deleted files

None.

### Evidence earlier behaviour is preserved

- All 10 Stage 1 test groups in `RulesSelfTest` pass unchanged under Stage 2: the spec section 3.3 walkthrough value by value, Waiter/Chef one task per turn, invalid choices keeping the task, READY-this-turn not servable, Cashier order, abandonment, money, seeded choices, victory boundaries and full game length. They build their own all-Regular schedules through `ArrivalSchedule.regulars(...)`, whose signature did not change.
- `./test.sh` at the `stage-2` commit prints `ALL CHECKS PASSED` (17 test groups, 0 failures, plus the three scripted runs).
- `git worktree add ../rr-verify stage-1 && (cd ../rr-verify/restaurant-rush && ./test.sh)` still passes, so Stage 1 stays runnable on its own.
- Seed 2002 food choices are identical in both stages (C1 Salad, C2 Pasta, C3 Salad, C4 Burger, C5 Pasta), because a customer's choice depends only on the seed and its arrival index, not on its type.

### Design note

Polymorphism removed the need for type checks. The rules that differ by customer type are each one overridable method on `Customer`:

- patience: `satisfactionLossPerTurn()`, used by the `final` template `applyWaitingPenalty()`
- pricing: `applyCustomerPricing()`, called by `BillCalculator`
- reaction to service: `onServed()`, called by the `final` template `receiveServedOrder()`

The turn engine (`GameController`), the staff classes other than the one-line `Waiter.serve` change, `Restaurant`, `Order`, `Table`, `Menu`, `Money` and the whole boundary (`ConsoleUI`, `InputReader`) were **not modified**. They work with `Customer` and so are closed to modification for new customer types. A new type needs one new subclass and one line in `ArrivalSchedule`. The Gate 2 grep `grep -rnE "instanceof|getClass\(\)" src/main/java/rush/control src/main/java/rush/boundary` returns nothing, and `RulesSelfTest` checks the same thing.

### Web game

| Status | File | What changed |
|---|---|---|
| Modified | `README.md` | Stage 2 mode: VIP and Critic customers and the random arrivals. |
| Modified | `restaurant-rush-app/README.md` | Stage 2 mode, arrivals, tests and actions. |
| Modified | `restaurant-rush-app/client/src/api/types.ts` | `CustomerType` adds VIP and Critic; `GameMode` is `stage2`. |
| Modified | `restaurant-rush-app/client/src/components/game/DecisionBox.tsx` | Warning while the Critic waits for food. |
| Modified | `restaurant-rush-app/client/src/components/game/Panels.tsx` | IMPRESSED / UNIMPRESSED event colours; Play again starts `stage2`. |
| Modified | `restaurant-rush-app/client/src/components/pixel/sprites.ts` | VIP (suit, gold tie) and Critic (beret, notepad) looks, also as penguins. |
| Modified | `restaurant-rush-app/client/src/content/tips.ts` | VIP and Critic tips. |
| Modified | `restaurant-rush-app/client/src/screens/GuideScreen.tsx` | VIP and Critic notes and arrivals. |
| Modified | `restaurant-rush-app/client/src/screens/MainMenu.tsx` | The Stage 2 mode card. |
| Modified | `restaurant-rush-app/client/src/styles.css` | `.type-VIP` and `.type-Critic` tags. |
| Modified | `restaurant-rush-app/client/src/theme/theme.css` | VIP and Critic colours in every theme. |
| Added | `restaurant-rush-app/server/src/rush/web/ArrivalPlan.java` | Seeded arrival plan: each slot Regular 50 / VIP 25 / Critic 25, repaired to at least 1 VIP, 1 Critic and 2 Regulars. |
| Modified | `restaurant-rush-app/server/src/rush/web/GameSession.java` | Builds the back end's `ArrivalSchedule` from the plan (the only adapter code that names customer classes); test-only `startWith` takes an explicit arrival list. |
| Modified | `restaurant-rush-app/server/src/rush/web/ModeFeatures.java` | The mode is `stage2`. |
| Modified | `restaurant-rush-app/server/src/rush/web/WebServer.java` | Javadoc: `mode=stage2`. |
| Modified | `restaurant-rush-app/server/test/rush/web/AdapterSelfTest.java` | Parity with the CLI's Stage 2 logs (fixed arrivals) and Stage 1 logs (six Regulars). |
| Modified | `restaurant-rush-app/server/test/rush/web/ModeSelfTest.java` | Arrival-plan invariants over 5,000 seeds; Stage 2 mode, timing and bot checks. |
| Modified | `restaurant-rush-app/test.sh` | Header comment. |
| Rebuilt | `restaurant-rush-app/client/dist/` | The built front end (`./build.sh`): D `assets/index-Bff8UhqO.css`, D `assets/index-BpanGB2h.js`, A `assets/index-CjDSG55d.css`, A `assets/index-fNLw5Akj.js`, M `index.html`. The asset names carry a content hash. |

## Stage 2 → Stage 3: New role and menu policy

Compare: https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-/compare/stage-2...stage-3

### New files

| File | Purpose |
|---|---|
| `src/main/java/rush/entity/Host.java` | New staff role (`extends Staff`): seats at most one queued customer per turn, reusing the seating rule in `Restaurant.seatCustomer`. |
| `src/main/java/rush/entity/ComboMeal.java` | `extends MenuItem`, **composed of** two base items: summed preparation units, 90% of the combined price (half up). Rejects null and nested combos; `describe()` shows the components. |
| `src/main/java/rush/control/HappyHourPolicy.java` | The Happy Hour policy: once-per-game activation, the 2-turn active window and remaining count, the 20% discount step and the +15 start-of-turn recovery. |
| `runs/stage3-victory.txt`, `runs/stage3-defeat.txt`, `runs/stage3-features-demo.txt` | Scripted Stage 3 runs (the demo shows Host seating, combos, Happy Hour recovery and discount, VIP and Critic behaviour). |
| `runs/logs/stage3-victory.log`, `runs/logs/stage3-defeat.log`, `runs/logs/stage3-features-demo.log` | Saved transcripts of the three runs, produced by `./test.sh`. |

### Modified files

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

### Deleted files

None.

### Evidence earlier behaviour is preserved

- All 10 Stage 1 and all 7 Stage 2 test groups in `RulesSelfTest` pass unchanged under Stage 3 (22 groups, 468 checks, 0 failures). They drive only the Waiter and Chef, which still works, because the Host step and the Happy Hour offer are optional and only *closed* by later steps (rule D18).
- The spec section 3.3 walkthrough still gives 92 / 84 / 76 / 76 / READY_TO_PAY / PAID 1800 with 76 recorded.
- `./test.sh` at `stage-3` prints `ALL CHECKS PASSED`, and the worktree checks of `stage-1` and `stage-2` still pass on their own.

### Design note

New behaviour came in as new classes plugged into existing extension points:

- **Host** reuses the `Staff` task-per-turn base class and the existing `Restaurant.seatCustomer` rule. No seating logic was copied.
- **ComboMeal** is a `MenuItem`, so `Order`, `Chef`, `Menu`, the customers' choice rule and `BillCalculator` handle it without changes (Liskov substitution). It overrides only `describe()`.
- **Happy Hour** is a separate policy object in `control`, so pricing (`BillCalculator`) and the turn engine call it instead of growing conditionals. The VIP step is still dispatched through `Customer.applyCustomerPricing`.

The only edits to existing classes are registrations (controller step and phase, menu entries, pricing step) and the matching prompts in the boundary. The polymorphism grep (`instanceof` / `getClass()` in `control` and `boundary`) and the BCE grep (`System.out` / `Scanner` in `control` and `entity`) both still return nothing.

### Web game

| Status | File | What changed |
|---|---|---|
| Modified | `README.md` | Stage 3 mode: the Host, combo meals and Happy Hour. |
| Modified | `restaurant-rush-app/README.md` | Stage 3 mode, the API-only `classic` mode, the Host and Happy Hour actions. |
| Modified | `restaurant-rush-app/client/public/sounds/README.txt` | Lists the Happy Hour sound. |
| Modified | `restaurant-rush-app/client/src/animation/choreography.ts` | The Host's seating walk and the menu drop; combo orders. |
| Modified | `restaurant-rush-app/client/src/api/types.ts` | Manager and Host roles and phases, combo `components`, the `happyHour` state; `GameMode` is `stage3`. |
| Modified | `restaurant-rush-app/client/src/components/ThemePicker.tsx` | Host and pedestal in the theme preview. |
| Modified | `restaurant-rush-app/client/src/components/game/DecisionBox.tsx` | Happy Hour explanation for the Manager; role-aware warnings. |
| Modified | `restaurant-rush-app/client/src/components/game/Hud.tsx` | Happy Hour chip. |
| Modified | `restaurant-rush-app/client/src/components/game/Panels.tsx` | Happy Hour events; Play again starts `stage3`. |
| Modified | `restaurant-rush-app/client/src/components/game/World.tsx` | The Host at their pedestal (replaces the door waypoint); combos drawn as two dishes. |
| Modified | `restaurant-rush-app/client/src/components/pixel/Pixel.tsx` | Doc comment: a combo's food icon shows both dishes. |
| Modified | `restaurant-rush-app/client/src/components/pixel/sprites.ts` | Host and Manager looks and the menu-book item. |
| Modified | `restaurant-rush-app/client/src/components/pixel/themeSprites.ts` | Pedestal sprite per theme. |
| Modified | `restaurant-rush-app/client/src/content/tips.ts` | Host, combo and Happy Hour tips. |
| Modified | `restaurant-rush-app/client/src/screens/GuideScreen.tsx` | Host, combo and Happy Hour sections. |
| Modified | `restaurant-rush-app/client/src/screens/MainMenu.tsx` | The Stage 3 mode card; the Host in the cast. |
| Modified | `restaurant-rush-app/client/src/sound/soundManager.ts` | Happy Hour jingle. |
| Modified | `restaurant-rush-app/client/src/store/gameStore.ts` | Sounds for `hostSeat` and `happyHourYes`. |
| Modified | `restaurant-rush-app/client/src/styles.css` | Host spot, pedestal, Happy Hour chip and preview styles (the door waypoint styles go). |
| Modified | `restaurant-rush-app/client/src/theme/theme.css` | Host, pedestal and menu-book colours. |
| Modified | `restaurant-rush-app/server/src/rush/web/ArrivalPlan.java` | `kinds(seed, mode)`: `classic` uses the terminal game's fixed list. |
| Modified | `restaurant-rush-app/server/src/rush/web/GameSession.java` | Happy Hour and Host phases; the menu with combos. |
| Modified | `restaurant-rush-app/server/src/rush/web/ModeFeatures.java` | `stage3` and the API-only `classic` mode. |
| Modified | `restaurant-rush-app/server/src/rush/web/StateJson.java` | Happy Hour state, combo components, Host and Happy Hour decisions. |
| Modified | `restaurant-rush-app/server/src/rush/web/WebServer.java` | Javadoc: `mode=stage3` or `mode=classic`. |
| Modified | `restaurant-rush-app/server/test/rush/web/AdapterSelfTest.java` | `classic`-mode parity with the CLI's Stage 3 logs. |
| Modified | `restaurant-rush-app/server/test/rush/web/ModeSelfTest.java` | Host timelines, mixed dishes and the classic fixed list. |
| Modified | `restaurant-rush-app/test.sh` | Header comment. |
| Rebuilt | `restaurant-rush-app/client/dist/` | The built front end (`./build.sh`): A `assets/index-CYuGBQO2.js`, D `assets/index-CjDSG55d.css`, A `assets/index-e2PfZ8Ka.css`, D `assets/index-fNLw5Akj.js`, M `index.html`, M `sounds/README.txt`. The asset names carry a content hash. |
