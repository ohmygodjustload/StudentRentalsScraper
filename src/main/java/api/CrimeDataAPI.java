/**
 * Fetches agency-level FBI Crime Data Explorer stats and writes a standalone report.
 * Does not attach crime data to individual listings.
 *
 * @author Andrew Peirce
 */
package api;

import utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class CrimeDataAPI {

    private static final String BASE_URL = "https://api.usa.gov/crime/fbi/cde";

    private final String apiKey;
    private final String ori;
    private final HttpTransport transport;

    public CrimeDataAPI(String apiKey, String ori) {
        this(apiKey, ori, new JdkHttpTransport());
    }

    public CrimeDataAPI(String apiKey, String ori, HttpTransport transport) {
        this.apiKey = apiKey;
        this.ori = ori;
        this.transport = transport;
    }

    /**
     * Fetches violent (V) and property (P) crime summaries for the last 12 months
     * and writes a JSON report to {@code outputPath}.
     *
     * @return the report object that was written
     */
    public CrimeReport fetchAndWriteReport(Path outputPath) {
        YearMonth end = YearMonth.now(ZoneId.of("America/Chicago")).minusMonths(1);
        YearMonth start = end.minusMonths(11);
        String from = String.format("%02d-%d", start.getMonthValue(), start.getYear());
        String to = String.format("%02d-%d", end.getMonthValue(), end.getYear());

        JsonNode violent = fetchOffense("V", from, to);
        JsonNode property = fetchOffense("P", from, to);

        CrimeReport report = new CrimeReport();
        report.ori = ori;
        report.from = from;
        report.to = to;
        report.fetchedAt = java.time.Instant.now().toString();
        report.violent = violent;
        report.property = property;
        report.totals = summarizeTotals(violent, property);

        JsonUtils.toJson(report, outputPath.toString());
        return report;
    }

    private JsonNode fetchOffense(String offenseCode, String from, String to) {
        String url = String.format(
                "%s/summarized/agency/%s/%s?from=%s&to=%s&type=counts&API_KEY=%s",
                BASE_URL,
                URLEncoder.encode(ori, StandardCharsets.UTF_8),
                URLEncoder.encode(offenseCode, StandardCharsets.UTF_8),
                URLEncoder.encode(from, StandardCharsets.UTF_8),
                URLEncoder.encode(to, StandardCharsets.UTF_8),
                URLEncoder.encode(apiKey, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = transport.send(request);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                        "CrimeData API failed for offense " + offenseCode
                                + ": HTTP " + response.statusCode() + " " + response.body(),
                        response.statusCode());
            }
            return JsonUtils.fromJsonString(response.body(), JsonNode.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("CrimeData request interrupted", e);
        } catch (IOException e) {
            throw new ApiException("CrimeData request failed", e);
        }
    }

    private Map<String, Object> summarizeTotals(JsonNode violent, JsonNode property) {
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("violentActualsSum", sumActuals(violent));
        totals.put("propertyActualsSum", sumActuals(property));
        return totals;
    }

    private static Integer sumActuals(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode actuals = root.at("/offenses/actuals");
        if (actuals.isMissingNode() || !actuals.isObject()) {
            // Some responses nest under a single offense key
            actuals = root.path("offenses");
            if (actuals.isObject() && actuals.size() == 1) {
                actuals = actuals.elements().next().path("actuals");
            }
        }
        if (!actuals.isObject()) {
            return null;
        }
        int sum = 0;
        boolean any = false;
        for (JsonNode value : actuals) {
            if (value.isNumber()) {
                sum += value.asInt();
                any = true;
            } else if (value.isObject()) {
                for (JsonNode monthVal : value) {
                    if (monthVal.isNumber()) {
                        sum += monthVal.asInt();
                        any = true;
                    }
                }
            }
        }
        return any ? sum : null;
    }

    public static class CrimeReport {
        public String ori;
        public String from;
        public String to;
        public String fetchedAt;
        public JsonNode violent;
        public JsonNode property;
        public Map<String, Object> totals;
    }
}
