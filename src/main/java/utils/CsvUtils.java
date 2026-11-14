/**
 * CSV export helpers for apartment listings.
 *
 * @author Andrew Peirce
 */
package utils;

import models.Apartment;
import models.Flag;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

import com.opencsv.CSVWriter;

public final class CsvUtils {

    private CsvUtils() {
    }

    private static final String[] HEADER = {
            "id", "url", "landlord", "bed_bath", "bedrooms", "bathrooms",
            "address", "price", "rentMonthly", "pricePerPerson", "available", "amenities", "flags",
            "latitude", "longitude", "distanceToCampus",
            "walkTravelTimeSeconds", "bikeTravelTimeSeconds",
            "walkScore", "bikeScore"
    };

    public static void toCsv(List<Apartment> listings, String filePath) {
        try (Writer writer = new FileWriter(filePath);
             CSVWriter csv = new CSVWriter(writer)) {
            csv.writeNext(HEADER);
            for (Apartment apt : listings) {
                csv.writeNext(new String[] {
                        nullToEmpty(apt.getId()),
                        nullToEmpty(apt.getUrl()),
                        nullToEmpty(apt.getLandlord()),
                        nullToEmpty(apt.getBedBath()),
                        numberToString(apt.getBedrooms()),
                        numberToString(apt.getBathrooms()),
                        nullToEmpty(apt.getAddress()),
                        nullToEmpty(apt.getPrice()),
                        numberToString(apt.getRentMonthly()),
                        numberToString(apt.getPricePerPerson()),
                        nullToEmpty(apt.getAvailable()),
                        nullToEmpty(apt.getAmenities()),
                        apt.getFlags().stream().map(Flag::toString).collect(Collectors.joining(";")),
                        numberToString(apt.getLatitude()),
                        numberToString(apt.getLongitude()),
                        numberToString(apt.getDistanceToCampus()),
                        numberToString(apt.getWalkTravelTimeSeconds()),
                        numberToString(apt.getBikeTravelTimeSeconds()),
                        numberToString(apt.getWalkScore()),
                        numberToString(apt.getBikeScore())
                });
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing CSV: " + e.getMessage(), e);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String numberToString(Number value) {
        return value == null ? "" : value.toString();
    }
}
