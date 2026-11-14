/**
 * Parses CLI arguments and loads defaults from {@code pipeline.properties}.
 *
 * @author Andrew Peirce
 */
package main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public final class PipelineOptions {

    public enum Mode {
        FULL_PIPELINE("full"),
        SCRAPE_AND_CLEAN_ONLY("scrape-clean"),
        CLEAN_ONLY("clean"),
        GEOCODE_ONLY("geocode"),
        APIS_ONLY("apis"),
        RESUME_FROM_CLEANED("resume"),
        STATUS("status"),
        HELP("help");

        private final String cliName;

        Mode(String cliName) {
            this.cliName = cliName;
        }

        public String getCliName() {
            return cliName;
        }

        public boolean isPipelineMode() {
            return this != STATUS && this != HELP;
        }

        public static Mode fromCli(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (Mode mode : values()) {
                if (mode.cliName.equals(normalized) || mode.name().equalsIgnoreCase(normalized)) {
                    return mode;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown command: " + value
                            + ". Expected one of: full, scrape-clean, clean, geocode, apis, resume, status, help");
        }
    }

    public enum GeocoderChoice {
        AUTO("auto"),
        CENSUS("census"),
        GEOCODIO("geocodio"),
        CSV("csv");

        private final String cliName;

        GeocoderChoice(String cliName) {
            this.cliName = cliName;
        }

        public String getCliName() {
            return cliName;
        }

        public static GeocoderChoice fromCli(String value) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            for (GeocoderChoice choice : values()) {
                if (choice.cliName.equals(normalized)) {
                    return choice;
                }
            }
            throw new IllegalArgumentException(
                    "Unknown geocoder: " + value + ". Expected one of: auto, census, geocodio, csv");
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
    private final GeocoderChoice geocoder;
    private final boolean helpRequested;
    private final boolean noArgs;
    private final String helpTopic;

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
            GeocoderChoice geocoder,
            boolean helpRequested,
            boolean noArgs,
            String helpTopic) {
        this.mode = mode;
        this.outputDir = outputDir;
        this.minId = minId;
        this.maxId = maxId;
        this.delayMs = delayMs;
        this.campusLat = campusLat;
        this.campusLng = campusLng;
        this.travelTimeMaxSeconds = travelTimeMaxSeconds;
        this.travelTimeArrivalHour = travelTimeArrivalHour;
        this.geocoder = geocoder;
        this.helpRequested = helpRequested;
        this.noArgs = noArgs;
        this.helpTopic = helpTopic;
    }

    public static PipelineOptions parse(String[] args) {
        if (args == null || args.length == 0) {
            return defaultsWithNoArgs();
        }

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
        GeocoderChoice geocoder = GeocoderChoice.AUTO;
        boolean helpRequested = false;
        String helpTopic = null;
        boolean modeSet = false;

        List<String> positionals = new ArrayList<>();
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                helpRequested = true;
                continue;
            }
            if (!arg.startsWith("--")) {
                positionals.add(arg);
                continue;
            }

            String[] parts = arg.substring(2).split("=", 2);
            String key = parts[0];
            String value = parts.length > 1 ? parts[1] : "";
            if (value.isBlank() && !"help".equals(key)) {
                throw new IllegalArgumentException("Missing value for --" + key);
            }

            switch (key) {
                case "mode" -> {
                    mode = Mode.fromCli(value);
                    modeSet = true;
                }
                case "output-dir" -> outputDir = Path.of(value);
                case "min-id" -> minId = Integer.parseInt(value);
                case "max-id" -> maxId = Integer.parseInt(value);
                case "delay-ms" -> delayMs = Integer.parseInt(value);
                case "campus-lat" -> campusLat = Double.parseDouble(value);
                case "campus-lng" -> campusLng = Double.parseDouble(value);
                case "geocoder" -> geocoder = GeocoderChoice.fromCli(value);
                case "help" -> helpRequested = true;
                default -> throw new IllegalArgumentException("Unknown option: --" + key);
            }
        }

        if (!positionals.isEmpty()) {
            mode = Mode.fromCli(positionals.get(0));
            modeSet = true;
            if (mode == Mode.HELP) {
                helpRequested = true;
                if (positionals.size() > 1) {
                    helpTopic = positionals.get(1);
                }
            } else if (positionals.size() > 1) {
                throw new IllegalArgumentException("Unexpected argument: " + positionals.get(1));
            }
        }

        if (!modeSet && !helpRequested) {
            // Flags only, no command — treat like scrape-clean for backward compatibility with --mode.
            mode = Mode.SCRAPE_AND_CLEAN_ONLY;
        }

        if (helpRequested && mode != Mode.HELP) {
            mode = Mode.HELP;
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
                geocoder,
                helpRequested,
                false,
                helpTopic);
    }

    private static PipelineOptions defaultsWithNoArgs() {
        Properties defaults = loadDefaults();
        return new PipelineOptions(
                Mode.HELP,
                Path.of(defaults.getProperty("output.dir", "output")).toAbsolutePath().normalize(),
                Integer.parseInt(defaults.getProperty("scraper.minId", "0")),
                Integer.parseInt(defaults.getProperty("scraper.maxId", "900")),
                Integer.parseInt(defaults.getProperty("scraper.delayMs", "1000")),
                Double.parseDouble(defaults.getProperty("campus.lat", "43.81610373619651")),
                Double.parseDouble(defaults.getProperty("campus.lng", "-91.2311481387267")),
                Integer.parseInt(defaults.getProperty("traveltime.maxSeconds", "900")),
                Integer.parseInt(defaults.getProperty("traveltime.arrivalHour", "8")),
                GeocoderChoice.AUTO,
                false,
                true,
                null);
    }

    /**
     * Rebuild options with a selected command and optional ID overrides (used by the menu).
     */
    public PipelineOptions withCommand(Mode mode, Integer minIdOverride, Integer maxIdOverride) {
        return new PipelineOptions(
                mode,
                outputDir,
                minIdOverride != null ? minIdOverride : minId,
                maxIdOverride != null ? maxIdOverride : maxId,
                delayMs,
                campusLat,
                campusLng,
                travelTimeMaxSeconds,
                travelTimeArrivalHour,
                geocoder,
                mode == Mode.HELP,
                false,
                null);
    }

    public static String helpText() {
        return CliHelp.overview();
    }

    static Properties loadDefaults() {
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

    public GeocoderChoice getGeocoder() {
        return geocoder;
    }

    public boolean isHelpRequested() {
        return helpRequested || mode == Mode.HELP;
    }

    public boolean isNoArgs() {
        return noArgs;
    }

    public String getHelpTopic() {
        return helpTopic;
    }
}
