package rush.boundary;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Reads the manager's input one line at a time. When input is piped from a script it echoes every line so
 * saved logs read like an interactive session, skips {@code #} comment lines, and after end of input answers
 * every prompt with its default (Wait / No) instead of failing.
 */
final class InputReader {

    private final BufferedReader reader;
    private final PrintStream out;
    private final boolean echo;
    private boolean ended;

    InputReader(InputStream in, PrintStream out) {
        this.reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
        this.echo = !isInteractiveTerminal();
    }

    /**
     * Prints a prompt and returns the next non-comment line, trimmed.
     *
     * @param prompt       text shown before the input
     * @param defaultOnEnd answer used once input has ended
     * @return the line, or {@code defaultOnEnd} after end of input
     */
    String readLine(String prompt, String defaultOnEnd) {
        while (true) {
            out.print(prompt);
            if (ended) {
                out.println(defaultOnEnd);
                return defaultOnEnd;
            }
            String line = nextRawLine();
            if (line == null) {
                ended = true;
                out.println();
                out.println("[input ended - defaulting to Wait]");
                continue;
            }
            String trimmed = line.trim();
            if (echo) {
                out.println(trimmed);
            }
            if (trimmed.startsWith("#")) {
                continue;
            }
            return trimmed;
        }
    }

    private String nextRawLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * True when stdin and stdout are a terminal. On JDK 22+ {@link System#console()} may be non-null even when
     * redirected, so {@code Console.isTerminal()} is consulted reflectively when it exists (we compile for 17).
     */
    private static boolean isInteractiveTerminal() {
        Console console = System.console();
        if (console == null) {
            return false;
        }
        try {
            Object terminal = Console.class.getMethod("isTerminal").invoke(console);
            return Boolean.TRUE.equals(terminal);
        } catch (ReflectiveOperationException e) {
            return true;
        }
    }
}
