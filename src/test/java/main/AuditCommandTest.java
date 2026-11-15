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

class AuditCommandTest {

    @Test
    void writes_csv_and_summary_from_latest_raw(@TempDir Path tempDir) throws Exception {
        Path rawDir = tempDir.resolve("Raw");
        Files.createDirectories(rawDir);

        Apartment conflict = new Apartment("424", "https://example.com/424");
        conflict.setRawTitle("Off-Campus La Crosse – 5br/1ba – 312 9th St N | All Rental Listings");
        conflict.setFeature(FeatureType.BED_BATH, "2/1");
        conflict.setFeature(FeatureType.LANDLORD, "Off-Campus La Crosse, LLC");
        conflict.setAddress("312 9th St N La Crosse WI");
        conflict.setRawDescription("five bedrooms, 2 bath, 2 kitchen duplex");

        Apartment agree = new Apartment("3", "https://example.com/3");
        agree.setRawTitle("Lancer Village – 2br/1.5ba – 709 12th St S | All Rental Listings");
        agree.setFeature(FeatureType.BED_BATH, "2/1.5");
        agree.setFeature(FeatureType.LANDLORD, "Wise Properties");
        agree.setAddress("709 12th St S La Crosse WI");

        JsonUtils.toJson(List.of(conflict, agree), rawDir.resolve("listings_scraped_11_14_2026_2100.json").toString());

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        OutputPaths paths = new OutputPaths(tempDir, "11_14_2026_2200");
        Path csv = new AuditCommand(paths, new PrintStream(buf, true, StandardCharsets.UTF_8)).run();

        assertEquals(paths.bedBathAuditCsv(), csv);
        List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("bedTitleVsField"));
        assertTrue(lines.get(0).contains("landlordCheck"));
        assertTrue(lines.get(0).contains("addressCheck"));

        String row424 = lines.stream().filter(l -> l.startsWith("\"424\"")).findFirst().orElseThrow();
        assertTrue(row424.contains("\"conflict\""), row424);
        assertTrue(row424.contains("\"matches_title\""), row424);
        assertTrue(row424.contains("\"5\""), row424);
        assertTrue(row424.contains("\"agree\""), row424);

        String text = buf.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Bedrooms - title vs Bed/Bath field"), text);
        assertTrue(text.contains("conflict"), text);
        assertTrue(text.contains("Landlord - title vs Landlord field"), text);
        assertTrue(text.contains("Address - title vs breadcrumb address"), text);
        assertTrue(text.contains("Report saved"), text);
    }
}
