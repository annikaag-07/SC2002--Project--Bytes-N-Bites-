/**
 * Restaurant themes picker. Each card is a small live scene drawn with the real themed sprites; clicking a
 * card previews that theme on the whole screen behind the dialog, and "Use this theme" saves it.
 */
import { useState } from "react";

import { PixelSprite } from "@/components/pixel/Pixel";
import { CUSTOMER_LOOKS, STAFF_LOOKS, figureSprite, seatedSprite, type Skin } from "@/components/pixel/sprites";
import { chairSprite, cornerDecor, lampSprite, pedestalSprite, signDecor, tableSprite } from "@/components/pixel/themeSprites";
import { sounds } from "@/sound/soundManager";
import { store } from "@/store/gameStore";
import { THEMES, type ThemeId } from "@/theme/themes";

/** A miniature of the restaurant in one theme (the data-theme attribute scopes its colours). */
export function ThemePreview({ id, skin }: { id: ThemeId; skin: Skin }) {
  return (
    <div className="theme-preview" data-theme={id} aria-hidden>
      <div className="tp-wall">
        <PixelSprite sprite={signDecor(id)} scale={2} />
        <span className="neon tp-sign">Bytes N Bites</span>
        <PixelSprite sprite={signDecor(id)} scale={2} />
      </div>
      <div className="tp-floor">
        <span className="tp-decor">
          <PixelSprite sprite={cornerDecor(id)} scale={2} />
        </span>
        <span className="tp-host">
          <PixelSprite sprite={figureSprite(STAFF_LOOKS.Host, skin)} scale={2} />
          <span className="tp-pedestal">
            <PixelSprite sprite={pedestalSprite(id)} scale={2} />
          </span>
        </span>
        <span className="tp-table">
          <span className="tp-lamp">
            <PixelSprite sprite={lampSprite(id)} scale={2} />
          </span>
          <span className="tp-seat">
            <span className="tp-chair">
              <PixelSprite sprite={chairSprite(id)} scale={2} />
            </span>
            <PixelSprite sprite={seatedSprite(figureSprite(CUSTOMER_LOOKS.Regular, skin, "calm", "C1"), skin)} scale={2} />
          </span>
          <PixelSprite sprite={tableSprite(id)} scale={3} className="table-sprite tp-table-sprite" />
        </span>
      </div>
    </div>
  );
}

export function ThemePicker({ current, skin, onClose }: { current: ThemeId; skin: Skin; onClose: () => void }) {
  const [selected, setSelected] = useState<ThemeId>(current);
  const close = () => {
    store.previewTheme(null);
    onClose();
  };
  return (
    <div className="overlay" onClick={close}>
      <div className="modal theme-modal pop-in" role="dialog" aria-label="Restaurant themes" onClick={(e) => e.stopPropagation()}>
        <h2>Restaurant themes</h2>
        <p className="dim">Pick a look for your restaurant. Themes only change how it looks: the game plays exactly the same.</p>
        <div className="theme-cards" role="radiogroup">
          {THEMES.map((t) => (
            <button
              key={t.id}
              role="radio"
              aria-checked={selected === t.id}
              className={`theme-card ${selected === t.id ? "selected" : ""}`}
              onClick={() => {
                sounds.play("click");
                setSelected(t.id);
                store.previewTheme(t.id);
              }}
            >
              <ThemePreview id={t.id} skin={skin} />
              <span className="theme-card-text">
                <strong>
                  {t.name}
                  {t.id === current && <span className="current-badge">Current</span>}
                </strong>
                <small>{t.tagline}</small>
              </span>
            </button>
          ))}
        </div>
        <div className="modal-actions">
          <button className="btn" onClick={close}>
            Cancel
          </button>
          <button
            className="btn btn-primary"
            onClick={() => {
              store.setTheme(selected);
              onClose();
            }}
          >
            Use this theme
          </button>
        </div>
      </div>
    </div>
  );
}
