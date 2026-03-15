package utils;

import models.Apartment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JsonUtils serialization and deserialization.
 */
class JsonUtilsTest {

    @Test
    void toJsonString_serializes_apartment() {
        Apartment apt = new Apartment("123", "https://example.com/123");
        apt.setAddress("100 Main St La Crosse WI");
        apt.setPrice("800");

        String json = JsonUtils.toJsonString(apt);
        assertNotNull(json);
        assertTrue(json.contains("\"id\""));
        assertTrue(json.contains("123"));
        assertTrue(json.contains("800"));
        assertTrue(json.contains("100 Main St La Crosse WI"));
    }

    @Test
    void fromJsonString_deserializes_list_of_strings() {
        String json = "[\"a\",\"b\"]";
        @SuppressWarnings("unchecked")
        List<String> result = JsonUtils.fromJsonString(json, List.class);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
    }

    @Test
    void fromJsonString_throws_on_invalid_json() {
        assertThrows(RuntimeException.class, () ->
                JsonUtils.fromJsonString("not valid json", Apartment.class));
    }
}
