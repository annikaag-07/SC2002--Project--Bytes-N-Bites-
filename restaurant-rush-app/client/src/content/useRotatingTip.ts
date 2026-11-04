import { useEffect, useState } from "react";

import { TIPS } from "./tips";

/** How long each tip stays on screen before the next one appears. */
export const TIP_DURATION_MS = 10000;

/** Returns a tip that changes every TIP_DURATION_MS (Minecraft-loading-screen style). */
export function useRotatingTip(intervalMs = TIP_DURATION_MS): { tip: string; index: number } {
  const [index, setIndex] = useState(() => Math.floor(Math.random() * TIPS.length));
  useEffect(() => {
    const timer = setInterval(() => setIndex((i) => (i + 1) % TIPS.length), intervalMs);
    return () => clearInterval(timer);
  }, [intervalMs]);
  return { tip: TIPS[index], index };
}
