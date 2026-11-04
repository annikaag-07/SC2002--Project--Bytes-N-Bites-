package rush.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rush.entity.Customer;
import rush.entity.RegularCustomer;

/** Decides who arrives on which turn. Customers are created up front, numbered C1, C2, ... in arrival order. */
public final class ArrivalSchedule {

    /** Arrival turns of the base setting. */
    private static final int[] BASE_ARRIVAL_TURNS = {1, 4, 7, 10, 13, 16};

    private final List<Customer> customers;

    private ArrivalSchedule(List<Customer> customers) {
        this.customers = Collections.unmodifiableList(customers);
    }

    /**
     * The base setting: six Regular customers arriving on turns 1, 4, 7, 10, 13 and 16.
     *
     * @param seed the game seed
     * @return the schedule
     */
    public static ArrivalSchedule base(long seed) {
        return regulars(seed, BASE_ARRIVAL_TURNS);
    }

    /**
     * A schedule of Regular customers arriving on the given turns (non-decreasing; repeats allowed).
     *
     * @param seed  the game seed
     * @param turns arrival turns
     * @return the schedule
     */
    public static ArrivalSchedule regulars(long seed, int... turns) {
        List<Customer> list = new ArrayList<>();
        int previous = 1;
        for (int i = 0; i < turns.length; i++) {
            if (turns[i] < previous) {
                throw new IllegalArgumentException("arrival turns must be positive and non-decreasing");
            }
            previous = turns[i];
            int index = i + 1;
            list.add(new RegularCustomer("C" + index, index, turns[i], seed));
        }
        return new ArrivalSchedule(list);
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
}
