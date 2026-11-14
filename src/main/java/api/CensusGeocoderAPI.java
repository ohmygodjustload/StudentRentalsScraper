/**
 * Batch geocoding via the free US Census Bureau Geocoder (no API key required).
 *
 * @author Andrew Peirce
 */
package api;

import models.Apartment;
import models.Flag;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class CensusGeocoderAPI {

    private static final String BATCH_URL =
            "https://geocoding.geo.census.gov/geocoder/locations/addressbatch";
    private static final String BENCHMARK = "Public_AR_Current";
    private static final int BATCH_SIZE = 10000;

    private static final List<String> KNOWN_CITIES = List.of(
            "La Crosse",
            "La Crescent",
            "Onalaska",
            "Holmen",
            "West Salem");

    private static final Pattern STATE_SUFFIX = Pattern.compile(",?\\s*(WI|MN)\\s*$", Pattern.CASE_INSENSITIVE);

    private final HttpTransport transport;

    public CensusGeocoderAPI() {
        this(new JdkHttpTransport());
    }

    public CensusGeocoderAPI(HttpTransport transport) {
        this.transport = transport;
    }

    public List<Apartment> geocode(List<Apartment> apartments) {
        List<Apartment> needsGeocode = apartments.stream()
                .filter(a -> a.getAddress() != null && !a.getAddress().isBlank())
                .toList();
        if (needsGeocode.isEmpty()) {
            System.out.println("[Geocoder] Census: no addresses to geocode");
            return apartments;
        }

        Map<String, Apartment> byId = new HashMap<>();
        for (Apartment apt : needsGeocode) {
            byId.put(apt.getId(), apt);
        }

        for (int start = 0; start < needsGeocode.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, needsGeocode.size());
            applyBatch(needsGeocode.subList(start, end), byId);
        }
        return apartments;
    }

    private void applyBatch(List<Apartment> batch, Map<String, Apartment> byId) {
        String csv = buildRequestCsv(batch);
        String boundary = "----CensusBoundary" + System.currentTimeMillis();
        byte[] body = buildMultipartBody(boundary, csv);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BATCH_URL))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Accept", "text/plain, text/csv, */*")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        try {
            HttpResponse<String> response = transport.send(request);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                        "Census geocoder failed: HTTP " + response.statusCode() + " " + response.body(),
                        response.statusCode());
            }
            parseResponse(response.body(), byId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Census geocoder request interrupted", e);
        } catch (IOException e) {
            throw new ApiException("Census geocoder request failed", e);
        }
    }

    String buildRequestCsv(List<Apartment> batch) {
        StringBuilder sb = new StringBuilder();
        for (Apartment apt : batch) {
            AddressParts parts = splitAddress(apt.getAddress());
            sb.append(escapeCsv(apt.getId())).append(',')
                    .append(escapeCsv(parts.street())).append(',')
                    .append(escapeCsv(parts.city())).append(',')
                    .append(escapeCsv(parts.state())).append(',')
                    .append(escapeCsv(parts.zip()))
                    .append('\n');
        }
        return sb.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private byte[] buildMultipartBody(String boundary, String csvContent) {
        String filePart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"addressFile\"; filename=\"addresses.csv\"\r\n"
                + "Content-Type: text/csv\r\n\r\n"
                + csvContent
                + "\r\n";
        String benchmarkPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"benchmark\"\r\n\r\n"
                + BENCHMARK
                + "\r\n";
        String end = "--" + boundary + "--\r\n";
        return (filePart + benchmarkPart + end).getBytes(StandardCharsets.UTF_8);
    }

    void parseResponse(String body, Map<String, Apartment> byId) {
        if (body == null || body.isBlank()) {
            return;
        }
        try (CSVReader reader = new CSVReader(new StringReader(body))) {
            String[] row;
            while ((row = reader.readNext()) != null) {
                if (row.length < 3) {
                    continue;
                }
                String id = strip(row[0]);
                Apartment apt = byId.get(id);
                if (apt == null) {
                    continue;
                }
                String matchIndicator = strip(row[2]);
                String matchType = row.length > 3 ? strip(row[3]) : "";
                String matchedAddress = row.length > 4 ? strip(row[4]) : "";
                String coords = row.length > 5 ? strip(row[5]) : "";

                if ("Match".equalsIgnoreCase(matchIndicator)) {
                    applyCoordinates(apt, coords);
                    if (!matchedAddress.isBlank()) {
                        apt.setGeocodedAddress(matchedAddress);
                    }
                    if ("Non_Exact".equalsIgnoreCase(matchType)) {
                        apt.addFlag(Flag.GEOCODE_APPROXIMATE);
                    }
                } else {
                    apt.addFlag(Flag.GEOCODE_NO_MATCH);
                }
            }
        } catch (IOException | CsvValidationException e) {
            throw new ApiException("Failed to parse Census geocoder response", e);
        }
    }

    private static void applyCoordinates(Apartment apt, String coords) {
        if (coords == null || coords.isBlank()) {
            return;
        }
        String[] parts = coords.split(",");
        if (parts.length != 2) {
            return;
        }
        try {
            // Census returns "longitude,latitude"
            double lng = Double.parseDouble(parts[0].strip());
            double lat = Double.parseDouble(parts[1].strip());
            apt.setLongitude(lng);
            apt.setLatitude(lat);
        } catch (NumberFormatException ignored) {
            // leave coordinates unset
        }
    }

    /**
     * Splits a cleaned address into Census batch fields.
     */
    AddressParts splitAddress(String raw) {
        if (raw == null || raw.isBlank()) {
            return new AddressParts("", "", "", "");
        }
        String address = raw.strip().replaceAll("\\s+", " ");
        String state = "";
        Matcher stateMatcher = STATE_SUFFIX.matcher(address);
        if (stateMatcher.find()) {
            state = stateMatcher.group(1).toUpperCase(Locale.ROOT);
            address = address.substring(0, stateMatcher.start()).strip().replaceAll(",$", "").strip();
        }

        String city = "";
        String street = address;
        String lower = address.toLowerCase(Locale.ROOT);
        for (String known : KNOWN_CITIES) {
            String needle = known.toLowerCase(Locale.ROOT);
            if (lower.endsWith(needle)) {
                city = known;
                street = address.substring(0, address.length() - known.length()).strip().replaceAll(",$", "").strip();
                break;
            }
            int idx = lower.lastIndexOf(" " + needle);
            if (idx >= 0) {
                city = known;
                street = address.substring(0, idx).strip().replaceAll(",$", "").strip();
                break;
            }
        }

        return new AddressParts(street, city, state, "");
    }

    private static String strip(String value) {
        return value == null ? "" : value.strip();
    }

    public record AddressParts(String street, String city, String state, String zip) {
    }
}
