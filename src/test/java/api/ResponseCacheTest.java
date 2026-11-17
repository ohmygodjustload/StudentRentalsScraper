package api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ResponseCacheTest {

    @TempDir
    Path tempDir;

    @Test
    void save_load_round_trip() {
        Path file = tempDir.resolve("traveltime.json");
        ResponseCache cache = ResponseCache.load(file, false);
        cache.put("101|43.810000|-91.240000|walking", 720);
        cache.put("102|43.810000|-91.240000|walking", null);
        cache.save();

        ResponseCache reloaded = ResponseCache.load(file, false);
        assertEquals(2, reloaded.size());

        ResponseCache.Lookup<Integer> hit = reloaded.get("101|43.810000|-91.240000|walking", Integer.class);
        assertTrue(hit.hit());
        assertEquals(720, hit.value());

        ResponseCache.Lookup<Integer> nullHit = reloaded.get("102|43.810000|-91.240000|walking", Integer.class);
        assertTrue(nullHit.hit());
        assertNull(nullHit.value());

        ResponseCache.Lookup<Integer> miss = reloaded.get("missing", Integer.class);
        assertFalse(miss.hit());
        assertEquals(2, reloaded.hits());
        assertEquals(1, reloaded.misses());
    }

    @Test
    void corrupt_file_loads_as_empty() throws Exception {
        Path file = tempDir.resolve("corrupt.json");
        Files.writeString(file, "{not valid json");
        ResponseCache cache = ResponseCache.load(file, false);
        assertEquals(0, cache.size());
        assertFalse(cache.get("any", String.class).hit());
    }

    @Test
    void disabled_is_noop() {
        ResponseCache cache = ResponseCache.disabled();
        cache.put("k", 1);
        assertFalse(cache.get("k", Integer.class).hit());
        assertEquals(0, cache.size());
        cache.save(); // must not throw
        assertTrue(cache.isDisabled());
    }

    @Test
    void refresh_ignores_reads_but_persists_writes() {
        Path file = tempDir.resolve("walkscore.json");
        ResponseCache seed = ResponseCache.load(file, false);
        seed.put("1|43.810000|-91.240000", "{\"status\":1}");
        seed.save();

        ResponseCache refresh = ResponseCache.load(file, true);
        assertTrue(refresh.isRefresh());
        assertFalse(refresh.get("1|43.810000|-91.240000", String.class).hit());

        refresh.put("1|43.810000|-91.240000", "{\"status\":1,\"walkscore\":90}");
        refresh.save();

        ResponseCache reloaded = ResponseCache.load(file, false);
        ResponseCache.Lookup<String> hit = reloaded.get("1|43.810000|-91.240000", String.class);
        assertTrue(hit.hit());
        assertTrue(hit.value().contains("90"));
    }

    @Test
    void formatCoord_uses_six_decimals() {
        assertEquals("43.810000", ResponseCache.formatCoord(43.81));
        assertEquals("-91.240000", ResponseCache.formatCoord(-91.24));
    }
}
