/**
 * Prints pipeline status: latest outputs, configured keys, and a suggested next step.
 *
 * @author Andrew Peirce
 */
package main;

import api.ApiConfig;
import models.Apartment;
import utils.JsonUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class StatusCommand {

    private final OutputPaths paths;
    private final PrintStream out;

    public StatusCommand(OutputPaths paths) {
        this(paths, System.out);
    }

    public StatusCommand(OutputPaths paths, PrintStream out) {
        this.paths = paths;
        this.out = out;
    }

    public void run() {
        out.println("Output directory: " + paths.getRoot());
        out.println();
        out.println("Latest files:");
        StageSummary raw = summarize("Raw", paths.rawDir(), "listings_scraped", "json");
        StageSummary cleaned = summarize("Cleaned", paths.cleanedDir(), "listings_cleaned", "json");
        StageSummary geocoded = summarize("Geocoded", paths.geocodedDir(), "listings_geocoded", "json");
        StageSummary finalStage = summarize("Final", paths.finalDir(), "listings_final", "json");
        summarize("API", paths.apiDir(), null, "json");

        out.println();
        out.println("API / geocoder config:");
        out.println("  Census geocoder : available (no key needed)");
        printKey("Geocodio (optional)", ApiConfig.load("Geocodio").has("api.key"));
        printKey("TravelTime", ApiConfig.load("TravelTime").hasAll("api.key", "api.id"));
        printKey("WalkScore", ApiConfig.load("WalkScore").has("api.key"));
        printKey("CrimeData", ApiConfig.load("CrimeData").hasAll("api.key", "api.ori"));

        out.println();
        out.println(suggestNext(raw, cleaned, geocoded, finalStage));
    }

    private StageSummary summarize(String label, Path dir, String prefix, String extension) {
        if (!Files.isDirectory(dir)) {
            out.printf("  %-8s (missing directory)%n", label);
            return StageSummary.missing(label);
        }
        Optional<Path> latest = findLatest(dir, prefix, extension);
        if (latest.isEmpty()) {
            out.printf("  %-8s (none)%n", label);
            return StageSummary.missing(label);
        }
        Path file = latest.get();
        String age = formatAge(file);
        Integer count = countListings(file);
        String countText = count == null ? "?" : String.valueOf(count);
        out.printf("  %-8s %s  (%s listings, %s)%n", label, file.getFileName(), countText, age);
        return new StageSummary(label, file, count);
    }

    private Optional<Path> findLatest(Path dir, String prefix, String extension) {
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(f -> {
                        String name = f.getFileName().toString();
                        if (prefix != null && !name.startsWith(prefix)) {
                            return false;
                        }
                        return extension == null || name.endsWith("." + extension);
                    })
                    .max(Comparator.comparingLong(f -> f.toFile().lastModified()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Integer countListings(Path file) {
        String name = file.getFileName().toString();
        if (!name.endsWith(".json") || name.startsWith("crime_report")) {
            return null;
        }
        try {
            List<Apartment> list = JsonUtils.fromJson(file.toString());
            return list.size();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String formatAge(Path file) {
        try {
            Instant modified = Files.getLastModifiedTime(file).toInstant();
            Duration age = Duration.between(modified, Instant.now());
            if (age.toDays() >= 1) {
                return age.toDays() + "d ago";
            }
            if (age.toHours() >= 1) {
                return age.toHours() + "h ago";
            }
            if (age.toMinutes() >= 1) {
                return age.toMinutes() + "m ago";
            }
            return "just now";
        } catch (IOException e) {
            return "unknown age";
        }
    }

    private void printKey(String name, boolean present) {
        out.printf("  %-18s %s%n", name, present ? "configured" : "missing");
    }

    private String suggestNext(
            StageSummary raw, StageSummary cleaned, StageSummary geocoded, StageSummary finalStage) {
        if (finalStage.present() && geocoded.present()) {
            return "Latest stage: final. You're up to date. Re-run with `srs scrape-clean` or `srs full` for a fresh scrape.";
        }
        if (geocoded.present()) {
            return "Latest stage: geocoded. Next: `srs apis`";
        }
        if (cleaned.present()) {
            return "Latest stage: cleaned. Next: `srs resume`  (or `srs geocode`)";
        }
        if (raw.present()) {
            return "Latest stage: raw. Next: `srs clean`";
        }
        return "No outputs yet. Next: `srs scrape-clean --max-id=25`";
    }

    private record StageSummary(String label, Path file, Integer count) {
        static StageSummary missing(String label) {
            return new StageSummary(label, null, null);
        }

        boolean present() {
            return file != null;
        }
    }
}
