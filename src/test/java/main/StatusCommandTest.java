package main;

import models.Apartment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import utils.JsonUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatusCommandTest {

    @Test
    void reports_latest_cleaned_and_suggests_resume(@TempDir Path tempDir) throws Exception {
        Path cleaned = tempDir.resolve("Cleaned");
        Files.createDirectories(cleaned);
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setAddress("100 Main St La Crosse WI");
        Path file = cleaned.resolve("listings_cleaned_01_01_2026_1200.json");
        JsonUtils.toJson(List.of(apt), file.toString());

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        StatusCommand status = new StatusCommand(new OutputPaths(tempDir), new PrintStream(buf, true, StandardCharsets.UTF_8));
        status.run();

        String text = buf.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Cleaned"), text);
        assertTrue(text.contains("listings_cleaned_01_01_2026_1200.json"), text);
        assertTrue(text.contains("listings"), text);
        assertTrue(text.contains("Census geocoder"), text);
        assertTrue(text.contains("srs resume") || text.contains("Next:"), text);
    }
}
