package rush.web;

import java.util.ArrayList;
import java.util.List;

/**
 * The web game's modes and what each one switches on. This is the only place that decides which features a
 * mode has; the rest of the adapter asks {@code mode.host()}, {@code mode.tips()} and so on, and the state
 * JSON passes the same flags to the front end, so the client draws only what the mode has.
 *
 * <p>The modes are cumulative and follow the assignment's stages: Stage 1 is the base game, Stage 2 adds VIP
 * and Critic customers, Stage 3 adds the Host, combo meals and Happy Hour, and Custom adds the web-only extras
 * (an Impatient guest on turn 10, Picky / Chatty / Grumpy regulars, Happy Hour tips). {@code classic} is an
 * API-only mode: Stage 3 with the terminal game's fixed arrival list, used by the parity test.</p>
 */
enum ModeFeatures {

    //          id         label      VIP+Critic host  combos happyHour tips   subTypes impatient randomArrivals
    STAGE1("stage1", "Stage 1", false, false, false, false, false, false, false, false),
    STAGE2("stage2", "Stage 2", true, false, false, false, false, false, false, true),
    STAGE3("stage3", "Stage 3", true, true, true, true, false, false, false, true),
    CUSTOM("custom", "Custom", true, true, true, true, true, true, true, true),
    CLASSIC("classic", "Classic", true, true, true, true, false, false, false, false);

    private final String id;
    private final String label;
    private final boolean vipAndCritic;
    private final boolean host;
    private final boolean combos;
    private final boolean happyHour;
    private final boolean tips;
    private final boolean subTypes;
    private final boolean impatient;
    private final boolean randomArrivals;

    ModeFeatures(String id, String label, boolean vipAndCritic, boolean host, boolean combos, boolean happyHour,
                 boolean tips, boolean subTypes, boolean impatient, boolean randomArrivals) {
        this.id = id;
        this.label = label;
        this.vipAndCritic = vipAndCritic;
        this.host = host;
        this.combos = combos;
        this.happyHour = happyHour;
        this.tips = tips;
        this.subTypes = subTypes;
        this.impatient = impatient;
        this.randomArrivals = randomArrivals;
    }

    /**
     * Looks up a mode by its API id.
     *
     * @param id e.g. "stage1"
     * @return the mode
     * @throws IllegalArgumentException for an unknown id (old "base" and "rush" included)
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
     * Display name for a mode id, including ids of older versions that may still be in the high-score file.
     *
     * @param id a mode id
     * @return e.g. "Stage 2", "Base" or the id itself
     */
    static String labelFor(String id) {
        if (isKnown(id)) {
            return fromId(id).label;
        }
        return switch (id) {
            case "base" -> "Base";
            case "rush" -> "Rush";
            default -> id;
        };
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

    /** VIP and Critic customers can arrive. */
    boolean vipAndCritic() {
        return vipAndCritic;
    }

    /** The Host (and their pedestal) is on duty. */
    boolean host() {
        return host;
    }

    /** The two combo meals are on the menu. */
    boolean combos() {
        return combos;
    }

    /** The manager can switch on Happy Hour. */
    boolean happyHour() {
        return happyHour;
    }

    /** Happy Hour tips at the cashier. */
    boolean tips() {
        return tips;
    }

    /** Regular customers roll Picky / Chatty / Grumpy sub-types. */
    boolean subTypes() {
        return subTypes;
    }

    /** An Impatient guest arrives on turn 10. */
    boolean impatient() {
        return impatient;
    }

    /** VIP and Critic arrivals come from the seeded random schedule ({@link ArrivalPlan}). */
    boolean randomArrivals() {
        return randomArrivals;
    }
}
