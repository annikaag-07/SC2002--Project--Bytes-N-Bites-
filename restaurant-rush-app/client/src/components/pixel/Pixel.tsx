/** Pixel sprite renderer and the small pixel widgets built on it. */
import type { CSSProperties } from "react";

import type { Customer } from "@/api/types";

import { ANGER, CUSTOMER_LOOKS, FOOD, PHONE, STAFF_LOOKS, figureSprite, moodFor, seatedSprite, type Skin, type Sprite } from "./sprites";

/** Draws a sprite as crisp SVG rectangles, one per run of same-coloured pixels. */
export function PixelSprite({
  sprite,
  scale = 3,
  className,
  title,
  style,
}: {
  sprite: Sprite;
  scale?: number;
  className?: string;
  title?: string;
  style?: CSSProperties;
}) {
  const width = Math.max(...sprite.rows.map((r) => r.length));
  const height = sprite.rows.length;
  const rects: React.ReactElement[] = [];
  sprite.rows.forEach((row, y) => {
    let x = 0;
    while (x < row.length) {
      const key = row[x];
      let run = 1;
      while (x + run < row.length && row[x + run] === key) run++;
      const fill = sprite.palette[key];
      if (key !== "." && fill) rects.push(<rect key={`${x}-${y}`} x={x} y={y} width={run} height={1} fill={fill} />);
      x += run;
    }
  });
  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      width={width * scale}
      height={height * scale}
      className={`pixel ${className ?? ""}`}
      style={style}
      role={title ? "img" : undefined}
      aria-label={title}
      aria-hidden={title ? undefined : true}
      shapeRendering="crispEdges"
    >
      {title && <title>{title}</title>}
      {rects}
    </svg>
  );
}

/** Food icons for an order (a combo shows both of its dishes). */
export function Food({ names, scale = 3 }: { names: readonly string[]; scale?: number }) {
  return (
    <span className="food">
      {names.map((name, i) => (
        <PixelSprite key={`${name}-${i}`} sprite={FOOD[name] ?? FOOD.Salad} scale={scale} title={name} />
      ))}
    </span>
  );
}

/** Satisfaction badge: id, bar and number (bar colour follows the emotion scale). */
export function SatisfactionBadge({ customer }: { customer: Customer }) {
  const value = customer.state === "PAID" ? customer.recordedSatisfaction : customer.satisfaction;
  return (
    <span className={`badge mood-${moodFor(value)}`} aria-label={`${customer.id} satisfaction ${value}`}>
      <strong>{customer.id}</strong>
      <span className="bar">
        <span style={{ width: `${value}%` }} />
      </span>
      <span>{value}</span>
    </span>
  );
}

/** A customer sprite, tinted red as satisfaction drops, with anger marks below 60. */
export function CustomerFigure({
  customer,
  skin,
  scale = 3,
  seated = false,
}: {
  customer: Customer;
  skin: Skin;
  scale?: number;
  /** Sitting at a table: drawn without legs, which the table covers. */
  seated?: boolean;
}) {
  const mood = moodFor(customer.satisfaction);
  const standing = figureSprite(CUSTOMER_LOOKS[customer.type], skin, mood, customer.id);
  // Sub-type aura: a crisp coloured outline around the sprite (same colours in every theme).
  const aura = customer.subType && customer.subType !== "normal" ? `aura aura-${customer.subType}` : "";
  return (
    <span className={`figure customer-figure mood-${mood} ${aura}`} data-type={customer.type} data-subtype={customer.subType ?? undefined}>
      {mood === "angry" && (
        <span className="anger" aria-hidden>
          <PixelSprite sprite={ANGER} scale={3} />
          <span>!!</span>
        </span>
      )}
      <PixelSprite
        sprite={seated ? seatedSprite(standing, skin) : standing}
        scale={scale}
        title={`${customer.id} (${customer.type})`}
        key={customer.state}
        className="bounce-in"
      />
      {customer.subType === "chatty" && (
        <>
          <span className="phone" style={{ bottom: (seated ? 6 : 7) * scale, right: -scale }} aria-hidden>
            <PixelSprite sprite={PHONE} scale={scale} />
          </span>
          {seated && <span className="typing" aria-hidden><i /><i /><i /></span>}
        </>
      )}
      {customer.type === "Impatient" && customer.state !== "EATING" && (
        <span className="tap" aria-hidden>
          tap tap
        </span>
      )}
    </span>
  );
}

/** One "MENUS" card (pixel style, theme colours). `lying`: flat on a table. */
export function MenuCard({ lying = false }: { lying?: boolean }) {
  return (
    <span className={`menu-card ${lying ? "menu-lying" : ""}`} aria-hidden>
      MENUS
    </span>
  );
}

/** The waiter, holding up to two menus (one in each hand). */
export function WaiterFigure({ skin, menus }: { skin: Skin; menus: number }) {
  return (
    <span className="waiter-figure">
      {menus >= 2 && (
        <span className="hand-menu hand-left">
          <MenuCard />
        </span>
      )}
      <PixelSprite sprite={figureSprite(STAFF_LOOKS.Waiter, skin)} scale={3} title="Waiter" />
      {menus >= 1 && (
        <span className="hand-menu hand-right">
          <MenuCard />
        </span>
      )}
    </span>
  );
}
