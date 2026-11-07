package rush.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import rush.entity.RegularCustomer;

/**
 * Normal-customer sub-types for the web edition, and the one place their numbers can be tuned.
 *
 * <p>Only Regular ("Normal") arrivals are rolled; VIP, Critic and Impatient customers are never touched. The
 * roll happens once per game, when the arrival schedule is built, from a generator seeded by the game seed,
 * so a given game (and a replayed save) always gets the same sub-types. Each sub-type is a tiny subclass of
 * the unchanged {@link RegularCustomer} that overrides one of the back end's optional hooks.</p>
 */
final class CustomerSubtypes {

    /** The four Normal sub-types. NORMAL behaves exactly like today's Regular customer. */
    enum SubType {
        NORMAL, PICKY, CHATTY, GRUMPY;

        String id() {
            return name().toLowerCase();
        }
    }

    // ------------------------------------------------------------------ configuration

    /** Normal customers in the base setting (Regulars on turns 1, 7, 13, 16). The real count is read from the schedule. */
    static final int NORMAL_CUSTOMER_COUNT = 4;

    /** Odds, in percent, that exactly 0, 1, 2, 3 or 4 of the Normal customers are specialised. */
    static final int[] SPECIALISED_COUNT_ODDS = {10, 60, 20, 7, 3};

    /** Grumpy customers start with a satisfaction from this range (inclusive) instead of 100. */
    static final int GRUMPY_START_MIN = 80;
    static final int GRUMPY_START_MAX = 89;

    /** Multiplier applied to a Grumpy customer's positive satisfaction gains (capped at the usual maximum). */
    static final int GRUMPY_GAIN_MULTIPLIER = 2;

    /** Extra turn a Chatty customer stays at the table after eating (on top of the usual one). */
    static final int CHATTY_EATING_TURNS = 2;

    // ---- Happy Hour tips (see TipJar logic in GameSession)

    /** A tip is at most this percentage of the final (discounted) bill. */
    static final int TIP_MAX_PERCENT = 20;

    /** A tip needs satisfaction at payment strictly above this. */
    static final int TIP_MIN_SATISFACTION = 80;

    /** Whether tips count towards the $45 revenue target. Off: tips are shown but victory is unchanged. */
    static final boolean TIPS_COUNT_TOWARDS_REVENUE_TARGET = false;

    private CustomerSubtypes() {
    }

    /** Separate generators so the rolls never disturb the customers' own food-choice generators. */
    static Random subtypeRandom(long gameSeed) {
        return new Random(gameSeed * 1_000_033L + 0x5EB7_7E5L);
    }

    static Random tipRandom(long gameSeed) {
        return new Random(gameSeed * 1_000_037L + 0x71B5L);
    }

    /**
     * Rolls the sub-types of {@code normalCount} Normal customers together: first how many are specialised
     * (SPECIALISED_COUNT_ODDS, capped at normalCount), then which ones, then each one's sub-type with equal odds.
     *
     * @param normalCount number of Normal customers in this game
     * @param rng         the game's sub-type generator
     * @return one sub-type per Normal customer, in arrival order
     */
    static List<SubType> roll(int normalCount, Random rng) {
        int specialised = Math.min(normalCount, pickCount(rng));
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < normalCount; i++) {
            slots.add(i);
        }
        Collections.shuffle(slots, rng);
        List<SubType> result = new ArrayList<>(Collections.nCopies(normalCount, SubType.NORMAL));
        SubType[] special = {SubType.PICKY, SubType.CHATTY, SubType.GRUMPY};
        for (int i = 0; i < specialised; i++) {
            result.set(slots.get(i), special[rng.nextInt(special.length)]);
        }
        return result;
    }

    private static int pickCount(Random rng) {
        int total = 0;
        for (int odds : SPECIALISED_COUNT_ODDS) {
            total += odds;
        }
        int r = rng.nextInt(total);
        for (int k = 0; k < SPECIALISED_COUNT_ODDS.length; k++) {
            r -= SPECIALISED_COUNT_ODDS[k];
            if (r < 0) {
                return k;
            }
        }
        return 0;
    }

    // ------------------------------------------------------------------ the sub-types

    /** Sends the food back the moment it is first served; accepts the second serve. */
    static final class PickyCustomer extends RegularCustomer {
        private boolean sentBack;

        PickyCustomer(String id, int arrivalIndex, int arrivalTurn, long gameSeed) {
            super(id, arrivalIndex, arrivalTurn, gameSeed);
        }

        @Override
        protected boolean rejectsServedFood(int turn) {
            if (sentBack) {
                return false;
            }
            sentBack = true;
            return true;
        }
    }

    /** Stays at the table for one extra turn after eating, then pays as usual. */
    static final class ChattyCustomer extends RegularCustomer {
        ChattyCustomer(String id, int arrivalIndex, int arrivalTurn, long gameSeed) {
            super(id, arrivalIndex, arrivalTurn, gameSeed);
        }

        @Override
        protected int eatingTurns() {
            return CHATTY_EATING_TURNS;
        }
    }

    /** Starts at 80-89 satisfaction; positive gains are multiplied (capped at 100). Losses are unchanged. */
    static final class GrumpyCustomer extends RegularCustomer {
        GrumpyCustomer(String id, int arrivalIndex, int arrivalTurn, long gameSeed, int startSatisfaction) {
            super(id, arrivalIndex, arrivalTurn, gameSeed);
            changeSatisfaction(startSatisfaction - getSatisfaction());
        }

        @Override
        protected int scalePositiveGain(int points) {
            return points * GRUMPY_GAIN_MULTIPLIER;
        }
    }
}
