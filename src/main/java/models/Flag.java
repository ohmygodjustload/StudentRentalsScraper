/**
 * Data-quality annotations for a listing.
 *
 * @author Andrew Peirce
 */
package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Flag {
    MISSING_LANDLORD,
    MISSING_BEDBATH,
    LANDLORD_FROM_TITLE,
    BEDBATH_FROM_TITLE,
    STUDIO,
    FRACTIONAL_BEDROOM,
    FRACTIONAL_BATHROOM,
    BEDBATH_UNPARSEABLE,
    UNUSUAL_BEDROOM_COUNT;

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }

    @JsonCreator
    public static Flag fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (Flag flag : values()) {
            if (flag.name().equalsIgnoreCase(value)) {
                return flag;
            }
        }
        return null;
    }
}
