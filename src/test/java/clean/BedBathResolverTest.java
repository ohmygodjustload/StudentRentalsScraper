package clean;

import clean.BedBathOverrides.Override;
import models.Apartment;
import models.FeatureType;
import models.Flag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BedBathResolverTest {

    private static Apartment listing(String id, String title, String field) {
        Apartment apt = new Apartment(id, "https://example.com/" + id);
        apt.setRawTitle(title);
        apt.setFeature(FeatureType.BED_BATH, field);
        return apt;
    }

    private static Map<String, Override> overrides(Override... list) {
        Map<String, Override> map = new java.util.HashMap<>();
        for (Override o : list) {
            map.put(o.id(), o);
        }
        return map;
    }

    @Test
    void conflict_is_flagged_and_field_is_left_alone_without_override() {
        Apartment apt = listing("5", "Landlord – 3br/1ba – 1 Main St", "2/1");

        new BedBathResolver(Map.of()).resolve(List.of(apt));

        assertEquals("2/1", apt.getFeature(FeatureType.BED_BATH));
        assertTrue(apt.getFlags().contains(Flag.BEDBATH_CONFLICT));
        assertFalse(apt.getFlags().contains(Flag.BEDBATH_OVERRIDDEN));
    }

    @Test
    void override_replaces_field_when_raw_field_matches_expected() {
        Apartment apt = listing("424", "Off-Campus La Crosse – 5br/1ba – 312 9th St N", "2/1");
        Map<String, Override> map = overrides(new Override("424", "2/1", 5, 1, "verified"));

        new BedBathResolver(map).resolve(List.of(apt));

        assertEquals("5/1", apt.getFeature(FeatureType.BED_BATH));
        assertTrue(apt.getFlags().contains(Flag.BEDBATH_OVERRIDDEN));
        assertTrue(apt.getFlags().contains(Flag.BEDBATH_CONFLICT), "conflict stays visible for audit");
        assertFalse(apt.getFlags().contains(Flag.BEDBATH_OVERRIDE_STALE));
    }

    @Test
    void studio_override_writes_studio_text_the_cleaner_reads() {
        Apartment apt = listing("868", "Three Sixty – 1br/1ba – 430 12th St N", "3/1");
        Map<String, Override> map = overrides(new Override("868", "3/1", 0, 1, "studio only"));

        new BedBathResolver(map).resolve(List.of(apt));

        assertEquals("Studio/1", apt.getFeature(FeatureType.BED_BATH));
        assertTrue(apt.getFlags().contains(Flag.BEDBATH_OVERRIDDEN));
    }

    @Test
    void stale_override_is_skipped_and_flagged_when_site_field_changed() {
        Apartment apt = listing("424", "Off-Campus La Crosse – 5br/1ba – 312 9th St N", "3/1");
        Map<String, Override> map = overrides(new Override("424", "2/1", 5, 1, "verified"));

        new BedBathResolver(map).resolve(List.of(apt));

        assertEquals("3/1", apt.getFeature(FeatureType.BED_BATH));
        assertTrue(apt.getFlags().contains(Flag.BEDBATH_OVERRIDE_STALE));
        assertFalse(apt.getFlags().contains(Flag.BEDBATH_OVERRIDDEN));
    }

    @Test
    void matching_title_and_field_get_no_flags() {
        Apartment apt = listing("3", "Lancer Village – 2br/1ba – 709 12th St S", "2/1");

        new BedBathResolver(Map.of()).resolve(List.of(apt));

        assertEquals("2/1", apt.getFeature(FeatureType.BED_BATH));
        assertTrue(apt.getFlags().isEmpty(), apt.getFlags().toString());
    }

    @Test
    void expected_field_match_ignores_case_and_spaces() {
        Apartment apt = listing("424", "Off-Campus La Crosse – 5br/1ba – 312 9th St N", "2 / 1");
        Map<String, Override> map = overrides(new Override("424", "2/1", 5, 1, "verified"));

        new BedBathResolver(map).resolve(List.of(apt));

        assertEquals("5/1", apt.getFeature(FeatureType.BED_BATH));
        assertTrue(apt.getFlags().contains(Flag.BEDBATH_OVERRIDDEN));
    }

    @Test
    void conflict_detected_on_baths_alone() {
        Apartment apt = listing("9", "Landlord – 2br/1.5ba – 1 Main St", "2/1");

        new BedBathResolver(Map.of()).resolve(List.of(apt));

        assertTrue(apt.getFlags().contains(Flag.BEDBATH_CONFLICT));
    }
}
