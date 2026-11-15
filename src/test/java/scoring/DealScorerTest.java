package scoring;

import models.Apartment;
import models.FeatureType;
import models.Flag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class DealScorerTest {

    private final DealScorer scorer = new DealScorer(ScoringConfig.defaults());

    /** A listing with rent, bedrooms, and coordinates, and no amenities or travel times. */
    private static Apartment listing(Integer rent, Double beds) {
        Apartment apt = new Apartment("1", "https://example.com/1");
        apt.setRentMonthly(rent);
        apt.setBedrooms(beds);
        apt.setLatitude(43.8);
        apt.setLongitude(-91.2);
        return apt;
    }

    private static Apartment priced(int rent, double beds) {
        return listing(rent, beds);
    }

    @Test
    void price_brackets_use_price_per_person() {
        assertEquals(50, scorer.priceScore(priced(450, 1)));
        assertEquals(25, scorer.priceScore(priced(600, 1)));
        assertEquals(0, scorer.priceScore(priced(750, 1)));
        assertEquals(0, scorer.priceScore(priced(900, 1)));
        // Two bedrooms at $1,000 total is $500 per person: (750 - 500) / 300 of 50 points.
        assertEquals(42, scorer.priceScore(priced(1000, 2)));
    }

    @Test
    void studio_uses_full_rent_as_price_per_person() {
        Apartment studio = priced(400, 0);
        assertEquals(400, (int) studio.getPricePerPerson());
        assertEquals(50, scorer.priceScore(studio));
    }

    @Test
    void missing_bedrooms_gives_no_price_points_and_missing_data_penalty() {
        Apartment apt = listing(600, null);
        assertEquals(0, scorer.priceScore(apt));
        assertEquals(DealScorer.PENALTY_MISSING_RENT_OR_BEDS, scorer.penalty(apt));
    }

    @Test
    void missing_rent_gives_no_price_points_and_missing_data_penalty() {
        Apartment apt = listing(null, 2.0);
        assertEquals(0, scorer.priceScore(apt));
        assertEquals(DealScorer.PENALTY_MISSING_RENT_OR_BEDS, scorer.penalty(apt));
    }

    @Test
    void missing_coordinates_give_no_location_points_and_penalty() {
        Apartment apt = new Apartment("2", "https://example.com/2");
        apt.setRentMonthly(600);
        apt.setBedrooms(2.0);
        assertEquals(0, scorer.locationScore(apt));
        assertEquals(DealScorer.PENALTY_NO_COORDINATES, scorer.penalty(apt));
    }

    @Test
    void null_walk_time_with_coordinates_is_beyond_cap_with_no_penalty() {
        Apartment apt = priced(600, 2);
        apt.setWalkTravelTimeSeconds(null);
        apt.setBikeTravelTimeSeconds(null);
        assertEquals(0, scorer.locationScore(apt));
        assertEquals(0, scorer.penalty(apt));
    }

    @Test
    void walk_points_scale_between_full_and_zero_minutes() {
        Apartment eight = priced(600, 2);
        eight.setWalkTravelTimeSeconds(8 * 60);
        assertEquals(15, scorer.locationScore(eight));

        Apartment twentyFive = priced(600, 2);
        twentyFive.setWalkTravelTimeSeconds(25 * 60);
        assertEquals(0, scorer.locationScore(twentyFive));

        Apartment midpoint = priced(600, 2);
        midpoint.setWalkTravelTimeSeconds(1020);
        // 17 minutes is 8/17 of the way from full (8 min) to zero (25 min): 15 * 8 / 17 = 7.06, rounds to 7.
        assertEquals(7, scorer.locationScore(midpoint));
    }

    @Test
    void distance_points_use_miles() {
        Apartment near = priced(600, 2);
        near.setDistanceToCampus(0.3);
        assertEquals(10, scorer.locationScore(near));

        Apartment far = priced(600, 2);
        far.setDistanceToCampus(2.0);
        assertEquals(0, scorer.locationScore(far));
    }

    @Test
    void ac_keywords_from_real_data() {
        assertEquals(5, DealScorer.acPoints("central"));
        assertEquals(5, DealScorer.acPoints("wall sleeve"));
        assertEquals(5, DealScorer.acPoints("window sleeve"));
        assertEquals(5, DealScorer.acPoints("wall mounted"));
        assertEquals(5, DealScorer.acPoints("yes"));
        assertEquals(5, DealScorer.acPoints("none, window"));
        assertEquals(0, DealScorer.acPoints("none"));
        assertEquals(0, DealScorer.acPoints("no air"));
        assertEquals(0, DealScorer.acPoints("no a/c"));
        assertEquals(0, DealScorer.acPoints("-"));
        assertEquals(0, DealScorer.acPoints(""));
    }

    @Test
    void laundry_keywords_from_real_data() {
        assertEquals(5, DealScorer.laundryPoints("in-unit"));
        assertEquals(5, DealScorer.laundryPoints("washer/dryer in unit"));
        assertEquals(5, DealScorer.laundryPoints("in apt, coin-op"));
        assertEquals(3, DealScorer.laundryPoints("on-site free"));
        assertEquals(3, DealScorer.laundryPoints("in building - coin"));
        assertEquals(3, DealScorer.laundryPoints("washer/dryer in basement"));
        assertEquals(0, DealScorer.laundryPoints("hook-ups"));
        assertEquals(0, DealScorer.laundryPoints("none"));
        assertEquals(0, DealScorer.laundryPoints("-"));
        assertEquals(0, DealScorer.laundryPoints(""));
    }

    @Test
    void parking_keywords_from_real_data() {
        assertEquals(4, DealScorer.parkingPoints("off-street parking +$$"));
        assertEquals(4, DealScorer.parkingPoints("off-steet parking"));
        assertEquals(4, DealScorer.parkingPoints("garage"));
        assertEquals(4, DealScorer.parkingPoints("on-site"));
        assertEquals(1, DealScorer.parkingPoints("on-street or ramp"));
        assertEquals(1, DealScorer.parkingPoints("street parking"));
        assertEquals(0, DealScorer.parkingPoints("negotiable"));
        assertEquals(0, DealScorer.parkingPoints(""));
    }

    @Test
    void utilities_and_dishwasher_from_included_field() {
        assertEquals(3, DealScorer.utilitiesPoints("refrigerator, trash, heat, water"));
        assertEquals(3, DealScorer.utilitiesPoints("electric"));
        assertEquals(1, DealScorer.utilitiesPoints("water/hot water"));
        assertEquals(0, DealScorer.utilitiesPoints("refrigerator, trash"));

        Apartment apt = priced(600, 2);
        apt.setFeature(FeatureType.INCLUDED, "Refrigerator, Dishwasher");
        assertEquals(3, scorer.amenityScore(apt));
    }

    @Test
    void penalty_stacks_to_the_fifteen_point_maximum() {
        Apartment apt = new Apartment("3", "https://example.com/3");
        apt.addFlag(Flag.BEDBATH_CONFLICT);
        assertEquals(DealScorer.PENALTY_MISSING_RENT_OR_BEDS
                + DealScorer.PENALTY_NO_COORDINATES
                + DealScorer.PENALTY_BEDBATH_CONFLICT, scorer.penalty(apt));
        assertEquals(15, scorer.penalty(apt));
    }

    @Test
    void conflict_penalty_is_skipped_when_overridden() {
        Apartment apt = priced(600, 2);
        apt.addFlag(Flag.BEDBATH_CONFLICT);
        apt.addFlag(Flag.BEDBATH_OVERRIDDEN);
        assertEquals(0, scorer.penalty(apt));
    }

    @Test
    void ideal_listing_scores_one_hundred_with_breakdown() {
        Apartment apt = priced(400, 2);
        apt.setWalkTravelTimeSeconds(300);
        apt.setBikeTravelTimeSeconds(120);
        apt.setDistanceToCampus(0.2);
        apt.setFeature(FeatureType.AIR_CONDITIONING, "Central");
        apt.setFeature(FeatureType.LAUNDRY, "In-unit");
        apt.setFeature(FeatureType.PARKING, "Off-Street Parking");
        apt.setFeature(FeatureType.INCLUDED, "Dishwasher, Heat");

        scorer.score(List.of(apt));

        assertEquals(100, apt.getDealScore());
        assertEquals("price 50/50, location 30/30, amenities 20/20, penalty 0", apt.getDealScoreDetail());
    }

    @Test
    void worst_listing_clamps_to_zero() {
        Apartment apt = listing(2000, 1.0);
        apt.setLatitude(null);
        apt.setLongitude(null);
        apt.addFlag(Flag.BEDBATH_CONFLICT);

        scorer.score(List.of(apt));

        assertEquals(0, apt.getDealScore());
        assertTrue(apt.getDealScoreDetail().contains("penalty -7"), apt.getDealScoreDetail());
    }

    @Test
    void score_sets_fields_on_every_listing() {
        Apartment a = priced(600, 2);
        Apartment b = new Apartment("2", "https://example.com/2");
        scorer.score(List.of(a, b));
        assertNotNull(a.getDealScore());
        assertNotNull(a.getDealScoreDetail());
        assertNotNull(b.getDealScore());
    }

    @Test
    void linear_interpolates_and_clamps() {
        assertEquals(50, DealScorer.linear(450, 450, 750, 50));
        assertEquals(25, DealScorer.linear(600, 450, 750, 50));
        assertEquals(0, DealScorer.linear(750, 450, 750, 50));
        assertEquals(0, DealScorer.linear(10_000, 450, 750, 50));
    }

    @Test
    void config_defaults_match_pipeline_properties() {
        assertEquals(ScoringConfig.defaults(), ScoringConfig.load());
    }

    @Test
    void config_reads_overrides_and_falls_back_to_defaults() {
        Properties props = new Properties();
        props.setProperty("scoring.price.fullAt", "500");
        props.setProperty("scoring.price.zeroAt", "800");
        ScoringConfig config = ScoringConfig.fromProperties(props);
        assertEquals(500, config.priceFullAt());
        assertEquals(800, config.priceZeroAt());
        assertEquals(ScoringConfig.defaults().walkZeroMinutes(), config.walkZeroMinutes());
    }

    @Test
    void config_rejects_zero_point_at_or_below_full_point() {
        Properties props = new Properties();
        props.setProperty("scoring.price.fullAt", "800");
        props.setProperty("scoring.price.zeroAt", "700");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> ScoringConfig.fromProperties(props));
        assertTrue(e.getMessage().contains("scoring.price"), e.getMessage());
    }

    @Test
    void config_rejects_non_numeric_value() {
        Properties props = new Properties();
        props.setProperty("scoring.walk.fullMinutes", "eight");
        assertThrows(IllegalArgumentException.class, () -> ScoringConfig.fromProperties(props));
    }
}
