package main;

import api.CrimeDataAPI;
import api.TravelTimeAPI;
import api.WalkScoreAPI;
import clean.DataCleaner;
import models.Apartment;
import scraper.ApartmentScraper;
import scraper.StudentRentalsScraper;
import utils.CsvUtils;
import utils.JsonUtils;
import geocoding.DataMerger;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

// Flask server - Why is this here?

/**
 * TODO - SCRAPER (Goal: scraper does ZERO cleaning. only extraction)
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

/**
 * TODO - CLEANER (Goal: ALL normalization is done here. predictable and consistent)
 * [Implement these]
 * - Add normalizeBedBath(raw) method (the fixed version)
 * - Add double-parsing logic for fractional bedrooms
 * - Add double-parsing logic for fractional bathrooms
 * - Add formatted output: 1.75bd/1ba
 * - Add studio detection when bedPart == "Studio"
 * - Add numeric extraction fields if needed (bedrooms, bathrooms)
 * - Update cleanApartment() to apply bed/bath cleaning
 * - Remove ANY redundant normalization left over in scraper
 * [RULES TO ENFORCE]
 * - Bedrooms: allow fractional values
 * - Bathrooms: allow fractional values
 * - Preserve 1.75, 1.5, 2.5 etc
 * - Keep “Studio” intact → “Studio/<bath>ba”
 * - Standardize to Xbd/Yba format
 * - Ensure cleaner writes back to apartment with normalized value
 * [New feature normalization (optional)]
 * - Normalize “br”, “ba”, “bdrm”, “bedroom” variants (optional)
 * - Normalize pets ("No Pets" → "No") (optional)
 * - Normalize laundry types (optional)
 * - Normalize parking types (optional)
 * 
 */

/**
 * TODO - APARTMENT (Goal: correctly model fractional beds/baths for future analytics)
 * [Modify Apartment Class]
 * - Add double bedrooms
 * - Add double bathrooms
 * - Keep existing String bedBath for CSV/GUI formatting
 * - Add getters/setters for new numeric fields
 * - Update toString() if needed to reflect normalized output
 * - Consider removing bed/bath logic from scraper-facing constructor
 * 
 */

/**
 * TODO - FEAUTURE MAP + FLAGS (Goal: annotate condition, don't clean in scraper)
 * [Add new flags if needed]
 * - Flag.RAW_BEDBATH_FALLBACK
 * - Flag.RAW_BEDBATH_STUDIO_DETECTED
 * - Flag.FRACTIONAL_BEDROOM
 * - Flag.FRACTIONAL_BATHROOM
 * - Flag.BEDBATH_NORMALIZED
 */

/**
 * TODO - DATA MERGER + API CLASSES (Goal: nothing breaks from reorganizing bed/bath)
 * [Verify]
 * - Merging geocoded data still works (address normalization now consistent)
 * - TravelTimeAPI still operates on cleaned addresses
 * - WalkScore & CrimeData still operate on cleaned addresses
 * - CSVUtils writes numeric fields correctly
 * - JsonUtils reflects both raw + normalized features if desired
 * - Make API keys secure
 */

/**
 * TODO - CSV/JSON OUTPUT (Goal: preserve raw data + produce canonical cleaned file)
 * [Raw output]
 * - Must contain exactly what scraper extracted
 * - No normalized bed/bath
 * - No cleaned landlord names
 * - Only fallback values (never cleaned values)
 * [Cleaned output]
 * - must contain normalized Xbd/Yba (or Studio/Yba)
 * - must contain numeric doubles for bed/bath if added
 * - must use cleaned addresses
 * - must use mapped landlords
 */

/**
 * TODO - ENGINEERING FUTURE PROOFING
 * - Add CI test that compares raw vs cleaned output to ensure cleaner is doing work
 * - Add tests for bed/bath normalization
 * - Add tests for fractional bedroom support
 * - Add sanity checks for unusual bedroom counts (>10, <0)
 * - Add diff tool between raw & cleaned for debugging
 * - Add caching for TravelTime API
 * - Add database persistence (SQLite or lightweight H2) 
 */
public class Orchestrator {
	
	private enum Mode {
		FULL_PIPELINE,			// Scrape -> Clean -> Geocode -> APIs -> Final JSON
		SCRAPE_AND_CLEAN_ONLY,	// Scrape -> Clean -> Stop
		CLEAN_ONLY,				// Clean existing scraped JSON
		GEOCODE_ONLY,			// Geocode existing cleaned JSON
		APIS_ONLY,				// Run only API enrichment on geocoded JSON
		RESUME_FROM_CLEANED		// Geocode -> APIs -> Final JSON
	}

	// Change this to control run mode
    private static final Mode RUN_MODE = Mode.SCRAPE_AND_CLEAN_ONLY;
	
	// Base output directory
	private static final String OUTPUT_DIR = "C:\\Users\\drew1\\Programming\\StudentRentalsScraper\\output";

	// Helper to generate unique filenames
    private static String uniqueFilename(String baseName, String extension, String timestamp) {
        return baseName + "_" + timestamp + "." + extension;
    }
    
    // Helper to find latest file matching pattern
    private static String findLatestFile(String prefix, String extension, String dir) throws IOException {
        try (Stream<Path> files = Files.list(Paths.get(OUTPUT_DIR + dir))) {
            return files
                .filter(f -> {
                    String fileName = f.getFileName().toString();
                    return fileName.startsWith(prefix) && fileName.endsWith("." + extension);
                })
                .max(Comparator.comparingLong(f -> f.toFile().lastModified()))
                .map(Path::toString)
                .orElseThrow(() -> new IOException("No matching file found for " + prefix + "*." + extension));
        }
    }
	
	public static void main(String[] args) {
		final String runTimestamp = LocalDateTime.now()
		        .format(DateTimeFormatter.ofPattern("MM_dd_yyyy_HHmm"));
		try {			
			List<Apartment> apartments = null;
			DataCleaner cleaner = new DataCleaner();
			
			// Declare variables outside switch to avoid scope issues
            String scrapedJson, cleanedJson, cleanedCsv;
            
			// Handle different run modes
			switch (RUN_MODE) {
				case FULL_PIPELINE:
				case SCRAPE_AND_CLEAN_ONLY:
					// Run scraper
                    ApartmentScraper scraper = new StudentRentalsScraper();
                    apartments = scraper.scrape();
                    System.out.println("[Scraper] Apartments scraped: " + apartments.size());
                    
                    // Save raw scraped data with unique filename
                    scrapedJson = Paths.get(OUTPUT_DIR + "\\Raw", 
                        uniqueFilename("listings_scraped", "json", runTimestamp)).toString();
                    JsonUtils.toJson(apartments, scrapedJson);
                    System.out.println("[Scraper] Raw data saved: " + scrapedJson);
                    
                    // Clean data
                    apartments = cleaner.clean(apartments);
                    System.out.println("[Cleaner] Data cleaning complete");
                    
                    // Save cleaned data with unique filename
                    cleanedJson = Paths.get(OUTPUT_DIR + "\\Cleaned", 
                        uniqueFilename("listings_cleaned", "json", runTimestamp)).toString();
                    cleanedCsv = Paths.get(OUTPUT_DIR + "\\Cleaned", 
                        uniqueFilename("listings_cleaned", "csv", runTimestamp)).toString();
                    JsonUtils.toJson(apartments, cleanedJson);
                    CsvUtils.toCsv(apartments, cleanedCsv);
                    System.out.println("[Cleaner] Cleaned data saved: " + cleanedJson);
                    System.out.println("[Cleaner] Cleaned data saved: " + cleanedCsv);
                    
                    if (RUN_MODE == Mode.SCRAPE_AND_CLEAN_ONLY) {
                    	System.out.println("[Orchestrator] SCRAPE_AND_CLEAN_ONLY complete. Exiting.");
                    	return;
                    }
                    break;
                    
				case CLEAN_ONLY:
					// Load latest scraped data
                    String scrapedFile = findLatestFile("listings_scraped", "json", "\\Raw");
                    apartments = JsonUtils.fromJson(scrapedFile);
                    System.out.println("[Cleaner] Loaded scraped data from: " + scrapedFile);
                    
                    // Clean data
                    apartments = cleaner.clean(apartments);
                    System.out.println("[Cleaner] Data cleaning complete");
                    
                    // Save cleaned data with new timestamp
                    cleanedJson = Paths.get(OUTPUT_DIR + "\\Cleaned", 
                        uniqueFilename("listings_cleaned", "json", runTimestamp)).toString();
                    cleanedCsv = Paths.get(OUTPUT_DIR + "\\Cleaned", 
                        uniqueFilename("listings_cleaned", "csv", runTimestamp)).toString();
                    JsonUtils.toJson(apartments, cleanedJson);
                    CsvUtils.toCsv(apartments, cleanedCsv);
                    System.out.println("[Cleaner] Cleaned data saved: " + cleanedJson);
                    return;
                    
				case GEOCODE_ONLY:
				case RESUME_FROM_CLEANED:
					// Load latest cleaned data
                    String cleanedFile = findLatestFile("listings_cleaned", "json", "\\Cleaned");
                    apartments = JsonUtils.fromJson(cleanedFile);
                    System.out.println("[Orchestrator] Loaded cleaned data from: " + cleanedFile);
                    break;
                    
				case APIS_ONLY:
					// Load latest geocoded data
                    String geocodedFile = findLatestFile("listings_geocoded", "json", "\\Geocoded");
                    apartments = JsonUtils.fromJson(geocodedFile);
                    System.out.println("[Orchestrator] Loaded geocoded data from: " + geocodedFile);
                    break;
                    
				default:
					throw new IllegalStateException("Unsupported run mode: " + RUN_MODE);
			}
			
			// Geocoding step (if needed)
            if (RUN_MODE == Mode.FULL_PIPELINE || 
                RUN_MODE == Mode.RESUME_FROM_CLEANED || 
                RUN_MODE == Mode.GEOCODE_ONLY) {
                
                System.out.println("[Geocoder] Pause: Place the enhanced Geocod.io CSV in the output folder");
                System.out.println("[Geocoder] Press Enter to continue after placing geocodio.csv...");
                new Scanner(System.in).nextLine();
                
                String geocodioCsv = Paths.get(OUTPUT_DIR + "\\Geocoded", "geocodio.csv").toString();
                DataMerger merger = new DataMerger();
                apartments = merger.mergeGeocodioData(apartments, geocodioCsv);
                System.out.println("[Geocoder] Geocoding complete");
                
                // Save geocoded data with unique filename
                String geocodedJson = Paths.get(OUTPUT_DIR + "\\Geocoded", 
                    uniqueFilename("listings_geocoded", "json", runTimestamp)).toString();
                JsonUtils.toJson(apartments, geocodedJson);
                System.out.println("[Geocoder] Geocoded data saved: " + geocodedJson);
                
                if (RUN_MODE == Mode.GEOCODE_ONLY) {
                    System.out.println("[Orchestrator] GEOCODE_ONLY complete. Exiting.");
                    return;
                }
            }
			
            // API enrichment step with intermediate saves
            if (RUN_MODE == Mode.FULL_PIPELINE || 
                RUN_MODE == Mode.RESUME_FROM_CLEANED || 
                RUN_MODE == Mode.APIS_ONLY) {
                
                System.out.println("[APIs] Starting API enrichment...");
                
                // Travel Time API
                TravelTimeAPI travelTime = new TravelTimeAPI();
                apartments = travelTime.addTravelTimes(apartments);
                System.out.println("[APIs] Travel times added");
                
                // Save intermediate state
                String afterTravelJson = Paths.get(OUTPUT_DIR + "\\API", 
                    uniqueFilename("listings_after_travel", "json", runTimestamp)).toString();
                JsonUtils.toJson(apartments, afterTravelJson);
                System.out.println("[APIs] Saved after travel API: " + afterTravelJson);
                
                // Walk Score API
                WalkScoreAPI walkScore = new WalkScoreAPI();
                apartments = walkScore.addWalkScores(apartments);
                System.out.println("[APIs] Walk scores added");
                
                // Save intermediate state
                String afterWalkJson = Paths.get(OUTPUT_DIR + "\\API", 
                    uniqueFilename("listings_after_walk", "json", runTimestamp)).toString();
                JsonUtils.toJson(apartments, afterWalkJson);
                System.out.println("[APIs] Saved after walk API: " + afterWalkJson);
                
                // Crime Data API
                // TODO - this api should have its own output, since the data is not needed for each listing
                CrimeDataAPI crimeData = new CrimeDataAPI();
                apartments = crimeData.addCrimeStats(apartments);
                System.out.println("[APIs] Crime data added");
                
                // Save final data with unique filename
                String finalJson = Paths.get(OUTPUT_DIR + "\\Final", 
                    uniqueFilename("listings_final", "json", runTimestamp)).toString();
                JsonUtils.toJson(apartments, finalJson);
                System.out.println("[APIs] Final data saved: " + finalJson);
            }
            
            System.out.println("[Orchestrator] Pipeline complete");
		} catch (IOException e) {
			System.err.println("[ERROR] Pipeline failed: " + e.getMessage());
            e.printStackTrace();
		}
	}
}