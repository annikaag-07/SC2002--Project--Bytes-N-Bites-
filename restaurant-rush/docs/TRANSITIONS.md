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
