package rush.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import rush.control.GameController;
import rush.control.GameEvent;
import rush.control.GameResult;
import rush.entity.ComboMeal;
import rush.entity.Customer;
import rush.entity.MenuItem;
import rush.entity.Money;
import rush.entity.Order;
import rush.entity.OrderStatus;
import rush.entity.Table;

/**
 * Converts a {@link GameSession} into the JSON snapshot the front end renders, including the current
 * decision: which role is asked and which choices exist. Choices and their targets come straight from the
 * back end's own queries (free tables, seated customers, servable and preparable orders), so the UI never
 * decides what is legal; it only shows these lists.
 */
final class StateJson {

    private StateJson() {
    }

    static String of(GameSession session) {
        GameController g = session.game();
        Json.Obj state = new Json.Obj()
                .put("seed", Long.toString(session.seed()))
                .put("day", session.day())
                .put("mode", session.mode().id())
                .put("modeLabel", session.mode().label())
                .raw("menu", menu(g))
                .put("turn", g.getCurrentTurn())
                .put("totalTurns", g.getTotalTurns())
                .put("phase", session.phase().name())
                .put("revenueCents", g.getRevenueCents())
                .put("revenue", Money.format(g.getRevenueCents()))
                .put("averageSatisfaction", g.getAverageSatisfactionText())
                .put("paidCount", g.getPaidCount())
                .put("servedCount", g.getServedCount())
                .put("departureCount", g.getDepartureCount())
                .raw("targets", new Json.Obj().put("paid", 4).put("revenueCents", 4500).put("revenue", "$45.00")
                        .put("averageSatisfaction", 60).toString())
                .raw("happyHour", new Json.Obj().put("available", g.isHappyHourAvailable())
                        .put("remainingTurns", g.getHappyHourRemainingTurns()).toString())
                .raw("queue", ids(g.getQueue()))
                .raw("tables", tables(g))
                .raw("customers", customers(session))
                .raw("orders", orders(g))
                .raw("nextArrival", nextArrival(session))
                .raw("cashierNext", g.getNextPayingCustomer().map(c -> Json.str(c.getId())).orElse("null"))
                .raw("turnSummary", events(session.lastTurnEvents()))
                .raw("turnStartEvents", events(session.turnStartEvents()))
                .raw("log", events(g.getEventLog()))
                .raw("decision", decision(session))
                .raw("result", session.isOver() || g.isGameOver() ? result(g.getResult()) : "null");
        return state.toString();
    }

    private static String menu(GameController g) {
        List<String> out = new ArrayList<>();
        for (MenuItem item : g.getMenu().getOfferings()) {
            out.add(new Json.Obj().put("name", item.getName()).put("priceCents", item.getPriceCents())
                    .put("price", Money.format(item.getPriceCents())).put("prepUnits", item.getPrepUnits()).toString());
        }
        return Json.arr(out);
    }

    static String events(List<GameEvent> events) {
        List<String> out = new ArrayList<>();
        for (GameEvent e : events) {
            out.add(new Json.Obj().put("turn", e.getTurn()).put("text", e.getText()).toString());
        }
        return Json.arr(out);
    }

    // ------------------------------------------------------------------ restaurant

    private static String ids(List<Customer> customers) {
        List<String> out = new ArrayList<>();
        for (Customer c : customers) {
            out.add(Json.str(c.getId()));
        }
        return Json.arr(out);
    }

    private static String tables(GameController g) {
        List<String> out = new ArrayList<>();
        for (Table t : g.getTables()) {
            out.add(new Json.Obj().put("id", t.getId())
                    .raw("occupant", t.isFree() ? "null" : Json.str(t.getOccupant().getId())).toString());
        }
        return Json.arr(out);
    }

    private static String customers(GameSession session) {
        GameController g = session.game();
        List<String> out = new ArrayList<>();
        for (Customer c : g.getCustomers()) {
            out.add(new Json.Obj()
                    .put("id", c.getId())
                    .put("type", c.typeLabel())
                    .put("satisfaction", c.getSatisfaction())
                    .put("recordedSatisfaction", c.getRecordedSatisfaction())
                    .put("state", c.getState().name())
                    .raw("table", c.getTable() == null ? "null" : Json.str(c.getTable().getId()))
                    .raw("orderId", c.getOrder() == null ? "null" : Json.str(c.getOrder().getId()))
                    .put("arrivalTurn", c.getArrivalTurn())
                    .toString());
        }
        return Json.arr(out);
    }

    private static String orders(GameController g) {
        List<String> out = new ArrayList<>();
        for (Order o : g.getOrders()) {
            out.add(new Json.Obj()
                    .put("id", o.getId())
                    .put("customerId", o.getCustomer().getId())
                    .put("item", o.getItem().getName())
                    .raw("components", components(o.getItem()))
                    .put("status", o.getStatus().name())
                    .put("unitsDone", o.getUnitsDone())
                    .put("prepUnits", o.getItem().getPrepUnits())
                    .put("priceCents", o.getLockedPriceCents())
                    .put("price", Money.format(o.getLockedPriceCents()))
                    .put("takenTurn", o.getTakenTurn())
                    .put("readyTurn", o.getReadyTurn())
                    .put("servableNow", o.isServableOn(g.getCurrentTurn()))
                    .toString());
        }
        return Json.arr(out);
    }

    /** Base dishes in an item, for drawing (a combo is drawn as its two dishes). */
    private static String components(MenuItem item) {
        List<String> names = new ArrayList<>();
        if (item instanceof ComboMeal combo) {
            for (MenuItem part : combo.getComponents()) {
                names.add(Json.str(part.getName()));
            }
        } else {
            names.add(Json.str(item.getName()));
        }
        return Json.arr(names);
    }

    private static String nextArrival(GameSession session) {
        List<Customer> upcoming = session.upcomingArrivals();
        if (upcoming.isEmpty()) {
            return "null";
        }
        Customer next = upcoming.get(0);
        return new Json.Obj().put("turn", next.getArrivalTurn()).put("type", next.typeLabel()).toString();
    }

    private static String result(GameResult r) {
        return new Json.Obj()
                .put("victory", r.isVictory())
                .put("paidCount", r.getPaidCount())
                .put("paidMet", r.isPaidCountMet())
                .put("revenueCents", r.getRevenueCents())
                .put("revenue", Money.format(r.getRevenueCents()))
                .put("revenueMet", r.isRevenueMet())
                .put("averageSatisfaction", r.getAverageText())
                .put("satisfactionMet", r.isSatisfactionMet())
                .put("stars", ScoreBoard.stars(r))
                .toString();
    }

    // ------------------------------------------------------------------ decision (MCQ)
    //
    // Each choice is { key, label, enabled, reason, actionType, steps }. A step asks one question, e.g.
    // "Which table should I serve?", and lists every table: the ones the back end allows are enabled, the
    // others are disabled with a reason. The front end walks the steps and sends actionType + the answers.

    private static String decision(GameSession session) {
        GameController g = session.game();
        GameSession.Phase phase = session.phase();
        List<String> choices = new ArrayList<>();
        switch (phase) {
            case HAPPY_HOUR -> {
                choices.add(choice("happyHourYes", "Start Happy Hour", null,
                        "20% off new orders and +15 for waiting guests, this turn and next.", List.of()));
                choices.add(choice("happyHourNo", "Not now", null, "Save it for later (once per game).", List.of()));
            }
            case HOST -> {
                choices.add(seatChoice(g, "hostSeat"));
                choices.add(choice("hostWait", "Wait", null, WAIT_HINT, List.of()));
            }
            case WAITER -> {
                choices.add(seatChoice(g, "waiterSeat"));
                choices.add(takeOrderChoice(g));
                choices.add(serveChoice(g));
                choices.add(choice("waiterWait", "Wait", null, WAIT_HINT, List.of()));
            }
            case CHEF -> {
                choices.add(cookChoice(g));
                choices.add(choice("chefWait", "Wait", null, WAIT_HINT, List.of()));
            }
            default -> {
                return "null";
            }
        }
        return new Json.Obj()
                .put("role", GameSession.roleName(phase))
                .raw("choices", Json.arr(choices))
                .toString();
    }

    private static final String WAIT_HINT = "Do nothing this turn (the task is used up).";

    /** Seat: first who (any queued customer), then which table (free tables enabled). */
    private static String seatChoice(GameController g, String type) {
        String reason = g.getQueue().isEmpty() ? "Nobody is waiting in the queue."
                : g.getFreeTables().isEmpty() ? "No table is free until someone pays or leaves." : null;
        if (reason != null) {
            return choice(type, "Seat a customer", reason, null, List.of());
        }
        List<String> who = new ArrayList<>();
        for (Customer c : g.getQueue()) {
            who.add(option(c.getId(), c.getId() + " (" + c.typeLabel() + ")",
                    "satisfaction " + c.getSatisfaction() + ", arrived turn " + c.getArrivalTurn(), null));
        }
        List<String> where = new ArrayList<>();
        for (Table t : g.getTables()) {
            where.add(option(t.getId(), tableName(t), t.isFree() ? "Free" : null,
                    t.isFree() ? null : "Taken by " + describe(t.getOccupant())));
        }
        String hint = g.getQueue().size() == 1
                ? describe(g.getQueue().get(0)) + " is waiting outside."
                : g.getQueue().size() + " guests are waiting outside.";
        return choice(type, "Seat a customer", null, hint, List.of(
                step("customer", "Who should I seat?", who),
                step("table", "Which table should I seat them at?", where)));
    }

    /** Take order: which table (tables whose customer is seated without an order are enabled). */
    private static String takeOrderChoice(GameController g) {
        List<Customer> ready = g.getCustomersAwaitingOrder();
        if (ready.isEmpty()) {
            return choice("waiterTakeOrder", "Take an order", "No seated customer is waiting to order.", null, List.of());
        }
        List<String> tables = new ArrayList<>();
        for (Table t : g.getTables()) {
            Customer c = t.getOccupant();
            boolean ok = c != null && ready.contains(c);
            tables.add(option(ok ? c.getId() : "", tableName(t), ok ? describe(c) + " is ready to order" : null,
                    ok ? null : tableStatus(g, t)));
        }
        String hint = ready.size() == 1
                ? tableName(ready.get(0).getTable()) + ": " + describe(ready.get(0)) + " is ready to order."
                : ready.size() + " tables are ready to order.";
        return choice("waiterTakeOrder", "Take an order", null, hint,
                List.of(step("customer", "Which table should I take the order from?", tables)));
    }

    /** Serve: which table (tables whose dish was READY before this turn are enabled). */
    private static String serveChoice(GameController g) {
        List<Order> servable = g.getServableOrders();
        if (servable.isEmpty()) {
            Optional<Order> justReady = g.getOrders().stream()
                    .filter(o -> o.getStatus() == OrderStatus.READY && o.getReadyTurn() == g.getCurrentTurn())
                    .findFirst();
            String reason = justReady.map(o -> o.getCustomer().getId() + "'s " + o.getItem().getName()
                    + " was finished this turn: serve it next turn.").orElse("No dish is ready to serve.");
            return choice("waiterServe", "Serve an order", reason, null, List.of());
        }
        List<String> tables = new ArrayList<>();
        for (Table t : g.getTables()) {
            Customer c = t.getOccupant();
            Order order = c == null ? null : c.getOrder();
            boolean ok = order != null && servable.contains(order);
            tables.add(option(ok ? order.getId() : "", tableName(t),
                    ok ? order.getItem().getName() + " for " + describe(c) : null, ok ? null : tableStatus(g, t)));
        }
        Order first = servable.get(0);
        String hint = servable.size() == 1
                ? tableName(first.getCustomer().getTable()) + ": " + first.getItem().getName() + " is ready."
                : servable.size() + " dishes are ready.";
        return choice("waiterServe", "Serve an order", null, hint,
                List.of(step("order", "Which table should I serve?", tables)));
    }

    /** Cook: which order (every order still in the kitchen), labelled by table. */
    private static String cookChoice(GameController g) {
        List<Order> preparable = g.getPreparableOrders();
        if (preparable.isEmpty()) {
            return choice("chefCook", "Cook an order", "No tickets on the rail.", null, List.of());
        }
        List<String> orders = new ArrayList<>();
        for (Order o : preparable) {
            Table t = o.getCustomer().getTable();
            orders.add(option(o.getId(), (t == null ? "" : tableName(t) + ": ") + o.getItem().getName(),
                    "for " + describe(o.getCustomer()) + ", " + o.progress() + " units done", null));
        }
        String hint = preparable.size() == 1 ? "1 ticket on the rail." : preparable.size() + " tickets on the rail.";
        return choice("chefCook", "Cook an order", null, hint,
                List.of(step("order", "Which order should I cook?", orders)));
    }

    /** Why a table cannot be picked right now (plain description of its state). */
    private static String tableStatus(GameController g, Table t) {
        Customer c = t.getOccupant();
        if (c == null) {
            return "Empty table";
        }
        Order order = c.getOrder();
        return switch (c.getState()) {
            case SEATED -> describe(c) + " hasn't ordered yet";
            case ORDERED -> order != null && order.getStatus() == OrderStatus.READY
                    ? order.getItem().getName() + " was finished this turn: serve it next turn"
                    : describe(c) + " is waiting for " + (order == null ? "food"
                            : order.getItem().getName() + " (" + order.progress() + " cooked)");
            case EATING -> describe(c) + " is eating";
            case READY_TO_PAY -> describe(c) + " is waiting to pay";
            default -> describe(c);
        };
    }

    private static String describe(Customer c) {
        return c.getId() + " (" + c.typeLabel() + ")";
    }

    private static String choice(String type, String label, String disabledReason, String hint, List<String> steps) {
        return new Json.Obj()
                .put("key", type)
                .put("label", label)
                .put("enabled", disabledReason == null)
                .raw("reason", disabledReason == null ? "null" : Json.str(disabledReason))
                .raw("hint", hint == null ? "null" : Json.str(hint))
                .put("actionType", type)
                .raw("steps", Json.arr(steps))
                .toString();
    }

    private static String step(String param, String prompt, List<String> options) {
        return new Json.Obj().put("param", param).put("prompt", prompt).raw("options", Json.arr(options)).toString();
    }

    private static String option(String value, String label, String detail, String disabledReason) {
        return new Json.Obj()
                .put("value", value)
                .put("label", label)
                .raw("detail", detail == null ? "null" : Json.str(detail))
                .put("enabled", disabledReason == null)
                .raw("reason", disabledReason == null ? "null" : Json.str(disabledReason))
                .toString();
    }

    private static String tableName(Table t) {
        return t == null ? "?" : "Table " + t.getId().substring(1);
    }
}
