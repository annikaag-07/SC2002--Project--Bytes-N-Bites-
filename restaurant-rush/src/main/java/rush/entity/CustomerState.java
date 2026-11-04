package rush.entity;

/** Lifecycle of a customer, from joining the queue to paying or leaving. */
public enum CustomerState {
    /** In the waiting queue, no table yet. */
    WAITING(true),
    /** At a table, order not yet taken. */
    SEATED(true),
    /** Order taken, food not yet served. */
    ORDERED(true),
    /** Served and eating. */
    EATING(false),
    /** Finished eating, waiting for the Cashier. */
    READY_TO_PAY(false),
    /** Paid; the visit is complete. */
    PAID(false),
    /** Left unhappy without paying. */
    LEFT(false);

    private final boolean awaitingService;

    CustomerState(boolean awaitingService) {
        this.awaitingService = awaitingService;
    }

    /**
     * Tells whether a customer in this state is still waiting for a table or for food, and so loses satisfaction.
     *
     * @return true for WAITING, SEATED and ORDERED
     */
    public boolean isAwaitingService() {
        return awaitingService;
    }
}
