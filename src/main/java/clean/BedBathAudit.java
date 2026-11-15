/**
 * Read-only comparison of bed/bath counts from three sources on each listing page:
 * the title, the site's Bed/Bath feature field, and the free-text description.
 * Used to decide which source to trust; it never modifies listings.
 *
 * @author Andrew Peirce
 */
package clean;

import clean.BedBathParser.Counts;
import models.Apartment;
import models.FeatureType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BedBathAudit {

    private static final String NUMBER =
            "\\d+(?:\\.\\d+)?|one|two|three|four|five|six|seven|eight|nine|ten";

    private static final Map<String, Double> NUMBER_WORDS = Map.of(
            "one", 1.0, "two", 2.0, "three", 3.0, "four", 4.0, "five", 5.0,
            "six", 6.0, "seven", 7.0, "eight", 8.0, "nine", 9.0, "ten", 10.0);

    private static final Pattern DESCRIPTION_BEDS = Pattern.compile(
            "(?<![\\w.$])(" + NUMBER + ")\\s*-?\\s*(?:bedrooms?|beds?|br|bd)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DESCRIPTION_BATHS = Pattern.compile(
            "(?<![\\w.$])(" + NUMBER + ")\\s*-?\\s*(?:full\\s+)?(?:bathrooms?|baths?|ba)\\b",
            Pattern.CASE_INSENSITIVE);

    public enum TitleVsField {
        AGREE, CONFLICT, TITLE_MISSING, FIELD_MISSING, BOTH_MISSING
    }

    public enum DescriptionMatch {
        MATCHES_BOTH, MATCHES_TITLE, MATCHES_FIELD, MATCHES_NEITHER, MATCHES_ONLY_SOURCE, AMBIGUOUS, MISSING
    }

    public record DescriptionCounts(Set<Double> beds, Set<Double> baths, List<String> phrases) {
    }

    public record Row(
            Apartment apartment,
            String titleText,
            String fieldText,
            Counts title,
            Counts field,
            DescriptionCounts description,
            TitleVsField bedTitleVsField,
            DescriptionMatch bedDescription,
            TitleVsField bathTitleVsField,
            DescriptionMatch bathDescription) {
    }

    public List<Row> audit(List<Apartment> apartments) {
        List<Row> rows = new ArrayList<>();
        for (Apartment apt : apartments) {
            rows.add(auditOne(apt));
        }
        return rows;
    }

    public Row auditOne(Apartment apt) {
        String titleText = BedBathParser.stripSiteSuffix(apt.getRawTitle());
        String fieldText = apt.getFeature(FeatureType.BED_BATH);
        Counts title = BedBathParser.parseTitle(titleText);
        Counts field = BedBathParser.parseField(fieldText);
        DescriptionCounts description = parseDescription(apt.getRawDescription());

        return new Row(
                apt,
                titleText,
                fieldText,
                title,
                field,
                description,
                compare(title.beds(), field.beds()),
                matchDescription(description.beds(), title.beds(), field.beds()),
                compare(title.baths(), field.baths()),
                matchDescription(description.baths(), title.baths(), field.baths()));
    }

    static DescriptionCounts parseDescription(String description) {
        Set<Double> beds = new LinkedHashSet<>();
        Set<Double> baths = new LinkedHashSet<>();
        List<String> phrases = new ArrayList<>();
        if (description == null || description.isBlank()) {
            return new DescriptionCounts(beds, baths, phrases);
        }
        collect(DESCRIPTION_BEDS, description, beds, phrases);
        collect(DESCRIPTION_BATHS, description, baths, phrases);
        return new DescriptionCounts(beds, baths, phrases);
    }

    private static void collect(Pattern pattern, String text, Set<Double> values, List<String> phrases) {
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            values.add(toNumber(m.group(1)));
            phrases.add(m.group().strip());
        }
    }

    private static Double toNumber(String token) {
        Double word = NUMBER_WORDS.get(token.toLowerCase(Locale.ROOT));
        return word != null ? word : Double.valueOf(token);
    }

    static TitleVsField compare(Double title, Double field) {
        if (title == null && field == null) {
            return TitleVsField.BOTH_MISSING;
        }
        if (title == null) {
            return TitleVsField.TITLE_MISSING;
        }
        if (field == null) {
            return TitleVsField.FIELD_MISSING;
        }
        return title.equals(field) ? TitleVsField.AGREE : TitleVsField.CONFLICT;
    }

    static DescriptionMatch matchDescription(Set<Double> description, Double title, Double field) {
        if (description.isEmpty()) {
            return DescriptionMatch.MISSING;
        }
        if (description.size() > 1) {
            return DescriptionMatch.AMBIGUOUS;
        }
        Double value = description.iterator().next();
        boolean matchesTitle = value.equals(title);
        boolean matchesField = value.equals(field);
        if (title == null || field == null) {
            return matchesTitle || matchesField ? DescriptionMatch.MATCHES_ONLY_SOURCE : DescriptionMatch.MATCHES_NEITHER;
        }
        if (matchesTitle && matchesField) {
            return DescriptionMatch.MATCHES_BOTH;
        }
        if (matchesTitle) {
            return DescriptionMatch.MATCHES_TITLE;
        }
        if (matchesField) {
            return DescriptionMatch.MATCHES_FIELD;
        }
        return DescriptionMatch.MATCHES_NEITHER;
    }
}
