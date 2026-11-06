/**
 * Game state store (layer b). Holds the latest snapshot from the server plus UI-only state (screen, skin,
 * mute), runs the API calls, and triggers sound hooks. Components read it with useStore() and call its
 * actions; they never change game state themselves.
 */
import { useSyncExternalStore } from "react";

import { api, ApiError } from "@/api/client";
import type { ActionResponse, GameAction, GameEvent, GameMode, GameState, ScoreEntry } from "@/api/types";
import { planAnimation, planDuration, type Plan } from "@/animation/choreography";
import { sounds, type SoundName } from "@/sound/soundManager";
import { DEFAULT_THEME, isThemeId, type ThemeId } from "@/theme/themes";

export type Screen = "loading" | "menu" | "guide" | "scores" | "game";
export type Skin = "people" | "penguins";
/** How character movement is shown: full speed, faster, or not at all. Never affects the game itself. */
export type AnimSpeed = "normal" | "fast" | "off";

/** Time multiplier for each animation speed. */
export const SPEED_FACTOR: Record<AnimSpeed, number> = { normal: 1, fast: 0.45, off: 0 };

export interface StoreState {
  screen: Screen;
  game: GameState | null;
  busy: boolean;
  error: string | null;
  /** Message from the last command (success or the back end's rejection reason). */
  lastMessage: { success: boolean; text: string } | null;
  /** Events produced by the last command, for small animations. */
  lastEvents: GameEvent[];
  hasSave: boolean;
  /** Day number the next new game will get. */
  nextDay: number;
  highScores: ScoreEntry[];
  skin: Skin;
  muted: boolean;
  /** Chosen restaurant theme (Neon Nights by default). */
  theme: ThemeId;
  /** Theme shown while the player is browsing the theme picker (null when not previewing). */
  previewTheme: ThemeId | null;
  animSpeed: AnimSpeed;
  /** The movement sequence currently being played over the restaurant, if any. */
  animation: Plan | null;
  /** True while an animation plays: decisions are locked until it finishes. */
  animating: boolean;
}

const PREFS_KEY = "bytes-n-bites:prefs";
const LOADING_MIN_MS = 1800;

/** Safety margin: an animation can never hold the input lock longer than its length plus this. */
const ANIMATION_GRACE_MS = 2500;

function loadPrefs(): Pick<StoreState, "skin" | "muted" | "theme" | "animSpeed"> {
  try {
    const prefs = JSON.parse(localStorage.getItem(PREFS_KEY) ?? "{}");
    return {
      skin: prefs.skin === "penguins" ? "penguins" : "people",
      muted: prefs.muted === true,
      theme: isThemeId(prefs.theme) ? prefs.theme : DEFAULT_THEME,
      animSpeed: prefs.animSpeed === "fast" || prefs.animSpeed === "off" ? prefs.animSpeed : "normal",
    };
  } catch {
    return { skin: "people", muted: false, theme: DEFAULT_THEME, animSpeed: "normal" };
  }
}

/** Sound to play for a successful command type. */
const ACTION_SOUNDS: Record<string, SoundName> = {
  hostSeat: "seat",
  waiterSeat: "seat",
  waiterTakeOrder: "order",
  waiterServe: "serve",
  chefCook: "cook",
  happyHourYes: "happyHour",
};

class GameStore {
  private state: StoreState = {
    screen: "loading",
    game: null,
    busy: false,
    error: null,
    lastMessage: null,
    lastEvents: [],
    hasSave: false,
    nextDay: 1,
    highScores: [],
    previewTheme: null,
    animation: null,
    animating: false,
    ...loadPrefs(),
  };
  private readonly listeners = new Set<() => void>();
  private animationTimer: ReturnType<typeof setTimeout> | null = null;
  private nextPlanId = 1;

  constructor() {
    sounds.setMuted(this.state.muted);
  }

  getState = (): StoreState => this.state;

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  private set(partial: Partial<StoreState>): void {
    this.state = { ...this.state, ...partial };
    this.listeners.forEach((listener) => listener());
  }

  private async run(task: () => Promise<void>): Promise<void> {
    this.set({ busy: true, error: null });
    try {
      await task();
    } catch (error) {
      this.set({ error: error instanceof ApiError ? error.message : String(error) });
    } finally {
      this.set({ busy: false });
    }
  }

  // ------------------------------------------------------------- navigation

  /** Loading screen: check the server and the saved game, but show the tips for a moment. */
  async boot(): Promise<void> {
    const started = Date.now();
    await this.run(async () => {
      const status = await api.status();
      this.set({ hasSave: status.hasSave, nextDay: status.nextDay });
    });
    const wait = Math.max(0, LOADING_MIN_MS - (Date.now() - started));
    setTimeout(() => this.set({ screen: "menu" }), wait);
  }

  goTo(screen: Screen): void {
    sounds.play("click");
    this.stopAnimation();
    this.set({ screen, error: null });
    if (screen === "scores") void this.loadHighScores();
    if (screen === "menu") void this.refreshStatus();
  }

  private async refreshStatus(): Promise<void> {
    try {
      const status = await api.status();
      this.set({ hasSave: status.hasSave, nextDay: status.nextDay });
    } catch {
      // the menu still works; Continue just stays as it was
    }
  }

  async loadHighScores(): Promise<void> {
    await this.run(async () => this.set({ highScores: await api.highScores() }));
  }

  // ------------------------------------------------------------- game

  async newGame(mode: GameMode): Promise<void> {
    sounds.play("select");
    await this.run(async () => {
      const game = await api.newGame(mode);
      this.set({ game, screen: "game", lastMessage: null, lastEvents: [], hasSave: true });
    });
  }

  async continueGame(): Promise<void> {
    sounds.play("click");
    await this.run(async () => {
      const game = await api.continueGame();
      this.set({ game, screen: "game", lastMessage: { success: true, text: "Welcome back! Game restored." }, lastEvents: [] });
    });
  }

  /** Sends the chosen MCQ option to the back end and shows the new state. */
  async choose(action: GameAction): Promise<void> {
    if (this.state.animating) return;
    sounds.play("click");
    await this.run(async () => this.apply(await api.act(action), action));
  }

  /** Leaves the end-of-turn summary (starts the next turn, or shows the result). */
  async nextTurn(): Promise<void> {
    if (this.state.animating) return;
    sounds.play("nextTurn");
    await this.run(async () => this.apply(await api.next(), { type: "next" }));
  }

  /**
   * Shows the back end's result. The new state is applied straight away (the game logic is the source of
   * truth); the movement plan only replays it visually, with input locked until it finishes.
   */
  private apply(response: ActionResponse, action: GameAction): void {
    const before = this.state.game;
    const { result, events, state } = response;
    const plan =
      result.success && before && this.state.animSpeed !== "off"
        ? planAnimation(this.nextPlanId++, before, state, action, events)
        : null;
    this.set({
      game: state,
      lastMessage: { success: result.success, text: result.message },
      lastEvents: events,
      hasSave: state.phase !== "OVER",
      animation: plan,
      animating: plan !== null,
    });
    if (plan) {
      if (this.animationTimer) clearTimeout(this.animationTimer);
      const limit = planDuration(plan) * SPEED_FACTOR[this.state.animSpeed] + ANIMATION_GRACE_MS;
      this.animationTimer = setTimeout(() => this.finishAnimation(plan.id), limit);
    }
    if (!result.success) {
      sounds.play("error");
      return;
    }
    this.playSoundsFor(action.type, events, before, state);
  }

  /** Called by the restaurant view when a movement sequence ends (or by the safety timer). */
  finishAnimation(id: number): void {
    if (this.state.animation?.id !== id) return;
    this.stopAnimation();
  }

  private stopAnimation(): void {
    if (this.animationTimer) clearTimeout(this.animationTimer);
    this.animationTimer = null;
    if (this.state.animation || this.state.animating) this.set({ animation: null, animating: false });
  }

  private playSoundsFor(type: string, events: GameEvent[], before: GameState | null, after: GameState): void {
    const actionSound = ACTION_SOUNDS[type];
    if (actionSound) sounds.play(actionSound);
    if (events.some((e) => e.text.includes("Cashier collects"))) setTimeout(() => sounds.play("pay"), 180);
    if (events.some((e) => e.text.includes(" arrives "))) setTimeout(() => sounds.play("arrive"), 380);
    if (events.some((e) => e.text.includes("leaves unhappy"))) sounds.play("leave");
    // Anger: someone newly dropped below 60 satisfaction.
    const wasCalm = new Map(before?.customers.map((c) => [c.id, c.satisfaction >= 60]) ?? []);
    if (after.customers.some((c) => c.satisfaction < 60 && c.state !== "LEFT" && wasCalm.get(c.id) !== false)) {
      sounds.play("anger");
    }
    if (after.phase === "OVER" && after.result) sounds.play(after.result.victory ? "victory" : "gameOver");
  }

  // ------------------------------------------------------------- preferences

  toggleSkin(): void {
    this.set({ skin: this.state.skin === "people" ? "penguins" : "people" });
    this.savePrefs();
  }

  /** Shows a theme everywhere while the player browses the picker; nothing is saved yet. */
  previewTheme(theme: ThemeId | null): void {
    this.set({ previewTheme: theme });
  }

  setTheme(theme: ThemeId): void {
    sounds.play("select");
    this.set({ theme, previewTheme: null });
    this.savePrefs();
  }

  /** Normal -> Fast -> Off -> Normal. */
  cycleAnimSpeed(): void {
    const next: Record<AnimSpeed, AnimSpeed> = { normal: "fast", fast: "off", off: "normal" };
    sounds.play("click");
    this.set({ animSpeed: next[this.state.animSpeed] });
    if (this.state.animSpeed === "off") this.stopAnimation();
    this.savePrefs();
  }

  toggleMute(): void {
    const muted = !this.state.muted;
    sounds.setMuted(muted);
    this.set({ muted });
    this.savePrefs();
  }

  dismissError(): void {
    this.set({ error: null });
  }

  private savePrefs(): void {
    try {
      const { skin, muted, theme, animSpeed } = this.state;
      localStorage.setItem(PREFS_KEY, JSON.stringify({ skin, muted, theme, animSpeed }));
    } catch {
      // preferences are a convenience only
    }
  }
}

export const store = new GameStore();

/** React hook: re-renders when the store changes. */
export function useStore(): StoreState {
  return useSyncExternalStore(store.subscribe, store.getState);
}
