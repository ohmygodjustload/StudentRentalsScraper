/**
 * Listing feature labels extracted from {@code div.rm_listing_features}.
 *
 * @author Andrew Peirce
 */
package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FeatureType {
    LANDLORD("Landlord"),
    BED_BATH("BedBath"),
    INCLUDED("Included"),
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

    @JsonCreator
    public static FeatureType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (FeatureType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
