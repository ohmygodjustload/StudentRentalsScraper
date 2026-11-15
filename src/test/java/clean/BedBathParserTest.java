package clean;

import clean.BedBathParser.Counts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BedBathParserTest {

    @Test
    void parseTitle_handles_studio_decimals_and_missing_baths() {
        assertEquals(new Counts(0.0, 1.0), BedBathParser.parseTitle("Wise – Studio/1ba – 100 Main St"));
        assertEquals(new Counts(2.0, 1.5), BedBathParser.parseTitle("Lancer Village – 2br/1.5ba – 709 12th St S"));
        assertEquals(new Counts(3.0, null), BedBathParser.parseTitle("Landlord – 3br – 100 Main St"));
        assertEquals(new Counts(null, null), BedBathParser.parseTitle("Landlord – 1234 Main St – Available Now"));
    }

    @Test
    void parseTitle_accepts_bare_b_as_bedroom_unit() {
        assertEquals(new Counts(6.0, 2.0), BedBathParser.parseTitle("Miller Properties – 6b/2ba – 1 Some St"));
    }

    @Test
    void parseTitle_reads_studio_or_efficiency_before_bath() {
        assertEquals(new Counts(0.0, 1.0), BedBathParser.parseTitle("Wedgewood Commons – 1ba Studio – 1935 Miller St"));
        assertEquals(new Counts(0.0, 1.0), BedBathParser.parseTitle("Wedgewood Commons – 1ba Efficiency – 1935 Miller St"));
    }

    @Test
    void parseTitle_does_not_read_ba_as_bedroom_or_address_numbers_as_counts() {
        assertEquals(new Counts(null, 1.0), BedBathParser.parseTitle("Landlord – 1ba – 1 Bedford St"));
        assertEquals(new Counts(null, null), BedBathParser.parseTitle("Landlord – 100 Main St"));
    }

    @Test
    void parseField_handles_slash_studio_and_title_style() {
        assertEquals(new Counts(2.0, 1.0), BedBathParser.parseField("2/1"));
        assertEquals(new Counts(0.0, 1.0), BedBathParser.parseField("Studio/1"));
        assertEquals(new Counts(4.0, 2.0), BedBathParser.parseField("4br/2ba"));
        assertEquals(new Counts(0.0, 1.0), BedBathParser.parseField("Studio 1 bath"));
        assertEquals(new Counts(0.0, 1.0), BedBathParser.parseField("1ba Efficiency"));
        assertEquals(new Counts(null, null), BedBathParser.parseField("-"));
        assertEquals(new Counts(null, null), BedBathParser.parseField(null));
    }

    @Test
    void stripSiteSuffix_removes_breadcrumb_and_rental_listing_text() {
        assertEquals("Off-Campus La Crosse – 5br/1ba – 312 9th St N – Available 6/1/2028 – Pet Friendly",
                BedBathParser.stripSiteSuffix(
                        "Off-Campus La Crosse – 5br/1ba – 312 9th St N – Available 6/1/2028 – Pet Friendly Rental Listing  | All Rental Listings – La Crosse"));
        assertNull(BedBathParser.stripSiteSuffix(null));
    }

    @Test
    void disagree_requires_both_sides_present_and_different() {
        assertTrue(BedBathParser.disagree(5.0, 2.0));
        assertFalse(BedBathParser.disagree(2.0, 2.0));
        assertFalse(BedBathParser.disagree(null, 2.0));
        assertFalse(BedBathParser.disagree(2.0, null));
    }
}
