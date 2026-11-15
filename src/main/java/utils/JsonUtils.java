/**
 * JSON serialization helpers backed by Jackson.
 *
 * @author Andrew Peirce
 */
package utils;

import models.Apartment;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JsonUtils {

    private static final ObjectMapper MAPPER = createMapper();

    private JsonUtils() {
    }

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Allow reading older JSON that contains fields this version no longer defines.
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    public static String toJsonString(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON serialization error: " + e.getMessage(), e);
        }
    }

    public static <T> T fromJsonString(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON deserialization error: " + e.getMessage(), e);
        }
    }

    public static void toJson(Object object, String filePath) {
        try {
            File file = new File(filePath);
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            MAPPER.writeValue(file, object);
        } catch (IOException e) {
            throw new RuntimeException("Error writing JSON to file: " + e.getMessage(), e);
        }
    }

    public static List<Apartment> fromJson(String filePath) {
        try {
            return MAPPER.readValue(
                    new File(filePath),
                    MAPPER.getTypeFactory().constructCollectionType(List.class, Apartment.class));
        } catch (IOException e) {
            throw new RuntimeException("Error reading JSON file: " + e.getMessage(), e);
        }
    }
}
