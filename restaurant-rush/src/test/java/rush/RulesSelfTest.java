package rush;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import rush.control.ArrivalSchedule;
import rush.control.GameConfig;
import rush.control.GameController;
import rush.control.GameResult;
import rush.control.HappyHourPolicy;
import rush.entity.ActionResult;
import rush.entity.Cashier;
import rush.entity.ComboMeal;
import rush.entity.CriticCustomer;
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
import rush.entity.VIPCustomer;

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
        // Stage 2: customer behaviour
        run("VIP loses 5 per waiting turn", RulesSelfTest::vipLoss);
        run("VIP pays 90% (Burger 1350, Salad 810)", RulesSelfTest::vipPricing);
        run("Critic loses 12 per waiting turn, pays full price", RulesSelfTest::criticLossAndPrice);
        run("Critic impressed by fast service (+10, capped at 100)", RulesSelfTest::criticImpressed);
        run("Critic unimpressed by slow service (-10, floored, never leaves)", RulesSelfTest::criticUnimpressed);
        run("base schedule: six arrivals including a VIP and a Critic", RulesSelfTest::baseScheduleTypes);
        run("no type checks in control or boundary", RulesSelfTest::noTypeChecksInControlOrBoundary);
        // Stage 3: new role and menu policy
        run("combo meals: summed units, 90% price, composition", RulesSelfTest::comboMeals);
        run("Happy Hour pricing: VIP then Happy Hour, locked at order", RulesSelfTest::happyHourPricing);
        run("Happy Hour: once per game, two turns, before staff act", RulesSelfTest::happyHourTiming);
        run("Happy Hour recovery +15, capped, waiting customers only", RulesSelfTest::happyHourRecovery);
        run("Host seats one extra customer, before the Waiter", RulesSelfTest::hostSeating);

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

    private static void vipLoss() {
        GameController game = typedGame("Salad", ArrivalSchedule.builder(SEED).add(1, VIPCustomer::new).build());
        Customer vip = waitTurns(game, 1);
        check("VIP 100 -> 95 after one turn", vip.getSatisfaction() == 95);
        waitTurns(game, 2);
        check("VIP 95 -> 85 after two more turns", vip.getSatisfaction() == 85);
    }

    private static void vipPricing() {
        check("VIP Burger locks 1350", lockedPriceFor("Burger", VIPCustomer::new) == 1350);
        check("VIP Salad locks 810", lockedPriceFor("Salad", VIPCustomer::new) == 810);
        check("VIP Pasta locks 1620", lockedPriceFor("Pasta", VIPCustomer::new) == 1620);
        check("Regular Burger locks 1500", lockedPriceFor("Burger", RegularCustomer::new) == 1500);
        check("VIP pricing rounds half up", new VIPCustomer("C1", 1, 1, SEED).applyCustomerPricing(5) == 5);
    }

    private static void criticLossAndPrice() {
        GameController game = typedGame("Salad", ArrivalSchedule.builder(SEED).add(1, CriticCustomer::new).build());
        Customer critic = waitTurns(game, 1);
        check("Critic 100 -> 88 after one turn", critic.getSatisfaction() == 88);
        check("Critic Burger locks full 1500", lockedPriceFor("Burger", CriticCustomer::new) == 1500);
    }

    private static void criticImpressed() {
        // Burger: seat turn 1 (88), order + cook turn 2 (76), serve turn 3 = 1 turn after ordering -> 86.
        GameController game = typedGame("Burger", ArrivalSchedule.builder(SEED).add(1, CriticCustomer::new).build());
        game.beginTurn();
        ok(game.waiterSeat("C1", "T1"));
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        ok(game.waiterTakeOrder("C1"));
        ok(game.chefAdvance("O1"));
        game.endTurn();
        Customer critic = game.getCustomers().get(0);
        check("Critic at 76 before serving", critic.getSatisfaction() == 76);
        game.beginTurn();
        ActionResult served = game.waiterServe("O1");
        ok(served);
        check("reaction reported as an event", served.getMessage().contains("IMPRESSED"));
        check("impressed: 76 -> 86", critic.getSatisfaction() == 86);
        ok(game.chefWait());
        game.endTurn();
        check("no loss once served", critic.getSatisfaction() == 86);

        // Pasta at the 2-turn limit is still fast; impressed at 100 stays capped at 100.
        Customer capped = new CriticCustomer("C1", 1, 1, SEED);
        EntityTestSupport.restaurantWithServedCustomers(new Customer[] {capped}, new int[] {3},
                Menu.standard().itemNamed("Pasta"));
        check("served exactly 2 turns after ordering: impressed, capped at 100", capped.getSatisfaction() == 100);
    }

    private static void criticUnimpressed() {
        Customer slow = new CriticCustomer("C1", 1, 1, SEED);
        EntityTestSupport.restaurantWithServedCustomers(new Customer[] {slow}, new int[] {4},
                Menu.standard().itemNamed("Salad"));
        check("served 3 turns after ordering: 100 -> 90", slow.getSatisfaction() == 90);

        // In play: seat turn 1, order turn 6 (sat 28), cook, serve turn 9 (sat 4) -> unimpressed floors at 0.
        GameController game = typedGame("Burger", ArrivalSchedule.builder(SEED).add(1, CriticCustomer::new).build());
        game.beginTurn();
        ok(game.waiterSeat("C1", "T1"));
        ok(game.chefWait());
        game.endTurn();
        Customer critic = waitTurns(game, 4);
        game.beginTurn();
        ok(game.waiterTakeOrder("C1"));
        ok(game.chefAdvance("O1"));
        game.endTurn();
        waitTurns(game, 2);
        check("Critic at 4 before late serving", critic.getSatisfaction() == 4);
        game.beginTurn();
        ok(game.waiterServe("O1"));
        ok(game.chefWait());
        game.endTurn();
        check("unimpressed floors at 0", critic.getSatisfaction() == 0);
        check("a served Critic at 0 does not leave", critic.getState() == CustomerState.EATING);
        waitTurns(game, 2);
        check("pays with 0 recorded", critic.getState() == CustomerState.PAID && critic.getRecordedSatisfaction() == 0);
        check("full price paid", game.getRevenueCents() == 1500);
    }

    private static void baseScheduleTypes() {
        List<String> types = new ArrayList<>();
        for (Customer customer : ArrivalSchedule.base(SEED).getCustomers()) {
            types.add(customer.getArrivalTurn() + ":" + customer.typeLabel());
        }
        check("base schedule " + types, types.equals(List.of(
                "1:Regular", "4:VIP", "7:Regular", "10:Critic", "13:Regular", "16:Regular")));
    }

    private static void noTypeChecksInControlOrBoundary() {
        Path root = Paths.get("src/main/java/rush");
        if (!Files.isDirectory(root)) {
            System.out.println("  (source tree not found from working directory; static check skipped)");
            return;
        }
        for (String layer : List.of("control", "boundary")) {
            try (Stream<Path> files = Files.walk(root.resolve(layer))) {
                for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                    String text = Files.readString(file);
                    check(file + " has no instanceof/getClass()",
                            !text.contains("instanceof") && !text.contains("getClass()"));
                }
            } catch (IOException e) {
                check("could read " + layer + ": " + e, false);
            }
        }
    }

    private static void comboMeals() {
        Menu menu = Menu.standard();
        MenuItem saladPasta = menu.itemNamed("Salad + Pasta Combo");
        check("Salad + Pasta combo = 2430 cents", saladPasta.getPriceCents() == 2430);
        check("Salad + Pasta combo = 3 units", saladPasta.getPrepUnits() == 3);
        MenuItem saladBurger = menu.itemNamed("Salad + Burger Combo");
        check("Salad + Burger combo = 2160 cents, 2 units",
                saladBurger.getPriceCents() == 2160 && saladBurger.getPrepUnits() == 2);
        check("menu order: Salad, Burger, Pasta, two combos", menu.getOfferings().size() == 5
                && menu.getOfferings().get(3) == saladBurger && menu.getOfferings().get(4) == saladPasta);
        ComboMeal combo = (ComboMeal) saladPasta;
        check("combo keeps both components", combo.getComponents().equals(
                List.of(menu.itemNamed("Salad"), menu.itemNamed("Pasta"))));
        check("combo description shows components", combo.describe().contains("$9.00 + $18.00"));
        check("combo of combos rejected", throwsIllegalArgument(() -> new ComboMeal(combo, menu.itemNamed("Salad"))));
        check("null component rejected", throwsIllegalArgument(() -> new ComboMeal(null, menu.itemNamed("Salad"))));
        check("half-up rounding: 90% of $0.05 + $0.00 = $0.05",
                new ComboMeal(new MenuItem("A", 5, 1), new MenuItem("B", 0, 1)).getPriceCents() == 5);

        // A combo needs all its units before it is READY.
        GameController game = typedGame("Salad + Pasta Combo", ArrivalSchedule.regulars(SEED, 1));
        game.beginTurn();
        ok(game.waiterSeat("C1", "T1"));
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        ok(game.waiterTakeOrder("C1"));
        ok(game.chefAdvance("O1"));
        game.endTurn();
        game.beginTurn();
        ok(game.waiterWait());
        ok(game.chefAdvance("O1"));
        game.endTurn();
        Order order = game.getOrders().get(0);
        check("combo 2/3 still PREPARING", order.getStatus() == OrderStatus.PREPARING);
        check("combo locked at 2430", order.getLockedPriceCents() == 2430);
    }

    private static void happyHourPricing() {
        check("VIP Burger during Happy Hour = 1080 ($10.80)",
                happyHourPriceFor("Burger", VIPCustomer::new) == 1080);
        check("Regular Pasta during Happy Hour = 1440", happyHourPriceFor("Pasta", RegularCustomer::new) == 1440);
        check("VIP Salad + Pasta combo during Happy Hour = 2430 -> 2187 -> 1750",
                happyHourPriceFor("Salad + Pasta Combo", VIPCustomer::new) == 1750);
        check("Critic (full price) Burger during Happy Hour = 1200",
                happyHourPriceFor("Burger", CriticCustomer::new) == 1200);

        // An order taken before activation keeps its locked price; one taken during Happy Hour is discounted.
        GameController game = typedGame("Pasta", ArrivalSchedule.regulars(SEED, 1, 1));
        game.beginTurn();
        ok(game.hostSeat("C1", "T1"));
        ok(game.waiterSeat("C2", "T2"));
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        ok(game.waiterTakeOrder("C1"));
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        ok(game.activateHappyHour());
        ok(game.waiterTakeOrder("C2"));
        ok(game.chefWait());
        game.endTurn();
        check("order taken before activation keeps 1800", game.getOrders().get(0).getLockedPriceCents() == 1800);
        check("order taken during Happy Hour locks 1440", game.getOrders().get(1).getLockedPriceCents() == 1440);
    }

    private static void happyHourTiming() {
        GameController game = typedGame("Salad", ArrivalSchedule.regulars(SEED, 1));
        game.beginTurn();
        check("available at the start", game.isHappyHourAvailable() && game.getHappyHourRemainingTurns() == 0);
        waitBoth(game);
        fail(game.activateHappyHour());
        check("cannot start after staff acted", game.isHappyHourAvailable());
        game.endTurn();
        game.beginTurn();
        ok(game.hostWait());
        fail(game.activateHappyHour());
        waitBoth(game);
        game.endTurn();
        game.beginTurn();
        ok(game.activateHappyHour());
        check("activation turn: 2 remaining", game.getHappyHourRemainingTurns() == 2 && !game.isHappyHourAvailable());
        fail(game.activateHappyHour());
        waitBoth(game);
        game.endTurn();
        game.beginTurn();
        check("next turn: 1 remaining", game.getHappyHourRemainingTurns() == 1);
        fail(game.activateHappyHour());
        waitBoth(game);
        game.endTurn();
        game.beginTurn();
        check("third turn: over", game.getHappyHourRemainingTurns() == 0);
        fail(game.activateHappyHour());
        check("cannot be activated twice", !game.isHappyHourAvailable());
        waitBoth(game);
        game.endTurn();

        HappyHourPolicy lastTurn = new HappyHourPolicy();
        ok(lastTurn.activate(18));
        check("activated on turn 18 covers one turn", lastTurn.remainingTurns(18, 18) == 1);
        check("active on exactly two turns", lastTurn.isActive(18) && lastTurn.isActive(19) && !lastTurn.isActive(20)
                && !lastTurn.isActive(17));
    }

    private static void happyHourRecovery() {
        // C1 is served (EATING) by turn 3; C2 waits in the queue; C3 arrives on turn 4 at 100.
        GameController game = typedGame("Salad", ArrivalSchedule.regulars(SEED, 1, 1, 4));
        game.beginTurn();
        ok(game.waiterSeat("C1", "T1"));
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        ok(game.waiterTakeOrder("C1"));
        ok(game.chefAdvance("O1"));
        game.endTurn();
        game.beginTurn();
        ok(game.waiterServe("O1"));
        ok(game.chefWait());
        game.endTurn();
        Customer c1 = game.getCustomers().get(0);
        Customer c2 = game.getCustomers().get(1);
        check("before Happy Hour: C1 eating at 84, C2 queued at 76",
                c1.getState() == CustomerState.EATING && c1.getSatisfaction() == 84 && c2.getSatisfaction() == 76);
        game.beginTurn();
        Customer c3 = game.getCustomers().get(2);
        ok(game.activateHappyHour());
        check("waiting C2 recovers 76 -> 91", c2.getSatisfaction() == 91);
        check("eating/READY_TO_PAY C1 unaffected", c1.getSatisfaction() == 84);
        check("new arrival capped at 100", c3.getSatisfaction() == 100);
        waitBoth(game);
        game.endTurn();
        check("end of turn 4: C2 83, C1 READY_TO_PAY", c2.getSatisfaction() == 83
                && c1.getState() == CustomerState.READY_TO_PAY);
        game.beginTurn();
        check("second active turn recovers at turn start: C2 83 -> 98", c2.getSatisfaction() == 98);
        check("second active turn: C3 capped at 100", c3.getSatisfaction() == 100);
        check("READY_TO_PAY C1 unaffected", c1.getSatisfaction() == 84);
        waitBoth(game);
        game.endTurn();
        check("C1 paid with 84", c1.getState() == CustomerState.PAID && c1.getRecordedSatisfaction() == 84);
        game.beginTurn();
        check("after Happy Hour no recovery: C2 stays 90", c2.getSatisfaction() == 90);
        waitBoth(game);
        game.endTurn();

        // A customer who has left is never affected.
        GameController late = typedGame("Salad", ArrivalSchedule.regulars(SEED, 1, 14));
        Customer gone = waitTurns(late, 13);
        check("C1 left on turn 13", gone.getState() == CustomerState.LEFT);
        late.beginTurn();
        ok(late.activateHappyHour());
        check("left customer stays at 0", gone.getSatisfaction() == 0 && gone.getState() == CustomerState.LEFT);
        waitBoth(late);
        late.endTurn();
    }

    private static void hostSeating() {
        GameController game = typedGame("Salad", ArrivalSchedule.regulars(SEED, 1, 1, 1));
        game.beginTurn();
        ok(game.hostSeat("C2", "T1"));
        fail(game.hostSeat("C1", "T2"));
        ok(game.waiterSeat("C1", "T2"));
        check("Host + Waiter seat two customers in one turn",
                game.getFreeTables().isEmpty() && game.getQueue().size() == 1);
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        fail(game.hostSeat("C3", "T1"));
        check("Host cannot seat without a free table; task kept", !game.hasHostActed());
        ok(game.hostWait());
        waitBoth(game);
        game.endTurn();

        GameController afterWaiter = typedGame("Salad", ArrivalSchedule.regulars(SEED, 1, 1));
        afterWaiter.beginTurn();
        ok(afterWaiter.waiterSeat("C1", "T1"));
        fail(afterWaiter.hostSeat("C2", "T2"));
        check("Host cannot act after the Waiter", !afterWaiter.hasHostActed() && afterWaiter.getQueue().size() == 1);
        ok(afterWaiter.chefWait());
        afterWaiter.endTurn();

        GameController sameTurn = typedGame("Burger", ArrivalSchedule.regulars(SEED, 1, 1));
        sameTurn.beginTurn();
        ok(sameTurn.hostSeat("C1", "T1"));
        ok(sameTurn.waiterTakeOrder("C1"));
        check("Waiter takes the order of a customer the Host seated this turn",
                sameTurn.getCustomers().get(0).getState() == CustomerState.ORDERED);
        fail(sameTurn.hostSeat("C2", "T2"));
        check("Host gets at most one seat per turn: C2 still queued", sameTurn.getQueue().size() == 1);
        ok(sameTurn.chefAdvance("O1"));
        sameTurn.endTurn();
    }

    // ---------------------------------------------------------------- helpers

    /** Price locked for one customer of the given type who orders on turn 2, with Happy Hour switched on that turn. */
    private static long happyHourPriceFor(String itemName, ArrivalSchedule.CustomerFactory type) {
        GameController game = typedGame(itemName, ArrivalSchedule.builder(SEED).add(1, type).build());
        game.beginTurn();
        ok(game.hostSeat("C1", "T1"));
        waitBoth(game);
        game.endTurn();
        game.beginTurn();
        ok(game.activateHappyHour());
        ok(game.waiterTakeOrder("C1"));
        ok(game.chefWait());
        game.endTurn();
        return game.getOrders().get(0).getLockedPriceCents();
    }

    private static boolean throwsIllegalArgument(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    private static GameController typedGame(String itemName, ArrivalSchedule schedule) {
        Menu single = new Menu(List.of(Menu.standard().itemNamed(itemName)));
        return new GameController(GameConfig.base(SEED).withMenu(single), schedule);
    }

    /** Plays the given number of all-wait turns and returns C1. */
    private static Customer waitTurns(GameController game, int turns) {
        for (int i = 0; i < turns; i++) {
            game.beginTurn();
            waitBoth(game);
            game.endTurn();
        }
        return game.getCustomers().get(0);
    }

    /** Seats one customer of the given type on turn 1 and takes its order (single-item menu) on turn 2. */
    private static long lockedPriceFor(String itemName, ArrivalSchedule.CustomerFactory type) {
        GameController game = typedGame(itemName, ArrivalSchedule.builder(SEED).add(1, type).build());
        game.beginTurn();
        ok(game.waiterSeat("C1", "T1"));
        ok(game.chefWait());
        game.endTurn();
        game.beginTurn();
        ok(game.waiterTakeOrder("C1"));
        ok(game.chefWait());
        game.endTurn();
        return game.getOrders().get(0).getLockedPriceCents();
    }

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
