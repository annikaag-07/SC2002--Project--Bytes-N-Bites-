package rush.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import rush.control.GameController;
import rush.control.GameEvent;
import rush.entity.ActionResult;
import rush.entity.Customer;
import rush.entity.CustomerState;

/**
 * Checks the web adapter against the real CLI: replays the Java project's Stage 3 input scripts through
 * GameSession in "classic" mode (answering prompts in the CLI's order) and compares every event line with the
 * CLI transcripts. Also checks save/replay, the Custom-mode extras (Impatient guest, sub-types, tips) and the
 * decision questions. The mode, schedule and timing tests are in {@link ModeSelfTest}.
 * Usage: java ... rush.web.AdapterSelfTest [backend-dir]
 */
public final class AdapterSelfTest {

    private static int passed;
    private static int failed;

    private AdapterSelfTest() {
    }

    public static void main(String[] args) throws IOException {
        Path backend = Paths.get(args.length > 0 ? args[0] : "../restaurant-rush");
        for (String name : List.of("stage3-victory", "stage3-defeat", "stage3-features-demo")) {
            cliParity(backend, name);
        }
        saveReplay();
        impatientCustomer();
        outOfTurnRejected();
        tableQuestions();
        subtypeRoll();
        subtypeBehaviour();
        happyHourTips();
        System.out.println("AdapterSelfTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** Same prompt order as ConsoleUI: Happy Hour (if available), Host, Waiter, Chef; invalid input re-asks. */
    private static void cliParity(Path backend, String name) throws IOException {
        GameSession s = replayScript(backend, name, GameSession.start(2002, ModeFeatures.CLASSIC, 1));
        check(name + ": " + eventLines(s).size() + " event lines identical to the CLI log",
                eventLines(s).equals(cliEventLines(backend, name)));
        String result = "RESULT: " + (s.game().getResult().isVictory() ? "VICTORY" : "DEFEAT");
        check(name + ": " + result, result.equals(cliResultLine(backend, name)));
    }

    /**
     * Feeds a CLI input script (runs/NAME.txt) to a session, asking roles in the CLI's prompt order. Modes without
     * Happy Hour or a Host never ask those prompts, exactly like the Stage 1 and Stage 2 terminal games.
     */
    static GameSession replayScript(Path backend, String name, GameSession s) throws IOException {
        List<String> input = new ArrayList<>();
        for (String line : Files.readAllLines(backend.resolve("runs/" + name + ".txt"), StandardCharsets.UTF_8)) {
            if (!line.trim().startsWith("#")) {
                input.add(line.trim());
            }
        }
        Reader in = new Reader(input);
        while (s.phase() != GameSession.Phase.OVER) {
            switch (s.phase()) {
                case HAPPY_HOUR -> {
                    String a = in.next("n");
                    if (a.equals("y")) {
                        s.perform("happyHourYes", "", "", "");
                    } else if (a.equals("n")) {
                        s.perform("happyHourNo", "", "", "");
                    }
                }
                case HOST -> {
                    String c = in.next("0");
                    if (c.equals("0")) {
                        s.perform("hostWait", "", "", "");
                    } else if (c.equals("1")) {
                        String customer = in.id("C");
                        s.perform("hostSeat", customer, in.id("T"), "");
                    }
                }
                case WAITER -> {
                    String c = in.next("0");
                    switch (c) {
                        case "0" -> s.perform("waiterWait", "", "", "");
                        case "1" -> {
                            String customer = in.id("C");
                            s.perform("waiterSeat", customer, in.id("T"), "");
                        }
                        case "2" -> s.perform("waiterTakeOrder", in.id("C"), "", "");
                        case "3" -> s.perform("waiterServe", "", "", in.id("O"));
                        default -> { }
                    }
                }
                case CHEF -> {
                    String c = in.next("0");
                    if (c.equals("0")) {
                        s.perform("chefWait", "", "", "");
                    } else if (c.equals("1")) {
                        s.perform("chefCook", "", "", in.id("O"));
                    }
                }
                case SUMMARY -> s.next();
                default -> { }
            }
        }
        return s;
    }

    /** The "  * " event lines of a CLI transcript (runs/logs/NAME.log), without staff "waits" lines. */
    static List<String> cliEventLines(Path backend, String name) throws IOException {
        List<String> expected = new ArrayList<>();
        for (String line : Files.readAllLines(backend.resolve("runs/logs/" + name + ".log"), StandardCharsets.UTF_8)) {
            if (line.startsWith("  * ") && !line.contains(" waits this turn.")) {
                expected.add(line.substring(4));
            }
        }
        return expected;
    }

    static String cliResultLine(Path backend, String name) throws IOException {
        for (String line : Files.readAllLines(backend.resolve("runs/logs/" + name + ".log"), StandardCharsets.UTF_8)) {
            if (line.startsWith("RESULT:")) {
                return line;
            }
        }
        return "";
    }

    static List<String> eventLines(GameSession s) {
        List<String> actual = new ArrayList<>();
        for (GameEvent e : s.game().getEventLog()) {
            if (!e.getText().contains(" waits this turn.")) {
                actual.add(e.getText());
            }
        }
        return actual;
    }

    private static void saveReplay() {
        GameSession s = GameSession.start(42, ModeFeatures.STAGE3, 1);
        s.perform("happyHourNo", "", "", "");
        s.perform("hostSeat", "C1", "T1", "");
        s.perform("waiterTakeOrder", "C1", "", "");
        s.perform("chefCook", "", "", "O1");
        s.next();
        GameSession copy = GameSession.replay(42, ModeFeatures.STAGE3, 1, s.actionLog());
        check("replayed save has same turn and phase",
                copy.game().getCurrentTurn() == 2 && copy.phase() == s.phase());
        check("replayed save has same order and satisfaction",
                copy.game().getOrders().get(0).getItem().getName().equals(s.game().getOrders().get(0).getItem().getName())
                        && copy.game().getCustomers().get(0).getSatisfaction() == s.game().getCustomers().get(0).getSatisfaction());
        check("state JSON is identical after replay", StateJson.of(copy).equals(StateJson.of(s)));
    }

    /** Custom mode: the Impatient guest is C4 on turn 10 (loses 10 a turn, walks out at 30 or below). */
    private static void impatientCustomer() {
        GameSession s = GameSession.start(7, ModeFeatures.CUSTOM, 1);
        Customer impatient = null;
        int at13 = -1;
        int at15 = -1;
        boolean allAccepted = true;
        while (s.phase() != GameSession.Phase.OVER) {
            if (s.phase() == GameSession.Phase.SUMMARY) {
                if (impatient != null && s.game().getCurrentTurn() == 13) {
                    at13 = impatient.getSatisfaction();
                }
                if (impatient != null && s.game().getCurrentTurn() == 15) {
                    at15 = impatient.getSatisfaction();
                }
                s.next();
                continue;
            }
            ActionResult r = switch (s.phase()) {
                case HAPPY_HOUR -> s.perform("happyHourNo", "", "", "");
                case HOST -> s.perform("hostWait", "", "", "");
                case WAITER -> s.perform("waiterWait", "", "", "");
                default -> s.perform("chefWait", "", "", "");
            };
            allAccepted &= r.isSuccess();
            for (Customer c : s.game().getCustomers()) {
                if (c.typeLabel().equals("Impatient")) {
                    impatient = c;
                }
            }
        }
        check("every all-wait command accepted", allAccepted);
        check("custom schedule: the Impatient customer is C4, arriving on turn 10",
                impatient != null && impatient.getId().equals("C4") && impatient.getArrivalTurn() == 10);
        // Turn 10: 100->90, 11: 80, 12: 70, 13: 60, 14: 50, 15: 40; on turn 16 it would reach 30, so it walks out.
        check("Impatient loses 10 per waiting turn (60 after 4 turns)", at13 == 60);
        check("Impatient still waiting at 40 after 6 turns", at15 == 40);
        check("Impatient walks out at the end of turn 16", impatient != null && impatient.getState() == CustomerState.LEFT);
        GameSession classic = GameSession.start(7, ModeFeatures.CLASSIC, 1);
        boolean none = classic.upcomingArrivals().stream().noneMatch(c -> c.typeLabel().equals("Impatient"));
        check("classic schedule is the unchanged base setting", none);
        for (ModeFeatures mode : List.of(ModeFeatures.STAGE1, ModeFeatures.STAGE2, ModeFeatures.STAGE3)) {
            check(mode.label() + " has no Impatient customer", GameSession.start(7, mode, 1).upcomingArrivals().stream()
                    .noneMatch(c -> c.typeLabel().equals("Impatient")));
        }
        ImpatientCustomer probe = new ImpatientCustomer("X", 9, 1, 7);
        for (int i = 0; i < 6; i++) {
            probe.applyWaitingPenalty();
        }
        check("probe at 40 after 6 penalties", probe.getSatisfaction() == 40);
        probe.applyWaitingPenalty();
        check("walks out when it would fall to 30 (satisfaction 0)", probe.getSatisfaction() == 0);
    }

    private static void outOfTurnRejected() {
        GameSession s = GameSession.start(1, ModeFeatures.STAGE3, 1);
        check("waiter command during Happy Hour question is rejected",
                !s.perform("waiterWait", "", "", "").isSuccess());
        check("unknown command rejected", !s.perform("dance", "", "", "").isSuccess());
        check("next() refused mid-turn", !s.next().isSuccess());
    }

    /** The front end asks "which table?" for seat / take order / serve, listing every table. */
    private static void tableQuestions() {
        GameSession s = GameSession.start(42, ModeFeatures.STAGE3, 1);
        s.perform("happyHourNo", "", "", "");
        String host = StateJson.of(s);
        check("host seat asks who, then which table",
                host.contains("\"prompt\":\"Who should I seat?\"") && host.contains("\"prompt\":\"Which table should I seat them at?\""));
        s.perform("hostSeat", "C1", "T1", "");
        String waiter = StateJson.of(s);
        check("take order asks which table", waiter.contains("Which table should I take the order from?"));
        check("table 1 offered for C1's order", waiter.contains("\"value\":\"C1\",\"label\":\"Table 1\""));
        check("empty table 2 shown but disabled with a reason",
                waiter.contains("\"label\":\"Table 2\",\"detail\":null,\"enabled\":false,\"reason\":\"Empty table\""));
        check("day number in the state", waiter.contains("\"day\":1"));
    }

    // ------------------------------------------------------------------ sub-types and tips

    private static void subtypeRoll() {
        int[] counts = new int[5];
        int games = 20000;
        for (long seed = 1; seed <= games; seed++) {
            List<CustomerSubtypes.SubType> r = CustomerSubtypes.roll(4, CustomerSubtypes.subtypeRandom(seed));
            counts[(int) r.stream().filter(t -> t != CustomerSubtypes.SubType.NORMAL).count()]++;
        }
        boolean close = true;
        StringBuilder seen = new StringBuilder();
        for (int k = 0; k <= 4; k++) {
            double pct = 100.0 * counts[k] / games;
            seen.append(k).append(':').append(String.format("%.1f", pct)).append("% ");
            close &= Math.abs(pct - CustomerSubtypes.SPECIALISED_COUNT_ODDS[k]) < 1.5;
        }
        check("specialised-count odds match 10/60/20/7/3 over " + games + " games (" + seen.toString().trim() + ")", close);
        GameSession a = GameSession.start(99, ModeFeatures.CUSTOM, 1);
        GameSession b = GameSession.start(99, ModeFeatures.CUSTOM, 1);
        check("same seed, same sub-types", a.subTypes().equals(b.subTypes()));
        boolean exactlyRegulars = true;
        for (long seed = 1; seed <= 500; seed++) {
            GameSession g = GameSession.start(seed, ModeFeatures.CUSTOM, 1);
            java.util.Set<String> regulars = new java.util.HashSet<>();
            for (Customer c : g.upcomingArrivals()) {
                if (c.typeLabel().equals("Regular")) {
                    regulars.add(c.getId());
                }
            }
            for (Customer c : g.game().getCustomers()) {
                if (c.typeLabel().equals("Regular")) {
                    regulars.add(c.getId());
                }
            }
            exactlyRegulars &= g.subTypes().keySet().equals(regulars);
        }
        check("custom: exactly the Regular customers are rolled (500 seeds)", exactlyRegulars);
        for (ModeFeatures mode : List.of(ModeFeatures.STAGE1, ModeFeatures.STAGE2, ModeFeatures.STAGE3, ModeFeatures.CLASSIC)) {
            check(mode.label() + " has no sub-types", GameSession.start(99, mode, 1).subTypes().isEmpty());
        }
    }

    /** First seed whose C1 has the given sub-type. */
    private static long seedWithC1(CustomerSubtypes.SubType type) {
        for (long seed = 1; seed < 5000; seed++) {
            if (GameSession.start(seed, ModeFeatures.CUSTOM, 1).subTypeOf("C1") == type) {
                return seed;
            }
        }
        throw new IllegalStateException("no seed with C1 " + type);
    }

    /** Host seats C1, Waiter takes the order, Chef cooks until READY; returns the session in the next turn. */
    private static GameSession seatAndCook(long seed) {
        GameSession s = GameSession.start(seed, ModeFeatures.CUSTOM, 1);
        s.perform("happyHourNo", "", "", "");
        s.perform("hostSeat", "C1", "T1", "");
        s.perform("waiterTakeOrder", "C1", "", "");
        s.perform("chefCook", "", "", "O1");
        s.next();
        while (s.game().getOrders().get(0).getStatus() != rush.entity.OrderStatus.READY) {
            s.perform("happyHourNo", "", "", "");
            s.perform("hostWait", "", "", "");
            s.perform("waiterWait", "", "", "");
            s.perform("chefCook", "", "", "O1");
            s.next();
        }
        s.perform("happyHourNo", "", "", "");
        s.perform("hostWait", "", "", "");
        return s;
    }

    private static void subtypeBehaviour() {
        // Picky: sends the food back on the first serve (same turn), accepts the second.
        GameSession picky = seatAndCook(seedWithC1(CustomerSubtypes.SubType.PICKY));
        Customer p = picky.game().getCustomers().get(0);
        ActionResult first = picky.perform("waiterServe", "", "", "O1");
        check("picky: first serve is sent back in the same turn", first.isSuccess() && first.getMessage().contains("back")
                && p.getState() == CustomerState.ORDERED);
        picky.perform("chefWait", "", "", "");
        picky.next();
        picky.perform("happyHourNo", "", "", "");
        picky.perform("hostWait", "", "", "");
        ActionResult second = picky.perform("waiterServe", "", "", "O1");
        check("picky: second serve accepted", second.isSuccess() && p.getState() == CustomerState.EATING);

        // Chatty vs Normal: READY_TO_PAY one turn later; no satisfaction lost while eating.
        int normalTurn = readyToPayDelay(seedWithC1(CustomerSubtypes.SubType.NORMAL));
        GameSession chatty = seatAndCook(seedWithC1(CustomerSubtypes.SubType.CHATTY));
        chatty.perform("waiterServe", "", "", "O1");
        int servedTurn = chatty.game().getCurrentTurn();
        Customer c = chatty.game().getCustomers().get(0);
        int satAtServe = c.getSatisfaction();
        chatty.perform("chefWait", "", "", "");
        int rtp = -1;
        while (rtp < 0 && chatty.game().getCurrentTurn() < 18) {
            chatty.next();
            chatty.perform("happyHourNo", "", "", "");
            chatty.perform("hostWait", "", "", "");
            chatty.perform("waiterWait", "", "", "");
            chatty.perform("chefWait", "", "", "");
            if (c.getState() == CustomerState.READY_TO_PAY) {
                rtp = chatty.game().getCurrentTurn();
            }
        }
        check("chatty: ready to pay two turns after being served (normal: one)", rtp - servedTurn == 2 && normalTurn == 1);
        check("chatty: no satisfaction lost during the extra turn", c.getSatisfaction() == satAtServe);

        // Grumpy: starts at 80-89; Happy Hour recovery doubled.
        boolean inRange = true;
        for (long seed = 1; seed < 3000; seed++) {
            GameSession g = GameSession.start(seed, ModeFeatures.CUSTOM, 1);
            if (g.subTypeOf("C1") == CustomerSubtypes.SubType.GRUMPY) {
                int start = g.game().getCustomers().get(0).getSatisfaction();
                inRange &= start >= 80 && start <= 89;
            }
        }
        check("grumpy: every start satisfaction is 80-89", inRange);
        GameSession grumpy = GameSession.start(seedWithC1(CustomerSubtypes.SubType.GRUMPY), ModeFeatures.CUSTOM, 1);
        Customer gc = grumpy.game().getCustomers().get(0);
        int before = gc.getSatisfaction();
        grumpy.perform("happyHourYes", "", "", "");
        check("grumpy: Happy Hour +15 doubled to +30 (capped at 100)", gc.getSatisfaction() == Math.min(100, before + 30));
    }

    /** Turns from serving C1 to C1 becoming READY_TO_PAY, with nothing else happening. */
    private static int readyToPayDelay(long seed) {
        GameSession s = seatAndCook(seed);
        s.perform("waiterServe", "", "", "O1");
        int served = s.game().getCurrentTurn();
        s.perform("chefWait", "", "", "");
        Customer c = s.game().getCustomers().get(0);
        while (c.getState() != CustomerState.READY_TO_PAY && s.game().getCurrentTurn() < 18) {
            s.next();
            s.perform("happyHourNo", "", "", "");
            s.perform("hostWait", "", "", "");
            s.perform("waiterWait", "", "", "");
            s.perform("chefWait", "", "", "");
        }
        return c.getReadyToPayTurn() - served;
    }

    private static void happyHourTips() {
        boolean tipped = false;
        boolean allValid = true;
        boolean revenueClean = true;
        boolean onPaymentTurn = true;
        for (long seed = 1; seed <= 300; seed++) {
            GameSession s = GameSession.start(seed, ModeFeatures.CUSTOM, 1);
            s.perform("happyHourYes", "", "", "");
            s.perform("hostSeat", "C1", "T1", "");
            s.perform("waiterTakeOrder", "C1", "", "");
            playOut(s);
            long bills = 0;
            for (Customer c : s.game().getCustomers()) {
                if (c.getState() == CustomerState.PAID) {
                    bills += c.getOrder().getLockedPriceCents();
                }
            }
            revenueClean &= s.game().getRevenueCents() == bills;
            for (GameSession.Tip tip : s.tips()) {
                Customer c = s.game().getCustomers().stream().filter(x -> x.getId().equals(tip.customerId())).findFirst().get();
                long max = rush.entity.Money.percent(c.getOrder().getLockedPriceCents(), 20);
                allValid &= tip.customerId().equals("C1") && tip.cents() > 0 && tip.cents() <= max
                        && c.getRecordedSatisfaction() > 80;
                // Paid at the end of turn readyToPay + k (k >= 1): the tip lands on that same turn.
                onPaymentTurn &= tip.turn() > c.getReadyToPayTurn() && paidOn(s, c.getId()) == tip.turn();
                tipped = true;
            }
        }
        check("tips: some Happy Hour customers tip", tipped);
        check("tips: only the discounted customer, satisfaction > 80, at most 20% of the bill", allValid);
        check("tips never change revenue (revenue = sum of paid bills)", revenueClean);
        check("tips land on the turn the Cashier charged that customer (end of turn)", onPaymentTurn);
        boolean noTips = true;
        for (ModeFeatures mode : List.of(ModeFeatures.STAGE3, ModeFeatures.CLASSIC)) {
            for (long seed = 1; seed <= 100; seed++) {
                GameSession s = GameSession.start(seed, mode, 1);
                s.perform("happyHourYes", "", "", "");
                s.perform("hostSeat", "C1", "T1", "");
                s.perform("waiterTakeOrder", "C1", "", "");
                playOut(s);
                noTips &= s.tips().isEmpty() && s.tipsCents() == 0;
            }
        }
        check("tips are Custom-only: none in Stage 3 or classic, even with Happy Hour (200 games)", noTips);
        GameSession plain = GameSession.start(5, ModeFeatures.CUSTOM, 1);
        playOut(plain);
        check("no Happy Hour: no tips", plain.tips().isEmpty());
        GameSession saved = GameSession.start(17, ModeFeatures.CUSTOM, 1);
        saved.perform("happyHourYes", "", "", "");
        saved.perform("hostSeat", "C1", "T1", "");
        saved.perform("waiterTakeOrder", "C1", "", "");
        playOut(saved);
        GameSession replayed = GameSession.replay(17, ModeFeatures.CUSTOM, 1, saved.actionLog());
        check("replay: same sub-types and tips", replayed.subTypes().equals(saved.subTypes()) && replayed.tips().equals(saved.tips()));
    }

    /** Turn on which a customer's payment appears in the event log, or -1. */
    static int paidOn(GameSession s, String customerId) {
        for (GameEvent e : s.game().getEventLog()) {
            if (e.getText().startsWith("Cashier collects ") && e.getText().contains(" from " + customerId + " for ")) {
                return e.getTurn();
            }
        }
        return -1;
    }

    /** Greedy play to the end: serve, take order, seat, cook. */
    static void playOut(GameSession s) {
        for (int guard = 0; guard < 400 && s.phase() != GameSession.Phase.OVER; guard++) {
            GameController g = s.game();
            switch (s.phase()) {
                case HAPPY_HOUR -> s.perform("happyHourNo", "", "", "");
                case HOST -> s.perform("hostWait", "", "", "");
                case WAITER -> {
                    if (!g.getServableOrders().isEmpty()) {
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
                    if (g.getPreparableOrders().isEmpty()) {
                        s.perform("chefWait", "", "", "");
                    } else {
                        s.perform("chefCook", "", "", g.getPreparableOrders().get(0).getId());
                    }
                }
                case SUMMARY -> s.next();
                default -> { }
            }
        }
    }

    static void check(String what, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("PASS " + what);
        } else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }

    /** Input lines with the CLI's defaults at end of input and its bare-number id shorthand. */
    static final class Reader {
        private final List<String> lines;
        private int i;

        Reader(List<String> lines) {
            this.lines = lines;
        }

        String next(String def) {
            return i < lines.size() ? lines.get(i++) : def;
        }

        String id(String prefix) {
            String t = next("");
            return t.matches("\\d+") ? prefix + t : t;
        }
    }
}
