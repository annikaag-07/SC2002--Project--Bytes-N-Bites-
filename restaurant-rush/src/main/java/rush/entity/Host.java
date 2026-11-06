package rush.entity;

/**
 * The Host: each turn, seats at most one queued customer at a free table, in addition to the Waiter's task.
 * The Host acts before the Waiter, so the Waiter may take the order of a customer the Host just seated.
 */
public final class Host extends Staff {

    /** Creates the Host. */
    public Host() {
        super("Host");
    }

    /**
     * Seats a queued customer (any position in the queue) at a free table.
     *
     * @param restaurant the restaurant
     * @param customerId the customer to seat
     * @param tableId    the table to use
     * @return success, or the reason it is not allowed
     */
    public ActionResult seat(Restaurant restaurant, String customerId, String tableId) {
        if (hasActedThisTurn()) {
            return alreadyActed();
        }
        ActionResult result = restaurant.seatCustomer(customerId, tableId);
        if (result.isSuccess()) {
            markActed();
            return ActionResult.ok("Host seats " + result.getMessage());
        }
        return result;
    }
}
