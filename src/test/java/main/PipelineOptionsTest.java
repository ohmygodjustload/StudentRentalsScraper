package main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PipelineOptionsTest {

    @Test
    void defaults_to_scrape_clean_mode() {
        PipelineOptions options = PipelineOptions.parse(new String[0]);
        assertEquals(PipelineOptions.Mode.SCRAPE_AND_CLEAN_ONLY, options.getMode());
        assertEquals(0, options.getMinId());
        assertEquals(900, options.getMaxId());
        assertEquals(1000, options.getDelayMs());
        assertFalse(options.isHelpRequested());
    }

    @Test
    void parses_cli_overrides() {
        PipelineOptions options = PipelineOptions.parse(new String[] {
                "--mode=full",
                "--min-id=10",
                "--max-id=25",
                "--delay-ms=50",
                "--output-dir=tmp-out"
        });
        assertEquals(PipelineOptions.Mode.FULL_PIPELINE, options.getMode());
        assertEquals(10, options.getMinId());
        assertEquals(25, options.getMaxId());
        assertEquals(50, options.getDelayMs());
        assertTrue(options.getOutputDir().toString().replace('\\', '/').endsWith("tmp-out"));
    }

    @Test
    void help_flag() {
        PipelineOptions options = PipelineOptions.parse(new String[] {"--help"});
        assertTrue(options.isHelpRequested());
    }

    @Test
    void rejects_invalid_mode() {
        assertThrows(IllegalArgumentException.class,
                () -> PipelineOptions.parse(new String[] {"--mode=nope"}));
    }

    @Test
    void rejects_invalid_range() {
        assertThrows(IllegalArgumentException.class,
                () -> PipelineOptions.parse(new String[] {"--min-id=10", "--max-id=5"}));
    }

    @Test
    void output_paths_timestamp_format() {
        OutputPaths paths = new OutputPaths(java.nio.file.Path.of("output"), "01_09_2026_0116");
        assertEquals("listings_cleaned_01_09_2026_0116.json",
                paths.cleanedJson().getFileName().toString());
        assertTrue(paths.getTimestamp().matches("\\d{2}_\\d{2}_\\d{4}_\\d{4}"));
    }
}
