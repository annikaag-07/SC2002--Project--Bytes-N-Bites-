# Bytes N Bites (Stage 2)

A turn-based restaurant management game: you are the manager of a tiny two-table diner. Over 18 turns, tell your Waiter and Chef what to do, keep the customers happy, and get them to pay.

This is **Stage 2** of our SC2002 Restaurant Rush project. Repository: https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-

## Play it

You only need **Java (JDK 17 or newer)**. Check with `javac -version`. If it's missing, install a free JDK such as https://adoptium.net.

- **macOS / Linux:** open a terminal in this folder and run `./play.sh`
- **Windows:** double-click `play.bat`

The game opens at **http://localhost:8090**. If the browser doesn't open by itself, open that address yourself. To stop the game, press Ctrl+C in the terminal (or close the window on Windows).

## How to win

After 18 turns you need **at least 4 paying customers**, **$45 revenue**, and an **average satisfaction of 60** among the customers who paid. The **Guide** button on the main menu explains everything, and the game shows a tip on the menu.

## Mode

**Start new game** offers one mode:

| Mode | What you get |
|---|---|
| **Stage 2** | Regular, VIP and Critic customers, the three-dish menu (Salad, Burger, Pasta), Waiter, Chef and Cashier. |

Six customers arrive, one at the start of turns 1, 4, 7, 10, 13 and 16. Who arrives is random but always includes at least one VIP, at least one Critic and at least two Regulars. VIPs lose patience slowly and get 10% off; Critics lose it fast and react to how quickly they are served.

**Eating and paying** follow the assignment brief: a customer served on turn s eats through turn s+1, becomes ready to pay at its end, and is charged by the Cashier at the **end** of turn s+2. Their table stays taken until then.

## Features

- Three restaurant themes: Neon Nights, Cozy Café and Tokyo Diner. Pick one with **Restaurant themes** on the main menu.
- Animated staff and customers. Use the **Moves** button to switch between 1x, 2x and off.
- Penguin skin, chiptune sound effects (with mute), keyboard shortcuts, Continue, high scores, a Day counter and a star rating.
- VIP (dark suit, gold tie) and Critic (beret, notepad) customers with their own looks. See the **Guide**.

## What's inside

| Folder | What |
|---|---|
| `restaurant-rush-app/` | The web game: a small Java web server (`server/`) and the browser front end (`client/`, already built in `client/dist/`) |
| `restaurant-rush/` | The Java game (SC2002 project) whose rules the web game uses. It also runs on its own in the terminal: `cd restaurant-rush && ./build.sh && ./run.sh` |

To change the front end, you also need Node.js 20+: run `cd restaurant-rush-app && ./build.sh`, then `./start.sh`. Developer details are in `restaurant-rush-app/README.md`.
