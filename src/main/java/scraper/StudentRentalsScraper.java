/**
 * Raw-only scraper for studentrentalslacrosse.com. Extracts listing fields
 * exactly as they appear on the page; normalization belongs in {@code DataCleaner}.
 *
 * @author Andrew Peirce
 */
package scraper;

import models.Apartment;
import models.FeatureType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class StudentRentalsScraper implements ApartmentScraper {

    private static final String BASE_URL =
            "https://www.studentrentalslacrosse.com/all-rental-listings-la-crosse/%s%d";
    private static final Pattern PRICE_PATTERN =
            Pattern.compile("\\$(\\d{4,}|\\d{1,3}(?:,\\d{3})*)");
    private static final String PLACEHOLDER_TITLE =
            "Rental Listing | All Rental Listings – La Crosse – Student Rentals La Crosse";

    private final int minId;
    private final int maxId;
    private final int delayMs;
    private final Path htmlDir;

    public StudentRentalsScraper(int minId, int maxId, int delayMs) {
        this(minId, maxId, delayMs, null);
    }

    /**
     * @param htmlDir directory for per-listing HTML snapshots, or {@code null} to skip saving HTML
     */
    public StudentRentalsScraper(int minId, int maxId, int delayMs, Path htmlDir) {
        this.minId = minId;
        this.maxId = maxId;
        this.delayMs = delayMs;
        this.htmlDir = htmlDir;
    }

    @Override
    public List<Apartment> scrape() {
        List<Apartment> results = new ArrayList<>();
        if (htmlDir != null) {
            try {
                Files.createDirectories(htmlDir);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create HTML snapshot directory " + htmlDir, e);
            }
        }

        for (int currentId = minId; currentId <= maxId; currentId++) {
            try {
                String url = String.format(BASE_URL, "?listing_id=", currentId);
                System.out.printf("Checking ID %d... ", currentId);

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:146.0) Gecko/20100101 Firefox/146.0")
                        .timeout(10000)
                        .ignoreHttpErrors(true)
                        .get();

                if (isInvalidListing(doc)) {
                    System.out.println("[INVALID LISTING]");
                    Thread.sleep(delayMs);
                    continue;
                }

                Apartment apt = parseListing(String.valueOf(currentId), url, doc);
                if (htmlDir != null) {
                    saveListingHtml(doc, htmlDir, apt.getId());
                }

                results.add(apt);
                System.out.printf("[VALID] %s%n", apt);
                Thread.sleep(delayMs);
            } catch (Exception e) {
                System.out.printf("[ERROR] ID %d: %s%n", currentId, e.getMessage());
            }
        }

        return results;
    }

    static Apartment parseListing(String id, String url, Document doc) {
        Apartment apt = new Apartment(id, url);
        apt.setRawTitle(doc.title());
        apt.setAddress(extractAddress(doc));
        apt.setPrice(extractPrice(doc.body().text()));
        apt.setRawDescription(extractDescription(doc));
        extractFeatures(doc).forEach(apt::setFeature);
        return apt;
    }

    private boolean isInvalidListing(Document doc) {
        if (doc.connection().response().statusCode() == 404
                || PLACEHOLDER_TITLE.equals(doc.title())) {
            return true;
        }
        return doc.selectFirst("div.rm_listing_features") == null;
    }

    private static String extractAddress(Document doc) {
        Element breadcrumb = doc.selectFirst("div.sp_rp_bread");
        if (breadcrumb == null) {
            return "";
        }
        Elements spans = breadcrumb.select("span");
        if (spans.size() <= 1) {
            return "";
        }
        return spans.get(spans.size() - 1).text().strip();
    }

    private static String extractPrice(String text) {
        Matcher matcher = PRICE_PATTERN.matcher(text);
        return matcher.find() ? "$" + matcher.group(1) : "";
    }

    /**
     * Returns the free-text "Listing Information" section: everything after that heading
     * up to the share widget or the next heading.
     */
    static String extractDescription(Document doc) {
        Element listing = doc.selectFirst("div.rm_listing_item");
        if (listing == null) {
            return null;
        }
        Element heading = null;
        for (Element h3 : listing.select("h3")) {
            if (h3.text().strip().equalsIgnoreCase("Listing Information")) {
                heading = h3;
                break;
            }
        }
        if (heading == null) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (Element sibling = heading.nextElementSibling();
                sibling != null;
                sibling = sibling.nextElementSibling()) {
            if ("sprm_share".equals(sibling.id()) || "h3".equals(sibling.tagName())) {
                break;
            }
            String part = sibling.text().strip();
            if (!part.isEmpty()) {
                if (!text.isEmpty()) {
                    text.append(' ');
                }
                text.append(part);
            }
        }
        return text.isEmpty() ? null : text.toString();
    }

    private static Map<FeatureType, String> extractFeatures(Document doc) {
        Map<FeatureType, String> featureMap = new EnumMap<>(FeatureType.class);
        Elements rows = doc.select("div.rm_listing_features > div.left-listing-row");

        for (Element row : rows) {
            Element labelElem = row.selectFirst(".left-column-listing strong");
            Element valueElem = row.selectFirst(".right-column-listing");
            if (labelElem == null || valueElem == null) {
                continue;
            }
            String label = labelElem.text().replaceAll("[^a-zA-Z0-9]", "").strip();
            FeatureType type = FeatureType.fromLabel(label);
            if (type != null) {
                featureMap.put(type, valueElem.text().strip());
            }
        }
        return featureMap;
    }

    /**
     * Writes the listing block ({@code div.rm_listing_item}) to {@code listing_<id>.html},
     * falling back to the whole page when that block is missing.
     */
    static Path saveListingHtml(Document doc, Path dir, String id) throws IOException {
        Element listing = doc.selectFirst("div.rm_listing_item");
        String html = listing != null ? listing.outerHtml() : doc.outerHtml();
        Path file = dir.resolve("listing_" + id + ".html");
        Files.writeString(file, html, StandardCharsets.UTF_8);
        return file;
    }

    @Override
    public String getSourceName() {
        return "StudentRentalsLaCrosse";
    }
}
