package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.listing.domain.Defendant;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.unallocated-hearing-listed")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class UnallocatedHearingListed extends HearingEvent {

    private final String type;
    private final LocalDate startDate;
    private final Integer estimateMinutes;
    private final String caseId;
    private final String courtCentreId;
    private final List<Defendant> defendants;

    @JsonCreator
    public UnallocatedHearingListed(@JsonProperty(value = "hearingId") final String hearingId,
                                    @JsonProperty(value = "type") final String type,
                                    @JsonProperty(value = "startDate") final LocalDate startDate,
                                    @JsonProperty(value = "estimateMinutes") final Integer estimateMinutes,
                                    @JsonProperty(value = "caseId") final String caseId,
                                    @JsonProperty(value = "courtCentreId") final String courtCentreId,
                                    @JsonProperty(value = "defendants") final List<Defendant> defendants) {
        super(hearingId);
        this.type = type;
        this.startDate = startDate;
        this.estimateMinutes = estimateMinutes;
        this.caseId = caseId;
        this.courtCentreId = courtCentreId;
        this.defendants = new ArrayList(defendants);
    }


    public String getType() { return type; }

    public LocalDate getStartDate() { return startDate; }

    public Integer getEstimateMinutes() { return estimateMinutes; }

    public String getCaseId() {
        return caseId;
    }

    public String getCourtCentreId() { return courtCentreId; }

    public List<Defendant> getDefendants() {
        return new ArrayList(defendants);
    }
}
