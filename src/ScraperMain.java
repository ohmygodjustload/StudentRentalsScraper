/**
 * Main.java
 * 
 * Description: A driver to run the Apartment Scraper.
 * 
 * @author Andrew Peirce
 * 
 * Date Last Modified: 08/04/2025
 */


import java.util.List;
public class ScraperMain {
    public static void main(String[] args) {
        ApartmentScraper scraper = new StudentRentalsScraper(); // Instantiate scraper for StudentRentalsLaCrosse
        List<Apartment> apartments = scraper.scrape();			// Fetch and parse apartment listings

        System.out.println("\nFinal Results:");
        for (Apartment apt : apartments) {
            System.out.println(apt);
        }
        System.out.println("\nDone. Listings saved to CSV and JSON");
    }
}