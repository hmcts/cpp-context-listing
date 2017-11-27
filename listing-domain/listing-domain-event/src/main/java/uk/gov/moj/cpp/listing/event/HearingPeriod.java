package uk.gov.moj.cpp.listing.event;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class HearingPeriod {

    private final LocalDate startDate;
    private final LocalTime startTime;
    private final Boolean notBefore;

    public HearingPeriod(@JsonProperty(value = "startDate") final LocalDate startDate,
                         @JsonProperty(value = "startTime") final LocalTime startTime,
                         @JsonProperty(value = "notBefore") final Boolean notBefore) {
        this.startDate = startDate;
        this.startTime = startTime;
        this.notBefore = notBefore;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public Boolean getNotBefore() {
        return notBefore;
    }
}
