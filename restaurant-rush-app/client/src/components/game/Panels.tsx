/** End-of-turn summary, event log and game-over dialog. */
import type { GameEvent, GameState } from "@/api/types";
import { PixelSprite } from "@/components/pixel/Pixel";
import { STAFF_LOOKS, figureSprite, type Skin } from "@/components/pixel/sprites";
import { store } from "@/store/gameStore";

function tone(text: string): string {
  if (/leaves unhappy|UNIMPRESSED/.test(text)) return "ev-bad";
  if (/collects|IMPRESSED/.test(text)) return "ev-good";
  if (/arrives|Happy Hour/.test(text)) return "ev-info";
  if (/waits/.test(text)) return "ev-dim";
  return "";
}

export function EventList({ events }: { events: GameEvent[] }) {
  return (
    <ul className="events">
      {events.map((e, i) => (
        <li key={i} className={tone(e.text)}>
          {e.text}
        </li>
      ))}
    </ul>
  );
}

export function TurnSummary({ game, skin, busy }: { game: GameState; skin: Skin; busy: boolean }) {
  const last = game.turn >= game.totalTurns;
  return (
    <div className="dialog fade-in">
      <div className="portrait">
        <PixelSprite sprite={figureSprite(STAFF_LOOKS.Manager, skin)} scale={6} title="Manager" />
        <span className="portrait-name">Manager</span>
      </div>
      <div className="dialog-main">
        <p className="ask">End of turn {game.turn}</p>
        <EventList events={game.turnSummary} />
        <p className="situation">
          Served {game.servedCount} · Left unhappy {game.departureCount} · Paid {game.paidCount} · Revenue {game.revenue}
          {game.features.tips ? ` · Tips ${game.tips}` : ""} · Avg {game.averageSatisfaction} · Turns used {game.turn}/{game.totalTurns}
        </p>
        <button className="btn btn-primary next-btn" disabled={busy} onClick={() => void store.nextTurn()} autoFocus>
          {last ? "See the result" : `Start turn ${game.turn + 1}`}
        </button>
      </div>
    </div>
  );
}

export function LogDrawer({ game, onClose }: { game: GameState; onClose: () => void }) {
  const turns = [...new Set(game.log.map((e) => e.turn))].sort((a, b) => b - a);
  return (
    <div className="overlay" onClick={onClose}>
      <aside className="modal log" role="dialog" aria-label="Event log" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h2>Log</h2>
          <button className="btn btn-small" onClick={onClose}>
            Close
          </button>
        </div>
        <div className="log-body">
          {turns.map((turn) => (
            <section key={turn}>
              <h3>Turn {turn}</h3>
              <EventList events={game.log.filter((e) => e.turn === turn)} />
            </section>
          ))}
        </div>
      </aside>
    </div>
  );
}

export function GameOver({ game }: { game: GameState }) {
  const r = game.result!;
  const rows = [
    { label: "Paid customers", value: String(r.paidCount), goal: `${game.targets.paid}`, met: r.paidMet },
    { label: "Revenue", value: r.revenue, goal: game.targets.revenue, met: r.revenueMet },
    { label: "Average satisfaction", value: r.averageSatisfaction, goal: `${game.targets.averageSatisfaction}`, met: r.satisfactionMet },
  ];
  // "classic" is API-only; Play again offers its closest playable mode.
  const mode = game.mode === "classic" ? "stage3" : game.mode;
  return (
    <div className="overlay">
      <div className="modal result pop-in" role="dialog" aria-label="Game over">
        <p className="dim">
          Closing time · day {game.day} ({game.modeLabel}) is over
        </p>
        <h2 className={r.victory ? "victory" : "defeat"}>{r.victory ? "Victory" : "Defeat"}</h2>
        <p className="stars" aria-label={`${r.stars} of 3 stars`}>
          {"★".repeat(r.stars)}
          <span className="stars-off">{"★".repeat(3 - r.stars)}</span>
        </p>
        <table className="criteria">
          <tbody>
            {rows.map((row) => (
              <tr key={row.label}>
                <td>{row.label}</td>
                <td>
                  <strong>{row.value}</strong> <span className="dim">(need ≥ {row.goal})</span>
                </td>
                <td className={row.met ? "met" : "not-met"}>{row.met ? "MET" : "NOT MET"}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {game.features.tips && (
          <p className="dim">
            Tips {game.tips} · Total earned {game.totalEarned}
            {game.tipsCountTowardsTarget ? "" : " (tips are a bonus: they do not count towards the revenue target)"}
          </p>
        )}
        <p className="result-line">RESULT: {r.victory ? "VICTORY" : "DEFEAT"}</p>
        <div className="modal-actions">
          <button className="btn" onClick={() => store.goTo("menu")}>
            Main menu
          </button>
          <button className="btn" onClick={() => store.goTo("scores")}>
            High scores
          </button>
          <button className="btn btn-primary" onClick={() => void store.newGame(mode)}>
            Play again
          </button>
        </div>
      </div>
    </div>
  );
}
