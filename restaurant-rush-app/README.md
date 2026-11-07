# Bytes N Bites: Web Front End

**Bytes N Bites** is the browser version of the SC2002 Restaurant Rush Java game, with a synthwave pixel-art look and chiptune sound effects. The game rules are **not rewritten**: a small Java HTTP server calls the existing back end (`../restaurant-rush/src/main/java`, compiled straight from that folder), and a React page draws the restaurant and asks for each decision.

## Run it

Requirements: JDK 17+ and Node.js 20+. The Java project must sit next to this folder as `../restaurant-rush`; to use another location, set `RUSH_BACKEND=/path/to/restaurant-rush`.

```bash
./build.sh
```

```bash
./start.sh
```

Open **http://localhost:8090**. For development with hot reload (API on 8090, Vite on 5173):

```bash
./dev.sh
```

Self-tests: `AdapterSelfTest` replays the CLI's Stage 3 scripts through the web adapter in `classic` mode and compares every event line with the CLI logs, and checks save/replay and the Custom-mode extras. `ModeSelfTest` checks the four modes' features, the random arrival schedule over 5,000 seeds per mode, the payment timing (including the brief's section 3.3 example row by row), replays the CLI's Stage 1 and Stage 2 scripts through `stage1` / `stage2` mode, and plays 200 seeds per mode with a bot:

```bash
./test.sh
```

## Architecture

```
browser (React)                              Java (JDK only)
-----------------------------------------    ----------------------------------------------------
(c) screens + components  -- render only     WebServer   HTTP + static files   (rush.web)
(b) store/gameStore.ts    -- state, calls    GameSession adapter: whose turn, forwards commands,
(a) api/client.ts         -- fetch /api/* -->            action log for save/continue
                                             StateJson   snapshot + valid choices -> JSON
                                             ----------------------------------------------------
                                             GameController, entities  (UNCHANGED back end)
```

- **(a) Adapter/API**: `client/src/api/` (types and fetch calls) and `server/src/rush/web/`. `GameSession` plays the same role as the CLI's `ConsoleUI`. It decides whose turn it is (Happy Hour → Host → Waiter → Chef, with Happy Hour and the Host only in modes that have them, then the end-of-turn summary) and forwards each choice to `GameController`.
- **(b) Store**: `client/src/store/gameStore.ts` holds the latest server snapshot plus UI-only state (screen, skin, mute), runs the API calls and fires sound hooks.
- **(c) UI**: `client/src/screens/` and `client/src/components/`. Components only render state and send the chosen action.

### Modes

The four modes are cumulative, like the assignment's stages. `server/src/rush/web/ModeFeatures.java` is the one place that says what each mode has; the state JSON passes the same flags (`features`) to the client, which draws only what the mode has (Host and pedestal, Happy Hour chip, Tips chip).

| Mode (`mode=`) | VIP + Critic | Host | Combos | Happy Hour | Tips | Picky / Chatty / Grumpy | Impatient (turn 10) |
|---|---|---|---|---|---|---|---|
| Stage 1 (`stage1`) | | | | | | | |
| Stage 2 (`stage2`) | yes | | | | | | |
| Stage 3 (`stage3`) | yes | yes | yes | yes | | | |
| Custom (`custom`) | yes | yes | yes | yes | yes | yes | yes |
| `classic` (API only) | fixed list | yes | yes | yes | | | |

- Stage 1 and 2 have no Host and no Happy Hour: those phases are skipped, and the server rejects `hostSeat`, `hostWait`, `happyHourYes` and `happyHourNo` with a reason. The Waiter seats guests. Their menu is Salad, Burger and Pasta (the first three items of the back end's standard menu).
- In Stage 1 to 3 every Regular is a plain `RegularCustomer`: no sub-types and no tips.
- `classic` is Stage 3 with the terminal game's fixed arrivals (Regular, VIP, Regular, Critic, Regular, Regular). It is used by the parity test against the CLI logs and is not shown in the UI.
- An unknown mode (including the old `base` and `rush`) is an HTTP 400 error. A saved game from an older version is discarded and treated as "no save". Old high-score entries still show, labelled "Base" or "Rush".

### Arrivals (Stage 2, Stage 3, Custom)

Arrival turns never change: 1, 4, 7, 10, 13 and 16. `server/src/rush/web/ArrivalPlan.java` picks each slot as Regular 50 / VIP 25 / Critic 25 (named constants), then repairs the result so every game has at least 1 VIP, at least 1 Critic and at least 2 Regulars. A VIP or Critic can land on any slot, including turns 1 and 16, and can appear more than once. In Custom mode, slot 4 (turn 10, customer C4) is always the Impatient guest. Stage 1 is six Regulars. The schedule is a pure function of (seed, mode) with its own seeded generator, so Continue rebuilds the same customers and the customers' food choices are unaffected.

### Eating and payment

All modes use the back end's default timing, the same as the terminal game and the brief (section 3.2, step 4): a customer served on turn s finishes eating at the end of turn s+1 (READY_TO_PAY), and the Cashier charges them in step 4 at the **end** of turn s+2 at the earliest, one customer per turn, oldest first. The table stays occupied during turn s+2's decisions and can be reused from turn s+3. On screen the diner stays seated with a "Ready to pay" tag; when the back end reports "Cashier collects ... table T# freed" at the end of the turn, they stand up, walk to the cashier booth, pay (a floating +$X, plus the tip in Custom mode) and walk out. Paid and Revenue in the HUD change at that moment. Nothing on the client counts turns: it only reacts to the back end's events and state.

All rules stay in the Java back end. The list of options for each role (and the reason a disabled option is disabled) is built on the server from the back end's own queries: free tables, seated customers, servable and preparable orders.

Options that need a target come with **follow-up questions** (`steps`), and the UI asks them one at a time without ever picking for the player:

- **Seat:** "Who should I seat?", then always "Which table should I seat them at?". The first question is skipped when only one person is waiting.
- **Take order:** "Which table should I take the order from?"
- **Serve:** "Which table should I serve?"
- **Cook (Chef):** "Which order should I cook?"

The table questions list both tables. A table where the job makes no sense is shown disabled with the reason, e.g. "Empty table", "Taken by C1 (Regular)", or "C2 (VIP) is waiting for Pasta (1/2 cooked)".

### API

| Method | Path | Returns |
|---|---|---|
| GET | `/api/status` | `{ hasSave, hasGame }` |
| POST | `/api/new?mode=stage1\|stage2\|stage3\|custom\|classic` | game state (unknown or missing mode: 400) |
| POST | `/api/continue` | game state (replays the save) |
| GET | `/api/state` | game state |
| POST | `/api/action?type=&customer=&table=&order=` | `{ result, events, state }` |
| POST | `/api/next` | `{ result, events, state }`; leaves the end-of-turn summary |
| GET | `/api/highscores` | top 10 scores |

Action types: `happyHourYes`, `happyHourNo`, `hostSeat`, `hostWait`, `waiterSeat`, `waiterTakeOrder`, `waiterServe`, `waiterWait`, `chefCook`, `chefWait`.

### Saving and high scores

- **Continue:** after every successful action the server writes `data/save.txt` (seed, mode and day, plus the list of actions). The back end is deterministic, so replaying that list rebuilds the exact game, and no back-end class needed to be serialisable. The save is deleted when a game ends.
- **High scores:** stored in `data/highscores.tsv`, top 10, victories first.
- **Day number:** each new game is the restaurant's next **Day** (counter in `data/day.txt`). Players see the day number and a **star rating** (one star per victory target met; 3 stars = victory) instead of the random seed. The seed is still stored in the save and the score file for replaying.

## Customising

- **Colours and look**: everything is a CSS variable in `client/src/theme/theme.css`. That includes shirts per customer type, staff uniforms, table ice, walls, emotion colours and fonts.
- **Who wears what**: `STAFF_LOOKS` and `CUSTOMER_LOOKS` in `client/src/components/pixel/sprites.ts`.
- **Pixel art**: the sprites are character grids in `client/src/components/pixel/sprites.ts`. Their colours come from `theme.css`, and each staff member's or customer type's outfit is in `STAFF_LOOKS` / `CUSTOMER_LOOKS`.
- **Penguin skin**: the Penguins toggle in the menu or HUD swaps every figure for a penguin wearing the same colours.
- **Sounds**: every effect is synthesised in the browser (Web Audio chiptune) in `client/src/sound/soundManager.ts`, so no audio files are needed. The effects are click, select, back, seat, order, cook, serve, pay, arrive, anger, leave, happy hour, next turn, error, victory and game over. To use a recorded sound instead, put it in `client/public/sounds/` and add it to `CUSTOM_FILES`.
- **Keyboard**: A-D pick an option, 1-9 answer a "which table?" question, Esc goes back.
- **Restaurant themes**: Neon Nights (default), Cozy Café and Tokyo Diner, chosen from **Restaurant themes** on the main menu (with live preview). The colours of each theme are in `theme.css` under `[data-theme="..."]`. Prop patterns (tables, chairs, lamps, door, pedestal, cashier booth, decorations) are in `client/src/components/pixel/themeSprites.ts`. Every prop has the same size in every theme, so positions and movement are identical and a theme can never affect gameplay.
- **Character movement**: `client/src/animation/choreography.ts` turns each back-end result (state before/after, the action, and its events) into short walking segments between fixed waypoints (`data-anchor` in `World.tsx`). `useChoreography.ts` plays them. The back end has already resolved the action, so the movement only replays it. Input is locked until it finishes, with a safety timeout so the game can never get stuck. The **Moves** button switches between 1x, 2x and off.
- **Regular sub-types** (Custom mode only, `server/src/rush/web/CustomerSubtypes.java`): once per game, the Regular customers are rolled as Normal, **Picky** (green outline, sends the food back once and it returns to the counter), **Chatty** (blue outline and a phone, eats one extra turn) or **Grumpy** (orange outline, starts at 80-89, Happy Hour recovery doubled). The odds (0/1/2/3/4 specialised: 10/60/20/7/3%) and all numbers are constants in that file.
- **Happy Hour tips** (Custom mode only): a customer whose bill got the Happy Hour discount and whose satisfaction at payment is above 80 tips 0-20% of the discounted bill, in whole cents. A tip is left when that customer pays (end of the turn) and is shown in the HUD, at the booth, the log, the turn summary and the result screen. They do **not** count towards the $45 revenue target (`TIPS_COUNT_TOWARDS_REVENUE_TARGET = false`).
- **Waiter menus**: the waiter carries two "MENUS" cards. One is placed on a table when a guest is seated and picked up when the order is taken. This is shown from the state only.
- **Paying flow (visual)**: see "Eating and payment" above. Revenue, satisfaction and table availability come only from the back end.
- **Tips**: `client/src/content/tips.ts`.

## Decisions to flag

0. **Modes.** New game offers **Stage 1**, **Stage 2**, **Stage 3** and **Custom mode** (see "Modes" above); picking one starts the game. `mode=classic` (API only) reproduces the CLI exactly and is used by `./test.sh`.
1. **Impatient customer** (Custom mode only). Added as `server/src/rush/web/ImpatientCustomer.java` without modifying the back end. It loses 10 per waiting turn and walks out once a turn would leave it at 30 or below. This uses the existing `satisfactionLossPerTurn()` hook, so the unchanged leave-at-0 rule does the rest; its satisfaction shows 0 when it storms out. It always arrives on turn 10 (customer C4).
2. **Host and Chef options** come from the back end: Host = seat / wait, Chef = cook one unit / wait.
3. **Happy Hour** (Stage 3 and Custom) has no staff owner in the back end, so it is asked by a **Manager** character at the start of a turn, as in the CLI.
4. **End of turn**: after the Chef chooses, the server closes the turn: payment, eating, waiting penalties and departures. The UI shows a summary, and **Start turn N ▶** begins the next turn.
7. **Payment timing.** Every mode uses the brief's timing (see "Eating and payment" above). The back end's opt-in `GameConfig.withChargeAtTurnStart` (start-of-turn payment, used by an earlier version of this web edition) is still in the back end but no mode uses it.
5. **Arrivals**: a guest arrives on turns 1, 4, 7, 10, 13 and 16 (every 3rd turn). Who arrives comes from `ArrivalPlan` on the server (see "Arrivals" above); the UI does not schedule anything.
6. Each new game uses a random seed internally; players see the Day number and stars instead.
