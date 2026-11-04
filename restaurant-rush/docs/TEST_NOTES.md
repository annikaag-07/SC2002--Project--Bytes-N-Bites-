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
