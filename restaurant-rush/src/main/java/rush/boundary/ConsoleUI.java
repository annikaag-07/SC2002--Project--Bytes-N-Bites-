package rush.boundary;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import rush.control.GameController;
import rush.control.GameEvent;
import rush.control.GameResult;
import rush.entity.ActionResult;
import rush.entity.Customer;
import rush.entity.MenuItem;
import rush.entity.Money;
import rush.entity.Order;
import rush.entity.Table;

/**
 * Command-line boundary. It shows the game state, reads the manager's choices, passes them to the
 * {@link GameController} as commands and prints what happened. It holds no game rules: whether a choice is
 * allowed, and what it does, is always decided by the controller.
 */
public final class ConsoleUI {

    private static final String RULE = "=".repeat(64);

    private final GameController game;
    private final InputReader input;
    private final PrintStream out;

    /**
     * @param game the game to drive
     * @param in   where the manager's input comes from
     * @param out  where the game is shown
     */
    public ConsoleUI(GameController game, InputStream in, PrintStream out) {
        this.game = game;
        this.input = new InputReader(in, out);
        this.out = out;
    }

    /** Plays the whole game: every turn, then the result. */
    public void run() {
        printBanner();
        while (!game.isGameOver()) {
            List<GameEvent> arrivals = game.beginTurn();
            out.println();
            out.println(RULE);
            out.println(" TURN " + game.getCurrentTurn() + " / " + game.getTotalTurns());
            out.println(RULE);
            printEvents(arrivals);
            printStartOfTurn();
            promptWaiter();
            promptChef();
            out.println("-- Eating, payment, waiting --");
            printEvents(game.endTurn());
            printEndOfTurn();
        }
        printResult(game.getResult());
    }

    // ---------------------------------------------------------------- display

    private void printBanner() {
        out.println(RULE);
        out.println(" RESTAURANT RUSH - you are the manager");
        out.println(RULE);
        out.println("Seed: " + game.getSeed() + "   Turns: " + game.getTotalTurns());
        out.println("Menu:");
        for (MenuItem item : game.getMenu().getOfferings()) {
            out.println("  - " + item.describe());
        }
        out.println("Enter ids like C1, T1, O1 (or just the number). Lines starting with # are ignored.");
    }

    private void printStartOfTurn() {
        out.println("Revenue: " + Money.format(game.getRevenueCents())
                + " | Avg satisfaction (paid): " + game.getAverageSatisfactionText());
        out.println("Waiting queue: " + joinCustomers(game.getQueue()));
        StringJoiner tables = new StringJoiner("   ");
        for (Table table : game.getTables()) {
            tables.add(table.getId() + ": " + (table.isFree() ? "free" : table.getOccupant().getId()));
        }
        out.println("Tables: " + tables);
        out.println("Customers:");
        if (game.getCustomers().isEmpty()) {
            out.println("  (none yet)");
        }
        for (Customer customer : game.getCustomers()) {
            out.println(String.format("  %-4s %-8s sat %3d  %-12s %s", customer.getId(), customer.typeLabel(),
                    customer.getSatisfaction(), customer.getState(),
                    customer.getTable() == null ? "" : "at " + customer.getTable().getId()).stripTrailing());
        }
        out.println("Orders:");
        if (game.getOrders().isEmpty()) {
            out.println("  (none yet)");
        }
        for (Order order : game.getOrders()) {
            out.printf("  %-4s %-4s %-28s %-10s units %s  price %s%n", order.getId(), order.getCustomer().getId(),
                    order.getItem().getName(), order.getStatus(), order.progress(),
                    Money.format(order.getLockedPriceCents()));
        }
        out.println("Staff tasks available:");
        out.println("  Waiter (1 task):  seat " + joinCustomers(game.getQueue()) + " at " + joinTables(game.getFreeTables())
                + " | take order " + joinCustomers(game.getCustomersAwaitingOrder())
                + " | serve " + joinOrders(game.getServableOrders()));
        out.println("  Chef (1 unit):    advance " + joinOrders(game.getPreparableOrders()));
        Optional<Customer> next = game.getNextPayingCustomer();
        out.println("  Cashier (auto):   " + (next.isPresent() ? "will charge " + next.get().getId() : "nobody to charge"));
    }

    private void printEndOfTurn() {
        out.println("-- End of turn " + game.getCurrentTurn() + " --");
        out.println("Served: " + game.getServedCount()
                + " | Unhappy departures: " + game.getDepartureCount()
                + " | Paid: " + game.getPaidCount()
                + " | Revenue: " + Money.format(game.getRevenueCents())
                + " | Avg satisfaction (paid): " + game.getAverageSatisfactionText()
                + " | Turns used: " + game.getCurrentTurn() + "/" + game.getTotalTurns());
    }

    private void printResult(GameResult result) {
        out.println();
        out.println(RULE);
        out.println(" FINAL RESULT after " + game.getTotalTurns() + " turns");
        out.println(RULE);
        out.println("Paid customers:       " + result.getPaidCount() + " (need >= "
                + result.getConfig().getMinPaidCustomers() + ") " + met(result.isPaidCountMet()));
        out.println("Revenue:              " + Money.format(result.getRevenueCents()) + " (need >= "
                + Money.format(result.getConfig().getMinRevenueCents()) + ") " + met(result.isRevenueMet()));
        out.println("Average satisfaction: " + result.getAverageText() + " (need >= "
                + result.getConfig().getMinAverageSatisfaction() + ") " + met(result.isSatisfactionMet()));
        out.println("RESULT: " + (result.isVictory() ? "VICTORY" : "DEFEAT"));
    }

    private void printEvents(List<GameEvent> events) {
        for (GameEvent event : events) {
            out.println("  * " + event.getText());
        }
    }

    // ---------------------------------------------------------------- manager prompts

    private void promptWaiter() {
        while (true) {
            out.println("Waiter: 1) Seat a queued customer  2) Take an order  3) Serve a READY order  0) Wait");
            String choice = input.readLine("Waiter choice: ", "0");
            ActionResult result;
            switch (choice) {
                case "0":
                    result = game.waiterWait();
                    break;
                case "1":
                    String customer = readId("  Customer to seat: ", "C");
                    result = game.waiterSeat(customer, readId("  Table: ", "T"));
                    break;
                case "2":
                    result = game.waiterTakeOrder(readId("  Customer whose order to take: ", "C"));
                    break;
                case "3":
                    result = game.waiterServe(readId("  Order to serve: ", "O"));
                    break;
                default:
                    out.println("  ! '" + choice + "' is not an option. Enter 0, 1, 2 or 3.");
                    continue;
            }
            if (report(result)) {
                return;
            }
        }
    }

    private void promptChef() {
        while (true) {
            out.println("Chef: 1) Advance an order by one unit  0) Wait");
            String choice = input.readLine("Chef choice: ", "0");
            ActionResult result;
            switch (choice) {
                case "0":
                    result = game.chefWait();
                    break;
                case "1":
                    result = game.chefAdvance(readId("  Order to prepare: ", "O"));
                    break;
                default:
                    out.println("  ! '" + choice + "' is not an option. Enter 0 or 1.");
                    continue;
            }
            if (report(result)) {
                return;
            }
        }
    }

    /** Prints the outcome; returns true if the action succeeded (otherwise the same staff member is asked again). */
    private boolean report(ActionResult result) {
        if (result.isSuccess()) {
            out.println("  * " + result.getMessage());
            return true;
        }
        out.println("  ! Not allowed: " + result.getMessage() + " Choose again.");
        return false;
    }

    /** Reads an id; a bare number gets the given prefix, so "2" means "C2" when asking for a customer. */
    private String readId(String prompt, String prefix) {
        String text = input.readLine(prompt, "");
        return text.matches("\\d+") ? prefix + text : text;
    }

    private static String joinCustomers(List<Customer> customers) {
        if (customers.isEmpty()) {
            return "(none)";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (Customer customer : customers) {
            joiner.add(customer.getId() + "(" + customer.typeLabel() + ", " + customer.getSatisfaction() + ")");
        }
        return joiner.toString();
    }

    private static String joinTables(List<Table> tables) {
        if (tables.isEmpty()) {
            return "(no free table)";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (Table table : tables) {
            joiner.add(table.getId());
        }
        return joiner.toString();
    }

    private static String joinOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            return "(none)";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (Order order : orders) {
            joiner.add(order.getId() + "[" + order.getCustomer().getId() + " " + order.getItem().getName()
                    + " " + order.progress() + "]");
        }
        return joiner.toString();
    }

    private static String met(boolean met) {
        return met ? "MET" : "NOT MET";
    }
}
