/**
 * Merges Geocodio CSV export columns into apartment listings by listing URL.
 *
 * @author Andrew Peirce
 */
package geocoding;

import models.Apartment;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class DataMerger {

    public List<Apartment> mergeGeocodioData(List<Apartment> apartments, String csvFile) throws IOException {
        Map<String, Apartment> urlMap = new HashMap<>();
        for (Apartment apt : apartments) {
            urlMap.put(apt.getUrl(), apt);
        }
        System.out.println("[Geocoder] Processing " + urlMap.size() + " apartments against CSV");

        try (Reader reader = new FileReader(csvFile);
             CSVReader csv = new CSVReader(reader)) {
            String[] headers = csv.readNext();
            if (headers == null) {
                throw new IOException("CSV is an empty file");
            }
            for (int i = 0; i < headers.length; i++) {
                headers[i] = stripQuotes(headers[i]);
            }

            int urlIndex = indexOf(headers, "URL", "url", "Url");
            if (urlIndex < 0) {
                urlIndex = 0;
            }
            int latIndex = indexOf(headers, "Geocodio Latitude");
            int lngIndex = indexOf(headers, "Geocodio Longitude");
            int accuracyIndex = indexOf(headers, "Geocodio Accuracy Score");
            int addressIndex = indexOf(headers, "Geocodio Address Line 1");

            String[] values;
            while ((values = csv.readNext()) != null) {
                String url = cell(values, urlIndex);
                Apartment apt = urlMap.get(url);
                if (apt == null) {
                    continue;
                }
                setDouble(apt, cell(values, latIndex), apt::setLatitude);
                setDouble(apt, cell(values, lngIndex), apt::setLongitude);
                setDouble(apt, cell(values, accuracyIndex), apt::setAccuracy);
                String address = cell(values, addressIndex);
                if (!address.isBlank()) {
                    apt.setGeocodioAddress(address);
                }
            }
        } catch (CsvValidationException e) {
            throw new IOException("Invalid Geocodio CSV: " + e.getMessage(), e);
        }

        return apartments;
    }

    private static int indexOf(String[] headers, String... candidates) {
        for (String candidate : candidates) {
            for (int i = 0; i < headers.length; i++) {
                if (headers[i] != null && headers[i].equalsIgnoreCase(candidate)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String cell(String[] values, int index) {
        if (index < 0 || index >= values.length || values[index] == null) {
            return "";
        }
        return stripQuotes(values[index]).strip();
    }

    private static String stripQuotes(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("^\"|\"$", "");
    }

    private static void setDouble(Apartment apt, String value, java.util.function.Consumer<Double> setter) {
        if (value == null || value.isBlank() || "N/A".equalsIgnoreCase(value)) {
            return;
        }
        try {
            setter.accept(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            System.err.println("[Geocoder] Bad number '" + value + "' for apartment " + apt.getId());
        }
    }
}
