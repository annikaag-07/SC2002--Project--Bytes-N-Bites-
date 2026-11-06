import type { CustomerType } from "@/api/types";
import { Frame, Title } from "@/components/Frame";
import { PixelSprite } from "@/components/pixel/Pixel";
import { CUSTOMER_LOOKS, STAFF_LOOKS, figureSprite } from "@/components/pixel/sprites";
import { store, type StoreState } from "@/store/gameStore";

const STAFF: { role: "Host" | "Waiter" | "Chef"; text: string }[] = [
  {
    role: "Host",
    text:
      "Stands by the door. Each turn the Host may seat one guest from the queue at a free table, or wait. " +
      "The Host acts first, so the Waiter can take that guest's order in the same turn.",
  },
  {
    role: "Waiter",
    text:
      "The busiest person here: one job per turn. Pick the job, then the table: seat a guest, take an order at a table " +
      "whose guest is ready, serve a table whose dish was ready before this turn, or wait. Every guest needs the Waiter " +
      "three times (seat, order, serve), so a wasted Waiter turn hurts.",
  },
  {
    role: "Chef",
    text:
      "Cooks one unit per turn, after the Waiter. Salad and Burger take 1 unit, Pasta 2, combos the sum of their " +
      "dishes. Finished dishes go on the counter as READY and can be served from the next turn.",
  },
];

const STEPS = [
  "A guest may arrive (turns 1, 4, 7, 10, 13, 16) and joins the queue outside.",
  "Optionally start Happy Hour (once per game).",
  "Host: seat someone or wait.",
  "Waiter: seat, take an order, serve, or wait.",
  "Chef: cook one unit of an order, or wait.",
  "Eating and payment: the Cashier charges one guest who became ready to pay on an earlier turn (oldest first) and frees their table. Then guests served on an earlier turn finish eating and become ready to pay.",
  "Waiting: everyone not served yet loses satisfaction; at 0 they walk out.",
];

const NOTES: Record<CustomerType, string> = {
  Regular: "Loses 8 per waiting turn.",
  VIP: "Loses 5 per turn and pays 10% less.",
  Critic: "Loses 12 per turn. Served within 2 turns of ordering: +10. Slower: -10.",
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
          <p className="dim">
            Every game has six guests (on turns 1, 4, 7, 10, 13 and 16) with at least one VIP and one Critic; who arrives when
            changes from game to game.
          </p>
          <p className="dim">
            The bar under each guest shows satisfaction. Below 80 their face turns a little red; below 60 they turn red and show anger
            marks.
          </p>
        </section>
      </div>
    </Frame>
  );
}
