/**
 * Normalizes address, landlord, bed/bath, and rent fields on scraped listings.
 *
 * @author Andrew Peirce
 */
package clean;

import models.Apartment;
import models.FeatureType;
import models.Flag;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataCleaner {

    /** Default locations, relative to the working directory. */
    public static final Path LANDLORD_FILE = Path.of("data", "landlord_aliases.csv");
    public static final Path ADDRESS_FILE = Path.of("data", "address_corrections.csv");

    private final Map<String, String> landlordAliases;
    private final Map<String, String> addressCorrections;

    /**
     * Loads the lookup tables from {@link #LANDLORD_FILE} and {@link #ADDRESS_FILE}.
     *
     * @throws UncheckedIOException if either file is missing or unreadable
     */
    public DataCleaner() {
        this(loadLandlordAliases(), loadAddressCorrections());
    }

    public DataCleaner(Map<String, String> landlordAliases, Map<String, String> addressCorrections) {
        this.landlordAliases = Map.copyOf(landlordAliases);
        this.addressCorrections = Map.copyOf(addressCorrections);
    }

    /**
     * @throws IOException if either lookup file is missing or unreadable
     * @throws IllegalArgumentException if either file is malformed (message names file and line)
     */
    public static DataCleaner loadDefault() throws IOException {
        return new DataCleaner(
                KeyValueCsv.load(LANDLORD_FILE, "alias", "canonical"),
                KeyValueCsv.load(ADDRESS_FILE, "raw", "corrected"));
    }

    private static Map<String, String> loadLandlordAliases() {
        try {
            return KeyValueCsv.load(LANDLORD_FILE, "alias", "canonical");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + LANDLORD_FILE, e);
        }
    }

    private static Map<String, String> loadAddressCorrections() {
        try {
            return KeyValueCsv.load(ADDRESS_FILE, "raw", "corrected");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + ADDRESS_FILE, e);
        }
    }

    public int landlordAliasCount() {
        return landlordAliases.size();
    }

    public int addressCorrectionCount() {
        return addressCorrections.size();
    }

    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "(\\d+)\\s+(.+?)\\s+([A-Za-z]+(?: [A-Za-z]+)?)\\s+([A-Z]{2})$");

    private static final Pattern PRICE_PATTERN = Pattern.compile("\\$?([\\d,]+)");

    private static final Pattern SLASH_BED_BATH = Pattern.compile(
            "(?i)^\\s*(studio|\\d+(?:\\.\\d+)?)\\s*(?:br|bd|bed(?:room)?s?)?\\s*/\\s*(\\d+(?:\\.\\d+)?)\\s*(?:ba|bath(?:room)?s?)?\\s*$");

    private static final Pattern STUDIO_BATH = Pattern.compile(
            "(?i)^\\s*studio\\s+(\\d+(?:\\.\\d+)?)\\s*(?:ba|bath(?:room)?s?)?\\s*$");

    public List<Apartment> clean(List<Apartment> apartments) {
        return apartments.stream()
                .map(this::cleanApartment)
                .collect(Collectors.toList());
    }

    private Apartment cleanApartment(Apartment apt) {
        apt.setAddress(normalizeAddress(apt.getAddress()));
        apt.setLandlord(normalizeLandlord(apt.getLandlord()));
        normalizeBedBath(apt);
        apt.setRentMonthly(parseRent(apt.getPrice()));
        return apt;
    }

    void normalizeBedBath(Apartment apt) {
        String raw = apt.getFeature(FeatureType.BED_BATH);
        if (raw == null || raw.isBlank()) {
            return;
        }

        String trimmed = raw.strip();
        Matcher slash = SLASH_BED_BATH.matcher(trimmed);
        Matcher studioOnly = STUDIO_BATH.matcher(trimmed);

        Double beds = null;
        Double baths = null;
        boolean isStudio = false;

        if (slash.matches()) {
            String bedPart = slash.group(1);
            String bathPart = slash.group(2);
            if ("studio".equalsIgnoreCase(bedPart)) {
                isStudio = true;
                beds = 0.0;
            } else {
                beds = Double.parseDouble(bedPart);
                if (beds == 0.0) {
                    isStudio = true;
                }
            }
            baths = Double.parseDouble(bathPart);
        } else if (studioOnly.matches()) {
            isStudio = true;
            beds = 0.0;
            baths = Double.parseDouble(studioOnly.group(1));
        } else {
            apt.addFlag(Flag.BEDBATH_UNPARSEABLE);
            return;
        }

        apt.setBedrooms(beds);
        apt.setBathrooms(baths);

        if (isStudio) {
            apt.addFlag(Flag.STUDIO);
        }
        if (beds != null && beds != Math.rint(beds)) {
            apt.addFlag(Flag.FRACTIONAL_BEDROOM);
        }
        if (baths != null && baths != Math.rint(baths)) {
            apt.addFlag(Flag.FRACTIONAL_BATHROOM);
        }
        if (beds != null && (beds < 0 || beds > 10)) {
            apt.addFlag(Flag.UNUSUAL_BEDROOM_COUNT);
        }

        String formatted;
        if (isStudio) {
            formatted = "Studio/" + formatNumber(baths) + "ba";
        } else {
            formatted = formatNumber(beds) + "bd/" + formatNumber(baths) + "ba";
        }
        apt.setFeature(FeatureType.BED_BATH, formatted);
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return Double.toString(value);
    }

    Integer parseRent(String price) {
        if (price == null || price.isBlank()) {
            return null;
        }
        Matcher matcher = PRICE_PATTERN.matcher(price.strip());
        if (!matcher.find()) {
            return null;
        }
        try {
            int rent = Integer.parseInt(matcher.group(1).replace(",", ""));
            // Sites show "$0" for "call for price"; treat that as unknown, not free.
            return rent > 0 ? rent : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    String normalizeAddress(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            return "";
        }

        String address = rawAddress.strip().replaceAll("\\s+", " ");

        if (addressCorrections.containsKey(address)) {
            return addressCorrections.get(address);
        }

        address = address
                .replaceAll("([a-zA-Z])(\\d)", "$1 $2")
                .replace(" N ", " North ")
                .replace(" S ", " South ")
                .replace(" E ", " East ")
                .replace(" W ", " West ")
                .replaceAll("(St|Ave|Blvd|Dr|Rd|Ct|Pl|Ln|Cir)\\s+([NESW])", "$1 $2");

        if (!address.matches(".*,?\\s*(WI|MN)$")) {
            if (address.contains("La Crescent")) {
                address += " MN";
            } else {
                address += " WI";
            }
        }

        Matcher matcher = ADDRESS_PATTERN.matcher(address);
        if (matcher.find()) {
            return String.format("%s %s %s %s",
                    matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4));
        }
        return address;
    }

    String normalizeLandlord(String rawLandlord) {
        if (rawLandlord == null || rawLandlord.isBlank() || "n/a".equalsIgnoreCase(rawLandlord)) {
            return "Unknown";
        }
        String key = rawLandlord.strip();
        return landlordAliases.getOrDefault(key, key);
    }
}
