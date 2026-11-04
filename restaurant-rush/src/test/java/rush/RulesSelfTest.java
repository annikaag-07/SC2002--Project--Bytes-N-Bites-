package rush;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import rush.control.ArrivalSchedule;
import rush.control.GameConfig;
import rush.control.GameController;
import rush.control.GameResult;
import rush.entity.ActionResult;
import rush.entity.Cashier;
import rush.entity.Customer;
import rush.entity.CustomerState;
import rush.entity.EntityTestSupport;
import rush.entity.Menu;
import rush.entity.MenuItem;
import rush.entity.Money;
import rush.entity.Order;
import rush.entity.OrderStatus;
import rush.entity.RegularCustomer;
import rush.entity.Restaurant;

/**
 * Assertion-based rule tests (no JUnit). Each check is counted; the process exits non-zero if any fails.
 * Every test builds its own configuration and arrival schedule, so changes to the base setting do not affect it.
 */
public final class RulesSelfTest {

    private static final long SEED = 2002L;
    private static int passed;
    private static int failed;
    private static String currentTest = "";

    private RulesSelfTest() {
    }

    /**
     * Runs every test and prints the totals.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        run("spec 3.3 walkthrough (Regular, Pasta)", RulesSelfTest::walkthroughFromSpec);
        run("Waiter: one task per turn; invalid choice keeps the task", RulesSelfTest::waiterOneTaskAndInvalidKeepsTask);
        run("Chef: one unit per turn; acts after the Waiter", RulesSelfTest::chefOneUnitAfterWaiter);
        run("dish finished this turn is served next turn at the earliest", RulesSelfTest::readyThisTurnNotServable);
        run("Cashier: one per turn, oldest first, not on READY_TO_PAY turn", RulesSelfTest::cashierOldestFirst);
        run("abandonment cancels order, frees table, no revenue", RulesSelfTest::abandonmentCancelsOrder);
        run("Money formatting and half-up rounding", RulesSelfTest::moneyRules);
        run("seeded food choice is reproducible and order-independent", RulesSelfTest::seededChoices);
        run("victory criteria boundaries", RulesSelfTest::victoryBoundaries);
        run("game always plays all turns; result only at the end", RulesSelfTest::fullGameLength);

        System.out.println();
        System.out.println("RulesSelfTest: " + passed + " checks passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- tests

    private static void walkthroughFromSpec() {
        GameController game = singleItemGame("Pasta", 1);
        Customer c1;

        game.beginTurn();
        ok(game.waiterSeat("C1", "T1"));
        ok(game.chefWait());
        game.endTurn();
        c1 = game.getCustomers().get(0);
        check("T1 seated", c1.getState() == CustomerState.SEATED);
        check("T1 satisfaction 92", c1.getSatisfaction() == 92);

        game.beginTurn();
        ok(game.waiterTakeOrder("C1"));
        Order order = c1.getOrder();
        check("T2 chose Pasta", order.getItem().getName().equals("Pasta"));
        check("T2 locked 1800", order.getLockedPriceCents() == 1800);
        check("T2 order taken turn 2", order.getTakenTurn() == 2);
        ok(game.chefAdvance(order.getId()));
        game.endTurn();
        check("T2 PREPARING 1/2", order.getStatus() == OrderStatus.PREPARING && order.getUnitsDone() == 1);
        check("T2 satisfaction 84", c1.getSatisfaction() == 84);

        game.beginTurn();
        fail(game.waiterServe(order.getId()));
        ok(game.waiterWait());
        ok(game.chefAdvance(order.getId()));
        check("T3 READY on turn 3", order.getStatus() == OrderStatus.READY && order.getReadyTurn() == 3);
        check("T3 not servable this turn", !order.isServableOn(3) && game.getServableOrders().isEmpty());
        game.endTurn();
        check("T3 satisfaction 76", c1.getSatisfaction() == 76);

        game.beginTurn();
        ok(game.waiterServe(order.getId()));
        ok(game.chefWait());
        game.endTurn();
        check("T4 SERVED, EATING", order.getStatus() == OrderStatus.SERVED && c1.getState() == CustomerState.EATING);
        check("T4 satisfaction stays 76", c1.getSatisfaction() == 76);

        game.beginTurn();
        waitBoth(game);
        game.endTurn();
        check("T5 READY_TO_PAY on turn 5", c1.getState() == CustomerState.READY_TO_PAY && c1.getReadyToPayTurn() == 5);
        check("T5 no revenue yet", game.getRevenueCents() == 0);

        game.beginTurn();
        waitBoth(game);
        game.endTurn();
        check("T6 PAID", c1.getState() == CustomerState.PAID && order.getStatus() == OrderStatus.PAID);
        check("T6 revenue 1800", game.getRevenueCents() == 1800);
        check("T6 recorded 76", c1.getRecordedSatisfaction() == 76);
        check("T6 table freed", game.getFreeTables().size() == 2);
        check("T6 average 76.0", game.getAverageSatisfactionText().equals("76.0"));
    }

    private static void waiterOneTaskAndInvalidKeepsTask() {
        GameController game = singleItemGame("Salad", 1, 1, 1);
        game.beginTurn();
        fail(game.waiterSeat("C9", "T1"));
        fail(game.waiterSeat("C1", "T7"));
        fail(game.waiterTakeOrder("C1"));
        fail(game.waiterServe("O1"));
        check("invalid choices leave the task unused", !game.hasWaiterActed());
        ok(game.waiterSeat("C2", "T2"));
        check("any queued customer may be seated (not only the head)", game.getQueue().get(0).getId().equals("C1"));
        fail(game.waiterSeat("C1", "T1"));
        fail(game.waiterWait());
        check("second seat refused, C1 still queued", game.getQueue().size() == 2);
        ok(game.chefWait());
        game.endTurn();

        game.beginTurn();
        fail(game.waiterSeat("C1", "T2"));
        ok(game.waiterSeat("C1", "T1"));
        ok(game.chefWait());
        game.endTurn();

        game.beginTurn();
        fail(game.waiterSeat("C1", "T1"));
        fail(game.waiterSeat("C3", "T1"));
        check("seat with no free table refused, task kept", !game.hasWaiterActed());
        ok(game.waiterWait());
        ok(game.chefWait());
        game.endTurn();
    }

    private static void chefOneUnitAfterWaiter() {
        GameController game = singleItemGame("Pasta", 1);
        game.beginTurn();
        fail(game.chefWait());
        check("Chef cannot act before the Waiter", !game.hasChefActed());
        ok(game.waiterSeat("C1", "T1"));
        ok(game.chefWait());
        game.endTurn();

        game.beginTurn();
        ok(game.waiterTakeOrder("C1"));
        fail(game.chefAdvance("O9"));
        check("invalid Chef choice keeps the task", !game.hasChefActed());
        ok(game.chefAdvance("O1"));
        fail(game.chefAdvance("O1"));
        Order order = game.getOrders().get(0);
        check("only one unit per turn", order.getUnitsDone() == 1);
        game.endTurn();

        game.beginTurn();
        ok(game.waiterWait());
        ok(game.chefAdvance("O1"));
        game.endTurn();
        game.beginTurn();
        ok(game.waiterWait());
        fail(game.chefAdvance("O1"));
        check("READY order needs no more preparation", order.getUnitsDone() == 2);
        ok(game.chefWait());
        game.endTurn();
    }

    private static void readyThisTurnNotServable() {
        GameController game = singleItemGame("Burger", 1);
        game.beginTurn();
        ok(game.waiterSeat("C1", "T1"));
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        ok(game.waiterTakeOrder("C1"));
        ok(game.chefAdvance("O1"));
        Order order = game.getOrders().get(0);
        check("READY this turn", order.getStatus() == OrderStatus.READY && order.getReadyTurn() == 2);
        check("not servable on its READY turn", !order.isServableOn(2));
        game.endTurn();
        game.beginTurn();
        check("servable next turn", order.isServableOn(3) && game.getServableOrders().contains(order));
        ok(game.waiterServe("O1"));
        ok(game.chefWait());
        game.endTurn();
        check("served on turn 3", order.getServedTurn() == 3);
    }

    private static void cashierOldestFirst() {
        MenuItem salad = Menu.standard().itemNamed("Salad");
        Customer c1 = new RegularCustomer("C1", 1, 1, SEED);
        Customer c2 = new RegularCustomer("C2", 2, 1, SEED);
        Customer c3 = new RegularCustomer("C3", 3, 1, SEED);
        Restaurant restaurant = EntityTestSupport.restaurantWithServedCustomers(
                new Customer[] {c1, c2, c3}, new int[] {3, 2, 2}, salad);
        Cashier cashier = new Cashier();

        // Turn 3: C2 and C3 (served on turn 2) finish eating; C1 (served on turn 3) does not.
        for (Customer c : restaurant.getCustomers()) {
            c.finishEatingIfDone(3);
        }
        check("served on turn 3 is still eating on turn 3", c1.getState() == CustomerState.EATING);
        cashier.startTurn();
        fail(cashier.collectPayment(restaurant, 3));
        check("nobody pays on the turn they became READY_TO_PAY", restaurant.getPaidCount() == 0);

        // Turn 4: C2 and C3 are payable (tie on READY_TO_PAY turn 3, broken by arrival); C1 becomes READY_TO_PAY.
        for (Customer c : restaurant.getCustomers()) {
            c.finishEatingIfDone(4);
        }
        cashier.startTurn();
        ok(cashier.collectPayment(restaurant, 4));
        fail(cashier.collectPayment(restaurant, 4));
        check("tie broken by earliest arrival: C2 first", c2.getState() == CustomerState.PAID);
        check("at most one payment per turn", restaurant.getPaidCount() == 1);

        // Turn 5: C3 (READY_TO_PAY on 3) beats C1 (READY_TO_PAY on 4) although C1 arrived first.
        cashier.startTurn();
        ok(cashier.collectPayment(restaurant, 5));
        check("earliest READY_TO_PAY first: C3 before C1",
                c3.getState() == CustomerState.PAID && c1.getState() == CustomerState.READY_TO_PAY);
        cashier.startTurn();
        ok(cashier.collectPayment(restaurant, 6));
        check("revenue counts paid orders only", restaurant.getRevenueCents() == 3 * 900);
    }

    private static void abandonmentCancelsOrder() {
        // C1 is seated and its Salad is cooked but never served; C2 is never seated.
        GameController game = singleItemGame("Salad", 1, 1);
        game.beginTurn();
        ok(game.waiterSeat("C1", "T1"));
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        ok(game.waiterTakeOrder("C1"));
        ok(game.chefAdvance("O1"));
        game.endTurn();
        Customer c1 = game.getCustomers().get(0);
        Customer c2 = game.getCustomers().get(1);
        Order order = c1.getOrder();
        for (int turn = 3; turn <= 12; turn++) {
            game.beginTurn();
            waitBoth(game);
            game.endTurn();
        }
        check("after 12 waiting turns satisfaction is 4", c1.getSatisfaction() == 4 && c2.getSatisfaction() == 4);
        game.beginTurn();
        waitBoth(game);
        game.endTurn();
        check("at 0 the customer leaves", c1.getState() == CustomerState.LEFT && c2.getState() == CustomerState.LEFT);
        check("READY food is discarded: order CANCELLED", order.getStatus() == OrderStatus.CANCELLED);
        check("table freed", game.getFreeTables().size() == 2);
        check("queue emptied", game.getQueue().isEmpty());
        check("no revenue", game.getRevenueCents() == 0 && game.getPaidCount() == 0);
        check("two unhappy departures", game.getDepartureCount() == 2);
        game.beginTurn();
        fail(game.waiterServe("O1"));
        check("a departed customer's satisfaction no longer changes", c1.getSatisfaction() == 0);
        waitBoth(game);
        game.endTurn();
    }

    private static void moneyRules() {
        check("$0.00", Money.format(0).equals("$0.00"));
        check("$24.30", Money.format(2430).equals("$24.30"));
        check("$9.05", Money.format(905).equals("$9.05"));
        check("90% of 1500 = 1350", Money.percent(1500, 90) == 1350);
        check("90% of 2700 = 2430", Money.percent(2700, 90) == 2430);
        check("half up: 50% of 1 = 1", Money.percent(1, 50) == 1);
        check("half up: 90% of 5 = 5 (4.5 -> 5)", Money.percent(5, 90) == 5);
        check("round down below half: 80% of 3 = 2 (2.4)", Money.percent(3, 80) == 2);
        check("average N/A when nobody paid", GameResult.formatAverage(0, 0).equals("N/A"));
        check("average rounds half up to one decimal", GameResult.formatAverage(181, 3).equals("60.3"));
    }

    private static void seededChoices() {
        List<String> forward = choicesSeatingInOrder(SEED, false);
        List<String> reversed = choicesSeatingInOrder(SEED, true);
        check("same seed, different action order: same choices", forward.equals(reversed));
        check("same seed twice: same choices", forward.equals(choicesSeatingInOrder(SEED, false)));
        List<MenuItem> offerings = Menu.standard().getOfferings();
        for (int index = 1; index <= 2; index++) {
            int draw = new Random(Customer.choiceSeed(SEED, index)).nextInt(offerings.size());
            check("C" + index + " choice follows its own seeded generator",
                    forward.get(index - 1).equals(offerings.get(draw).getName()));
        }
        boolean anyDifferent = false;
        List<String> base = firstChoices(1L);
        for (long seed = 2; seed <= 20 && !anyDifferent; seed++) {
            anyDifferent = !firstChoices(seed).equals(base);
        }
        check("different seeds give different choices", anyDifferent);
    }

    private static void victoryBoundaries() {
        GameConfig config = GameConfig.base(SEED);
        check("exactly 4 / $45.00 / 60 -> victory", GameResult.evaluate(config, 4, 4500, 240).isVictory());
        check("3 paid -> defeat", !GameResult.evaluate(config, 3, 4500, 180).isVictory());
        check("$44.99 -> defeat", !GameResult.evaluate(config, 4, 4499, 240).isVictory());
        check("average 59.75 -> defeat", !GameResult.evaluate(config, 4, 4500, 239).isVictory());
        check("no payers -> satisfaction not met", !GameResult.evaluate(config, 0, 0, 0).isSatisfactionMet());
    }

    private static void fullGameLength() {
        GameController game = new GameController(GameConfig.base(SEED), ArrivalSchedule.base(SEED));
        boolean earlyResult = false;
        for (int turn = 1; turn <= 18; turn++) {
            check("not over before turn " + turn, !game.isGameOver());
            try {
                game.getResult();
                earlyResult = true;
            } catch (IllegalStateException expected) {
                // the result is only available after the last turn
            }
            game.beginTurn();
            waitBoth(game);
            game.endTurn();
        }
        check("no result before the end", !earlyResult);
        check("over after 18 turns", game.isGameOver() && game.getCurrentTurn() == 18);
        check("six arrivals", game.getCustomers().size() == 6);
        check("all-wait game is a defeat", !game.getResult().isVictory());
    }

    // ---------------------------------------------------------------- helpers

    /** A base-setting game whose menu has a single item, so every customer's choice is fixed. */
    private static GameController singleItemGame(String itemName, int... arrivalTurns) {
        Menu single = new Menu(List.of(Menu.standard().itemNamed(itemName)));
        return new GameController(GameConfig.base(SEED).withMenu(single), ArrivalSchedule.regulars(SEED, arrivalTurns));
    }

    /** Two customers arrive on turn 1; their orders are taken in arrival order or reversed. */
    private static List<String> choicesSeatingInOrder(long seed, boolean reversed) {
        GameController game = new GameController(GameConfig.base(seed), ArrivalSchedule.regulars(seed, 1, 1));
        String first = reversed ? "C2" : "C1";
        String second = reversed ? "C1" : "C2";
        game.beginTurn();
        ok(game.waiterSeat(first, "T1"));
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        ok(game.waiterSeat(second, "T2"));
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        ok(game.waiterTakeOrder(first));
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        ok(game.waiterTakeOrder(second));
        ok(game.chefWait());
        game.endTurn();
        List<String> choices = new ArrayList<>();
        for (Customer customer : game.getCustomers()) {
            choices.add(customer.getOrder().getItem().getName());
        }
        return choices;
    }

    private static List<String> firstChoices(long seed) {
        List<MenuItem> offerings = Menu.standard().getOfferings();
        List<String> names = new ArrayList<>();
        for (int index = 1; index <= 6; index++) {
            names.add(offerings.get(new Random(Customer.choiceSeed(seed, index)).nextInt(offerings.size())).getName());
        }
        return names;
    }

    private static void waitBoth(GameController game) {
        ok(game.waiterWait());
        ok(game.chefWait());
    }

    private static void run(String name, Runnable test) {
        currentTest = name;
        int failedBefore = failed;
        try {
            test.run();
        } catch (RuntimeException | AssertionError e) {
            failed++;
            System.out.println("  FAIL " + name + ": unexpected " + e);
        }
        System.out.println((failed == failedBefore ? "PASS " : "FAIL ") + name);
    }

    private static void check(String what, boolean condition) {
        if (condition) {
            passed++;
        } else {
            failed++;
            System.out.println("  FAIL [" + currentTest + "] " + what);
        }
    }

    private static void ok(ActionResult result) {
        check("expected success: " + result.getMessage(), result.isSuccess());
    }

    private static void fail(ActionResult result) {
        check("expected rejection: " + result.getMessage(), !result.isSuccess());
    }
}
