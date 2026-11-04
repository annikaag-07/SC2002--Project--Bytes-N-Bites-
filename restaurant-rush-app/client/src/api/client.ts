/**
 * Thin HTTP adapter to the Java API server. Every function maps 1:1 to an endpoint in WebServer.java.
 * In development Vite proxies /api to http://127.0.0.1:8090; in production the Java server serves this app.
 */
import type { ActionResponse, GameAction, GameMode, GameState, ScoreEntry } from "./types";

export class ApiError extends Error {}

async function call<T>(path: string, method: "GET" | "POST" = "GET", params: Record<string, string | undefined> = {}): Promise<T> {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") query.set(key, value);
  }
  const url = query.size > 0 ? `${path}?${query}` : path;
  let response: Response;
  try {
    response = await fetch(url, { method });
  } catch {
    throw new ApiError("Cannot reach the game server. Start it with ./start.sh (or ./dev.sh) and reload.");
  }
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(body?.error ?? `Server error ${response.status}`);
  }
  return body as T;
}

export const api = {
  status: () => call<{ hasSave: boolean; hasGame: boolean; nextDay: number }>("/api/status"),
  newGame: (mode: GameMode) => call<GameState>("/api/new", "POST", { mode }),
  continueGame: () => call<GameState>("/api/continue", "POST"),
  state: () => call<GameState>("/api/state"),
  act: (action: GameAction) =>
    call<ActionResponse>("/api/action", "POST", {
      type: action.type,
      customer: action.customer,
      table: action.table,
      order: action.order,
    }),
  next: () => call<ActionResponse>("/api/next", "POST"),
  highScores: () => call<ScoreEntry[]>("/api/highscores"),
};
