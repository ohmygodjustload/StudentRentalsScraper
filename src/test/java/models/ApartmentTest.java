package models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApartmentTest {

    @Test
    void hasCoordinates_rejects_null_and_zero_zero() {
        Apartment apt = new Apartment("1", "https://example.com/1");
        assertFalse(apt.hasCoordinates());

        apt.setLatitude(0.0);
        apt.setLongitude(0.0);
        assertFalse(apt.hasCoordinates());

        apt.setLatitude(43.81);
        apt.setLongitude(-91.24);
        assertTrue(apt.hasCoordinates());
    }

    @Test
    void clearGeocode_nulls_coords_and_removes_geocode_flags() {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setLatitude(0.0);
        apt.setLongitude(0.0);
        apt.setAccuracy(1.0);
        apt.setGeocodedAddress("somewhere");
        apt.setDistanceToCampus(6279.0);
        apt.addFlag(Flag.GEOCODE_NO_MATCH);
        apt.addFlag(Flag.GEOCODE_APPROXIMATE);
        apt.addFlag(Flag.STUDIO);

        apt.clearGeocode();

        assertNull(apt.getLatitude());
        assertNull(apt.getLongitude());
        assertNull(apt.getAccuracy());
        assertNull(apt.getGeocodedAddress());
        assertNull(apt.getDistanceToCampus());
        assertFalse(apt.getFlags().contains(Flag.GEOCODE_NO_MATCH));
        assertFalse(apt.getFlags().contains(Flag.GEOCODE_APPROXIMATE));
        assertTrue(apt.getFlags().contains(Flag.STUDIO));
    }

    @Test
    void clearEnrichment_nulls_travel_and_score_fields() {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setWalkTravelTimeSeconds(0);
        apt.setBikeTravelTimeSeconds(0);
        apt.setWalkScore(50);
        apt.setWalkScoreDescription("Somewhat Walkable");
        apt.setBikeScore(0);
        apt.setBikeScoreDescription("Somewhat Bikeable");

        apt.clearEnrichment();

        assertNull(apt.getWalkTravelTimeSeconds());
        assertNull(apt.getWalkTravelTimeFormatted());
        assertNull(apt.getBikeTravelTimeSeconds());
        assertNull(apt.getBikeTravelTimeFormatted());
        assertNull(apt.getWalkScore());
        assertNull(apt.getWalkScoreDescription());
        assertNull(apt.getBikeScore());
        assertNull(apt.getBikeScoreDescription());
    }

    @Test
    void removeFlag_removes_only_matching_flag() {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.addFlag(Flag.STUDIO);
        apt.addFlag(Flag.GEOCODE_NO_MATCH);

        apt.removeFlag(Flag.GEOCODE_NO_MATCH);

        assertFalse(apt.getFlags().contains(Flag.GEOCODE_NO_MATCH));
        assertTrue(apt.getFlags().contains(Flag.STUDIO));
    }
}
