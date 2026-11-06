package rush.web;

import java.util.ArrayList;
import java.util.List;

/**
 * The web game's modes. {@code stage3} is the game players start from the menu: the assignment's Stage 3 rules
 * (Regular, VIP and Critic customers, the Host, combo meals and Happy Hour) with a seeded random arrival plan
 * ({@link ArrivalPlan}). {@code classic} is an API-only mode with the same rules and the terminal game's fixed
 * arrival list, used by the parity test against the CLI transcripts; it never appears in the UI.
 */
enum ModeFeatures {

    STAGE3("stage3", "Stage 3", true),
    CLASSIC("classic", "Classic", false);

    private final String id;
    private final String label;
    private final boolean randomArrivals;

    ModeFeatures(String id, String label, boolean randomArrivals) {
        this.id = id;
        this.label = label;
        this.randomArrivals = randomArrivals;
    }

    /**
     * Looks up a mode by its API id.
     *
     * @param id e.g. "stage3"
     * @return the mode
     * @throws IllegalArgumentException for an unknown id
     */
    static ModeFeatures fromId(String id) {
        for (ModeFeatures mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown mode '" + id + "'. Use one of: " + String.join(", ", ids()) + ".");
    }

    /** @return true if {@code id} names a mode */
    static boolean isKnown(String id) {
        for (ModeFeatures mode : values()) {
            if (mode.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Display name for a mode id (as stored in the high-score file).
     *
     * @param id a mode id
     * @return e.g. "Stage 3", or the id itself if it is not a mode
     */
    static String labelFor(String id) {
        return isKnown(id) ? fromId(id).label : id;
    }

    private static List<String> ids() {
        List<String> ids = new ArrayList<>();
        for (ModeFeatures mode : values()) {
            ids.add(mode.id);
        }
        return ids;
    }

    String id() {
        return id;
    }

    String label() {
        return label;
    }

    /** VIP and Critic arrivals come from the seeded random schedule ({@link ArrivalPlan}); classic uses the fixed list. */
    boolean randomArrivals() {
        return randomArrivals;
    }
}
