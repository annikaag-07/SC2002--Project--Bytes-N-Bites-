package rush.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The Cashier: automatically charges at most one customer per turn, oldest first
 * (earliest READY_TO_PAY turn, then earliest arrival).
 */
public final class Cashier extends Staff {

    private static final Comparator<Customer> OLDEST_FIRST = Comparator
            .comparingInt(Customer::getReadyToPayTurn)
            .thenComparingInt(Customer::getArrivalIndex);

    /** Creates the Cashier. */
    public Cashier() {
        super("Cashier");
    }

    /**
     * Lists the customers who could pay on this turn, in the order the Cashier would take them.
     *
     * @param restaurant the restaurant
     * @param turn       the current turn
     * @return eligible customers, oldest first
     */
    public List<Customer> payableCustomers(Restaurant restaurant, int turn) {
        List<Customer> payable = new ArrayList<>();
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.canPayOn(turn)) {
                payable.add(customer);
            }
        }
        payable.sort(OLDEST_FIRST);
        return payable;
    }

    /**
     * Charges the oldest eligible customer: adds the locked price to revenue, records the customer's
     * satisfaction, marks customer and order PAID and frees the table.
     *
     * @param restaurant the restaurant
     * @param turn       the current turn
     * @return success with the payment, or failure if nobody can pay or the Cashier already acted
     */
    public ActionResult collectPayment(Restaurant restaurant, int turn) {
        if (hasActedThisTurn()) {
            return alreadyActed();
        }
        List<Customer> payable = payableCustomers(restaurant, turn);
        if (payable.isEmpty()) {
            return ActionResult.fail("Cashier: nobody is ready to pay.");
        }
        Customer customer = payable.get(0);
        String tableId = customer.getTable().getId();
        restaurant.recordPayment(customer);
        markActed();
        return ActionResult.ok("Cashier collects " + Money.format(customer.getOrder().getLockedPriceCents())
                + " from " + customer.getId() + " for " + customer.getOrder().getId() + ". PAID; satisfaction "
                + customer.getRecordedSatisfaction() + " recorded; table " + tableId + " freed.");
    }
}
