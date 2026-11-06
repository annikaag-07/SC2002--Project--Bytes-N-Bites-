# Test Notes

All runs are reproducible: the same seed and the same input script always produce the same transcript. `./test.sh` replays every run listed in `runs/expected.txt`, saves the transcript to `runs/logs/<script>.log` and checks the final `RESULT:` line. Scripts contain `#` comment lines that label each turn; the game echoes them in the log.

Rule tests: `src/test/java/rush/RulesSelfTest.java` (run by `./test.sh`). It covers the spec section 3.3 walkthrough value by value, one task per staff member per turn, invalid choices keeping the task, READY-this-turn not servable, Cashier order, abandonment, money rounding, seeded choices and the victory boundaries.

## Stage 1

Seed 2002 food choices (drawn by each customer when its order is taken): C1 Salad, C2 Pasta, C3 Salad, C4 Burger, C5 Pasta.

### Run 1: victory

- **Command:** `./run.sh --seed 2002 < runs/stage1-victory.txt`
- **Seed:** 2002
- **Log:** [`runs/logs/stage1-victory.log`](../runs/logs/stage1-victory.log)

| Turn | Waiter | Chef | Key events |
|---|---|---|---|
| 1 | Seat C1 at T1 | Wait | C1 arrives |
| 2 | Take C1's order | Advance O1 | C1 chooses Salad, locked $9.00; O1 READY |
| 3 | Serve O1 | Wait | O1 SERVED |
| 4 | Seat C2 at T2 | Wait | C2 arrives; C1 READY_TO_PAY |
| 5 | Take C2's order | Advance O2 (1/2) | C2 chooses Pasta, locked $18.00; **C1 PAID $9.00 (sat 84)** |
| 6 | Wait | Advance O2 (2/2) | O2 READY |
| 7 | Serve O2 | Wait | C3 arrives (waits a turn); O2 SERVED |
| 8 | Seat C3 at T1 | Wait | C2 READY_TO_PAY |
| 9 | Take C3's order | Advance O3 | C3 chooses Salad, locked $9.00; O3 READY; **C2 PAID $18.00 (sat 76)** |
| 10 | Serve O3 | Wait | C4 arrives; O3 SERVED |
| 11 | Seat C4 at T2 | Wait | C3 READY_TO_PAY |
| 12 | Take C4's order | Advance O4 | C4 chooses Burger, locked $15.00; O4 READY; **C3 PAID $9.00 (sat 76)** |
| 13 | Serve O4 | Wait | C5 arrives; O4 SERVED |
| 14 | Seat C5 at T1 | Wait | C4 READY_TO_PAY |
| 15 | Take C5's order | Advance O5 (1/2) | C5 chooses Pasta, locked $18.00; **C4 PAID $15.00 (sat 76)** |
| 16 | Wait | Advance O5 (2/2) | C6 arrives; O5 READY |
| 17 | Serve O5 | Wait | O5 SERVED |
| 18 | Wait | Wait | C5 READY_TO_PAY (too late to pay) |

Final summary from the log:

```
Served: 5 | Unhappy departures: 0 | Paid: 4 | Revenue: $51.00 | Avg satisfaction (paid): 78.0 | Turns used: 18/18

================================================================
 FINAL RESULT after 18 turns
================================================================
Paid customers:       4 (need >= 4) MET
Revenue:              $51.00 (need >= $45.00) MET
Average satisfaction: 78.0 (need >= 60) MET
RESULT: VICTORY
```

**Why it wins.** The Waiter is the bottleneck: every customer needs three Waiter tasks (seat, take order, serve), and arrivals come every three turns. The manager never lets the Waiter idle while a task is useful, and the Chef cooks each order the same turn it is taken, so one-unit dishes are served after only two waiting turns (satisfaction 84 or 76). On turns 7, 10 and 13 the manager serves the current customer's food before seating the newcomer. This costs the newcomer 8 points, but stops the seated customer losing more and keeps the pipeline moving. Four customers pay $51.00 in total with an average of 78.0. C5 is served on turn 17, too late to pay, and C6 cannot pay at all (rule D4).

### Run 2: defeat

- **Command:** `./run.sh --seed 2002 < runs/stage1-defeat.txt`
- **Seed:** 2002
- **Log:** [`runs/logs/stage1-defeat.log`](../runs/logs/stage1-defeat.log)

| Turn | Waiter | Chef | Key events |
|---|---|---|---|
| 1 | Serve O1 (**rejected**: no such order), `7` (**rejected**: not an option), then seat C1 at T1 | Advance O1 (**rejected**), then Wait | C1 arrives. Each rejection re-asks the same staff member, and no task is used. |
| 2 | Take C1's order | `abc` (**rejected**), then Advance O1 | C1 chooses Salad, locked $9.00; O1 READY |
| 3 | Seat C1 at T2 (**rejected**: C1 is not in the queue), then serve O1 | Wait | O1 SERVED |
| 4 to 7 | Wait | Wait | C2, C3 arrive and wait. C1 READY_TO_PAY on 4, **PAID $9.00 (sat 84)** on 5 |
| 8 to 18 | (script ended: `[input ended - defaulting to Wait]`) | (Wait) | C4, C5, C6 arrive. **C2 leaves unhappy on turn 16** after 13 waiting turns x 8 points. |

Final summary from the log:

```
Served: 1 | Unhappy departures: 1 | Paid: 1 | Revenue: $9.00 | Avg satisfaction (paid): 84.0 | Turns used: 18/18

================================================================
 FINAL RESULT after 18 turns
================================================================
Paid customers:       1 (need >= 4) NOT MET
Revenue:              $9.00 (need >= $45.00) NOT MET
Average satisfaction: 84.0 (need >= 60) MET
RESULT: DEFEAT
```

**Why it loses.** After serving C1, the manager stops giving the Waiter tasks. Nobody else is seated, so no other order is taken and nothing reaches the Cashier. Only one customer pays ($9.00), C2 runs out of patience and leaves, and the paid-count and revenue targets are missed. The run also shows invalid choices being explained and re-asked without using up the task, and that the game finishes all 18 turns after the input script ends.

## Stage 2

The base schedule is now C1 Regular, C2 **VIP**, C3 Regular, C4 **Critic**, C5 Regular, C6 Regular (rule D13). Seed 2002 food choices are unchanged (C1 Salad, C2 Pasta, C3 Salad, C4 Burger, C5 Pasta) because each customer's choice depends only on the seed and its arrival index.

**Stage 1 scripts.** `runs/stage1-*.txt` and their logs are kept, but their lines were removed from `runs/expected.txt`. They were written for the all-Regular Stage 1 schedule, so the narrated satisfaction values and prices no longer apply under the Stage 2 base schedule. To replay them as documented, check out the `stage-1` tag (`git checkout stage-1 && ./test.sh`). The Stage 1 rule tests still run unchanged in `RulesSelfTest`, because they build their own all-Regular schedules.

New rule tests: VIP loses 5 per turn, and VIP Burger / Salad / Pasta lock at 1350 / 810 / 1620 cents. The Critic loses 12 per turn and pays full price. The Critic is impressed when served 1 or 2 turns after ordering (+10, capped at 100) and unimpressed at 3 or more turns (-10, floored at 0; a served Critic never leaves). The base schedule contains a VIP and a Critic. A static check confirms that `control` and `boundary` contain no `instanceof` or `getClass()`.

### Run 3: Stage 2 victory (impressed Critic)

- **Command:** `./run.sh --seed 2002 < runs/stage2-victory.txt`
- **Seed:** 2002
- **Log:** [`runs/logs/stage2-victory.log`](../runs/logs/stage2-victory.log)

| Turn | Waiter | Chef | Key events |
|---|---|---|---|
| 1 | Seat C1 at T1 | Wait | C1 (Regular) arrives |
| 2 | Take C1's order | Advance O1 | Salad, locked $9.00; O1 READY |
| 3 | Serve O1 | Wait | O1 SERVED |
| 4 | Seat C2 at T2 | Wait | C2 (**VIP**) arrives; C1 READY_TO_PAY |
| 5 | Take C2's order | Advance O2 (1/2) | Pasta, **VIP price locked $16.20** (90% of $18.00); C1 PAID $9.00 (sat 84) |
| 6 | Wait | Advance O2 (2/2) | O2 READY |
| 7 | Serve O2 | Wait | C3 arrives; O2 SERVED |
| 8 | Seat C3 at T1 | Wait | C2 READY_TO_PAY |
| 9 | Take C3's order | Advance O3 | Salad $9.00; O3 READY; C2 PAID $16.20 (sat 85: VIP lost only 5 per turn) |
| 10 | Serve O3 | Wait | C4 (**Critic**) arrives; O3 SERVED |
| 11 | Seat C4 at T2 | Wait | C3 READY_TO_PAY |
| 12 | Take C4's order | Advance O4 | Burger $15.00; O4 READY; C3 PAID $9.00 (sat 76) |
| 13 | Serve O4 | Wait | C5 arrives; **Critic IMPRESSED** (served 1 turn after ordering): 64 -> 74 |
| 14 | Seat C5 at T1 | Wait | C4 READY_TO_PAY |
| 15 | Take C5's order | Advance O5 (1/2) | Pasta $18.00; C4 PAID $15.00 (sat 74) |
| 16 | Wait | Advance O5 (2/2) | C6 arrives; O5 READY |
| 17 | Serve O5 | Wait | O5 SERVED |
| 18 | Wait | Wait | C5 READY_TO_PAY (too late to pay) |

```
Served: 5 | Unhappy departures: 0 | Paid: 4 | Revenue: $49.20 | Avg satisfaction (paid): 79.8 | Turns used: 18/18

================================================================
 FINAL RESULT after 18 turns
================================================================
Paid customers:       4 (need >= 4) MET
Revenue:              $49.20 (need >= $45.00) MET
Average satisfaction: 79.8 (need >= 60) MET
RESULT: VICTORY
```

**Why it wins.** It uses the same Waiter-first pipeline as Stage 1. The Critic's Burger is cooked the turn it is ordered and served the next turn, so the Critic is impressed and records 74 despite losing 12 per waiting turn. The VIP discount lowers revenue to $49.20, still above $45.00.

### Run 4: Stage 2 alternative victory (five payers, unimpressed Critic)

- **Command:** `./run.sh --seed 2002 < runs/stage2-five-payers.txt`
- **Log:** [`runs/logs/stage2-five-payers.log`](../runs/logs/stage2-five-payers.log)

Identical to Run 3 up to turn 12. Then:

| Turn | Waiter | Chef | Key events |
|---|---|---|---|
| 13 | Seat C5 at T1 (instead of serving the Critic) | Wait | C5 arrives |
| 14 | Take C5's order | Advance O5 (1/2) | Pasta $18.00 |
| 15 | Serve O4 | Advance O5 (2/2) | **Critic UNIMPRESSED** (3 turns after ordering): 40 -> 30 |
| 16 | Serve O5 | Wait | C6 arrives; O5 SERVED; C4 READY_TO_PAY |
| 17 | Wait | Wait | C4 PAID $15.00 (sat 30) |
| 18 | Wait | Wait | **C5 PAID $18.00** (sat 76) |

```
Served: 5 | Unhappy departures: 0 | Paid: 5 | Revenue: $67.20 | Avg satisfaction (paid): 70.2 | Turns used: 18/18
...
RESULT: VICTORY
```

**Why it matters.** This is the constrained-staff decision. On turn 13 the single Waiter can either serve the Critic (impressed) or seat C5 (who can then still pay by turn 18). Seating C5 earns $18.00 more and a fifth payer, but the Critic's recorded satisfaction drops from 74 to 30.

### Run 5: Stage 2 defeat

- **Command:** `./run.sh --seed 2002 < runs/stage2-defeat.txt`
- **Log:** [`runs/logs/stage2-defeat.log`](../runs/logs/stage2-defeat.log)

| Turn | Waiter | Chef | Key events |
|---|---|---|---|
| 1 | Seat C1 at T1 | Wait | C1 arrives |
| 2 | Take C1's order | Wait (kitchen idles) | Salad $9.00 |
| 3 | Wait | Advance O1 | O1 READY |
| 4 | Seat C2 at T2 | Wait | C2 (VIP) arrives |
| 5 | Serve O1 | Wait | O1 SERVED (C1 now at 68) |
| 6 | Take C2's order | Advance O2 (1/2) | Pasta, VIP $16.20; C1 READY_TO_PAY |
| 7 | Seat C3 at T1 (**rejected: no table is free**), then Wait | Advance O2 (2/2) | C3 arrives; C1 PAID $9.00 (sat 68), T1 freed at end of turn |
| 8 | Seat C3 at T1 | Wait | |
| 9 | Serve O2 | Wait | O2 SERVED |
| 10 | Take C3's order | Advance O3 | C4 (Critic) arrives, no table free; Salad $9.00 |
| 11 | Serve O3 | Wait | C2 PAID $16.20 (sat 75) |
| 12 | Seat C4 at T2 | Wait | Critic seated after two turns in the queue (64 at end of turn) |
| 13 | Take C4's order | Wait (kitchen idles) | Burger $15.00; C3 PAID $9.00 (sat 68) |
| 14 | Seat C5 at T1 | Advance O4 | O4 READY |
| 15 | Take C5's order | Advance O5 (1/2) | Pasta $18.00 |
| 16 | Serve O4 | Advance O5 (2/2) | **Critic UNIMPRESSED** (3 turns after ordering): 28 -> 18 |
| 17 | Serve O5 | Wait | C4 READY_TO_PAY |
| 18 | Wait | Wait | C4 PAID $15.00 (sat 18) |

```
Served: 5 | Unhappy departures: 0 | Paid: 4 | Revenue: $49.20 | Avg satisfaction (paid): 57.3 | Turns used: 18/18

================================================================
 FINAL RESULT after 18 turns
================================================================
Paid customers:       4 (need >= 4) MET
Revenue:              $49.20 (need >= $45.00) MET
Average satisfaction: 57.3 (need >= 60) NOT MET
RESULT: DEFEAT
```

**Why it loses.** Four customers pay the same $49.20 as Run 3, but the idle Chef (turns 2 and 13) and late serving push every recorded satisfaction down. The Critic, who loses 12 per waiting turn, sits without a table on turns 10 and 11 and then gets slow service. Being unimpressed brings the Critic to 18, which drags the average to 57.25, below 60. Compared with Run 3, the only difference is the order and timing of the manager's choices.

## Stage 3

With the 5-item menu, each customer draws over five offerings, so seed 2002 choices change: C1 Burger, C2 (VIP) Salad + Burger Combo, C3 Salad + Pasta Combo, C4 (Critic) Pasta, C5 Salad + Burger Combo, C6 Burger. In Stage 3 every turn asks, in order: Happy Hour (y/n, only while unused), Host, Waiter, Chef.

**Earlier scripts.** `runs/stage1-*.txt`, `runs/stage2-*.txt` and their logs are kept, but their lines were removed from `runs/expected.txt`. They were written for the earlier menus and prompts (no Host or Happy Hour questions). Replay them from their tags: `git checkout stage-1` or `git checkout stage-2`, then `./test.sh`. All Stage 1 and Stage 2 rule tests still pass unchanged in `RulesSelfTest`.

New rule tests:
- **Combos:** Salad + Pasta = 2430 cents / 3 units, Salad + Burger = 2160 / 2. Menu order, components kept, combo-of-combo and null rejected, a combo needs all its units.
- **Happy Hour pricing:** VIP Burger = 1080, Regular Pasta = 1440, VIP Salad + Pasta Combo = 2430 -> 2187 -> 1750, Critic Burger = 1200. An order taken before activation keeps its price.
- **Happy Hour timing:** once per game, exactly two turns (one if activated on turn 18), not after staff have acted.
- **Recovery:** +15 capped at 100, skipping EATING / READY_TO_PAY / PAID / LEFT customers.
- **Host:** Host + Waiter seat two customers in one turn. The Host cannot seat without a free table and keeps its task. The Waiter can take the order of a customer the Host seated that turn. The Host cannot act after the Waiter.

### Run 6: Stage 3 victory

- **Command:** `./run.sh --seed 2002 < runs/stage3-victory.txt`
- **Seed:** 2002
- **Log:** [`runs/logs/stage3-victory.log`](../runs/logs/stage3-victory.log)

| Turn | HH | Host | Waiter | Chef | Key events |
|---|---|---|---|---|---|
| 1 | n | Seat C1 at T1 | Take C1's order | Advance O1 | Burger $15.00; O1 READY |
| 2 | n | Wait | Serve O1 | Wait | O1 SERVED |
| 3 | n | Wait | Wait | Wait | C1 READY_TO_PAY |
| 4 | n | Seat C2 at T2 | Take C2's order | Advance O2 (1/2) | C2 (VIP): Salad + Burger Combo $21.60 -> **VIP $19.44**; C1 PAID $15.00 (sat 92) |
| 5 | n | Wait | Wait | Advance O2 (2/2) | O2 READY |
| 6 | n | Wait | Serve O2 | Wait | O2 SERVED |
| 7 | n | Seat C3 at T1 | Take C3's order | Advance O3 (1/3) | Salad + Pasta Combo $24.30 (3 units) |
| 8 | n | Wait | Wait | Advance O3 (2/3) | C2 PAID $19.44 (sat 90) |
| 9 | **y** | Wait | Wait | Advance O3 (3/3) | **Happy Hour on**; C3 recovers 84 -> 99; O3 READY |
| 10 | - | Seat C4 at T2 | Serve O3 | Wait | HH 2nd turn: C3 91 -> 100; C4 (Critic) arrives and is seated at once |
| 11 | - | Wait | Take C4's order | Advance O4 (1/2) | Pasta $18.00 |
| 12 | - | Wait | Wait | Advance O4 (2/2) | C3 PAID $24.30 (sat 100) |
| 13 | - | Wait | Serve O4 | Wait | **Critic IMPRESSED** (2 turns): 64 -> 74 |
| 14 | - | Seat C5 at T1 | Take C5's order | Advance O5 (1/2) | Salad + Burger Combo $21.60 |
| 15 | - | Wait | Wait | Advance O5 (2/2) | C4 PAID $18.00 (sat 74) |
| 16 | - | Wait | Serve O5 | Wait | C6 arrives |
| 17 | - | Seat C6 at T2 | Take C6's order | Advance O6 | Burger $15.00 |
| 18 | - | Wait | Serve O6 | Wait | **C5 PAID $21.60** (sat 76) |

```
Served: 6 | Unhappy departures: 0 | Paid: 5 | Revenue: $98.34 | Avg satisfaction (paid): 86.4 | Turns used: 18/18

================================================================
 FINAL RESULT after 18 turns
================================================================
Paid customers:       5 (need >= 4) MET
Revenue:              $98.34 (need >= $45.00) MET
Average satisfaction: 86.4 (need >= 60) MET
RESULT: VICTORY
```

**Why it wins.** The Host seats each arrival on its arrival turn, so the Waiter can take the order that same turn and the Chef can start at once. This saves one waiting turn per customer compared with Stage 2, and C1 records 92 instead of 84. Happy Hour is kept for the slow 3-unit combo: C3 is still waiting for food on turns 9 and 10, recovers to 100, and pays with 100. No order is taken during Happy Hour, so no revenue is lost to the discount. The Critic is served exactly 2 turns after ordering and is impressed. Five customers pay $98.34 with an average of 86.4.

### Run 7: Stage 3 defeat

- **Command:** `./run.sh --seed 2002 < runs/stage3-defeat.txt`
- **Log:** [`runs/logs/stage3-defeat.log`](../runs/logs/stage3-defeat.log)

| Turn | HH | Host | Waiter | Chef | Key events |
|---|---|---|---|---|---|
| 1 | n | Wait | Seat C1 at T1 | Wait | |
| 2 | n | Wait | Take C1's order | Wait (idle) | Burger $15.00 |
| 3 | n | Wait | Wait | Advance O1 | O1 READY |
| 4 | n | Wait (C2 left in the queue) | Serve O1 | Wait | C2 (VIP) arrives |
| 5 | n | Wait | Seat C2 at T2 | Wait | C1 READY_TO_PAY |
| 6 | n | Wait | Take C2's order | Advance O2 (1/2) | Combo, VIP $19.44; C1 PAID $15.00 (sat 76) |
| 7 | n | Wait | Serve O2 (**rejected**: not READY, units 1/2), then Wait | Advance O2 (2/2) | C3 arrives |
| 8 | n | Wait | Serve O2 | Wait | |
| 9 | n | Wait | Seat C3 at T1 | Wait | |
| 10 | n | Wait | Take C3's order | Advance O3 (1/3) | Salad + Pasta Combo $24.30; C4 (Critic) arrives; C2 PAID $19.44 (sat 80) |
| 11 | n | Wait | Seat C4 at T2 | Advance O3 (2/3) | |
| 12 | n | Wait | Take C4's order | Advance O3 (3/3) | Pasta $18.00 |
| 13 | n | Wait | Serve O3 | Advance O4 (1/2) | C5 arrives (never seated) |
| 14 | n | Wait | Wait | Advance O4 (2/2) | |
| 15 | n | Wait | Serve O4 | Wait | **Critic UNIMPRESSED** (3 turns): 40 -> 30; C3 PAID $24.30 (sat 52) |
| 16 to 18 | (input ended: No / Wait) | | | | C4 PAID $18.00 on 17 (sat 30) |

```
Served: 4 | Unhappy departures: 0 | Paid: 4 | Revenue: $76.74 | Avg satisfaction (paid): 59.5 | Turns used: 18/18

================================================================
 FINAL RESULT after 18 turns
================================================================
Paid customers:       4 (need >= 4) MET
Revenue:              $76.74 (need >= $45.00) MET
Average satisfaction: 59.5 (need >= 60) NOT MET
RESULT: DEFEAT
```

**Why it loses.** The same customers order the same dishes as in Run 6, but this manager never uses the Host or Happy Hour and lets the Chef idle on turn 2. Every arrival waits at least a turn for a seat, and the Waiter must spend separate turns seating, ordering and serving. C3 waits six turns for the 3-unit combo (recorded 52), and the Critic is served late and is unimpressed (30). Revenue is comfortably above target, but the average is 59.5, just below 60. A single Happy Hour recovery for C3 or the Critic would have been enough to win.

### Run 8: Stage 3 features demo

- **Command:** `./run.sh --seed 2002 < runs/stage3-features-demo.txt`
- **Log:** [`runs/logs/stage3-features-demo.log`](../runs/logs/stage3-features-demo.log)
- **Outcome:** VICTORY (4 paid, $72.85, average 82.8), recorded in `runs/expected.txt`.

| Turn | What it shows |
|---|---|
| 1 | **Host** seats C1 |
| 2 | C1's Burger ordered **before** Happy Hour: locked at $15.00 |
| 4 | **Happy Hour activated**: recovery C1 76 -> 91 (C2 capped at 100). Host seats VIP C2, and the Waiter takes C2's order the same turn: **Salad + Burger Combo $21.60 -> VIP $19.44 -> Happy Hour $15.55**. O1 keeps $15.00 |
| 5 | Happy Hour 2nd turn: recovery at turn start, C1 83 -> 98, C2 95 -> 100. Display: `ACTIVE (1 turn(s) remaining incl. this one)` |
| 6 | Display: `Happy Hour: used` |
| 7 | **Invalid Host choice**: seat C3 at T1, but no table is free. Explained, re-asked, Host waits (task not lost) |
| 8 | Host seats C3; Waiter takes a **3-unit Salad + Pasta Combo** ($24.30) |
| 10 to 12 | Host seats the Critic; the Chef switches to the Critic's Pasta; **Critic IMPRESSED** on turn 12 (76 -> 86) |
| 13 to 15 | C3's combo served; C5 waits for a free table and is seated by the Host on 15 |
| 16 to 18 | Input ends: remaining prompts default to No / Wait |
