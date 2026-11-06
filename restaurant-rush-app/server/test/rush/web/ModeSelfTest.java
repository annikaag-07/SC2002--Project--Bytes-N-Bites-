package rush.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import rush.control.GameController;
import rush.control.GameEvent;
import rush.entity.ActionResult;
import rush.entity.ComboMeal;
import rush.entity.Customer;
import rush.entity.CustomerState;
import rush.entity.Menu;
import rush.entity.MenuItem;
import rush.entity.Order;
import rush.entity.Table;
import rush.web.ArrivalPlan.Kind;

/**
 * Tests for the web game's mode: what it has, the seeded VIP / Critic schedule, the brief's eating and payment
 * timing (section 3.2 step 4 and the example in 3.3), and a bot that plays many seeds.
 * Usage: java ... rush.web.ModeSelfTest
 */
public final class ModeSelfTest {

    /** The mode players start from the menu. */
    private static final ModeFeatures MODE = ModeFeatures.STAGE3;
    private static final int SCHEDULE_SEEDS = 5000;
    private static final int BOT_SEEDS = 200;

    private static int passed;
    private static int failed;

    private ModeSelfTest() {
    }

    public static void main(String[] args) {
        featureMatrix();
        unknownModes();
        scheduleInvariants();
        briefExample();
        earliestTimelines();
        mixedDishes();
        botPlay();
        System.out.println("ModeSelfTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ what the mode has

    private static void featureMatrix() {
        String m = MODE.label() + ": ";
        GameSession s = GameSession.start(11, MODE, 1);
        check(m + "first decision is Happy Hour", s.phase() == GameSession.Phase.HAPPY_HOUR);
        check(m + "Happy Hour accepted", GameSession.start(11, MODE, 1).perform("happyHourYes", "", "", "").isSuccess()
                && s.perform("happyHourNo", "", "", "").isSuccess());
        check(m + "the Host is asked next and can seat", s.phase() == GameSession.Phase.HOST
                && s.perform("hostSeat", "C1", "T1", "").isSuccess());

        List<String> dishes = new ArrayList<>();
        boolean anyCombo = false;
        for (MenuItem item : s.game().getMenu().getOfferings()) {
            dishes.add(item.getName());
            anyCombo |= item instanceof ComboMeal;
        }
        check(m + "menu " + dishes, anyCombo && dishes.size() == 5);

        boolean vipSeen = false;
        boolean criticSeen = false;
        for (long seed = 1; seed <= 300; seed++) {
            for (Customer c : allCustomers(GameSession.start(seed, MODE, 1))) {
                vipSeen |= c.typeLabel().equals("VIP");
                criticSeen |= c.typeLabel().equals("Critic");
            }
        }
        check(m + "VIPs and Critics arrive (300 seeds)", vipSeen && criticSeen);

        String json = StateJson.of(GameSession.start(11, MODE, 1));
        check(m + "state JSON carries mode, label and the Happy Hour status",
                json.contains("\"mode\":\"" + MODE.id() + "\"") && json.contains("\"modeLabel\":\"" + MODE.label() + "\"")
                        && json.contains("\"happyHour\":{\"available\":true"));
    }

    private static void unknownModes() {
        for (String id : List.of("base", "", "STAGE3", "stage9")) {
            boolean threw;
            try {
                ModeFeatures.fromId(id);
                threw = false;
            } catch (IllegalArgumentException e) {
                threw = e.getMessage().contains("Unknown mode");
            }
            check("unknown mode '" + id + "' is rejected", threw);
        }
        check("high-score labels", ModeFeatures.labelFor("stage3").equals("Stage 3")
                && ModeFeatures.labelFor("classic").equals("Classic") && ModeFeatures.labelFor("other").equals("other"));
    }

    // ------------------------------------------------------------------ random VIP and Critic arrivals

    private static void scheduleInvariants() {
        boolean invariants = true;
        boolean deterministic = true;
        boolean twoVips = false;
        boolean twoCritics = false;
        boolean[] vipAt = new boolean[6];
        boolean[] criticAt = new boolean[6];
        for (long seed = 1; seed <= SCHEDULE_SEEDS; seed++) {
            List<Kind> k = ArrivalPlan.kinds(seed, MODE);
            deterministic &= k.equals(ArrivalPlan.kinds(seed, MODE));
            int vips = ArrivalPlan.count(k, Kind.VIP);
            int critics = ArrivalPlan.count(k, Kind.CRITIC);
            int regulars = ArrivalPlan.count(k, Kind.REGULAR);
            invariants &= k.size() == 6 && vips >= 1 && critics >= 1 && regulars >= 2;
            twoVips |= vips >= 2;
            twoCritics |= critics >= 2;
            for (int i = 0; i < 6; i++) {
                vipAt[i] |= k.get(i) == Kind.VIP;
                criticAt[i] |= k.get(i) == Kind.CRITIC;
            }
        }
        String m = MODE.label() + " schedule (" + SCHEDULE_SEEDS + " seeds): ";
        check(m + ">= 1 VIP, >= 1 Critic, >= 2 Regulars", invariants);
        check(m + "same seed, same schedule", deterministic);
        boolean everySlot = true;
        for (int i = 0; i < 6; i++) {
            everySlot &= vipAt[i] && criticAt[i];
        }
        check(m + "every slot sees a VIP and a Critic (incl. turns 1 and 16)", everySlot);
        check(m + "games with two VIPs and with two Critics occur", twoVips && twoCritics);
        // The built game really uses the plan, on the fixed turns, and a replay rebuilds the same customers.
        boolean matches = true;
        for (long seed = 1; seed <= 200; seed++) {
            GameSession g = GameSession.start(seed, MODE, 1);
            List<Kind> plan = ArrivalPlan.kinds(seed, MODE);
            List<Customer> all = allCustomers(g);
            for (int i = 0; i < 6; i++) {
                matches &= all.get(i).getArrivalTurn() == ArrivalPlan.ARRIVAL_TURNS[i]
                        && all.get(i).typeLabel().equalsIgnoreCase(plan.get(i).name());
            }
        }
        check("built games follow the plan on turns 1, 4, 7, 10, 13, 16 (200 seeds)", matches);
        GameSession a = GameSession.start(77, MODE, 1);
        AdapterSelfTest.playOut(a);
        GameSession b = GameSession.replay(77, MODE, 1, a.actionLog());
        check("Continue (replay) rebuilds the same schedule and result",
                labels(allCustomers(a)).equals(labels(allCustomers(b))) && StateJson.of(a).equals(StateJson.of(b)));
        check("classic keeps the fixed list Regular, VIP, Regular, Critic, Regular, Regular",
                labels(allCustomers(GameSession.start(5, ModeFeatures.CLASSIC, 1)))
                        .equals(List.of("Regular", "VIP", "Regular", "Critic", "Regular", "Regular")));
    }

    // ------------------------------------------------------------------ eating and paying (brief 3.2 and 3.3)

    /**
     * Brief 3.3: one Regular, Pasta, full attention, checked row by row. The Waiter seats the guest (the Host waits
     * and Happy Hour is not used), exactly as in the brief's example.
     */
    private static void briefExample() {
        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        GameSession s = GameSession.startWith(2002, MODE, new int[] {1}, List.of(Kind.REGULAR),
                new Menu(List.of(pasta)));
        Customer c = s.game().getCustomers().get(0);
        String[][] script = {
            {"waiterSeat C1 T1", "chefWait"},
            {"waiterTakeOrder C1", "chefCook O1"},
            {"waiterWait", "chefCook O1"},
            {"waiterServe O1", "chefWait"},
            {"waiterWait", "chefWait"},
            {"waiterWait", "chefWait"},
        };
        int[] satisfaction = {92, 84, 76, 76, 76};
        String[] stateAtEnd = {"SEATED", "ORDERED", "ORDERED", "EATING", "READY_TO_PAY", "PAID"};
        String[] event = {"Waiter seats C1 at T1.", "units 1/2", "units 2/2. READY", "Waiter serves O1",
            "C1 finishes eating: READY_TO_PAY", "Cashier collects $18.00 from C1 for O1. PAID; satisfaction 76 recorded; table T1 freed."};
        boolean rows = true;
        StringBuilder why = new StringBuilder();
        for (int turn = 1; turn <= 6; turn++) {
            if (turn == 6) {
                // Turn 6's decisions: the table is still occupied; the Cashier charges at the end of the turn.
                boolean held = s.game().getTables().get(0).getOccupant() == c
                        && !s.game().getFreeTables().contains(s.game().getTables().get(0));
                if (!held) {
                    why.append(" T1 not occupied during turn 6's decisions;");
                }
                rows &= held;
            }
            play(s, new String[] {"happyHourNo", "hostWait"});
            play(s, script[turn - 1]);
            String expectedEvent = event[turn - 1];
            boolean row = c.getState().name().equals(stateAtEnd[turn - 1])
                    && (turn == 6 ? c.getRecordedSatisfaction() == 76 : c.getSatisfaction() == satisfaction[turn - 1])
                    && turnEvents(s, turn).stream().anyMatch(e -> e.contains(expectedEvent));
            if (!row) {
                why.append(" turn ").append(turn).append(": ").append(c.getState()).append(' ').append(c.getSatisfaction());
            }
            rows &= row;
            s.next();
        }
        check("brief 3.3 example, row by row: seated 1 (92), order 2 (84), READY 3 (76), SERVED 4, READY_TO_PAY 5, "
                + "PAID end of 6 with $18.00 and 76 recorded" + why, rows);
        check("brief example: revenue $18.00 counted only at the end of turn 6",
                s.game().getRevenueCents() == 1800 && AdapterSelfTest.paidOn(s, "C1") == 6);
        check("brief example: table T1 free again from turn 7", s.game().getCurrentTurn() == 7
                && s.game().getFreeTables().contains(s.game().getTables().get(0)));
    }

    /**
     * The earliest timeline for a lone Regular arriving on turn a = 1 with perfect play (the Host seats, the Waiter
     * takes the order the same turn), for u = 1, 2, 3 preparation units.
     */
    private static void earliestTimelines() {
        MenuItem salad = new MenuItem("Salad", 900, 1);
        MenuItem pasta = new MenuItem("Pasta", 1800, 2);
        Map<Integer, MenuItem> dishOf = Map.of(1, salad, 2, pasta, 3, new ComboMeal(salad, pasta));
        int a = 1;
        for (int u = 1; u <= 3; u++) {
            GameSession s = GameSession.startWith(31, MODE, new int[] {a}, List.of(Kind.REGULAR),
                    new Menu(List.of(dishOf.get(u))));
            Timeline t = playPerfect(s, "C1");
            int[] expected = {a, a, a + u - 1, a + u, a + u + 1, a + u + 2, 100 - 8 * u};
            int[] actual = {t.seated, t.orderTaken, t.ready, t.served, t.readyToPay, t.paid, t.recorded};
            check("u=" + u + ": seated/order/READY/SERVED/READY_TO_PAY/PAID/recorded = " + java.util.Arrays.toString(actual),
                    java.util.Arrays.equals(expected, actual));
            check("u=" + u + ": table occupied during turn " + t.paid + "'s decisions, freed at its end, reusable from turn "
                    + (t.paid + 1), t.occupiedDuringPaidTurn && t.freedAtEndOf == t.paid && t.freeNextTurn);
        }
    }

    /**
     * One game, two diners with different dishes, same attention: C1 (Pasta, 2 units) is seated first, C2
     * (Salad, 1 unit) second, yet C2 is served, ready and paid first, each from its own engine state.
     */
    private static void mixedDishes() {
        Menu menu = new Menu(List.of(new MenuItem("Salad", 900, 1), new MenuItem("Pasta", 1800, 2)));
        for (long seed = 1; seed < 5000; seed++) {
            GameSession s = GameSession.startWith(seed, ModeFeatures.STAGE3, new int[] {1, 1},
                    List.of(Kind.REGULAR, Kind.REGULAR), menu);
            play(s, new String[] {"happyHourNo", "hostSeat C1 T1", "waiterTakeOrder C1", "chefCook O1"});
            s.next();
            play(s, new String[] {"happyHourNo", "hostSeat C2 T2", "waiterTakeOrder C2", "chefCook O2"});
            List<Order> orders = s.game().getOrders();
            if (!orders.get(0).getItem().getName().equals("Pasta") || !orders.get(1).getItem().getName().equals("Salad")) {
                continue;
            }
            s.next();
            play(s, new String[] {"happyHourNo", "hostWait", "waiterServe O2", "chefCook O1"});
            s.next();
            play(s, new String[] {"happyHourNo", "hostWait", "waiterServe O1", "chefWait"});
            for (int guard = 0; guard < 6; guard++) {
                s.next();
                play(s, new String[] {"happyHourNo", "hostWait", "waiterWait", "chefWait"});
            }
            Customer c1 = s.game().getCustomers().get(0);
            Customer c2 = s.game().getCustomers().get(1);
            int paid1 = AdapterSelfTest.paidOn(s, "C1");
            int paid2 = AdapterSelfTest.paidOn(s, "C2");
            check("mixed dishes (seed " + seed + "): Salad diner C2 served 3, READY_TO_PAY 4, paid 5; Pasta diner C1 "
                    + "served 4, READY_TO_PAY 5, paid 6 (got C2 " + c2.getReadyToPayTurn() + "/" + paid2 + ", C1 "
                    + c1.getReadyToPayTurn() + "/" + paid1 + ")",
                    c2.getReadyToPayTurn() == 4 && paid2 == 5 && c1.getReadyToPayTurn() == 5 && paid1 == 6);
            return;
        }
        check("mixed dishes: found a seed where C1 orders Pasta and C2 orders Salad", false);
    }

    /** Turns at which one customer reached each step, observed from the engine only. */
    private static final class Timeline {
        int seated = -1;
        int orderTaken = -1;
        int ready = -1;
        int served = -1;
        int readyToPay = -1;
        int paid = -1;
        int recorded = -1;
        int freedAtEndOf = -1;
        boolean occupiedDuringPaidTurn;
        boolean freeNextTurn;
    }

    /** Plays a lone customer perfectly (Host seats; Waiter serves > takes an order > seats; Chef cooks). */
    private static Timeline playPerfect(GameSession s, String id) {
        Timeline t = new Timeline();
        Customer c = s.game().getCustomers().get(0);
        Table table = null;
        while (s.phase() != GameSession.Phase.OVER) {
            int turn = s.game().getCurrentTurn();
            if (s.phase() == GameSession.Phase.WAITER && t.paid < 0 && table != null && turn > t.readyToPay && t.readyToPay > 0) {
                t.occupiedDuringPaidTurn = table.getOccupant() == c;
            }
            if (s.phase() == GameSession.Phase.WAITER && t.paid > 0 && turn == t.paid + 1) {
                t.freeNextTurn = s.game().getFreeTables().contains(table);
            }
            if (s.phase() == GameSession.Phase.SUMMARY) {
                if (t.seated < 0 && c.getTable() != null) {
                    t.seated = turn;
                    table = c.getTable();
                }
                if (t.served < 0 && (c.getState() == CustomerState.EATING || c.getState() == CustomerState.READY_TO_PAY)) {
                    t.served = turn;
                }
                if (t.paid < 0 && c.getState() == CustomerState.PAID) {
                    t.paid = turn;
                    t.recorded = c.getRecordedSatisfaction();
                    t.freedAtEndOf = table != null && table.isFree() ? turn : -1;
                }
                if (c.getOrder() != null) {
                    t.orderTaken = c.getOrder().getTakenTurn();
                    t.ready = c.getOrder().getReadyTurn();
                }
                t.readyToPay = c.getReadyToPayTurn() > 0 ? c.getReadyToPayTurn() : t.readyToPay;
            }
            greedyStep(s, null);
        }
        return t;
    }


    // ------------------------------------------------------------------ bot over many seeds

    private static void botPlay() {
        boolean noCrash = true;
        boolean neverEarly = true;
        boolean atMostFive = true;
        boolean tableHeld = true;
        int paidTotal = 0;
        for (long seed = 1; seed <= BOT_SEEDS; seed++) {
            try {
                GameSession s = GameSession.start(seed, MODE, 1);
                Random rng = new Random(seed);
                Map<String, Integer> paidAt = new HashMap<>();
                for (int guard = 0; guard < 1000 && s.phase() != GameSession.Phase.OVER; guard++) {
                    if (s.phase() == GameSession.Phase.WAITER) {
                        // Anyone READY_TO_PAY from an earlier turn still holds their table during the decisions.
                        for (Customer c : s.game().getCustomers()) {
                            if (c.getState() == CustomerState.READY_TO_PAY) {
                                tableHeld &= c.getTable() != null && c.getTable().getOccupant() == c;
                            }
                        }
                    }
                    if (s.phase() == GameSession.Phase.SUMMARY) {
                        for (Customer c : s.game().getCustomers()) {
                            if (c.getState() == CustomerState.PAID && paidAt.putIfAbsent(c.getId(), s.game().getCurrentTurn()) == null) {
                                neverEarly &= s.game().getCurrentTurn() >= c.getReadyToPayTurn() + 1
                                        && c.getReadyToPayTurn() >= c.getOrder().getServedTurn() + 1;
                            }
                        }
                    }
                    greedyStep(s, rng);
                }
                noCrash &= s.phase() == GameSession.Phase.OVER;
                atMostFive &= s.game().getPaidCount() <= 5;
                paidTotal += s.game().getPaidCount();
            } catch (RuntimeException e) {
                noCrash = false;
                System.out.println("  crash: " + MODE.id() + " seed " + seed + ": " + e);
            }
        }
        String m = MODE.label() + " bot, " + BOT_SEEDS + " seeds: ";
        check(m + "every game finishes (" + paidTotal + " payments in total)", noCrash && paidTotal > 0);
        check(m + "nobody pays before READY_TO_PAY + 1, nor becomes READY_TO_PAY before SERVED + 1", neverEarly);
        check(m + "READY_TO_PAY diners keep their table during the next turn's decisions", tableHeld);
        check(m + "never more than five paid customers", atMostFive);
    }

    // ------------------------------------------------------------------ helpers

    /**
     * One decision of a simple bot. With {@code rng == null} it is greedy (Host seats, Waiter serves > takes an
     * order > seats, Chef cooks the oldest ticket, no Happy Hour); with a generator it sometimes waits or starts
     * Happy Hour, so many different games come out of the same seeds.
     */
    private static void greedyStep(GameSession s, Random rng) {
        GameController g = s.game();
        boolean random = rng != null;
        switch (s.phase()) {
            case HAPPY_HOUR -> s.perform(random && rng.nextInt(6) == 0 ? "happyHourYes" : "happyHourNo", "", "", "");
            case HOST -> {
                if (!g.getQueue().isEmpty() && !g.getFreeTables().isEmpty() && !(random && rng.nextInt(5) == 0)) {
                    s.perform("hostSeat", g.getQueue().get(0).getId(), g.getFreeTables().get(0).getId(), "");
                } else {
                    s.perform("hostWait", "", "", "");
                }
            }
            case WAITER -> {
                if (random && rng.nextInt(7) == 0) {
                    s.perform("waiterWait", "", "", "");
                } else if (!g.getServableOrders().isEmpty()) {
                    s.perform("waiterServe", "", "", g.getServableOrders().get(0).getId());
                } else if (!g.getCustomersAwaitingOrder().isEmpty()) {
                    s.perform("waiterTakeOrder", g.getCustomersAwaitingOrder().get(0).getId(), "", "");
                } else if (!g.getQueue().isEmpty() && !g.getFreeTables().isEmpty()) {
                    s.perform("waiterSeat", g.getQueue().get(0).getId(), g.getFreeTables().get(0).getId(), "");
                } else {
                    s.perform("waiterWait", "", "", "");
                }
            }
            case CHEF -> {
                if (g.getPreparableOrders().isEmpty() || (random && rng.nextInt(7) == 0)) {
                    s.perform("chefWait", "", "", "");
                } else {
                    s.perform("chefCook", "", "", g.getPreparableOrders().get(0).getId());
                }
            }
            case SUMMARY -> s.next();
            default -> { }
        }
    }

    /** Performs scripted steps like "hostSeat C1 T1", "waiterServe O2" or "chefCook O1"; fails loudly if one is refused. */
    private static void play(GameSession s, String[] steps) {
        for (String step : steps) {
            String[] f = step.split(" ");
            String type = f[0];
            String a = f.length > 1 ? f[1] : "";
            String b = f.length > 2 ? f[2] : "";
            ActionResult r = switch (type) {
                case "waiterServe", "chefCook" -> s.perform(type, "", "", a);
                default -> s.perform(type, a, b, "");
            };
            if (!r.isSuccess()) {
                throw new IllegalStateException(step + " refused: " + r.getMessage());
            }
        }
    }

    private static List<String> turnEvents(GameSession s, int turn) {
        List<String> out = new ArrayList<>();
        for (GameEvent e : s.game().getEventLog()) {
            if (e.getTurn() == turn) {
                out.add(e.getText());
            }
        }
        return out;
    }

    /** Arrived and upcoming customers, in arrival order. */
    private static List<Customer> allCustomers(GameSession s) {
        List<Customer> all = new ArrayList<>(s.game().getCustomers());
        Set<String> seen = new HashSet<>();
        for (Customer c : all) {
            seen.add(c.getId());
        }
        for (Customer c : s.upcomingArrivals()) {
            if (seen.add(c.getId())) {
                all.add(c);
            }
        }
        return all;
    }

    private static List<String> labels(List<Customer> customers) {
        List<String> out = new ArrayList<>();
        for (Customer c : customers) {
            out.add(c.typeLabel());
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("PASS " + what);
        } else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }
}
