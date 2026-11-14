package clean;

import models.Apartment;
import models.FeatureType;
import models.Flag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CI-style fixture: raw JSON → post-process → clean, assert cleaner did real work.
 */
class RawVsCleanedFixtureTest {

    @Test
    void raw_fixture_is_normalized() throws Exception {
        List<Apartment> raw;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/raw_listings.json")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            raw = mapper.readValue(in,
                    mapper.getTypeFactory().constructCollectionType(List.class, Apartment.class));
        }

        ApartmentPostProcessor postProcessor = new ApartmentPostProcessor();
        DataCleaner cleaner = new DataCleaner();
        List<Apartment> cleaned = cleaner.clean(postProcessor.process(raw));

        Apartment first = cleaned.get(0);
        assertEquals("Off-Campus La Crosse LLC", first.getLandlord());
        assertEquals("2bd/1.5ba", first.getFeature(FeatureType.BED_BATH));
        assertEquals(2.0, first.getBedrooms());
        assertEquals(1.5, first.getBathrooms());
        assertEquals(1200, first.getRentMonthly());
        assertEquals("420 9th St N La Crosse WI", first.getAddress());
        assertTrue(first.getFlags().contains(Flag.FRACTIONAL_BATHROOM));

        Apartment second = cleaned.get(1);
        assertEquals("Favre Rentals", second.getLandlord());
        assertEquals("Studio/1ba", second.getFeature(FeatureType.BED_BATH));
        assertTrue(second.getFlags().contains(Flag.STUDIO));
        assertTrue(second.getFlags().contains(Flag.LANDLORD_FROM_TITLE));
        assertTrue(second.getFlags().contains(Flag.BEDBATH_FROM_TITLE));

        Apartment third = cleaned.get(2);
        assertEquals("Sawyer Properties", third.getLandlord());
        assertEquals("1.75bd/1ba", third.getFeature(FeatureType.BED_BATH));
        assertTrue(third.getFlags().contains(Flag.FRACTIONAL_BEDROOM));
    }
}
