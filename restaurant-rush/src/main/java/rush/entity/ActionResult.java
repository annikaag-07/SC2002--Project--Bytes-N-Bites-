package rush.entity;

/**
 * Outcome of a manager command or staff action: success or failure, with a human-readable message.
 * A failure means nothing changed and the staff member's task is still available.
 */
public final class ActionResult {

    private final boolean success;
    private final String message;

    private ActionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * Creates a successful result.
     *
     * @param message what happened
     * @return the result
     */
    public static ActionResult ok(String message) {
        return new ActionResult(true, message);
    }

    /**
     * Creates a failed result.
     *
     * @param reason why the action was rejected
     * @return the result
     */
    public static ActionResult fail(String reason) {
        return new ActionResult(false, reason);
    }

    /** @return true if the action was carried out */
    public boolean isSuccess() {
        return success;
    }

    /** @return what happened, or why it was rejected */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return (success ? "OK: " : "FAILED: ") + message;
    }
}
