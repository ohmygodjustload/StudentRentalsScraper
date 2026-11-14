/**
 * Loads API credentials from {@code config/<name>.properties}, with environment
 * variable overrides taking precedence.
 *
 * @author Andrew Peirce
 */
package api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

public final class ApiConfig {

    private final String name;
    private final Properties properties;

    private ApiConfig(String name, Properties properties) {
        this.name = name;
        this.properties = properties;
    }

    /**
     * Loads {@code config/<name>.properties} if present.
     *
     * @param name config basename without extension (e.g. {@code TravelTime})
     */
    public static ApiConfig load(String name) {
        return load(name, Path.of("config", name + ".properties"));
    }

    public static ApiConfig load(String name, Path propertiesFile) {
        Properties props = new Properties();
        if (Files.isRegularFile(propertiesFile)) {
            try (InputStream in = Files.newInputStream(propertiesFile)) {
                props.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read " + propertiesFile, e);
            }
        }
        return new ApiConfig(name, props);
    }

    public String getName() {
        return name;
    }

    /**
     * Returns a property value, preferring an environment variable when set.
     * Env var form: {@code <NAME>_<KEY>} with dots replaced by underscores
     * (e.g. TravelTime + api.key → {@code TRAVELTIME_API_KEY}).
     */
    public Optional<String> get(String key) {
        String envName = toEnvName(name, key);
        String fromEnv = System.getenv(envName);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return Optional.of(fromEnv.strip());
        }
        String fromFile = properties.getProperty(key);
        if (fromFile != null && !fromFile.isBlank() && !fromFile.startsWith("<")) {
            return Optional.of(fromFile.strip());
        }
        return Optional.empty();
    }

    public boolean has(String key) {
        return get(key).isPresent();
    }

    public boolean hasAll(String... keys) {
        for (String key : keys) {
            if (!has(key)) {
                return false;
            }
        }
        return true;
    }

    static String toEnvName(String configName, String key) {
        return (configName + "_" + key)
                .replace('.', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
    }
}
