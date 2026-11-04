package rush.entity;

/**
 * Helper for money held as integer cents. Every amount in the game is a {@code long} number of cents;
 * this class formats it as dollars and applies percentages with half-up rounding.
 */
public final class Money {

    private Money() {
    }

    /**
     * Formats an amount of cents as dollars with exactly two decimals, for example 2430 as "$24.30".
     *
     * @param cents the amount in cents
     * @return the formatted dollar string
     */
    public static String format(long cents) {
        String sign = cents < 0 ? "-" : "";
        long abs = Math.abs(cents);
        return sign + "$" + (abs / 100) + "." + String.format("%02d", abs % 100);
    }

    /**
     * Returns {@code percent}% of an amount, rounded half up to the nearest cent: {@code (cents * percent + 50) / 100}.
     *
     * @param cents   a non-negative amount in cents
     * @param percent a non-negative percentage, for example 90 for "90% of"
     * @return the rounded result in cents
     */
    public static long percent(long cents, int percent) {
        if (cents < 0 || percent < 0) {
            throw new IllegalArgumentException("cents and percent must be non-negative");
        }
        return (cents * percent + 50) / 100;
    }
}
