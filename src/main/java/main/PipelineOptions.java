/**
 * Parses CLI arguments and loads defaults from {@code pipeline.properties}.
 *
 * @author Andrew Peirce
 */
package main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

public final class PipelineOptions {

    public enum Mode {
        FULL_PIPELINE("full"),
        SCRAPE_AND_CLEAN_ONLY("scrape-clean"),
        CLEAN_ONLY("clean"),
        GEOCODE_ONLY("geocode"),
        APIS_ONLY("apis"),
        RESUME_FROM_CLEANED("resume");

        private final String cliName;

        Mode(String cliName) {
            this.cliName = cliName;
        }

        public String getCliName() {
            return cliName;
        }

        public static Mode fromCli(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (Mode mode : values()) {
                if (mode.cliName.equals(normalized) || mode.name().equalsIgnoreCase(normalized)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown mode: " + value + ". Expected one of: full, scrape-clean, clean, geocode, apis, resume");
        }
    }

    private final Mode mode;
    private final Path outputDir;
    private final int minId;
    private final int maxId;
    private final int delayMs;
    private final double campusLat;
    private final double campusLng;
    private final int travelTimeMaxSeconds;
    private final int travelTimeArrivalHour;
    private final boolean helpRequested;

    private PipelineOptions(
            Mode mode,
            Path outputDir,
            int minId,
            int maxId,
            int delayMs,
            double campusLat,
            double campusLng,
            int travelTimeMaxSeconds,
            int travelTimeArrivalHour,
            boolean helpRequested) {
        this.mode = mode;
        this.outputDir = outputDir;
        this.minId = minId;
        this.maxId = maxId;
        this.delayMs = delayMs;
        this.campusLat = campusLat;
        this.campusLng = campusLng;
        this.travelTimeMaxSeconds = travelTimeMaxSeconds;
        this.travelTimeArrivalHour = travelTimeArrivalHour;
        this.helpRequested = helpRequested;
    }

    public static PipelineOptions parse(String[] args) {
        Properties defaults = loadDefaults();

        Mode mode = Mode.SCRAPE_AND_CLEAN_ONLY;
        Path outputDir = Path.of(defaults.getProperty("output.dir", "output"));
        int minId = Integer.parseInt(defaults.getProperty("scraper.minId", "0"));
        int maxId = Integer.parseInt(defaults.getProperty("scraper.maxId", "900"));
        int delayMs = Integer.parseInt(defaults.getProperty("scraper.delayMs", "1000"));
        double campusLat = Double.parseDouble(defaults.getProperty("campus.lat", "43.81610373619651"));
        double campusLng = Double.parseDouble(defaults.getProperty("campus.lng", "-91.2311481387267"));
        int travelTimeMaxSeconds = Integer.parseInt(defaults.getProperty("traveltime.maxSeconds", "900"));
        int travelTimeArrivalHour = Integer.parseInt(defaults.getProperty("traveltime.arrivalHour", "8"));
        boolean helpRequested = false;

        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                helpRequested = true;
                continue;
            }
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Unexpected argument: " + arg);
            }

            String[] parts = arg.substring(2).split("=", 2);
            String key = parts[0];
            String value = parts.length > 1 ? parts[1] : "";
            if (value.isBlank() && !"help".equals(key)) {
                throw new IllegalArgumentException("Missing value for --" + key);
            }

            switch (key) {
                case "mode" -> mode = Mode.fromCli(value);
                case "output-dir" -> outputDir = Path.of(value);
                case "min-id" -> minId = Integer.parseInt(value);
                case "max-id" -> maxId = Integer.parseInt(value);
                case "delay-ms" -> delayMs = Integer.parseInt(value);
                case "campus-lat" -> campusLat = Double.parseDouble(value);
                case "campus-lng" -> campusLng = Double.parseDouble(value);
                case "help" -> helpRequested = true;
                default -> throw new IllegalArgumentException("Unknown option: --" + key);
            }
        }

        if (minId < 0 || maxId < minId) {
            throw new IllegalArgumentException("Invalid ID range: minId=" + minId + ", maxId=" + maxId);
        }
        if (delayMs < 0) {
            throw new IllegalArgumentException("delay-ms must be >= 0");
        }

        return new PipelineOptions(
                mode,
                outputDir.toAbsolutePath().normalize(),
                minId,
                maxId,
                delayMs,
                campusLat,
                campusLng,
                travelTimeMaxSeconds,
                travelTimeArrivalHour,
                helpRequested);
    }

    public static String helpText() {
        return """
                StudentRentalsScraper pipeline

                Usage: mvn exec:java -Dexec.args="[options]"

                Options:
                  --mode=<mode>       full | scrape-clean | clean | geocode | apis | resume
                                      (default: scrape-clean)
                  --output-dir=<path> Output root directory (default: output)
                  --min-id=<n>        First listing ID to scrape (default: 0)
                  --max-id=<n>        Last listing ID to scrape (default: 900)
                  --delay-ms=<n>      Delay between scrape requests in ms (default: 1000)
                  --help              Show this help
                """;
    }

    private static Properties loadDefaults() {
        Properties props = new Properties();
        try (InputStream in = PipelineOptions.class.getClassLoader().getResourceAsStream("pipeline.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load pipeline.properties", e);
        }
        return props;
    }

    public Mode getMode() {
        return mode;
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public int getMinId() {
        return minId;
    }

    public int getMaxId() {
        return maxId;
    }

    public int getDelayMs() {
        return delayMs;
    }

    public double getCampusLat() {
        return campusLat;
    }

    public double getCampusLng() {
        return campusLng;
    }

    public int getTravelTimeMaxSeconds() {
        return travelTimeMaxSeconds;
    }

    public int getTravelTimeArrivalHour() {
        return travelTimeArrivalHour;
    }

    public boolean isHelpRequested() {
        return helpRequested;
    }
}
