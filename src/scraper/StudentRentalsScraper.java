/**
 * StudentRentalsScraper.java
 * 
 * Description: A web scraper project I created to find cheap listings that weren't publicly
 * listed on the studentrenatlslacrosse.com web site and output relevant info to a CSV.
 * 
 * @author Andrew Peirce
 * 
 * Date Last Modified: 08/05/2025
 */
package scraper;
import models.Apartment;
import models.FeatureType;
import models.Flag;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * TODO
 * 
 *
 * Create a simple (at first) GUI for this project using Swing or JavaFX with a buffer/loading bar,
 * different methods of sorting, and annotations for the listings missing some info, then
 * as I get some more back end robustness, add more to the GUI
 * 
 * Get historical rent price data using an API
 * 
 * Create a "deal scoring" system, which takes price per bed, price per sqft (if can
 * be inferred), possible landlord reliability, missing data penalty, distance to
 * campus/transport using free geocoding service, ranking the utilities/amenities included,
 * etc and scores the properties on a scale from 0-100 that changes color depending on
 * the level (higher number = green, medium = yellow, lower = orange to red). Could make it
 * simple at first, and add to it as functionality and robustness increases.
 * 
 * Detect any changes by storing each scrape using JSON or SQL and flag when price,
 * availability, or other fields change.
 * 
 *
 * In the GUI, show a map, and also a scatter plot of rent vs distance to 
 * campus (geocoding API) to evaluate tradeoff
 * 
 * 
 * Crime map overlay using public crime data APIs, color code neighborhoods
 * 
 * Integrate public transport APIs to calculate commute time
 * 
 * 
 */



/**
 * TODO - SCRAPER (goal: scraper does ZERO cleaning. only extraction)
 * [remove/move out]
 * - remove normalizeBedBath from scraper
 * - remove bedroom/bathroom normalization inside handleBedBath
 * - remove automatic rewriting of "studio" -> "Studio/1ba"
 * - remove formatting like "3bd/1ba" from scraper
 * - remove integer parsing for bed counts
 * - Remove parsing fractional baths in scraper
 * - Stop converting fractional values to ints
 * - Stop stripping or modifying scraped strings other than .strip()
 * [keep in scraper]
 * - Extract raw strings EXACTLY from HTML
 * - Store raw bed/bath feature: e.g. "1.75/1"
 * - If bed/bath missing, mark Flag.MISSING_BEDBATH
 * - Provide fallback detection ONLY when scraper MUST avoid a null 
 * - (e.g. missing landlord -> fallback to doc text)
 * [Add these changes]
 * - Ensure scraper sets apt.setFeature(BED_BATH, rawValue) exactly as scraped
 * - Ensure scraper DOES NOT call normalizeBedBath
 * - Replace early-cleaning with simple NPE-safe raw passthrough
 * - Add flag for fallback values rather than transforming them
 * - Keep calculateBasicScore() (BUT simplify if needed later)
 * [New scraper responsibilities]
 * - Raw extraction
 * - Flagging missing/odd data
 * - NEVER modifying or interpreting data
 * - Returning the dirtiest possible “raw truth”
 */

public class StudentRentalsScraper implements ApartmentScraper {
	
	// URL pattern for scraping listings (formatted with query + listing ID)
    private static final String BASE_URL = "https://www.studentrentalslacrosse.com/all-rental-listings-la-crosse/%s%d";
    
    // Regex pattern to extract property title (landlord and bed/bath info)
    private static final Pattern TITLE_PATTERN = Pattern.compile("^(.*?)\\s*[\\–\\-]\\s*(\\d+br\\/\\d+(\\.\\d+)?ba).*");
    
    // Regex pattern to extract prices with different formats (e.g. $650, $1,200)
    private static final Pattern PRICE_PATTERN = Pattern.compile("\\$(\\d{4,}|\\d{1,3}(?:,\\d{3})*)");

    // Min and Max numbers of listing IDs to check (0 - 900 seems to cover all)
    private static final int MIN_ID = 20;
    private static final int MAX_ID = 50;
    
    // Delay between requests to avoid hammering the server
    private static final int DELAY_MS = 800 + (int)(Math.random() * 400); // Random delay between 800-1200 ms
    
	/**
	 * Scrapes apartment listings from StudentRentalsLaCrosse.com
	 * 
	 * @return A list of valid Apartment listings
	 */
    @Override
    public List<Apartment> scrape() {
    	// List to hold all valid listings
        List<Apartment> results = new ArrayList<>();
        
        // Loop through given listing IDs
        for (int currentID = MIN_ID; currentID <= MAX_ID; currentID++) {
            try {
                String url = String.format(BASE_URL, "?listing_id=", currentID);
                System.out.printf("Checking ID %d... ", currentID);
                
                // Fetch HTML document using Jsoup
                Document doc = Jsoup.connect(url)
                					.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:146.0) Gecko/20100101 Firefox/146.0")
                					.timeout(10000)
                					.ignoreHttpErrors(true)
                					.get();
                              
                if (isInvalidListing(doc)) { // if listing is invalid
                	System.out.println("[INVALID LISTING]"); // Log and skip
                	Thread.sleep(DELAY_MS);
                	continue;
                }
                
				// Create new Apartment object with URL and ID
                Apartment apt = new Apartment(String.valueOf(currentID), url);
                
				// Extract address from "sp_rp_bread" div
                // String address = extractAddress(doc);
                apt.setAddress(extractAddress(doc));
                
                apt.setPrice(extractPrice(doc.body().text()));

				// Extract features from "rm_listing_features" div
                Map<FeatureType, String> features = extractFeatures(doc);
                features.forEach(apt::setFeature);
                
                flagMissingFeatures(apt, doc); // TODO - move missing feature handling to cleaner
                
                // apt.calculateBasicScore(); // TODO - move scoring to standalone scorer class
                
                results.add(apt); // Add valid listing to results
                
                System.out.printf("[VALID] %s%n", apt.toString()); // Log valid listing
                
                Thread.sleep(DELAY_MS); // Pause before next request
                
            } catch (Exception e) {
                System.out.printf("[ERROR] ID %d: %s%n", currentID, e.getMessage());
            }
        }

//        saveResults(results);
        return results; // Return all valid listings
    }
        
	/**
	 * Checks if a listing is invalid based on status code and content
	 * 
	 * @param doc The document to check
	 * @return True if invalid, false otherwise
	 */
    private boolean isInvalidListing(Document doc) {
    	// If 404 or contains placeholder title
		String placeholderTitle = "Rental Listing | All Rental Listings – La Crosse – Student Rentals La Crosse";
    	if (doc.connection().response().statusCode() == 404 ||
    		doc.title().equals(placeholderTitle)) {
    		return true;
    	}
    	
    	// If no features detected
    	if (doc.selectFirst("div.rm_listing_features") == null) {
    		return true;
    	}
    	
    	return false;
	}


	// // TODO - move this method to data cleaner class later
	// /**
    //  * Normalizes the bed/bath info for compatibility in Excel
	//  * 
	//  * SOON TO BE DEPRECATED - move to data cleaner
    //  * @param raw The raw bed/bath info from the page source
    //  * @return The correctly formatted bed/bath info
    //  */
    // private String normalizeBedBath(String raw) {
    // 	// TODO - remove/move out: remove normalizeBedBath from scraper
    // 	if (raw == null) {
    // 		return "";
    // 	}
    // 	raw = raw.strip();
    // 	if (raw.isEmpty()) {
    // 		return "";
    // 	}
    	
    // 	String[] parts = raw.split("/");
    // 	if (parts.length != 2) {
    // 		return raw; // fallback if unexpected format
    // 	}
    	
    // 	String bedPart = parts[0].strip();
    // 	String bathPart = parts[1].strip();
    	
	// 	// TODO - remove/move out: remove bedroom/bathroom normalization inside handleBedBath
    // 	boolean isStudio = false;

	// 	// TODO - remove/move out: remove integer parsing for bed counts
    // 	int beds = 0;
    // 	try {
    // 		// TODO - remove/move out: Stop converting fractional values to ints
    // 		beds = Integer.parseInt(bedPart);
    // 		if (beds == 0 || bedPart.equalsIgnoreCase("Studio")) {
    // 			isStudio = true;
    // 		}
    // 	} catch (NumberFormatException e) {
    // 		// If bed part non-numeric, leave as-is
    // 	}
    // 	// TODO - remove "xbd/yba" formatting from scraper and do it in cleaner
    // 	String bathFormatted;
    // 	try {
    // 		// TODO - remove/move out: Stop stripping or modifying scraped strings other than .strip()
    // 		double bath = Double.parseDouble(bathPart);
    // 		if (bath == (int) bath) {
    // 			bathFormatted = String.format("%dba", (int) bath);
    // 		} else {
    // 			bathFormatted = String.format("%sba", bathPart);
    // 		}
    // 	} catch (NumberFormatException e) {
    // 		bathFormatted = bathPart + "ba"; // if format weird, still append "ba"
    // 	}
    	
    // 	if (isStudio) {
	// 		// TODO - remove automatic "Studio/1ba" conversion from scraper and do it in cleaner
    // 		return "Studio/" + bathFormatted;
    // 	} else if (beds > 0) {
    // 		return String.format("%dbd/%s", beds, bathFormatted);
    // 	} else { // Parsing Failed
    // 		return raw;
    // 	}
	// }

	/**
	 * Fixes bad characters and extra text from listing titles
	 * Use this method before parsing the title for landlord and bed/bath info,
	 * not for the final stored title.
	 * 
	 * REDUNDANT - move to ApartmentPostProcessor class
	 * @param input The raw title with all characters
	 * @return The cleaned title
	 */
	private String cleanTitle(String input) {
	    return input.replace("â€“", "-")
	               .replaceAll("\\|.*", "")
	               .strip();
	}

	/**
	 * Extracts the landlord name and bed/bath info from the listing title
	 * 
	 * REDUNDANT - move to ApartmentPostProcessor class
	 * @param rawTitle The raw title which is the renter name and bed/bath info in one string
	 * @return The landlord name and bed/bath info as two strings in an array
	 */
	private String[] parseTitle(String rawTitle) {
	    Matcher matcher = TITLE_PATTERN.matcher(rawTitle);
	    if (matcher.find()) {
	        return new String[] {
	        	matcher.group(1).strip(), // Landlord name
	            matcher.group(2).strip()  // Bed/Bath format
	        };
	    }
	    return null;
	}

	/**
	 * Extracts the address from "sp_rp_bread" div
	 * 
	 * @param doc The document to parse
	 * @return address The extracted address
	 */
	private String extractAddress(Document doc) {
		String address = "";
		Element breadcrumb = doc.selectFirst("div.sp_rp_bread");
		if (breadcrumb != null) {
	    	Elements spans = breadcrumb.select("span");
	    	if (spans.size() > 1) {
	    		address = spans.get(spans.size() - 1).text().strip();
	    	}
	    }
		return address;
	}

	/**
	 * Searches the page text for a price using predefined regex patterns
	 * 
	 * @param text The page text
	 * @return The price data
	 */
	private String extractPrice(String text) {
	    Matcher matcher = PRICE_PATTERN.matcher(text);
	    return matcher.find() ? "$" + matcher.group(1) : "";
	}

	/**
     * Extracts features found in the "rm_listing_features" div
	 * (e.g. landlord, bed/bath, included amenities)
	 * 
     * @param doc The document to parse
     * @param rows The map that will hold data under <div class="rm_listing_features">
     * @return The String representation of the amenities
     */
    private Map<FeatureType, String> extractFeatures(Document doc) {
        Map<FeatureType, String> featureMap = new EnumMap<>(FeatureType.class);
        Elements rows = doc.select("div.rm_listing_features > div.left-listing-row");
    	
        for (Element row : rows) {
        	Element labelElem = row.selectFirst(".left-column-listing strong");
        	Element valueElem = row.selectFirst(".right-column-listing");
        	
        	if (labelElem != null && valueElem != null) {
        		String label = labelElem.text().replaceAll("[^a-zA-Z0-9]", "").strip();
        		FeatureType type = FeatureType.fromLabel(label);
        		
        		if (type != null) {
        			featureMap.put(type, valueElem.text().strip());
        		}
        	}
        }
        
        return featureMap;
	}

	/**
	 * Flags missing features in the apartment listing without modifying existing data
	 * 
	 * DEPRECATED - move logic to a standalone ApartmentPostProcessor class to handle missing features
	 * and flagging
	 * @param apt The apartment being processed
	 * @param doc The document to parse
	 */
    private void flagMissingFeatures(Apartment apt, Document doc) {
    	
    	// Landlord fallback
    	flagLandlord(apt, doc);
    	
    	// Bed/Bath fallback
    	flagBedBath(apt, doc);
    }

	/**
	 * Handles missing landlord info by attempting to extract from title
	 * and flags accordingly (missing or from title)
	 * 
	 * DEPRECATED - move logic to a standalone ApartmentPostProcessor class to handle missing features
	 * @param apt The apartment being processed
	 * @param doc The document to parse
	 */
	private void flagLandlord(Apartment apt, Document doc) {
    	if (apt.getFeature(FeatureType.LANDLORD) == null) {
    		String title = cleanTitle(doc.title());
    		String[] titleParts = parseTitle(title);
    		
    		if (titleParts != null && titleParts.length > 0) {
    			// apt.setFeature(FeatureType.LANDLORD, titleParts[0]);
    			apt.addFlag(Flag.LANDLORD_FROM_TITLE); // Mark that landlord needs to be retrieved from title
    		} else {
    			apt.addFlag(Flag.MISSING_LANDLORD);
    		}
    	}
		
	}

	// TODO - ensure this method handles studio and other edge cases correctly
	/**
	 * Handles missing bed/bath info by attempting to extract from title
	 * and flags accordingly (missing or from title)
	 * 
	 * DEPRECATED - move logic to a standalone ApartmentPostProcessor class to handle missing features
	 * @param apt The apartment being processed
	 * @param doc The document to parse
	 */
	private void flagBedBath(Apartment apt, Document doc) {
    	// Preserve raw scraped value if present
    	String bedBath = apt.getFeature(FeatureType.BED_BATH);
    	if (bedBath == null) {
    		String title = cleanTitle(doc.title());
    		String[] titleParts = parseTitle(title);
    		if (titleParts != null && titleParts.length > 1) {
    			// bedBath = titleParts[1];
    			apt.addFlag(Flag.BEDBATH_FROM_TITLE); // Mark that bed/bath needs to be retrieved from title
    		} else {
    			apt.addFlag(Flag.MISSING_BEDBATH);
				// bedBath = ""; // Avoid null pointer error
    		}
    	// } else {
    	// 	// The bed/bath info is already present from features
    	// 	if (apt.getFeature(FeatureType.BED_BATH).toLowerCase().contains("studio")) {
    	// 		apt.setFeature(FeatureType.BED_BATH, "Studio/1ba");
    	// 		return;
    	// 	}
    	}
    	
		// TODO - remove normalization here and do it in cleaner
    	// Normalize regardless of source
    	// apt.setFeature(FeatureType.BED_BATH, normalizeBedBath(bedBath));
	}

	// TODO - shit ass poop fart
	// /**
	//  * DEPRECATED - use src/utils/JsonUtils and src/utils/CsvUtils instead
	//  * 
	//  * Saves final scraped listings to JSON and CSV files
	//  * @param listings The list of valid apartment listings
	//  */
	// private void saveResults(List<Apartment> listings) {
	// 	saveToJSON(listings);
	// 	saveToCSV(listings);
	// }

	// /**
	//  * Saves final scraped listings to a JSON file
	//  * 
	//  * DEPRECATED - use src/utils/JsonUtils instead
	//  * @param listings The list of valid apartment listings
	//  */
	// private void saveToJSON(List<Apartment> listings) {
	// 	ObjectMapper mapper = new ObjectMapper();
		
	// 	// Configure mapper
	// 	mapper.enable(SerializationFeature.INDENT_OUTPUT);
	// 	mapper.registerModule(new JavaTimeModule());
		
	// 	try {
	// 		// Create metadata object
	// 		Map<String, Object> metadata = new HashMap<>();
	// 		metadata.put("source", getSourceName());
	// 		metadata.put("scrapeDate", LocalDateTime.now());
	// 		metadata.put("listingCount", listings.size());
	// 		metadata.put("apiVersion", "1.0");
			
	// 		// Create root object
	// 		Map<String, Object> root = new HashMap<>();
	// 		root.put("metadata", metadata);
	// 		root.put("apartments", listings);
			
	// 		// Generate filename with timestamp
	// 		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern
	// 				("yyyyMMdd_HHmmss"));
	// 		String filename = "listings_" + timestamp + ".json";
			
	// 		// Write to file
	// 		mapper.writeValue(new File(filename), root);
			
	// 		System.out.printf("%nSaved %d listings to %s%n", listings.size(), filename);
	// 	} catch (IOException e) {
	// 		System.err.println("Error saving JSON: " + e.getMessage());
	// 		try {
	// 			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern
	// 					("yyyyMMdd_HHmmss"));
	// 			String filename = "listings_error_" + timestamp + ".json";
	// 			mapper.writeValue(new File(filename), listings);
	// 		} catch (IOException ex) {
	// 			System.err.println("Critical error saving fallback JSON: " + ex.getMessage());
	// 		}
	// 	}
	// }

	// /**
    //  * Saves final scraped listings to a CSV file
	//  * 
	//  * DEPRECATED - use src/utils/CsvUtils instead
    //  * @param listings The list of valid apartment listings
    //  */
    // private void saveToCSV(List<Apartment> listings) {
    // 	String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern
	// 			("yyyyMMdd_HHmmss"));
    // 	String filename = "listings_" + timestamp + ".csv";
    //     try (FileWriter writer = new FileWriter(filename)) {
    //         writer.append("URL,Landlord,Bed/Bath,Address,Price,Amenities,DealScore,Flags\n");
    //         for (Apartment apt : listings) {
    //             writer.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.1f,\"%s\"%n",
    //             	escape(apt.getURL()),
    //             	escape(apt.getLandlord()),
    //             	escape(apt.getBedBath()),
    //             	escape(apt.getAddress()),
    //             	escape(apt.getPrice()),
    //             	escape(apt.getAmenities()),
    //             	apt.getDealScore(),
    //             	escape(apt.getFlags().toString())
    //             ));
    //         }
    //         System.out.printf("%nSaved %d listings to %s%n", listings.size(), filename);
    //     } catch (IOException e) {
    //         System.out.println("Error saving CSV: " + e.getMessage());
    //     }
    // }

    // /**
    //  * Escapes quotes for CSV compatibility
    //  * @param input
    //  * @return input without quotes
    //  */
    // private String escape(String input) {
    //     return input.replace("\"", "\"\"");
    // }

    @Override
    public String getSourceName() {
        return "StudentRentalsLaCrosse";
    }
}