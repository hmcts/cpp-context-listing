package uk.gov.moj.cpp.listing.event.processor.azure.util;

public enum Meridian {
    TWELVE_AM("00"),
    TEN_AM("10"),
    ELEVEN_AM("11"),
    TWELVE_PM("12"),
    ONE_PM("01"),
    TWO_PM("02"),
    THREE_PM("03"),
    FOUR_PM("04"),
    FIVE_PM("05");

    private String value;

    private Meridian(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
