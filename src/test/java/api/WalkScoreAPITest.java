package api;

import models.Apartment;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WalkScoreAPITest {

    @Test
    void addWalkScores_sets_scores() throws Exception {
        String body = new String(
                getClass().getResourceAsStream("/fixtures/walkscore_response.json").readAllBytes(),
                StandardCharsets.UTF_8);
        FakeHttpTransport transport = new FakeHttpTransport().enqueue(200, body);

        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setAddress("100 Main St");
        apt.setLatitude(43.81);
        apt.setLongitude(-91.24);

        WalkScoreAPI api = new WalkScoreAPI("key", transport, 0);
        api.addWalkScores(List.of(apt));

        assertEquals(78, apt.getWalkScore());
        assertEquals("Very Walkable", apt.getWalkScoreDescription());
        assertEquals(65, apt.getBikeScore());
        assertEquals("Bikeable", apt.getBikeScoreDescription());
    }
}
