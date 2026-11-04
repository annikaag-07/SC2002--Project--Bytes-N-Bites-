import { Frame, Title } from "@/components/Frame";
import { store, type StoreState } from "@/store/gameStore";

export function HighScoreScreen({ ui }: { ui: StoreState }) {
  return (
    <Frame rain className="menu-frame">
      <div className="page">
        <Title small />
        <section className="panel">
          <h2>High scores</h2>
          {ui.highScores.length === 0 ? (
            <p className="dim">No days finished yet. Go open the restaurant!</p>
          ) : (
            <table className="scores">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Day</th>
                  <th>Mode</th>
                  <th>Result</th>
                  <th>Stars</th>
                  <th>Revenue</th>
                  <th>Paid</th>
                  <th>Avg</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {ui.highScores.map((s, i) => (
                  <tr key={i}>
                    <td>{i + 1}</td>
                    <td>Day {s.day}</td>
                    <td>{s.modeLabel ?? s.mode}</td>
                    <td className={s.victory ? "met" : "not-met"}>{s.victory ? "VICTORY" : "DEFEAT"}</td>
                    <td className="stars small">
                      {"★".repeat(s.stars)}
                      <span className="stars-off">{"★".repeat(3 - s.stars)}</span>
                    </td>
                    <td>${(s.revenueCents / 100).toFixed(2)}</td>
                    <td>{s.paid}</td>
                    <td>{s.average}</td>
                    <td className="dim">{s.date}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
        <button className="btn" onClick={() => store.goTo("menu")}>
          Back
        </button>
      </div>
    </Frame>
  );
}
