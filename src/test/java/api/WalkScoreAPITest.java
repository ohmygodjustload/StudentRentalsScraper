package api;

import models.Apartment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WalkScoreAPITest {

    @TempDir
    Path tempDir;

    @Test
    void addWalkScores_sets_scores() throws Exception {
        String body = fixtureBody();
        FakeHttpTransport transport = new FakeHttpTransport().enqueue(200, body);

        Apartment apt = geocoded("1", 43.81, -91.24);
        apt.setAddress("100 Main St");

        WalkScoreAPI api = new WalkScoreAPI("key", transport, 0);
        api.addWalkScores(List.of(apt));

        assertEquals(78, apt.getWalkScore());
        assertEquals("Very Walkable", apt.getWalkScoreDescription());
        assertEquals(65, apt.getBikeScore());
        assertEquals("Bikeable", apt.getBikeScoreDescription());
    }

    @Test
    void cache_hit_makes_no_http_request() throws Exception {
        String body = fixtureBody();
        Path cacheFile = tempDir.resolve("walkscore.json");
        ResponseCache cache = ResponseCache.load(cacheFile, false);

        Apartment apt = geocoded("1", 43.81, -91.24);
        WalkScoreAPI keyApi = new WalkScoreAPI("key", new FakeHttpTransport(), 0, cache);
        cache.put(keyApi.cacheKey(apt), body);
        cache.save();

        FakeHttpTransport transport = new FakeHttpTransport();
        ResponseCache loaded = ResponseCache.load(cacheFile, false);
        WalkScoreAPI api = new WalkScoreAPI("key", transport, 0, loaded);
        Apartment apt2 = geocoded("1", 43.81, -91.24);
        api.addWalkScores(List.of(apt2));

        assertTrue(transport.getRequests().isEmpty());
        assertEquals(78, apt2.getWalkScore());
        assertEquals(65, apt2.getBikeScore());
    }

    @Test
    void status_2_response_is_not_cached() {
        String body = """
                {"status": 2, "walkscore": null, "description": null}
                """;
        FakeHttpTransport transport = new FakeHttpTransport().enqueue(200, body);
        Path cacheFile = tempDir.resolve("walkscore.json");
        ResponseCache cache = ResponseCache.load(cacheFile, false);

        Apartment apt = geocoded("1", 43.81, -91.24);
        WalkScoreAPI api = new WalkScoreAPI("key", transport, 0, cache);
        api.addWalkScores(List.of(apt));

        assertNull(apt.getWalkScore());
        assertEquals(0, cache.size());
    }

    private String fixtureBody() throws Exception {
        return new String(
                getClass().getResourceAsStream("/fixtures/walkscore_response.json").readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private static Apartment geocoded(String id, double lat, double lng) {
        Apartment apt = new Apartment(id, "https://example.com/" + id);
        apt.setLatitude(lat);
        apt.setLongitude(lng);
        return apt;
    }
}
