/**
 * Loads hand-verified bed/bath corrections from a CSV file. Blank lines and lines
 * starting with '#' are ignored. A missing file means there are no overrides.
 *
 * @author Andrew Peirce
 */
package clean;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BedBathOverrides {

    /** Default location, relative to the working directory. */
    public static final Path DEFAULT_FILE = Path.of("data", "bedbath_overrides.csv");

    private static final String[] REQUIRED_COLUMNS = {"id", "expectedField", "beds", "baths"};

    /**
     * One hand-verified correction.
     *
     * @param id            listing id as it appears in the site URL
     * @param expectedField raw site Bed/Bath field the override was verified against
     * @param beds          bedroom count; 0 means studio
     * @param baths         bathroom count
     * @param note          why the override exists
     */
    public record Override(String id, String expectedField, double beds, double baths, String note) {

        /**
         * @return Bed/Bath text in the form the cleaner reads, such as "5/1" or "Studio/1"
         */
        public String formatted() {
            String bedText = beds == 0.0 ? "Studio" : formatNumber(beds);
            return bedText + "/" + formatNumber(baths);
        }
    }

    private BedBathOverrides() {
    }

    /**
     * @return overrides keyed by listing id; empty if the file does not exist
     */
    public static Map<String, Override> load(Path file) throws IOException {
        if (file == null || !Files.exists(file)) {
            return Map.of();
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        return parse(text, file.toString());
    }

    /**
     * Parses overrides from CSV text.
     *
     * @param csvText CSV content with a header row
     * @param source  name used in error messages
     * @throws IllegalArgumentException with the source and line number for any invalid row
     */
    public static Map<String, Override> parse(String csvText, String source) {
        Map<String, Override> overrides = new HashMap<>();
        List<String> lines = new ArrayList<>();
        List<Integer> lineNumbers = new ArrayList<>();
        String[] physical = csvText.replace("\uFEFF", "").split("\\R", -1);
        for (int i = 0; i < physical.length; i++) {
            String trimmed = physical[i].strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            lines.add(physical[i]);
            lineNumbers.add(i + 1);
        }
        if (lines.isEmpty()) {
            return overrides;
        }

        try (CSVReader reader = new CSVReader(new StringReader(String.join("\n", lines)))) {
            String[] header = reader.readNext();
            Map<String, Integer> columns = indexColumns(header, source, lineNumbers.get(0));

            String[] record;
            int recordIndex = 1;
            while ((record = reader.readNext()) != null) {
                int line = lineNumbers.get(recordIndex);
                recordIndex++;
                Override override = toOverride(record, columns, source, line);
                if (overrides.putIfAbsent(override.id(), override) != null) {
                    throw new IllegalArgumentException(
                            location(source, line) + ": duplicate id " + override.id());
                }
            }
        } catch (IOException | CsvValidationException e) {
            throw new IllegalArgumentException(source + ": could not read CSV: " + e.getMessage(), e);
        }
        return overrides;
    }

    private static Map<String, Integer> indexColumns(String[] header, String source, int line) {
        if (header == null) {
            throw new IllegalArgumentException(location(source, line) + ": missing header row");
        }
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            columns.put(header[i].strip().toLowerCase(Locale.ROOT), i);
        }
        for (String required : REQUIRED_COLUMNS) {
            if (!columns.containsKey(required.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException(
                        location(source, line) + ": header must include " + String.join(",", REQUIRED_COLUMNS));
            }
        }
        return columns;
    }

    private static Override toOverride(String[] record, Map<String, Integer> columns, String source, int line) {
        String id = cell(record, columns, "id");
        if (id.isEmpty()) {
            throw new IllegalArgumentException(location(source, line) + ": id is required");
        }
        String expectedField = cell(record, columns, "expectedField");
        if (expectedField.isEmpty()) {
            throw new IllegalArgumentException(location(source, line) + ": expectedField is required");
        }
        double beds = parseBeds(cell(record, columns, "beds"), source, line);
        double baths = parseNonNegative(cell(record, columns, "baths"), "baths", source, line);
        String note = cell(record, columns, "note");
        return new Override(id, expectedField, beds, baths, note);
    }

    private static String cell(String[] record, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name.toLowerCase(Locale.ROOT));
        if (index == null || index >= record.length) {
            return "";
        }
        return record[index].strip();
    }

    private static double parseBeds(String text, String source, int line) {
        if ("studio".equalsIgnoreCase(text)) {
            return 0.0;
        }
        try {
            return parseNonNegative(text, "beds", source, line);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    location(source, line) + ": beds must be a number or Studio, got '" + text + "'");
        }
    }

    private static double parseNonNegative(String text, String column, String source, int line) {
        double value;
        try {
            value = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    location(source, line) + ": " + column + " must be a number, got '" + text + "'");
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException(
                    location(source, line) + ": " + column + " must be zero or more, got '" + text + "'");
        }
        return value;
    }

    private static String location(String source, int line) {
        return source + " line " + line;
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return Double.toString(value);
    }
}
