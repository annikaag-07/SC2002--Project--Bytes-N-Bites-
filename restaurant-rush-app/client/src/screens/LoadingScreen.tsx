import { Frame, Title } from "@/components/Frame";
import { PixelSprite } from "@/components/pixel/Pixel";
import { STAFF_LOOKS, figureSprite } from "@/components/pixel/sprites";
import { useRotatingTip } from "@/content/useRotatingTip";
import type { StoreState } from "@/store/gameStore";

export function LoadingScreen({ ui }: { ui: StoreState }) {
  const { tip, index } = useRotatingTip();
  return (
    <Frame rain className="menu-frame">
      <div className="menu">
        <PixelSprite sprite={figureSprite(STAFF_LOOKS.Chef, ui.skin)} scale={4} className="bob" />
        <Title />
        <div className="loading-bar" aria-label="Loading">
          <span />
        </div>
        <p className="dim">Warming up the grill...</p>
        {ui.error && <p className="warning">{ui.error}</p>}
      </div>
      <p className="tip fade-in" key={index}>
        <span className="tip-tag">Tip</span> {tip}
      </p>
    </Frame>
  );
}
