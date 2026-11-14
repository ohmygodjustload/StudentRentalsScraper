/**
 * Normalizes address, landlord, bed/bath, and rent fields on scraped listings.
 *
 * @author Andrew Peirce
 */
package clean;

import models.Apartment;
import models.FeatureType;
import models.Flag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DataCleaner {

    private static final Map<String, String> LANDLORD_MAP = new HashMap<>();
    static {
        LANDLORD_MAP.put("Wise Properties", "Wise Properties");
        LANDLORD_MAP.put("Miller Properties", "Miller Properties");
        LANDLORD_MAP.put("Parker Properties", "Parker Properties");
        LANDLORD_MAP.put("EKIM-PT", "EKIM Properties");
        LANDLORD_MAP.put("EKIM Properties", "EKIM Properties");
        LANDLORD_MAP.put("Off-Campus La Crosse, LLC", "Off-Campus La Crosse LLC");
        LANDLORD_MAP.put("Off-Campus La Crosse Inc", "Off-Campus La Crosse LLC");
        LANDLORD_MAP.put("Off-Campus La Crosse", "Off-Campus La Crosse LLC");
        LANDLORD_MAP.put("Off-Campus La Crosse LLC", "Off-Campus La Crosse LLC");
        LANDLORD_MAP.put("Benson Properties", "Benson Properties");
        LANDLORD_MAP.put("Benson Management", "Benson Properties");
        LANDLORD_MAP.put("JCR Properties", "JCR Properties");
        LANDLORD_MAP.put("Favre Rentals LLC", "Favre Rentals");
        LANDLORD_MAP.put("Favre Retnals", "Favre Rentals");
        LANDLORD_MAP.put("Three Sixty Real Estate", "Three Sixty");
        LANDLORD_MAP.put("Three Sixty", "Three Sixty");
        LANDLORD_MAP.put("608 Rental Properties", "608 Properties");
        LANDLORD_MAP.put("608 Rental Properteis", "608 Properties");
        LANDLORD_MAP.put("608 Properties", "608 Properties");
        LANDLORD_MAP.put("608 Properties LLC", "608 Properties");
        LANDLORD_MAP.put("608 Rentals", "608 Properties");
        LANDLORD_MAP.put("Wrobel Properties", "Wrobel Rentals");
        LANDLORD_MAP.put("Wrobel Rentals", "Wrobel Rentals");
        LANDLORD_MAP.put("Coleman Properties", "Coleman Properties");
        LANDLORD_MAP.put("Colemen Properties", "Coleman Properties");
        LANDLORD_MAP.put("JTH Homes LLC", "JTH Homes");
        LANDLORD_MAP.put("RK&D Properties", "RK&D Properties");
        LANDLORD_MAP.put("RK&D LLC1", "RK&D Properties");
        LANDLORD_MAP.put("Dillaber Properties", "Dillaber Properties");
        LANDLORD_MAP.put("Dillabar Properties", "Dillaber Properties");
        LANDLORD_MAP.put("Sawyer Properties", "Sawyer Properties");
        LANDLORD_MAP.put("Sawyer Rentals", "Sawyer Properties");
        LANDLORD_MAP.put("Pedace", "Pedace Properties");
        LANDLORD_MAP.put("Pedace Properties", "Pedace Properties");
        LANDLORD_MAP.put("Gander Rentals", "Gander Rentals");
        LANDLORD_MAP.put("Gander Rantals", "Gander Rentals");
        LANDLORD_MAP.put("Resurrection Rentals", "Resurrection Rentals");
        LANDLORD_MAP.put("Resurrection Properties", "Resurrection Rentals");
        LANDLORD_MAP.put("Munson Realty", "Munson Realty");
        LANDLORD_MAP.put("Common Ground Property", "Munson Realty");
        LANDLORD_MAP.put("AptsLax LLC", "AptsLax LLC");
        LANDLORD_MAP.put("Mickelson Properties", "Mickelson Properties");
        LANDLORD_MAP.put("Clarkin Properties", "Clarkin Properties");
        LANDLORD_MAP.put("Gerrard Properties", "Gerrard Properties");
        LANDLORD_MAP.put("Baumgartner Properties", "Baumgartner Properties");
        LANDLORD_MAP.put("Prairie Properties", "Prairie Properties");
        LANDLORD_MAP.put("Cappuccio Rentals", "Cappuccio Rentals");
        LANDLORD_MAP.put("MQuick Properties", "MQuick Properties");
        LANDLORD_MAP.put("Biondo Properties", "Biondo Properties");
        LANDLORD_MAP.put("Legends Living", "Legends Living");
        LANDLORD_MAP.put("Classic Housing LLC", "Classic Housing LLC");
        LANDLORD_MAP.put("JCH Rentals", "JCH Rentals");
        LANDLORD_MAP.put("JAMAR Properties", "JAMAR Properties");
        LANDLORD_MAP.put("JAMAR", "JAMAR Properties");
        LANDLORD_MAP.put("Hilton Properties", "Hilton Properties");
        LANDLORD_MAP.put("HNT Properties", "HNT Properties");
        LANDLORD_MAP.put("MAT Properties, LLC", "MAT Properties");
        LANDLORD_MAP.put("Mack Properties", "Mack Properties");
        LANDLORD_MAP.put("Casey Properties", "Casey Properties");
        LANDLORD_MAP.put("Weis Rentals", "Weis Rentals");
        LANDLORD_MAP.put("Wedgewood Commons", "Wedgewood Commons");
        LANDLORD_MAP.put("Alkar Apartments", "Alkar Apartments");
        LANDLORD_MAP.put("Clott Rentals", "Clott Rentals");
        LANDLORD_MAP.put("McKeogh Properties", "McKeogh Properties");
        LANDLORD_MAP.put("Buchner Properties", "Buchner Properties");
        LANDLORD_MAP.put("Karbin Properties", "Karbin Properties");
        LANDLORD_MAP.put("Hougom Properties", "Hougom Properties");
        LANDLORD_MAP.put("DBB Properties", "DBB Properties");
        LANDLORD_MAP.put("PK Investments", "PK Investments");
        LANDLORD_MAP.put("Lesniewski LLC", "Lesniewski LLC");
        LANDLORD_MAP.put("G&N Properties", "G&N Properties");
        LANDLORD_MAP.put("Mack Highland Properties", "Mack Properties");
    }

    private static final Map<String, String> ADDRESS_CORRECTIONS = Map.ofEntries(
            Map.entry("420 9th St N La Crosse", "420 9th St N La Crosse WI"),
            Map.entry("128 9th St N La Crosse", "128 9th St N La Crosse WI"),
            Map.entry("509 12th St N La Crosse", "509 12th St N La Crosse WI"),
            Map.entry("101 West Ave N La Crosse", "101 West Ave N La Crosse WI"),
            Map.entry("62410th St N La Crosse WI", "624 10th St N La Crosse WI"),
            Map.entry("330 21th St N La Crosse WI", "330 21st St N La Crosse WI"),
            Map.entry("821 State St #1 La Crosse WI", "821 State St, #1, La Crosse WI"),
            Map.entry("821 State St #2 La Crosse WI", "821 State St, #2, La Crosse WI"),
            Map.entry("821 State St #3 La Crosse WI", "821 State St, #3, La Crosse WI"),
            Map.entry("821 State St #4 La Crosse WI", "821 State St, #4, La Crosse WI"),
            Map.entry("1003 La Crosse St A La Crosse WI", "1003 La Crosse St, Unit A, La Crosse WI"),
            Map.entry("1003 La Crosse St B La Crosse WI", "1003 La Crosse St, Unit B, La Crosse WI"),
            Map.entry("509 11th St N Dwn La Crosse WI", "509 11th St N, Down, La Crosse WI"),
            Map.entry("1118 La Crosse St Dwn La Crosse WI", "1118 La Crosse St, Down, La Crosse WI"),
            Map.entry("1801 Cameron Ave Dwn La Crosse WI", "1801 Cameron Ave, Down, La Crosse WI"),
            Map.entry("1915 State St Dwn La Crosse WI", "1915 State St, Down, La Crosse WI"),
            Map.entry("1917 State St Up La Crosse WI", "1917 State St, Up, La Crosse WI"),
            Map.entry("1308 State St Dwn La Crosse WI", "1308 State St, Down, La Crosse WI"),
            Map.entry("1310 State St Up La Crosse WI", "1310 State St, Up, La Crosse WI"),
            Map.entry("624 8th St S Down La Crosse WI", "624 8th St S, Down, La Crosse WI"),
            Map.entry("624 8th St S Up La Crosse WI", "624 8th St S, Up, La Crosse WI"),
            Map.entry("509 11th St N Up La Crosse WI", "509 11th St N, Up, La Crosse WI"),
            Map.entry("218 11th St N-RENTED La Crosse WI", "218 11th St N, La Crosse WI"),
            Map.entry("222 11th St N-RENTED La Crosse WI", "222 11th St N, La Crosse WI"),
            Map.entry("230 11th St N-RENTED La Crosse WI", "230 11th St N, La Crosse WI"),
            Map.entry("1108 Vine St-RENTED La Crosse WI", "1108 Vine St, La Crosse WI")
    );

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
            return Integer.parseInt(matcher.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    String normalizeAddress(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            return "";
        }

        String address = rawAddress.strip().replaceAll("\\s+", " ");

        if (ADDRESS_CORRECTIONS.containsKey(address)) {
            return ADDRESS_CORRECTIONS.get(address);
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
        return LANDLORD_MAP.getOrDefault(key, key);
    }
}
