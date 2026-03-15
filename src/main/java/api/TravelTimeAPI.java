package api;
import models.Apartment;
import utils.JsonUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
// import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.http.*;
import java.nio.file.Paths;
import java.io.FileInputStream;
// import java.io.File;
import java.io.IOException;
import java.net.URI;
// import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// import java.time.temporal.ChronoUnit;
import java.util.*;
import java.time.*;

public class TravelTimeAPI {

	private static final String BASE_URL = "https://api.traveltimeapp.com/v4/time-filter";
	private static final double CAMPUS_LAT = 43.81610373619651;
	private static final double CAMPUS_LNG = -91.2311481387267;
//	private static final ObjectMapper mapper = new ObjectMapper();

	// TODO - create method to read geocoded file dynamically
	private static String geocoded = "listings_geocoded_10_14_2025_1618.json";
	private static String geocodioFilepath = "output\\Geocoded\\" + geocoded;
	// private static String requestFilepath = "C:\\Users\\drew1\\StudentRentalsScraper\\output\\travel_time_request.json";
	private static String responseFilepath = "output\\API\\TravelTime";
	
	/**
	 * Helper method to read API credentials from properties file
	 * @return
	 */
	private String[] getCredentials() {
		String apiKey = "";
		String apiId = "";
		Properties props = new Properties();

		try (FileInputStream fis = new FileInputStream("config\\TravelTime.properties")) {
			props.load(fis);
		} catch (IOException e) {
			e.printStackTrace();
		}

		apiKey = props.getProperty("api.key");
		apiId = props.getProperty("api.id");
		return new String[] {apiKey, apiId};
	}

	public List<Apartment> addTravelTimes(List<Apartment> apartments) {
		
		return apartments; // to avoid nullifying input in full pipelines
	}
	
	public static void main(String[] args) {
		List<Apartment> apartments = JsonUtils.fromJson(geocodioFilepath);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		// File file = new File(requestFilepath);
		final String runTimeStamp = LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("MM_dd_yyyy_HHmm"));
		
		
		
		// Write JSON as String for testing
		try {
			Request walkRequest = generateRequest(apartments, "walking"); // for walking
//			String walkJson = mapper.writeValueAsString(walkRequest);
			String walkJson = JsonUtils.toJsonString(walkRequest);
			System.out.println(walkJson);
			Request bikeRequest = generateRequest(apartments, "cycling");
//			String bikeJson = mapper.writeValueAsString(bikeRequest);
			String bikeJson = JsonUtils.toJsonString(bikeRequest);
			System.out.println(bikeJson);
			Response walkResponse = callAPI(walkRequest);
			String walkResponseJson = mapper.writeValueAsString(walkResponse);
			System.out.println(walkResponseJson);
			
			// Push JSON to disk
			walkJson= Paths.get(responseFilepath + "\\API", uniqueFilename("traveltime_response", "json", runTimeStamp)).toString();
			// TODO - write json to disk
			JsonUtils.toJson(walkResponse, walkJson);
			
			
		} catch (JsonProcessingException e) {
			throw new RuntimeException("JSON Serialization error: " + e);
		}
	}
	
	public static class Request {
		public List<Location> locations;
		public List<ArrivalSearch> arrival_searches;
	}
	
	public static class Response {
		public List<Result> results;
		
	}
	
	public static class Result {
		public String search_id;
		public List<ResponseLocation> locations;
		public List<String> unreachable;
	}
	
	public static class ResponseLocation {
		public String id; // changed from search_id
		public List<Property> properties;
	}
	
	public static class Property {
		public int travel_time;
	}
	
	public static class Location {
		public String id;
		public Coordinate coords;
		
		public Location (String id, double lat, double lng) {
			this.id = id;
			this.coords = new Coordinate(lat, lng);
		}
		
		public static class Coordinate {
			public double lat;
			public double lng;
			public Coordinate(double lat, double lng) {
				this.lat = lat;
				this.lng = lng;
			}
		}
	}
	
	public static class ArrivalSearch {
		public String id;
		public List<String> departure_location_ids;
		public String arrival_location_id;
		public String arrival_time;
		public int travel_time;
		public List<String> properties;
		public Transportation transportation;
		
		public ArrivalSearch() {}
		
	}
	
	public static class Transportation {
		public String type;
		public Transportation(String type) {
			this.type = type;
		}
	}

	
	private static Request generateRequest(List<Apartment> apartments, String transportationType) {
		Request request = new TravelTimeAPI.Request();
		request.locations = new ArrayList<>();
		request.locations.add(new Location("starting-location", CAMPUS_LAT, CAMPUS_LNG)); // starting location
		
		for (int i = 0; i < apartments.size(); i++) {
			request.locations.add(new Location("other-location-" + i, apartments.get(i).getLatitude(), apartments.get(i).getLongitude()));
		}
		
		request.arrival_searches = new ArrayList<>();
		request.arrival_searches.add(new ArrivalSearch());
		ArrivalSearch arrivalSearch = request.arrival_searches.getFirst();
		arrivalSearch.id = "Arrival search";
		arrivalSearch.arrival_location_id = request.locations.getFirst().id; // "starting-id"
		List<String> departure_location_ids = request.locations.stream()
															.filter(loc -> !"starting-location".equals(loc.id))
															.map(loc -> loc.id)
															.toList();
		arrivalSearch.departure_location_ids = departure_location_ids;
		arrivalSearch.arrival_time = ZonedDateTime.now(ZoneId.of("America/Chicago")) // Central Time
			    .plusDays(1)
			    .withHour(8).withMinute(0).withSecond(0).withNano(0)
			    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME); // tomorrow at 8:00
		arrivalSearch.travel_time = 900; // 900 seconds max travel time (15 mins)
		arrivalSearch.properties = new ArrayList<>(List.of("travel_time"));
		arrivalSearch.transportation = new Transportation(transportationType); // "cycling" or "walking"
		
		return request;
	}

	// TODO - send the request, and process response
	private static Response callAPI(Request request) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		String jsonBody = "";
		try {
			jsonBody = mapper.writeValueAsString(request);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] credentials = new TravelTimeAPI().getCredentials();
		final String API_KEY = credentials[0];
		final String API_ID = credentials[1];
		
		HttpRequest httpRequest = HttpRequest.newBuilder()
				.uri(URI.create(BASE_URL))
				.header("Content-Type", "application/json")
				.header("Accept", "application/json")
				.header("X-Application-Id", API_ID)
				.header("X-Api-Key", API_KEY)
				.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
				.build();
		
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response = null;
		try {
			response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			return mapper.readValue(response.body(), Response.class);
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private static String uniqueFilename(String baseName, String extension, String timestamp) {
        return baseName + "_" + timestamp + "." + extension;
    }
}
