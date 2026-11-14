package clean;

import models.Apartment;
import models.FeatureType;
import models.Flag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApartmentPostProcessorTest {

    private final ApartmentPostProcessor processor = new ApartmentPostProcessor();

    @Test
    void fills_landlord_and_bedbath_from_title() {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setRawTitle("Miller Properties - 3br/1.5ba - Available 8/1");

        processor.process(List.of(apt));

        assertEquals("Miller Properties", apt.getFeature(FeatureType.LANDLORD));
        assertEquals("3br/1.5ba", apt.getFeature(FeatureType.BED_BATH));
        assertTrue(apt.getFlags().contains(Flag.LANDLORD_FROM_TITLE));
        assertTrue(apt.getFlags().contains(Flag.BEDBATH_FROM_TITLE));
    }

    @Test
    void fills_studio_from_title() {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setRawTitle("Favre Rentals LLC - Studio 1 bath - No Pets");

        processor.process(List.of(apt));

        assertEquals("Favre Rentals LLC", apt.getFeature(FeatureType.LANDLORD));
        assertTrue(apt.getFeature(FeatureType.BED_BATH).toLowerCase().contains("studio"));
        assertTrue(apt.getFlags().contains(Flag.BEDBATH_FROM_TITLE));
    }

    @Test
    void marks_missing_when_title_unusable() {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setRawTitle("Weird Listing Without Pattern");

        processor.process(List.of(apt));

        assertTrue(apt.getFlags().contains(Flag.MISSING_LANDLORD));
        assertTrue(apt.getFlags().contains(Flag.MISSING_BEDBATH));
    }

    @Test
    void leaves_apartment_unchanged_when_rawTitle_absent() {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setFeature(FeatureType.LANDLORD, "Existing");
        apt.setFeature(FeatureType.BED_BATH, "2/1");

        processor.process(List.of(apt));

        assertEquals("Existing", apt.getFeature(FeatureType.LANDLORD));
        assertEquals("2/1", apt.getFeature(FeatureType.BED_BATH));
        assertTrue(apt.getFlags().isEmpty());
    }

    @Test
    void does_not_overwrite_existing_features() {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setRawTitle("Other Landlord - 5br/2ba");
        apt.setFeature(FeatureType.LANDLORD, "Keep Me");
        apt.setFeature(FeatureType.BED_BATH, "2/1");

        processor.process(List.of(apt));

        assertEquals("Keep Me", apt.getFeature(FeatureType.LANDLORD));
        assertEquals("2/1", apt.getFeature(FeatureType.BED_BATH));
        assertTrue(apt.getFlags().isEmpty());
    }
}
