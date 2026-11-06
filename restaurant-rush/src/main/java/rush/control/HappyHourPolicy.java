package rush.control;

import java.util.List;
import java.util.StringJoiner;
import rush.entity.ActionResult;
import rush.entity.Customer;
import rush.entity.Money;

/**
 * The Happy Hour pricing policy. The manager may switch it on once per game, at the start of a turn; it is
 * active on that turn and the next. While active, orders taken get 20% off, and at the start of each active
 * turn every customer still waiting for a table or food recovers 15 satisfaction (capped at 100).
 */
public final class HappyHourPolicy {

    /** Number of turns Happy Hour lasts, including the activation turn. */
    public static final int DURATION_TURNS = 2;
    /** Satisfaction recovered at the start of each active turn. */
    public static final int RECOVERY_POINTS = 15;
    private static final int PERCENT_PAID = 80;

    private int activationTurn;

    /** @return true if Happy Hour has not been used yet */
    public boolean isAvailable() {
        return activationTurn == 0;
    }

    /**
     * Switches Happy Hour on for this turn and the next.
     *
     * @param turn the current turn
     * @return success, or failure if it was already used this game
     */
    public ActionResult activate(int turn) {
        if (!isAvailable()) {
            return ActionResult.fail("Happy Hour can only be activated once per game (it was activated on turn "
                    + activationTurn + ").");
        }
        activationTurn = turn;
        return ActionResult.ok("Happy Hour activated on turn " + turn + ": orders taken on turns " + turn + " and "
                + (turn + 1) + " get 20% off.");
    }

    /**
     * @param turn a turn number
     * @return true if Happy Hour is active on that turn
     */
    public boolean isActive(int turn) {
        return activationTurn > 0 && turn >= activationTurn && turn < activationTurn + DURATION_TURNS;
    }

    /**
     * @param turn       the current turn
     * @param totalTurns the game length (Happy Hour activated on the last turn covers one turn only)
     * @return active turns left including this one, or 0 if not active
     */
    public int remainingTurns(int turn, int totalTurns) {
        if (!isActive(turn)) {
            return 0;
        }
        int lastActiveTurn = Math.min(activationTurn + DURATION_TURNS - 1, totalTurns);
        return lastActiveTurn - turn + 1;
    }

    /**
     * Applies the Happy Hour discount to a price if the order is taken while it is active.
     *
     * @param priceCents price after any customer pricing
     * @param turn       the turn the order is taken
     * @return the discounted price (80%, rounded half up), or the price unchanged
     */
    public long applyDiscount(long priceCents, int turn) {
        return isActive(turn) ? Money.percent(priceCents, PERCENT_PAID) : priceCents;
    }

    /**
     * Start-of-turn recovery: every customer still waiting for a table or food gains 15 (max 100).
     *
     * @param customers every customer who has arrived
     * @return a description of who recovered, for the event log
     */
    public String applyRecovery(List<Customer> customers) {
        StringJoiner changes = new StringJoiner(", ");
        for (Customer customer : customers) {
            int before = customer.getSatisfaction();
            if (customer.recoverWhileWaiting(RECOVERY_POINTS)) {
                changes.add(customer.getId() + " " + before + " -> " + customer.getSatisfaction());
            }
        }
        return "Happy Hour recovery +" + RECOVERY_POINTS + " for customers still waiting: "
                + (changes.length() == 0 ? "nobody waiting" : changes.toString()) + ".";
    }
}
