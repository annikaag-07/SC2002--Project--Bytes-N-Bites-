package rush.entity;

/**
 * Test-only helper living in the entity package so tests can build states that the normal turn flow cannot
 * reach (for example several customers waiting for the Cashier at once, which one Waiter can never cause).
 */
public final class EntityTestSupport {

    private EntityTestSupport() {
    }

    /**
     * Builds a restaurant with one table per customer, in which customer i was seated, ordered {@code item},
     * had it cooked on turn 1 and was served on {@code servedTurns[i]}.
     *
     * @param customers   customers in arrival order
     * @param servedTurns the turn each one was served
     * @param item        what each one ordered (price locked at the menu price)
     * @return the restaurant
     */
    public static Restaurant restaurantWithServedCustomers(Customer[] customers, int[] servedTurns, MenuItem item) {
        Restaurant restaurant = new Restaurant(customers.length);
        for (int i = 0; i < customers.length; i++) {
            Customer customer = customers[i];
            restaurant.admitArrival(customer);
            if (!restaurant.seatCustomer(customer.getId(), "T" + (i + 1)).isSuccess()) {
                throw new IllegalStateException("could not seat " + customer.getId());
            }
            Order order = restaurant.createOrder(customer, item, 1);
            order.lockPrice(item.getPriceCents());
            for (int unit = 0; unit < item.getPrepUnits(); unit++) {
                order.advanceUnit(1);
            }
            order.markServed(servedTurns[i]);
            customer.receiveServedOrder();
        }
        return restaurant;
    }
}
