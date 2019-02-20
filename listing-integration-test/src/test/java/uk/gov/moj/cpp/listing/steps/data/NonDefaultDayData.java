package uk.gov.moj.cpp.listing.steps.data;

import java.util.Optional;

public class NonDefaultDayData {


    private final Optional<Integer> duration;
    private final String startTime;

    public NonDefaultDayData(String startTime) {
        this.startTime = startTime;
        this.duration = Optional.empty();
    }

    public NonDefaultDayData(String startTime, Optional<Integer> duration) {
        this.startTime = startTime;
        this.duration = duration;
    }

    public Optional<Integer> getDuration() {
        return duration;
    }

    public String getStartTime() {
        return startTime;
    }
}
