/**
 * Shapes of the JSON returned by the Java API server (server/src/rush/web/StateJson.java).
 * The front end only reads these; every rule is decided by the back end.
 */

export type CustomerType = "Regular" | "VIP" | "Critic";
export type CustomerState = "WAITING" | "SEATED" | "ORDERED" | "EATING" | "READY_TO_PAY" | "PAID" | "LEFT";
export type OrderStatus = "PENDING" | "PREPARING" | "READY" | "SERVED" | "PAID" | "CANCELLED";
export type Phase = "WAITER" | "CHEF" | "SUMMARY" | "OVER";
export type Role = "Waiter" | "Chef";

export interface Customer {
  id: string;
  type: CustomerType;
  satisfaction: number;
  /** Satisfaction recorded at payment, or -1. */
  recordedSatisfaction: number;
  state: CustomerState;
  table: string | null;
  orderId: string | null;
  arrivalTurn: number;
}

export interface Order {
  id: string;
  customerId: string;
  item: string;
  status: OrderStatus;
  unitsDone: number;
  prepUnits: number;
  priceCents: number;
  price: string;
  takenTurn: number;
  readyTurn: number;
  /** READY since an earlier turn, so the Waiter may serve it now. */
  servableNow: boolean;
}

export interface Table {
  id: string;
  occupant: string | null;
}

/** A command the back end understands (see GameSession.perform). */
export interface GameAction {
  type: string;
  customer?: string;
  table?: string;
  order?: string;
}

/** One answer to a follow-up question, e.g. "Table 1". Disabled answers carry the reason. */
export interface StepOption {
  /** Value sent to the back end for this step's parameter (e.g. a customer or order id). */
  value: string;
  label: string;
  detail: string | null;
  enabled: boolean;
  reason: string | null;
}

/** A follow-up question such as "Which table should I serve?". */
export interface ChoiceStep {
  param: "customer" | "table" | "order";
  prompt: string;
  options: StepOption[];
}

/** One MCQ option. Disabled options carry the back end's reason. */
export interface Choice {
  key: string;
  label: string;
  enabled: boolean;
  reason: string | null;
  /** Short description shown under an enabled option, e.g. "T1: Burger is ready." */
  hint: string | null;
  /** Command sent once every step has been answered. */
  actionType: string;
  /** Questions asked in order after picking this option (none for e.g. Wait). */
  steps: ChoiceStep[];
}

export interface Decision {
  role: Role;
  choices: Choice[];
}

export interface GameEvent {
  turn: number;
  text: string;
}

export interface GameResult {
  victory: boolean;
  /** One star per victory target met (3 = victory). */
  stars: number;
  paidCount: number;
  paidMet: boolean;
  revenueCents: number;
  revenue: string;
  revenueMet: boolean;
  averageSatisfaction: string;
  satisfactionMet: boolean;
}

export interface MenuEntry {
  name: string;
  priceCents: number;
  price: string;
  prepUnits: number;
}

export interface GameState {
  /** Kept for debugging/replays; players see the day number instead. */
  seed: string;
  /** Which restaurant day this game is (1, 2, 3...). */
  day: number;
  mode: GameMode;
  /** "Stage 2". */
  modeLabel: string;
  menu: MenuEntry[];
  turn: number;
  totalTurns: number;
  phase: Phase;
  revenueCents: number;
  revenue: string;
  averageSatisfaction: string;
  paidCount: number;
  servedCount: number;
  departureCount: number;
  targets: { paid: number; revenueCents: number; revenue: string; averageSatisfaction: number };
  queue: string[];
  tables: Table[];
  customers: Customer[];
  orders: Order[];
  nextArrival: { turn: number; type: CustomerType } | null;
  cashierNext: string | null;
  /** Events of the turn that just closed (shown on the summary). */
  turnSummary: GameEvent[];
  /** What happened as this turn started: arrivals. (Payment is at the end of a turn.) */
  turnStartEvents: GameEvent[];
  /** Every event so far, oldest first (for the Log). */
  log: GameEvent[];
  decision: Decision | null;
  result: GameResult | null;
}

export interface ActionResponse {
  result: { success: boolean; message: string };
  events: GameEvent[];
  state: GameState;
}

/** The playable mode (see ModeFeatures on the server). */
export type GameMode = "stage2";

export interface ScoreEntry {
  date: string;
  day: number;
  mode: string;
  /** Display name, e.g. "Stage 2". */
  modeLabel: string;
  victory: boolean;
  stars: number;
  paid: number;
  revenueCents: number;
  average: string;
}
