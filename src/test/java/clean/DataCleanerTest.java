package clean;

import models.Apartment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DataCleaner normalization (landlord, address).
 */
class DataCleanerTest {

    @Test
    void clean_normalizes_landlord_variations() {
        DataCleaner cleaner = new DataCleaner();
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setLandlord("Off-Campus La Crosse Inc");
        apt.setAddress("100 Main St La Crosse WI");

        List<Apartment> result = cleaner.clean(List.of(apt));
        assertEquals(1, result.size());
        assertEquals("Off-Campus La Crosse LLC", result.get(0).getLandlord());
    }

    @Test
    void clean_applies_address_corrections() {
        DataCleaner cleaner = new DataCleaner();
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setLandlord("Test");
        apt.setAddress("420 9th St N La Crosse");

        List<Apartment> result = cleaner.clean(List.of(apt));
        assertEquals(1, result.size());
        assertEquals("420 9th St N La Crosse WI", result.get(0).getAddress());
    }

    @Test
    void clean_handles_empty_list() {
        DataCleaner cleaner = new DataCleaner();
        List<Apartment> result = cleaner.clean(List.of());
        assertTrue(result.isEmpty());
    }
}
