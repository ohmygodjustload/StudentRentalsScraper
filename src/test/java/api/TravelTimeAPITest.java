package api;

import models.Apartment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TravelTimeAPITest {

    @TempDir
    Path tempDir;

    @Test
    void addTravelTimes_sets_walk_and_bike() throws Exception {
        String body = fixtureBody();
        FakeHttpTransport transport = new FakeHttpTransport()
                .enqueue(200, body)
                .enqueue(200, body.replace("720", "300"));

        Apartment apt = geocoded("101", 43.81, -91.24);

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

    @Test
    void fully_cached_makes_no_http_requests() {
        Path cacheFile = tempDir.resolve("traveltime.json");
        ResponseCache cache = ResponseCache.load(cacheFile, false);

        Apartment apt = geocoded("101", 43.81, -91.24);
        TravelTimeAPI keyApi = new TravelTimeAPI(
                "key", "id", 43.8161, -91.2311, 900, 8, new FakeHttpTransport(), cache);
        cache.put(keyApi.cacheKey(apt, "walking"), 720);
        cache.put(keyApi.cacheKey(apt, "cycling"), 300);
        cache.save();

        FakeHttpTransport transport = new FakeHttpTransport();
        ResponseCache loaded = ResponseCache.load(cacheFile, false);
        TravelTimeAPI api = new TravelTimeAPI(
                "key", "id", 43.8161, -91.2311, 900, 8, transport, loaded);
        api.addTravelTimes(List.of(apt));

        assertTrue(transport.getRequests().isEmpty());
        assertEquals(720, apt.getWalkTravelTimeSeconds());
        assertEquals(300, apt.getBikeTravelTimeSeconds());
    }

    @Test
    void partial_hit_requests_only_missed_ids() throws Exception {
        String body = """
                {
                  "results": [{
                    "search_id": "walking-to-campus",
                    "locations": [{"id": "102", "properties": [{"travel_time": 600}]}],
                    "unreachable": []
                  }]
                }
                """;
        FakeHttpTransport transport = new FakeHttpTransport()
                .enqueue(200, body)
                .enqueue(200, body.replace("600", "240"));

        Path cacheFile = tempDir.resolve("traveltime.json");
        ResponseCache cache = ResponseCache.load(cacheFile, false);
        Apartment cached = geocoded("101", 43.81, -91.24);
        Apartment missed = geocoded("102", 43.82, -91.25);

        TravelTimeAPI api = new TravelTimeAPI(
                "key", "id", 43.8161, -91.2311, 900, 8, transport, cache);
        cache.put(api.cacheKey(cached, "walking"), 720);
        cache.put(api.cacheKey(cached, "cycling"), 300);

        api.addTravelTimes(List.of(cached, missed));

        assertEquals(2, transport.getRequests().size());
        String walkRequestBody = readBody(transport.getRequests().get(0));
        assertTrue(walkRequestBody.contains("\"102\""));
        assertFalse(walkRequestBody.contains("\"101\""));

        TravelTimeAPI.Request onlyMiss = api.generateRequest(List.of(missed), "walking");
        assertEquals(List.of("102"), onlyMiss.arrival_searches.get(0).departure_location_ids);

        assertEquals(720, cached.getWalkTravelTimeSeconds());
        assertEquals(600, missed.getWalkTravelTimeSeconds());
        assertEquals(300, cached.getBikeTravelTimeSeconds());
        assertEquals(240, missed.getBikeTravelTimeSeconds());
    }

    @Test
    void unreachable_listing_is_cached_and_not_requeried() {
        String body = """
                {
                  "results": [{
                    "search_id": "walking-to-campus",
                    "locations": [],
                    "unreachable": ["101"]
                  }]
                }
                """;
        FakeHttpTransport transport = new FakeHttpTransport()
                .enqueue(200, body)
                .enqueue(200, body.replace("walking", "cycling"));

        Path cacheFile = tempDir.resolve("traveltime.json");
        ResponseCache cache = ResponseCache.load(cacheFile, false);
        Apartment apt = geocoded("101", 43.81, -91.24);

        TravelTimeAPI api = new TravelTimeAPI(
                "key", "id", 43.8161, -91.2311, 900, 8, transport, cache);
        api.addTravelTimes(List.of(apt));

        assertNull(apt.getWalkTravelTimeSeconds());
        assertNull(apt.getBikeTravelTimeSeconds());
        assertEquals(2, transport.getRequests().size());

        FakeHttpTransport transport2 = new FakeHttpTransport();
        ResponseCache reloaded = ResponseCache.load(cacheFile, false);
        TravelTimeAPI api2 = new TravelTimeAPI(
                "key", "id", 43.8161, -91.2311, 900, 8, transport2, reloaded);
        Apartment apt2 = geocoded("101", 43.81, -91.24);
        api2.addTravelTimes(List.of(apt2));

        assertTrue(transport2.getRequests().isEmpty());
        assertNull(apt2.getWalkTravelTimeSeconds());
    }

    @Test
    void changing_maxSeconds_causes_miss() throws Exception {
        Path cacheFile = tempDir.resolve("traveltime.json");
        ResponseCache cache = ResponseCache.load(cacheFile, false);
        Apartment apt = geocoded("101", 43.81, -91.24);

        TravelTimeAPI api900 = new TravelTimeAPI(
                "key", "id", 43.8161, -91.2311, 900, 8, new FakeHttpTransport(), cache);
        cache.put(api900.cacheKey(apt, "walking"), 720);
        cache.put(api900.cacheKey(apt, "cycling"), 300);
        cache.save();

        String body = fixtureBody();
        FakeHttpTransport transport = new FakeHttpTransport()
                .enqueue(200, body)
                .enqueue(200, body.replace("720", "280"));
        ResponseCache loaded = ResponseCache.load(cacheFile, false);
        TravelTimeAPI api1800 = new TravelTimeAPI(
                "key", "id", 43.8161, -91.2311, 1800, 8, transport, loaded);
        api1800.addTravelTimes(List.of(apt));

        assertEquals(2, transport.getRequests().size());
        assertEquals(720, apt.getWalkTravelTimeSeconds());
        assertEquals(280, apt.getBikeTravelTimeSeconds());
    }

    private String fixtureBody() throws Exception {
        return new String(
                getClass().getResourceAsStream("/fixtures/traveltime_response.json").readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private static Apartment geocoded(String id, double lat, double lng) {
        Apartment apt = new Apartment(id, "https://example.com/" + id);
        apt.setLatitude(lat);
        apt.setLongitude(lng);
        return apt;
    }

    private static String readBody(HttpRequest request) throws Exception {
        var publisher = request.bodyPublisher().orElseThrow();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CountDownLatch done = new CountDownLatch(1);
        publisher.subscribe(new Flow.Subscriber<>() {
            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                out.writeBytes(bytes);
            }

            @Override
            public void onError(Throwable throwable) {
                done.countDown();
            }

            @Override
            public void onComplete() {
                done.countDown();
            }
        });
        assertTrue(done.await(2, TimeUnit.SECONDS));
        return out.toString(StandardCharsets.UTF_8);
    }
}
