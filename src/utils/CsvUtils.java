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

    // TODO : Implement reading from CSV
    // public static List<Apartment> fromCsv(String filePath) {
    //     List<Apartment> listings = new ArrayList<>();
    //     try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
    //         String line = reader.readLine(); // Skip header
    //         while ((line = reader.readLine()) != null) {
    //             String[] tokens = parseCsvLine(line);
    //             if (tokens.length < 8) continue; // Skip malformed lines
                
                
    //             String[] flagTokens = unescape(tokens[7]).split(";");
    //             Set<Flag> flags = Arrays.stream(flagTokens)
    //                                     .map(String::trim)
    //                                     .filter(s -> !s.isEmpty())
    //                                     .map(Flag::valueOf)
    //                                     .collect(Collectors.toSet());
    //             apt.setFlags(flags);
                
    //             listings.add(apt);
    //         }
    // }


	// Helper for escaping quotes in CSV
    private static String escape(String s) {
        return s == null ? "" : s.replace("\"", "\"\"");
    }

}