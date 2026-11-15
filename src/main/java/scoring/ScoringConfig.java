/**
 * Thresholds for {@link DealScorer}, read from the {@code scoring.*} keys in pipeline.properties.
 * Each component scores linearly between a "full" value (best) and a "zero" value (worst).
 *
 * @author Andrew Peirce
 */
package scoring;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

public record ScoringConfig(
        double priceFullAt,
        double priceZeroAt,
        double walkFullMinutes,
        double walkZeroMinutes,
        double bikeFullMinutes,
        double bikeZeroMinutes,
        double distanceFullMiles,
        double distanceZeroMiles) {

    private static final String RESOURCE = "pipeline.properties";

    public ScoringConfig {
        requireOrdered("scoring.price", priceFullAt, priceZeroAt);
        requireOrdered("scoring.walk", walkFullMinutes, walkZeroMinutes);
        requireOrdered("scoring.bike", bikeFullMinutes, bikeZeroMinutes);
        requireOrdered("scoring.distance", distanceFullMiles, distanceZeroMiles);
    }

    /**
     * @return the defaults, tuned on the 11/14/2026 scrape
     */
    public static ScoringConfig defaults() {
        return new ScoringConfig(450, 750, 8, 25, 3, 10, 0.35, 1.5);
    }

    /**
     * Loads thresholds from the classpath {@code pipeline.properties}. Missing keys use the defaults.
     */
    public static ScoringConfig load() {
        Properties props = new Properties();
        try (InputStream in = ScoringConfig.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + RESOURCE, e);
        }
        return fromProperties(props);
    }

    static ScoringConfig fromProperties(Properties props) {
        ScoringConfig d = defaults();
        return new ScoringConfig(
                number(props, "scoring.price.fullAt", d.priceFullAt()),
                number(props, "scoring.price.zeroAt", d.priceZeroAt()),
                number(props, "scoring.walk.fullMinutes", d.walkFullMinutes()),
                number(props, "scoring.walk.zeroMinutes", d.walkZeroMinutes()),
                number(props, "scoring.bike.fullMinutes", d.bikeFullMinutes()),
                number(props, "scoring.bike.zeroMinutes", d.bikeZeroMinutes()),
                number(props, "scoring.distance.fullMiles", d.distanceFullMiles()),
                number(props, "scoring.distance.zeroMiles", d.distanceZeroMiles()));
    }

    private static double number(Properties props, String key, double fallback) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be a number, got '" + value + "'", e);
        }
    }

    private static void requireOrdered(String name, double full, double zero) {
        if (full < 0 || zero <= full) {
            throw new IllegalArgumentException(
                    name + ": the zero-point value must be greater than the full-point value, and both non-negative"
                            + " (got full=" + full + ", zero=" + zero + ")");
        }
    }
}
