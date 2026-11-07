import type { GameState } from "@/api/types";
import { store, type StoreState } from "@/store/gameStore";

/** Top bar: menu, turn progress, the three victory targets, Tips and Happy Hour (only in modes that have them), mode/day, and toggles. */
export function Hud({ game, ui, onLog }: { game: GameState; ui: StoreState; onLog: () => void }) {
  const avg = game.averageSatisfaction;
  const hh = game.happyHour;
  const met = {
    paid: game.paidCount >= game.targets.paid,
    revenue: game.revenueCents >= game.targets.revenueCents,
    avg: avg !== "N/A" && parseFloat(avg) >= game.targets.averageSatisfaction,
  };
  return (
    <header className="hud">
      <button className="btn btn-small" onClick={() => store.goTo("menu")}>
        Menu
      </button>
      <div className="turn">
        <span>
          Turn <strong className="turn-number">{game.turn}</strong> of {game.totalTurns}
        </span>
        <span className="turn-bar" aria-hidden>
          <span style={{ width: `${(game.turn / game.totalTurns) * 100}%` }} />
        </span>
      </div>
      <span className={`stat ${met.paid ? "met" : ""}`}>
        Paid <strong>{game.paidCount}</strong>/{game.targets.paid}
      </span>
      <span className={`stat ${met.revenue ? "met" : ""}`}>
        Revenue <strong>{game.revenue}</strong>/{game.targets.revenue}
      </span>
      {game.features.tips && (
        <span className="stat" title="Happy Hour tips (not part of the $45 revenue target)">
          Tips <strong>{game.tips}</strong>
        </span>
      )}
      <span className={`stat ${met.avg ? "met" : ""}`}>
        Avg <strong>{avg}</strong>/{game.targets.averageSatisfaction}
      </span>
      {game.features.happyHour && (
        <span className={`stat ${hh.remainingTurns > 0 ? "happy" : ""}`}>
          Happy Hour{" "}
          <strong>{hh.remainingTurns > 0 ? `ON ${hh.remainingTurns}` : hh.available ? "ready" : "used"}</strong>
        </span>
      )}
      <span className="hud-spacer" />
      <span className="mode-label">
        {game.modeLabel} · day {game.day}
      </span>
      <button className="btn btn-small" onClick={onLog}>
        Log
      </button>
      <button className="btn btn-small" onClick={() => store.cycleAnimSpeed()} title="Character movement: Normal, Fast or Off (skip)">
        Moves {ui.animSpeed === "normal" ? "1x" : ui.animSpeed === "fast" ? "2x" : "off"}
      </button>
      <button className={`btn btn-small ${ui.skin === "penguins" ? "on" : ""}`} onClick={() => store.toggleSkin()} aria-pressed={ui.skin === "penguins"}>
        Penguins
      </button>
      <button className={`btn btn-small ${ui.muted ? "" : "on"}`} onClick={() => store.toggleMute()} aria-pressed={!ui.muted}>
        Sound
      </button>
    </header>
  );
}
