package main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PipelineOptionsTest {

    @Test
    void no_args_sets_noArgs_flag() {
        PipelineOptions options = PipelineOptions.parse(new String[0]);
        assertTrue(options.isNoArgs());
        assertEquals(PipelineOptions.Mode.HELP, options.getMode());
    }

    @Test
    void parses_positional_command() {
        PipelineOptions options = PipelineOptions.parse(new String[] {
                "full",
                "--min-id=10",
                "--max-id=25",
                "--delay-ms=50",
                "--geocoder=census"
        });
        assertEquals(PipelineOptions.Mode.FULL_PIPELINE, options.getMode());
        assertEquals(10, options.getMinId());
        assertEquals(25, options.getMaxId());
        assertEquals(50, options.getDelayMs());
        assertEquals(PipelineOptions.GeocoderChoice.CENSUS, options.getGeocoder());
        assertFalse(options.isNoArgs());
    }

    @Test
    void mode_flag_still_works() {
        PipelineOptions options = PipelineOptions.parse(new String[] {"--mode=geocode"});
        assertEquals(PipelineOptions.Mode.GEOCODE_ONLY, options.getMode());
    }

    @Test
    void help_command_with_topic() {
        PipelineOptions options = PipelineOptions.parse(new String[] {"help", "full"});
        assertEquals(PipelineOptions.Mode.HELP, options.getMode());
        assertTrue(options.isHelpRequested());
        assertEquals("full", options.getHelpTopic());
    }

    @Test
    void help_flag() {
        PipelineOptions options = PipelineOptions.parse(new String[] {"--help"});
        assertTrue(options.isHelpRequested());
        assertEquals(PipelineOptions.Mode.HELP, options.getMode());
    }

    @Test
    void rejects_invalid_mode() {
        assertThrows(IllegalArgumentException.class,
                () -> PipelineOptions.parse(new String[] {"--mode=nope"}));
    }

    @Test
    void rejects_invalid_geocoder() {
        assertThrows(IllegalArgumentException.class,
                () -> PipelineOptions.parse(new String[] {"geocode", "--geocoder=bing"}));
    }

    @Test
    void rejects_invalid_range() {
        assertThrows(IllegalArgumentException.class,
                () -> PipelineOptions.parse(new String[] {"scrape-clean", "--min-id=10", "--max-id=5"}));
    }

    @Test
    void save_html_flag_defaults_off_and_parses_without_value() {
        assertFalse(PipelineOptions.parse(new String[] {"scrape-clean"}).isSaveHtml());

        PipelineOptions options = PipelineOptions.parse(new String[] {"scrape-clean", "--save-html", "--max-id=5"});
        assertTrue(options.isSaveHtml());
        assertEquals(5, options.getMaxId());
        assertTrue(options.withCommand(PipelineOptions.Mode.FULL_PIPELINE, null, null).isSaveHtml());
    }

    @Test
    void no_cache_flag_defaults_off_and_parses_without_value() {
        assertFalse(PipelineOptions.parse(new String[] {"apis"}).isNoCache());

        PipelineOptions options = PipelineOptions.parse(new String[] {"apis", "--no-cache"});
        assertTrue(options.isNoCache());
        assertTrue(options.withCommand(PipelineOptions.Mode.FULL_PIPELINE, null, null).isNoCache());
    }

    @Test
    void output_paths_cache_dir() {
        OutputPaths paths = new OutputPaths(java.nio.file.Path.of("output"), "11_14_2026_2130");
        assertEquals(java.nio.file.Path.of("output", "Cache"), paths.cacheDir());
    }

    @Test
    void audit_command() {
        PipelineOptions options = PipelineOptions.parse(new String[] {"audit"});
        assertEquals(PipelineOptions.Mode.AUDIT, options.getMode());
        assertFalse(options.getMode().isPipelineMode());
    }

    @Test
    void score_command() {
        PipelineOptions options = PipelineOptions.parse(new String[] {"score"});
        assertEquals(PipelineOptions.Mode.SCORE, options.getMode());
        assertFalse(options.getMode().isPipelineMode());
    }

    @Test
    void output_paths_html_and_audit_locations() {
        OutputPaths paths = new OutputPaths(java.nio.file.Path.of("output"), "11_14_2026_2130");
        assertEquals(java.nio.file.Path.of("output", "Html", "scrape_11_14_2026_2130"), paths.htmlDir());
        assertEquals("bedbath_audit_11_14_2026_2130.csv", paths.bedBathAuditCsv().getFileName().toString());
    }

    @Test
    void status_command() {
        PipelineOptions options = PipelineOptions.parse(new String[] {"status"});
        assertEquals(PipelineOptions.Mode.STATUS, options.getMode());
    }

    @Test
    void output_paths_timestamp_format() {
        OutputPaths paths = new OutputPaths(java.nio.file.Path.of("output"), "01_09_2026_0116");
        assertEquals("listings_cleaned_01_09_2026_0116.json",
                paths.cleanedJson().getFileName().toString());
        assertTrue(paths.getTimestamp().matches("\\d{2}_\\d{2}_\\d{4}_\\d{4}"));
    }
}
