package rush.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Who arrives in each of the six arrival slots of a web game. A pure function of the seed: the same seed always
 * gives the same list, so a replayed save (Continue) gets the same customers.
 *
 * <p>Each slot is drawn as Regular 50 / VIP 25 / Critic 25, then the draw is repaired so that every game has at least
 * one VIP, at least one Critic and at least two Regulars (the brief requires a VIP and a Critic in every reproducible
 * run; at most four of the six are not Regulars). The arrival turns never change.</p>
 */
final class ArrivalPlan {

    /** What arrives in a slot. */
    enum Kind { REGULAR, VIP, CRITIC }

    /** Arrival turns of the six slots (the brief's base setting). */
    static final int[] ARRIVAL_TURNS = {1, 4, 7, 10, 13, 16};

    /** Draw weights for each slot. */
    static final int WEIGHT_REGULAR = 50;
    static final int WEIGHT_VIP = 25;
    static final int WEIGHT_CRITIC = 25;

    /** Invariants after the repair. */
    static final int MIN_VIPS = 1;
    static final int MIN_CRITICS = 1;
    static final int MIN_REGULARS = 2;

    private ArrivalPlan() {
    }

    /**
     * Separate generator for the schedule, so it never disturbs the customers' own food-choice generators.
     */
    static Random scheduleRandom(long gameSeed) {
        return new Random(gameSeed * 1_000_039L + 0xA221_7AL);
    }

    /**
     * The six arrivals of a game.
     *
     * @param seed game seed
     * @return one kind per slot, in arrival order
     */
    static List<Kind> kinds(long seed) {
        List<Kind> kinds = new ArrayList<>(Collections.nCopies(ARRIVAL_TURNS.length, Kind.REGULAR));
        Random rng = scheduleRandom(seed);
        int total = WEIGHT_REGULAR + WEIGHT_VIP + WEIGHT_CRITIC;
        for (int i = 0; i < kinds.size(); i++) {
            int r = rng.nextInt(total);
            kinds.set(i, r < WEIGHT_REGULAR ? Kind.REGULAR : r < WEIGHT_REGULAR + WEIGHT_VIP ? Kind.VIP : Kind.CRITIC);
        }
        repair(kinds, rng);
        return List.copyOf(kinds);
    }

    /**
     * Fixes a draw that breaks an invariant by changing randomly chosen slots that have a surplus kind. Each
     * change removes one shortfall without creating another, so the loop ends.
     */
    private static void repair(List<Kind> kinds, Random rng) {
        while (true) {
            if (count(kinds, Kind.VIP) < MIN_VIPS) {
                replaceOne(kinds, rng, Kind.VIP, Kind.REGULAR, MIN_REGULARS, Kind.CRITIC, MIN_CRITICS);
            } else if (count(kinds, Kind.CRITIC) < MIN_CRITICS) {
                replaceOne(kinds, rng, Kind.CRITIC, Kind.REGULAR, MIN_REGULARS, Kind.VIP, MIN_VIPS);
            } else if (count(kinds, Kind.REGULAR) < MIN_REGULARS) {
                replaceOne(kinds, rng, Kind.REGULAR, Kind.VIP, MIN_VIPS, Kind.CRITIC, MIN_CRITICS);
            } else {
                return;
            }
        }
    }

    /** Turns one slot holding {@code a} (if above {@code minA}) or {@code b} (if above {@code minB}) into {@code into}. */
    private static void replaceOne(List<Kind> kinds, Random rng, Kind into, Kind a, int minA, Kind b, int minB) {
        boolean takeA = count(kinds, a) > minA;
        boolean takeB = count(kinds, b) > minB;
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < kinds.size(); i++) {
            Kind k = kinds.get(i);
            if ((takeA && k == a) || (takeB && k == b)) {
                candidates.add(i);
            }
        }
        kinds.set(candidates.get(rng.nextInt(candidates.size())), into);
    }

    static int count(List<Kind> kinds, Kind kind) {
        int n = 0;
        for (Kind k : kinds) {
            n += k == kind ? 1 : 0;
        }
        return n;
    }
}
