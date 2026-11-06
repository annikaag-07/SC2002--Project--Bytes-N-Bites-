package rush.control;

import rush.entity.Order;

/**
 * The single place where an order's price is worked out and locked, at the moment the order is taken.
 * Pipeline, rounding half up after each step: menu price (a combo is already at 90%) -> customer pricing
 * (VIP 90%) -> Happy Hour (80% if active on the order turn) -> lock.
 */
public final class BillCalculator {

    private final HappyHourPolicy happyHour;

    /**
     * @param happyHour the Happy Hour policy consulted for every order
     */
    public BillCalculator(HappyHourPolicy happyHour) {
        this.happyHour = happyHour;
    }

    /**
     * Works out the price of a freshly taken order and locks it: the menu price, then the customer's own
     * pricing (for example the VIP discount, dispatched polymorphically through the customer), then Happy Hour.
     *
     * @param order an order whose price is not yet locked
     * @param turn  the turn the order is taken
     * @return the locked price in cents
     */
    public long lockPrice(Order order, int turn) {
        long base = order.getItem().getPriceCents();
        long afterCustomer = order.getCustomer().applyCustomerPricing(base);
        long price = happyHour.applyDiscount(afterCustomer, turn);
        order.lockPrice(price);
        return price;
    }
}
