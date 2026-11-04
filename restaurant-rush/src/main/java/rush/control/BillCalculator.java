package rush.control;

import rush.entity.Order;

/** The single place where an order's price is worked out and locked, at the moment the order is taken. */
public final class BillCalculator {

    /**
     * Works out the price of a freshly taken order and locks it. Stage 1: the menu price.
     *
     * @param order an order whose price is not yet locked
     * @param turn  the turn the order is taken
     * @return the locked price in cents
     */
    public long lockPrice(Order order, int turn) {
        long price = order.getItem().getPriceCents();
        order.lockPrice(price);
        return price;
    }
}
