package main;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for date/time formatting (migrated from src/tmp/DateTimeTest).
 */
class DateTimeFormatTest {

    @Test
    void timestamp_format_is_yyyyMMdd_HHmmss() {
        LocalDateTime fixed = LocalDateTime.of(2026, 1, 9, 1, 16, 0);
        String formatted = fixed.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        assertEquals("20260109_011600", formatted);
    }

    @Test
    void date_format_is_MM_dd_yyyy() {
        LocalDateTime fixed = LocalDateTime.of(2026, 1, 9, 1, 16, 0);
        String formatted = fixed.format(DateTimeFormatter.ofPattern("MM_dd_yyyy"));
        assertEquals("01_09_2026", formatted);
    }

    @Test
    void timestamp_format_has_no_spaces() {
        String formatted = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        assertFalse(formatted.contains(" "));
        assertTrue(formatted.matches("\\d{8}_\\d{6}"));
    }
}
