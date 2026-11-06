/**
 * Themed props. Each prop has ONE grid size for every theme, so a theme can change colours and patterns but
 * never a prop's footprint, position, or the walking paths around it. Colours are CSS variables from
 * theme/theme.css; patterns (wood grain, glass sheen, red runner, shoji grid) are chosen per theme here.
 */
import type { ThemeId } from "@/theme/themes";

import type { Sprite } from "./sprites";

/** Builds a grid of `width` x `height` from a per-pixel function ("." = transparent). */
function grid(width: number, height: number, at: (x: number, y: number) => string): string[] {
  const rows: string[] = [];
  for (let y = 0; y < height; y++) {
    let row = "";
    for (let x = 0; x < width; x++) row += at(x, y);
    rows.push(row);
  }
  return rows;
}

// ------------------------------------------------------------------ tables (26 x 14 for every theme)

export function tableSprite(theme: ThemeId): Sprite {
  const width = 26;
  const inTop = (x: number, y: number) => {
    if (y < 0 || y > 6 || x < 0 || x >= width) return false;
    if (theme === "tokyo") {
      // low rectangular table with clipped corners
      if (y === 0 || y === 6) return x >= 2 && x <= 23;
      return x >= 1 && x <= 24;
    }
    const dx = (x + 0.5 - width / 2) / 12.5;
    const dy = (y + 0.5 - 3.5) / 3.6;
    return dx * dx + dy * dy <= 1;
  };
  const top = grid(width, 7, (x, y) => {
    if (!inTop(x, y)) return ".";
    const edge = !inTop(x - 1, y) || !inTop(x + 1, y) || !inTop(x, y - 1) || !inTop(x, y + 1);
    if (edge) return "r";
    if (theme === "neon") return (x - 2 * y + 40) % 13 < 2 ? "C" : "c"; // glass sheen
    if (theme === "cafe") return y % 2 === 0 ? "c" : (x + y) % 7 === 0 ? "g" : "C"; // wood grain
    // tokyo: dark wood with a red runner and gold edging
    if (x === 9 || x === 16) return "g";
    if (x > 9 && x < 16) return "C";
    return "c";
  });
  const rim = theme === "tokyo" ? ".rrrrrrrrrrrrrrrrrrrrrrrr." : "....rrrrrrrrrrrrrrrrrr....";
  const legs = theme === "tokyo"
    ? Array(5).fill("...ll..................ll...".slice(1, 27))
    : Array(5).fill("............ll............");
  const base = theme === "tokyo" ? "..llll..............llll.." : ".........llllllll.........";
  return {
    rows: [...top, rim, ...legs, base],
    palette: {
      c: "var(--table-top)",
      C: "var(--table-top-2)",
      r: "var(--table-rim)",
      g: "var(--table-gold)",
      l: "var(--table-leg)",
    },
  };
}

// ------------------------------------------------------------------ chairs (16 x 14)

export function chairSprite(theme: ThemeId): Sprite {
  const back =
    theme === "cafe"
      ? [".ffffffffffffff.", ".fsfsfsfsfsfsff.", ".fsfsfsfsfsfsff.", ".fsfsfsfsfsfsff.", ".fsfsfsfsfsfsff.", ".fsfsfsfsfsfsff.", ".fsfsfsfsfsfsff.", ".ffffffffffffff."]
      : theme === "tokyo"
        ? [".aaaaaaaaaaaaaa.", "..f..........f..", "..f..........f..", "..ffffffffffff..", "..f..........f..", "..f..........f..", "..f..........f..", "..ffffffffffff.."]
        : ["..ffffffffffff..", ".fAAAAAAAAAAAAf.", ".fssssssssssssf.", ".fssssssssssssf.", ".fssssssssssssf.", ".fssssssssssssf.", ".fAAAAAAAAAAAAf.", ".ffffffffffffff."];
  return {
    rows: [...back, "ssssssssssssssss", "ffffffffffffffff", ".f............f.", ".f............f.", ".f............f.", ".f............f."],
    palette: { f: "var(--chair-frame)", s: "var(--chair-seat)", a: "var(--chair-accent)", A: "var(--chair-accent)" },
  };
}

// ------------------------------------------------------------------ hanging lamp over each table (8 x 7)

export function lampSprite(theme: ThemeId): Sprite {
  if (theme === "tokyo") {
    return {
      rows: ["...kk...", "..gggg..", ".rrrrrr.", "rrwrrwrr", ".rrrrrr.", "..gggg..", "...yy..."],
      palette: { k: "var(--lamp-cord)", g: "#e6b422", r: "var(--lamp)", w: "#ff9a7a", y: "#e6b422" },
    };
  }
  if (theme === "cafe") {
    return {
      rows: ["...kk...", "...kk...", "..rrrr..", ".rrrrrr.", "rrrrrrrr", "rrrrrrrr", "..yyyy.."],
      palette: { k: "var(--lamp-cord)", r: "var(--lamp)", y: "var(--lamp-light)" },
    };
  }
  return {
    rows: ["...kk...", "...kk...", "...kk...", "..rrrr..", ".rrrrrr.", "rrrrrrrr", "..yyyy.."],
    palette: { k: "var(--lamp-cord)", r: "var(--lamp)", y: "var(--lamp-light)" },
  };
}

// ------------------------------------------------------------------ door (8 x 14)

export function doorSprite(theme: ThemeId): Sprite {
  if (theme === "tokyo") {
    // sliding shoji: dark wood frame with paper panels
    return {
      rows: grid(8, 14, (x, y) => (x === 0 || x === 7 || y === 0 || y === 13 || x === 4 || y % 3 === 0 ? "d" : "p")),
      palette: { d: "var(--door-dark)", p: "var(--door)" },
    };
  }
  return {
    rows: ["dddddddd", "dbbbbbbd", "dbwwwwbd", "dbwwwwbd", "dbbbbbbd", "dbbbbbbd", "dbbbbbbd", "dbbbbbyd", "dbbbbbbd", "dbbbbbbd", "dbbbbbbd", "dbbbbbbd", "dbbbbbbd", "dddddddd"],
    palette: { d: "var(--door-dark)", b: "var(--door)", w: "var(--door-window)", y: "var(--door-handle)" },
  };
}

export const STOVE_THEMED: Sprite = {
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
  palette: { k: "var(--stove-frame)", g: "var(--stove-body)", d: "var(--stove-inner)", o: "var(--burner)", y: "#f7c948" },
};

// ------------------------------------------------------------------ host pedestal (14 x 9) and accessory
// 9 rows tall (was 12) so the host's upper body shows clearly; same width and bottom anchor.

export function pedestalSprite(theme: ThemeId): Sprite {
  const body = (x: number) => (theme === "cafe" ? (x % 3 === 0 ? "B" : "b") : "b");
  const rows = grid(14, 9, (x, y) => {
    if (y === 0) return "t";
    if (y === 1) return "T";
    if (y === 7) return theme === "tokyo" ? "g" : "b";
    if (y === 8) return "r";
    if (x === 0 || x === 13) return ".";
    if (theme === "tokyo" && (x === 1 || x === 12)) return "g";
    if (y === 3 || y === 6) return "r";
    return body(x);
  });
  return {
    rows,
    palette: {
      t: "var(--pedestal-top)",
      T: "var(--pedestal-shade)",
      b: "var(--pedestal-body)",
      B: "var(--pedestal-shade)",
      r: "var(--pedestal-trim)",
      g: "var(--table-gold)",
    },
  };
}

/** Small item on top of the pedestal: glowing reservation book, brass lamp + plant, or a paper lantern. */
export function pedestalAccessory(theme: ThemeId): Sprite {
  if (theme === "cafe") {
    return {
      rows: [".yyy......", "yyyyy..gg.", ".yyy..gggg", "..k...gggg", "..k....gg.", ".kkk..pppp", ".kkk..pppp"],
      palette: { y: "#ffe2a0", k: "#c9963f", g: "#5aa04a", p: "#b0623a" },
    };
  }
  if (theme === "tokyo") {
    return {
      rows: ["....k.....", "...ggg....", "..rrrrr...", "..rwrwr...", "..rrrrr...", "...ggg..mm", "........mm"],
      palette: { k: "#24150f", g: "#e6b422", r: "#e8473d", w: "#ffb08a", m: "#f7ead2" },
    };
  }
  return {
    rows: ["..........", "..........", "..cccccc..", ".cwwccwwc.", ".cwwccwwc.", "..cccccc..", ".........."],
    palette: { c: "#6ee7ff", w: "#d8fbff" },
  };
}

// ------------------------------------------------------------------ cashier booth (32 x 12) and its items
// 12 rows tall (was 16) so the cashier's upper body shows clearly; same width and position.

export function boothSprite(theme: ThemeId): Sprite {
  const rows = grid(32, 12, (x, y) => {
    if (y <= 1) return "t";
    if (y === 2) return "s";
    if (theme === "neon") {
      if (y === 3 || y === 11) return "n";
      if (x === 0 || x === 31) return "m";
      if (y === 7) return "m";
      return "b";
    }
    if (theme === "cafe") {
      if (y === 11) return "r";
      return x % 5 === 0 ? "s" : "b";
    }
    // tokyo
    if (y === 3 || y === 11) return "n";
    if (y === 6 || y === 7) return "m";
    if (x === 0 || x === 31) return "n";
    return "b";
  });
  return {
    rows,
    palette: {
      t: "var(--booth-top)",
      s: "var(--booth-shade)",
      b: "var(--booth-body)",
      r: "var(--booth-trim)",
      n: "var(--booth-trim)",
      m: "var(--booth-trim-2)",
    },
  };
}

/** The item on the booth counter: screen register, brass bell, or a gold lucky cat. */
export function boothItem(theme: ThemeId): Sprite {
  if (theme === "cafe") {
    return {
      rows: ["....y....", "...yyy...", "..yyyyy..", ".yyyyyyy.", "kkkkkkkkk"],
      palette: { y: "#e8c06a", k: "#8a5a32" },
    };
  }
  if (theme === "tokyo") {
    // maneki-neko
    return {
      rows: ["y.....y..", "yy...yy.y", "yyyyyyy.y", "yeyyyey.y", "yyyryyyyy", ".yyyyyyy.", ".yyyyyyy.", "yyyyyyyyy"],
      palette: { y: "#e6b422", e: "#24150f", r: "#c8283f" },
    };
  }
  return {
    rows: ["kkkkkkkkk", "kccccccck", "kcwcwcwck", "kccccccck", "kkkkkkkkk", "...kkk...", ".kkkkkkk."],
    palette: { k: "#2b3666", c: "#6ee7ff", w: "#0e3a4a" },
  };
}

export const NOREN: Sprite = {
  rows: grid(30, 10, (x, y) => {
    if (y === 0) return "k";
    if (x % 10 === 9) return ".";
    const cx = x % 10;
    if (y >= 3 && y <= 6 && cx >= 3 && cx <= 5) return "w";
    return "r";
  }),
  palette: { k: "#3a2418", r: "#b3202f", w: "#f7ead2" },
};

export const PLANT: Sprite = {
  rows: ["...g.g....", "..ggggg...", ".gggGggg..", "ggGgggGgg.", ".ggggggg..", "..ggGgg...", "...ppp....", "..ppppp...", "..ppppp...", "...ppp...."],
  palette: { g: "#5aa04a", G: "#7cc45f", p: "#b0623a" },
};

// ------------------------------------------------------------------ decorations

/** A big decoration for the free bottom-left corner of the floor. */
export function cornerDecor(theme: ThemeId): Sprite {
  if (theme === "cafe") {
    return {
      rows: [
        "....g..g.....",
        "..gggggggg...",
        ".ggGgggGggg..",
        "gggggGgggggg.",
        ".gGgggggGgg..",
        "..ggggGggg...",
        "...gggggg....",
        ".....kk......",
        "...pppppp....",
        "..pppppppp...",
        "..pppppppp...",
        "...pppppp....",
      ],
      palette: { g: "#4f9443", G: "#7cc45f", k: "#6e4424", p: "#c2703f" },
    };
  }
  if (theme === "tokyo") {
    // bonsai on a low stand
    return {
      rows: [
        "...ggg..gg...",
        "..ggggggggg..",
        ".gggGgg.gggg.",
        "..gg..kk..gg.",
        ".......k.....",
        "......kk.....",
        "....rrrrrr...",
        "...rrrrrrrr..",
        "..kkkkkkkkkk.",
        "..k........k.",
        "..k........k.",
        "..k........k.",
      ],
      palette: { g: "#3f7d3a", G: "#62a552", k: "#24150f", r: "#b3202f" },
    };
  }
  // arcade cabinet
  return {
    rows: [
      ".mmmmmmmmmmm.",
      "mmcccccccccmm",
      "mkkkkkkkkkkkm",
      "mksswssswsskm",
      "mkssssssssskm",
      "mkswsssswsskm",
      "mkkkkkkkkkkkm",
      "mmmmmmmmmmmmm",
      "mbbbyrbbbbbbm",
      "mbbbbbbbbbbbm",
      "mbbbbbbbbbbbm",
      "mmmmmmmmmmmmm",
    ],
    palette: { m: "#2b1f55", c: "#ff4fd8", k: "#120e26", s: "#1c4a63", w: "#6ee7ff", b: "#3a2266", y: "#f7c948", r: "#ff5d73" },
  };
}

/** Small decoration repeated beside the sign (lantern, hanging plant, neon star). */
export function signDecor(theme: ThemeId): Sprite {
  if (theme === "tokyo") return lampSprite("tokyo");
  if (theme === "cafe") {
    return {
      rows: ["....k....", "....k....", "..ppppp..", "..ppppp..", ".gggggg..", "gg.gg.gg.", "g..g..g..", "...g....."],
      palette: { k: "#4a3020", p: "#c2703f", g: "#5aa04a" },
    };
  }
  return {
    rows: ["....c....", "...ccc...", "ccccccccc", ".ccccccc.", "..ccccc..", ".cc...cc.", "c.......c"],
    palette: { c: "#6ee7ff" },
  };
}

/** Kitchen wall decoration (top right): neon sign, shelf of jars, or wooden menu plaques. */
export function kitchenDecor(theme: ThemeId): Sprite {
  if (theme === "cafe") {
    return {
      rows: ["..yy..rr..gg..yy..", ".yyyy.rr.gggg.yyyy", ".yyyy.rr.gggg.yyyy", ".yyyy.rr.gggg.yyyy", "kkkkkkkkkkkkkkkkkk", ".k..............k."],
      palette: { y: "#e8c06a", r: "#c2703f", g: "#7cc45f", k: "#7f5230" },
    };
  }
  if (theme === "tokyo") {
    return {
      rows: ["kkkkkkkkkkkkkkkkkk", "..w....w....w.....", ".www..www..www....", ".wdw..wdw..wdw....", ".www..www..www....", ".wdw..wdw..wdw....", ".www..www..www...."],
      palette: { k: "#24150f", w: "#efe3c6", d: "#24150f" },
    };
  }
  return {
    rows: ["mmmmmmmmmmmmmmmmmm", "m................m", "m.cc.c..c.ccc.cc.m", "m.c..cc.c..c..c..m", "m.cc.c.cc..c..cc.m", "m................m", "mmmmmmmmmmmmmmmmmm"],
    palette: { m: "#ff4fd8", c: "#6ee7ff" },
  };
}
