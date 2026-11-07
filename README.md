# Bytes N Bites

A turn-based restaurant management game: you are the manager of a tiny two-table diner. Over 18 turns, tell your Waiter and Chef (and, from Stage 3, your Host) what to do, keep the customers happy, and get them to pay.

This is **Stage 4**, the final game of our SC2002 Restaurant Rush project. Repository: https://github.com/annikaag-07/SC2002--Project--Bytes-N-Bites-

## Play it

You only need **Java (JDK 17 or newer)**. Check with `javac -version`. If it's missing, install a free JDK such as https://adoptium.net.

- **macOS / Linux:** open a terminal in this folder and run `./play.sh`
- **Windows:** double-click `play.bat`

The game opens at **http://localhost:8090**. If the browser doesn't open by itself, open that address yourself. To stop the game, press Ctrl+C in the terminal (or close the window on Windows).

## How to win

After 18 turns you need **at least 4 paying customers**, **$45 revenue**, and an **average satisfaction of 60** among the customers who paid. The **Guide** button on the main menu explains everything, and the game shows a tip on the menu.

## Modes

**Start new game** offers four modes. Each one includes everything in the one before, following the SC2002 assignment's stages:

| Mode | What you get |
|---|---|
| **Stage 1** | Regular customers, the three-dish menu (Salad, Burger, Pasta), Waiter, Chef and Cashier. The base game. |
| **Stage 2** | Stage 1 plus VIP and Critic customers. |
| **Stage 3** | Stage 2 plus the Host, combo meals and Happy Hour. |
| **Custom mode** | Stage 3 plus an Impatient guest on turn 10, Picky, Chatty and Grumpy regulars, and Happy Hour tips. |

From Stage 2, who arrives on each turn (1, 4, 7, 10, 13, 16) is random but always includes at least one VIP, at least one Critic and at least two Regulars.

**Eating and paying** follow the assignment brief: a customer served on turn s eats through turn s+1, becomes ready to pay at its end, and is charged by the Cashier at the **end** of turn s+2. Their table stays taken until then.

## Features

- Three restaurant themes: Neon Nights, Cozy Café and Tokyo Diner. Pick one with **Restaurant themes** on the main menu.
- Animated staff and customers. Use the **Moves** button to switch between 1x, 2x and off.
- Custom mode only: special regulars marked by a coloured outline, Picky (green), Chatty (blue, on their phone) and Grumpy (orange), the Impatient guest, and Happy Hour tips at the cashier. See the **Guide**.

## What's inside

| Folder | What |
|---|---|
| `restaurant-rush-app/` | The web game: a small Java web server (`server/`) and the browser front end (`client/`, already built in `client/dist/`) |
| `restaurant-rush/` | The original Java game (SC2002 project) whose rules the web game uses. It also runs on its own in the terminal: `cd restaurant-rush && ./build.sh && ./run.sh` |

To change the front end, you also need Node.js 20+: run `cd restaurant-rush-app && ./build.sh`, then `./start.sh`. Developer details are in `restaurant-rush-app/README.md`.
