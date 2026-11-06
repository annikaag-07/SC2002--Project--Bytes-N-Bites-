/**
 * The decision box at the bottom of the screen: the deciding role's portrait, a short description of the
 * situation, "What should I do?", and option cards built from the back end's choices. Disabled cards show the
 * back end's reason. Options that need a target ask follow-up questions one at a time and never auto-answer:
 * Seat asks "Who should I seat?" (skipped when only one person is waiting) then always "Which table?";
 * Take an order and Serve an order ask "Which table?".
 *
 * Keyboard: A-D pick an option, 1-9 answer a follow-up question, Esc goes back.
 */
import { useEffect, useState } from "react";

import type { Choice, GameState, Role } from "@/api/types";
import { PixelSprite } from "@/components/pixel/Pixel";
import { STAFF_LOOKS, figureSprite, type Skin } from "@/components/pixel/sprites";
import { sounds } from "@/sound/soundManager";
import { store } from "@/store/gameStore";

/** The news line: what happened as this turn started (arrivals, Happy Hour). Payments happen at a turn's end. */
function newsLines(game: GameState): string[] {
  return game.turnStartEvents
    .map((e) => e.text)
    .map((t) => {
      const arrived = t.match(/^(C\d+) \((\w+)\) arrives/);
      if (arrived) return `${arrived[1]} (${arrived[2]}) just arrived and is waiting outside.`;
      return t;
    });
}

/** One or two plain sentences about the situation, chosen per role. Pure description - no rules. */
function situation(game: GameState, role: Role): string[] {
  const c = (id: string) => game.customers.find((x) => x.id === id)!;
  const lines: string[] = [];
  const queue =
    game.queue.length === 0
      ? "Nobody is waiting outside."
      : game.queue.length === 1
        ? `${game.queue[0]} (${c(game.queue[0]).type}, ${c(game.queue[0]).satisfaction}) is waiting outside.`
        : `${game.queue.length} guests are waiting outside: ${game.queue.join(", ")}.`;
  const [t1, t2] = game.tables;
  const part = (t: typeof t1) => (t.occupant ? `${t.id} has ${t.occupant}` : `${t.id} is free`);
  const tables = !t1.occupant && !t2.occupant ? "Both tables are free." : `${part(t1)} and ${part(t2)}.`;

  if (role === "Manager") {
    lines.push("Happy Hour can be used once: this turn and the next, new orders are 20% off and waiting guests get +15.");
  }
  if (role === "Chef") {
    const tickets = game.orders.filter((o) => o.status === "PENDING" || o.status === "PREPARING");
    lines.push(
      tickets.length === 0
        ? "No tickets on the rail."
        : `On the rail: ${tickets.map((o) => `${o.item} for ${o.customerId} (${o.unitsDone}/${o.prepUnits})`).join(", ")}.`,
    );
  } else {
    lines.push(`${queue} ${tables}`);
    // The engine's next payer: READY_TO_PAY since an earlier turn, charged in step 4 at the end of this turn.
    const payer = game.cashierNext ? c(game.cashierNext) : null;
    if (payer?.table) lines.push(`${payer.id} is ready to pay: the Cashier charges them at the end of this turn, then ${payer.table} is free.`);
  }
  if (role === "Waiter") {
    const ready = game.orders.filter((o) => o.status === "READY");
    if (ready.length > 0) {
      lines.push(`On the counter: ${ready.map((o) => `${o.item} for ${o.customerId}${o.servableNow ? "" : " (finished this turn)"}`).join(", ")}.`);
    }
  }
  return lines;
}

function warnings(game: GameState, role: Role): string[] {
  const out: string[] = [];
  const angry = game.customers.filter((x) => x.satisfaction < 60 && ["WAITING", "SEATED", "ORDERED"].includes(x.state));
  if (angry.length > 0) out.push(`Getting angry: ${angry.map((x) => `${x.id} (${x.satisfaction})`).join(", ")}!`);
  const critic = game.customers.find((x) => x.type === "Critic" && x.state === "ORDERED");
  if (critic && role !== "Host") out.push("The Critic is waiting: serve within 2 turns of ordering to impress.");
  return out;
}

interface Pending {
  at: string;
  key: string;
  step: number;
  answers: Record<string, string>;
}

export function DecisionBox({ game, skin, busy }: { game: GameState; skin: Skin; busy: boolean }) {
  const decision = game.decision!;
  const here = `${game.turn}-${decision.role}`;
  const [pending, setPending] = useState<Pending | null>(null);
  const [hidden, setHidden] = useState(false);
  const active = pending?.at === here ? pending : null;
  const choice = active ? decision.choices.find((c) => c.key === active.key) ?? null : null;
  const step = choice && active ? choice.steps[active.step] : null;

  const send = (c: Choice, answers: Record<string, string>) => {
    setPending(null);
    void store.choose({ type: c.actionType, ...answers });
  };

  /** Moves to the next unanswered question, or sends the command when all are answered. */
  const goTo = (c: Choice, from: number, answers: Record<string, string>) => {
    let next = from;
    let filled = answers;
    // "Who should I seat?" is skipped when only one person is waiting; "Which table?" is always asked.
    while (next < c.steps.length - 1 && c.steps[next].param === "customer") {
      const enabled = c.steps[next].options.filter((o) => o.enabled);
      if (enabled.length !== 1) break;
      filled = { ...filled, customer: enabled[0].value };
      next++;
    }
    if (next >= c.steps.length) send(c, filled);
    else {
      sounds.play("select");
      setPending({ at: here, key: c.key, step: next, answers: filled });
    }
  };

  const pickChoice = (c: Choice) => {
    if (!c.enabled || busy) return;
    if (c.steps.length === 0) send(c, {});
    else goTo(c, 0, {});
  };

  const answer = (index: number) => {
    if (!choice || !step || !active || busy) return;
    const option = step.options[index];
    if (!option?.enabled) return;
    const answers = { ...active.answers, [step.param]: option.value };
    if (active.step + 1 >= choice.steps.length) send(choice, answers);
    else goTo(choice, active.step + 1, answers);
  };

  const back = () => {
    sounds.play("back");
    setPending(null);
  };

  // Keyboard shortcuts (an external input source, so an effect is appropriate here).
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.metaKey || e.ctrlKey || e.altKey) return;
      const key = e.key.toLowerCase();
      if (step) {
        if (key === "escape") back();
        const n = Number(key);
        if (n >= 1 && n <= 9) answer(n - 1);
      } else if (key.length === 1 && key >= "a" && key <= "d") {
        const c = decision.choices[key.charCodeAt(0) - 97];
        if (c) pickChoice(c);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  const news = newsLines(game);
  const seating = choice && active?.answers.customer && choice.key.endsWith("Seat") ? active.answers.customer : null;

  return (
    <div className="dialog fade-in" role="group" aria-label={`${decision.role}'s decision`}>
      <div className="portrait">
        <PixelSprite sprite={figureSprite(STAFF_LOOKS[decision.role], skin)} scale={6} title={decision.role} />
        <span className="portrait-name">{decision.role}</span>
      </div>
      <div className="dialog-main">
        <button className="btn btn-small hide-btn" onClick={() => setHidden((h) => !h)}>
          {hidden ? "Show choices" : "Hide choices"}
        </button>
        {news.map((line) => (
          <p key={line} className="news">
            <span className="news-tag">News</span> {line}
          </p>
        ))}
        {situation(game, decision.role).map((line) => (
          <p key={line} className="situation">
            {line}
          </p>
        ))}
        {warnings(game, decision.role).map((line) => (
          <p key={line} className="warning">
            {line}
          </p>
        ))}
        <p className="ask">
          {step ? step.prompt : "What should I do?"}
          {seating && <span className="ask-context"> (seating {seating})</span>}
        </p>

        {!hidden &&
          (step && choice && active ? (
            <div className="cards">
              {step.options.map((option, i) => (
                <button key={option.label} className="card-option" disabled={!option.enabled || busy} onClick={() => answer(i)}>
                  <span className="key">{i + 1}</span>
                  <span className="card-text">
                    <strong>{option.label}</strong>
                    <small>{option.enabled ? option.detail : option.reason}</small>
                  </span>
                </button>
              ))}
              <button className="card-option card-back" onClick={back}>
                <span className="key">Esc</span>
                <span className="card-text">
                  <strong>Back</strong>
                  <small>Choose a different job.</small>
                </span>
              </button>
            </div>
          ) : (
            <div className="cards">
              {decision.choices.map((c, i) => (
                <button key={c.key} className="card-option" disabled={!c.enabled || busy} onClick={() => pickChoice(c)}>
                  <span className="key">{String.fromCharCode(65 + i)}</span>
                  <span className="card-text">
                    <strong>{c.label}</strong>
                    <small>{c.enabled ? c.hint : c.reason}</small>
                  </span>
                </button>
              ))}
            </div>
          ))}
      </div>
    </div>
  );
}
