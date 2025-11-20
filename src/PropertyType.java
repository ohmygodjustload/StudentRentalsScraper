//import java.util.Set;
//
///**
// * An enum to hold different property types
// * 
// * @author Andrew Peirce
// * 
// * Date Last Modified: 08/05/2025
// */
//public enum PropertyType {
//	MULTI_UNIT("Multi-unit building", "Multi-unit", "Duplex", "Triplex", 
//            "Above Commercial Space", "Above Business", "Over a Business",
//            "Upper Duplex", "Duplex Upstairs", "Duples", "Multi-family"),
//	HOUSE("House", "Single Family Home", "Split Level"),
// 	COMPLEX("Apartment Building", "Complex", "Condo"),
// 	TOWNHOUSE("Townhouse", "Town home"),
// 	STUDIO("Studio", "Studio Apartment"),
// 	COMMERCIAL("Above Commercial Space", "Above Business", "Over a Business"),
// 	UNKNOWN; 	// Placeholder for any unknown types. TODO - This is temporary, I want to develop 
//		   	// an algorithm that reads property types and assigns. Machine Learning?
//	
//	private final Set<String> keywords;
//	
//	PropertyType(String... keywords) {
//		this.keywords = Set.of(keywords);
//	}
//	
//	public static PropertyType fromString(String value) {
//        if (value == null || value.isEmpty()) return UNKNOWN;
//        
//        String normalized = value.trim().toLowerCase();
//        for (PropertyType type : values()) {
//            for (String keyword : type.keywords) {
//                if (normalized.contains(keyword.toLowerCase())) {
//                    return type;
//                }
//            }
//        }
//        return UNKNOWN;
//    }
//}
