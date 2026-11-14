/**
 * Batch geocoding via the Geocodio API.
 *
 * @author Andrew Peirce
 */
package api;

import models.Apartment;
import utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

public class GeocodioAPI {

    private static final String BASE_URL = "https://api.geocod.io/v1.7/geocode";
    private static final int BATCH_SIZE = 100;

    private final String apiKey;
    private final HttpTransport transport;

    public GeocodioAPI(String apiKey) {
        this(apiKey, new JdkHttpTransport());
    }

    public GeocodioAPI(String apiKey, HttpTransport transport) {
        this.apiKey = apiKey;
        this.transport = transport;
    }

    public List<Apartment> geocode(List<Apartment> apartments) {
        List<Apartment> needsGeocode = apartments.stream()
                .filter(a -> a.getAddress() != null && !a.getAddress().isBlank())
                .toList();

        for (int start = 0; start < needsGeocode.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, needsGeocode.size());
            List<Apartment> batch = needsGeocode.subList(start, end);
            applyBatch(batch);
        }
        return apartments;
    }

    private void applyBatch(List<Apartment> batch) {
        List<String> addresses = batch.stream().map(Apartment::getAddress).toList();
        String body = JsonUtils.toJsonString(addresses);
        String url = BASE_URL + "?api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = transport.send(request);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                        "Geocodio batch failed: HTTP " + response.statusCode() + " " + response.body(),
                        response.statusCode());
            }
            BatchResponse parsed = JsonUtils.fromJsonString(response.body(), BatchResponse.class);
            if (parsed.results == null) {
                return;
            }
            for (int i = 0; i < batch.size() && i < parsed.results.size(); i++) {
                applyResult(batch.get(i), parsed.results.get(i));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Geocodio request interrupted", e);
        } catch (IOException e) {
            throw new ApiException("Geocodio request failed", e);
        }
    }

    private void applyResult(Apartment apt, BatchItem item) {
        if (item == null || item.response == null || item.response.results == null
                || item.response.results.isEmpty()) {
            return;
        }
        GeocodeResult best = item.response.results.get(0);
        if (best.location != null) {
            apt.setLatitude(best.location.lat);
            apt.setLongitude(best.location.lng);
        }
        apt.setAccuracy(best.accuracy);
        if (best.formatted_address != null) {
            apt.setGeocodioAddress(best.formatted_address);
        } else if (best.address_components != null) {
            apt.setGeocodioAddress(best.address_components.path("formatted_street").asText(null));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BatchResponse {
        public List<BatchItem> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BatchItem {
        public String query;
        public SingleResponse response;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SingleResponse {
        public List<GeocodeResult> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeocodeResult {
        public Double accuracy;
        public String formatted_address;
        public Location location;
        public JsonNode address_components;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        public Double lat;
        public Double lng;
    }
}
