/**
 * Loads a two-column lookup table (key -> value) from a CSV file with a header row.
 * Blank lines and lines starting with '#' are ignored. Unlike {@link BedBathOverrides},
 * a missing file is an error because the cleaner depends on these tables.
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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KeyValueCsv {

    private KeyValueCsv() {
    }

    /**
     * @return entries keyed by {@code keyColumn}, in file order
     * @throws NoSuchFileException if the file does not exist
     * @throws IllegalArgumentException with the file and line number for any invalid row
     */
    public static Map<String, String> load(Path file, String keyColumn, String valueColumn) throws IOException {
        if (file == null || !Files.isRegularFile(file)) {
            throw new NoSuchFileException(String.valueOf(file), null, "required lookup file is missing");
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        return parse(text, file.toString(), keyColumn, valueColumn);
    }

    /**
     * Parses a lookup table from CSV text.
     *
     * @param csvText CSV content with a header row
     * @param source  name used in error messages
     */
    public static Map<String, String> parse(String csvText, String source, String keyColumn, String valueColumn) {
        Map<String, String> entries = new LinkedHashMap<>();
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
            return Collections.unmodifiableMap(entries);
        }

        try (CSVReader reader = new CSVReader(new StringReader(String.join("\n", lines)))) {
            String[] header = reader.readNext();
            int headerLine = lineNumbers.get(0);
            Map<String, Integer> columns = indexColumns(header);
            Integer keyIndex = columns.get(keyColumn.toLowerCase(Locale.ROOT));
            Integer valueIndex = columns.get(valueColumn.toLowerCase(Locale.ROOT));
            if (keyIndex == null || valueIndex == null) {
                throw new IllegalArgumentException(
                        location(source, headerLine) + ": header must include " + keyColumn + "," + valueColumn);
            }

            String[] record;
            int recordIndex = 1;
            while ((record = reader.readNext()) != null) {
                int line = lineNumbers.get(recordIndex);
                recordIndex++;
                String key = normalizeKey(cell(record, keyIndex));
                String value = cell(record, valueIndex);
                if (key.isEmpty()) {
                    throw new IllegalArgumentException(location(source, line) + ": " + keyColumn + " is required");
                }
                if (value.isEmpty()) {
                    throw new IllegalArgumentException(location(source, line) + ": " + valueColumn + " is required");
                }
                if (entries.putIfAbsent(key, value) != null) {
                    throw new IllegalArgumentException(
                            location(source, line) + ": duplicate " + keyColumn + " '" + key + "'");
                }
            }
        } catch (IOException | CsvValidationException e) {
            throw new IllegalArgumentException(source + ": could not read CSV: " + e.getMessage(), e);
        }
        return Collections.unmodifiableMap(entries);
    }

    /**
     * Trims and collapses internal whitespace, matching how the cleaner prepares lookup input.
     */
    static String normalizeKey(String text) {
        return text.strip().replaceAll("\\s+", " ");
    }

    private static Map<String, Integer> indexColumns(String[] header) {
        Map<String, Integer> columns = new HashMap<>();
        if (header == null) {
            return columns;
        }
        for (int i = 0; i < header.length; i++) {
            columns.put(header[i].strip().toLowerCase(Locale.ROOT), i);
        }
        return columns;
    }

    private static String cell(String[] record, int index) {
        if (index >= record.length) {
            return "";
        }
        return record[index].strip();
    }

    private static String location(String source, int line) {
        return source + " line " + line;
    }
}
