package rush.entity;

import java.util.List;
import java.util.Random;

/**
 * A restaurant guest. A customer tracks its own satisfaction and lifecycle state and chooses its own food
 * with a private, seeded random generator. Subclasses define how quickly they lose patience.
 */
public abstract class Customer {

    /** Satisfaction every customer starts with, and the maximum. */
    public static final int MAX_SATISFACTION = 100;

    private final String id;
    private final int arrivalIndex;
    private final int arrivalTurn;
    private final Random choiceRng;
    private int satisfaction = MAX_SATISFACTION;
    private CustomerState state = CustomerState.WAITING;
    private Table table;
    private Order order;
    private int readyToPayTurn;
    private int recordedSatisfaction = -1;

    /**
     * Creates a customer who is about to join the queue with satisfaction 100.
     *
     * @param id           display id, for example "C1"
     * @param arrivalIndex 1-based position in the arrival order (also used for the food-choice seed)
     * @param arrivalTurn  the turn on which the customer arrives
     * @param gameSeed     the game seed
     */
    protected Customer(String id, int arrivalIndex, int arrivalTurn, long gameSeed) {
        this.id = id;
        this.arrivalIndex = arrivalIndex;
        this.arrivalTurn = arrivalTurn;
        this.choiceRng = new Random(choiceSeed(gameSeed, arrivalIndex));
    }

    /**
     * Computes the seed of a customer's private food-choice generator: {@code gameSeed * 1_000_003 + arrivalIndex}.
     *
     * @param gameSeed     the game seed
     * @param arrivalIndex the customer's 1-based arrival index
     * @return the seed
     */
    public static long choiceSeed(long gameSeed, int arrivalIndex) {
        return gameSeed * 1_000_003L + arrivalIndex;
    }

    /** @return a short type name for display, for example "Regular" */
    public abstract String typeLabel();

    /** @return satisfaction points lost per turn while waiting for a table or food */
    protected abstract int satisfactionLossPerTurn();

    /**
     * End-of-turn patience rule: a customer still waiting for a table or food loses its per-turn amount
     * (never below 0). Customers who are eating, ready to pay, paid or gone are unaffected.
     */
    public final void applyWaitingPenalty() {
        if (state.isAwaitingService()) {
            satisfaction = Math.max(0, satisfaction - satisfactionLossPerTurn());
        }
    }

    /**
     * Tells whether this customer is still waiting for service but has no satisfaction left, and so must leave.
     *
     * @return true if the customer should leave now
     */
    public final boolean hasRunOutOfPatience() {
        return state.isAwaitingService() && satisfaction == 0;
    }

    /**
     * Eating rule: a customer served on an earlier turn finishes eating and becomes READY_TO_PAY on this turn.
     *
     * @param turn the current turn
     * @return true if the customer just became READY_TO_PAY
     */
    public final boolean finishEatingIfDone(int turn) {
        if (state == CustomerState.EATING && order.getServedTurn() < turn) {
            state = CustomerState.READY_TO_PAY;
            readyToPayTurn = turn;
            return true;
        }
        return false;
    }

    /**
     * Tells whether the Cashier may charge this customer on the given turn
     * (READY_TO_PAY since an earlier turn).
     *
     * @param turn the current turn
     * @return true if the customer can pay now
     */
    public final boolean canPayOn(int turn) {
        return state == CustomerState.READY_TO_PAY && readyToPayTurn < turn;
    }

    /** Draws this customer's own food choice, uniformly over the menu's offerings. */
    MenuItem chooseFrom(Menu menu) {
        List<MenuItem> offerings = menu.getOfferings();
        return offerings.get(choiceRng.nextInt(offerings.size()));
    }

    void seatAt(Table assigned) {
        table = assigned;
        state = CustomerState.SEATED;
    }

    void attachOrder(Order placed) {
        order = placed;
        state = CustomerState.ORDERED;
    }

    void receiveServedOrder() {
        state = CustomerState.EATING;
    }

    void markPaid() {
        recordedSatisfaction = satisfaction;
        state = CustomerState.PAID;
        table = null;
    }

    void leave() {
        state = CustomerState.LEFT;
        table = null;
    }

    /** @return the display id, for example "C1" */
    public String getId() {
        return id;
    }

    /** @return the 1-based arrival index */
    public int getArrivalIndex() {
        return arrivalIndex;
    }

    /** @return the turn on which this customer arrived */
    public int getArrivalTurn() {
        return arrivalTurn;
    }

    /** @return current satisfaction, 0 to 100 */
    public int getSatisfaction() {
        return satisfaction;
    }

    /** @return the lifecycle state */
    public CustomerState getState() {
        return state;
    }

    /** @return the table, or null if not seated */
    public Table getTable() {
        return table;
    }

    /** @return the order, or null if no order has been taken */
    public Order getOrder() {
        return order;
    }

    /** @return the turn the customer became READY_TO_PAY, or 0 if not yet */
    public int getReadyToPayTurn() {
        return readyToPayTurn;
    }

    /** @return the satisfaction recorded at payment, or -1 if the customer has not paid */
    public int getRecordedSatisfaction() {
        return recordedSatisfaction;
    }

    @Override
    public String toString() {
        return id;
    }
}
