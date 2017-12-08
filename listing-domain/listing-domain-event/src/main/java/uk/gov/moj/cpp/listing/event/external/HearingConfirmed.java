package uk.gov.moj.cpp.listing.event.external;

import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("hearing-confirmed")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HearingConfirmed {

    private final String caseId;
    private final String urn;
    private final Hearing hearing;



    public HearingConfirmed(@JsonProperty(value = "caseId") final String caseId,
                            @JsonProperty(value = "urn") final String urn,
                            @JsonProperty(value = "hearing") final Hearing hearing) {
        this.caseId = caseId;
        this.urn = urn;
        this.hearing = hearing;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    public Hearing getHearing() {
        return hearing;
    }

    @Override
    public String toString() {
        return "HearingConfirmed{" +
                "caseId='" + caseId + '\'' +
                ", urn='" + urn + '\'' +
                ", hearing=" + hearing +
                '}';
    }
}
