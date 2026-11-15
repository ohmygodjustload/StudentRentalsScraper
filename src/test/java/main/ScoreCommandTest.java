package main;

import models.Apartment;
import models.FeatureType;
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

class ScoreCommandTest {

    @Test
    void rescores_latest_final_and_writes_new_timestamped_outputs(@TempDir Path tempDir) throws Exception {
        Path finalDir = tempDir.resolve("Final");
        Files.createDirectories(finalDir);

        Apartment cheap = new Apartment("10", "https://example.com/10");
        cheap.setRentMonthly(800);
        cheap.setBedrooms(2.0);
        cheap.setLatitude(43.8);
        cheap.setLongitude(-91.2);
        cheap.setFeature(FeatureType.BED_BATH, "2bd/1ba");
        cheap.setAddress("100 Main St La Crosse WI");

        Apartment unknown = new Apartment("11", "https://example.com/11");
        unknown.setFeature(FeatureType.BED_BATH, "Studio/1ba");

        JsonUtils.toJson(List.of(cheap, unknown),
                finalDir.resolve("listings_final_11_14_2026_2200.json").toString());

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        OutputPaths paths = new OutputPaths(tempDir, "11_14_2026_2300");
        Path json = new ScoreCommand(paths, new PrintStream(buf, true, StandardCharsets.UTF_8)).run();

        assertEquals(paths.finalJson(), json);
        List<Apartment> rescored = JsonUtils.fromJson(json.toString());
        assertEquals(2, rescored.size());
        Apartment scored = rescored.stream().filter(a -> a.getId().equals("10")).findFirst().orElseThrow();
        assertNotNull(scored.getDealScore());
        assertNotNull(scored.getDealScoreDetail());

        String csv = Files.readString(paths.finalCsv(), StandardCharsets.UTF_8);
        assertTrue(csv.lines().findFirst().orElseThrow().contains("dealScore"), csv);
        assertTrue(Files.exists(paths.finalCsv()));

        String text = buf.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Scored 2 of 2 listings"), text);
        assertTrue(text.contains("Top 10:"), text);
        assertTrue(text.contains("Final JSON saved"), text);
    }
}
