/**
 * Read-only comparison of the landlord and street address on each listing page:
 * the title against the site's Landlord field and the breadcrumb address.
 * Used to flag listings for review; it never modifies listings.
 *
 * @author Andrew Peirce
 */
package clean;

import models.Apartment;
import models.FeatureType;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class LandlordAddressAudit {

    public enum Check {
        AGREE, CONFLICT, TITLE_MISSING, FIELD_MISSING
    }

    /**
     * @param titleLandlord  landlord text from the title, or null
     * @param fieldLandlord  the site's Landlord field, or null
     * @param landlord       comparison result for the landlord
     * @param titleAddress   street address segment from the title, or null
     * @param fieldAddress   breadcrumb address, or null
     * @param address        comparison result for the address
     */
    public record Result(
            String titleLandlord,
            String fieldLandlord,
            Check landlord,
            String titleAddress,
            String fieldAddress,
            Check address) {
    }

    /** Title segments are separated by an en dash, or a hyphen with spaces around it. */
    private static final Pattern LANDLORD_SEPARATOR = Pattern.compile("\\s*–\\s*|\\s+-\\s+");

    /** Address segments may also be separated by a hyphen with no spaces, as in "709 12th St S- Inquire". */
    private static final Pattern ADDRESS_SEPARATOR = Pattern.compile("\\s*[–-]\\s*");

    /** A street address starts with a house number followed by a word that is not a bed/bath token. */
    private static final Pattern STREET_START = Pattern.compile(
            "^\\d+\\s+(?!(?:bedrooms?|beds?|br|bd|b|bathrooms?|baths?|ba|studio|efficiency)\\b)\\S.*$",
            Pattern.CASE_INSENSITIVE);

    /** A segment is a street only if it also ends with a street-type word; this keeps "608 Rentals" out. */
    private static final Set<String> STREET_SUFFIXES = Set.of(
            "st", "street", "ave", "avenue", "dr", "drive", "blvd", "boulevard", "rd", "road",
            "ct", "court", "pl", "place", "ln", "lane", "way", "ter", "terrace", "cir", "circle",
            "pkwy", "hwy");

    private static final Map<String, String> STREET_WORDS = Map.ofEntries(
            Map.entry("north", "n"), Map.entry("south", "s"),
            Map.entry("east", "e"), Map.entry("west", "w"),
            Map.entry("street", "st"), Map.entry("avenue", "ave"),
            Map.entry("road", "rd"), Map.entry("drive", "dr"),
            Map.entry("boulevard", "blvd"), Map.entry("place", "pl"),
            Map.entry("court", "ct"), Map.entry("lane", "ln"),
            Map.entry("terrace", "ter"), Map.entry("circle", "cir"));

    private static final Set<String> COMPANY_WORDS =
            Set.of("llc", "inc", "ltd", "co", "corp", "company", "the");

    /** City and state text that a breadcrumb address may carry after the street. */
    private static final Pattern CITY_SUFFIX = Pattern.compile(
            "\\s+(la crosse|onalaska|holmen|west salem|la crescent|bangor|sparta|westby|coon valley)$");

    private LandlordAddressAudit() {
    }

    public static Result check(Apartment apt) {
        String titleText = BedBathParser.stripSiteSuffix(apt.getRawTitle());
        String titleLandlord = titleLandlord(titleText);
        String fieldLandlord = apt.getFeature(FeatureType.LANDLORD);
        String titleAddress = titleAddress(titleText);
        String fieldAddress = apt.getAddress();
        return new Result(
                titleLandlord,
                fieldLandlord,
                compareLandlord(titleLandlord, fieldLandlord),
                titleAddress,
                fieldAddress,
                compareAddress(titleAddress, fieldAddress));
    }

    /**
     * @return the text before the first separator, or null if it looks like a bed/bath or address segment
     */
    static String titleLandlord(String titleText) {
        if (titleText == null || titleText.isBlank()) {
            return null;
        }
        String first = LANDLORD_SEPARATOR.split(titleText.strip(), 2)[0].strip();
        if (first.isEmpty() || looksLikeStreet(first)) {
            return null;
        }
        BedBathParser.Counts counts = BedBathParser.parseTitle(first);
        if (counts.baths() != null) {
            return null;
        }
        if (counts.beds() != null && Character.isDigit(first.charAt(0))) {
            return null;
        }
        return first;
    }

    /**
     * @return the first title segment that looks like a street address, or null
     */
    static String titleAddress(String titleText) {
        if (titleText == null || titleText.isBlank()) {
            return null;
        }
        for (String segment : ADDRESS_SEPARATOR.split(titleText.strip())) {
            String trimmed = segment.strip();
            if (looksLikeStreet(trimmed)) {
                return trimmed;
            }
        }
        return null;
    }

    private static boolean looksLikeStreet(String segment) {
        if (!STREET_START.matcher(segment).matches()) {
            return false;
        }
        for (String token : segment.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip().split(" ")) {
            if (STREET_SUFFIXES.contains(token)) {
                return true;
            }
        }
        return false;
    }

    static Check compareLandlord(String titleLandlord, String fieldLandlord) {
        String title = normalizeLandlord(titleLandlord);
        if (title.isEmpty()) {
            return Check.TITLE_MISSING;
        }
        String field = normalizeLandlord(fieldLandlord);
        if (field.isEmpty()) {
            return Check.FIELD_MISSING;
        }
        return containsWords(title, field) || containsWords(field, title) ? Check.AGREE : Check.CONFLICT;
    }

    static Check compareAddress(String titleAddress, String fieldAddress) {
        String title = normalizeAddress(titleAddress);
        if (title.isEmpty()) {
            return Check.TITLE_MISSING;
        }
        String field = normalizeAddress(stripCityState(fieldAddress));
        if (field.isEmpty()) {
            return Check.FIELD_MISSING;
        }
        boolean sameNumber = title.split(" ")[0].equals(field.split(" ")[0]);
        boolean overlaps = containsWords(title, field) || containsWords(field, title);
        return sameNumber && overlaps ? Check.AGREE : Check.CONFLICT;
    }

    static String normalizeLandlord(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String token : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip().split(" ")) {
            if (!token.isEmpty() && !COMPANY_WORDS.contains(token)) {
                out.append(token).append(' ');
            }
        }
        return out.toString().strip();
    }

    static String normalizeAddress(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (String token : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip().split(" ")) {
            out.append(STREET_WORDS.getOrDefault(token, token)).append(' ');
        }
        return out.toString().strip();
    }

    private static String stripCityState(String address) {
        if (address == null) {
            return null;
        }
        String text = address.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").strip();
        text = text.replaceAll("\\s+wi$", "");
        text = CITY_SUFFIX.matcher(text).replaceAll("");
        return text;
    }

    /**
     * True when the shorter word sequence appears as a whole-word run inside the longer one.
     */
    private static boolean containsWords(String longer, String shorter) {
        return (" " + longer + " ").contains(" " + shorter + " ");
    }
}
