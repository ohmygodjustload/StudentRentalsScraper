/**
 * Resolves Bed/Bath conflicts between the listing title and the site's Bed/Bath field.
 * Every disagreement is flagged. A hand-verified override replaces the field only when
 * the raw field still equals the value the override was verified against; otherwise the
 * override is skipped and flagged as stale so a human can review it.
 *
 * @author Andrew Peirce
 */
package clean;

import clean.BedBathOverrides.Override;
import clean.BedBathParser.Counts;
import models.Apartment;
import models.FeatureType;
import models.Flag;

import java.util.List;
import java.util.Map;

public final class BedBathResolver {

    private final Map<String, Override> overrides;

    public BedBathResolver(Map<String, Override> overrides) {
        this.overrides = overrides;
    }

    /**
     * Applies overrides and flags conflicts in place. Runs after title fallbacks and before cleaning.
     *
     * @return the same list, for chaining
     */
    public List<Apartment> resolve(List<Apartment> apartments) {
        for (Apartment apt : apartments) {
            resolveOne(apt);
        }
        long conflicts = apartments.stream().filter(a -> a.getFlags().contains(Flag.BEDBATH_CONFLICT)).count();
        long applied = apartments.stream().filter(a -> a.getFlags().contains(Flag.BEDBATH_OVERRIDDEN)).count();
        long stale = apartments.stream().filter(a -> a.getFlags().contains(Flag.BEDBATH_OVERRIDE_STALE)).count();
        System.out.println("[BedBath] Conflicts flagged: " + conflicts
                + ", overrides applied: " + applied + ", stale overrides skipped: " + stale);
        return apartments;
    }

    private void resolveOne(Apartment apt) {
        String rawField = apt.getFeature(FeatureType.BED_BATH);
        Counts title = BedBathParser.parseTitle(BedBathParser.stripSiteSuffix(apt.getRawTitle()));
        Counts field = BedBathParser.parseField(rawField);

        boolean conflict = BedBathParser.disagree(title.beds(), field.beds())
                || BedBathParser.disagree(title.baths(), field.baths());
        if (conflict) {
            apt.addFlag(Flag.BEDBATH_CONFLICT);
        }

        Override override = overrides.get(apt.getId());
        if (override == null) {
            return;
        }
        if (sameField(rawField, override.expectedField())) {
            apt.setFeature(FeatureType.BED_BATH, override.formatted());
            apt.addFlag(Flag.BEDBATH_OVERRIDDEN);
        } else {
            apt.addFlag(Flag.BEDBATH_OVERRIDE_STALE);
            System.out.println("[BedBath] Warning: override for listing " + apt.getId()
                    + " skipped. Site field is '" + rawField + "' but the override was verified against '"
                    + override.expectedField() + "'. Review data/bedbath_overrides.csv.");
        }
    }

    private static boolean sameField(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return normalizeField(actual).equalsIgnoreCase(normalizeField(expected));
    }

    private static String normalizeField(String text) {
        return text.replaceAll("\\s+", "");
    }
}
