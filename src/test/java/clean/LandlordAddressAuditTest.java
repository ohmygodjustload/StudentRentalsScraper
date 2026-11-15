package clean;

import clean.LandlordAddressAudit.Check;
import models.Apartment;
import models.FeatureType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LandlordAddressAuditTest {

    @Test
    void title_landlord_is_text_before_first_separator() {
        assertEquals("Off-Campus La Crosse",
                LandlordAddressAudit.titleLandlord("Off-Campus La Crosse – 5br/1ba – 312 9th St N"));
        assertEquals("Off-Campus La Crosse",
                LandlordAddressAudit.titleLandlord("Off-Campus La Crosse - 5br/1ba - 312 9th St N"));
    }

    @Test
    void title_landlord_is_missing_when_title_starts_with_bed_bath_or_number() {
        assertNull(LandlordAddressAudit.titleLandlord("5br/1ba – 312 9th St N"));
        assertNull(LandlordAddressAudit.titleLandlord("1ba Studio – 312 9th St N"));
        assertNull(LandlordAddressAudit.titleLandlord(null));
    }

    @Test
    void landlord_name_starting_with_a_number_is_not_mistaken_for_address() {
        String title = "608 Rentals – 2br/1ba – 1324 Vine St";
        assertEquals("608 Rentals", LandlordAddressAudit.titleLandlord(title));
        assertEquals("1324 Vine St", LandlordAddressAudit.titleAddress(title));
    }

    @Test
    void title_address_is_first_segment_starting_with_house_number() {
        assertEquals("709 12th St S",
                LandlordAddressAudit.titleAddress("Lancer Village – 2br/1.5ba – 709 12th St S- Inquire Pets"));
        assertEquals("312 9th St N",
                LandlordAddressAudit.titleAddress("Off-Campus La Crosse – 5br/1ba – 312 9th St N – Available 6/1/2028"));
        assertNull(LandlordAddressAudit.titleAddress("Wise – 1ba Studio – Available Now"));
    }

    @Test
    void landlord_agrees_ignoring_case_punctuation_and_company_suffix() {
        assertEquals(Check.AGREE, LandlordAddressAudit.compareLandlord(
                "Off-Campus La Crosse", "Off-Campus La Crosse, LLC"));
        assertEquals(Check.AGREE, LandlordAddressAudit.compareLandlord("Wise Properties", "Wise"));
        assertEquals(Check.AGREE, LandlordAddressAudit.compareLandlord("G&N Properties", "g&n properties"));
    }

    @Test
    void landlord_conflicts_when_names_differ() {
        assertEquals(Check.CONFLICT, LandlordAddressAudit.compareLandlord("Miller Properties", "Wise Properties"));
    }

    @Test
    void landlord_missing_categories() {
        assertEquals(Check.TITLE_MISSING, LandlordAddressAudit.compareLandlord(null, "Wise Properties"));
        assertEquals(Check.FIELD_MISSING, LandlordAddressAudit.compareLandlord("Wise Properties", ""));
        assertEquals(Check.FIELD_MISSING, LandlordAddressAudit.compareLandlord("Wise Properties", null));
    }

    @Test
    void address_agrees_with_breadcrumb_city_and_street_abbreviations() {
        assertEquals(Check.AGREE, LandlordAddressAudit.compareAddress("312 9th St N", "312 9th St N La Crosse WI"));
        assertEquals(Check.AGREE, LandlordAddressAudit.compareAddress("100 North Ave", "100 N Ave La Crosse WI"));
        assertEquals(Check.AGREE, LandlordAddressAudit.compareAddress("624 8th St S 8th St S", "624 8th St S La Crosse WI"));
    }

    @Test
    void address_conflicts_on_different_house_number_or_street() {
        assertEquals(Check.CONFLICT, LandlordAddressAudit.compareAddress("1 Main St", "11 Main St La Crosse WI"));
        assertEquals(Check.CONFLICT, LandlordAddressAudit.compareAddress("312 9th St N", "418 9th St N La Crosse WI"));
        assertEquals(Check.CONFLICT, LandlordAddressAudit.compareAddress("312 9th St N", "312 Cass St La Crosse WI"));
    }

    @Test
    void address_missing_categories() {
        assertEquals(Check.TITLE_MISSING, LandlordAddressAudit.compareAddress(null, "312 9th St N La Crosse WI"));
        assertEquals(Check.FIELD_MISSING, LandlordAddressAudit.compareAddress("312 9th St N", ""));
    }

    @Test
    void check_reads_title_landlord_field_and_breadcrumb_from_listing() {
        Apartment apt = new Apartment("424", "https://example.com/424");
        apt.setRawTitle("Off-Campus La Crosse – 5br/1ba – 312 9th St N – Available 6/1/2028 | All Rental Listings");
        apt.setFeature(FeatureType.LANDLORD, "Off-Campus La Crosse, LLC");
        apt.setAddress("312 9th St N La Crosse WI");

        LandlordAddressAudit.Result result = LandlordAddressAudit.check(apt);

        assertEquals("Off-Campus La Crosse", result.titleLandlord());
        assertEquals(Check.AGREE, result.landlord());
        assertEquals("312 9th St N", result.titleAddress());
        assertEquals(Check.AGREE, result.address());
    }
}
