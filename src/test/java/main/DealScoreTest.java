package main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for deal scoring logic (migrated from src/tmp/DealScoreTest).
 * Asserts expected score adjustments based on price per person.
 */
class DealScoreTest {

    private static double calculateScore(int beds, double price) {
        double score = 100;
        double pricePerPerson = price / (beds + 1);
        if (pricePerPerson >= 500) score -= 35;
        else if (pricePerPerson >= 475) score -= 30;
        else if (pricePerPerson >= 450) score -= 25;
        else if (pricePerPerson >= 425) score -= 20;
        else if (pricePerPerson >= 400) score -= 15;
        else if (pricePerPerson >= 375) score -= 10;
        else if (pricePerPerson >= 350) score -= 5;
        return Math.max(0, Math.min(100, score));
    }

    @Test
    void score_is_capped_between_0_and_100() {
        assertTrue(calculateScore(1, 100) >= 0 && calculateScore(1, 100) <= 100);
        assertTrue(calculateScore(5, 2400) >= 0 && calculateScore(5, 2400) <= 100);
    }

    @Test
    void price_per_person_under_350_keeps_high_score() {
        // 5 beds + 1 = 6 people, 1800/6 = 300
        assertEquals(100, calculateScore(5, 1800));
    }

    @Test
    void price_per_person_500_or_more_penalizes_heavily() {
        // 5 beds + 1 = 6 people, 3000/6 = 500
        assertEquals(65, calculateScore(5, 3000));
    }

    @Test
    void example_from_original_main_beds5_price2400() {
        // 5 beds, 2400 -> 2400/6 = 400 -> score -= 15 -> 85
        assertEquals(85, calculateScore(5, 2400));
    }
}
