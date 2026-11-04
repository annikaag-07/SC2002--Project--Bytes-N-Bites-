/** Starry page with the neon pink frame (about 90% of the screen) that every screen sits in. */
export function Frame({ children, rain = false, className }: { children: React.ReactNode; rain?: boolean; className?: string }) {
  return (
    <div className="stage">
      <div className={`frame ${className ?? ""}`}>
        {rain && <div className="rain rain-full" aria-hidden />}
        {children}
      </div>
    </div>
  );
}

/** The game's title, as on the main menu. */
export function Title({ small = false }: { small?: boolean }) {
  return (
    <h1 className={`title ${small ? "title-small" : ""}`}>
      <span className="title-top">Bytes N</span>
      <span className="title-big">Bites</span>
    </h1>
  );
}
