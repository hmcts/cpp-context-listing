package uk.gov.moj.cpp.listing.persistence.enums;

/**
 * Enum representing the type of record in hearing CSV data.
 * Used to distinguish between case records and application records.
 */
public enum CsvRecordType {
    CASE("CASE"),
    APPLICATION("APPLICATION"),
    UNKNOWN("UNKNOWN");
    
    private final String value;
    
    CsvRecordType(String value) {
        this.value = value;
    }
    
    /**
     * Returns the string value of the enum.
     * @return The string representation of the record type
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Converts a string value to the corresponding RecordType enum.
     * @param value The string value to convert
     * @return The corresponding RecordType enum, or null if no match found
     */
    public static CsvRecordType fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (CsvRecordType recordType : CsvRecordType.values()) {
            if (recordType.value.equals(value)) {
                return recordType;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
