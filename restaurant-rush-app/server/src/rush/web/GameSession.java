package rush.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rush.control.ArrivalSchedule;
import rush.control.GameConfig;
import rush.control.GameController;
import rush.control.GameEvent;
import rush.entity.ActionResult;
import rush.entity.Customer;
import rush.entity.CriticCustomer;
import rush.entity.Menu;
import rush.entity.RegularCustomer;
import rush.entity.VIPCustomer;
import rush.web.ArrivalPlan.Kind;

/**
 * Adapter between the web front end and the unchanged back end. Like the CLI's ConsoleUI it is a boundary:
 * it decides which role is asked next (Happy Hour while it is available, Host, Waiter, Chef), forwards the chosen
 * command to the {@link GameController}, and closes the turn after the Chef. It never applies a game rule itself.
 * The Cashier charges in the back end's step 4, at the end of a turn (brief section 3.2).
 *
 * <p>Every successful command is appended to an action log. The game is deterministic for a seed, so replaying
 * the log rebuilds the exact state; that is how "Continue" works without serialising back-end objects.</p>
 */
final class GameSession {

    /** The decision the front end should ask for next. */
    enum Phase { HAPPY_HOUR, HOST, WAITER, CHEF, SUMMARY, OVER }

    private final long seed;
    private final ModeFeatures mode;
    private final int day;
    private final ArrivalSchedule schedule;
    private final GameController game;
    private final List<String> actionLog = new ArrayList<>();
    private boolean happyHourAnswered;
    private boolean over;
    private List<GameEvent> lastTurnEvents = List.of();
    private List<GameEvent> turnStartEvents = List.of();

    private GameSession(long seed, ModeFeatures mode, int day, int[] turns, List<Kind> kinds, Menu menu) {
        this.seed = seed;
        this.mode = mode;
        this.day = day;
        this.schedule = buildSchedule(seed, turns, kinds);
        this.game = new GameController(GameConfig.base(seed).withMenu(menu), schedule);
    }

    /**
     * Starts a game on turn 1.
     *
     * @param seed game seed
     * @param mode the mode (see {@link ModeFeatures})
     * @param day  which restaurant day this is (shown to players instead of the seed)
     */
    static GameSession start(long seed, ModeFeatures mode, int day) {
        return begin(new GameSession(seed, mode, day, ArrivalPlan.ARRIVAL_TURNS, ArrivalPlan.kinds(seed, mode),
                Menu.standard()));
    }

    /**
     * Test-only start with an explicit arrival list and menu (the production path is {@link #start}).
     *
     * @param seed  game seed (still drives the customers' food choices)
     * @param mode  the mode
     * @param turns arrival turns, non-decreasing
     * @param kinds who arrives on each of those turns
     * @param menu  the menu customers choose from
     */
    static GameSession startWith(long seed, ModeFeatures mode, int[] turns, List<Kind> kinds, Menu menu) {
        return begin(new GameSession(seed, mode, 1, turns, kinds, menu));
    }

    private static GameSession begin(GameSession session) {
        session.turnStartEvents = session.game.beginTurn();
        return session;
    }

    /** Rebuilds a session by replaying a saved action log. */
    static GameSession replay(long seed, ModeFeatures mode, int day, List<String> log) {
        GameSession session = start(seed, mode, day);
        for (String line : log) {
            String[] f = line.split("\t", -1);
            ActionResult result = f[0].equals("next")
                    ? session.next()
                    : session.perform(f[0], f.length > 1 ? f[1] : "", f.length > 2 ? f[2] : "", f.length > 3 ? f[3] : "");
            if (!result.isSuccess()) {
                throw new IllegalStateException("save does not replay: " + line + " -> " + result.getMessage());
            }
        }
        return session;
    }

    /**
     * Builds the back end's arrival schedule from the plan's kinds. This is the only place in the adapter that
     * names customer classes.
     */
    private static ArrivalSchedule buildSchedule(long seed, int[] turns, List<Kind> kinds) {
        ArrivalSchedule.Builder builder = ArrivalSchedule.builder(seed);
        for (int i = 0; i < kinds.size(); i++) {
            switch (kinds.get(i)) {
                case VIP -> builder.add(turns[i], VIPCustomer::new);
                case CRITIC -> builder.add(turns[i], CriticCustomer::new);
                default -> builder.add(turns[i], RegularCustomer::new);
            }
        }
        return builder.build();
    }

    /** @return the decision to ask for now */
    Phase phase() {
        if (over) {
            return Phase.OVER;
        }
        if (game.hasChefActed()) {
            // The Chef acted, so endTurn() has run: show the summary until next() is called.
            return Phase.SUMMARY;
        }
        if (game.isHappyHourAvailable() && !happyHourAnswered && !game.hasHostActed() && !game.hasWaiterActed()) {
            return Phase.HAPPY_HOUR;
        }
        if (!game.hasHostActed() && !game.hasWaiterActed()) {
            return Phase.HOST;
        }
        if (!game.hasWaiterActed()) {
            return Phase.WAITER;
        }
        return Phase.CHEF;
    }

    /**
     * Forwards one decision to the back end. The command must belong to the role being asked now.
     *
     * @param type     command, e.g. "waiterSeat"
     * @param customer customer id, if the command needs one
     * @param table    table id, if needed
     * @param order    order id, if needed
     * @return the back end's result (or why the command is out of turn)
     */
    ActionResult perform(String type, String customer, String table, String order) {
        Phase phase = phase();
        Phase expected = phaseOf(type);
        if (expected == null) {
            return ActionResult.fail("Unknown action '" + type + "'.");
        }
        if (expected != phase) {
            return ActionResult.fail("It is not the " + roleName(expected) + "'s turn to decide (now: "
                    + roleName(phase) + ").");
        }
        ActionResult result = switch (type) {
            case "happyHourYes" -> game.activateHappyHour();
            case "happyHourNo" -> ActionResult.ok("Happy Hour not started this turn.");
            case "hostSeat" -> game.hostSeat(customer, table);
            case "hostWait" -> game.hostWait();
            case "waiterSeat" -> game.waiterSeat(customer, table);
            case "waiterTakeOrder" -> game.waiterTakeOrder(customer);
            case "waiterServe" -> game.waiterServe(order);
            case "waiterWait" -> game.waiterWait();
            case "chefCook" -> game.chefAdvance(order);
            case "chefWait" -> game.chefWait();
            default -> ActionResult.fail("Unknown action '" + type + "'.");
        };
        if (!result.isSuccess()) {
            return result;
        }
        if (type.startsWith("happyHour")) {
            happyHourAnswered = true;
        }
        actionLog.add(String.join("\t", type, customer, table, order));
        if (phaseOf(type) == Phase.CHEF) {
            // The Chef was the last role: run payment, eating, waiting and abandonment.
            lastTurnEvents = game.endTurn();
        }
        return result;
    }

    /** Leaves the end-of-turn summary: starts the next turn, or finishes the game after the last one. */
    ActionResult next() {
        if (phase() != Phase.SUMMARY) {
            return ActionResult.fail("The turn is not finished yet.");
        }
        actionLog.add("next");
        if (game.isGameOver()) {
            over = true;
            return ActionResult.ok("Game over.");
        }
        turnStartEvents = game.beginTurn();
        happyHourAnswered = false;
        lastTurnEvents = List.of();
        return ActionResult.ok("Turn " + game.getCurrentTurn() + " begins.");
    }

    private static Phase phaseOf(String type) {
        if (type.startsWith("happyHour")) {
            return Phase.HAPPY_HOUR;
        }
        if (type.startsWith("host")) {
            return Phase.HOST;
        }
        if (type.startsWith("waiter")) {
            return Phase.WAITER;
        }
        if (type.startsWith("chef")) {
            return Phase.CHEF;
        }
        return null;
    }

    static String roleName(Phase phase) {
        return switch (phase) {
            case HAPPY_HOUR -> "Manager";
            case HOST -> "Host";
            case WAITER -> "Waiter";
            case CHEF -> "Chef";
            case SUMMARY -> "end-of-turn summary";
            case OVER -> "game-over screen";
        };
    }

    /** @return customers who have not arrived yet, in arrival order */
    List<Customer> upcomingArrivals() {
        List<Customer> upcoming = new ArrayList<>();
        for (Customer customer : schedule.getCustomers()) {
            if (customer.getArrivalTurn() > game.getCurrentTurn()) {
                upcoming.add(customer);
            }
        }
        return upcoming;
    }

    GameController game() {
        return game;
    }

    long seed() {
        return seed;
    }

    ModeFeatures mode() {
        return mode;
    }

    boolean isOver() {
        return over;
    }

    List<GameEvent> lastTurnEvents() {
        return lastTurnEvents;
    }

    /** Events at the start of the current turn: arrivals and continuing Happy Hour recovery. */
    List<GameEvent> turnStartEvents() {
        return turnStartEvents;
    }

    int day() {
        return day;
    }

    List<String> actionLog() {
        return Collections.unmodifiableList(actionLog);
    }
}
