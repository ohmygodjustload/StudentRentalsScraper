/**
 * Walk and bike travel times from listings to campus via the TravelTime API.
 *
 * @author Andrew Peirce
 */
package api;

import models.Apartment;
import utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class TravelTimeAPI {

    private static final String BASE_URL = "https://api.traveltimeapp.com/v4/time-filter";

    private final String apiKey;
    private final String apiId;
    private final double campusLat;
    private final double campusLng;
    private final int maxTravelSeconds;
    private final int arrivalHour;
    private final HttpTransport transport;
    private final ResponseCache cache;

    public TravelTimeAPI(
            String apiKey,
            String apiId,
            double campusLat,
            double campusLng,
            int maxTravelSeconds,
            int arrivalHour) {
        this(apiKey, apiId, campusLat, campusLng, maxTravelSeconds, arrivalHour, new JdkHttpTransport(),
                ResponseCache.disabled());
    }

    public TravelTimeAPI(
            String apiKey,
            String apiId,
            double campusLat,
            double campusLng,
            int maxTravelSeconds,
            int arrivalHour,
            HttpTransport transport) {
        this(apiKey, apiId, campusLat, campusLng, maxTravelSeconds, arrivalHour, transport, ResponseCache.disabled());
    }

    public TravelTimeAPI(
            String apiKey,
            String apiId,
            double campusLat,
            double campusLng,
            int maxTravelSeconds,
            int arrivalHour,
            HttpTransport transport,
            ResponseCache cache) {
        this.apiKey = apiKey;
        this.apiId = apiId;
        this.campusLat = campusLat;
        this.campusLng = campusLng;
        this.maxTravelSeconds = maxTravelSeconds;
        this.arrivalHour = arrivalHour;
        this.transport = transport;
        this.cache = cache != null ? cache : ResponseCache.disabled();
    }

    public List<Apartment> addTravelTimes(List<Apartment> apartments) {
        List<Apartment> geocoded = apartments.stream()
                .filter(Apartment::hasCoordinates)
                .toList();
        if (geocoded.isEmpty()) {
            System.out.println("[TravelTime] No geocoded apartments; skipping");
            return apartments;
        }

        Map<String, Apartment> byId = new HashMap<>();
        for (Apartment apt : geocoded) {
            byId.put(apt.getId(), apt);
        }

        applyMode(geocoded, byId, "walking", true);
        applyMode(geocoded, byId, "cycling", false);
        return apartments;
    }

    private void applyMode(
            List<Apartment> geocoded,
            Map<String, Apartment> byId,
            String transportationType,
            boolean walking) {
        cache.resetCounters();
        List<Apartment> misses = new ArrayList<>();
        for (Apartment apt : geocoded) {
            String key = cacheKey(apt, transportationType);
            ResponseCache.Lookup<Integer> lookup = cache.get(key, Integer.class);
            if (lookup.hit()) {
                applySeconds(apt, walking, lookup.value());
            } else {
                misses.add(apt);
            }
        }

        if (!misses.isEmpty()) {
            Request request = generateRequest(misses, transportationType);
            Response response = callApi(request);
            Map<String, Integer> fetched = new HashMap<>();
            if (response != null && response.results != null && !response.results.isEmpty()) {
                Result result = response.results.get(0);
                if (result.locations != null) {
                    for (ResponseLocation loc : result.locations) {
                        if (loc.properties != null && !loc.properties.isEmpty()) {
                            fetched.put(loc.id, loc.properties.get(0).travel_time);
                        }
                    }
                }
            }
            for (Apartment apt : misses) {
                Integer seconds = fetched.get(apt.getId());
                if (seconds != null) {
                    applySeconds(apt, walking, seconds);
                    cache.put(cacheKey(apt, transportationType), seconds);
                } else {
                    // Missing from response or listed unreachable — cache null so we don't re-query.
                    cache.put(cacheKey(apt, transportationType), null);
                }
            }
        }

        cache.save();
        System.out.println("[TravelTime] " + transportationType + ": "
                + cache.hits() + " cached, " + cache.misses() + " fetched");
    }

    private void applySeconds(Apartment apt, boolean walking, Integer seconds) {
        if (seconds == null) {
            return;
        }
        if (walking) {
            apt.setWalkTravelTimeSeconds(seconds);
        } else {
            apt.setBikeTravelTimeSeconds(seconds);
        }
    }

    String cacheKey(Apartment apt, String mode) {
        return apt.getId()
                + "|" + ResponseCache.formatCoord(apt.getLatitude())
                + "|" + ResponseCache.formatCoord(apt.getLongitude())
                + "|" + mode
                + "|" + ResponseCache.formatCoord(campusLat)
                + "|" + ResponseCache.formatCoord(campusLng)
                + "|" + maxTravelSeconds
                + "|" + arrivalHour;
    }

    Request generateRequest(List<Apartment> apartments, String transportationType) {
        Request request = new Request();
        request.locations = new ArrayList<>();
        request.locations.add(new Location("campus", campusLat, campusLng));

        List<String> departureIds = new ArrayList<>();
        for (Apartment apt : apartments) {
            request.locations.add(new Location(apt.getId(), apt.getLatitude(), apt.getLongitude()));
            departureIds.add(apt.getId());
        }

        ArrivalSearch search = new ArrivalSearch();
        search.id = transportationType + "-to-campus";
        search.arrival_location_id = "campus";
        search.departure_location_ids = departureIds;
        search.arrival_time = ZonedDateTime.now(ZoneId.of("America/Chicago"))
                .plusDays(1)
                .withHour(arrivalHour).withMinute(0).withSecond(0).withNano(0)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        search.travel_time = maxTravelSeconds;
        search.properties = List.of("travel_time");
        search.transportation = new Transportation(transportationType);

        request.arrival_searches = List.of(search);
        return request;
    }

    private Response callApi(Request request) {
        String jsonBody = JsonUtils.toJsonString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("X-Application-Id", apiId)
                .header("X-Api-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
            HttpResponse<String> response = transport.send(httpRequest);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                        "TravelTime API failed: HTTP " + response.statusCode() + " " + response.body(),
                        response.statusCode());
            }
            return JsonUtils.fromJsonString(response.body(), Response.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("TravelTime request interrupted", e);
        } catch (IOException e) {
            throw new ApiException("TravelTime request failed", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Request {
        public List<Location> locations;
        public List<ArrivalSearch> arrival_searches;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        public List<Result> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        public String search_id;
        public List<ResponseLocation> locations;
        public List<String> unreachable;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResponseLocation {
        public String id;
        public List<Property> properties;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Property {
        public int travel_time;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        public String id;
        public Coordinate coords;

        public Location() {
        }

        public Location(String id, double lat, double lng) {
            this.id = id;
            this.coords = new Coordinate(lat, lng);
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Coordinate {
            public double lat;
            public double lng;

            public Coordinate() {
            }

            public Coordinate(double lat, double lng) {
                this.lat = lat;
                this.lng = lng;
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ArrivalSearch {
        public String id;
        public List<String> departure_location_ids;
        public String arrival_location_id;
        public String arrival_time;
        public int travel_time;
        public List<String> properties;
        public Transportation transportation;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Transportation {
        public String type;

        public Transportation() {
        }

        public Transportation(String type) {
            this.type = type;
        }
    }
}
