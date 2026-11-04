package rush.entity;

/** An ordinary customer: loses 8 satisfaction per waiting turn and pays full price. */
public class RegularCustomer extends Customer {

    /**
     * Creates a Regular customer.
     *
     * @param id           display id
     * @param arrivalIndex 1-based arrival index
     * @param arrivalTurn  arrival turn
     * @param gameSeed     game seed
     */
    public RegularCustomer(String id, int arrivalIndex, int arrivalTurn, long gameSeed) {
        super(id, arrivalIndex, arrivalTurn, gameSeed);
    }

    @Override
    public String typeLabel() {
        return "Regular";
    }

    @Override
    protected int satisfactionLossPerTurn() {
        return 8;
    }
}
