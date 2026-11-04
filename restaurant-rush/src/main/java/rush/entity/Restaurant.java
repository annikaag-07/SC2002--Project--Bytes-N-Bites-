package rush.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The restaurant floor. It owns its tables (composition), the waiting queue, every customer who has
 * arrived, every order taken and the revenue collected. Seating, payment and departure rules that touch
 * several of these objects at once live here.
 */
public final class Restaurant {

    private final List<Table> tables = new ArrayList<>();
    private final List<Customer> queue = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private long revenueCents;
    private int nextOrderNumber = 1;

    /**
     * Creates a restaurant with tables T1..Tn.
     *
     * @param tableCount number of tables, at least 1
     */
    public Restaurant(int tableCount) {
        if (tableCount < 1) {
            throw new IllegalArgumentException("need at least one table");
        }
        for (int i = 1; i <= tableCount; i++) {
            tables.add(new Table("T" + i));
        }
    }

    /**
     * Arrival rule: a new customer joins the end of the waiting queue.
     *
     * @param customer a customer who has not arrived before
     */
    public void admitArrival(Customer customer) {
        if (customers.contains(customer) || customer.getState() != CustomerState.WAITING) {
            throw new IllegalArgumentException(customer.getId() + " cannot arrive twice");
        }
        customers.add(customer);
        queue.add(customer);
    }

    /** Seating rule shared by every staff member who can seat: a queued customer goes to a free table. */
    ActionResult seatCustomer(String customerId, String tableId) {
        Optional<Customer> found = findCustomer(customerId);
        if (found.isEmpty()) {
            return ActionResult.fail("There is no customer '" + customerId + "'.");
        }
        Customer customer = found.get();
        if (!queue.contains(customer)) {
            return ActionResult.fail(customer.getId() + " is not in the waiting queue (state "
                    + customer.getState() + ").");
        }
        if (freeTables().isEmpty()) {
            return ActionResult.fail("No table is free.");
        }
        Optional<Table> table = findTable(tableId);
        if (table.isEmpty()) {
            return ActionResult.fail("There is no table '" + tableId + "'.");
        }
        if (!table.get().isFree()) {
            return ActionResult.fail(table.get().getId() + " is occupied by " + table.get().getOccupant().getId() + ".");
        }
        queue.remove(customer);
        table.get().occupy(customer);
        customer.seatAt(table.get());
        return ActionResult.ok(customer.getId() + " at " + table.get().getId() + ".");
    }

    Order createOrder(Customer customer, MenuItem item, int turn) {
        Order order = new Order("O" + nextOrderNumber++, customer, item, turn);
        orders.add(order);
        customer.attachOrder(order);
        return order;
    }

    void recordPayment(Customer customer) {
        Order order = customer.getOrder();
        Table table = customer.getTable();
        revenueCents += order.getLockedPriceCents();
        order.markPaid();
        customer.markPaid();
        table.release();
    }

    /**
     * Abandonment rule: a customer who ran out of patience leaves the queue or their table, and any order
     * they placed is cancelled whatever its progress, so it never earns revenue.
     *
     * @param customer a customer for whom {@link Customer#hasRunOutOfPatience()} is true
     * @return a description of what was cancelled and freed
     */
    public String removeAbandoned(Customer customer) {
        if (!customer.hasRunOutOfPatience()) {
            throw new IllegalStateException(customer.getId() + " has not run out of patience");
        }
        StringBuilder text = new StringBuilder(customer.getId() + " (" + customer.typeLabel()
                + ") leaves unhappy without paying.");
        queue.remove(customer);
        Order order = customer.getOrder();
        if (order != null) {
            text.append(" Order ").append(order.getId()).append(" (").append(order.getItem().getName())
                    .append(", ").append(order.getStatus()).append(") cancelled.");
            order.cancel();
        }
        Table table = customer.getTable();
        if (table != null) {
            table.release();
            text.append(" Table ").append(table.getId()).append(" freed.");
        }
        customer.leave();
        return text.toString();
    }

    /**
     * Finds a customer who has arrived, ignoring case.
     *
     * @param id customer id, for example "C1"
     * @return the customer, if any
     */
    public Optional<Customer> findCustomer(String id) {
        return customers.stream().filter(c -> c.getId().equalsIgnoreCase(id)).findFirst();
    }

    /**
     * Finds a table, ignoring case.
     *
     * @param id table id, for example "T1"
     * @return the table, if any
     */
    public Optional<Table> findTable(String id) {
        return tables.stream().filter(t -> t.getId().equalsIgnoreCase(id)).findFirst();
    }

    /**
     * Finds an order, ignoring case.
     *
     * @param id order id, for example "O1"
     * @return the order, if any
     */
    public Optional<Order> findOrder(String id) {
        return orders.stream().filter(o -> o.getId().equalsIgnoreCase(id)).findFirst();
    }

    /** @return free tables in table order */
    public List<Table> freeTables() {
        List<Table> free = new ArrayList<>();
        for (Table table : tables) {
            if (table.isFree()) {
                free.add(table);
            }
        }
        return Collections.unmodifiableList(free);
    }

    /** @return all tables in order (unmodifiable) */
    public List<Table> getTables() {
        return Collections.unmodifiableList(tables);
    }

    /** @return the waiting queue, head first (unmodifiable) */
    public List<Customer> getQueue() {
        return Collections.unmodifiableList(queue);
    }

    /** @return every customer who has arrived, in arrival order (unmodifiable) */
    public List<Customer> getCustomers() {
        return Collections.unmodifiableList(customers);
    }

    /** @return every order taken, in the order taken (unmodifiable) */
    public List<Order> getOrders() {
        return Collections.unmodifiableList(orders);
    }

    /** @return revenue from paid orders, in cents */
    public long getRevenueCents() {
        return revenueCents;
    }

    /** @return the number of customers who have paid */
    public int getPaidCount() {
        return countInState(CustomerState.PAID);
    }

    /** @return the number of customers who left unhappy */
    public int getDepartureCount() {
        return countInState(CustomerState.LEFT);
    }

    /** @return the number of customers who have ever been served */
    public int getServedCount() {
        int count = 0;
        for (Order order : orders) {
            if (order.getServedTurn() > 0) {
                count++;
            }
        }
        return count;
    }

    /** @return the sum of the satisfaction recorded at payment over all paid customers */
    public long getRecordedSatisfactionSum() {
        long sum = 0;
        for (Customer customer : customers) {
            if (customer.getState() == CustomerState.PAID) {
                sum += customer.getRecordedSatisfaction();
            }
        }
        return sum;
    }

    private int countInState(CustomerState state) {
        int count = 0;
        for (Customer customer : customers) {
            if (customer.getState() == state) {
                count++;
            }
        }
        return count;
    }
}
