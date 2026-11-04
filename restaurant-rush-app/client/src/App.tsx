import { useEffect } from "react";

import { GameScreen } from "@/screens/GameScreen";
import { GuideScreen } from "@/screens/GuideScreen";
import { HighScoreScreen } from "@/screens/HighScoreScreen";
import { LoadingScreen } from "@/screens/LoadingScreen";
import { MainMenu } from "@/screens/MainMenu";
import { store, useStore } from "@/store/gameStore";

/** Picks the screen from the store; screens render state and call store actions. */
export default function App() {
  const ui = useStore();
  useEffect(() => {
    void store.boot();
  }, []);

  return (
    // The theme only restyles the restaurant; the preview is shown while the player browses the picker.
    <div className="app" data-skin={ui.skin} data-theme={ui.previewTheme ?? ui.theme}>
      {/* a short fade whenever the theme changes */}
      <div className="theme-fade" key={ui.previewTheme ?? ui.theme} aria-hidden />
      {ui.screen === "loading" && <LoadingScreen ui={ui} />}
      {ui.screen === "menu" && <MainMenu ui={ui} />}
      {ui.screen === "guide" && <GuideScreen ui={ui} />}
      {ui.screen === "scores" && <HighScoreScreen ui={ui} />}
      {ui.screen === "game" && ui.game && <GameScreen ui={ui} />}
    </div>
  );
}
