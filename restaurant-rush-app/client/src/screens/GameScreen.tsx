import { useState } from "react";

import { Frame } from "@/components/Frame";
import { DecisionBox } from "@/components/game/DecisionBox";
import { Hud } from "@/components/game/Hud";
import { GameOver, LogDrawer, TurnSummary } from "@/components/game/Panels";
import { World } from "@/components/game/World";
import { store, type StoreState } from "@/store/gameStore";

/** The restaurant inside the neon frame, with the decision box along the bottom. */
export function GameScreen({ ui }: { ui: StoreState }) {
  const game = ui.game!;
  const [showLog, setShowLog] = useState(false);
  return (
    <Frame className="game-frame">
      <Hud game={game} ui={ui} onLog={() => setShowLog(true)} />
      <World game={game} ui={ui} theme={ui.previewTheme ?? ui.theme} />
      <div className="dialog-area">
        {ui.lastMessage && !ui.lastMessage.success && <p className="warning">Not allowed: {ui.lastMessage.text}</p>}
        {ui.error && (
          <p className="warning" role="alert">
            {ui.error}{" "}
            <button className="btn btn-small" onClick={() => store.dismissError()}>
              OK
            </button>
          </p>
        )}
        {/* Choices stay locked while an animation replays the last result, so the turn flow cannot desync. */}
        {game.decision && <DecisionBox game={game} skin={ui.skin} busy={ui.busy || ui.animating} />}
        {game.phase === "SUMMARY" && <TurnSummary game={game} skin={ui.skin} busy={ui.busy || ui.animating} />}
      </div>
      {showLog && <LogDrawer game={game} onClose={() => setShowLog(false)} />}
      {game.phase === "OVER" && <GameOver game={game} />}
    </Frame>
  );
}
