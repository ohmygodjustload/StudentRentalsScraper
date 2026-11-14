package geocoding;

import models.Apartment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataMergerTest {

    @Test
    void merges_geocodio_csv_by_url(@TempDir Path tempDir) throws IOException {
        Path csv = tempDir.resolve("geocodio.csv");
        Files.copy(
                getClass().getResourceAsStream("/fixtures/geocodio_sample.csv"),
                csv);

        Apartment a1 = new Apartment("1", "https://example.com/1");
        Apartment a2 = new Apartment("2", "https://example.com/2");
        Apartment a3 = new Apartment("3", "https://example.com/3");

        DataMerger merger = new DataMerger();
        merger.mergeGeocodioData(List.of(a1, a2, a3), csv.toString());

        assertEquals(43.8101, a1.getLatitude());
        assertEquals(-91.2401, a1.getLongitude());
        assertEquals(1.0, a1.getAccuracy());
        assertEquals("100 Main St", a1.getGeocodioAddress());

        assertEquals(43.8202, a2.getLatitude());
        assertNull(a3.getLatitude());
    }

    @Test
    void haversine_zero_for_same_point() {
        assertEquals(0.0, GeoUtils.haversineMiles(43.8, -91.2, 43.8, -91.2), 0.0001);
    }

    @Test
    void haversine_known_short_distance() {
        double miles = GeoUtils.haversineMiles(43.8161, -91.2311, 43.8261, -91.2311);
        assertTrue(miles > 0.6 && miles < 0.8, "expected ~0.69 miles, got " + miles);
    }
}
