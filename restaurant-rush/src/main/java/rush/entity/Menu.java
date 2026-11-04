package rush.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The ordered list of dishes customers can choose from. */
public final class Menu {

    private final List<MenuItem> offerings;

    /**
     * Creates a menu with the given offerings, kept in the given order.
     *
     * @param offerings at least one item
     */
    public Menu(List<MenuItem> offerings) {
        if (offerings == null || offerings.isEmpty()) {
            throw new IllegalArgumentException("a menu needs at least one item");
        }
        this.offerings = Collections.unmodifiableList(new ArrayList<>(offerings));
    }

    /**
     * Builds the base-setting menu: Salad $9.00 (1 unit), Burger $15.00 (1 unit), Pasta $18.00 (2 units).
     *
     * @return the standard menu
     */
    public static Menu standard() {
        return new Menu(List.of(
                new MenuItem("Salad", 900, 1),
                new MenuItem("Burger", 1500, 1),
                new MenuItem("Pasta", 1800, 2)));
    }

    /** @return the offerings in menu order (unmodifiable) */
    public List<MenuItem> getOfferings() {
        return offerings;
    }

    /**
     * Looks up an offering by name.
     *
     * @param name the item name, matched exactly
     * @return the item
     * @throws IllegalArgumentException if no item has that name
     */
    public MenuItem itemNamed(String name) {
        for (MenuItem item : offerings) {
            if (item.getName().equals(name)) {
                return item;
            }
        }
        throw new IllegalArgumentException("no menu item named " + name);
    }
}
