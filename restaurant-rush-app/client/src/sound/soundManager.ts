/**
 * Sound effects. Every sound is a short chiptune effect synthesised with the Web Audio API, so the game has
 * sound with no audio files. To use a recorded sound instead, put a file in client/public/sounds/ and add it
 * to CUSTOM_FILES below (e.g. serve: "/sounds/serve.mp3").
 */
export type SoundName =
  | "click"
  | "select"
  | "back"
  | "seat"
  | "order"
  | "cook"
  | "serve"
  | "pay"
  | "arrive"
  | "anger"
  | "leave"
  | "happyHour"
  | "nextTurn"
  | "error"
  | "victory"
  | "gameOver";

/** Optional recorded sounds that replace the synthesised ones. */
const CUSTOM_FILES: Partial<Record<SoundName, string>> = {};

type Wave = OscillatorType;
/** One note: frequency (Hz), start offset and length (s), waveform, volume. */
interface Note {
  f: number;
  at: number;
  len: number;
  wave?: Wave;
  vol?: number;
  slideTo?: number;
}

const N = { C4: 262, E4: 330, G4: 392, A4: 440, C5: 523, D5: 587, E5: 659, G5: 784, A5: 880, B5: 988, C6: 1047, E6: 1319 };

/** Recipes: each sound is a few square/triangle notes, like an old console. */
const RECIPES: Record<SoundName, { notes: Note[]; noise?: { at: number; len: number; vol: number } }> = {
  click: { notes: [{ f: N.A5, at: 0, len: 0.04, wave: "square", vol: 0.25 }] },
  select: { notes: [{ f: N.E5, at: 0, len: 0.05, wave: "square", vol: 0.25 }, { f: N.A5, at: 0.05, len: 0.06, wave: "square", vol: 0.25 }] },
  back: { notes: [{ f: N.A5, at: 0, len: 0.05, wave: "square", vol: 0.2 }, { f: N.E5, at: 0.05, len: 0.06, wave: "square", vol: 0.2 }] },
  seat: { notes: [{ f: N.C5, at: 0, len: 0.08, wave: "square" }, { f: N.E5, at: 0.08, len: 0.08, wave: "square" }, { f: N.G5, at: 0.16, len: 0.12, wave: "square" }] },
  order: {
    notes: [{ f: N.G5, at: 0, len: 0.05, wave: "square" }, { f: N.G5, at: 0.09, len: 0.05, wave: "square" }],
    noise: { at: 0.15, len: 0.12, vol: 0.12 },
  },
  cook: { notes: [{ f: 140, at: 0, len: 0.25, wave: "sawtooth", vol: 0.12, slideTo: 90 }], noise: { at: 0, len: 0.35, vol: 0.18 } },
  serve: { notes: [{ f: N.E6, at: 0, len: 0.35, wave: "triangle", vol: 0.5 }, { f: N.B5, at: 0.06, len: 0.3, wave: "triangle", vol: 0.3 }] },
  pay: { notes: [{ f: N.B5, at: 0, len: 0.07, wave: "square" }, { f: N.E6, at: 0.07, len: 0.25, wave: "square" }] },
  arrive: { notes: [{ f: N.E5, at: 0, len: 0.18, wave: "triangle", vol: 0.45 }, { f: N.C5, at: 0.18, len: 0.3, wave: "triangle", vol: 0.45 }] },
  anger: { notes: [{ f: 220, at: 0, len: 0.12, wave: "square", vol: 0.2 }, { f: 196, at: 0.13, len: 0.2, wave: "square", vol: 0.2 }] },
  leave: { notes: [{ f: N.G4, at: 0, len: 0.15, wave: "square" }, { f: N.E4, at: 0.15, len: 0.15, wave: "square" }, { f: N.C4, at: 0.3, len: 0.35, wave: "square", slideTo: 200 }] },
  happyHour: {
    notes: [N.C5, N.E5, N.G5, N.C6, N.G5, N.C6].map((f, i) => ({ f, at: i * 0.07, len: 0.08, wave: "square" as Wave, vol: 0.25 })),
  },
  nextTurn: { notes: [{ f: N.C5, at: 0, len: 0.05, wave: "triangle", vol: 0.35 }, { f: N.G5, at: 0.05, len: 0.08, wave: "triangle", vol: 0.35 }] },
  error: { notes: [{ f: 150, at: 0, len: 0.12, wave: "square", vol: 0.2 }, { f: 150, at: 0.15, len: 0.12, wave: "square", vol: 0.2 }] },
  victory: {
    notes: [
      { f: N.C5, at: 0, len: 0.12 }, { f: N.E5, at: 0.12, len: 0.12 }, { f: N.G5, at: 0.24, len: 0.12 },
      { f: N.C6, at: 0.36, len: 0.3 }, { f: N.A5, at: 0.68, len: 0.12 }, { f: N.C6, at: 0.8, len: 0.5 },
    ].map((n) => ({ ...n, wave: "square" as Wave, vol: 0.28 })),
  },
  gameOver: {
    notes: [
      { f: N.G4, at: 0, len: 0.2 }, { f: 370, at: 0.22, len: 0.2 }, { f: 349, at: 0.44, len: 0.2 }, { f: N.E4, at: 0.66, len: 0.6, slideTo: 250 },
    ].map((n) => ({ ...n, wave: "square" as Wave, vol: 0.25 })),
  },
};

class SoundManager {
  private muted = false;
  private volume = 0.5;
  private context: AudioContext | null = null;

  /** Plays a sound effect. Never throws: sound is a nice-to-have. */
  play(name: SoundName): void {
    if (this.muted) return;
    try {
      const file = CUSTOM_FILES[name];
      if (file) {
        const audio = new Audio(file);
        audio.volume = this.volume;
        audio.play().catch(() => this.synth(name));
        return;
      }
      this.synth(name);
    } catch {
      // no audio available (old browser, blocked) - play silently
    }
  }

  private synth(name: SoundName): void {
    const ctx = this.audio();
    if (!ctx) return;
    const now = ctx.currentTime + 0.01;
    const master = ctx.createGain();
    master.gain.value = this.volume * 0.5;
    master.connect(ctx.destination);
    const recipe = RECIPES[name];
    for (const note of recipe.notes) {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = note.wave ?? "square";
      osc.frequency.setValueAtTime(note.f, now + note.at);
      if (note.slideTo) osc.frequency.exponentialRampToValueAtTime(note.slideTo, now + note.at + note.len);
      const vol = note.vol ?? 0.3;
      gain.gain.setValueAtTime(0.0001, now + note.at);
      gain.gain.exponentialRampToValueAtTime(vol, now + note.at + 0.01);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + note.at + note.len);
      osc.connect(gain).connect(master);
      osc.start(now + note.at);
      osc.stop(now + note.at + note.len + 0.02);
    }
    if (recipe.noise) {
      const { at, len, vol } = recipe.noise;
      const buffer = ctx.createBuffer(1, Math.ceil(ctx.sampleRate * len), ctx.sampleRate);
      const data = buffer.getChannelData(0);
      for (let i = 0; i < data.length; i++) data[i] = (Math.random() * 2 - 1) * (1 - i / data.length);
      const source = ctx.createBufferSource();
      const gain = ctx.createGain();
      gain.gain.value = vol;
      source.buffer = buffer;
      source.connect(gain).connect(master);
      source.start(now + at);
    }
  }

  private audio(): AudioContext | null {
    if (!this.context) {
      const Ctx = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!Ctx) return null;
      this.context = new Ctx();
    }
    if (this.context.state === "suspended") void this.context.resume();
    return this.context;
  }

  setMuted(muted: boolean): void {
    this.muted = muted;
  }

  isMuted(): boolean {
    return this.muted;
  }

  setVolume(volume: number): void {
    this.volume = Math.max(0, Math.min(1, volume));
  }
}

export const sounds = new SoundManager();
