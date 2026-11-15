package clean;

import clean.BedBathAudit.DescriptionCounts;
import clean.BedBathAudit.DescriptionMatch;
import clean.BedBathAudit.Row;
import clean.BedBathAudit.TitleVsField;
import clean.BedBathParser.Counts;
import models.Apartment;
import models.FeatureType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BedBathAuditTest {

    private static Apartment listing(String title, String field, String description) {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setRawTitle(title);
        if (field != null) {
            apt.setFeature(FeatureType.BED_BATH, field);
        }
        apt.setRawDescription(description);
        return apt;
    }

    @Test
    void listing_424_title_and_description_beat_field_on_beds() {
        Row row = new BedBathAudit().auditOne(listing(
                "Off-Campus La Crosse – 5br/1ba – 312 9th St N – Available 6/1/2028 – Pet Friendly Rental Listing  | All Rental Listings – La Crosse",
                "2/1",
                "RENTED FOR 2027-2028 SCHOOL YEAR 312-314-316 9th St N, five bedrooms, 2 bath, 2 kitchen duplex. Plus $10/mo/per person water."));

        assertEquals(new Counts(5.0, 1.0), row.title());
        assertEquals(new Counts(2.0, 1.0), row.field());
        assertEquals(Set.of(5.0), row.description().beds());
        assertEquals(Set.of(2.0), row.description().baths());

        assertEquals(TitleVsField.CONFLICT, row.bedTitleVsField());
        assertEquals(DescriptionMatch.MATCHES_TITLE, row.bedDescription());
        assertEquals(TitleVsField.AGREE, row.bathTitleVsField());
        assertEquals(DescriptionMatch.MATCHES_NEITHER, row.bathDescription());
        assertFalse(row.titleText().contains("|"));
    }

    @Test
    void auditOne_reads_new_title_forms_from_shared_parser() {
        Row row = new BedBathAudit().auditOne(listing(
                "Miller Properties – 6b/2ba – 1 Some St", "6/2", null));
        assertEquals(new Counts(6.0, 2.0), row.title());
        assertEquals(TitleVsField.AGREE, row.bedTitleVsField());
        assertEquals(TitleVsField.AGREE, row.bathTitleVsField());
    }

    @Test
    void parseDescription_reads_digits_words_and_abbreviations() {
        DescriptionCounts counts = BedBathAudit.parseDescription(
                "Spacious 4-bedroom home with 2 full baths. Also listed as 4br/2ba.");
        assertEquals(Set.of(4.0), counts.beds());
        assertEquals(Set.of(2.0), counts.baths());
        assertFalse(counts.phrases().isEmpty());
    }

    @Test
    void parseDescription_flags_multiple_bed_counts_as_ambiguous() {
        DescriptionCounts counts = BedBathAudit.parseDescription("2 bedroom upstairs, 3 bedroom downstairs");
        assertEquals(Set.of(2.0, 3.0), counts.beds());
        assertEquals(DescriptionMatch.AMBIGUOUS, BedBathAudit.matchDescription(counts.beds(), 3.0, 2.0));
    }

    @Test
    void parseDescription_ignores_unrelated_numbers() {
        DescriptionCounts counts = BedBathAudit.parseDescription(
                "2 blocks from Western, 4 blocks from UW-L. $10/mo per person water. Bathtub included.");
        assertTrue(counts.beds().isEmpty());
        assertTrue(counts.baths().isEmpty());
    }

    @Test
    void compare_and_match_categories() {
        assertEquals(TitleVsField.AGREE, BedBathAudit.compare(3.0, 3.0));
        assertEquals(TitleVsField.CONFLICT, BedBathAudit.compare(3.0, 2.0));
        assertEquals(TitleVsField.TITLE_MISSING, BedBathAudit.compare(null, 2.0));
        assertEquals(TitleVsField.FIELD_MISSING, BedBathAudit.compare(3.0, null));
        assertEquals(TitleVsField.BOTH_MISSING, BedBathAudit.compare(null, null));

        assertEquals(DescriptionMatch.MISSING, BedBathAudit.matchDescription(Set.of(), 3.0, 2.0));
        assertEquals(DescriptionMatch.MATCHES_BOTH, BedBathAudit.matchDescription(Set.of(3.0), 3.0, 3.0));
        assertEquals(DescriptionMatch.MATCHES_FIELD, BedBathAudit.matchDescription(Set.of(2.0), 3.0, 2.0));
        assertEquals(DescriptionMatch.MATCHES_NEITHER, BedBathAudit.matchDescription(Set.of(4.0), 3.0, 2.0));
        assertEquals(DescriptionMatch.MATCHES_ONLY_SOURCE, BedBathAudit.matchDescription(Set.of(3.0), 3.0, null));
    }
}
