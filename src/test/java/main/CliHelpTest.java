package main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CliHelpTest {

    @Test
    void overview_lists_commands() {
        String text = CliHelp.overview();
        assertTrue(text.contains("full"));
        assertTrue(text.contains("status"));
        assertTrue(text.contains("--geocoder"));
        assertTrue(text.contains("--no-cache"));
    }

    @Test
    void command_help_includes_example() {
        String text = CliHelp.forCommand("geocode");
        assertTrue(text.contains("Command: geocode"));
        assertTrue(text.contains("Example:"));
        assertTrue(text.contains("Census"));
    }
}
