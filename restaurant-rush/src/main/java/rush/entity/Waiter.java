package rush.entity;

import java.util.Optional;

/** The Waiter: each turn, seats one queued customer, takes one order, serves one READY order, or waits. */
public final class Waiter extends Staff {

    /** Creates the Waiter. */
    public Waiter() {
        super("Waiter");
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
            return ActionResult.ok("Waiter seats " + result.getMessage());
        }
        return result;
    }

    /**
     * Takes a seated customer's order. The customer chooses the dish; the price is locked separately by the
     * caller's pricing step straight after this succeeds.
     *
     * @param restaurant the restaurant
     * @param customerId a SEATED customer
     * @param menu       the menu the customer chooses from
     * @param turn       the current turn
     * @return success, or the reason it is not allowed
     */
    public ActionResult takeOrder(Restaurant restaurant, String customerId, Menu menu, int turn) {
        if (hasActedThisTurn()) {
            return alreadyActed();
        }
        Optional<Customer> found = restaurant.findCustomer(customerId);
        if (found.isEmpty()) {
            return ActionResult.fail("There is no customer '" + customerId + "'.");
        }
        Customer customer = found.get();
        if (customer.getState() != CustomerState.SEATED) {
            return ActionResult.fail(customer.getId() + " is not seated without an order (state "
                    + customer.getState() + ").");
        }
        MenuItem choice = customer.chooseFrom(menu);
        Order order = restaurant.createOrder(customer, choice, turn);
        markActed();
        return ActionResult.ok("Waiter takes " + customer.getId() + "'s order " + order.getId() + ": "
                + customer.getId() + " chooses " + choice.describe() + ".");
    }

    /**
     * Serves an order that was already READY before this turn.
     *
     * @param restaurant the restaurant
     * @param orderId    the order to serve
     * @param turn       the current turn
     * @return success, or the reason it is not allowed
     */
    public ActionResult serve(Restaurant restaurant, String orderId, int turn) {
        if (hasActedThisTurn()) {
            return alreadyActed();
        }
        Optional<Order> found = restaurant.findOrder(orderId);
        if (found.isEmpty()) {
            return ActionResult.fail("There is no order '" + orderId + "'.");
        }
        Order order = found.get();
        if (order.getStatus() != OrderStatus.READY) {
            return ActionResult.fail(order.getId() + " is not READY (status " + order.getStatus() + ", units "
                    + order.progress() + ").");
        }
        if (!order.isServableOn(turn)) {
            return ActionResult.fail(order.getId() + " only became READY this turn; it can be served next turn.");
        }
        order.markServed(turn);
        order.getCustomer().receiveServedOrder();
        markActed();
        return ActionResult.ok("Waiter serves " + order.getId() + " (" + order.getItem().getName() + ") to "
                + order.getCustomer().getId() + ". SERVED.");
    }
}
