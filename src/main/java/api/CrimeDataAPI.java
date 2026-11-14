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
    private static final int MONTHS_REQUESTED = 12;

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
        YearMonth start = end.minusMonths(MONTHS_REQUESTED - 1);
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

        Integer monthsWithData = (Integer) report.totals.get("monthsWithData");
        if (monthsWithData != null && monthsWithData < MONTHS_REQUESTED) {
            System.out.println("[CrimeData] Warning: only " + monthsWithData + "/"
                    + MONTHS_REQUESTED + " months have agency data"
                    + " (FBI reporting lag; max_data_date="
                    + report.totals.get("maxDataDate") + ")");
        }

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
        SeriesSum violentSum = sumSeries(violent);
        SeriesSum propertySum = sumSeries(property);

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("violentOffenses", violentSum.offenses);
        totals.put("violentClearances", violentSum.clearances);
        totals.put("propertyOffenses", propertySum.offenses);
        totals.put("propertyClearances", propertySum.clearances);
        totals.put("monthsWithData", Math.max(violentSum.monthsWithData, propertySum.monthsWithData));
        totals.put("monthsRequested", MONTHS_REQUESTED);
        totals.put("maxDataDate", firstNonNull(
                extractMaxDataDate(violent),
                extractMaxDataDate(property)));
        return totals;
    }

    static SeriesSum sumSeries(JsonNode root) {
        SeriesSum sum = new SeriesSum();
        if (root == null) {
            return sum;
        }
        JsonNode actuals = root.at("/offenses/actuals");
        if (actuals.isMissingNode() || !actuals.isObject()) {
            return sum;
        }
        int offenseMonths = 0;
        int clearanceMonths = 0;
        for (var entry = actuals.fields(); entry.hasNext(); ) {
            var field = entry.next();
            String name = field.getKey().toLowerCase();
            JsonNode months = field.getValue();
            if (!months.isObject()) {
                continue;
            }
            boolean isClearances = name.contains("clearance");
            int seriesMonths = 0;
            for (JsonNode monthVal : months) {
                if (!monthVal.isNumber()) {
                    continue;
                }
                int value = monthVal.asInt();
                if (isClearances) {
                    sum.clearances += value;
                } else {
                    sum.offenses += value;
                }
                seriesMonths++;
            }
            if (isClearances) {
                clearanceMonths = Math.max(clearanceMonths, seriesMonths);
            } else {
                offenseMonths = Math.max(offenseMonths, seriesMonths);
            }
        }
        sum.monthsWithData = offenseMonths > 0 ? offenseMonths : clearanceMonths;
        return sum;
    }

    private static String extractMaxDataDate(JsonNode root) {
        if (root == null) {
            return null;
        }
        JsonNode date = root.at("/cde_properties/max_data_date/UCR");
        if (date.isMissingNode() || date.isNull()) {
            return null;
        }
        return date.asText();
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    static final class SeriesSum {
        int offenses;
        int clearances;
        int monthsWithData;
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
