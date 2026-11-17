/**
 * Walk Score and Bike Score enrichment via the Walk Score API.
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

public class WalkScoreAPI {

    private static final String BASE_URL = "https://api.walkscore.com/score";
    private static final long DELAY_MS = 200;

    private final String apiKey;
    private final HttpTransport transport;
    private final long delayMs;
    private final ResponseCache cache;

    public WalkScoreAPI(String apiKey) {
        this(apiKey, new JdkHttpTransport(), DELAY_MS, ResponseCache.disabled());
    }

    public WalkScoreAPI(String apiKey, HttpTransport transport) {
        this(apiKey, transport, DELAY_MS, ResponseCache.disabled());
    }

    public WalkScoreAPI(String apiKey, HttpTransport transport, long delayMs) {
        this(apiKey, transport, delayMs, ResponseCache.disabled());
    }

    public WalkScoreAPI(String apiKey, HttpTransport transport, ResponseCache cache) {
        this(apiKey, transport, DELAY_MS, cache);
    }

    public WalkScoreAPI(String apiKey, HttpTransport transport, long delayMs, ResponseCache cache) {
        this.apiKey = apiKey;
        this.transport = transport;
        this.delayMs = delayMs;
        this.cache = cache != null ? cache : ResponseCache.disabled();
    }

    public List<Apartment> addWalkScores(List<Apartment> apartments) {
        int enriched = 0;
        cache.resetCounters();
        try {
            for (Apartment apt : apartments) {
                if (!apt.hasCoordinates()) {
                    continue;
                }
                try {
                    if (enrichOne(apt)) {
                        enriched++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ApiException("WalkScore enrichment interrupted", e);
                } catch (ApiException e) {
                    System.err.println("[WalkScore] Skipping listing " + apt.getId() + ": " + e.getMessage());
                }
            }
        } finally {
            cache.save();
        }
        System.out.println("[WalkScore] Enriched " + enriched + " listings ("
                + cache.hits() + " cached, " + cache.misses() + " fetched)");
        return apartments;
    }

    /**
     * @return true if scores were applied from cache or a successful HTTP response
     */
    private boolean enrichOne(Apartment apt) throws InterruptedException {
        String key = cacheKey(apt);
        ResponseCache.Lookup<String> lookup = cache.get(key, String.class);
        if (lookup.hit()) {
            applyBody(apt, lookup.value());
            return true;
        }

        String address = apt.getGeocodedAddress() != null ? apt.getGeocodedAddress() : apt.getAddress();
        if (address == null) {
            address = "";
        }
        String query = String.format(
                "?format=json&lat=%s&lon=%s&address=%s&bike=1&wsapikey=%s",
                apt.getLatitude(),
                apt.getLongitude(),
                URLEncoder.encode(address, StandardCharsets.UTF_8),
                URLEncoder.encode(apiKey, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + query))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = transport.send(request);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                        "WalkScore HTTP " + response.statusCode() + ": " + response.body(),
                        response.statusCode());
            }
            String body = response.body();
            ScoreResponse parsed = JsonUtils.fromJsonString(body, ScoreResponse.class);
            if (parsed.status != null && parsed.status != 1) {
                throw new ApiException("WalkScore status=" + parsed.status + " for listing " + apt.getId());
            }
            applyParsed(apt, parsed);
            if (parsed.status != null && parsed.status == 1) {
                cache.put(key, body);
            }
            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (IOException e) {
            throw new ApiException("WalkScore request failed for listing " + apt.getId(), e);
        }
    }

    private void applyBody(Apartment apt, String body) {
        if (body == null || body.isBlank()) {
            return;
        }
        ScoreResponse parsed = JsonUtils.fromJsonString(body, ScoreResponse.class);
        applyParsed(apt, parsed);
    }

    private void applyParsed(Apartment apt, ScoreResponse parsed) {
        if (parsed.walkscore != null) {
            apt.setWalkScore(parsed.walkscore);
        }
        if (parsed.description != null) {
            apt.setWalkScoreDescription(parsed.description);
        }
        if (parsed.bike != null) {
            if (parsed.bike.has("score") && !parsed.bike.get("score").isNull()) {
                apt.setBikeScore(parsed.bike.get("score").asInt());
            }
            if (parsed.bike.has("description") && !parsed.bike.get("description").isNull()) {
                apt.setBikeScoreDescription(parsed.bike.get("description").asText());
            }
        }
    }

    String cacheKey(Apartment apt) {
        return apt.getId()
                + "|" + ResponseCache.formatCoord(apt.getLatitude())
                + "|" + ResponseCache.formatCoord(apt.getLongitude());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScoreResponse {
        public Integer status;
        public Integer walkscore;
        public String description;
        public JsonNode bike;
    }
}
