package rush.entity;

import java.util.Optional;

/** The Chef: each turn, advances one PENDING or PREPARING order by one preparation unit, or waits. */
public final class Chef extends Staff {

    /** Creates the Chef. */
    public Chef() {
        super("Chef");
    }

    /**
     * Does one preparation unit on an order; the order becomes READY when its last unit is done.
     *
     * @param restaurant the restaurant
     * @param orderId    the order to work on
     * @param turn       the current turn
     * @return success, or the reason it is not allowed
     */
    public ActionResult advance(Restaurant restaurant, String orderId, int turn) {
        if (hasActedThisTurn()) {
            return alreadyActed();
        }
        Optional<Order> found = restaurant.findOrder(orderId);
        if (found.isEmpty()) {
            return ActionResult.fail("There is no order '" + orderId + "'.");
        }
        Order order = found.get();
        if (!order.getStatus().isInKitchen()) {
            return ActionResult.fail(order.getId() + " needs no more preparation (status " + order.getStatus() + ").");
        }
        boolean ready = order.advanceUnit(turn);
        markActed();
        return ActionResult.ok("Chef prepares " + order.getId() + " (" + order.getItem().getName() + "): units "
                + order.progress() + (ready ? ". READY (can be served from next turn)." : "."));
    }
}
