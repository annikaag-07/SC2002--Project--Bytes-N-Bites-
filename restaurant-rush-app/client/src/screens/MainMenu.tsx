import { useState } from "react";

import type { GameMode } from "@/api/types";
import { Frame, Title } from "@/components/Frame";
import { ThemePicker } from "@/components/ThemePicker";
import { PixelSprite } from "@/components/pixel/Pixel";
import { STAFF_LOOKS, figureSprite } from "@/components/pixel/sprites";
import { useRotatingTip } from "@/content/useRotatingTip";
import { sounds } from "@/sound/soundManager";
import { store, type StoreState } from "@/store/gameStore";

/** The four modes, in order. Each builds on the one before (the assignment's stages, then the web extras). */
const MODES: { mode: GameMode; name: string; text: string }[] = [
  { mode: "stage1", name: "Stage 1", text: "Regular customers, the three-dish menu, Waiter, Chef and Cashier. The base game." },
  { mode: "stage2", name: "Stage 2", text: "Stage 1 plus VIP and Critic customers." },
  { mode: "stage3", name: "Stage 3", text: "Stage 2 plus the Host, combo meals and Happy Hour." },
  {
    mode: "custom",
    name: "Custom mode",
    text: "Stage 3 plus an Impatient guest on turn 10, Picky, Chatty and Grumpy regulars, and Happy Hour tips.",
  },
];

/** Picking a mode starts that game straight away. */
function NewGameDialog({ ui, onClose }: { ui: StoreState; onClose: () => void }) {
  return (
    <div className="overlay" onClick={onClose}>
      <div className="modal new-game pop-in" role="dialog" aria-label="New game" onClick={(e) => e.stopPropagation()}>
        <h2>New game</h2>
        <p className="field-label">Choose a mode</p>
        <div className="radio-cards">
          {MODES.map((m) => (
            <button
              key={m.mode}
              className="radio-card"
              aria-label={`Start ${m.name}: ${m.text}`}
              disabled={ui.busy}
              onClick={() => void store.newGame(m.mode)}
            >
              <span className="radio-dot" />
              <span>
                <strong>{m.name}</strong>
                <small>{m.text}</small>
              </span>
            </button>
          ))}
        </div>
        <p className="dim">
          This will be <strong className="hl">day {ui.nextDay}</strong> at Bytes N Bites. Every customer still picks their own food.
        </p>
        <div className="modal-actions">
          <button className="btn" onClick={onClose}>
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}

export function MainMenu({ ui }: { ui: StoreState }) {
  const { tip, index } = useRotatingTip();
  const [newGame, setNewGame] = useState(false);
  const [themes, setThemes] = useState(false);
  return (
    <Frame rain className="menu-frame">
      <div className="corner-toggles">
        <button className={`btn btn-small ${ui.skin === "penguins" ? "on" : ""}`} onClick={() => store.toggleSkin()} aria-pressed={ui.skin === "penguins"}>
          Penguins
        </button>
        <button className={`btn btn-small ${ui.muted ? "" : "on"}`} onClick={() => store.toggleMute()} aria-pressed={!ui.muted}>
          {ui.muted ? "Sound off" : "Sound on"}
        </button>
        <button className="btn btn-small" onClick={() => store.cycleAnimSpeed()} title="Character movement: Normal, Fast or Off (skip)">
          Moves: {ui.animSpeed === "normal" ? "Normal" : ui.animSpeed === "fast" ? "Fast" : "Off"}
        </button>
      </div>
      <div className="menu">
        <div className="menu-cast" aria-hidden>
          <PixelSprite sprite={figureSprite(STAFF_LOOKS.Host, ui.skin)} scale={3} className="bob" />
          <PixelSprite sprite={figureSprite(STAFF_LOOKS.Chef, ui.skin)} scale={3} className="bob delay-1" />
          <PixelSprite sprite={figureSprite(STAFF_LOOKS.Waiter, ui.skin)} scale={3} className="bob delay-2" />
        </div>
        <Title />
        <p className="tagline">Eighteen turns, two tables, one very busy manager.</p>
        <nav className="menu-buttons">
          <button
            className="btn btn-primary"
            onClick={() => {
              sounds.play("select");
              setNewGame(true);
            }}
          >
            Start new game
          </button>
          <button className="btn" disabled={ui.busy || !ui.hasSave} title={ui.hasSave ? undefined : "No saved game yet"} onClick={() => void store.continueGame()}>
            Continue
          </button>
          <button className="btn" onClick={() => store.goTo("scores")}>
            High scores
          </button>
          <button className="btn" onClick={() => store.goTo("guide")}>
            Guide
          </button>
          <button
            className="btn"
            onClick={() => {
              sounds.play("select");
              setThemes(true);
            }}
          >
            Restaurant themes
          </button>
        </nav>
        {ui.error && <p className="warning">{ui.error}</p>}
      </div>
      <p className="tip fade-in" key={index}>
        <span className="tip-tag">Tip</span> {tip}
      </p>
      {newGame && <NewGameDialog ui={ui} onClose={() => setNewGame(false)} />}
      {themes && <ThemePicker current={ui.theme} skin={ui.skin} onClose={() => setThemes(false)} />}
    </Frame>
  );
}
