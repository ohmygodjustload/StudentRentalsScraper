/**
 * A class designed to enhance the baseline JSON after scraping. After being cleaned, it goes into this
 * class to enhance it with coordinates and other data from Geocod.io. Creates a new JSON with this
 * enhanced data, ready to go on to the next step in the pipeline.
 * 
 * @author Andrew Peirce
 * 
 * Date Last Modified: 08/13/2025
 */
package geocoding;

import models.Apartment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataMerger {
	
	public List<Apartment> mergeGeocodioData(List<Apartment> apartments, String csvFile) throws IOException {
		// Create URL-to-apartment map
		Map<String, Apartment> urlMap = new HashMap<>();
		for (Apartment apt : apartments) {
			urlMap.put(apt.getURL(), apt);
		}
		System.out.println("Processing " + urlMap.size() + " apartments");
		
		// Process enhanced CSV
		List<String> csvLines = Files.readAllLines(Path.of(csvFile));
		if (csvLines.isEmpty()) {
			throw new IOException("CSV is an empty file");
		}
		
		String[] headers = parseCsvLine(csvLines.get(0));
		
		// Clean header strings to remove quotations
	    for (int i = 0; i < headers.length; i++) {
	        headers[i] = cleanCsvCell(headers[i]);  
	    }
		
		for (int i = 1; i < csvLines.size(); i++) {
	        String[] values = parseCsvLine(csvLines.get(i));
	        String url = cleanCsvCell(safeExtract(values, 0));
	        Apartment apt = urlMap.get(url);
	        
	        if (apt != null) {
	            setDouble(apt, "latitude", findValue(values, headers, "Geocodio Latitude"));
	            setDouble(apt, "longitude", findValue(values, headers, "Geocodio Longitude"));
	            setDouble(apt, "accuracy", findValue(values, headers, "Geocodio Accuracy Score"));
	            setString(apt, "geocodioAddress", findValue(values, headers, "Geocodio Address Line 1"));
	        }
	    }
		
		return apartments;
	}
	
	private String cleanCsvCell(String cell) {
	    return cell.replaceAll("^\"|\"$", "").strip();
	}
	
	private void setDouble(Apartment apt, String field, String value) {
		if (value == null || value.isEmpty() || value.equals("N/A")) {
	        return; // Skip empty values
	    }
		if (value != null && !value.isEmpty()) {
			try {
				double doubleValue = Double.parseDouble(value);
				switch (field) {
					case "latitude" -> apt.setLatitude(doubleValue);
					case "longitude" -> apt.setLongitude(doubleValue);
					case "accuracy" -> apt.setAccuracy(doubleValue);
				}
			} catch (NumberFormatException e) {
				System.err.println("Error parsing double for " + field + ": " + value);
			}
		}
	}

	private void setString(Apartment apt, String field, String value) {
		if (value != null && !value.isEmpty()) {
			if ("geocodioAddress".equals(field)) {
				apt.setGeocodioAddress(value);
			}
		}
	}


	private String findValue(String[] values, String[] headers, String column) {
		for (int i = 0; i < headers.length; i++) {
	        if (i < values.length && headers[i].equalsIgnoreCase(column)) {
	            return cleanCsvCell(values[i]);
	        }
	    }
        return "";
    }

	private String safeExtract(String[] values, int index) {
	    return (index < values.length) 
	        ? cleanCsvCell(values[index])
	        : "";
	}

	private String[] parseCsvLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
    }
}
