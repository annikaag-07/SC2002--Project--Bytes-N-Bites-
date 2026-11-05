package rush.entity;

/** A VIP customer: more patient (loses 5 satisfaction per waiting turn) and gets 10% off the bill. */
public class VIPCustomer extends Customer {

    private static final int PERCENT_PAID = 90;

    /**
     * Creates a VIP customer.
     *
     * @param id           display id
     * @param arrivalIndex 1-based arrival index
     * @param arrivalTurn  arrival turn
     * @param gameSeed     game seed
     */
    public VIPCustomer(String id, int arrivalIndex, int arrivalTurn, long gameSeed) {
        super(id, arrivalIndex, arrivalTurn, gameSeed);
    }

    @Override
    public String typeLabel() {
        return "VIP";
    }

    @Override
    protected int satisfactionLossPerTurn() {
        return 5;
    }

    /** VIPs pay 90% of the price, rounded half up to the nearest cent. */
    @Override
    public long applyCustomerPricing(long priceCents) {
        return Money.percent(priceCents, PERCENT_PAID);
    }
}
