/**
 * An enum to hold possible red/yellow flags about a listing. 
 * In essence, if a listing has any flags it is less likely to be accurate (there was one listing
 * which was not up to date, property manager called me in state of shock on how I even found it)
 * 
 * @author Andrew Peirce
 * 
 * Date Last Modified: 08/05/2025
 */
import com.fasterxml.jackson.annotation.JsonValue;

public enum Flag {
	MISSING_LANDLORD, // From features AND title 
	MISSING_BEDBATH, // From features AND title
	LANDLORD_FROM_TITLE, // Landlord missing from features, had to grab from title
	BEDBATH_FROM_TITLE; // Bed/Bath missing from features, had to grab from title
	
	@JsonValue
	public String toValue() {
		return this.name().toLowerCase();
	}
	
	public static Flag fromValue(String value) {
		for (Flag flag : values()) {
			if (flag.name().equalsIgnoreCase(value)) {
				return flag;
			}
		}
		return null;
	}
}
