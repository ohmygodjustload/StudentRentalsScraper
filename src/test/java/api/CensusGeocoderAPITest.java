package api;

import models.Apartment;
import models.Flag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CensusGeocoderAPITest {

    @Test
    void splitAddress_parses_known_city_and_state() {
        CensusGeocoderAPI api = new CensusGeocoderAPI();
        CensusGeocoderAPI.AddressParts parts = api.splitAddress("709 12th St South La Crescent MN");
        assertEquals("709 12th St South", parts.street());
        assertEquals("La Crescent", parts.city());
        assertEquals("MN", parts.state());
    }

    @Test
    void splitAddress_parses_la_crosse() {
        CensusGeocoderAPI api = new CensusGeocoderAPI();
        CensusGeocoderAPI.AddressParts parts = api.splitAddress("420 9th St N La Crosse WI");
        assertEquals("420 9th St N", parts.street());
        assertEquals("La Crosse", parts.city());
        assertEquals("WI", parts.state());
    }

    @Test
    void parseResponse_sets_exact_approx_and_no_match() throws Exception {
        String body = new String(
                getClass().getResourceAsStream("/fixtures/census_batch_response.csv").readAllBytes(),
                StandardCharsets.UTF_8);

        Apartment exact = new Apartment("101", "https://example.com/101");
        Apartment approx = new Apartment("102", "https://example.com/102");
        Apartment none = new Apartment("103", "https://example.com/103");

        Map<String, Apartment> byId = new HashMap<>();
        byId.put("101", exact);
        byId.put("102", approx);
        byId.put("103", none);

        CensusGeocoderAPI api = new CensusGeocoderAPI();
        api.parseResponse(body, byId);

        assertEquals(43.81, exact.getLatitude());
        assertEquals(-91.24, exact.getLongitude());
        assertEquals("100 MAIN ST, LA CROSSE, WI, 54601", exact.getGeocodedAddress());
        assertFalse(exact.getFlags().contains(Flag.GEOCODE_APPROXIMATE));
        assertFalse(exact.getFlags().contains(Flag.GEOCODE_NO_MATCH));

        assertEquals(43.82, approx.getLatitude());
        assertTrue(approx.getFlags().contains(Flag.GEOCODE_APPROXIMATE));

        assertNull(none.getLatitude());
        assertNull(none.getLongitude());
        assertTrue(none.getFlags().contains(Flag.GEOCODE_NO_MATCH));
        assertFalse(none.hasCoordinates());
    }

    @Test
    void parseResponse_no_match_does_not_set_coordinates_on_stale_zeros() throws Exception {
        String body = new String(
                getClass().getResourceAsStream("/fixtures/census_batch_response.csv").readAllBytes(),
                StandardCharsets.UTF_8);

        Apartment none = new Apartment("103", "https://example.com/103");
        none.setLatitude(0.0);
        none.setLongitude(0.0);
        none.clearGeocode();

        Map<String, Apartment> byId = new HashMap<>();
        byId.put("103", none);

        CensusGeocoderAPI api = new CensusGeocoderAPI();
        api.parseResponse(body, byId);

        assertNull(none.getLatitude());
        assertNull(none.getLongitude());
        assertFalse(none.hasCoordinates());
        assertTrue(none.getFlags().contains(Flag.GEOCODE_NO_MATCH));
    }

    @Test
    void geocode_posts_multipart_and_applies_results() throws Exception {
        String body = new String(
                getClass().getResourceAsStream("/fixtures/census_batch_response.csv").readAllBytes(),
                StandardCharsets.UTF_8);
        FakeHttpTransport transport = new FakeHttpTransport().enqueue(200, body);

        Apartment apt = new Apartment("101", "https://example.com/101");
        apt.setAddress("100 Main St La Crosse WI");

        CensusGeocoderAPI api = new CensusGeocoderAPI(transport);
        api.geocode(List.of(apt));

        assertEquals(1, transport.getRequests().size());
        assertTrue(transport.getRequests().get(0).uri().toString().contains("addressbatch"));
        assertEquals(43.81, apt.getLatitude());
        assertEquals(-91.24, apt.getLongitude());
    }
}
