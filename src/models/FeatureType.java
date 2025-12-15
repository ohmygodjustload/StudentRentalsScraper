/**
 * An enum to hold extra features of the listing.
 * Found under <div class="rm_listing_features">
 * 
 * @author Andrew Peirce
 * 
 * Date Last Modified: 08/05/2025
 */
package models;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FeatureType {
	// Core features
	LANDLORD("Landlord"),
    BED_BATH("BedBath"),
    INCLUDED("Included"),
    
    // Additional features
    AVAILABLE("Available"),
    AIR_CONDITIONING("Air"),
    BUS("Bus"),
    EMAIL("Email"),
    LAUNDRY("Laundry"),
    PARKING("Parking"),
    PETS("Pets"),
    PHONE("Phone"),
    SQ_FT("SqFt"),
    PROPERTY_TYPE("Type");
	
	public final String label;
	
	FeatureType(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	// Helper to find enum by label
	public static FeatureType fromLabel(String label) {
		for (FeatureType type : values()) {
			if (type.getLabel().equalsIgnoreCase(label)) {
				return type;
			}
		}
		return null;
	}
	
	@JsonValue
	public String toValue() {
		return this.name().toLowerCase();
	}
	
	// For deserialization
	public static FeatureType fromValue(String value) {
		for (FeatureType type : values()) {
			if (type.name().equalsIgnoreCase(value)) {
				return type;
			}
		}
		return null;
	}
}
