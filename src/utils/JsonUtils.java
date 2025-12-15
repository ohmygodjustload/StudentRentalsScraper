/**
 * JSON serialization helper class
 * 
 * @author Andrew Peirce
 * 
 * Date Last Modified: 8/7/2025
 */
package utils;

import models.Apartment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsonUtils {
	private static final ObjectMapper mapper = createMapper();
	
	private static ObjectMapper createMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		return mapper;
	}
	
	// In-memory conversions
	public static String toJsonString(Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("JSON Serialization error: " + e);
		}
	}
	
	public static <T> T fromJsonString(String json, Class<T> type) {
		try {
			return mapper.readValue(json,  type);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("JSON deserialization error: " + e);
		}
	}
	
	
	// File-based conversions
	public static void toJson(Object object, String filePath) {
		try {
			mapper.writeValue(new File(filePath), object);
		} catch (IOException e) {
			throw new RuntimeException("Error writing JSON to file: " + e.getMessage());
		}
	}
	
	public static List<Apartment> fromJson(String filePath) {
		try {
	        return mapper.readValue(
	            new File(filePath),
	            mapper.getTypeFactory().constructCollectionType(List.class, Apartment.class)
	        );
	    } catch (IOException e) {
	        throw new RuntimeException("Error reading JSON file: " + e.getMessage());
	    }
	}
}
