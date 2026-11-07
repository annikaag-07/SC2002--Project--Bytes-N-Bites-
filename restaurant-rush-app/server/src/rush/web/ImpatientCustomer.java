package rush.web;

import rush.entity.Customer;

/**
 * Impatient customer (web edition only). Loses 10 satisfaction per waiting turn and walks out at a higher
 * threshold: once a turn's loss would leave it at {@value #WALK_OUT_AT} or below, it storms out.
 *
 * <p>Implemented purely through the existing {@code satisfactionLossPerTurn()} hook: when the customer would
 * fall to the walk-out threshold, the loss is its whole remaining satisfaction, so the unchanged back-end
 * abandonment rule (leave at 0) applies. No back-end class had to be modified.</p>
 */
public class ImpatientCustomer extends Customer {

    /** Satisfaction at or below which an Impatient customer walks out. */
    public static final int WALK_OUT_AT = 30;
    private static final int LOSS = 10;

    /**
     * @param id           display id
     * @param arrivalIndex 1-based arrival index
     * @param arrivalTurn  arrival turn
     * @param gameSeed     game seed
     */
    public ImpatientCustomer(String id, int arrivalIndex, int arrivalTurn, long gameSeed) {
        super(id, arrivalIndex, arrivalTurn, gameSeed);
    }

    @Override
    public String typeLabel() {
        return "Impatient";
    }

    @Override
    protected int satisfactionLossPerTurn() {
        int after = getSatisfaction() - LOSS;
        return after <= WALK_OUT_AT ? getSatisfaction() : LOSS;
    }
}
