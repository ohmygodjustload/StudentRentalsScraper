import java.util.List;

/**
 * ApartmentScraper.java
 * 
 * Description: An interface to describe the actions of an apartment scraper
 * 
 * @author Andrew Peirce
 * 
 * Date Last Modified: 05/11/2025
 */
public interface ApartmentScraper {
	List<Apartment> scrape();
	String getSourceName();
}
