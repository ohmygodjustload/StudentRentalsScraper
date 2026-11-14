package main;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class InteractiveMenuTest {

    @Test
    void non_interactive_prints_help_and_returns_help_args() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InteractiveMenu menu = new InteractiveMenu(
                new InputStreamReader(new ByteArrayInputStream(new byte[0]), StandardCharsets.UTF_8),
                new PrintStream(out, true, StandardCharsets.UTF_8),
                false);

        String[] args = menu.run(PipelineOptions.parse(new String[0]));
        assertArrayEquals(new String[] {"help"}, args);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Commands:"));
    }

    @Test
    void interactive_scrape_clean_collects_id_range() throws Exception {
        String input = "2\n0\n25\n";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InteractiveMenu menu = new InteractiveMenu(
                new InputStreamReader(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8),
                new PrintStream(out, true, StandardCharsets.UTF_8),
                true);

        String[] args = menu.run(PipelineOptions.parse(new String[0]));
        assertArrayEquals(new String[] {"scrape-clean", "--min-id=0", "--max-id=25"}, args);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains(".\\srs.ps1 scrape-clean"));
    }

    @Test
    void quit_returns_null() throws Exception {
        String input = "9\n";
        InteractiveMenu menu = new InteractiveMenu(
                new InputStreamReader(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                        StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                true);

        assertNull(menu.run(PipelineOptions.parse(new String[0])));
    }
}
