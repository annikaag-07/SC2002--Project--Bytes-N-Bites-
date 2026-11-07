/**
 * The restaurant scene, drawn from the current state. Positions of the door, host spot, tables (T1/T2
 * centres), waiter spot and kitchen are fixed by CSS and do not depend on the theme.
 *
 * Movement: when the store has an animation plan, useChoreography walks "ghost" sprites between the
 * [data-anchor] waypoints below and hides the scene element each ghost stands in for (keys such as
 * "staff-Waiter", "seat-T1", "plate-T1", "tag-T1", "ready-O3"). The game state itself is already final; the
 * animation only replays it.
 *
 * What is drawn follows the mode's features from the server: no Host, pedestal or Host name tag in Stage 1 and
 * Stage 2 (their space stays reserved and invisible, so the layout never moves and the "host-meet" anchor where
 * the Waiter greets new guests stays in place).
 */
import { useRef } from "react";

import type { Customer, GameState, Order } from "@/api/types";
import { useChoreography } from "@/animation/useChoreography";
import { CustomerFigure, Food, MenuCard, PixelSprite, SatisfactionBadge, WaiterFigure } from "@/components/pixel/Pixel";
import { COIN, SPARKLE, STAFF_LOOKS, figureSprite, type Skin } from "@/components/pixel/sprites";
import { menusOnTables } from "@/animation/choreography";
import {
  NOREN,
  PLANT,
  STOVE_THEMED,
  boothItem,
  boothSprite,
  chairSprite,
  cornerDecor,
  doorSprite,
  kitchenDecor,
  lampSprite,
  pedestalAccessory,
  pedestalSprite,
  signDecor,
  tableSprite,
} from "@/components/pixel/themeSprites";
import { SPEED_FACTOR, store, type StoreState } from "@/store/gameStore";
import type { ThemeId } from "@/theme/themes";

function StateTag({ customer, order }: { customer: Customer; order: Order | undefined }) {
  switch (customer.state) {
    case "SEATED":
      return <span className="tag pop-in" key="seated">Wants to order</span>;
    case "ORDERED":
      return order ? (
        <span className={`tag pop-in ${order.servableNow ? "tag-ready" : ""}`} key="ordered">
          <Food names={order.components} scale={2} /> {order.servableNow ? "Food's ready!" : "Waiting"}
        </span>
      ) : null;
    case "EATING":
      return <span className="tag pop-in" key="eating">Eating</span>;
    case "READY_TO_PAY":
      return (
        <span className="tag tag-gold pop-in" key="pay">
          <PixelSprite sprite={COIN} scale={2} /> Ready to pay
        </span>
      );
    default:
      return null;
  }
}

/** A staff member at their home spot. The sprite (not the name tag) is hidden while their ghost walks. */
function Staff({ role, skin, active, hidden, anchor }: { role: "Host" | "Waiter" | "Chef"; skin: Skin; active: boolean; hidden: boolean; anchor: string }) {
  return (
    <div className={`staff ${active ? "is-active" : ""}`}>
      <span className="staff-figure" data-anchor={anchor} style={{ visibility: hidden ? "hidden" : undefined }}>
        <PixelSprite sprite={figureSprite(STAFF_LOOKS[role], skin)} scale={3} title={role} />
      </span>
      <span className="name-tag">{role}</span>
    </div>
  );
}

/** Theme-specific sign above/behind the cashier booth. */
function BoothSign({ theme }: { theme: ThemeId }) {
  if (theme === "cafe") return <span className="chalkboard">Pay Here</span>;
  if (theme === "tokyo") {
    return (
      <span className="noren">
        <PixelSprite sprite={NOREN} scale={3} />
      </span>
    );
  }
  return <span className="pay-neon">PAY</span>;
}

export function World({ game, ui, theme }: { game: GameState; ui: StoreState; theme: ThemeId }) {
  const skin = ui.skin;
  const worldRef = useRef<HTMLDivElement>(null);
  const motion = useChoreography(ui.animation, worldRef, SPEED_FACTOR[ui.animSpeed], (id) => store.finishAnimation(id));
  const isHidden = (key: string) => motion.hidden.has(key);

  const byId = (id: string) => game.customers.find((c) => c.id === id)!;
  const orderOf = (c: Customer | null) => game.orders.find((o) => c && o.customerId === c.id && o.status !== "CANCELLED");
  const tickets = game.orders.filter((o) => o.status === "PENDING" || o.status === "PREPARING");
  const ready = game.orders.filter((o) => o.status === "READY");
  const role = game.decision?.role;
  const hasHost = game.features.host;
  // The Cashier charges in step 4 at the END of a turn, so a payment shows up in the turn summary.
  const summary = game.phase === "SUMMARY" || game.phase === "OVER";
  const paidNews = summary ? game.turnSummary.find((e) => e.text.startsWith("Cashier collects")) : undefined;
  // Diners who finished eating stay at their table, still occupying it, until the Cashier charges them.
  const readyToPay = game.customers.filter((c) => c.state === "READY_TO_PAY");
  const waitingToPay = readyToPay.filter((c) => c.id !== game.cashierNext).map((c) => c.id);

  return (
    <div className="world" ref={worldRef}>
      {/* ------------- outside: rain, queue, next guest ------------- */}
      <section className="outside" aria-label="Queue outside">
        <div className="rain" aria-hidden />
        <span className="chip-label">Queue</span>
        <i className="anchor-mark mark-queue-front" data-anchor="queue-front" />
        <i className="anchor-mark mark-street-end" data-anchor="street-end" />
        <div className="queue">
          {game.queue.length === 0 && <p className="dim">Nobody waiting</p>}
          {game.queue.map((id) => {
            const c = byId(id);
            return (
              <div key={id} className="queued fade-in">
                <CustomerFigure customer={c} skin={skin} scale={2} />
                <SatisfactionBadge customer={c} />
                <span className={`type type-${c.type}`}>{c.type}</span>
              </div>
            );
          })}
        </div>
        <p className="next-guest">
          {game.nextArrival ? `Next guest on turn ${game.nextArrival.turn}` : "No more guests today"}
        </p>
      </section>

      <div className="interior">
        {/* ------------- kitchen ------------- */}
        <section className="kitchen" aria-label="Kitchen">
          <span className="kitchen-decor" aria-hidden>
            <PixelSprite sprite={kitchenDecor(theme)} scale={2} />
          </span>
          <div className="rail">
            <div className="rail-bar" />
            <div className="tickets">
              {tickets.length === 0 && <span className="dim">No tickets on the rail</span>}
              {tickets.map((o) => (
                <div key={o.id} className="ticket drop-in">
                  <Food names={o.components} scale={2} />
                  <span>
                    {o.id} · {byId(o.customerId).table ?? ""}
                  </span>
                  <span className="pips" aria-label={`${o.unitsDone} of ${o.prepUnits} cooked`}>
                    {Array.from({ length: o.prepUnits }, (_, i) => (
                      <i key={i} className={i < o.unitsDone ? "done" : ""} />
                    ))}
                  </span>
                </div>
              ))}
            </div>
          </div>
          <div className="cookline">
            <PixelSprite sprite={STOVE_THEMED} scale={3} />
            <Staff role="Chef" skin={skin} active={role === "Chef"} hidden={isHidden("staff-Chef")} anchor="chef-home" />
          </div>
          <div className="counter-shelf">
            <i className="anchor-mark mark-counter" data-anchor="counter" />
            {ready.length === 0 ? (
              <span className="dim">Counter is clear</span>
            ) : (
              ready.map((o) => (
                <span
                  key={o.id}
                  className={`ready pop-in ${o.servableNow ? "servable" : ""}`}
                  style={{ visibility: isHidden(`ready-${o.id}`) ? "hidden" : undefined }}
                >
                  <Food names={o.components} scale={3} />
                  <small>
                    {o.item} ready{o.servableNow ? "" : " (next turn)"}
                  </small>
                </span>
              ))
            )}
          </div>
          <div className="counter" aria-hidden />
        </section>

        {/* ------------- sign ------------- */}
        <div className="sign-band">
          <span className="sign-decor" aria-hidden>
            <PixelSprite sprite={signDecor(theme)} scale={2} />
            <PixelSprite sprite={signDecor(theme)} scale={2} />
          </span>
          <span className="neon">Bytes N Bites</span>
          <span className="sign-decor" aria-hidden>
            <PixelSprite sprite={signDecor(theme)} scale={2} />
            <PixelSprite sprite={signDecor(theme)} scale={2} />
          </span>
        </div>

        {/* ------------- dining room ------------- */}
        <section className="floor" aria-label="Dining room">
          <i className="anchor-mark mark-exit" data-anchor="exit" />
          <i className="anchor-mark mark-door-in" data-anchor="door-in" />
          <i className="anchor-mark mark-aisle-left" data-anchor="aisle-left" />
          <i className="anchor-mark mark-aisle-right" data-anchor="aisle-right" />

          <div className="door">
            <PixelSprite sprite={doorSprite(theme)} scale={3} title="Door" />
          </div>

          <div className={`host-spot ${hasHost ? "" : "no-host"}`} aria-hidden={hasHost ? undefined : true}>
            <div className={`staff host ${role === "Host" ? "is-active" : ""}`}>
              <span className="host-stand">
                <span
                  className="staff-figure"
                  data-anchor="host-home"
                  style={{ visibility: !hasHost || isHidden("staff-Host") ? "hidden" : undefined }}
                >
                  <PixelSprite sprite={figureSprite(STAFF_LOOKS.Host, skin)} scale={3} title="Host" />
                </span>
                {/* the pedestal sits in front of the host, so only the upper body shows */}
                <span className="pedestal" aria-hidden style={{ visibility: hasHost ? undefined : "hidden" }}>
                  <span className="pedestal-item">
                    <PixelSprite sprite={pedestalAccessory(theme)} scale={2} />
                  </span>
                  <PixelSprite sprite={pedestalSprite(theme)} scale={3} />
                </span>
                {/* where the Host (or, without a Host, the Waiter) greets a guest at the door */}
                <i className="anchor-mark mark-host-meet" data-anchor="host-meet" />
              </span>
              <span className="name-tag" style={{ visibility: hasHost ? undefined : "hidden" }}>
                Host
              </span>
            </div>
          </div>

          <span className="corner-decor" aria-hidden>
            <PixelSprite sprite={cornerDecor(theme)} scale={4} />
          </span>

          {game.tables.map((table, i) => {
            const occupant = table.occupant ? byId(table.occupant) : null;
            const atTable = occupant;
            const order = orderOf(atTable);
            const seatHidden = isHidden(`seat-${table.id}`);
            return (
              <div key={table.id} className={`table-spot t${i + 1}`}>
                <div className="lamp" aria-hidden>
                  <PixelSprite sprite={lampSprite(theme)} scale={3} />
                  <span className="lamp-glow" />
                </div>
                <div className="seat" data-anchor={`seat-${table.id}`}>
                  <i className="anchor-mark mark-stand" data-anchor={`stand-${table.id}`} />
                  <span className="chair" aria-hidden>
                    <PixelSprite sprite={chairSprite(theme)} scale={3} />
                  </span>
                  {atTable && (
                    <div
                      className={`diner ${seatHidden ? "" : "sit-down"}`}
                      key={`${atTable.id}-${seatHidden ? "away" : "here"}`}
                      style={{ visibility: seatHidden ? "hidden" : undefined }}
                    >
                      <span style={{ visibility: isHidden(`tag-${table.id}`) ? "hidden" : undefined }}>
                        <StateTag customer={atTable} order={order} />
                      </span>
                      <CustomerFigure customer={atTable} skin={skin} scale={3} seated />
                    </div>
                  )}
                </div>
                <div className="table-top">
                  <PixelSprite sprite={tableSprite(theme)} scale={4} className="table-sprite" />
                  {((atTable?.state === "SEATED" && !isHidden(`menu-${table.id}`)) || motion.shown.has(`menu-${table.id}`)) && (
                    <span className="table-menu pop-in">
                      <MenuCard lying />
                    </span>
                  )}
                  {atTable?.state === "EATING" && order && !isHidden(`plate-${table.id}`) && (
                    <span className="plate pop-in">
                      <Food names={order.components} scale={2} />
                    </span>
                  )}
                </div>
                <span className="table-name">{occupant ? table.id : `${table.id} free`}</span>
                {atTable && !seatHidden && <SatisfactionBadge customer={atTable} />}
              </div>
            );
          })}

          <div className="waiter-spot">
            <div className={`staff ${role === "Waiter" ? "is-active" : ""}`}>
              <span className="staff-figure" data-anchor="waiter-home" style={{ visibility: isHidden("staff-Waiter") ? "hidden" : undefined }}>
                {/* two menus: one lies on each table whose guest is seated but has not ordered yet */}
                <WaiterFigure skin={skin} menus={Math.max(0, 2 - menusOnTables(game))} />
              </span>
              <span className="name-tag">Waiter</span>
            </div>
          </div>

          {/* ------------- cashier booth ------------- */}
          <div className="booth-area">
            {/* diners walk here only when the Cashier charges them (end of turn), then leave */}
            <div className="booth-slot" data-anchor="booth" />
            <div className="booth">
              <BoothSign theme={theme} />
              <span className="booth-cashier" aria-hidden>
                <PixelSprite sprite={figureSprite(STAFF_LOOKS.Cashier, skin)} scale={3} />
              </span>
              <span className="booth-front">
                <span className="booth-item">
                  <PixelSprite sprite={boothItem(theme)} scale={2} />
                </span>
                {theme === "cafe" && (
                  <span className="booth-plant">
                    <PixelSprite sprite={PLANT} scale={2} />
                  </span>
                )}
                {theme === "tokyo" && (
                  <span className="booth-lantern">
                    <PixelSprite sprite={lampSprite("tokyo")} scale={2} />
                  </span>
                )}
                <PixelSprite sprite={boothSprite(theme)} scale={3} className="booth-sprite" />
              </span>
            </div>
            <div className="booth-caption">
              <strong>Cashier</strong>
              <span>
                {paidNews
                  ? paidNews.text.replace(/^Cashier collects (\$[\d.]+) from (C\d+).*/, "Collected $1 from $2") +
                    game.tipList
                      .filter((t) => t.turn === game.turn)
                      .map((t) => ` + ${t.amount} tip`)
                      .join("")
                  : !summary && game.cashierNext
                    ? `${game.cashierNext} pays at the end of this turn${waitingToPay.length > 0 ? `, then ${waitingToPay.join(", ")}` : ""}`
                    : readyToPay.length > 0
                      ? `${readyToPay.map((c) => c.id).join(", ")} ${readyToPay.length > 1 ? "pay" : "pays"} at the end of ${summary ? "next turn" : "a later turn"}`
                      : "Nobody to charge"}
              </span>
            </div>
          </div>
        </section>
      </div>

      {/* ------------- walking characters (behind props, never over text) ------------- */}
      <div className="ghost-layer" aria-hidden>
        {motion.ghosts.map((g) => {
          const actor = ui.animation?.actors.find((a) => a.id === g.id);
          if (!actor) return null;
          const body =
            actor.kind === "dish" ? (
              <Food names={actor.dish ?? []} scale={3} />
            ) : actor.role === "Waiter" ? (
              <WaiterFigure skin={skin} menus={g.menus ?? 0} />
            ) : actor.kind === "staff" ? (
              <PixelSprite sprite={figureSprite(STAFF_LOOKS[actor.role!], skin)} scale={3} />
            ) : (
              // same figure as in the scene: aura outline, phone, emotion tint and anger marks included
              <CustomerFigure customer={actor.customer!} skin={skin} scale={3} />
            );
          return (
            <div key={g.id} className="ghost" style={{ left: g.x, top: g.y }}>
              {g.bubble && <span className="tag ghost-bubble">{g.bubble}</span>}
              {g.carry && (
                <span className="ghost-carry">
                  <Food names={g.carry} scale={2} />
                </span>
              )}
              <span className={`ghost-body ${g.walking ? "walking" : ""} ${g.cooking ? "cooking" : ""}`} style={{ transform: `scaleX(${g.facing})` }}>
                {body}
              </span>
            </div>
          );
        })}
      </div>
      <div className="float-layer" aria-hidden>
        {motion.floats.map((f) => (
          <span key={f.key} className={`float-text ${f.kind ? `float-${f.kind}` : ""}`} style={{ left: f.x, top: f.y }}>
            {f.kind === "tip" && <PixelSprite sprite={SPARKLE} scale={2} className="sparkle" />}
            {f.kind === "tip" && <PixelSprite sprite={COIN} scale={2} />}
            {f.text}
          </span>
        ))}
      </div>
    </div>
  );
}
