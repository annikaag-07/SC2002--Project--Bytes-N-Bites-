package rush.control;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import rush.entity.ActionResult;
import rush.entity.Cashier;
import rush.entity.Chef;
import rush.entity.Customer;
import rush.entity.CustomerState;
import rush.entity.Menu;
import rush.entity.Money;
import rush.entity.Order;
import rush.entity.Restaurant;
import rush.entity.Table;
import rush.entity.Waiter;

/**
 * The turn engine. It runs the turn steps in their fixed order, accepts the manager's commands for each
 * staff member, enforces "Waiter before Chef" and one task per staff member per turn, and exposes read-only
 * state and the valid targets for each task. It never reads input or prints.
 *
 * <p>A turn is driven as: {@link #beginTurn()} (arrivals), one Waiter command, one Chef command,
 * then {@link #endTurn()} (payment, eating, waiting and abandonment).</p>
 */
public final class GameController {

    private final GameConfig config;
    private final ArrivalSchedule schedule;
    private final Restaurant restaurant;
    private final BillCalculator billCalculator = new BillCalculator();
    private final Waiter waiter = new Waiter();
    private final Chef chef = new Chef();
    private final Cashier cashier = new Cashier();
    private final List<GameEvent> eventLog = new ArrayList<>();
    private int currentTurn;
    private boolean turnInProgress;

    /**
     * Creates a game that has not started yet.
     *
     * @param config   settings and victory thresholds
     * @param schedule who arrives when
     */
    public GameController(GameConfig config, ArrivalSchedule schedule) {
        this.config = config;
        this.schedule = schedule;
        this.restaurant = new Restaurant(config.getTableCount());
    }

    // ---------------------------------------------------------------- turn steps

    /**
     * Starts the next turn: resets every staff member's task and runs step 1, arrivals.
     *
     * @return the arrival events
     */
    public List<GameEvent> beginTurn() {
        if (turnInProgress || isGameOver()) {
            throw new IllegalStateException("cannot begin a turn now");
        }
        currentTurn++;
        turnInProgress = true;
        waiter.startTurn();
        chef.startTurn();
        cashier.startTurn();
        List<GameEvent> events = new ArrayList<>();
        for (Customer customer : schedule.arrivalsOn(currentTurn)) {
            restaurant.admitArrival(customer);
            events.add(event(customer.getId() + " (" + customer.typeLabel()
                    + ") arrives and joins the waiting queue with satisfaction " + customer.getSatisfaction() + "."));
        }
        eventLog.addAll(events);
        return events;
    }

    /**
     * Waiter command: seat a queued customer at a free table.
     *
     * @param customerId any customer in the queue
     * @param tableId    a free table
     * @return the outcome; on failure the Waiter's task is still available
     */
    public ActionResult waiterSeat(String customerId, String tableId) {
        ActionResult turnCheck = checkWaiterMayAct();
        return turnCheck != null ? turnCheck : record(waiter.seat(restaurant, customerId, tableId));
    }

    /**
     * Waiter command: take a seated customer's order. The customer chooses the dish and the price is locked.
     *
     * @param customerId a SEATED customer
     * @return the outcome, including the chosen dish and locked price
     */
    public ActionResult waiterTakeOrder(String customerId) {
        ActionResult turnCheck = checkWaiterMayAct();
        if (turnCheck != null) {
            return turnCheck;
        }
        ActionResult taken = waiter.takeOrder(restaurant, customerId, config.getMenu(), currentTurn);
        if (!taken.isSuccess()) {
            return taken;
        }
        Order order = restaurant.findCustomer(customerId).orElseThrow().getOrder();
        long price = billCalculator.lockPrice(order, currentTurn);
        return record(ActionResult.ok(taken.getMessage() + " Locked price: " + Money.format(price) + "."));
    }

    /**
     * Waiter command: serve an order that was READY before this turn.
     *
     * @param orderId a servable order
     * @return the outcome
     */
    public ActionResult waiterServe(String orderId) {
        ActionResult turnCheck = checkWaiterMayAct();
        return turnCheck != null ? turnCheck : record(waiter.serve(restaurant, orderId, currentTurn));
    }

    /**
     * Waiter command: do nothing this turn.
     *
     * @return the outcome
     */
    public ActionResult waiterWait() {
        ActionResult turnCheck = checkWaiterMayAct();
        return turnCheck != null ? turnCheck : record(waiter.waitTurn());
    }

    /**
     * Chef command: advance one order by one preparation unit. Only allowed after the Waiter has acted.
     *
     * @param orderId a PENDING or PREPARING order
     * @return the outcome
     */
    public ActionResult chefAdvance(String orderId) {
        ActionResult turnCheck = checkChefMayAct();
        return turnCheck != null ? turnCheck : record(chef.advance(restaurant, orderId, currentTurn));
    }

    /**
     * Chef command: do nothing this turn.
     *
     * @return the outcome
     */
    public ActionResult chefWait() {
        ActionResult turnCheck = checkChefMayAct();
        return turnCheck != null ? turnCheck : record(chef.waitTurn());
    }

    /**
     * Finishes the turn after the Waiter and Chef have acted: step 4 (Cashier, then eating) and
     * step 5 (waiting penalties and abandonment).
     *
     * @return the events of these steps
     */
    public List<GameEvent> endTurn() {
        if (!turnInProgress || !waiter.hasActedThisTurn() || !chef.hasActedThisTurn()) {
            throw new IllegalStateException("the Waiter and Chef must act (or wait) before the turn can end");
        }
        List<GameEvent> events = new ArrayList<>();
        // Step 4a: the Cashier charges at most one customer who became READY_TO_PAY on an earlier turn.
        ActionResult payment = cashier.collectPayment(restaurant, currentTurn);
        events.add(event(payment.getMessage()));
        // Step 4b: customers served on an earlier turn finish eating.
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.finishEatingIfDone(currentTurn)) {
                events.add(event(customer.getId() + " finishes eating: READY_TO_PAY (can pay from next turn)."));
            }
        }
        // Step 5: everyone still waiting for a table or food loses satisfaction; at 0 they leave.
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.getState().isAwaitingService()) {
                int before = customer.getSatisfaction();
                customer.applyWaitingPenalty();
                events.add(event(customer.getId() + " (" + customer.getState() + ") waits: satisfaction "
                        + before + " -> " + customer.getSatisfaction() + "."));
            }
        }
        for (Customer customer : new ArrayList<>(restaurant.getCustomers())) {
            if (customer.hasRunOutOfPatience()) {
                events.add(event(restaurant.removeAbandoned(customer)));
            }
        }
        eventLog.addAll(events);
        turnInProgress = false;
        return events;
    }

    // ---------------------------------------------------------------- queries for display

    /** @return true once the last turn has ended */
    public boolean isGameOver() {
        return currentTurn >= config.getTotalTurns() && !turnInProgress;
    }

    /**
     * @return the victory/defeat evaluation
     * @throws IllegalStateException before the last turn has ended
     */
    public GameResult getResult() {
        if (!isGameOver()) {
            throw new IllegalStateException("the result is only decided after the last turn");
        }
        return GameResult.evaluate(config, restaurant.getPaidCount(), restaurant.getRevenueCents(),
                restaurant.getRecordedSatisfactionSum());
    }

    /** @return the current turn number (0 before the first turn) */
    public int getCurrentTurn() {
        return currentTurn;
    }

    /** @return total turns in the game */
    public int getTotalTurns() {
        return config.getTotalTurns();
    }

    /** @return the game seed */
    public long getSeed() {
        return config.getSeed();
    }

    /** @return the menu */
    public Menu getMenu() {
        return config.getMenu();
    }

    /** @return revenue in cents */
    public long getRevenueCents() {
        return restaurant.getRevenueCents();
    }

    /** @return average satisfaction recorded at payment, formatted, or "N/A" */
    public String getAverageSatisfactionText() {
        return GameResult.formatAverage(restaurant.getRecordedSatisfactionSum(), restaurant.getPaidCount());
    }

    /** @return customers ever served */
    public int getServedCount() {
        return restaurant.getServedCount();
    }

    /** @return customers who left unhappy */
    public int getDepartureCount() {
        return restaurant.getDepartureCount();
    }

    /** @return customers who paid */
    public int getPaidCount() {
        return restaurant.getPaidCount();
    }

    /** @return the waiting queue, head first */
    public List<Customer> getQueue() {
        return restaurant.getQueue();
    }

    /** @return all tables */
    public List<Table> getTables() {
        return restaurant.getTables();
    }

    /** @return every customer who has arrived, in arrival order */
    public List<Customer> getCustomers() {
        return restaurant.getCustomers();
    }

    /** @return every order taken */
    public List<Order> getOrders() {
        return restaurant.getOrders();
    }

    /** @return free tables (valid targets for seating) */
    public List<Table> getFreeTables() {
        return restaurant.freeTables();
    }

    /** @return SEATED customers (valid targets for taking an order) */
    public List<Customer> getCustomersAwaitingOrder() {
        List<Customer> seated = new ArrayList<>();
        for (Customer customer : restaurant.getCustomers()) {
            if (customer.getState() == CustomerState.SEATED) {
                seated.add(customer);
            }
        }
        return Collections.unmodifiableList(seated);
    }

    /** @return orders the Waiter may serve this turn */
    public List<Order> getServableOrders() {
        List<Order> servable = new ArrayList<>();
        for (Order order : restaurant.getOrders()) {
            if (order.isServableOn(currentTurn)) {
                servable.add(order);
            }
        }
        return Collections.unmodifiableList(servable);
    }

    /** @return orders the Chef may work on */
    public List<Order> getPreparableOrders() {
        List<Order> preparable = new ArrayList<>();
        for (Order order : restaurant.getOrders()) {
            if (order.getStatus().isInKitchen()) {
                preparable.add(order);
            }
        }
        return Collections.unmodifiableList(preparable);
    }

    /** @return the customer the Cashier would charge at the end of this turn, if any */
    public Optional<Customer> getNextPayingCustomer() {
        List<Customer> payable = cashier.payableCustomers(restaurant, currentTurn);
        return payable.isEmpty() ? Optional.empty() : Optional.of(payable.get(0));
    }

    /** @return true if the Waiter has used this turn's task */
    public boolean hasWaiterActed() {
        return waiter.hasActedThisTurn();
    }

    /** @return true if the Chef has used this turn's task */
    public boolean hasChefActed() {
        return chef.hasActedThisTurn();
    }

    /** @return every event so far, in order (unmodifiable) */
    public List<GameEvent> getEventLog() {
        return Collections.unmodifiableList(eventLog);
    }

    // ---------------------------------------------------------------- helpers

    private ActionResult checkWaiterMayAct() {
        if (!turnInProgress) {
            return ActionResult.fail("No turn is in progress.");
        }
        return null;
    }

    private ActionResult checkChefMayAct() {
        if (!turnInProgress) {
            return ActionResult.fail("No turn is in progress.");
        }
        if (!waiter.hasActedThisTurn()) {
            return ActionResult.fail("The Waiter acts before the Chef: choose the Waiter's task (or Wait) first.");
        }
        return null;
    }

    private ActionResult record(ActionResult result) {
        if (result.isSuccess()) {
            eventLog.add(event(result.getMessage()));
        }
        return result;
    }

    private GameEvent event(String text) {
        return new GameEvent(currentTurn, text);
    }
}
