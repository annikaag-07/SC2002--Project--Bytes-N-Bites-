package rush.entity;

/**
 * A food critic: impatient (loses 12 satisfaction per waiting turn), pays full price, and judges the speed
 * of service. When served, a Critic whose food arrives within 2 turns of the order being taken is impressed
 * (+10 satisfaction, capped at 100); otherwise the Critic is unimpressed (-10, floored at 0).
 */
public class CriticCustomer extends Customer {

    /** Maximum turns from order taken to food served that still impresses the Critic. */
    public static final int FAST_SERVICE_TURNS = 2;
    private static final int REACTION_POINTS = 10;

    /**
     * Creates a Critic customer.
     *
     * @param id           display id
     * @param arrivalIndex 1-based arrival index
     * @param arrivalTurn  arrival turn
     * @param gameSeed     game seed
     */
    public CriticCustomer(String id, int arrivalIndex, int arrivalTurn, long gameSeed) {
        super(id, arrivalIndex, arrivalTurn, gameSeed);
    }

    @Override
    public String typeLabel() {
        return "Critic";
    }

    @Override
    protected int satisfactionLossPerTurn() {
        return 12;
    }

    /** Service-speed reaction: impressed if served within {@value #FAST_SERVICE_TURNS} turns of ordering. */
    @Override
    protected String onServed(int turn) {
        int serviceTurns = turn - getOrder().getTakenTurn();
        int before = getSatisfaction();
        boolean impressed = serviceTurns <= FAST_SERVICE_TURNS;
        changeSatisfaction(impressed ? REACTION_POINTS : -REACTION_POINTS);
        return "Critic " + getId() + " is " + (impressed ? "IMPRESSED" : "UNIMPRESSED") + " (served "
                + serviceTurns + " turn(s) after ordering; fast is <= " + FAST_SERVICE_TURNS + "): satisfaction "
                + before + " -> " + getSatisfaction() + ".";
    }
}
