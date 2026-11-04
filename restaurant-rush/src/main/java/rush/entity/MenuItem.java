package rush.entity;

/** An immutable dish on the menu: a name, a price in cents and the number of preparation units the Chef must do. */
public class MenuItem {

    private final String name;
    private final long priceCents;
    private final int prepUnits;

    /**
     * Creates a menu item.
     *
     * @param name       display name, not blank
     * @param priceCents price in cents, not negative
     * @param prepUnits  preparation units, at least 1
     */
    public MenuItem(String name, long priceCents, int prepUnits) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (priceCents < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
        if (prepUnits < 1) {
            throw new IllegalArgumentException("prepUnits must be at least 1");
        }
        this.name = name;
        this.priceCents = priceCents;
        this.prepUnits = prepUnits;
    }

    /** @return the display name */
    public String getName() {
        return name;
    }

    /** @return the menu price in cents */
    public long getPriceCents() {
        return priceCents;
    }

    /** @return the number of preparation units the Chef must do */
    public int getPrepUnits() {
        return prepUnits;
    }

    /**
     * Describes the item for display, for example "Pasta ($18.00, 2 units)".
     *
     * @return a one-line description
     */
    public String describe() {
        return name + " (" + Money.format(priceCents) + ", " + prepUnits + (prepUnits == 1 ? " unit)" : " units)");
    }

    @Override
    public String toString() {
        return name;
    }
}
