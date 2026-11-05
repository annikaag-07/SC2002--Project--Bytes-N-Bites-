package rush.web;

import java.util.ArrayList;
import java.util.List;

/**
 * The web game's mode: {@code stage2}, the assignment's Stage 2 rules (Regular, VIP and Critic customers, Waiter,
 * Chef and Cashier) with a seeded random arrival plan ({@link ArrivalPlan}). Kept as an enum so the API, the save
 * file and the high-score file name the mode the same way.
 */
enum ModeFeatures {

    STAGE2("stage2", "Stage 2");

    private final String id;
    private final String label;

    ModeFeatures(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /**
     * Looks up a mode by its API id.
     *
     * @param id e.g. "stage2"
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
     * @return e.g. "Stage 2", or the id itself if it is not a mode
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
}
