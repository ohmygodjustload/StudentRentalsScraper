package utils;

import models.Apartment;
import models.FeatureType;
import models.Flag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonUtilsTest {

    @Test
    void toJsonString_serializes_apartment() {
        Apartment apt = new Apartment("123", "https://example.com/123");
        apt.setAddress("100 Main St La Crosse WI");
        apt.setPrice("800");

        String json = JsonUtils.toJsonString(apt);
        assertTrue(json.contains("\"id\""));
        assertTrue(json.contains("123"));
        assertTrue(json.contains("800"));
    }

    @Test
    void fromJsonString_throws_on_invalid_json() {
        assertThrows(RuntimeException.class, () ->
                JsonUtils.fromJsonString("not valid json", Apartment.class));
    }

    @Test
    void enriched_apartment_survives_round_trip(@TempDir Path tempDir) {
        Apartment apt = new Apartment("42", "https://example.com/42");
        apt.setAddress("100 Main St La Crosse WI");
        apt.setPrice("$900");
        apt.setRentMonthly(900);
        apt.setRawTitle("Title");
        apt.setFeature(FeatureType.LANDLORD, "Test Landlord");
        apt.setFeature(FeatureType.BED_BATH, "2bd/1ba");
        apt.setBedrooms(2.0);
        apt.setBathrooms(1.0);
        apt.addFlag(Flag.STUDIO);
        apt.setLatitude(43.81);
        apt.setLongitude(-91.23);
        apt.setAccuracy(1.0);
        apt.setGeocodedAddress("100 Main St");
        apt.setDistanceToCampus(0.5);
        apt.setWalkTravelTimeSeconds(720);
        apt.setBikeTravelTimeSeconds(300);
        apt.setWalkScore(80);
        apt.setWalkScoreDescription("Very Walkable");
        apt.setBikeScore(70);
        apt.setBikeScoreDescription("Bikeable");

        Path file = tempDir.resolve("apt.json");
        JsonUtils.toJson(List.of(apt), file.toString());
        List<Apartment> loaded = JsonUtils.fromJson(file.toString());

        assertEquals(1, loaded.size());
        Apartment round = loaded.get(0);
        assertEquals("42", round.getId());
        assertEquals("https://example.com/42", round.getUrl());
        assertEquals("Title", round.getRawTitle());
        assertEquals(Integer.valueOf(900), round.getRentMonthly());
        assertEquals(Double.valueOf(2.0), round.getBedrooms());
        assertEquals(Double.valueOf(43.81), round.getLatitude());
        assertEquals(Integer.valueOf(720), round.getWalkTravelTimeSeconds());
        assertEquals("12:00", round.getWalkTravelTimeFormatted());
        assertEquals(Integer.valueOf(300), round.getBikeTravelTimeSeconds());
        assertEquals("05:00", round.getBikeTravelTimeFormatted());
        assertEquals(Integer.valueOf(80), round.getWalkScore());
        assertEquals("Very Walkable", round.getWalkScoreDescription());
        assertEquals(Integer.valueOf(70), round.getBikeScore());
        assertTrue(round.getFlags().contains(Flag.STUDIO));
        assertEquals("Test Landlord", round.getLandlord());
        assertEquals("2bd/1ba", round.getBedBath());
    }
}
