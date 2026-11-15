/**
 * Assigns each listing a 0-100 deal score from price per person, location, and amenities,
 * minus a capped penalty for missing or conflicting data. Every score carries a breakdown
 * string so it can be explained.
 *
 * <p>Points: price 50, location 30 (walk 15, bike 5, distance 10),
 * amenities 20 (AC 5, laundry 5, parking 4, dishwasher 3, utilities 3).
 * Penalty: up to 15 (missing rent or bedrooms 8, no coordinates 4, unresolved bed/bath conflict 3).
 *
 * @author Andrew Peirce
 */
package scoring;

import models.Apartment;
import models.FeatureType;
import models.Flag;

import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class DealScorer {

    static final int PRICE_MAX = 50;
    static final int WALK_MAX = 15;
    static final int BIKE_MAX = 5;
    static final int DISTANCE_MAX = 10;
    static final int LOCATION_MAX = WALK_MAX + BIKE_MAX + DISTANCE_MAX;
    static final int AC_MAX = 5;
    static final int LAUNDRY_MAX = 5;
    static final int PARKING_MAX = 4;
    static final int DISHWASHER_MAX = 3;
    static final int UTILITIES_MAX = 3;
    static final int AMENITY_MAX = AC_MAX + LAUNDRY_MAX + PARKING_MAX + DISHWASHER_MAX + UTILITIES_MAX;
    static final int PENALTY_CAP = 15;
    static final int PENALTY_MISSING_RENT_OR_BEDS = 8;
    static final int PENALTY_NO_COORDINATES = 4;
    static final int PENALTY_BEDBATH_CONFLICT = 3;

    private static final Pattern AC_POSITIVE = Pattern.compile("central|wall|window|mounted|yes");
    private static final Pattern LAUNDRY_HOOKUPS = Pattern.compile("hook");
    private static final Pattern LAUNDRY_IN_UNIT = Pattern.compile(
            "in[- ]?unit|in each unit|every unit|in apt|in the unit");
    private static final Pattern LAUNDRY_SHARED = Pattern.compile(
            "on[- ]?site|in[- ]?building|coin|free|washer|dryer|w/d|w&d|laundry|each floor");
    private static final Pattern PARKING_OFF_STREET = Pattern.compile(
            "off[- ]?st|garage|carport|on[- ]?site|driveway");
    private static final Pattern PARKING_ON_STREET = Pattern.compile("street");
    private static final Pattern UTILITIES_HEAT = Pattern.compile("heat|electric");
    private static final Pattern UTILITIES_WATER = Pattern.compile("water|sewer");

    private final ScoringConfig config;

    public DealScorer(ScoringConfig config) {
        this.config = config;
    }

    /**
     * Sets {@code dealScore} and {@code dealScoreDetail} on every listing.
     */
    public void score(List<Apartment> apartments) {
        for (Apartment apt : apartments) {
            Breakdown breakdown = breakdown(apt);
            apt.setDealScore(breakdown.total());
            apt.setDealScoreDetail(breakdown.describe());
        }
    }

    Breakdown breakdown(Apartment apt) {
        return new Breakdown(priceScore(apt), locationScore(apt), amenityScore(apt), penalty(apt));
    }

    /**
     * Points for price per person. Listings without a rent or bedroom count get 0.
     */
    int priceScore(Apartment apt) {
        Integer perPerson = apt.getPricePerPerson();
        if (perPerson == null) {
            return 0;
        }
        return linear(perPerson, config.priceFullAt(), config.priceZeroAt(), PRICE_MAX);
    }

    /**
     * Points for walk time, bike time, and distance to campus. A null travel time on a listing
     * with coordinates means it was beyond the TravelTime cap, so it earns 0 points with no penalty.
     */
    int locationScore(Apartment apt) {
        return walkPoints(apt) + bikePoints(apt) + distancePoints(apt);
    }

    private int walkPoints(Apartment apt) {
        Integer seconds = apt.getWalkTravelTimeSeconds();
        return seconds == null ? 0
                : linear(seconds / 60.0, config.walkFullMinutes(), config.walkZeroMinutes(), WALK_MAX);
    }

    private int bikePoints(Apartment apt) {
        Integer seconds = apt.getBikeTravelTimeSeconds();
        return seconds == null ? 0
                : linear(seconds / 60.0, config.bikeFullMinutes(), config.bikeZeroMinutes(), BIKE_MAX);
    }

    private int distancePoints(Apartment apt) {
        Double miles = apt.getDistanceToCampus();
        return miles == null ? 0
                : linear(miles, config.distanceFullMiles(), config.distanceZeroMiles(), DISTANCE_MAX);
    }

    /**
     * Points for amenities, matched by keyword against the messy free-text site fields.
     */
    int amenityScore(Apartment apt) {
        return acPoints(text(apt, FeatureType.AIR_CONDITIONING))
                + laundryPoints(text(apt, FeatureType.LAUNDRY))
                + parkingPoints(text(apt, FeatureType.PARKING))
                + (text(apt, FeatureType.INCLUDED).contains("dishwasher") ? DISHWASHER_MAX : 0)
                + utilitiesPoints(text(apt, FeatureType.INCLUDED));
    }

    static int acPoints(String ac) {
        return AC_POSITIVE.matcher(ac).find() ? AC_MAX : 0;
    }

    static int laundryPoints(String laundry) {
        if (laundry.isEmpty() || laundry.equals("-") || laundry.equals("none")) {
            return 0;
        }
        if (LAUNDRY_IN_UNIT.matcher(laundry).find()) {
            return LAUNDRY_MAX;
        }
        if (LAUNDRY_HOOKUPS.matcher(laundry).find()) {
            return 0;
        }
        return LAUNDRY_SHARED.matcher(laundry).find() ? 3 : 0;
    }

    static int parkingPoints(String parking) {
        if (PARKING_OFF_STREET.matcher(parking).find()) {
            return PARKING_MAX;
        }
        return PARKING_ON_STREET.matcher(parking).find() ? 1 : 0;
    }

    static int utilitiesPoints(String included) {
        if (UTILITIES_HEAT.matcher(included).find()) {
            return UTILITIES_MAX;
        }
        return UTILITIES_WATER.matcher(included).find() ? 1 : 0;
    }

    /**
     * Penalty for missing or uncertain data, capped at {@link #PENALTY_CAP}.
     */
    int penalty(Apartment apt) {
        int points = 0;
        if (apt.getRentMonthly() == null || apt.getBedrooms() == null) {
            points += PENALTY_MISSING_RENT_OR_BEDS;
        }
        if (!apt.hasCoordinates()) {
            points += PENALTY_NO_COORDINATES;
        }
        boolean conflict = apt.getFlags().contains(Flag.BEDBATH_CONFLICT);
        boolean overridden = apt.getFlags().contains(Flag.BEDBATH_OVERRIDDEN);
        if (conflict && !overridden) {
            points += PENALTY_BEDBATH_CONFLICT;
        }
        return Math.min(points, PENALTY_CAP);
    }

    /**
     * Prints the score distribution and the top 10 listings.
     */
    public static void printSummary(List<Apartment> apartments, PrintStream out) {
        long scored = apartments.stream().filter(a -> a.getDealScore() != null).count();
        out.println("[Scoring] Scored " + scored + " of " + apartments.size() + " listings");

        int[] buckets = new int[5];
        for (Apartment apt : apartments) {
            if (apt.getDealScore() != null) {
                int bucket = Math.min(4, Math.max(0, apt.getDealScore() / 20));
                buckets[bucket]++;
            }
        }
        String[] labels = {"0-19", "20-39", "40-59", "60-79", "80-100"};
        out.println("Score distribution:");
        for (int i = 0; i < buckets.length; i++) {
            out.printf("  %-7s %d%n", labels[i], buckets[i]);
        }

        out.println("Top 10:");
        apartments.stream()
                .filter(a -> a.getDealScore() != null)
                .sorted(Comparator.comparing(Apartment::getDealScore).reversed()
                        .thenComparing(Apartment::getId))
                .limit(10)
                .forEach(a -> out.printf("  %-5s score %3d  %-12s  $%-5s/person  %s%n",
                        a.getId(), a.getDealScore(), a.getBedBath(),
                        a.getPricePerPerson() == null ? "-" : a.getPricePerPerson().toString(),
                        a.getAddress()));
    }

    /**
     * Points for each component, plus the total.
     */
    record Breakdown(int price, int location, int amenities, int penalty) {

        int total() {
            return Math.max(0, Math.min(100, price + location + amenities - penalty));
        }

        String describe() {
            String penaltyText = penalty == 0 ? "0" : "-" + penalty;
            return String.format("price %d/%d, location %d/%d, amenities %d/%d, penalty %s",
                    price, PRICE_MAX, location, LOCATION_MAX, amenities, AMENITY_MAX, penaltyText);
        }
    }

    /**
     * Linear interpolation: full points at or below {@code full}, zero at or above {@code zero}.
     */
    static int linear(double value, double full, double zero, int max) {
        if (value <= full) {
            return max;
        }
        if (value >= zero) {
            return 0;
        }
        return (int) Math.round(max * (zero - value) / (zero - full));
    }

    private static String text(Apartment apt, FeatureType type) {
        String value = apt.getFeature(type);
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }
}
