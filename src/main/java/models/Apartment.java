/**
 * Mutable listing model used across scrape, clean, geocode, and API enrichment stages.
 *
 * @author Andrew Peirce
 */
package models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Apartment {

    private final String id;
    private final String url;

    private String address;
    private String price;
    private Integer rentMonthly;

    private EnumMap<FeatureType, String> features = new EnumMap<>(FeatureType.class);
    private List<Flag> flags = new ArrayList<>();

    private String rawTitle;
    private String rawDescription;

    private Double bedrooms;
    private Double bathrooms;

    private Double latitude;
    private Double longitude;
    private Double accuracy;
    private String geocodedAddress;
    private Double distanceToCampus;

    private Integer walkTravelTimeSeconds;
    private String walkTravelTimeFormatted;
    private Integer bikeTravelTimeSeconds;
    private String bikeTravelTimeFormatted;

    private Integer walkScore;
    private String walkScoreDescription;
    private Integer bikeScore;
    private String bikeScoreDescription;

    @JsonCreator
    public Apartment(@JsonProperty("id") String id, @JsonProperty("url") String url) {
        this.id = id;
        this.url = url;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setRentMonthly(Integer rentMonthly) {
        this.rentMonthly = rentMonthly;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setRawTitle(String rawTitle) {
        this.rawTitle = rawTitle;
    }

    public void setRawDescription(String rawDescription) {
        this.rawDescription = rawDescription;
    }

    public void setBedrooms(Double bedrooms) {
        this.bedrooms = bedrooms;
    }

    public void setBathrooms(Double bathrooms) {
        this.bathrooms = bathrooms;
    }

    public void setFeature(FeatureType type, String value) {
        features.put(type, value);
    }

    @JsonProperty("allFeatures")
    public void setAllFeatures(Map<FeatureType, String> features) {
        this.features = features == null ? new EnumMap<>(FeatureType.class) : new EnumMap<>(features);
    }

    @JsonProperty("landlord")
    public void setLandlord(String landlord) {
        setFeature(FeatureType.LANDLORD, landlord);
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    @JsonProperty("geocodedAddress")
    @JsonAlias("geocodioAddress")
    public void setGeocodedAddress(String geocodedAddress) {
        this.geocodedAddress = geocodedAddress;
    }

    public void setDistanceToCampus(Double distance) {
        this.distanceToCampus = distance;
    }

    public void setWalkTravelTimeSeconds(Integer seconds) {
        this.walkTravelTimeSeconds = seconds;
        this.walkTravelTimeFormatted = seconds == null ? null : formatTravelTime(seconds);
    }

    public void setWalkTravelTimeFormatted(String formatted) {
        this.walkTravelTimeFormatted = formatted;
    }

    public void setBikeTravelTimeSeconds(Integer seconds) {
        this.bikeTravelTimeSeconds = seconds;
        this.bikeTravelTimeFormatted = seconds == null ? null : formatTravelTime(seconds);
    }

    public void setBikeTravelTimeFormatted(String formatted) {
        this.bikeTravelTimeFormatted = formatted;
    }

    public void setWalkScore(Integer walkScore) {
        this.walkScore = walkScore;
    }

    public void setWalkScoreDescription(String walkScoreDescription) {
        this.walkScoreDescription = walkScoreDescription;
    }

    public void setBikeScore(Integer bikeScore) {
        this.bikeScore = bikeScore;
    }

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

    @JsonIgnore
    public String getLandlord() {
        return features.getOrDefault(FeatureType.LANDLORD, "n/a");
    }

    @JsonIgnore
    public String getBedBath() {
        return features.getOrDefault(FeatureType.BED_BATH, "n/a");
    }

    @JsonProperty("rawTitle")
    public String getRawTitle() {
        return rawTitle;
    }

    @JsonProperty("rawDescription")
    public String getRawDescription() {
        return rawDescription;
    }

    @JsonIgnore
    public String getAmenities() {
        return features.getOrDefault(FeatureType.INCLUDED, "n/a");
    }

    @JsonIgnore
    public String getAvailable() {
        return features.getOrDefault(FeatureType.AVAILABLE, "n/a");
    }

    public void addFlag(Flag flag) {
        if (!flags.contains(flag)) {
            flags.add(flag);
        }
    }

    public void removeFlag(Flag flag) {
        flags.remove(flag);
    }

    /**
     * Clears geocode-owned fields so a re-run does not keep stale coordinates or flags.
     */
    public void clearGeocode() {
        latitude = null;
        longitude = null;
        accuracy = null;
        geocodedAddress = null;
        distanceToCampus = null;
        removeFlag(Flag.GEOCODE_APPROXIMATE);
        removeFlag(Flag.GEOCODE_NO_MATCH);
    }

    /**
     * Clears TravelTime / WalkScore fields so a re-run does not keep stale zeros.
     */
    public void clearEnrichment() {
        walkTravelTimeSeconds = null;
        walkTravelTimeFormatted = null;
        bikeTravelTimeSeconds = null;
        bikeTravelTimeFormatted = null;
        walkScore = null;
        walkScoreDescription = null;
        bikeScore = null;
        bikeScoreDescription = null;
    }

    @JsonProperty("flags")
    public void setFlags(List<Flag> flags) {
        this.flags = flags == null ? new ArrayList<>() : new ArrayList<>(flags);
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("address")
    public String getAddress() {
        return address;
    }

    @JsonProperty("price")
    public String getPrice() {
        return price;
    }

    @JsonProperty("rentMonthly")
    public Integer getRentMonthly() {
        return rentMonthly;
    }

    @JsonProperty("flags")
    public List<Flag> getFlags() {
        return flags;
    }

    @JsonProperty("bedrooms")
    public Double getBedrooms() {
        return bedrooms;
    }

    @JsonProperty("bathrooms")
    public Double getBathrooms() {
        return bathrooms;
    }

    @JsonProperty("latitude")
    public Double getLatitude() {
        return latitude;
    }

    @JsonProperty("longitude")
    public Double getLongitude() {
        return longitude;
    }

    @JsonProperty("accuracy")
    public Double getAccuracy() {
        return accuracy;
    }

    @JsonProperty("geocodedAddress")
    public String getGeocodedAddress() {
        return geocodedAddress;
    }

    @JsonProperty("distanceToCampus")
    public Double getDistanceToCampus() {
        return distanceToCampus;
    }

    @JsonProperty("walkTravelTimeSeconds")
    public Integer getWalkTravelTimeSeconds() {
        return walkTravelTimeSeconds;
    }

    @JsonProperty("walkTravelTimeFormatted")
    public String getWalkTravelTimeFormatted() {
        return walkTravelTimeFormatted;
    }

    @JsonProperty("bikeTravelTimeSeconds")
    public Integer getBikeTravelTimeSeconds() {
        return bikeTravelTimeSeconds;
    }

    @JsonProperty("bikeTravelTimeFormatted")
    public String getBikeTravelTimeFormatted() {
        return bikeTravelTimeFormatted;
    }

    @JsonProperty("walkScore")
    public Integer getWalkScore() {
        return walkScore;
    }

    @JsonProperty("walkScoreDescription")
    public String getWalkScoreDescription() {
        return walkScoreDescription;
    }

    @JsonProperty("bikeScore")
    public Integer getBikeScore() {
        return bikeScore;
    }

    @JsonProperty("bikeScoreDescription")
    public String getBikeScoreDescription() {
        return bikeScoreDescription;
    }

    /**
     * Monthly rent divided by the number of people, assuming one person per bedroom
     * (studios and fractional bedrooms count as at least one person).
     *
     * @return rounded dollars per person, or null if rent or bedrooms are unknown
     */
    @JsonProperty("pricePerPerson")
    public Integer getPricePerPerson() {
        if (rentMonthly == null || bedrooms == null) {
            return null;
        }
        double people = Math.max(1.0, bedrooms);
        return (int) Math.round(rentMonthly / people);
    }

    public boolean hasCoordinates() {
        return latitude != null
                && longitude != null
                && !(latitude == 0.0 && longitude == 0.0);
    }

    private static String formatTravelTime(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    public String toString() {
        return String.format("%s | %s | %s", getLandlord(), address, price);
    }
}
