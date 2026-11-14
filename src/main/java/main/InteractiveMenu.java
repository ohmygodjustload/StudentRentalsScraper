/**
 * Numbered menu shown when the tool is launched with no arguments.
 *
 * @author Andrew Peirce
 */
package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public final class InteractiveMenu {

    private final BufferedReader in;
    private final PrintStream out;
    private final boolean interactive;

    public InteractiveMenu(Reader in, PrintStream out, boolean interactive) {
        this.in = new BufferedReader(in);
        this.out = out;
        this.interactive = interactive;
    }

    /**
     * @return args to pass to {@link PipelineOptions#parse(String[])}, or {@code null} if the user quit
     */
    public String[] run(PipelineOptions defaults) throws IOException {
        if (!interactive) {
            out.println(CliHelp.overview());
            return new String[] {"help"};
        }

        out.println("StudentRentalsScraper");
        out.println();
        out.println("  1) full           Scrape -> clean -> geocode -> enrich");
        out.println("  2) scrape-clean   Scrape and clean only");
        out.println("  3) clean          Re-clean latest raw JSON");
        out.println("  4) geocode        Geocode latest cleaned JSON");
        out.println("  5) apis           Enrich latest geocoded JSON");
        out.println("  6) resume         Geocode + enrich from cleaned");
        out.println("  7) status         Show latest outputs and keys");
        out.println("  8) help           Show CLI help");
        out.println("  9) quit");
        out.println();
        out.print("Choose [1-9]: ");
        out.flush();

        String choice = readLine();
        if (choice == null) {
            return null;
        }
        choice = choice.strip();

        return switch (choice) {
            case "1", "full" -> scrapeArgs("full", defaults);
            case "2", "scrape-clean" -> scrapeArgs("scrape-clean", defaults);
            case "3", "clean" -> new String[] {"clean"};
            case "4", "geocode" -> new String[] {"geocode"};
            case "5", "apis" -> new String[] {"apis"};
            case "6", "resume" -> new String[] {"resume"};
            case "7", "status" -> new String[] {"status"};
            case "8", "help" -> new String[] {"help"};
            case "9", "q", "quit", "" -> {
                out.println("Bye.");
                yield null;
            }
            default -> {
                out.println("Invalid choice: " + choice);
                yield new String[] {"help"};
            }
        };
    }

    private String[] scrapeArgs(String command, PipelineOptions defaults) throws IOException {
        out.print("Min ID [" + defaults.getMinId() + "]: ");
        out.flush();
        String minRaw = readLine();
        int minId = parseOrDefault(minRaw, defaults.getMinId());

        out.print("Max ID [" + defaults.getMaxId() + "]: ");
        out.flush();
        String maxRaw = readLine();
        int maxId = parseOrDefault(maxRaw, defaults.getMaxId());

        List<String> args = new ArrayList<>();
        args.add(command);
        args.add("--min-id=" + minId);
        args.add("--max-id=" + maxId);

        String equivalent = ".\\srs.ps1 " + command + " --min-id=" + minId + " --max-id=" + maxId;
        out.println();
        out.println("Running: " + equivalent);
        out.println();
        return args.toArray(String[]::new);
    }

    private int parseOrDefault(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Integer.parseInt(raw.strip());
    }

    private String readLine() throws IOException {
        return in.readLine();
    }
}
