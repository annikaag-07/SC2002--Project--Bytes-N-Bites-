/**
 * Shapes of the JSON returned by the Java API server (server/src/rush/web/StateJson.java).
 * The front end only reads these; every rule is decided by the back end.
 */

export type CustomerType = "Regular" | "VIP" | "Critic" | "Impatient";
export type CustomerState = "WAITING" | "SEATED" | "ORDERED" | "EATING" | "READY_TO_PAY" | "PAID" | "LEFT";
export type OrderStatus = "PENDING" | "PREPARING" | "READY" | "SERVED" | "PAID" | "CANCELLED";
export type Phase = "HAPPY_HOUR" | "HOST" | "WAITER" | "CHEF" | "SUMMARY" | "OVER";
export type Role = "Manager" | "Host" | "Waiter" | "Chef";

/** Regular-customer sub-types (Custom mode only). null in other modes and for VIP, Critic and Impatient customers. */
export type CustomerSubType = "normal" | "picky" | "chatty" | "grumpy";

export interface Customer {
  id: string;
  type: CustomerType;
  subType: CustomerSubType | null;
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
  /** Base dishes to draw (a combo has two). */
  components: string[];
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

/** What the current mode has (server: ModeFeatures). The client draws only these. */
export interface ModeFeatures {
  vipAndCritic: boolean;
  /** The Host and their pedestal (Stage 3 and Custom). */
  host: boolean;
  combos: boolean;
  happyHour: boolean;
  /** Happy Hour tips (Custom only). */
  tips: boolean;
  subTypes: boolean;
  impatient: boolean;
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
  mode: GameMode | "classic";
  /** "Stage 1", "Stage 2", "Stage 3", "Custom" (or "Classic"). */
  modeLabel: string;
  features: ModeFeatures;
  menu: MenuEntry[];
  turn: number;
  totalTurns: number;
  phase: Phase;
  revenueCents: number;
  revenue: string;
  averageSatisfaction: string;
  paidCount: number;
  /** Happy Hour tips (Custom only, else 0). Shown separately: they are not part of revenue (the $45 target). */
  tipsCents: number;
  tips: string;
  totalEarnedCents: number;
  totalEarned: string;
  tipsCountTowardsTarget: boolean;
  tipList: { turn: number; customerId: string; cents: number; amount: string }[];
  servedCount: number;
  departureCount: number;
  targets: { paid: number; revenueCents: number; revenue: string; averageSatisfaction: number };
  happyHour: { available: boolean; remainingTurns: number };
  queue: string[];
  tables: Table[];
  customers: Customer[];
  orders: Order[];
  nextArrival: { turn: number; type: CustomerType } | null;
  cashierNext: string | null;
  /** Events of the turn that just closed (shown on the summary). */
  turnSummary: GameEvent[];
  /** What happened as this turn started: arrivals and Happy Hour recovery. (Payment is at the end of a turn.) */
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

/** The four playable modes, cumulative like the assignment's stages (see ModeFeatures on the server). */
export type GameMode = "stage1" | "stage2" | "stage3" | "custom";

export interface ScoreEntry {
  date: string;
  day: number;
  /** May also be an older version's mode ("base", "rush"). */
  mode: string;
  /** Display name, e.g. "Stage 2" (older entries: "Base", "Rush"). */
  modeLabel: string;
  victory: boolean;
  stars: number;
  paid: number;
  revenueCents: number;
  average: string;
}
