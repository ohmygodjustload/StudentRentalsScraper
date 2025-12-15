package utils;

import models.Apartment;
import models.Flag;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class CsvUtils {

	public static void toCsv(List<Apartment> listings, String filePath) {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath))) {
            // Header
            writer.write("URL,Landlord,Bed/Bath,Address,Price,Amenities,DealScore,Flags");
            writer.newLine();
            
            for (Apartment apt : listings) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.1f,\"%s\"",
                        escape(apt.getURL()),
                        escape(apt.getLandlord()),
                        escape(apt.getBedBath()),
                        escape(apt.getAddress()),
                        escape(apt.getPrice()),
                        escape(apt.getAmenities()),
                        apt.getDealScore(),
                        escape(apt.getFlags().stream()
                                    .map(Flag::toString)
                                    .collect(Collectors.joining(";")))
                ));
                writer.newLine();
            }
//            System.out.println("Saved " + listings.size() + " listings to CSV: " + filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error writing CSV: " + e.getMessage(), e);
        }
    }


	// Helper for escaping quotes in CSV
    private static String escape(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }

}