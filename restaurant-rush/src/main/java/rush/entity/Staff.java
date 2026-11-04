package rush.entity;

/**
 * A staff member with a limit of one task per turn. Tracks whether the task has been used this turn;
 * waiting also uses the turn. Failed (invalid) actions never use the task.
 */
public abstract class Staff {

    private final String role;
    private boolean actedThisTurn;

    /**
     * @param role display name of the role, for example "Waiter"
     */
    protected Staff(String role) {
        this.role = role;
    }

    /** Resets the per-turn task at the start of a turn. */
    public final void startTurn() {
        actedThisTurn = false;
    }

    /** @return true if this staff member has already acted or waited this turn */
    public final boolean hasActedThisTurn() {
        return actedThisTurn;
    }

    /**
     * Uses this turn's task by doing nothing.
     *
     * @return success, or failure if the task was already used
     */
    public final ActionResult waitTurn() {
        if (actedThisTurn) {
            return alreadyActed();
        }
        actedThisTurn = true;
        return ActionResult.ok(role + " waits this turn.");
    }

    /** @return the role name */
    public final String getRole() {
        return role;
    }

    /** Marks this turn's task as used; called only after an action succeeded. */
    protected final void markActed() {
        actedThisTurn = true;
    }

    /** @return the standard failure for a second task in one turn */
    protected final ActionResult alreadyActed() {
        return ActionResult.fail("The " + role + " has already used this turn's task.");
    }
}
