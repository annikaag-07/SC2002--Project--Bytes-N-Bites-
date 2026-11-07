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
     * Customer-specific pricing applied to a dish's menu price when the order is taken.
     * The default is full price; subclasses may override (for example a discount).
     *
     * @param priceCents the price before customer pricing
     * @return the price after customer pricing
     */
    public long applyCustomerPricing(long priceCents) {
        return priceCents;
    }

    /**
     * Hook called when this customer's food is served. The default is no reaction; subclasses may change
     * their own satisfaction here.
     *
     * @param turn the turn the food was served
     * @return a description of the reaction, or null if there is none
     */
    protected String onServed(int turn) {
        return null;
    }

    // ---- Optional behaviour hooks (added after stage 3 for the web edition's customer sub-types).
    // ---- Every default keeps the original behaviour, so Regular, VIP and Critic customers are unchanged.

    /**
     * Scales a positive satisfaction gain (Happy Hour recovery) before it is applied. Default: unchanged.
     *
     * @param points the gain the rules award
     * @return the gain actually applied
     */
    protected int scalePositiveGain(int points) {
        return points;
    }

    /**
     * Number of full turns this customer spends eating after being served. Default: 1 (served on turn t,
     * READY_TO_PAY on turn t+1).
     *
     * @return eating turns, at least 1
     */
    protected int eatingTurns() {
        return 1;
    }

    /**
     * Whether this customer sends the food back the moment it is served. If true, the order returns to the
     * counter as READY (servable again from the next turn) and the customer keeps waiting. Default: never.
     *
     * @param turn the turn the food was served
     * @return true to send the food back this time
     */
    protected boolean rejectsServedFood(int turn) {
        return false;
    }

    /**
     * Changes satisfaction by {@code delta}, kept between 0 and 100.
     *
     * @param delta points to add (negative to remove)
     */
    protected final void changeSatisfaction(int delta) {
        satisfaction = Math.max(0, Math.min(MAX_SATISFACTION, satisfaction + delta));
    }

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
     * Recovery rule (Happy Hour): a customer still waiting for a table or for food gains {@code points}
     * satisfaction, capped at 100. Customers who are eating, ready to pay, paid or gone are unaffected.
     *
     * @param points satisfaction to add
     * @return true if the customer was eligible (even if already at 100)
     */
    public final boolean recoverWhileWaiting(int points) {
        if (!state.isAwaitingService()) {
            return false;
        }
        changeSatisfaction(scalePositiveGain(points));
        return true;
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
        if (state == CustomerState.EATING && order.getServedTurn() + Math.max(1, eatingTurns()) <= turn) {
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

    /** Serving rule: the customer starts eating, then reacts in its own way. Returns the reaction, or null. */
    final String receiveServedOrder(int turn) {
        if (rejectsServedFood(turn)) {
            // Sent back: the dish returns to the counter and the customer keeps waiting (state stays ORDERED).
            order.sendBack(turn);
            return id + " sends the " + order.getItem().getName() + " back! It returns to the counter.";
        }
        state = CustomerState.EATING;
        return onServed(turn);
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
