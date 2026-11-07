import type { Customer, CustomerSubType, CustomerType } from "@/api/types";
import { Frame, Title } from "@/components/Frame";
import { CustomerFigure, PixelSprite } from "@/components/pixel/Pixel";
import { CUSTOMER_LOOKS, STAFF_LOOKS, figureSprite } from "@/components/pixel/sprites";
import { store, type StoreState } from "@/store/gameStore";

/** The four modes (each includes everything in the one before). */
const MODES: { name: string; text: string }[] = [
  { name: "Stage 1", text: "Regular customers, the three-dish menu (Salad, Burger, Pasta), Waiter, Chef and Cashier. The base game." },
  { name: "Stage 2", text: "Stage 1 plus VIP and Critic customers. Every game has at least one of each, on random arrival turns." },
  { name: "Stage 3", text: "Stage 2 plus the Host, two combo meals and Happy Hour." },
  {
    name: "Custom mode",
    text: "Stage 3 plus an Impatient guest on turn 10, Picky, Chatty and Grumpy regulars, and Happy Hour tips.",
  },
];

const STAFF: { role: "Host" | "Waiter" | "Chef"; text: string }[] = [
  {
    role: "Host",
    text:
      "From Stage 3. Stands by the door. Each turn the Host may seat one guest from the queue at a free table, or wait. " +
      "The Host acts first, so the Waiter can take that guest's order in the same turn.",
  },
  {
    role: "Waiter",
    text:
      "The busiest person here: one job per turn. Pick the job, then the table: seat a guest (the only one who seats in " +
      "Stage 1 and 2), take an order at a table " +
      "whose guest is ready, serve a table whose dish was ready before this turn, or wait. Every guest needs the Waiter " +
      "three times (seat, order, serve), so a wasted Waiter turn hurts.",
  },
  {
    role: "Chef",
    text:
      "Cooks one unit per turn, after the Waiter. Salad and Burger take 1 unit, Pasta 2, combos (from Stage 3) the sum " +
      "of their dishes. Finished dishes go on the counter as READY and can be served from the next turn.",
  },
];

const STEPS = [
  "A guest may arrive (turns 1, 4, 7, 10, 13, 16) and joins the queue outside.",
  "From Stage 3: optionally start Happy Hour (once per game).",
  "From Stage 3: Host: seat someone or wait.",
  "Waiter: seat, take an order, serve, or wait.",
  "Chef: cook one unit of an order, or wait.",
  "Eating and payment: the Cashier charges one guest who became ready to pay on an earlier turn (oldest first) and frees their table. Then guests served on an earlier turn finish eating and become ready to pay.",
  "Waiting: everyone not served yet loses satisfaction; at 0 they walk out.",
];

/** Normal-customer sub-types (display only; the rules live on the server). */
const SUBTYPES: { subType: CustomerSubType; name: string; aura: string; text: string }[] = [
  {
    subType: "normal",
    name: "Normal Customer",
    aura: "no outline",
    text: "The everyday guest. No surprises: seat them, take their order, serve them, and they pay as usual.",
  },
  {
    subType: "picky",
    name: "Picky Eater",
    aura: "green outline",
    text:
      "Hard to please! Sends their food back the moment it is served, and it goes back to the counter, so be ready to serve it " +
      "a second time. Stay patient and they will come around.",
  },
  {
    subType: "chatty",
    name: "Chatty Eater",
    aura: "blue outline, holds a phone",
    text:
      "Always glued to their phone. After eating, they stay at the table for one extra turn before paying, so plan your " +
      "seating around them.",
  },
  {
    subType: "grumpy",
    name: "Grumpy Eater",
    aura: "orange outline",
    text:
      "Walks in already annoyed, with satisfaction starting around 80 to 89. Treat them well and their mood improves a lot " +
      "faster than anyone else's: Happy Hour cheers them up twice as much.",
  },
];

/** A stand-in customer so the Guide can draw the exact in-game figure. */
function sample(subType: CustomerSubType): Customer {
  return {
    id: `guide-${subType}`,
    type: "Regular",
    subType,
    satisfaction: subType === "grumpy" ? 85 : 100,
    recordedSatisfaction: -1,
    state: "WAITING",
    table: null,
    orderId: null,
    arrivalTurn: 1,
  };
}

const NOTES: Record<CustomerType, string> = {
  Regular: "Every mode. Loses 8 per waiting turn.",
  VIP: "From Stage 2. Loses 5 per turn and pays 10% less.",
  Critic: "From Stage 2. Loses 12 per turn. Served within 2 turns of ordering: +10. Slower: -10.",
  Impatient: "Custom mode only, on turn 10. Loses 10 per turn and walks out at 30.",
};

export function GuideScreen({ ui }: { ui: StoreState }) {
  return (
    <Frame className="menu-frame scroll">
      <div className="page guide">
        <div className="page-head">
          <Title small />
          <button className="btn" onClick={() => store.goTo("menu")}>
            Back
          </button>
        </div>
        <section className="panel">
          <h2>Your goal</h2>
          <p>
            You are the manager. After <span className="hl">18 turns</span> you win if at least <span className="hl">4 guests paid</span>,
            revenue is at least <span className="hl">$45.00</span>, and the <span className="hl">average satisfaction</span> of paying
            guests is at least <span className="hl">60</span>. Customers choose their own food.
          </p>
        </section>
        <section className="panel">
          <h2>Modes</h2>
          <ul className="steps">
            {MODES.map((m) => (
              <li key={m.name}>
                <span className="hl">{m.name}:</span> {m.text}
              </li>
            ))}
          </ul>
        </section>
        <div className="guide-staff">
          {STAFF.map((s) => (
            <section key={s.role} className="panel staff-card">
              <div className="portrait small">
                <PixelSprite sprite={figureSprite(STAFF_LOOKS[s.role], ui.skin)} scale={5} />
                <span className="portrait-name">{s.role}</span>
              </div>
              <p>{s.text}</p>
            </section>
          ))}
        </div>
        <section className="panel">
          <h2>How a turn works</h2>
          <ol className="steps">
            {STEPS.map((step) => (
              <li key={step}>{step}</li>
            ))}
          </ol>
          <p className="dim">
            A dish finished this turn is served next turn at the earliest. A guest served on turn s eats through turn s+1, is ready to
            pay at its end, and pays at the end of turn s+2. Their table stays taken until then, so it is free again from turn s+3.
          </p>
        </section>
        <section className="panel">
          <h2>Customers</h2>
          <div className="guide-customers">
            {(Object.keys(CUSTOMER_LOOKS) as CustomerType[]).map((type) => (
              <div key={type} className="guide-customer">
                <PixelSprite sprite={figureSprite(CUSTOMER_LOOKS[type], ui.skin, "calm", type)} scale={4} />
                <strong className={`type type-${type}`}>{type}</strong>
                <span>{NOTES[type]}</span>
              </div>
            ))}
          </div>
          <p className="dim">The bar under each guest shows satisfaction. Below 80 their face turns a little red; below 60 they turn red and show anger marks.</p>
        </section>
        <section className="panel">
          <h2>Regular customer types (Custom mode only)</h2>
          <p className="aura-legend">
            <span className="legend-title">Customer auras:</span>
            <span className="legend-item">
              <i className="legend-swatch legend-picky" /> green = Picky
            </span>
            <span className="legend-item">
              <i className="legend-swatch legend-chatty" /> blue = Chatty
            </span>
            <span className="legend-item">
              <i className="legend-swatch legend-grumpy" /> orange = Grumpy
            </span>
            <span className="legend-item">
              <i className="legend-swatch legend-none" /> no outline = Normal
            </span>
          </p>
          <div className="guide-customers">
            {SUBTYPES.map((t) => (
              <div key={t.subType} className="guide-customer">
                <CustomerFigure customer={sample(t.subType)} skin={ui.skin} scale={4} />
                <strong>{t.name}</strong>
                <small className="dim">{t.aura}</small>
                <span>{t.text}</span>
              </div>
            ))}
          </div>
          <p className="dim">
            In Custom mode, a few of the Regular customers may turn out Picky, Chatty or Grumpy. VIPs, Critics and the Impatient guest
            never do. In Stages 1 to 3 every Regular is a Normal customer.
          </p>
          <p className="hh-tip-note">
            <span className="tip-tag">Tip</span> Custom mode only: during happy hour, happy customers may leave a tip at the cashier.
          </p>
        </section>
      </div>
    </Frame>
  );
}
