package clean;

import models.Apartment;
import models.FeatureType;
import models.Flag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataCleanerTest {

    private final DataCleaner cleaner = new DataCleaner();

    @Test
    void clean_normalizes_landlord_variations() {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setLandlord("Off-Campus La Crosse Inc");
        apt.setAddress("100 Main St La Crosse WI");
        apt.setFeature(FeatureType.BED_BATH, "2/1");

        List<Apartment> result = cleaner.clean(List.of(apt));
        assertEquals("Off-Campus La Crosse LLC", result.get(0).getLandlord());
    }

    @Test
    void clean_applies_address_corrections() {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setLandlord("Test");
        apt.setAddress("420 9th St N La Crosse");
        apt.setFeature(FeatureType.BED_BATH, "2/1");

        List<Apartment> result = cleaner.clean(List.of(apt));
        assertEquals("420 9th St N La Crosse WI", result.get(0).getAddress());
    }

    @Test
    void clean_handles_empty_list() {
        assertTrue(cleaner.clean(List.of()).isEmpty());
    }

    @Test
    void normalizeBedBath_slash_formats() {
        assertBedBath("2/1.5", "2bd/1.5ba", 2.0, 1.5, Flag.FRACTIONAL_BATHROOM);
        assertBedBath("1.75/1", "1.75bd/1ba", 1.75, 1.0, Flag.FRACTIONAL_BEDROOM);
        assertBedBath("0/1", "Studio/1ba", 0.0, 1.0, Flag.STUDIO);
        assertBedBath("2br/1ba", "2bd/1ba", 2.0, 1.0);
    }

    @Test
    void normalizeBedBath_studio_space_format() {
        assertBedBath("Studio 1 bath", "Studio/1ba", 0.0, 1.0, Flag.STUDIO);
    }

    @Test
    void normalizeBedBath_unparseable_sets_flag() {
        Apartment apt = base("weird beds");
        cleaner.normalizeBedBath(apt);
        assertTrue(apt.getFlags().contains(Flag.BEDBATH_UNPARSEABLE));
        assertEquals("weird beds", apt.getFeature(FeatureType.BED_BATH));
    }

    @Test
    void normalizeBedBath_unusual_count_flag() {
        Apartment apt = base("12/2");
        cleaner.normalizeBedBath(apt);
        assertTrue(apt.getFlags().contains(Flag.UNUSUAL_BEDROOM_COUNT));
        assertEquals("12bd/2ba", apt.getFeature(FeatureType.BED_BATH));
    }

    @Test
    void parseRent_strips_currency_and_commas() {
        assertEquals(1200, cleaner.parseRent("$1,200"));
        assertEquals(980, cleaner.parseRent("$980"));
        assertNull(cleaner.parseRent(""));
        assertNull(cleaner.parseRent(null));
    }

    @Test
    void parseRent_zero_is_unknown_not_free() {
        assertNull(cleaner.parseRent("$0"));
    }

    @Test
    void normalizeAddress_appends_state_without_breaking_pattern() {
        String result = cleaner.normalizeAddress("100 Main St La Crosse");
        assertTrue(result.endsWith("WI"), result);
        assertFalse(result.contains(", WI"));
    }

    @Test
    void normalizeLandlord_unknown_and_favre_typo() {
        assertEquals("Unknown", cleaner.normalizeLandlord(null));
        assertEquals("Favre Rentals", cleaner.normalizeLandlord("Favre Rentals LLC"));
        assertEquals("Favre Rentals", cleaner.normalizeLandlord("Favre Retnals"));
    }

    @Test
    void uses_injected_maps_without_reading_files() {
        DataCleaner custom = new DataCleaner(
                Map.of("Acme Rentls", "Acme Rentals"),
                Map.of("1 Test St La Crosse", "1 Test St La Crosse WI"));

        assertEquals("Acme Rentals", custom.normalizeLandlord("Acme Rentls"));
        assertEquals("Favre Retnals", custom.normalizeLandlord("Favre Retnals"));
        assertEquals("1 Test St La Crosse WI", custom.normalizeAddress("1 Test St La Crosse"));
    }

    @Test
    void loadDefault_reads_data_files() throws Exception {
        DataCleaner loaded = DataCleaner.loadDefault();

        assertEquals(72, loaded.landlordAliasCount());
        assertEquals(26, loaded.addressCorrectionCount());
        assertEquals("Favre Rentals", loaded.normalizeLandlord("Favre Retnals"));
        assertEquals("Off-Campus La Crosse LLC", loaded.normalizeLandlord("Off-Campus La Crosse, LLC"));
        assertEquals("MAT Properties", loaded.normalizeLandlord("MAT Properties, LLC"));
        assertEquals("821 State St, #1, La Crosse WI", loaded.normalizeAddress("821 State St #1 La Crosse WI"));
        assertEquals("330 21st St N La Crosse WI", loaded.normalizeAddress("330 21th St N La Crosse WI"));
    }

    private void assertBedBath(String raw, String expected, double beds, double baths, Flag... flags) {
        Apartment apt = base(raw);
        cleaner.normalizeBedBath(apt);
        assertEquals(expected, apt.getFeature(FeatureType.BED_BATH));
        assertEquals(beds, apt.getBedrooms());
        assertEquals(baths, apt.getBathrooms());
        for (Flag flag : flags) {
            assertTrue(apt.getFlags().contains(flag), "missing flag " + flag);
        }
    }

    private Apartment base(String bedBath) {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setFeature(FeatureType.BED_BATH, bedBath);
        return apt;
    }
}
