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
 * Checks the web adapter against the real CLI: replays the Java project's Stage 1 input scripts through
 * GameSession (answering prompts in the CLI's order) and compares every event line with the CLI transcripts. Also checks save/replay and the decision questions. The mode, schedule and timing tests are in {@link ModeSelfTest}.
 * Usage: java ... rush.web.AdapterSelfTest [backend-dir]
 */
public final class AdapterSelfTest {

    private static int passed;
    private static int failed;

    private AdapterSelfTest() {
    }

    public static void main(String[] args) throws IOException {
        Path backend = Paths.get(args.length > 0 ? args[0] : "../restaurant-rush");
        for (String name : List.of("stage1-victory", "stage1-defeat")) {
            cliParity(backend, name);
        }
        saveReplay();
        outOfTurnRejected();
        tableQuestions();
        System.out.println("AdapterSelfTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** Replays a CLI script (seed 2002, the same six Regulars as the CLI) and compares every event line. */
    private static void cliParity(Path backend, String name) throws IOException {
        GameSession s = replayScript(backend, name, GameSession.start(2002, ModeFeatures.STAGE1, 1));
        check(name + ": " + eventLines(s).size() + " event lines identical to the CLI log",
                eventLines(s).equals(cliEventLines(backend, name)));
        String result = "RESULT: " + (s.game().getResult().isVictory() ? "VICTORY" : "DEFEAT");
        check(name + ": " + result, result.equals(cliResultLine(backend, name)));
    }

    /** Feeds a CLI input script (runs/NAME.txt) to a session, asking roles in the CLI's prompt order: Waiter, Chef. */
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
        GameSession s = GameSession.start(42, ModeFeatures.STAGE1, 1);
        s.perform("waiterSeat", "C1", "T1", "");
        s.perform("chefWait", "", "", "");
        s.next();
        s.perform("waiterTakeOrder", "C1", "", "");
        s.perform("chefCook", "", "", "O1");
        s.next();
        GameSession copy = GameSession.replay(42, ModeFeatures.STAGE1, 1, s.actionLog());
        check("replayed save has same turn and phase",
                copy.game().getCurrentTurn() == 3 && copy.phase() == s.phase());
        check("replayed save has same order and satisfaction",
                copy.game().getOrders().get(0).getItem().getName().equals(s.game().getOrders().get(0).getItem().getName())
                        && copy.game().getCustomers().get(0).getSatisfaction() == s.game().getCustomers().get(0).getSatisfaction());
        check("state JSON is identical after replay", StateJson.of(copy).equals(StateJson.of(s)));
    }

    private static void outOfTurnRejected() {
        GameSession s = GameSession.start(1, ModeFeatures.STAGE1, 1);
        check("first decision is the Waiter's", s.phase() == GameSession.Phase.WAITER);
        check("Chef command before the Waiter is rejected", !s.perform("chefWait", "", "", "").isSuccess());
        check("unknown command rejected", !s.perform("dance", "", "", "").isSuccess());
        check("next() refused mid-turn", !s.next().isSuccess());
    }

    /** The front end asks "which table?" for seat / take order / serve, listing every table. */
    private static void tableQuestions() {
        GameSession s = GameSession.start(42, ModeFeatures.STAGE1, 1);
        String seat = StateJson.of(s);
        check("seat asks who, then which table",
                seat.contains("\"prompt\":\"Who should I seat?\"") && seat.contains("\"prompt\":\"Which table should I seat them at?\""));
        s.perform("waiterSeat", "C1", "T1", "");
        s.perform("chefWait", "", "", "");
        s.next();
        String waiter = StateJson.of(s);
        check("take order asks which table", waiter.contains("Which table should I take the order from?"));
        check("table 1 offered for C1's order", waiter.contains("\"value\":\"C1\",\"label\":\"Table 1\""));
        check("empty table 2 shown but disabled with a reason",
                waiter.contains("\"label\":\"Table 2\",\"detail\":null,\"enabled\":false,\"reason\":\"Empty table\""));
        check("day number in the state", waiter.contains("\"day\":1"));
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
