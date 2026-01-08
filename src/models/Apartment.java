/**
 * Apartment.java
 * 
 * Description: An apartment class for holding the basic data such as the renter's name, 
 * bed/bath info, price per month, and link to the listing
 * 
 * @author Andrew Peirce
 * 
 * Date Last Modified: 08/05/2025
 * 
 */
package models;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonCreator;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Apartment {
	
	// Core attributes (Added at instantiation)
	private final String ID;
	private final String URL;
	private String address;
	
	// Additional essential attributes (added after instantiation)
    private String price;

    // TODO - remove this attribute and algoroithm and do it in a separate scorer class
    private double dealScore; // Calculate with subjective scoring system
    
    // Feature Storage
    private EnumMap<FeatureType, String> features = new EnumMap<>(FeatureType.class); // Map of all features found on listing page
    private List<Flag> flags = new ArrayList<>(); // A list of potential issues with the listing (missing info, etc.)

    // Raw Scraped Attributes
    @JsonIgnore
    private String rawTitle; // the title of the listing used for fallback info (landlord, bed/bath) if missing from features
    
    // Geocoding attributes
    private double latitude;
    private double longitude;
    private double accuracy;
    private String geocodioAddress; // Formatted address from Geocodio API
    
    // Attributes requiring geocoding
    private double distanceToCampus;
    
    // Travel Time attributes (TravelTime API)
    private int walkTravelTimeSeconds;
    private String walkTravelTimeFormatted;
    private int bikeTravelTimeSeconds;
    private String bikeTravelTimeFormatted;
    
    // Walk Score attributes (WalkScore API)
    private int walkScore;
    private String walkScoreDescription;
    private int bikeScore;
    private String bikeScoreDescription;
    
    // Constructor
    @JsonCreator
    public Apartment(@JsonProperty("id") String id, @JsonProperty("url") String url) {
        this.ID = id;
        this.URL = url;
    }

    // Core Setters
    public void setPrice(String price) { this.price = price; }
    public void setDealScore(double dealScore) { this.dealScore = dealScore; }
    public void setAddress(String address) {this.address = address; }
    public void setTitle(String title) { this.rawTitle = title; }
    
    // Feature management
    public void setFeature(FeatureType type, String value) {
    	features.put(type, value);
    }
    
    @JsonProperty("allFeatures")
    public void setAllFeatures(Map<FeatureType, String> features) {
        this.features = new EnumMap<>(features);
    }
    
    @JsonProperty("landlord")
    public void setLandlord(String landlord) {
        setFeature(FeatureType.LANDLORD, landlord);
    }
    
    @JsonProperty("latitude")
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    @JsonProperty("longitude")
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    @JsonProperty("accuracy")
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    
    @JsonProperty("geocodioAddress")
    public void setGeocodioAddress(String geocodioAddress) {
    	this.geocodioAddress = geocodioAddress;
    }
    
    @JsonProperty("distanceToCampus")
    public void setDistanceToCampus(double distance) { this.distanceToCampus = distance; }
    
    @JsonProperty("walkTravelTimeSeconds")
    public void setTravelTimeSeconds(int seconds) {
    	this.walkTravelTimeSeconds = seconds;
    }
    
    @JsonProperty("walkTravelTimeFormatted")
    public void setTravelTimeFormatted(int seconds) { 
    	this.walkTravelTimeFormatted = formatTravelTime(seconds);
    }
    
    @JsonProperty("bikeTravelTimeSeconds")
    public void setBikeTravelTimeSeconds(int seconds) {
    	this.bikeTravelTimeSeconds = seconds;
    }
    
    @JsonProperty("bikeTravelTimeFormatted")
    public void setBikeTravelTimeFormatted(int seconds) {
    	this.bikeTravelTimeFormatted = formatTravelTime(seconds);
    }
    
    @JsonProperty("walkScore")
    public void setWalkscore(int walkScore) {
    	this.walkScore = walkScore;
    }
    
    @JsonProperty("walkScoreDescription")
    public void setWalkScoreDescription(String walkScoreDescription) {
    	this.walkScoreDescription = walkScoreDescription;
    }
    
    @JsonProperty("bikeScore")
    public void setBikescore(int bikeScore) {
    	this.bikeScore = bikeScore;
    }
    
    @JsonProperty("bikeScoreDescription")
    public void setBikeScoreDescription(String bikeScoreDescription) {
    	this.bikeScoreDescription = bikeScoreDescription;
    }
    
    public String getFeature(FeatureType type) {
    	return features.get(type);
    }
    
    @JsonProperty("allFeatures")
    public Map<FeatureType, String> getAllFeatures() {
    	return Collections.unmodifiableMap(features);
    }
    
    // Convenience getters
    @JsonIgnore
    public String getLandlord() {
    	return features.getOrDefault(FeatureType.LANDLORD, "n/a");
    }
    
    @JsonIgnore
    public String getBedBath() {
        return features.getOrDefault(FeatureType.BED_BATH, "n/a");
    }

    @JsonIgnore
    public String getRawTitle() {
        return rawTitle != null ? rawTitle : "n/a";
    }
    
    @JsonIgnore
    public String getAmenities() {
        return features.getOrDefault(FeatureType.INCLUDED, "n/a");
    }
    
    // Add a flag
    public void addFlag(Flag flag) {
    	if (!flags.contains(flag)) flags.add(flag);
    }
    
    
    // Getters
    @JsonProperty("id")
    public String getID() { return ID; }
    
    @JsonProperty("url")
    public String getURL() { return URL; }
    
    @JsonProperty("address")
    public String getAddress() {return address; }
    
    @JsonProperty("price")
    public String getPrice() { return price; }

    @JsonProperty("flags")
    public List<Flag> getFlags() { return flags; }
    
    @JsonProperty("dealScore")
    public double getDealScore() { return dealScore; }
    
    @JsonProperty("latitude")
    public double getLatitude() { return latitude; }
    
    @JsonProperty("longitude")
    public double getLongitude() { return longitude; }
    
    @JsonProperty("accuracy")
    public double getAccuracy() { return accuracy; }
    
    @JsonProperty("geocodioAddress")
    public String getGeocodioAddress() { return geocodioAddress; }
    
    @JsonProperty("distanceToCampus")
    public double getDistanceToCampus() { return distanceToCampus; }
    
    @JsonProperty("walkTravelTimeSeconds")
    public int getTravelTimeSeconds() { return walkTravelTimeSeconds; }
    
    @JsonProperty("walkTravelTimeFormatted")
    public String getTravelTimeFormatted() { return walkTravelTimeFormatted; }
    
    @JsonProperty("bikeTravelTimeSeconds")
    public int getBikeTravelTimeSeconds() { return bikeTravelTimeSeconds; }
    
    @JsonProperty("bikeTravelTimeFormatted")
    public String getBikeTravelTimeFormatted() { return bikeTravelTimeFormatted; }
    
    @JsonProperty("walkScore")
    public int getWalkScore() { return walkScore; }
    
    @JsonProperty("walkScoreDescription")
    public String getWalkScoreDescription() { return walkScoreDescription; }
    
    @JsonProperty("bikeScore")
    public int getBikeScore() { return bikeScore; }
    
    @JsonProperty("bikeScoreDescription")
    public String getBikeScoreDescription() { return bikeScoreDescription; }
    
    /**
     * Calculates a basic deal score based on price and flags.
     * The score starts at 100 and is reduced based on missing information and price per bedroom.
     * This is a subjective scoring system and can be adjusted as needed.
     * This specific implementation assumes at least two tenants share one bedroom.
     * 
     * TODO - Move this to a separate Scorer class for better separation of concerns
     */
    public void calculateBasicScore() {
    	double score = 100;
    	
    	// Penalize flags
    	if (flags.contains(Flag.MISSING_LANDLORD)) score -= 25;
    	if (flags.contains(Flag.MISSING_BEDBATH)) score -= 15;
    	if (flags.contains(Flag.LANDLORD_FROM_TITLE)) score -= 5;
    	if (flags.contains(Flag.BEDBATH_FROM_TITLE)) score -= 5;
    	// Calculate price per bedroom
    	try {
    		String bedBath = getBedBath();
    		int beds = bedBath.contains("Studio") ? 1 : 
    			Integer.parseInt(bedBath.split("bd")[0]);
    		
    		double priceVal = Double.parseDouble(price.replaceAll("[^0-9]", ""));
    		double pricePerPerson = priceVal / (beds + 1); // Because my Fiance and I share a room
    		if (pricePerPerson >= 500) score -= 35;
    		else if (pricePerPerson >= 475) score -= 30;
    		else if (pricePerPerson >= 450) score -= 25;
    		else if (pricePerPerson >= 425) score -= 20;
    		else if (pricePerPerson >= 400) score -= 15;
    		else if (pricePerPerson >= 375) score -= 10;
    		else if (pricePerPerson >= 350) score -= 5;
    		
    	} catch (Exception e) { /* Ignore calculation errors */ }
    	
    	this.dealScore = Math.max(0, Math.min(100, score));
    }

    // Helper method to format travel times to mm:ss format
    private String formatTravelTime(int seconds) {
    	return String.format("%02d:%02d", seconds / 60, seconds % 60);
	}

	@Override
    public String toString() {
        return String.format("%s | %s | %s",
        	this.getLandlord(), this.address, this.price);
    }
}