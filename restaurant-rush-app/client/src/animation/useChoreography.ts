/**
 * Plays a movement plan over the restaurant view. Measures the named anchors in the already-updated scene,
 * then tweens "ghost" sprites between them with requestAnimationFrame. While a ghost stands in for a scene
 * element (a staff member, a seated customer, a plate), that element is hidden; it is revealed again when
 * the ghost arrives, so every character ends exactly at its normal place.
 */
import { useLayoutEffect, useRef, useState, type RefObject } from "react";

import type { AnchorRef, Plan } from "./choreography";

export interface GhostView {
  id: string;
  x: number;
  y: number;
  facing: 1 | -1;
  walking: boolean;
  cooking: boolean;
  carry: string[] | null;
  bubble: string | null;
  /** Waiter only: menus in hand (null for everyone else). */
  menus: number | null;
}

export interface FloatView {
  key: string;
  x: number;
  y: number;
  text: string;
  kind?: "tip" | "ugh";
}

export interface ChoreographyView {
  planId: number;
  ghosts: GhostView[];
  hidden: ReadonlySet<string>;
  /** Elements the plan keeps visible although the new state no longer has them. */
  shown: ReadonlySet<string>;
  floats: FloatView[];
}

const EMPTY: ChoreographyView = { planId: 0, ghosts: [], hidden: new Set(), shown: new Set(), floats: [] };

/** Feet position (bottom centre) of every [data-anchor] element, relative to the container. */
function measure(container: HTMLElement): Map<string, { x: number; y: number }> {
  const box = container.getBoundingClientRect();
  const points = new Map<string, { x: number; y: number }>();
  container.querySelectorAll<HTMLElement>("[data-anchor]").forEach((el) => {
    const r = el.getBoundingClientRect();
    points.set(el.dataset.anchor!, { x: r.left + r.width / 2 - box.left, y: r.bottom - box.top });
  });
  return points;
}

/**
 * Next frame: requestAnimationFrame normally, a short timer when the page is hidden (browsers pause
 * requestAnimationFrame in background tabs, which would otherwise freeze a sequence mid-way).
 */
function nextFrame(fn: (now: number) => void): () => void {
  if (document.hidden) {
    const t = setTimeout(() => fn(performance.now()), 33);
    return () => clearTimeout(t);
  }
  const f = requestAnimationFrame(fn);
  return () => cancelAnimationFrame(f);
}

const ease = (t: number) => (t < 0.5 ? 2 * t * t : 1 - (-2 * t + 2) ** 2 / 2);

export function useChoreography(
  plan: Plan | null,
  container: RefObject<HTMLElement | null>,
  speed: number,
  onDone: (id: number) => void,
): ChoreographyView {
  const [view, setView] = useState<ChoreographyView>(EMPTY);
  const settings = useRef({ speed, onDone });
  useLayoutEffect(() => {
    settings.current = { speed, onDone };
  });

  useLayoutEffect(() => {
    const root = container.current;
    if (!plan || !root) return;
    const anchors = measure(root);
    const point = (ref: AnchorRef) => {
      const a = anchors.get(ref.name) ?? { x: 0, y: 0 };
      return { x: a.x + (ref.dx ?? 0), y: a.y + (ref.dy ?? 0) };
    };
    const ghosts = new Map<string, GhostView & { visible: boolean }>();
    for (const actor of plan.actors) {
      const p = point(actor.start);
      ghosts.set(actor.id, {
        id: actor.id,
        ...p,
        facing: 1,
        walking: false,
        cooking: false,
        carry: null,
        bubble: null,
        menus: actor.menus ?? null,
        visible: !actor.startHidden,
      });
    }
    const hidden = new Set(plan.hidden);
    const shown = new Set(plan.shown ?? []);
    let floats: FloatView[] = [];
    let index = 0;
    let segmentStart = performance.now();
    let from = new Map<string, { x: number; y: number }>();
    let cancelFrame = () => {};
    let done = false;

    const begin = (i: number, now: number) => {
      const s = plan.segments[i];
      segmentStart = now;
      from = new Map([...ghosts].map(([k, g]) => [k, { x: g.x, y: g.y }]));
      s.appear?.forEach((k) => {
        const g = ghosts.get(k);
        if (g) g.visible = true;
      });
      for (const [k, v] of Object.entries(s.carry ?? {})) {
        const g = ghosts.get(k);
        if (g) g.carry = v;
      }
      for (const [k, v] of Object.entries(s.bubble ?? {})) {
        const g = ghosts.get(k);
        if (g) g.bubble = v;
      }
      for (const [k, v] of Object.entries(s.menus ?? {})) {
        const g = ghosts.get(k);
        if (g) g.menus = v;
      }
      ghosts.forEach((g) => (g.cooking = s.cooking?.includes(g.id) ?? false));
      floats = (s.float ?? []).map((f, n) => ({ key: `${plan.id}-${i}-${n}`, ...point(f.at), text: f.text, kind: f.kind }));
    };

    const publish = () =>
      setView({
        planId: plan.id,
        ghosts: [...ghosts.values()].filter((g) => g.visible).map(({ visible: _visible, ...g }) => g),
        hidden: new Set(hidden),
        shown: new Set(shown),
        floats,
      });

    const tick = (now: number) => {
      if (done) return;
      const s = plan.segments[index];
      const length = s.ms * settings.current.speed;
      const t = length <= 0 ? 1 : Math.min(1, (now - segmentStart) / length);
      for (const move of s.moves) {
        const g = ghosts.get(move.actor);
        const f = from.get(move.actor);
        if (!g || !f) continue;
        const target = point(move.to);
        const k = ease(t);
        g.x = f.x + (target.x - f.x) * k;
        g.y = f.y + (target.y - f.y) * k;
        const distance = Math.hypot(target.x - f.x, target.y - f.y);
        g.walking = t < 1 && distance > 2;
        if (target.x < f.x - 1) g.facing = -1;
        else if (target.x > f.x + 1) g.facing = 1;
      }
      if (t >= 1) {
        s.reveal?.forEach((k) => hidden.delete(k));
        s.unshow?.forEach((k) => shown.delete(k));
        s.vanish?.forEach((k) => {
          const g = ghosts.get(k);
          if (g) g.visible = false;
        });
        ghosts.forEach((g) => (g.walking = false));
        index++;
        if (index >= plan.segments.length) {
          done = true;
          setView(EMPTY);
          settings.current.onDone(plan.id);
          return;
        }
        begin(index, now);
      }
      publish();
      cancelFrame = nextFrame(tick);
    };

    begin(0, performance.now());
    cancelFrame = nextFrame(tick);
    // If the view goes away mid-sequence the store's safety timer releases the input lock.
    return () => {
      done = true;
      cancelFrame();
    };
  }, [plan, container]);

  if (!plan) return EMPTY;
  // Before the first frame of a new plan, hide its elements straight away so nothing flashes in place.
  if (view.planId !== plan.id) {
    return { planId: plan.id, ghosts: [], hidden: new Set(plan.hidden), shown: new Set(plan.shown ?? []), floats: [] };
  }
  return view;
}
