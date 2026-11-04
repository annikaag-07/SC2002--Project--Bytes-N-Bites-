/**
 * Restaurant themes. A theme only changes how the restaurant looks (colours in theme.css and prop patterns in
 * components/pixel/themeSprites.ts). Every theme uses the same layout, prop sizes, positions and animation
 * timing, so the theme can never change gameplay or difficulty.
 */
export type ThemeId = "neon" | "cafe" | "tokyo";

export interface ThemeInfo {
  id: ThemeId;
  name: string;
  tagline: string;
}

export const THEMES: ThemeInfo[] = [
  { id: "neon", name: "Neon Nights", tagline: "Synthwave arcade diner: purple, magenta and cyan neon." },
  { id: "cafe", name: "Cozy Café", tagline: "Warm wood, cream walls, golden lamps and plants." },
  { id: "tokyo", name: "Tokyo Diner", tagline: "Red lanterns, dark wood, tatami and gold." },
];

export const DEFAULT_THEME: ThemeId = "neon";

export function isThemeId(value: unknown): value is ThemeId {
  return value === "neon" || value === "cafe" || value === "tokyo";
}
