package rush.entity;

import java.util.List;

/**
 * A combo made of two base menu items (composition). Its preparation units are the sum of the components'
 * units, and its price is 90% of the components' combined price, rounded half up to the nearest cent.
 */
public final class ComboMeal extends MenuItem {

    private static final int PERCENT_PAID = 90;

    private final MenuItem first;
    private final MenuItem second;

    /**
     * Creates a combo of two base items, named "First + Second Combo".
     *
     * @param first  a base (non-combo) item
     * @param second a base (non-combo) item
     * @throws IllegalArgumentException if a component is null or is itself a combo
     */
    public ComboMeal(MenuItem first, MenuItem second) {
        super(requireBase(first).getName() + " + " + requireBase(second).getName() + " Combo",
                Money.percent(first.getPriceCents() + second.getPriceCents(), PERCENT_PAID),
                first.getPrepUnits() + second.getPrepUnits());
        this.first = first;
        this.second = second;
    }

    private static MenuItem requireBase(MenuItem item) {
        if (item == null) {
            throw new IllegalArgumentException("a combo needs two items");
        }
        if (item instanceof ComboMeal) {
            throw new IllegalArgumentException("a combo cannot contain another combo");
        }
        return item;
    }

    /** @return the two component items, in order */
    public List<MenuItem> getComponents() {
        return List.of(first, second);
    }

    /** Shows the components and the undiscounted total, for example "Salad + Pasta Combo [$9.00 + $18.00 - 10%] ($24.30, 3 units)". */
    @Override
    public String describe() {
        return getName() + " [" + Money.format(first.getPriceCents()) + " + " + Money.format(second.getPriceCents())
                + " - 10%] (" + Money.format(getPriceCents()) + ", " + getPrepUnits() + " units)";
    }
}
