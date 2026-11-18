package clean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class KeyValueCsvTest {

    @Test
    void parses_rows_with_comments_blank_lines_and_quoted_commas() {
        Map<String, String> map = KeyValueCsv.parse(
                "# comment\n\nalias,canonical\n# inline comment\n"
                        + "\"Off-Campus La Crosse, LLC\",Off-Campus La Crosse LLC\n"
                        + "Favre Retnals,Favre Rentals\n",
                "test", "alias", "canonical");

        assertEquals(2, map.size());
        assertEquals("Off-Campus La Crosse LLC", map.get("Off-Campus La Crosse, LLC"));
        assertEquals("Favre Rentals", map.get("Favre Retnals"));
    }

    @Test
    void collapses_whitespace_in_keys() {
        Map<String, String> map = KeyValueCsv.parse(
                "raw,corrected\n  420  9th St N   La Crosse ,420 9th St N La Crosse WI\n",
                "test", "raw", "corrected");
        assertEquals("420 9th St N La Crosse WI", map.get("420 9th St N La Crosse"));
    }

    @Test
    void header_columns_are_case_insensitive() {
        Map<String, String> map = KeyValueCsv.parse("Alias,Canonical\na,b\n", "test", "alias", "canonical");
        assertEquals("b", map.get("a"));
    }

    @Test
    void empty_or_comment_only_text_yields_empty_map() {
        assertTrue(KeyValueCsv.parse("", "test", "alias", "canonical").isEmpty());
        assertTrue(KeyValueCsv.parse("# only comments\n", "test", "alias", "canonical").isEmpty());
    }

    @Test
    void duplicate_key_error_names_the_line() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> KeyValueCsv.parse("# c\nalias,canonical\na,b\na,c\n", "data/landlord_aliases.csv",
                        "alias", "canonical"));
        assertTrue(e.getMessage().contains("data/landlord_aliases.csv line 4"), e.getMessage());
        assertTrue(e.getMessage().contains("duplicate alias"), e.getMessage());
    }

    @Test
    void empty_key_or_value_is_rejected() {
        IllegalArgumentException emptyKey = assertThrows(IllegalArgumentException.class,
                () -> KeyValueCsv.parse("alias,canonical\n,b\n", "test", "alias", "canonical"));
        assertTrue(emptyKey.getMessage().contains("line 2"), emptyKey.getMessage());

        IllegalArgumentException emptyValue = assertThrows(IllegalArgumentException.class,
                () -> KeyValueCsv.parse("alias,canonical\na,\n", "test", "alias", "canonical"));
        assertTrue(emptyValue.getMessage().contains("canonical is required"), emptyValue.getMessage());
    }

    @Test
    void header_must_include_both_columns() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> KeyValueCsv.parse("alias,name\na,b\n", "test", "alias", "canonical"));
        assertTrue(e.getMessage().contains("header must include alias,canonical"), e.getMessage());
    }

    @Test
    void missing_file_is_an_error(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("nope.csv");
        NoSuchFileException e = assertThrows(NoSuchFileException.class,
                () -> KeyValueCsv.load(missing, "alias", "canonical"));
        assertTrue(e.getMessage().contains("nope.csv"), e.getMessage());
    }

    @Test
    void loads_from_file(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("aliases.csv");
        Files.writeString(file, "alias,canonical\nJAMAR,JAMAR Properties\n");
        assertEquals("JAMAR Properties", KeyValueCsv.load(file, "alias", "canonical").get("JAMAR"));
    }
}
