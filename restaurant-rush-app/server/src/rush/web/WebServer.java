package rush.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import rush.control.GameEvent;
import rush.entity.ActionResult;

/**
 * Local HTTP server for the web edition. Serves the built front end and a small JSON API over the existing
 * back end. Uses only the JDK (com.sun.net.httpserver); listens on 127.0.0.1 only.
 *
 * <pre>
 * GET  /api/status                         -> { hasSave, hasGame }
 * POST /api/new?seed=&amp;mode=stage1 -> state (unknown mode: 400)
 * POST /api/continue                       -> state (replays the saved game)
 * GET  /api/state                          -> state
 * POST /api/action?type=&amp;customer=&amp;table=&amp;order= -> { result, events, state }
 * POST /api/next                           -> { result, events, state }   (leave end-of-turn summary)
 * GET  /api/highscores                     -> [ entries ]
 * </pre>
 */
public final class WebServer {

    private final Path saveFile;
    private final Path dayFile;
    private final Path webRoot;
    private final ScoreBoard scores;
    private GameSession session;

    private WebServer(Path dataDir, Path webRoot) {
        this.saveFile = dataDir.resolve("save.txt");
        this.dayFile = dataDir.resolve("day.txt");
        this.webRoot = webRoot.toAbsolutePath().normalize();
        this.scores = new ScoreBoard(dataDir.resolve("highscores.tsv"));
    }

    /**
     * Starts the server. Environment: PORT (default 8090), RUSH_DATA (default "data"), RUSH_WEB
     * (default "client/dist").
     *
     * @param args unused
     * @throws IOException if the port cannot be opened
     */
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8090"));
        WebServer app = new WebServer(Paths.get(System.getenv().getOrDefault("RUSH_DATA", "data")),
                Paths.get(System.getenv().getOrDefault("RUSH_WEB", "client/dist")));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/", app::handleApi);
        server.createContext("/", app::handleStatic);
        server.start();
        System.out.println("Bytes N Bites web server: http://localhost:" + port);
    }

    // ------------------------------------------------------------------ API

    private synchronized void handleApi(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            Map<String, String> q = query(ex);
            boolean post = "POST".equals(ex.getRequestMethod());
            switch (path) {
                case "/api/status" -> send(ex, 200, new Json.Obj().put("hasSave", hasUsableSave())
                        .put("hasGame", session != null && !session.isOver()).put("nextDay", peekDay() + 1).toString());
                case "/api/new" -> {
                    requirePost(post);
                    long seed = q.containsKey("seed") && !q.get("seed").isBlank()
                            ? Long.parseLong(q.get("seed").trim())
                            : new Random().nextInt(1_000_000);
                    // An unknown or missing mode is an error (400), never a silent default.
                    ModeFeatures mode = ModeFeatures.fromId(q.getOrDefault("mode", ""));
                    session = GameSession.start(seed, mode, nextDay());
                    save();
                    send(ex, 200, StateJson.of(session));
                }
                case "/api/continue" -> {
                    requirePost(post);
                    GameSession loaded = hasUsableSave() ? load() : null;
                    if (loaded == null) {
                        send(ex, 404, error("There is no saved game."));
                        return;
                    }
                    session = loaded;
                    send(ex, 200, StateJson.of(session));
                }
                case "/api/state" -> {
                    if (session == null) {
                        send(ex, 404, error("No game in progress."));
                        return;
                    }
                    send(ex, 200, StateJson.of(session));
                }
                case "/api/action", "/api/next" -> {
                    requirePost(post);
                    if (session == null) {
                        send(ex, 404, error("No game in progress."));
                        return;
                    }
                    int before = session.game().getEventLog().size();
                    ActionResult result = path.equals("/api/next")
                            ? session.next()
                            : session.perform(q.getOrDefault("type", ""), q.getOrDefault("customer", ""),
                                    q.getOrDefault("table", ""), q.getOrDefault("order", ""));
                    List<GameEvent> log = session.game().getEventLog();
                    if (result.isSuccess()) {
                        afterChange();
                    }
                    send(ex, 200, new Json.Obj()
                            .raw("result", new Json.Obj().put("success", result.isSuccess())
                                    .put("message", result.getMessage()).toString())
                            .raw("events", StateJson.events(log.subList(before, log.size())))
                            .raw("state", StateJson.of(session))
                            .toString());
                }
                case "/api/highscores" -> {
                    List<String> entries = new ArrayList<>();
                    for (ScoreBoard.Entry e : scores.load()) {
                        entries.add(e.toJson());
                    }
                    send(ex, 200, Json.arr(entries));
                }
                default -> send(ex, 404, error("Unknown endpoint " + path));
            }
        } catch (NumberFormatException e) {
            send(ex, 400, error("Seed must be a whole number."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            send(ex, 400, error(e.getMessage()));
        }
    }

    /** Saves after every successful command; on game over records the score and clears the save. */
    private void afterChange() throws IOException {
        if (session.isOver()) {
            scores.record(session.seed(), session.day(), session.mode().id(), session.game().getResult());
            Files.deleteIfExists(saveFile);
        } else {
            save();
        }
    }

    private void save() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(session.seed() + "\t" + session.mode().id() + "\t" + session.day());
        lines.addAll(session.actionLog());
        Files.createDirectories(saveFile.getParent());
        Files.write(saveFile, lines, StandardCharsets.UTF_8);
    }

    /**
     * Replays the save. A save with an unknown mode, or anything unreadable, cannot be
     * replayed: it is deleted and treated as "no save" instead of crashing the server.
     *
     * @return the restored game, or null if there is no usable save
     */
    private GameSession load() throws IOException {
        try {
            List<String> lines = Files.readAllLines(saveFile, StandardCharsets.UTF_8);
            String[] header = lines.get(0).split("\t");
            return GameSession.replay(Long.parseLong(header[0]), ModeFeatures.fromId(header.length > 1 ? header[1] : ""),
                    header.length > 2 ? Integer.parseInt(header[2]) : 1, lines.subList(1, lines.size()));
        } catch (RuntimeException e) {
            System.err.println("Discarding a saved game that cannot be restored: " + e.getMessage());
            Files.deleteIfExists(saveFile);
            return null;
        }
    }

    /** @return true if a save exists and was written for a known mode (other saves are discarded) */
    private boolean hasUsableSave() throws IOException {
        if (!Files.exists(saveFile)) {
            return false;
        }
        List<String> lines = Files.readAllLines(saveFile, StandardCharsets.UTF_8);
        String[] header = lines.isEmpty() ? new String[0] : lines.get(0).split("\t");
        if (header.length > 1 && ModeFeatures.isKnown(header[1])) {
            return true;
        }
        Files.deleteIfExists(saveFile);
        return false;
    }

    /** Each new game is the restaurant's next "day" (a friendlier label than the random seed). */
    private int nextDay() throws IOException {
        int day = peekDay() + 1;
        Files.createDirectories(dayFile.getParent());
        Files.writeString(dayFile, Integer.toString(day), StandardCharsets.UTF_8);
        return day;
    }

    /** @return the last day played (0 if none) */
    private int peekDay() {
        try {
            return Files.exists(dayFile) ? Integer.parseInt(Files.readString(dayFile, StandardCharsets.UTF_8).trim()) : 0;
        } catch (IOException | NumberFormatException e) {
            return 0;
        }
    }

    private static void requirePost(boolean post) {
        if (!post) {
            throw new IllegalArgumentException("Use POST for this endpoint.");
        }
    }

    private static String error(String message) {
        return new Json.Obj().put("error", message).toString();
    }

    private static Map<String, String> query(HttpExchange ex) {
        Map<String, String> params = new HashMap<>();
        String raw = ex.getRequestURI().getRawQuery();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    params.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
                }
            }
        }
        return params;
    }

    // ------------------------------------------------------------------ static files

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        Path file = webRoot.resolve(path.substring(1)).normalize();
        if (!file.startsWith(webRoot) || Files.isDirectory(file) || !Files.exists(file)) {
            file = webRoot.resolve("index.html"); // single-page app fallback
        }
        if (!Files.exists(file)) {
            send(ex, 404, "text/plain", "Front end not built. Run: (cd client && npm install && npm run build)");
            return;
        }
        send(ex, 200, contentType(file), Files.readAllBytes(file));
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (name.endsWith(".wav")) {
            return "audio/wav";
        }
        return "application/octet-stream";
    }

    private static void send(HttpExchange ex, int status, String json) throws IOException {
        send(ex, status, "application/json; charset=utf-8", json);
    }

    private static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        send(ex, status, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }
}
