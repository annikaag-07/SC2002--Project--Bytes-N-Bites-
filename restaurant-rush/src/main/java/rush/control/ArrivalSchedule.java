package rush.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rush.entity.CriticCustomer;
import rush.entity.Customer;
import rush.entity.RegularCustomer;
import rush.entity.VIPCustomer;

/**
 * Decides who arrives on which turn. Customers are created up front, numbered C1, C2, ... in arrival order.
 * This is the only place that names concrete customer types; everything else works with {@link Customer}.
 */
public final class ArrivalSchedule {

    /** Creates the customer for one arrival slot; satisfied by a customer constructor reference. */
    @FunctionalInterface
    public interface CustomerFactory {
        /**
         * @param id           display id
         * @param arrivalIndex 1-based arrival index
         * @param arrivalTurn  arrival turn
         * @param gameSeed     game seed
         * @return the new customer
         */
        Customer create(String id, int arrivalIndex, int arrivalTurn, long gameSeed);
    }

    private final List<Customer> customers;

    private ArrivalSchedule(List<Customer> customers) {
        this.customers = Collections.unmodifiableList(customers);
    }

    /**
     * The base setting: six arrivals on turns 1, 4, 7, 10, 13 and 16 -
     * Regular, VIP, Regular, Critic, Regular, Regular.
     *
     * @param seed the game seed
     * @return the schedule
     */
    public static ArrivalSchedule base(long seed) {
        return builder(seed)
                .add(1, RegularCustomer::new)
                .add(4, VIPCustomer::new)
                .add(7, RegularCustomer::new)
                .add(10, CriticCustomer::new)
                .add(13, RegularCustomer::new)
                .add(16, RegularCustomer::new)
                .build();
    }

    /**
     * A schedule of Regular customers arriving on the given turns (non-decreasing; repeats allowed).
     *
     * @param seed  the game seed
     * @param turns arrival turns
     * @return the schedule
     */
    public static ArrivalSchedule regulars(long seed, int... turns) {
        Builder builder = builder(seed);
        for (int turn : turns) {
            builder.add(turn, RegularCustomer::new);
        }
        return builder.build();
    }

    /**
     * Starts building a custom schedule.
     *
     * @param seed the game seed
     * @return a builder
     */
    public static Builder builder(long seed) {
        return new Builder(seed);
    }

    /**
     * @param turn a turn number
     * @return the customers arriving at the start of that turn, in arrival order
     */
    public List<Customer> arrivalsOn(int turn) {
        List<Customer> arriving = new ArrayList<>();
        for (Customer customer : customers) {
            if (customer.getArrivalTurn() == turn) {
                arriving.add(customer);
            }
        }
        return arriving;
    }

    /** @return every scheduled customer in arrival order (unmodifiable) */
    public List<Customer> getCustomers() {
        return customers;
    }

    /** Builds a schedule slot by slot, in arrival order. */
    public static final class Builder {

        private final long seed;
        private final List<Customer> customers = new ArrayList<>();
        private int lastTurn = 1;

        private Builder(long seed) {
            this.seed = seed;
        }

        /**
         * Adds the next arrival.
         *
         * @param turn    arrival turn, not earlier than the previous one
         * @param factory creates the customer, for example {@code VIPCustomer::new}
         * @return this builder
         */
        public Builder add(int turn, CustomerFactory factory) {
            if (turn < lastTurn) {
                throw new IllegalArgumentException("arrival turns must be positive and non-decreasing");
            }
            lastTurn = turn;
            int index = customers.size() + 1;
            customers.add(factory.create("C" + index, index, turn, seed));
            return this;
        }

        /** @return the finished schedule */
        public ArrivalSchedule build() {
            return new ArrivalSchedule(new ArrayList<>(customers));
        }
    }
}
