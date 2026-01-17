/**
 * A class to clean and normalize apartment data.
 * This includes standardizing addresses, landlord names, and normalizing bed/bath formats.
 * It uses explicit mappings and regex patterns to handle common variations and errors.
 * TODO - expand landlord mappings as needed.
 * 
 * @author Andrew Peirce
 * Date Last Modified: 01/09/2026
 */
package clean;
import models.Apartment;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

public class DataCleaner {
	// Canonical landlord mapping with all variations
	private static final Map<String, String> LANDLORD_MAP = new HashMap<>();
	static {
		// Wise Properties variations
		LANDLORD_MAP.put("Wise Properties", "Wise Properties");
		
		// Miller Properties variations
		LANDLORD_MAP.put("Miller Properties", "Miller Properties");
		
		// Parker Properties variations
		LANDLORD_MAP.put("Parker Properties", "Parker Properties");
		
		// EKIM variations
		LANDLORD_MAP.put("EKIM-PT", "EKIM Properties");
		LANDLORD_MAP.put("EKIM Properties", "EKIM Properties");
		
		// Off-Campus variations
		LANDLORD_MAP.put("Off-Campus La Crosse, LLC", "Off-Campus La Crosse LLC");
		LANDLORD_MAP.put("Off-Campus La Crosse Inc", "Off-Campus La Crosse LLC");
		LANDLORD_MAP.put("Off-Campus La Crosse", "Off-Campus La Crosse LLC");
		LANDLORD_MAP.put("Off-Campus La Crosse LLC", "Off-Campus La Crosse LLC");
		
		// Benson variations
		LANDLORD_MAP.put("Benson Properties", "Benson Properties");
		LANDLORD_MAP.put("Benson Management", "Benson Properties");
		
		// JCR variations
		LANDLORD_MAP.put("JCR Properties", "JCR Properties");
		
		// Favre variations
		LANDLORD_MAP.put("Favre Rentals LLC", "Favre Retnals");
		
		// Three Sixty variations
		LANDLORD_MAP.put("Three Sixty Real Estate", "Three Sixty");
		LANDLORD_MAP.put("Three Sixty", "Three Sixty");
		
		// 608 Properties variations
		LANDLORD_MAP.put("608 Rental Properties", "608 Properties");
		LANDLORD_MAP.put("608 Rental Properteis", "608 Properties");
		LANDLORD_MAP.put("608 Properties", "608 Properties");
		LANDLORD_MAP.put("608 Properties LLC", "608 Properties");
		LANDLORD_MAP.put("608 Rentals", "608 Properties");
		
		// Wrobel variations
		LANDLORD_MAP.put("Wrobel Properties", "Wrobel Rentals");
		LANDLORD_MAP.put("Wrobel Rentals", "Wrobel Rentals");
		
		// Coleman variations
		LANDLORD_MAP.put("Coleman Properties", "Coleman Properties");
		LANDLORD_MAP.put("Colemen Properties", "Coleman Properties");
		
		// JTH variations
		LANDLORD_MAP.put("JTH Homes LLC", "JTH Homes");
		
		// RK&D variations
		LANDLORD_MAP.put("RK&D Properties", "RK&D Properties");
		LANDLORD_MAP.put("RK&D LLC1", "RK&D Properties");
		
		// Dillaber variations
		LANDLORD_MAP.put("Dillaber Properties", "Dillaber Properties");
		LANDLORD_MAP.put("Dillabar Properties", "Dillaber Properties");
		
		// Sawyer variations
		LANDLORD_MAP.put("Sawyer Properties", "Sawyer Properties");
        LANDLORD_MAP.put("Sawyer Rentals", "Sawyer Properties");
        
        // Pedace variations
        LANDLORD_MAP.put("Pedace", "Pedace Properties");
        LANDLORD_MAP.put("Pedace Properties", "Pedace Properties");
        
        // Gander variations
        LANDLORD_MAP.put("Gander Rentals", "Gander Rentals");
        LANDLORD_MAP.put("Gander Rantals", "Gander Rentals");
        
        // Resurrection variations
        LANDLORD_MAP.put("Resurrection Rentals", "Resurrection Rentals");
        LANDLORD_MAP.put("Resurrection Properties", "Resurrection Rentals");
        
        // Munson Realty
        LANDLORD_MAP.put("Munson Realty", "Munson Realty");
        LANDLORD_MAP.put("Common Ground Property", "Munson Realty");
        
        // Add other landlords
        LANDLORD_MAP.put("AptsLax LLC", "AptsLax LLC");
        LANDLORD_MAP.put("Mickelson Properties", "Mickelson Properties");
        LANDLORD_MAP.put("Clarkin Properties", "Clarkin Properties");
        LANDLORD_MAP.put("Gerrard Properties", "Gerrard Properties");
        LANDLORD_MAP.put("Baumgartner Properties", "Baumgartner Properties");
        LANDLORD_MAP.put("Prairie Properties", "Prairie Properties");
        LANDLORD_MAP.put("Cappuccio Rentals", "Cappuccio Rentals");
        LANDLORD_MAP.put("MQuick Properties", "MQuick Properties");
        LANDLORD_MAP.put("Biondo Properties", "Biondo Properties");
        LANDLORD_MAP.put("Legends Living", "Legends Living");
        LANDLORD_MAP.put("Classic Housing LLC", "Classic Housing LLC");
        LANDLORD_MAP.put("JCH Rentals", "JCH Rentals");
        LANDLORD_MAP.put("JAMAR Properties", "JAMAR Properties");
        LANDLORD_MAP.put("JAMAR", "JAMAR Properties");
        LANDLORD_MAP.put("Hilton Properties", "Hilton Properties");
        LANDLORD_MAP.put("HNT Properties", "HNT Properties");
        LANDLORD_MAP.put("MAT Properties, LLC", "MAT Properties");
        LANDLORD_MAP.put("Mack Properties", "Mack Properties");
        LANDLORD_MAP.put("Casey Properties", "Casey Properties");
        LANDLORD_MAP.put("Weis Rentals", "Weis Rentals");
        LANDLORD_MAP.put("Wedgewood Commons", "Wedgewood Commons");
        LANDLORD_MAP.put("Alkar Apartments", "Alkar Apartments");
        LANDLORD_MAP.put("Clott Rentals", "Clott Rentals");
        LANDLORD_MAP.put("McKeogh Properties", "McKeogh Properties");
        LANDLORD_MAP.put("Buchner Properties", "Buchner Properties");
        LANDLORD_MAP.put("Karbin Properties", "Karbin Properties");
        LANDLORD_MAP.put("Hougom Properties", "Hougom Properties");
        LANDLORD_MAP.put("DBB Properties", "DBB Properties");
        LANDLORD_MAP.put("PK Investments", "PK Investments");
        LANDLORD_MAP.put("Lesniewski LLC", "Lesniewski LLC");
        LANDLORD_MAP.put("G&N Properties", "G&N Properties");
        LANDLORD_MAP.put("Mack Highland Properties", "Mack Properties");
	}
	
	// Explicit address corrections
	private static final Map<String, String> ADDRESS_CORRECTIONS = Map.ofEntries(
        Map.entry("420 9th St N La Crosse", "420 9th St N La Crosse WI"),
        Map.entry("128 9th St N La Crosse", "128 9th St N La Crosse WI"),
        Map.entry("509 12th St N La Crosse", "509 12th St N La Crosse WI"),
        Map.entry("101 West Ave N La Crosse", "101 West Ave N La Crosse WI"),
        Map.entry("62410th St N La Crosse WI", "624 10th St N La Crosse WI"),
        Map.entry("330 21th St N La Crosse WI", "330 21st St N La Crosse WI"),
        Map.entry("821 State St #1 La Crosse WI", "821 State St, #1, La Crosse WI"),
        Map.entry("821 State St #2 La Crosse WI", "821 State St, #2, La Crosse WI"),
        Map.entry("821 State St #3 La Crosse WI", "821 State St, #3, La Crosse WI"),
        Map.entry("821 State St #4 La Crosse WI", "821 State St, #4, La Crosse WI"),
        Map.entry("1003 La Crosse St A La Crosse WI", "1003 La Crosse St, Unit A, La Crosse WI"),
        Map.entry("1003 La Crosse St B La Crosse WI", "1003 La Crosse St, Unit B, La Crosse WI"),
        Map.entry("509 11th St N Dwn La Crosse WI", "509 11th St N, Down, La Crosse WI"),
        Map.entry("1118 La Crosse St Dwn La Crosse WI", "1118 La Crosse St, Down, La Crosse WI"),
        Map.entry("1801 Cameron Ave Dwn La Crosse WI", "1801 Cameron Ave, Down, La Crosse WI"),
        Map.entry("1915 State St Dwn La Crosse WI", "1915 State St, Down, La Crosse WI"),
        Map.entry("1917 State St Up La Crosse WI", "1917 State St, Up, La Crosse WI"),
        Map.entry("1308 State St Dwn La Crosse WI", "1308 State St, Down, La Crosse WI"),
        Map.entry("1310 State St Up La Crosse WI", "1310 State St, Up, La Crosse WI"),
        Map.entry("624 8th St S Down La Crosse WI", "624 8th St S, Down, La Crosse WI"),
        Map.entry("624 8th St S Up La Crosse WI", "624 8th St S, Up, La Crosse WI"),
        Map.entry("509 11th St N Up La Crosse WI", "509 11th St N, Up, La Crosse WI"),
        Map.entry("218 11th St N-RENTED La Crosse WI", "218 11th St N, La Crosse WI"),
        Map.entry("222 11th St N-RENTED La Crosse WI", "222 11th St N, La Crosse WI"),
        Map.entry("230 11th St N-RENTED La Crosse WI", "230 11th St N, La Crosse WI"),
        Map.entry("1108 Vine St-RENTED La Crosse WI", "1108 Vine St, La Crosse WI")
	);
	
	// TODO - normalize bed/bath formats using old StudentRentalsScraper method

	// Regex to parse address components
	private static final Pattern ADDRESS_PATTERN = Pattern.compile(
		"(\\d+)\\s+(.+?)\\s+([A-Za-z]+(?: [A-Za-z]+)?)\\s+([A-Z]{2})$");
	
	public List<Apartment> clean(List<Apartment> apartments){
		return apartments.stream()
				.map(this::cleanApartment)
				.collect(Collectors.toList());
	}
	
	private Apartment cleanApartment(Apartment apt) {
		apt.setAddress(normalizeAddress(apt.getAddress()));
		apt.setLandlord(normalizeLandlord(apt.getLandlord()));
		return apt;
	}
	
	private String normalizeAddress(String rawAddress) {
		if (rawAddress == null || rawAddress.isBlank()) return "";
		
		// Trim and collapse spaces
		String address = rawAddress.strip().replaceAll("\\s+", " ");
		
		// Apply explicit corrections
		if (ADDRESS_CORRECTIONS.containsKey(address)) {
			return ADDRESS_CORRECTIONS.get(address);
		}
		
		// Handle specific patterns
		address = address
			.replaceAll("([a-zA-Z])(\\d)", "$1 $2") // Add space between letter and number
//			.replaceAll("(\\d)([a-zA-Z])", "$1 $2") // Add space between number and letter
			.replace(" N ", " North ")
			.replace(" S ", " South ")
			.replace(" E ", " East ")
			.replace(" W ", " West ")
			.replaceAll("(St|Ave|Blvd|Dr|Rd|Ct|Pl|Ln|Cir)\\s+([NESW])", "$1 $2");
		
		// Add state if missing
		if (!address.matches(".*,?\\s*(WI|MN)$")) {
			if (address.contains("La Crosse")) {
				address += ", WI";
			} else if (address.contains("La Crescent")) {
				address += ", MN";
			} else {
				address += ", WI"; // Default
			}
		}
		
		// Standardize format
		var matcher = ADDRESS_PATTERN.matcher(address);
		if (matcher.find()) {
			String houseNum = matcher.group(1);
			String street = matcher.group(2);
			String city = matcher.group(3);
			String state = matcher.group(4);
			return String.format("%s %s %s %s", houseNum, street, city, state);
		}
		
		return address;
	}
	
	private String normalizeLandlord(String rawLandlord) {
		if (rawLandlord == null || rawLandlord.isBlank()) return "Unkown";
		String key = rawLandlord.strip();
		return LANDLORD_MAP.getOrDefault(key, key);
	}
}
