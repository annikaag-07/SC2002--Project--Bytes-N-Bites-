package rush.control;

import rush.entity.Menu;

/** Immutable game settings: length, tables, seed, menu and the three victory thresholds. */
public final class GameConfig {

    private final int totalTurns;
    private final int tableCount;
    private final long seed;
    private final Menu menu;
    private final int minPaidCustomers;
    private final long minRevenueCents;
    private final int minAverageSatisfaction;

    /**
     * Creates a configuration.
     *
     * @param totalTurns             number of turns, always all played
     * @param tableCount             number of tables
     * @param seed                   game seed for customers' food choices
     * @param menu                   the menu
     * @param minPaidCustomers       victory needs at least this many paid customers
     * @param minRevenueCents        victory needs at least this much revenue
     * @param minAverageSatisfaction victory needs at least this average recorded satisfaction
     */
    public GameConfig(int totalTurns, int tableCount, long seed, Menu menu,
                      int minPaidCustomers, long minRevenueCents, int minAverageSatisfaction) {
        if (totalTurns < 1 || tableCount < 1 || menu == null) {
            throw new IllegalArgumentException("invalid configuration");
        }
        this.totalTurns = totalTurns;
        this.tableCount = tableCount;
        this.seed = seed;
        this.menu = menu;
        this.minPaidCustomers = minPaidCustomers;
        this.minRevenueCents = minRevenueCents;
        this.minAverageSatisfaction = minAverageSatisfaction;
    }

    /**
     * The required base setting: 18 turns, 2 tables, the standard menu, and victory at
     * 4 paid customers, $45.00 revenue and average satisfaction 60.
     *
     * @param seed the game seed
     * @return the base configuration
     */
    public static GameConfig base(long seed) {
        return new GameConfig(18, 2, seed, Menu.standard(), 4, 4500, 60);
    }

    /**
     * Returns a copy of this configuration with a different menu (used by rule tests).
     *
     * @param otherMenu the menu to use
     * @return the new configuration
     */
    public GameConfig withMenu(Menu otherMenu) {
        return new GameConfig(totalTurns, tableCount, seed, otherMenu,
                minPaidCustomers, minRevenueCents, minAverageSatisfaction);
    }

    /** @return number of turns */
    public int getTotalTurns() {
        return totalTurns;
    }

    /** @return number of tables */
    public int getTableCount() {
        return tableCount;
    }

    /** @return the game seed */
    public long getSeed() {
        return seed;
    }

    /** @return the menu */
    public Menu getMenu() {
        return menu;
    }

    /** @return minimum paid customers for victory */
    public int getMinPaidCustomers() {
        return minPaidCustomers;
    }

    /** @return minimum revenue in cents for victory */
    public long getMinRevenueCents() {
        return minRevenueCents;
    }

    /** @return minimum average recorded satisfaction for victory */
    public int getMinAverageSatisfaction() {
        return minAverageSatisfaction;
    }
}
