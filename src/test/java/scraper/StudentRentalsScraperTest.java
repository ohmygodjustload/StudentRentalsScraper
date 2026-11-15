package scraper;

import models.Apartment;
import models.FeatureType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StudentRentalsScraperTest {

    private Document loadListing424() throws Exception {
        String html = new String(
                getClass().getResourceAsStream("/fixtures/listing_424.html").readAllBytes(),
                StandardCharsets.UTF_8);
        return Jsoup.parse(html, "https://www.studentrentalslacrosse.com/");
    }

    @Test
    void parseListing_extracts_title_field_and_description() throws Exception {
        Apartment apt = StudentRentalsScraper.parseListing("424", "https://example.com/424", loadListing424());

        assertTrue(apt.getRawTitle().contains("5br/1ba"));
        assertEquals("2/1", apt.getFeature(FeatureType.BED_BATH));
        assertEquals("Off-Campus La Crosse, LLC", apt.getFeature(FeatureType.LANDLORD));
        assertEquals("$2500", apt.getPrice());
        assertEquals("312 9th St N La Crosse WI", apt.getAddress().replaceAll("\\s+", " "));

        String description = apt.getRawDescription();
        assertNotNull(description);
        assertTrue(description.contains("five bedrooms, 2 bath"), description);
        assertTrue(description.contains("RENTED FOR 2027-2028 SCHOOL YEAR"), description);
        assertFalse(description.contains("Details and Features"), description);
        assertFalse(description.contains("Duplex"), description);
    }

    @Test
    void extractDescription_returns_null_without_listing_block() {
        Document doc = Jsoup.parse("<html><body><p>nothing here</p></body></html>");
        assertNull(StudentRentalsScraper.extractDescription(doc));
    }

    @Test
    void saveListingHtml_writes_only_listing_block(@TempDir Path dir) throws Exception {
        Path file = StudentRentalsScraper.saveListingHtml(loadListing424(), dir, "424");

        assertEquals(dir.resolve("listing_424.html"), file);
        String saved = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(saved.startsWith("<div class=\"rm_listing_item\">"), saved.substring(0, 60));
        assertTrue(saved.contains("five bedrooms"));
        assertFalse(saved.contains("<title>"));
    }
}
