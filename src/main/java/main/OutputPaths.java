/**
 * Resolves timestamped output paths under the pipeline output root.
 *
 * @author Andrew Peirce
 */
package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.stream.Stream;

public final class OutputPaths {

    public static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("MM_dd_yyyy_HHmm");

    private final Path root;
    private final String timestamp;

    public OutputPaths(Path root) {
        this(root, LocalDateTime.now().format(TIMESTAMP_FORMAT));
    }

    public OutputPaths(Path root, String timestamp) {
        this.root = root;
        this.timestamp = timestamp;
    }

    public Path getRoot() {
        return root;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Path rawDir() {
        return root.resolve("Raw");
    }

    public Path cleanedDir() {
        return root.resolve("Cleaned");
    }

    public Path geocodedDir() {
        return root.resolve("Geocoded");
    }

    public Path apiDir() {
        return root.resolve("API");
    }

    public Path cacheDir() {
        return root.resolve("Cache");
    }

    public Path finalDir() {
        return root.resolve("Final");
    }

    public Path htmlDir() {
        return root.resolve("Html").resolve("scrape_" + timestamp);
    }

    public Path auditDir() {
        return root.resolve("Audit");
    }

    public Path bedBathAuditCsv() {
        return auditDir().resolve(uniqueFilename("bedbath_audit", "csv"));
    }

    public Path scrapedJson() {
        return rawDir().resolve(uniqueFilename("listings_scraped", "json"));
    }

    public Path cleanedJson() {
        return cleanedDir().resolve(uniqueFilename("listings_cleaned", "json"));
    }

    public Path cleanedCsv() {
        return cleanedDir().resolve(uniqueFilename("listings_cleaned", "csv"));
    }

    public Path geocodedJson() {
        return geocodedDir().resolve(uniqueFilename("listings_geocoded", "json"));
    }

    public Path geocodioCsv() {
        return geocodedDir().resolve("geocodio.csv");
    }

    public Path afterTravelJson() {
        return apiDir().resolve(uniqueFilename("listings_after_travel", "json"));
    }

    public Path afterWalkJson() {
        return apiDir().resolve(uniqueFilename("listings_after_walk", "json"));
    }

    public Path crimeReportJson() {
        return apiDir().resolve(uniqueFilename("crime_report", "json"));
    }

    public Path finalJson() {
        return finalDir().resolve(uniqueFilename("listings_final", "json"));
    }

    public Path finalCsv() {
        return finalDir().resolve(uniqueFilename("listings_final", "csv"));
    }

    public String uniqueFilename(String baseName, String extension) {
        return baseName + "_" + timestamp + "." + extension;
    }

    public Path findLatest(String prefix, String extension, Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            throw new IOException("Directory not found: " + dir);
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(f -> {
                        String name = f.getFileName().toString();
                        return name.startsWith(prefix) && name.endsWith("." + extension);
                    })
                    .max(Comparator.comparingLong(f -> f.toFile().lastModified()))
                    .orElseThrow(() -> new IOException(
                            "No matching file found for " + prefix + "*." + extension + " in " + dir));
        }
    }

    public void ensureDirectories() throws IOException {
        Files.createDirectories(rawDir());
        Files.createDirectories(cleanedDir());
        Files.createDirectories(geocodedDir());
        Files.createDirectories(apiDir());
        Files.createDirectories(cacheDir());
        Files.createDirectories(finalDir());
    }
}
