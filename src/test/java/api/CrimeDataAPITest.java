package api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CrimeDataAPITest {

    @Test
    void fetchAndWriteReport_writes_json(@TempDir Path tempDir) throws Exception {
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
        CrimeDataAPI api = new CrimeDataAPI("key", "WI0320000", transport);
        CrimeDataAPI.CrimeReport report = api.fetchAndWriteReport(out);

        assertTrue(Files.exists(out));
        assertEquals("WI0320000", report.ori);
        assertEquals(5, report.totals.get("violentActualsSum"));
        assertEquals(22, report.totals.get("propertyActualsSum"));
        assertEquals(2, transport.getRequests().size());
    }
}
