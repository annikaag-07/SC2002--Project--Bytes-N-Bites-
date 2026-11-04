package rush.control;

/** The outcome check after the last turn: each victory criterion, actual against required. */
public final class GameResult {

    private final GameConfig config;
    private final int paidCount;
    private final long revenueCents;
    private final long satisfactionSum;

    private GameResult(GameConfig config, int paidCount, long revenueCents, long satisfactionSum) {
        this.config = config;
        this.paidCount = paidCount;
        this.revenueCents = revenueCents;
        this.satisfactionSum = satisfactionSum;
    }

    /**
     * Evaluates the victory criteria.
     *
     * @param config          thresholds
     * @param paidCount       customers who paid
     * @param revenueCents    revenue
     * @param satisfactionSum sum of satisfaction recorded at payment
     * @return the result
     */
    public static GameResult evaluate(GameConfig config, int paidCount, long revenueCents, long satisfactionSum) {
        return new GameResult(config, paidCount, revenueCents, satisfactionSum);
    }

    /**
     * Formats an average satisfaction to one decimal (rounded half up), or "N/A" if nobody has paid.
     *
     * @param sum   sum of recorded satisfaction
     * @param count number of paid customers
     * @return the formatted average
     */
    public static String formatAverage(long sum, int count) {
        if (count == 0) {
            return "N/A";
        }
        long tenths = (sum * 10 * 2 + count) / (2L * count);
        return (tenths / 10) + "." + (tenths % 10);
    }

    /** @return true if enough customers paid */
    public boolean isPaidCountMet() {
        return paidCount >= config.getMinPaidCustomers();
    }

    /** @return true if revenue reached the target */
    public boolean isRevenueMet() {
        return revenueCents >= config.getMinRevenueCents();
    }

    /** @return true if average recorded satisfaction reached the target (compared exactly, as sum >= target * count) */
    public boolean isSatisfactionMet() {
        return paidCount > 0 && satisfactionSum >= (long) config.getMinAverageSatisfaction() * paidCount;
    }

    /** @return true if all three criteria are met */
    public boolean isVictory() {
        return isPaidCountMet() && isRevenueMet() && isSatisfactionMet();
    }

    /** @return customers who paid */
    public int getPaidCount() {
        return paidCount;
    }

    /** @return revenue in cents */
    public long getRevenueCents() {
        return revenueCents;
    }

    /** @return the average recorded satisfaction, formatted, or "N/A" */
    public String getAverageText() {
        return formatAverage(satisfactionSum, paidCount);
    }

    /** @return the thresholds used */
    public GameConfig getConfig() {
        return config;
    }
}
