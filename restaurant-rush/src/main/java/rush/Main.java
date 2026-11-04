package rush;

import rush.boundary.ConsoleUI;
import rush.control.ArrivalSchedule;
import rush.control.GameConfig;
import rush.control.GameController;

/** Entry point: reads {@code --seed}, wires the base-setting game together and starts the console UI. */
public final class Main {

    private static final long DEFAULT_SEED = 2002L;

    private Main() {
    }

    /**
     * Starts the game.
     *
     * @param args optional {@code --seed <long>}
     */
    public static void main(String[] args) {
        long seed = DEFAULT_SEED;
        for (int i = 0; i < args.length; i++) {
            if ("--seed".equals(args[i]) && i + 1 < args.length) {
                try {
                    seed = Long.parseLong(args[++i]);
                } catch (NumberFormatException e) {
                    usage("seed must be a whole number: " + args[i]);
                    return;
                }
            } else {
                usage("unknown argument: " + args[i]);
                return;
            }
        }
        GameConfig config = GameConfig.base(seed);
        GameController game = new GameController(config, ArrivalSchedule.base(seed));
        new ConsoleUI(game, System.in, System.out).run();
    }

    private static void usage(String problem) {
        System.err.println(problem);
        System.err.println("usage: run.sh [--seed <long>]   (default seed " + DEFAULT_SEED + ")");
        System.exit(2);
    }
}
