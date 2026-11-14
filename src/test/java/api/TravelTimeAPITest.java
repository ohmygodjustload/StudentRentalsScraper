package api;

import models.Apartment;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TravelTimeAPITest {

    @Test
    void addTravelTimes_sets_walk_and_bike() throws Exception {
        String body = new String(
                getClass().getResourceAsStream("/fixtures/traveltime_response.json").readAllBytes(),
                StandardCharsets.UTF_8);
        FakeHttpTransport transport = new FakeHttpTransport()
                .enqueue(200, body)
                .enqueue(200, body.replace("720", "300"));

        Apartment apt = new Apartment("101", "https://example.com/101");
        apt.setLatitude(43.81);
        apt.setLongitude(-91.24);

        TravelTimeAPI api = new TravelTimeAPI(
                "key", "id", 43.8161, -91.2311, 900, 8, transport);
        api.addTravelTimes(List.of(apt));

        assertEquals(720, apt.getWalkTravelTimeSeconds());
        assertEquals("12:00", apt.getWalkTravelTimeFormatted());
        assertEquals(300, apt.getBikeTravelTimeSeconds());
        assertEquals(2, transport.getRequests().size());
    }

    @Test
    void skips_ungeocoded_apartments() {
        FakeHttpTransport transport = new FakeHttpTransport();
        Apartment apt = new Apartment("101", "https://example.com/101");

        TravelTimeAPI api = new TravelTimeAPI(
                "key", "id", 43.8161, -91.2311, 900, 8, transport);
        api.addTravelTimes(List.of(apt));

        assertTrue(transport.getRequests().isEmpty());
        assertNull(apt.getWalkTravelTimeSeconds());
    }
}
