/**
 * Applies title-based fallbacks for missing landlord and bed/bath values,
 * and records data-quality flags. Runs after scrape and before cleaning.
 *
 * @author Andrew Peirce
 */
package clean;

import models.Apartment;
import models.FeatureType;
import models.Flag;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApartmentPostProcessor {

    private static final Pattern TITLE_PATTERN = Pattern.compile(
            "^(.*?)\\s*[\\–\\-]\\s*((?:Studio|\\d+(?:\\.\\d+)?)\\s*(?:br|bd|bed)?\\s*/?\\s*\\d*(?:\\.\\d+)?\\s*(?:ba|bath)?).*",
            Pattern.CASE_INSENSITIVE);

    public List<Apartment> process(List<Apartment> apartments) {
        for (Apartment apt : apartments) {
            processOne(apt);
        }
        return apartments;
    }

    private void processOne(Apartment apt) {
        String rawTitle = apt.getRawTitle();
        if (rawTitle == null || rawTitle.isBlank()) {
            // Older raw JSON already contains fallback values; leave unchanged.
            return;
        }

        String cleanedTitle = cleanTitle(rawTitle);
        String[] titleParts = parseTitle(cleanedTitle);

        if (apt.getFeature(FeatureType.LANDLORD) == null) {
            if (titleParts != null && titleParts[0] != null && !titleParts[0].isBlank()) {
                apt.setFeature(FeatureType.LANDLORD, titleParts[0]);
                apt.addFlag(Flag.LANDLORD_FROM_TITLE);
            } else {
                apt.addFlag(Flag.MISSING_LANDLORD);
            }
        }

        if (apt.getFeature(FeatureType.BED_BATH) == null) {
            if (titleParts != null && titleParts[1] != null && !titleParts[1].isBlank()) {
                apt.setFeature(FeatureType.BED_BATH, titleParts[1]);
                apt.addFlag(Flag.BEDBATH_FROM_TITLE);
            } else {
                apt.addFlag(Flag.MISSING_BEDBATH);
            }
        }
    }

    private String cleanTitle(String input) {
        return input.replace("â€“", "-")
                .replaceAll("\\|.*", "")
                .strip();
    }

    String[] parseTitle(String rawTitle) {
        Matcher matcher = TITLE_PATTERN.matcher(rawTitle);
        if (!matcher.find()) {
            return null;
        }
        return new String[] {
                matcher.group(1).strip(),
                matcher.group(2).strip()
        };
    }
}
