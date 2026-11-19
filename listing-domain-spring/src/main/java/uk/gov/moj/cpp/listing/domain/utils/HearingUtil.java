package uk.gov.moj.cpp.listing.domain.utils;

public class HearingUtil {

    private HearingUtil() {}

    public static Integer getAdjustedDuration(final Integer estimatedMinutes) {
        return (estimatedMinutes != null && estimatedMinutes > 0) ? estimatedMinutes : 1;
    }
}
