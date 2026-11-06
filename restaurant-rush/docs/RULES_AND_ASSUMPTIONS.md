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

## 6. Stage 2: customer behaviour

| Type | Loss per waiting turn | Bill | Special behaviour |
|---|---|---|---|
| Regular | 8 | Full price | None |
| VIP | 5 | 10% off: `Money.percent(price, 90)` | None |
| Critic | 12 | Full price | Service-speed reaction (below) |

**Critic service-speed reaction.** When the Critic is served, the game compares `servedTurn - orderTakenTurn`.
- **At most 2 turns: impressed.** +10 satisfaction, capped at 100.
- **More than 2 turns: unimpressed.** -10 satisfaction, floored at 0. A served Critic never leaves because of this, since only customers still waiting for service can leave.

The reaction is reported in the serve event.

| # | Decision | Rationale |
|---|---|---|
| D12 | Critic threshold = 2 turns from order taken to served. | Every dish can meet it (Pasta: order and unit 1 on turn T, unit 2 on T+1, serve on T+2), so being impressed depends only on the manager giving the Critic kitchen and serving priority. That is a real trade-off against other customers. |
| D13 | Base-setting arrivals: T1 Regular, T4 **VIP**, T7 Regular, T10 **Critic**, T13 Regular, T16 Regular (still six). | The brief allows replacing two Regulars. Putting the Critic late makes its priority compete with C5's chance to pay. |
| D14 | Type-specific behaviour is dispatched through `Customer` methods: `satisfactionLossPerTurn()`, `applyCustomerPricing(long)` and `onServed(int)`, the last called from the final template `receiveServedOrder(int)`. `ArrivalSchedule` is the only class that names concrete customer types. | The brief requires polymorphism, not type-check chains, in the controller. |
| D15 | VIP pricing happens when the order is taken (`BillCalculator`), and the price is then locked. | The price is locked at order time. |
| D16 | `onServed` returns a short description of the reaction (or `null`) rather than `void`. | The reaction needs to reach the player as part of the serve event, without the controller knowing the customer type. |

**Constrained-staff consequence (seed 2002).** From turn 7 on, the Waiter needs 10 tasks in turns 7 to 16 for C5 to pay by turn 18. Those 10 tasks fit only if the Critic's food is served 3 turns after ordering. So the manager must choose between an impressed Critic with 4 payers ($49.20, average 79.8) and an unimpressed Critic with 5 payers ($67.20, average 70.2). Both runs are scripted: `runs/stage2-victory.txt` and `runs/stage2-five-payers.txt`.

## 7. Stage 3: new role and menu policy

### Host

- The Host seats at most **one** queued customer per turn at a free table, **in addition to** the Waiter. The Waiter can still seat.
- Host options: `1` seat a chosen queued customer at a chosen free table, `0` wait. Invalid choices are explained and re-asked, and the task is not used up.
- Execution order within step 3: **Happy Hour decision -> Host -> Waiter -> Chef**. Because the Host acts first, the Waiter may take the order of a customer the Host seated this turn.

### ComboMeal

- `ComboMeal extends MenuItem` and is **composed of** two base `MenuItem`s (it keeps references to both). Preparation units = sum of the components' units; price = `Money.percent(sum of component prices, 90)`. The constructor rejects null components and combos of combos.
- Menu offerings, in order: Salad ($9.00, 1), Burger ($15.00, 1), Pasta ($18.00, 2), **Salad + Burger Combo** ($21.60, 2), **Salad + Pasta Combo** ($24.30, 3).

### Happy Hour

- At the start of each turn (after arrivals and the state display, before any staff act), while Happy Hour has not been used, the manager is asked `Activate Happy Hour now? (y/n)`. It can be activated **once per game** and is active on the activation turn and the next (activated on turn 18, it covers one turn). The display shows `ACTIVE (n turn(s) remaining incl. this one)`, `available` or `used`.
- **Recovery:** on each active turn, before any staff act, every WAITING, SEATED or ORDERED customer gains +15 satisfaction (capped at 100). LEFT, EATING, READY_TO_PAY and PAID customers are unaffected.
- **Discount:** orders **taken** while Happy Hour is active get 20% off.
- **Pricing pipeline (`BillCalculator`, one place, rounding half up after each step):** `base = item price` (a combo is already at 90%) -> `afterCustomer = customer.applyCustomerPricing(base)` (VIP 90%) -> `final = Happy Hour active ? 80% of afterCustomer : afterCustomer` -> `order.lockPrice(final)`. Example: VIP Burger in Happy Hour = 1500 -> 1350 -> **1080** ($10.80).
- The price is locked when the order is taken. Happy Hour never changes orders already placed, and never affects customers who have left.

| # | Decision | Rationale |
|---|---|---|
| D17 | Combo choice rule: each customer draws uniformly over all **5** offerings with its own seeded generator (rule D6); a combo is ordered exactly when the draw lands on a combo. | Keeps one simple, reproducible rule; combos are ordinary offerings. |
| D18 | Order within a turn: Happy Hour decision, then Host, then Waiter, then Chef. The controller enforces it by closing earlier steps once a later one has acted: the Host cannot act after the Waiter, and Happy Hour cannot be activated after the Host or Waiter has acted. The Host and the Happy Hour decision are optional (not acting = Wait / No). | The Host seats first so the Waiter can take that customer's order in the same turn. Closing rather than requiring earlier steps keeps the Stage 1/2 controller contract (Waiter then Chef) unchanged. |
| D19 | On the activation turn, recovery is applied at the moment of activation (after the start-of-turn display). On the second active turn it is applied in `beginTurn`, right after arrivals, so the start-of-turn display already shows recovered values. | Both happen before any staff act, as required. |
| D20 | Activating Happy Hour while nobody is waiting is allowed; the recovery then has no effect. | It is a manager choice; wasting it is a valid (bad) decision. |
| D21 | A newly arrived customer (satisfaction 100) is eligible for recovery but is capped at 100. | Follows directly from "maximum 100". |
