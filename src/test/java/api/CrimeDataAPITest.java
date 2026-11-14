package api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CrimeDataAPITest {

    @Test
    void fetchAndWriteReport_separates_offenses_and_clearances(@TempDir Path tempDir) throws Exception {
        String violent = new String(
                getClass().getResourceAsStream("/fixtures/crime_violent_response.json").readAllBytes(),
                StandardCharsets.UTF_8);
        String property = new String(
                getClass().getResourceAsStream("/fixtures/crime_property_response.json").readAllBytes(),
                StandardCharsets.UTF_8);
        FakeHttpTransport transport = new FakeHttpTransport()
                .enqueue(200, violent)
                .enqueue(200, property);

        Path out = tempDir.resolve("crime_report.json");
        CrimeDataAPI api = new CrimeDataAPI("key", "WI0320100", transport);
        CrimeDataAPI.CrimeReport report = api.fetchAndWriteReport(out);

        assertTrue(Files.exists(out));
        assertEquals("WI0320100", report.ori);
        // 22+12+7+8+18 = 67; clearances 13+11+9+9+17 = 59
        assertEquals(67, report.totals.get("violentOffenses"));
        assertEquals(59, report.totals.get("violentClearances"));
        // 101+123+94+101+85 = 504; clearances 20+55+26+41+49 = 191
        assertEquals(504, report.totals.get("propertyOffenses"));
        assertEquals(191, report.totals.get("propertyClearances"));
        assertEquals(5, report.totals.get("monthsWithData"));
        assertEquals(12, report.totals.get("monthsRequested"));
        assertEquals("10/2026", report.totals.get("maxDataDate"));
        assertEquals(2, transport.getRequests().size());
    }

    @Test
    void sumSeries_ignores_null_months() throws Exception {
        String violent = new String(
                getClass().getResourceAsStream("/fixtures/crime_violent_response.json").readAllBytes(),
                StandardCharsets.UTF_8);
        var root = utils.JsonUtils.fromJsonString(violent, com.fasterxml.jackson.databind.JsonNode.class);
        CrimeDataAPI.SeriesSum sum = CrimeDataAPI.sumSeries(root);
        assertEquals(67, sum.offenses);
        assertEquals(59, sum.clearances);
        assertEquals(5, sum.monthsWithData);
    }
}
