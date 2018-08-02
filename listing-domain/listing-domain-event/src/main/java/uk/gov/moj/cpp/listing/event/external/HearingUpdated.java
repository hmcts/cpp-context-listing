package uk.gov.moj.cpp.listing.event.external;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("hearing-updated")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HearingUpdated {

    private final BaseHearing hearing;

    public HearingUpdated(@JsonProperty(value = "hearing") final BaseHearing hearing) {
        this.hearing = hearing;
    }

    public BaseHearing getHearing() {
        return hearing;
    }

    @Override
    public String toString() {
        return "HearingUpdated {" +
                ", hearing=" + hearing +
                '}';
    }
}
