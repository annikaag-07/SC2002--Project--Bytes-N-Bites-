package rush.control;

/** Something that happened during a turn, reported to the player. */
public final class GameEvent {

    private final int turn;
    private final String text;

    /**
     * @param turn the turn it happened on
     * @param text what happened
     */
    public GameEvent(int turn, String text) {
        this.turn = turn;
        this.text = text;
    }

    /** @return the turn */
    public int getTurn() {
        return turn;
    }

    /** @return the description */
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "[T" + turn + "] " + text;
    }
}
