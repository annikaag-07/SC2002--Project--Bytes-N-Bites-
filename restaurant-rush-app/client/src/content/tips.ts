/**
 * Rotating tips shown on the loading screen and main menu. They rotate before a mode is chosen, so each tip is
 * either true in every mode or starts with the mode it belongs to ("From Stage 2", "From Stage 3", "Custom mode
 * only"). Add more freely, following that rule.
 */
export const TIPS: string[] = [
  "A dish finished this turn can only be served next turn. Plan one turn ahead!",
  "A served guest eats for one full turn, then is ready to pay. The Cashier charges them at the end of the turn after that.",
  "A guest who is ready to pay still holds their table until the Cashier charges them at the end of the turn.",
  "The Cashier charges one guest per turn, oldest first. If two are ready at once, one waits a turn.",
  "Every guest needs the Waiter three times: seat, take the order, serve. Don't waste Waiter turns!",
  "Satisfaction below 60? Watch for the anger marks above their head!",
  "You need 4 paying customers, $45 and an average satisfaction of 60 to win.",
  "The guest arriving on turn 16 can never pay in time. Focus on the others!",
  "Waiter tip: choose the job first, then pick the table. Keys A-D and 1-2 work too!",
  "From Stage 2: VIPs are patient (only -5 a turn) and get 10% off their bill.",
  "From Stage 2: Critics hate waiting. Serve them within 2 turns of ordering and they leave a glowing review.",
  "From Stage 2: every game has at least one VIP and one Critic, but who arrives when changes from game to game.",
  "From Stage 3: the Host seats before the Waiter, so the Waiter can take that guest's order in the same turn.",
  "From Stage 3: Happy Hour works once per game: +15 for everyone still waiting, and 20% off orders taken.",
  "From Stage 3: combos take longer to cook. Salad + Pasta needs 3 Chef turns.",
  "Custom mode only: the Impatient guest on turn 10 taps their feet and walks out once satisfaction would drop to 30.",
  "Custom mode only: during Happy Hour, happy customers may leave a tip at the cashier.",
  "Custom mode only: green outlines are picky, blue ones chat on their phone, orange ones walk in grumpy.",
];
