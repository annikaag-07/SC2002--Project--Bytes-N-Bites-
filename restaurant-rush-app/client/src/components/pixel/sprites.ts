/**
 * Pixel-art sprites. A sprite is a grid of palette keys ("." = transparent); PixelSprite draws each run of
 * pixels as a crisp SVG rectangle. Colours are CSS variables from theme/theme.css (or plain hex for skin and
 * hair tones), so the art can be recoloured without touching this file.
 */
import type { CustomerType, Role } from "@/api/types";

export type Sprite = { rows: string[]; palette: Record<string, string> };
export type Mood = "calm" | "annoyed" | "angry";
export type Skin = "people" | "penguins";

// ------------------------------------------------------------------ helpers

/** Mixes two #rrggbb colours (t = 0 gives a, 1 gives b). Used to tint faces red as customers get angry. */
function mix(a: string, b: string, t: number): string {
  const pa = [1, 3, 5].map((i) => parseInt(a.slice(i, i + 2), 16));
  const pb = [1, 3, 5].map((i) => parseInt(b.slice(i, i + 2), 16));
  return "#" + pa.map((v, i) => Math.round(v + (pb[i] - v) * t).toString(16).padStart(2, "0")).join("");
}

const ANGRY_RED = "#ff3b4e";

/** Emotion scale: 80+ calm, below 80 slightly red, below 60 clearly red (with anger marks). */
export function moodFor(satisfaction: number): Mood {
  if (satisfaction >= 80) return "calm";
  if (satisfaction >= 60) return "annoyed";
  return "angry";
}

function tint(color: string, mood: Mood): string {
  if (mood === "annoyed") return mix(color, ANGRY_RED, 0.3);
  if (mood === "angry") return mix(color, ANGRY_RED, 0.62);
  return color;
}

// ------------------------------------------------------------------ people

type Head = "hair" | "slick" | "toque" | "beret" | "bald";
type Item = "menu" | "tray" | "notepad" | null;

export interface Look {
  head: Head;
  shirt: string;
  /** Centre stripe: shirt front, tie, buttons. */
  accent: string;
  /** Sleeves (defaults to the shirt). */
  sleeve?: string;
  item?: Item;
  neckerchief?: string;
}

const HEADS: Record<Head, string[]> = {
  hair: ["............", "....hhhh....", "...hhhhhh...", "..hhhhhhhh..", "..hssssssh.."],
  slick: ["............", "...hhhhh....", "..hhhhhhhh..", "..hhhhhhhh..", "..hssssssh.."],
  bald: ["............", "............", "....ssss....", "...ssssss...", "..ssssssss.."],
  toque: ["..wwwwwwww..", ".wwwwwwwwww.", ".wwwwwwwwww.", "..wwwwwwww..", "..WWWWWWWW..", "..ssssssss.."],
  beret: ["......R.....", "...RRRRRR...", "..RRRRRRRRR.", ".RRRRRRRRR..", "..hssssssh.."],
};

const MOUTHS: Record<Mood, [string, string]> = {
  calm: ["..ssssssss..", "...smmmms..."],
  annoyed: ["..ssssssss..", "...ssmmss..."],
  angry: ["..ssmmmmss..", "...mssssm..."],
};

const SKIN_TONES = ["#f2c49b", "#d99a6c", "#a8673f", "#7b4a2d", "#e8b48a"];
const HAIR_TONES = ["#2b1d14", "#5a3a22", "#1b1a24", "#8a4b23", "#3a2a40"];

/** Stable per-customer variety (C1 always looks the same). */
function pick<T>(list: T[], id: string, salt: number): T {
  let hash = salt;
  for (const ch of id) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0;
  return list[hash % list.length];
}

export function personSprite(look: Look, mood: Mood = "calm", id = "staff"): Sprite {
  const [m0, m1] = MOUTHS[mood];
  const sleeveRows = look.item === "tray"
    ? [".rbbbaabbggg", ".rbbbaabbbr.", ".sbbbaabbbs."]
    : [".rbbbaabbbr.", ".rbbbaabbbr.", ".sbbbaabbbs."];
  if (look.item === "menu" || look.item === "notepad") {
    sleeveRows[1] = "krbbbaabbbr.";
    sleeveRows[2] = "ksbbbaabbbs.";
  }
  const rows = [
    ...HEADS[look.head],
    "..sesssses..",
    m0,
    m1,
    "....ssss....",
    look.neckerchief ? "..bbnnnnbb.." : "..bbbaabbb..",
    ...sleeveRows,
    "..bbbbbbbb..",
    "..pppppppp..",
    "..ppp..ppp..",
    "..ppp..ppp..",
    ".fff....fff.",
  ];
  const skin = pick(SKIN_TONES, id, 7);
  return {
    rows,
    palette: {
      h: pick(HAIR_TONES, id, 3),
      s: tint(skin, mood),
      e: "var(--outline)",
      m: "var(--mouth)",
      b: look.shirt,
      a: look.accent,
      r: look.sleeve ?? look.shirt,
      n: look.neckerchief ?? look.accent,
      p: "var(--pants)",
      f: "var(--shoes)",
      w: "#f4f4f4",
      W: "#c9cde0",
      R: "var(--critic-beret)",
      g: "var(--tray)",
      k: look.item === "notepad" ? "#fff6d8" : "var(--menu-book)",
    },
  };
}

/** Penguin version: same uniform colour shown as a scarf, same hats. */
export function penguinSprite(look: Look, mood: Mood = "calm"): Sprite {
  const hat = look.head === "toque" ? HEADS.toque.slice(0, 5) : look.head === "beret" ? HEADS.beret.slice(0, 4) : ["............"];
  const rows = [
    ...hat.map((r) => r.replace(/[hs]/g, "o")),
    "....oooo....",
    "...oooooo...",
    "..oowwwwoo..",
    "..owewwewo..",
    "..owwyywwo..",
    "..oowyywoo..",
    "...oaaaao...",
    "..oooaaooo..",
    ".ooowwwwooo.",
    ".oowwwwwwoo.",
    ".oowwwwwwoo.",
    "..owwwwwwo..",
    "..oowwwwoo..",
    "...oooooo...",
    "..yyy..yyy..",
  ];
  return {
    rows,
    palette: {
      o: "var(--penguin-body)",
      w: tint("#f4f4f4", mood),
      e: "var(--outline)",
      y: "var(--penguin-beak)",
      a: look.shirt === "var(--shirt-vip)" || look.shirt === "var(--waiter-vest)" ? look.accent : look.shirt,
      R: "var(--critic-beret)",
      W: "#c9cde0",
    },
  };
}

export function figureSprite(look: Look, skin: Skin, mood: Mood = "calm", id = "staff"): Sprite {
  return skin === "penguins" ? penguinSprite(look, mood) : personSprite(look, mood, id);
}

/** Seated pose: the same figure cut at the lap (legs are hidden under the table). */
export function seatedSprite(sprite: Sprite, skin: Skin): Sprite {
  return { ...sprite, rows: sprite.rows.slice(0, sprite.rows.length - (skin === "penguins" ? 2 : 4)) };
}

export const CUSTOMER_LOOKS: Record<CustomerType, Look> = {
  Regular: { head: "hair", shirt: "var(--shirt-regular)", accent: "var(--shirt-regular)" },
  VIP: { head: "slick", shirt: "var(--shirt-vip)", accent: "var(--shirt-vip-tie)" },
  Critic: { head: "beret", shirt: "var(--shirt-critic)", accent: "#c9cde0", item: "notepad" },
  Impatient: { head: "hair", shirt: "var(--shirt-impatient)", accent: "var(--shirt-impatient)" },
};

export const STAFF_LOOKS: Record<Role | "Cashier", Look> = {
  Host: { head: "slick", shirt: "var(--host-vest)", accent: "var(--host-shirt)", sleeve: "var(--host-shirt)", item: "menu" },
  // The waiter's two menus are drawn as separate pixel cards (one per hand) by WaiterFigure.
  Waiter: { head: "hair", shirt: "var(--waiter-vest)", accent: "var(--waiter-shirt)", sleeve: "var(--waiter-shirt)" },
  Chef: { head: "toque", shirt: "var(--chef-jacket)", accent: "#c9cde0", neckerchief: "var(--chef-scarf)" },
  Manager: { head: "slick", shirt: "var(--manager-suit)", accent: "var(--manager-tie)" },
  Cashier: { head: "hair", shirt: "var(--cashier-shirt)", accent: "#f4f4f4" },
};

// ------------------------------------------------------------------ food

export const FOOD: Record<string, Sprite> = {
  Salad: {
    rows: ["..g.lg..", ".glgglg.", "gglgtglg", "wwwwwwww", ".dwwwwd.", "..dddd.."],
    palette: { g: "#43b65a", l: "#a7f070", t: "#e8364f", w: "#f4f4f4", d: "#c9cde0" },
  },
  Burger: {
    rows: ["..bbbb..", ".bybbyb.", "bbbbbbbb", "gggggggg", "pppppppp", ".bbbbbb."],
    palette: { b: "#e89a3c", y: "#fff1b0", g: "#43b65a", p: "#6b3a22" },
  },
  Pasta: {
    rows: ["..yryy..", ".yyryyy.", "yyyyryyy", "wwwwwwww", ".dwwwwd.", "..dddd.."],
    palette: { y: "#f7c948", r: "#e8364f", w: "#f4f4f4", d: "#c9cde0" },
  },
};

// ------------------------------------------------------------------ scenery

/** Round table with a red/white checkered top (2x2-pixel checks), rim, pedestal and base. */
export const TABLE: Sprite = (() => {
  const width = 26;
  const rows: string[] = [];
  const rx = 12.5;
  const ry = 3.6;
  for (let y = 0; y < 7; y++) {
    let row = "";
    for (let x = 0; x < width; x++) {
      const dx = (x + 0.5 - width / 2) / rx;
      const dy = (y + 0.5 - 3.5) / ry;
      row += dx * dx + dy * dy <= 1 ? ((Math.floor(x / 2) + Math.floor(y / 2)) % 2 === 0 ? "c" : "C") : ".";
    }
    rows.push(row);
  }
  rows.push("....rrrrrrrrrrrrrrrrrr....");
  for (let i = 0; i < 5; i++) rows.push("............ll............");
  rows.push(".........llllllll.........");
  return {
    rows,
    palette: { c: "var(--table-check)", C: "var(--table-check-2)", r: "var(--table-rim)", l: "var(--table-leg)" },
  };
})();

export const LAMP: Sprite = {
  rows: ["...kk...", "...kk...", "...kk...", "..rrrr..", ".rrrrrr.", "rrrrrrrr", "..yyyy.."],
  palette: { k: "#55507a", r: "var(--lamp)", y: "#ffe9a8" },
};

export const STOVE: Sprite = {
  rows: [
    "..k..........k....",
    "kkkkkkkkkkkkkkkkkk",
    "kggggggggggggggggk",
    "kgddddddgdddddddgk",
    "kgdoodddgdddoodgk.",
    "kgddddddgdddddddgk",
    "kggggggggggggggggk",
    "kgyyyyggggggyyyygk",
    "kggggggggggggggggk",
    "kkkkkkkkkkkkkkkkkk",
  ].map((r) => r.padEnd(18, ".")),
  palette: { k: "#3a3a52", g: "#8c90ad", d: "#5d6180", o: "#ff8a2a", y: "#f7c948" },
};

export const DOOR: Sprite = {
  rows: [
    "dddddddd",
    "dbbbbbbd",
    "dbwwwwbd",
    "dbwwwwbd",
    "dbbbbbbd",
    "dbbbbbbd",
    "dbbbbbbd",
    "dbbbbbyd",
    "dbbbbbbd",
    "dbbbbbbd",
    "dbbbbbbd",
    "dbbbbbbd",
    "dbbbbbbd",
    "dddddddd",
  ],
  palette: { d: "var(--door-dark)", b: "var(--door)", w: "#9fd6ff", y: "#f7c948" },
};

/** Anime-style anger vein. */
export const ANGER: Sprite = {
  rows: [".r.r.", "rr.rr", ".....", "rr.rr", ".r.r."],
  palette: { r: "var(--anger)" },
};

/** Chatty customer's phone. */
export const PHONE: Sprite = {
  rows: ["kkk", "kck", "kck", "kkk"],
  palette: { k: "#1a1c2c", c: "#7fd8ff" },
};

/** Little sparkle beside a tip. */
export const SPARKLE: Sprite = {
  rows: ["..y..", ".yyy.", "yyyyy", ".yyy.", "..y.."],
  palette: { y: "var(--tip-ink)" },
};

export const COIN: Sprite = {
  rows: [".yyyy.", "yyooyy", "yoyyoy", "yoyyoy", "yyooyy", ".yyyy."],
  palette: { y: "#f7c948", o: "#c98a1a" },
};

export const REGISTER: Sprite = {
  rows: ["..gggggg..", "..gddddg..", "kkkkkkkkkk", "kyyyyyyyyk", "kyykyykyyk", "kkkkkkkkkk"],
  palette: { g: "#43b65a", d: "#1d3a24", k: "#5d6180", y: "#c9cde0" },
};
