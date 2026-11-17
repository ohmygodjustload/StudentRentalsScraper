/**
 * File-backed key → JSON value cache for API enrichment results.
 *
 * @author Andrew Peirce
 */
package api;

import utils.JsonUtils;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ResponseCache {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    private final Path file;
    private final boolean disabled;
    private final boolean refresh;
    private final Map<String, JsonNode> entries;
    private int hits;
    private int misses;

    private ResponseCache(Path file, boolean disabled, boolean refresh, Map<String, JsonNode> entries) {
        this.file = file;
        this.disabled = disabled;
        this.refresh = refresh;
        this.entries = entries;
    }

    /**
     * No-op cache used when caching is not wired (tests / default constructors).
     */
    public static ResponseCache disabled() {
        return new ResponseCache(null, true, false, new LinkedHashMap<>());
    }

    /**
     * Load from {@code file} (or start empty). When {@code refresh} is true, lookups always miss
     * but puts still write so a forced run repairs the cache.
     */
    public static ResponseCache load(Path file, boolean refresh) {
        Map<String, JsonNode> entries = new LinkedHashMap<>();
        if (Files.isRegularFile(file)) {
            try {
                Map<String, JsonNode> loaded = MAPPER.readValue(
                        file.toFile(),
                        new TypeReference<LinkedHashMap<String, JsonNode>>() {
                        });
                if (loaded != null) {
                    entries.putAll(loaded);
                }
            } catch (IOException e) {
                System.err.println("[Cache] Ignoring corrupt cache file " + file + ": " + e.getMessage());
            }
        }
        return new ResponseCache(file, false, refresh, entries);
    }

    public static String formatCoord(double value) {
        return String.format("%.6f", value);
    }

    /**
     * Look up a cached value. Distinguishes hit-with-null from miss via {@link Lookup#hit()}.
     * Disabled and refresh caches always miss.
     */
    public <T> Lookup<T> get(String key, Class<T> type) {
        if (disabled || refresh || !entries.containsKey(key)) {
            misses++;
            return Lookup.miss();
        }
        hits++;
        JsonNode node = entries.get(key);
        if (node == null || node.isNull()) {
            return Lookup.hit(null);
        }
        if (type == String.class && node.isTextual()) {
            return Lookup.hit(type.cast(node.asText()));
        }
        return Lookup.hit(JsonUtils.fromJsonString(node.toString(), type));
    }

    public void put(String key, Object value) {
        if (disabled) {
            return;
        }
        if (value == null) {
            entries.put(key, MAPPER.nullNode());
        } else {
            entries.put(key, MAPPER.valueToTree(value));
        }
    }

    public void save() {
        if (disabled || file == null) {
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = Files.createTempFile(
                    parent != null ? parent : Path.of("."),
                    file.getFileName().toString() + ".",
                    ".tmp");
            try {
                MAPPER.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), entries);
                try {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
                throw e;
            }
        } catch (IOException e) {
            System.err.println("[Cache] Failed to save " + file + ": " + e.getMessage());
        }
    }

    public int size() {
        return entries.size();
    }

    public int hits() {
        return hits;
    }

    public int misses() {
        return misses;
    }

    public void resetCounters() {
        hits = 0;
        misses = 0;
    }

    public Path getFile() {
        return file;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isRefresh() {
        return refresh;
    }

    public static final class Lookup<T> {
        private final boolean hit;
        private final T value;

        private Lookup(boolean hit, T value) {
            this.hit = hit;
            this.value = value;
        }

        public static <T> Lookup<T> hit(T value) {
            return new Lookup<>(true, value);
        }

        public static <T> Lookup<T> miss() {
            return new Lookup<>(false, null);
        }

        public boolean hit() {
            return hit;
        }

        public T value() {
            return value;
        }
    }
}
