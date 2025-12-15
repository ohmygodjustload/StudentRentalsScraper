/**
 * ApartmentScraper.java
 * 
 * Description: An interface to describe the actions of an apartment scraper
 * 
 * @author Andrew Peirce
 * 
 * Date Last Modified: 05/11/2025
 */
package scraper;
import models.Apartment;
import java.util.List;

public interface ApartmentScraper {
	List<Apartment> scrape();
	String getSourceName();
}
