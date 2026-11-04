package rush.entity;

/** Lifecycle of an order, from being taken to being paid or cancelled. */
public enum OrderStatus {
    /** Taken, no preparation unit done yet. */
    PENDING,
    /** At least one, but not all, preparation units done. */
    PREPARING,
    /** All preparation units done; can be served from the next turn on. */
    READY,
    /** Delivered to the customer. */
    SERVED,
    /** Paid at the Cashier. */
    PAID,
    /** Discarded because the customer left. */
    CANCELLED;

    /**
     * Tells whether the Chef may still work on an order in this status.
     *
     * @return true for PENDING and PREPARING
     */
    public boolean isInKitchen() {
        return this == PENDING || this == PREPARING;
    }
}
