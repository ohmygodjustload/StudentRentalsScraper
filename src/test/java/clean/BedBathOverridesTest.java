package clean;

import clean.BedBathOverrides.Override;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BedBathOverridesTest {

    private static final String HEADER = "id,expectedField,beds,baths,note\n";

    @Test
    void parses_valid_rows_including_studio_and_quoted_notes() {
        Map<String, Override> overrides = BedBathOverrides.parse(
                HEADER
                        + "424,2/1,5,1,\"Title says 5br, description says five\"\n"
                        + "868,3/1,Studio,1,Studio floorplans only\n",
                "test");

        assertEquals(2, overrides.size());
        Override p424 = overrides.get("424");
        assertEquals("2/1", p424.expectedField());
        assertEquals(5.0, p424.beds());
        assertEquals("5/1", p424.formatted());
        assertEquals("Title says 5br, description says five", p424.note());

        Override p868 = overrides.get("868");
        assertEquals(0.0, p868.beds());
        assertEquals("Studio/1", p868.formatted());
    }

    @Test
    void formats_fractional_baths() {
        Override o = new Override("1", "2/1.5", 2, 1.5, "");
        assertEquals("2/1.5", o.formatted());
    }

    @Test
    void skips_blank_lines_and_comments() {
        Map<String, Override> overrides = BedBathOverrides.parse(
                "# hand-verified\n\n" + HEADER + "# inline comment row\n\n771,4/1,5,1,ok\n",
                "test");
        assertEquals(1, overrides.size());
        assertTrue(overrides.containsKey("771"));
    }

    @Test
    void empty_file_yields_no_overrides() {
        assertTrue(BedBathOverrides.parse("", "test").isEmpty());
        assertTrue(BedBathOverrides.parse("# only comments\n", "test").isEmpty());
    }

    @Test
    void invalid_beds_error_names_the_line() {
        String text = "# comment\n" + HEADER + "\n424,2/1,five,1,x\n";
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> BedBathOverrides.parse(text, "data/bedbath_overrides.csv"));
        assertTrue(e.getMessage().contains("line 4"), e.getMessage());
        assertTrue(e.getMessage().contains("beds"), e.getMessage());
    }

    @Test
    void negative_or_non_numeric_baths_are_rejected() {
        IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
                () -> BedBathOverrides.parse(HEADER + "424,2/1,5,-1,x\n", "test"));
        assertTrue(negative.getMessage().contains("baths"), negative.getMessage());

        IllegalArgumentException text = assertThrows(IllegalArgumentException.class,
                () -> BedBathOverrides.parse(HEADER + "424,2/1,5,one,x\n", "test"));
        assertTrue(text.getMessage().contains("line 2"), text.getMessage());
    }

    @Test
    void missing_id_or_expected_field_is_rejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BedBathOverrides.parse(HEADER + ",2/1,5,1,x\n", "test"));
        assertThrows(IllegalArgumentException.class,
                () -> BedBathOverrides.parse(HEADER + "424,,5,1,x\n", "test"));
    }

    @Test
    void duplicate_ids_are_rejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> BedBathOverrides.parse(HEADER + "424,2/1,5,1,a\n424,2/1,4,1,b\n", "test"));
        assertTrue(e.getMessage().contains("duplicate id 424"), e.getMessage());
    }

    @Test
    void header_must_include_required_columns() {
        assertThrows(IllegalArgumentException.class,
                () -> BedBathOverrides.parse("id,beds,baths\n424,5,1\n", "test"));
    }

    @Test
    void missing_file_means_no_overrides(@TempDir Path tempDir) throws Exception {
        assertTrue(BedBathOverrides.load(tempDir.resolve("nope.csv")).isEmpty());
    }

    @Test
    void loads_from_file(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("bedbath_overrides.csv");
        Files.writeString(file, HEADER + "771,4/1,5,1,ok\n");
        assertEquals("5/1", BedBathOverrides.load(file).get("771").formatted());
    }
}
