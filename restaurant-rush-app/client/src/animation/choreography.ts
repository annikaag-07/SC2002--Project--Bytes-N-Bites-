/**
 * Movement planner. Turns one back-end result (state before, state after, the chosen action, and the
 * events the back end reported) into a short sequence of walking segments between fixed waypoints.
 *
 * The back end has already resolved everything; this module only decides how to *show* it. It never changes
 * game state, and the paths and timings do not depend on the theme, so the visuals cannot affect gameplay.
 *
 * Waypoints are named anchors in the restaurant view (data-anchor="..."), e.g. "host-home", "seat-T1",
 * "counter", "booth". See World.tsx for where each one sits.
 */
import type { Customer, GameAction, GameEvent, GameState } from "@/api/types";

export interface AnchorRef {
  name: string;
  dx?: number;
  dy?: number;
}

export type Role = "Host" | "Waiter" | "Chef";

export interface ActorSpec {
  id: string;
  kind: "staff" | "customer" | "dish";
  role?: Role;
  customer?: Customer;
  dish?: string[];
  start: AnchorRef;
  /** Not drawn until a segment lists it in `appear`. */
  startHidden?: boolean;
  /** Drawn at its start anchor from the beginning of the plan, even when its part plays later. */
  holdAtStart?: boolean;
  /** Waiter only: menus in hand at the start (0-2). */
  menus?: number;
}

export interface Move {
  actor: string;
  to: AnchorRef;
}

export interface Segment {
  /** Length at normal speed. */
  ms: number;
  moves: Move[];
  /** Applied when the segment starts. */
  appear?: string[];
  carry?: Record<string, string[] | null>;
  bubble?: Record<string, string | null>;
  cooking?: string[];
  float?: { at: AnchorRef; text: string; kind?: "tip" | "ugh" }[];
  /** Waiter's menus in hand from this segment on. */
  menus?: Record<string, number>;
  /** Applied when the segment ends. */
  reveal?: string[];
  vanish?: string[];
  /** Elements forced visible by the plan (`shown`) that stop showing when this segment ends. */
  unshow?: string[];
}

export interface Plan {
  id: number;
  actors: ActorSpec[];
  /** Scene elements hidden while the plan plays (their ghost stands in for them). Keys: see World.tsx. */
  hidden: string[];
  /** Scene elements kept visible although the new state no longer has them (e.g. a menu until it is picked up). */
  shown?: string[];
  segments: Segment[];
}

type Part = Omit<Plan, "id">;

const STAFF_KEY = (role: Role) => `staff-${role}`;

/** The waiter owns two menus. One lies on each table whose customer has been seated but has not ordered. */
const MENU_COUNT = 2;
export function menusOnTables(state: GameState): number {
  return state.customers.filter((c) => c.state === "SEATED").length;
}
const MENU_KEY = (tableId: string) => `menu-${tableId}`;

/** Total length of a plan at normal speed. */
export function planDuration(plan: Plan): number {
  return plan.segments.reduce((sum, s) => sum + s.ms, 0);
}

// ------------------------------------------------------------------ building blocks

function seatPart(leader: "Host" | "Waiter", customer: Customer, tableId: string, menusBefore: number): Part {
  const c = `walk-${customer.id}`;
  const l = `ghost-${leader}`;
  const home = leader === "Host" ? "host-home" : "waiter-home";
  const waiterSeats = leader === "Waiter";
  return {
    actors: [
      { id: c, kind: "customer", customer: { ...customer, state: "WAITING" }, start: { name: "queue-front" } },
      { id: l, kind: "staff", role: leader, start: { name: home }, menus: waiterSeats ? menusBefore : undefined },
    ],
    hidden: [`seat-${tableId}`, STAFF_KEY(leader), ...(waiterSeats ? [MENU_KEY(tableId)] : [])],
    segments: [
      // Customer steps in from the queue to meet whoever seats them.
      { ms: 650, moves: [{ actor: c, to: { name: "host-meet" } }, ...(waiterSeats ? [{ actor: l, to: { name: "host-meet", dx: 26 } }] : [])] },
      // Leader walks to the table with the customer following.
      { ms: 900, moves: [{ actor: l, to: { name: `stand-${tableId}` } }, { actor: c, to: { name: `seat-${tableId}` } }] },
      // Customer sits down (the real seated figure takes over with its sit-down animation).
      { ms: 250, moves: [], vanish: [c], reveal: [`seat-${tableId}`] },
      // A seating waiter puts one of their menus on the table.
      ...(waiterSeats
        ? [{ ms: 350, moves: [], cooking: [l], menus: { [l]: Math.max(0, menusBefore - 1) }, reveal: [MENU_KEY(tableId)] }]
        : []),
      // Leader walks back to their home spot.
      { ms: 850, moves: [{ actor: l, to: { name: home } }], vanish: [l], reveal: [STAFF_KEY(leader)] },
    ],
  };
}

/** After the Host seats someone, the waiter brings a menu to the table and walks back. */
function menuDropPart(tableId: string, menusBefore: number): Part {
  const w = "ghost-Waiter";
  return {
    actors: [{ id: w, kind: "staff", role: "Waiter", start: { name: "waiter-home" }, menus: menusBefore }],
    hidden: [STAFF_KEY("Waiter"), MENU_KEY(tableId)],
    segments: [
      { ms: 750, moves: [{ actor: w, to: { name: `stand-${tableId}` } }] },
      { ms: 350, moves: [], cooking: [w], menus: { [w]: Math.max(0, menusBefore - 1) }, reveal: [MENU_KEY(tableId)] },
      { ms: 750, moves: [{ actor: w, to: { name: "waiter-home" } }], vanish: [w], reveal: [STAFF_KEY("Waiter")] },
    ],
  };
}

function takeOrderPart(tableId: string, menusBefore: number): Part {
  const w = "ghost-Waiter";
  return {
    actors: [{ id: w, kind: "staff", role: "Waiter", start: { name: "waiter-home" }, menus: menusBefore }],
    hidden: [STAFF_KEY("Waiter"), `tag-${tableId}`],
    // the menu stays on the table until the waiter picks it up
    shown: [MENU_KEY(tableId)],
    segments: [
      { ms: 750, moves: [{ actor: w, to: { name: `stand-${tableId}` } }] },
      {
        ms: 450,
        moves: [],
        bubble: { [w]: "Order up!" },
        cooking: [w],
        menus: { [w]: Math.min(MENU_COUNT, menusBefore + 1) },
        unshow: [MENU_KEY(tableId)],
        reveal: [`tag-${tableId}`],
      },
      { ms: 750, moves: [{ actor: w, to: { name: "waiter-home" } }], bubble: { [w]: null }, vanish: [w], reveal: [STAFF_KEY("Waiter")] },
    ],
  };
}

/** A picky eater sends the dish straight back: the waiter carries it back to the counter. */
function sendBackPart(tableId: string, orderId: string, dish: string[], menus: number): Part {
  const w = "ghost-Waiter";
  const d = "dish-on-counter";
  return {
    actors: [
      { id: w, kind: "staff", role: "Waiter", start: { name: "waiter-home" }, menus },
      { id: d, kind: "dish", dish, start: { name: "counter" } },
    ],
    hidden: [STAFF_KEY("Waiter"), `ready-${orderId}`],
    segments: [
      { ms: 800, moves: [{ actor: w, to: { name: "counter" } }] },
      { ms: 200, moves: [], vanish: [d], carry: { [w]: dish } },
      { ms: 900, moves: [{ actor: w, to: { name: `stand-${tableId}` } }] },
      { ms: 600, moves: [], bubble: { [w]: "Sent back!" }, float: [{ at: { name: `seat-${tableId}`, dy: -64 }, text: "Ugh!", kind: "ugh" }] },
      { ms: 900, moves: [{ actor: w, to: { name: "counter" } }], bubble: { [w]: null } },
      { ms: 200, moves: [], carry: { [w]: null }, reveal: [`ready-${orderId}`] },
      { ms: 800, moves: [{ actor: w, to: { name: "waiter-home" } }], vanish: [w], reveal: [STAFF_KEY("Waiter")] },
    ],
  };
}

function servePart(tableId: string, dish: string[], menus: number): Part {
  const w = "ghost-Waiter";
  const d = "dish-on-counter";
  return {
    actors: [
      { id: w, kind: "staff", role: "Waiter", start: { name: "waiter-home" }, menus },
      { id: d, kind: "dish", dish, start: { name: "counter" } },
    ],
    hidden: [STAFF_KEY("Waiter"), `plate-${tableId}`, `tag-${tableId}`],
    segments: [
      { ms: 800, moves: [{ actor: w, to: { name: "counter" } }] },
      { ms: 200, moves: [], vanish: [d], carry: { [w]: dish } },
      { ms: 900, moves: [{ actor: w, to: { name: `stand-${tableId}` } }] },
      { ms: 200, moves: [], carry: { [w]: null }, reveal: [`plate-${tableId}`, `tag-${tableId}`] },
      { ms: 800, moves: [{ actor: w, to: { name: "waiter-home" } }], vanish: [w], reveal: [STAFF_KEY("Waiter")] },
    ],
  };
}

function cookPart(orderId: string, finished: boolean, dish: string[]): Part {
  const ch = "ghost-Chef";
  const cooking: Segment = { ms: 600, moves: [], cooking: [ch] };
  if (!finished) {
    return {
      actors: [{ id: ch, kind: "staff", role: "Chef", start: { name: "chef-home" } }],
      hidden: [STAFF_KEY("Chef")],
      segments: [{ ...cooking, vanish: [ch], reveal: [STAFF_KEY("Chef")] }],
    };
  }
  return {
    actors: [{ id: ch, kind: "staff", role: "Chef", start: { name: "chef-home" } }],
    hidden: [STAFF_KEY("Chef"), `ready-${orderId}`],
    segments: [
      cooking,
      { ms: 700, moves: [{ actor: ch, to: { name: "counter" } }], carry: { [ch]: dish } },
      { ms: 200, moves: [], carry: { [ch]: null }, reveal: [`ready-${orderId}`] },
      { ms: 700, moves: [{ actor: ch, to: { name: "chef-home" } }], vanish: [ch], reveal: [STAFF_KEY("Chef")] },
    ],
  };
}

/**
 * The back end's "Cashier collects ... table T# freed" event (step 4, end of the turn): the diner, who stayed
 * seated while READY_TO_PAY, stands up, walks to the cashier booth, pays (and tips in Custom mode), then walks
 * out of the door. The new state already shows the table free; the walker stands in for the diner.
 */
function payAndLeavePart(customer: Customer, tableId: string, amount: string, tip: string | null): Part {
  const c = `walk-${customer.id}`;
  return {
    // Held at the table from the start: the new state has already freed it, but the diner leaves only now.
    actors: [{ id: c, kind: "customer", customer, start: { name: `seat-${tableId}`, dy: 12 }, holdAtStart: true }],
    hidden: [],
    segments: [
      { ms: 250, moves: [{ actor: c, to: { name: `seat-${tableId}` } }] },
      { ms: 700, moves: [{ actor: c, to: { name: "aisle-right" } }] },
      { ms: 500, moves: [{ actor: c, to: { name: "booth" } }] },
      { ms: 550, moves: [], float: [{ at: { name: "booth", dy: -70 }, text: `+${amount}` }] },
      // Happy Hour tip (decided by the server): its own floating "+$X tip" with a sparkle.
      ...(tip ? [{ ms: 650, moves: [], float: [{ at: { name: "booth", dy: -92 }, text: `+${tip} tip`, kind: "tip" as const }] }] : []),
      { ms: 650, moves: [{ actor: c, to: { name: "aisle-right" } }] },
      { ms: 1000, moves: [{ actor: c, to: { name: "aisle-left" } }] },
      { ms: 500, moves: [{ actor: c, to: { name: "door-in" } }] },
      { ms: 400, moves: [{ actor: c, to: { name: "exit" } }], vanish: [c] },
    ],
  };
}

/** Walks out unhappy: from the queue straight down the street, or from a table out of the door. */
function leavePart(customer: Customer, tableId: string | null): Part {
  const c = `walk-${customer.id}`;
  if (!tableId) {
    return {
      actors: [{ id: c, kind: "customer", customer, start: { name: "queue-front" } }],
      hidden: [],
      segments: [{ ms: 900, moves: [{ actor: c, to: { name: "street-end" } }], bubble: { [c]: "Hmph!" }, vanish: [c] }],
    };
  }
  return {
    actors: [{ id: c, kind: "customer", customer, start: { name: `seat-${tableId}`, dy: 12 } }],
    hidden: [],
    segments: [
      { ms: 250, moves: [{ actor: c, to: { name: `seat-${tableId}` } }], bubble: { [c]: "Hmph!" } },
      { ms: 800, moves: [{ actor: c, to: { name: "aisle-left" } }] },
      { ms: 500, moves: [{ actor: c, to: { name: "door-in" } }] },
      { ms: 400, moves: [{ actor: c, to: { name: "exit" } }], bubble: { [c]: null }, vanish: [c] },
    ],
  };
}

// ------------------------------------------------------------------ combining parts

/** Plays parts one after another. Actors used by later parts start hidden until their first segment. */
function sequence(parts: Part[]): Part {
  const out: Part = { actors: [], hidden: [], segments: [] };
  parts.forEach((part, index) => {
    const ids = part.actors.map((a) => a.id);
    for (const actor of part.actors) {
      if (!out.actors.some((a) => a.id === actor.id)) {
        // Staff stand at home from the start (their home figure is hidden for the whole plan);
        // customers and dishes of later parts only appear when their part begins.
        out.actors.push(index === 0 || actor.kind === "staff" || actor.holdAtStart ? actor : { ...actor, startHidden: true });
      }
    }
    out.hidden.push(...part.hidden);
    if (part.shown) out.shown = [...(out.shown ?? []), ...part.shown];
    part.segments.forEach((segment, i) => {
      out.segments.push(i === 0 && index > 0 ? { ...segment, appear: [...(segment.appear ?? []), ...ids] } : segment);
    });
  });
  return out;
}

/** Plays parts at the same time (different actors), segment by segment. */
function together(parts: Part[]): Part {
  if (parts.length <= 1) return parts[0] ?? { actors: [], hidden: [], segments: [] };
  const length = Math.max(...parts.map((p) => p.segments.length));
  const segments: Segment[] = [];
  for (let i = 0; i < length; i++) {
    const merged: Segment = { ms: 0, moves: [] };
    for (const part of parts) {
      const s = part.segments[i];
      if (!s) continue;
      merged.ms = Math.max(merged.ms, s.ms);
      merged.moves.push(...s.moves);
      for (const key of ["appear", "cooking", "reveal", "vanish", "unshow"] as const) {
        if (s[key]) merged[key] = [...(merged[key] ?? []), ...s[key]!];
      }
      if (s.carry) merged.carry = { ...merged.carry, ...s.carry };
      if (s.bubble) merged.bubble = { ...merged.bubble, ...s.bubble };
      if (s.float) merged.float = [...(merged.float ?? []), ...s.float];
      if (s.menus) merged.menus = { ...merged.menus, ...s.menus };
    }
    segments.push(merged);
  }
  return {
    actors: parts.flatMap((p) => p.actors),
    hidden: parts.flatMap((p) => p.hidden),
    shown: parts.flatMap((p) => p.shown ?? []),
    segments,
  };
}

// ------------------------------------------------------------------ entry point

/** Builds the movement plan for one result, or null when there is nothing to animate. */
export function planAnimation(id: number, before: GameState, after: GameState, action: GameAction, events: GameEvent[]): Plan | null {
  const find = (s: GameState, cid: string) => s.customers.find((c) => c.id === cid);
  const parts: Part[] = [];
  // Menus the waiter holds before this action (one lies on each table with a seated, not-yet-ordered customer).
  const menusBefore = Math.max(0, MENU_COUNT - menusOnTables(before));

  // 1. The staff action itself.
  if ((action.type === "hostSeat" || action.type === "waiterSeat") && action.customer) {
    const c = find(after, action.customer);
    if (c?.table) {
      parts.push(seatPart(action.type === "hostSeat" ? "Host" : "Waiter", c, c.table, menusBefore));
      // After the Host seats someone the waiter brings a menu (queued after the Host's walk, never overlapping).
      if (action.type === "hostSeat" && menusBefore > 0) parts.push(menuDropPart(c.table, menusBefore));
    }
  }
  if (action.type === "waiterTakeOrder" && action.customer) {
    const c = find(after, action.customer);
    if (c?.table) parts.push(takeOrderPart(c.table, menusBefore));
  }
  if (action.type === "waiterServe" && action.order) {
    const order = after.orders.find((o) => o.id === action.order);
    const c = order && find(after, order.customerId);
    const sentBack = events.some((e) => /sends the .* back/.test(e.text));
    if (order && c?.table) {
      parts.push(sentBack ? sendBackPart(c.table, order.id, order.components, menusBefore) : servePart(c.table, order.components, menusBefore));
    }
  }
  if (action.type === "chefCook" && action.order) {
    const order = after.orders.find((o) => o.id === action.order);
    if (order) parts.push(cookPart(order.id, order.status === "READY", order.components));
  }

  // 2. End-of-turn results reported by the back end (after the Chef), in the engine's step order: the Cashier
  //    charges one diner (who walks from their table to the booth, pays and leaves), then unhappy walk-outs.
  //    "finishes eating" needs no movement: that diner stays seated with a "Ready to pay" tag.
  const endOfTurn: Part[] = [];
  for (const e of events) {
    const paid = e.text.match(/^Cashier collects (\$[\d.]+) from (C\d+).*table (T\d+) freed/);
    if (paid) {
      const c = find(after, paid[2]) ?? find(before, paid[2]);
      const tip = after.tipList?.find((t) => t.customerId === paid[2] && t.turn === after.turn);
      if (c) endOfTurn.push(payAndLeavePart(c, paid[3], paid[1], tip ? tip.amount : null));
    }
    const left = e.text.match(/^(C\d+) \(\w+\) leaves unhappy/);
    if (left) {
      const was = find(before, left[1]);
      const now = find(after, left[1]);
      if (was && now) endOfTurn.push(leavePart({ ...now, satisfaction: 0 }, was.state === "WAITING" ? null : was.table));
    }
  }
  if (endOfTurn.length > 0) parts.push(together(endOfTurn));

  if (parts.length === 0) return null;
  return { id, ...sequence(parts) };
}
