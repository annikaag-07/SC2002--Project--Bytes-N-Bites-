package rush.entity;

/**
 * One customer's order for one menu item. It tracks kitchen progress, its status, the turns of key events
 * and the price locked when the order was taken.
 */
public final class Order {

    private final String id;
    private final Customer customer;
    private final MenuItem item;
    private final int takenTurn;
    private OrderStatus status = OrderStatus.PENDING;
    private int unitsDone;
    private long lockedPriceCents = -1;
    private int readyTurn;
    private int servedTurn;

    Order(String id, Customer customer, MenuItem item, int takenTurn) {
        this.id = id;
        this.customer = customer;
        this.item = item;
        this.takenTurn = takenTurn;
    }

    /**
     * Locks the bill for this order. The price can be locked once only, when the order is taken.
     *
     * @param priceCents the final price in cents
     * @throws IllegalStateException if the price is already locked
     */
    public void lockPrice(long priceCents) {
        if (isPriceLocked()) {
            throw new IllegalStateException("price of " + id + " is already locked");
        }
        if (priceCents < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
        lockedPriceCents = priceCents;
    }

    /** @return true once the price has been locked */
    public boolean isPriceLocked() {
        return lockedPriceCents >= 0;
    }

    /**
     * Tells whether the Waiter may serve this order on the given turn: it must be READY and
     * must have become READY on an earlier turn.
     *
     * @param turn the current turn
     * @return true if servable now
     */
    public boolean isServableOn(int turn) {
        return status == OrderStatus.READY && readyTurn < turn;
    }

    /** Does one preparation unit; the order becomes READY when the last unit is done. */
    boolean advanceUnit(int turn) {
        unitsDone++;
        if (unitsDone == item.getPrepUnits()) {
            status = OrderStatus.READY;
            readyTurn = turn;
            return true;
        }
        status = OrderStatus.PREPARING;
        return false;
    }

    void markServed(int turn) {
        status = OrderStatus.SERVED;
        servedTurn = turn;
    }

    void markPaid() {
        status = OrderStatus.PAID;
    }

    void cancel() {
        status = OrderStatus.CANCELLED;
    }

    /** @return the display id, for example "O1" */
    public String getId() {
        return id;
    }

    /** @return the customer who placed the order */
    public Customer getCustomer() {
        return customer;
    }

    /** @return the chosen menu item */
    public MenuItem getItem() {
        return item;
    }

    /** @return the turn the order was taken */
    public int getTakenTurn() {
        return takenTurn;
    }

    /** @return the current status */
    public OrderStatus getStatus() {
        return status;
    }

    /** @return preparation units done so far */
    public int getUnitsDone() {
        return unitsDone;
    }

    /** @return the locked price in cents, or -1 if not yet locked */
    public long getLockedPriceCents() {
        return lockedPriceCents;
    }

    /** @return the turn the order became READY, or 0 if not yet */
    public int getReadyTurn() {
        return readyTurn;
    }

    /** @return the turn the order was served, or 0 if not yet */
    public int getServedTurn() {
        return servedTurn;
    }

    /** @return kitchen progress as "done/total", for example "1/2" */
    public String progress() {
        return unitsDone + "/" + item.getPrepUnits();
    }

    @Override
    public String toString() {
        return id;
    }
}
