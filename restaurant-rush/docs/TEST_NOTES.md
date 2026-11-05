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
