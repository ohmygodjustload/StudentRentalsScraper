/**
 * Contract for rental listing scrapers.
 *
 * @author Andrew Peirce
 */
package scraper;

import models.Apartment;
import java.util.List;

public interface ApartmentScraper {
    List<Apartment> scrape();
    String getSourceName();
}
