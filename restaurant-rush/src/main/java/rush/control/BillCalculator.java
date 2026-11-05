package rush.control;

import rush.entity.Order;

/** The single place where an order's price is worked out and locked, at the moment the order is taken. */
public final class BillCalculator {

    /**
     * Works out the price of a freshly taken order and locks it: the menu price, then the customer's own
     * pricing (for example the VIP discount), dispatched polymorphically through the customer.
     *
     * @param order an order whose price is not yet locked
     * @param turn  the turn the order is taken
     * @return the locked price in cents
     */
    public long lockPrice(Order order, int turn) {
        long base = order.getItem().getPriceCents();
        long price = order.getCustomer().applyCustomerPricing(base);
        order.lockPrice(price);
        return price;
    }
}
