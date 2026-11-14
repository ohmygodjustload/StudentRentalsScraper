package api;

import models.Apartment;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeocodioAPITest {

    @Test
    void geocode_sets_coordinates() throws Exception {
        String body = new String(
                getClass().getResourceAsStream("/fixtures/geocodio_batch_response.json").readAllBytes(),
                StandardCharsets.UTF_8);
        FakeHttpTransport transport = new FakeHttpTransport().enqueue(200, body);

        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setAddress("100 Main St La Crosse WI");

        GeocodioAPI api = new GeocodioAPI("key", transport);
        api.geocode(List.of(apt));

        assertEquals(43.81, apt.getLatitude());
        assertEquals(-91.24, apt.getLongitude());
        assertEquals(1.0, apt.getAccuracy());
        assertEquals("100 Main St, La Crosse, WI 54601", apt.getGeocodedAddress());
    }
}
