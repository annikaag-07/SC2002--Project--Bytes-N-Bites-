# Rules and Assumptions

This file is the single source of truth for how the game interprets the assignment brief. Each decision has a one-line rationale.

## 1. Base setting

| Setting | Value |
|---|---|
| Turns | 18. All 18 are always played; there is no early ending. |
| Tables | 2 (T1, T2) |
| Staff | Waiter x1 (at most 1 task per turn), Chef x1 (at most 1 preparation unit per turn), Cashier x1 (at most 1 payment per turn, automatic) |
| Arrivals | One customer at the start of turns 1, 4, 7, 10, 13, 16 (ids C1 to C6, satisfaction 100) |
| Victory | After turn 18: paid customers >= 4 **and** revenue >= 4500 cents **and** average recorded satisfaction of paid customers >= 60, compared exactly as `sum >= 60 * count` |

| Menu item | Price | Preparation units |
|---|---|---|
| Salad | $9.00 (900 cents) | 1 |
| Burger | $15.00 (1500 cents) | 1 |
| Pasta | $18.00 (1800 cents) | 2 |

## 2. States

- **Customer:** `WAITING` (in queue) -> `SEATED` (at a table, no order) -> `ORDERED` (order taken, not served) -> `EATING` (served) -> `READY_TO_PAY` -> `PAID`; or `LEFT` (abandoned).
- **Order:** `PENDING` (taken, 0 units done) -> `PREPARING` -> `READY` -> `SERVED` -> `PAID`; or `CANCELLED`.
- Tracked on the domain objects: customer `arrivalTurn`, `readyToPayTurn` and `recordedSatisfaction` (set at payment); order `takenTurn`, `readyTurn`, `servedTurn`, `unitsDone` and `lockedPriceCents`.

## 3. Turn sequence

1. **Arrivals.** A scheduled customer joins the end of the queue with satisfaction 100.
2. **Start-of-turn display.** Turn number, revenue, average paid satisfaction (or `N/A`), the waiting queue, tables, every customer's type, satisfaction and state, every order's status, progress (`units x/y`) and locked price, and each staff member's task with its valid targets.
3. **Manager choices with kitchen and serving.** The manager is asked for each staff member in execution order, and each valid choice runs immediately: **Waiter**, then **Chef**.
   - Waiter: `1` seat a chosen queued customer (any position in the queue) at a chosen free table, `2` take a chosen SEATED customer's order, `3` serve a chosen READY order, `0` wait.
   - Serving requires `status == READY && readyTurn < currentTurn`.
   - Chef: `1` advance a chosen PENDING/PREPARING order by one unit, `0` wait. The Chef may work on an order the Waiter took this turn. When `unitsDone == prepUnits` the order becomes READY with `readyTurn = currentTurn`. The Chef may wait even when work exists.
   - An invalid choice prints the reason and asks the same staff member again; the task is not used up.
4. **Eating and payment.**
   - (a) **Cashier:** among READY_TO_PAY customers with `readyToPayTurn < currentTurn`, charge one: earliest `readyToPayTurn`, ties broken by earliest arrival. Payment adds the locked price to revenue, records the customer's current satisfaction, marks customer and order PAID and frees the table.
   - (b) **Eating:** every EATING customer with `servedTurn < currentTurn` becomes READY_TO_PAY with `readyToPayTurn = currentTurn`.
5. **Waiting and abandonment.** Every WAITING, SEATED or ORDERED customer loses their per-turn amount (minimum 0), including on the arrival turn. Anyone at 0 becomes LEFT: removed from the queue, order CANCELLED whatever its progress, table freed.
6. **End-of-turn results.** Served count (customers ever served), unhappy departures, paid count, revenue, average paid satisfaction (`N/A` if none), turns used `t/18`.
7. **Result** (only after turn 18). VICTORY or DEFEAT, with each criterion shown as met or not met (actual vs required). The final line starts `RESULT: VICTORY` or `RESULT: DEFEAT`.

## 4. Documented decisions

| # | Decision | Rationale |
|---|---|---|
| D1 | Waiter-before-Chef ordering is done by choosing and executing staff tasks in sequence (the Waiter is asked and acts, then the Chef). The controller also rejects a Chef command until the Waiter has acted or waited. | The brief says the Waiter acts first; executing in prompt order makes that automatic and visible to the player. |
| D2 | Cashier "oldest first" = earliest `readyToPayTurn`, then earliest arrival. | The customer who has waited longest at the till is served first; arrival order is a stable tie-break. |
| D3 | Customers lose satisfaction on their arrival turn. | Matches the spec section 3.3 example (100 -> 92 on turn 1). |
| D4 | The turn-16 arrival can never pay; at most 5 customers pay. | Stated as intended in the brief (seat 16, order 17, serve 18 at the earliest). |
| D5 | An abandoned customer's order is cancelled at any progress, including READY food; it never earns revenue. | "Revenue never includes unpaid or abandoned orders"; discarding is the simplest consistent rule. |
| D6 | Food choice uses a per-customer `java.util.Random` seeded with `gameSeed * 1_000_003 + arrivalIndex`, drawn uniformly over the current menu's offerings when the order is taken. Default seed 2002, overridable with `--seed`. | Customers choose, not the manager; each customer's choice is independent of the order of the manager's actions, so runs are reproducible. |
| D7 | Rounding is half up to the nearest cent after **each** percentage step: `(cents * percent + 50) / 100`. | One rule in one helper (`Money.percent`), applied the same way everywhere. |
| D8 | The Waiter may seat any queued customer, not only the head of the queue. | Seating priority is a meaningful player choice. |
| D9 | Because the Waiter serves at most one order per turn, at most one customer becomes READY_TO_PAY per turn, so the Cashier never has a backlog in the base setting. The oldest-first tie-break is still implemented and tested directly. | Explains why the tie-break never shows up in a normal run. |
| D10 | Average satisfaction is displayed to one decimal, rounded half up; the victory check compares the exact integer sum. | Display is readable; the check has no rounding error. |
| D11 | When scripted input ends, every later prompt is answered with Wait (or No), and the game still plays all 18 turns. | Scripts can stop early and the game never crashes. |

## 5. Deviations from the implementation plan

| Planned | Implemented | Why |
|---|---|---|
| `ActionResult` in `control` | `ActionResult` in `entity` | Staff classes (entities) return it; keeping it in `entity` avoids an entity -> control dependency. |
| Arrival turns in `GameConfig` | Arrival turns in `ArrivalSchedule` | The schedule already owns who arrives when; storing the turns twice would allow them to disagree. |
| Scope check: a case-insensitive grep of `src/` for the names of the features the brief adds in later stages returns nothing | It matches only the word "unhappy" ("Unhappy departures", the brief's own section 3.5 wording). The check used is the same grep with `grep -vi unhappy` applied, which returns nothing. | Keeping the brief's terminology matters more than a literal substring match. |
