/**
 * Shared parsing of bed/bath text from listing titles and the site's Bed/Bath field.
 * Used by the audit and by the override resolver so both read the text the same way.
 *
 * @author Andrew Peirce
 */
package clean;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BedBathParser {

    /**
     * Bed and bath counts read from one source. A null component means the source
     * did not state that count. A bed count of 0 means studio or efficiency.
     */
    public record Counts(Double beds, Double baths) {
        public static final Counts NONE = new Counts(null, null);
    }

    /** Matches "Studio", "Efficiency", "6b", "2br", "3 bedrooms" and similar; not "1ba". */
    private static final Pattern BEDS = Pattern.compile(
            "(?<![\\w.])(?:(studio|efficiency)|(\\d+(?:\\.\\d+)?)(?:\\s*(?:bedrooms?|beds?|br|bd)|b))(?![a-z])",
            Pattern.CASE_INSENSITIVE);

    /** Matches "1ba", "1.5 baths", "2 bathrooms". */
    private static final Pattern BATHS = Pattern.compile(
            "(?<![\\w.])(\\d+(?:\\.\\d+)?)\\s*(?:bathrooms?|baths?|ba)(?![a-z])",
            Pattern.CASE_INSENSITIVE);

    /** Matches the site field form "2/1", "Studio/1", "2br/1.5ba". */
    private static final Pattern SLASH_FIELD = Pattern.compile(
            "^\\s*(?:(studio|efficiency)|(\\d+(?:\\.\\d+)?)\\s*(?:bedrooms?|beds?|br|bd|b)?)\\s*/\\s*(\\d+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);

    private BedBathParser() {
    }

    /**
     * Removes the site suffix and "Rental Listing" text from a page title.
     */
    public static String stripSiteSuffix(String rawTitle) {
        if (rawTitle == null) {
            return null;
        }
        return rawTitle.replace("â€“", "–")
                .replaceAll("\\|.*", "")
                .replaceAll("\\s*Rental Listing\\s*$", "")
                .strip();
    }

    /**
     * Reads bed and bath counts from a listing title such as
     * "Lancer Village – 2br/1.5ba – 709 12th St S". Works on any text containing bed/bath tokens.
     */
    public static Counts parseTitle(String title) {
        if (title == null || title.isBlank()) {
            return Counts.NONE;
        }
        Matcher beds = BEDS.matcher(title);
        Double bedCount = null;
        if (beds.find()) {
            bedCount = beds.group(1) != null ? 0.0 : Double.valueOf(beds.group(2));
        }
        Matcher baths = BATHS.matcher(title);
        Double bathCount = baths.find() ? Double.valueOf(baths.group(1)) : null;
        return new Counts(bedCount, bathCount);
    }

    /**
     * Reads bed and bath counts from the site's Bed/Bath field. Accepts the slash form
     * ("2/1", "Studio/1") and falls back to the title form ("Studio 1 bath", "3br").
     */
    public static Counts parseField(String field) {
        if (field == null || field.isBlank()) {
            return Counts.NONE;
        }
        Matcher slash = SLASH_FIELD.matcher(field);
        if (slash.find()) {
            Double beds = slash.group(1) != null ? 0.0 : Double.valueOf(slash.group(2));
            return new Counts(beds, Double.valueOf(slash.group(3)));
        }
        return parseTitle(field);
    }

    /**
     * @return true only when both sides state a count and the counts differ
     */
    public static boolean disagree(Double a, Double b) {
        return a != null && b != null && !a.equals(b);
    }
}
