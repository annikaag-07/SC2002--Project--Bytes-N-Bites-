package rush.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import rush.control.GameResult;

/** High scores kept in a small tab-separated file: victories first, then revenue, then average satisfaction. */
final class ScoreBoard {

    private static final int MAX_ENTRIES = 10;

    /** One finished game. The seed is kept in the file for reproducibility but not shown to players. */
    record Entry(String date, int day, String mode, boolean victory, int stars, int paid, long revenueCents,
                 String average, String seed) {
        String toJson() {
            return new Json.Obj().put("date", date).put("day", day).put("mode", mode)
                    .put("modeLabel", ModeFeatures.labelFor(mode)).put("victory", victory)
                    .put("stars", stars)
                    .put("paid", paid).put("revenueCents", revenueCents).put("average", average).toString();
        }
    }

    private final Path file;

    ScoreBoard(Path file) {
        this.file = file;
    }

    synchronized List<Entry> load() {
        List<Entry> entries = new ArrayList<>();
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String[] f = line.split("\t");
                    if (f.length == 9) {
                        entries.add(new Entry(f[0], Integer.parseInt(f[1]), f[2], Boolean.parseBoolean(f[3]),
                                Integer.parseInt(f[4]), Integer.parseInt(f[5]), Long.parseLong(f[6]), f[7], f[8]));
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Could not read high scores: " + e.getMessage());
        }
        return entries;
    }

    synchronized void record(long seed, int day, String mode, GameResult result) {
        List<Entry> entries = load();
        entries.add(new Entry(LocalDate.now().toString(), day, mode, result.isVictory(), stars(result),
                result.getPaidCount(), result.getRevenueCents(), result.getAverageText(), Long.toString(seed)));
        entries.sort(Comparator.comparing(Entry::victory).reversed()
                .thenComparing(Comparator.comparingInt(Entry::stars).reversed())
                .thenComparing(Comparator.comparingLong(Entry::revenueCents).reversed())
                .thenComparing(e -> -averageValue(e.average())));
        List<String> lines = new ArrayList<>();
        for (Entry e : entries.subList(0, Math.min(MAX_ENTRIES, entries.size()))) {
            lines.add(String.join("\t", e.date(), Integer.toString(e.day()), e.mode(), Boolean.toString(e.victory()),
                    Integer.toString(e.stars()), Integer.toString(e.paid()), Long.toString(e.revenueCents()),
                    e.average(), e.seed()));
        }
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save high scores: " + e.getMessage());
        }
    }

    /** One star per victory target met (3 stars = victory). */
    static int stars(GameResult result) {
        return (result.isPaidCountMet() ? 1 : 0) + (result.isRevenueMet() ? 1 : 0) + (result.isSatisfactionMet() ? 1 : 0);
    }

    private static double averageValue(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
